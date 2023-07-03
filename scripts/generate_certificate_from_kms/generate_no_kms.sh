#! /bin/sh -e

# preliminary AWS login:
#   aws sso login --profile sso_pn-core-dev
#
# call with sudo (for certbot)
#
# see generated certificate request with:
#   openssl req -noout -text -in LOCAL_CSR.csr
#   openssl req -noout -text -in new.csr
# or with: https://www.digicert.com/ssltools/view-csr/
#
# examine the generated certificate file with:
#   openssl x509 -in 0000_cert.pem -text -noout

# cd scripts/generate_certificate_from_kms
# Python part:
#   python -m venv aws-kms-sign-csr/venv
#   source venv/bin/activate
#   pip install -r aws-kms-sign-csr/requirements.txt
#
# FQDN schema:
#   infocamere.client.<env>.notifichedigitali.it
#
# new certificate generation process:
# 1. generate new key pair RSA_2048 senza passphase
# 2. generate CSR, signed with local private key
# 3. generate certificate from CSR, signed with local private key
# 4. save private key in AWS secret, creating or updating it (if as pem: openssl rsa -in private.key -out private.pem, che with https://8gwifi.org/PemParserFunctions.jsp)
# 5. save certificate in AWS parameter store, together with dns (public key is already in the certificate) and secret id
#
#
#   real dev infocamere (example, NOT to be used for infocamere):
# sudo ./generate_no_kms.sh --fqdn infocamere.client.dev.notifichedigitali.it --secretid pn-national-registries/infocamere-cert --parameter-name /pn-national-registries/infocamere-cert --e-mail test@pagopa.it --region eu-south-1 --profile sso_pn-core-dev
#
#   real dev ade-api:
# sudo ./generate_no_kms.sh --fqdn ade-api.client.dev.notifichedigitali.it --secretid pn-national-registries/ade-api-cert --parameter-name /pn-national-registries/ade-api-cert --e-mail test@pagopa.it --region eu-south-1 --profile sso_pn-core-dev
#
#
#
#
#   test:
# sudo ./generate_no_kms.sh --fqdn testinfocamere4.dev.notifichedigitali.it --secretid pn-national-registries/infocamere-test4 --parameter-name /infocamere/test4 --e-mail test@pagopa.it --region eu-south-1 --profile sso_pn-core-dev


# uncomment and set the profile name for executing locally, or make the wanted profile the default one, or pass the profile name as a parameter
# export AWS_PROFILE=sso_pn-core-dev

