import boto3
import json
import os
import subprocess
import logging
from datetime import datetime, timedelta, timezone

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def get_cert_expiry(acm, cert_arn):
    try:
        response = acm.describe_certificate(CertificateArn=cert_arn)
        return response['Certificate']['NotAfter']
    except Exception as e:
        logger.error(f"Error describing certificate {cert_arn}: {e}")
        return None

def lambda_handler(event, context):
    acm = boto3.client('acm')
    secrets = boto3.client('secretsmanager')
    ssm = boto3.client('ssm')
    
    action = event.get('action', 'renewal')
    fqdn = event.get('fqdn')
    secret_id = event.get('secret_id') # Base secret name, e.g., "pn-national-registries/ade-api-cert"
    parameter_name = event.get('parameter_name') # Base parameter name, e.g., "/pn-national-registries/ade-api-cert"
    
    if not fqdn or not secret_id:
        return {"error": "Missing fqdn or secret_id in event"}
    
    current_aws_secret = f"{secret_id}-aws"
    next_aws_secret = f"{secret_id}-next-aws"
    current_aws_param = f"{parameter_name}-aws" if parameter_name else None
    next_aws_param = f"{parameter_name}-next-aws" if parameter_name else None

    # --- MANUAL SWAP LOGIC ---
    if action == 'swap':
        logger.info(f"Starting manual swap: promoting {next_aws_secret} to {current_aws_secret}")
        try:
            next_secret_val = secrets.get_secret_value(SecretId=next_aws_secret)
            secret_data = json.loads(next_secret_val['SecretString'])
            
            # 1. Update the 'actual' secret with 'next' content
            # Ensure the secret data points to the main secret ID now
            secret_data['secret_id_used'] = current_aws_secret 
            
            secrets.put_secret_value(
                SecretId=current_aws_secret,
                SecretString=json.dumps(secret_data)
            )
            logger.info(f"Successfully promoted secret content to {current_aws_secret}")

            # 2. Update the 'actual' SSM parameter
            if current_aws_param:
                param_value = {
                    "secretid": current_aws_secret,
                    "cert": secret_data.get('certificate'),
                    "dns": fqdn
                }
                ssm.put_parameter(
                    Name=current_aws_param,
                    Value=json.dumps(param_value),
                    Type='String',
                    Overwrite=True
                )
                logger.info(f"Successfully updated active SSM parameter: {current_aws_param}")
            
            return {
                "status": "SWAP_SUCCESS",
                "promoted_from": next_aws_secret,
                "promoted_to": current_aws_secret
            }
        except Exception as e:
            logger.error(f"Error during swap: {e}")
            return {"error": f"Swap failed: {str(e)}"}

    # --- RENEWAL / GENERATION LOGIC ---
    force_renewal = event.get('force_renewal', False)
    days_before_expiry = event.get('days_before_expiry', 30)
    
    target_secret_id = current_aws_secret
    target_parameter_name = current_aws_param
    
    # 0. Check if renewal is needed
    if not force_renewal:
        try:
            # Check if current-aws exists and its expiry
            current_secret_val = secrets.get_secret_value(SecretId=current_aws_secret)
            secret_data = json.loads(current_secret_val['SecretString'])
            cert_arn = secret_data.get('certificate_arn')
            
            if cert_arn:
                expiry_date = get_cert_expiry(acm, cert_arn)
                if expiry_date:
                    now = datetime.now(timezone.utc)
                    remaining_time = expiry_date - now
                    logger.info(f"Current certificate {cert_arn} expires on {expiry_date}. Remaining: {remaining_time}")
                    
                    if remaining_time > timedelta(days=days_before_expiry):
                        logger.info(f"Current certificate is still valid for more than {days_before_expiry} days. Skipping.")
                        return {
                            "status": "SKIPPED",
                            "message": f"Certificate valid until {expiry_date}",
                            "certificate_arn": cert_arn
                        }
                    else:
                        logger.info("Current certificate expiring soon. Will generate renewal in -next slot.")
                        target_secret_id = next_aws_secret
                        target_parameter_name = next_aws_param
        except secrets.exceptions.ResourceNotFoundException:
            logger.info(f"Target {current_aws_secret} not found. Proceeding with initial generation.")
            target_secret_id = current_aws_secret
            target_parameter_name = current_aws_param
        except Exception as e:
            logger.warning(f"Could not check status: {e}. Defaulting to -next-aws to be safe.")
            target_secret_id = next_aws_secret
            target_parameter_name = next_aws_param

    logger.info(f"Generating ACM certificate for {fqdn} and storing in {target_secret_id}")
    
    # 1. Generate Private Key
    subprocess.run(["openssl", "genrsa", "-out", "/tmp/private.key", "2048"], check=True)
    
    # 2. Generate Self-Signed Certificate
    subj = f"/C=IT/ST=Italy/L=Rome/O=PagoPA/OU=PN/CN={fqdn}"
    subprocess.run([
        "openssl", "req", "-new", "-x509", "-sha256", "-key", "/tmp/private.key", 
        "-out", "/tmp/certificate.pem", "-days", "365", "-subj", subj
    ], check=True)
    
    with open("/tmp/private.key", "rb") as f:
        private_key = f.read()
    with open("/tmp/certificate.pem", "rb") as f:
        certificate = f.read()
        
    # 3. Import into ACM
    response = acm.import_certificate(
        Certificate=certificate,
        PrivateKey=private_key
    )
    cert_arn = response['CertificateArn']
    logger.info(f"Imported certificate into ACM: {cert_arn}")
    
    # 4. Store in Secrets Manager
    secret_content = {
        "private_key": private_key.decode('utf-8'),
        "certificate": certificate.decode('utf-8'),
        "certificate_arn": cert_arn,
        "fqdn": fqdn
    }
    
    try:
        secrets.create_secret(
            Name=target_secret_id,
            SecretString=json.dumps(secret_content),
            Description=f"ACM certificate for {fqdn} (Managed by Lambda)"
        )
    except secrets.exceptions.ResourceExistsException:
        secrets.put_secret_value(
            SecretId=target_secret_id,
            SecretString=json.dumps(secret_content)
        )

    # 5. Optional: Update SSM Parameter
    if target_parameter_name:
        ssm = boto3.client('ssm')
        param_value = {
            "secretid": target_secret_id,
            "cert": certificate.decode('utf-8'),
            "dns": fqdn
        }
        ssm.put_parameter(
            Name=target_parameter_name,
            Value=json.dumps(param_value),
            Type='String',
            Overwrite=True
        )
        logger.info(f"Updated SSM parameter: {target_parameter_name}")
    
    return {
        "status": "SUCCESS",
        "certificate_arn": cert_arn,
        "secret_id": target_secret_id,
        "ssm_parameter": target_parameter_name
    }