# Check if the user has provided the correct number of parameters (--profile <PROFILE> is optional)
#
# parameters ordering is important, as we are using positional parameters
if [ $# -ne 10 ] && [ $# -ne 12 ]; then
    echo "Usage: sudo ./generate.sh --fqdn <FQDN> --secretid <SECRETID> --parameter-name <PARAMETER_NAME> --e-mail <E-MAIL> --region <REGION> (--profile <PROFILE>))"
    exit 1
fi

rm -f *.key *.csr

# read variable data from the command line
FQDN=$2
SECRETID=$4
PARAMETER_NAME=$6
EMAIL=$8
REGION=${10}
# read optional profile from the command line
if [ $# -eq 12 ]; then
    PROFILE=${12}
    export AWS_PROFILE=${PROFILE}
fi


# fixed parameters
CSR_FILE=LOCAL_CSR.csr
PRIVATE_KEY_FILE=PRIVATEKEY.key
FIXED=/C=IT/ST=Italy/L=Rome/O=PagoPA/OU=SEND

# generate private key and CSR (e-mail address is optional), without passphrase
openssl req -newkey rsa:2048 -keyout ${PRIVATE_KEY_FILE} -out ${CSR_FILE} -subj ${FIXED}/CN=${FQDN} -nodes


if [ $? -ne 0 ]; then
    echo "Error generating CSR"
    exit 1
fi

# create the certificate from the certificate request, creating records on AWS Route53
rm -f 0000_cert.pem 0000_chain.pem 0001_chain.pem
certbot certonly --csr ${CSR_FILE} --dns-route53 -d ${FQDN} --non-interactive --agree-tos -m ${EMAIL}

# Requesting a certificate for cert.dev.notifichedigitali.it
#Successfully received certificate.
#Certificate is saved at:            /Users/marcoiannaccone/dev/work/PagoPA/testcert/0000_cert.pem
#Intermediate CA chain is saved at:  /Users/marcoiannaccone/dev/work/PagoPA/testcert/0000_chain.pem
#Full certificate chain is saved at: /Users/marcoiannaccone/dev/work/PagoPA/testcert/0001_chain.pem
#This certificate expires on 2023-09-04.

# obtain public key from certificate
# openssl x509 -pubkey -noout -in 0000_cert.pem  > 0000_public_key.pem

# if file exists...
if [ -f "0000_cert.pem" ]; then

    echo "Certificate generated"

    # save private key in AWS secret
    #
    # if the secret already exists, update it, else create it
    if aws secretsmanager describe-secret --secret-id ${SECRETID} --region ${REGION} >/dev/null 2>&1; then
        echo "Secret already present, updating it"
        aws secretsmanager update-secret --secret-id ${SECRETID} --secret-string file://${PRIVATE_KEY_FILE} --region ${REGION}
    else
        echo "Secret not present, creating it"
        aws secretsmanager create-secret --name ${SECRETID} --secret-string file://${PRIVATE_KEY_FILE} --region ${REGION}
    fi
    # this way if describe-secret doesn't find the secret it doesn't stop the script

    # in case of error, exit
    if [ $? -ne 0 ]; then
        echo "Error saving private key in AWS secret"
        exit 1
    fi

    # produce a json with this structure
    # {
    #   "secretid": "ID della chiave Certificate manager contenente la chiave privata",
    #   "cert": "certificato",
    #   "dns": "DNS da usare per la validazione del certificato e come common name nella CSR"
    # }
    parameter_json="{ \
        \"secretid\": \"${SECRETID}\", \
        \"cert\": \"$(base64 -i 0000_cert.pem | tr -d '\n')\", \
        \"dns\": \"${FQDN}\" \
    }"
    # the base 64 conversion must not contain newlines, for the JSON to be valid

    # produce a json in parameter_json variable
    # parameter_json=$(jq -n \
    #     --arg secretid "${SECRETID}" \
    #     --arg cert "$(base64 -i 0000_cert.pem)" \
    #     --arg dns "${FQDN}" \
    #     '{secretid: $secretid, cert: $cert, dns: $dns}')


    # convert the file to base64 and send that to AWS parameter store
        # -i works on Linux and Mac, without -i
    aws ssm put-parameter --name "${PARAMETER_NAME}" --type "String" --value "${parameter_json}" --overwrite --region ${REGION}

    if [ $? -ne 0 ]; then
        echo "Error sending certificate to AWS parameter store"
        exit 1
    else
        echo "Certificate generated from KMS and sent to AWS parameter store"
        exit 0
    fi
else
    echo "Error generating certificate"
    exit 1
fi

# in case we need to read parameters from AWS parameter store:
# aws ssm --profile sso_pn-core-dev --region eu-south-1 get-parameter --name /certificates/cert1.dev.notifichedigitali.it/cert --output json --no-paginate | jq -r .Parameter.Name

# write the PRIVATE_KEY_FILE to AWS Secrets Manager
# aws secretsmanager create-secret --name ${PARAMETER_NAME} --secret-string file://${PRIVATE_KEY_FILE} --region ${REGION}


# update the AWS secrets manager secret
# aws secretsmanager update-secret --secret-id ${PARAMETER_NAME} --secret-string file://${PRIVATE_KEY_FILE} --region ${REGION}

# check if the secret exists
# aws secretsmanager describe-secret --secret-id ${PARAMETER_NAME} --region ${REGION}

# create or update an aws secrets manager secret
# aws secretsmanager create-secret --name ${PARAMETER_NAME} --secret-string file://${PRIVATE_KEY_FILE} --region ${REGION}

# retrieve the secret from an aws secrets manager secret
# aws secretsmanager get-secret-value --secret-id ${PARAMETER_NAME} --region ${REGION}

# retrieve the secret from an aws secrets manager secret, decrypting it
# aws secretsmanager get-secret-value --secret-id ${PARAMETER_NAME} --region ${REGION} | jq -r .SecretString | jq -r .private_key | base64 -D > private_key.pem

# *** retrieve the secret from an aws secrets manager secret, as key-value pairs
# aws secretsmanager get-secret-value --secret-id ${PARAMETER_NAME} --region ${REGION} | jq -r .SecretString | jq -r 'to_entries[] | "\(.key)=\(.value)"'

# write two parameters to AWS as single secret
# aws secretsmanager create-secret --name TestSecret1 --secret-string '{"Key1": "Value1", "Key2": "Value2"}' --region ${REGION}
# aws secretsmanager create-secret --name TestSecret1 --secret-string '{"Key1": "Value1", "Key2": "Value2"}' --region eu-south-1 --profile sso_pn-core-dev