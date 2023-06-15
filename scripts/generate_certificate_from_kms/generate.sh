#! /bin/sh -e

# preliminary AWS login:
#   aws sso login --profile sso_pn-core-dev
#
# command must be in the form (sudo is needed for certbot):
#   sudo ./generate.sh --fqdn cert6.dev.notifichedigitali.it --keyid 50431e00-79d4-4966-ad70-881d965bdb07 --parameter-name testcert6 --region eu-south-1 --e-mail test@pagopa.it
#
#   (sudo ./generate.sh --fqdn infocamere.client.dev.notifichedigitali.it --keyid 50431e00-79d4-4966-ad70-881d965bdb07 --parameter-name infocamere-client --region eu-south-1 --e-mail test@pagopa.it)
#       or
#   sudo ./generate.sh --fqdn test1.client.dev.notifichedigitali.it --keyid 50431e00-79d4-4966-ad70-881d965bdb07 --parameter-name infocamere-client-test --region eu-south-1 --e-mail test@pagopa.it --profile sso_pn-core-dev
#
#           real generations:
#       infocamere (InfoCamereSigningKeyARN):
#   sudo ./generate.sh --fqdn infocamere.client.dev.notifichedigitali.it --keyid 6fcca06c-077a-4819-ac87-a8fc9bdcecf6 --parameter-name /pn-national-registries/infocamere-cert --region eu-south-1 --e-mail test@pagopa.it --profile sso_pn-core-dev
#
#       ade-api (CheckCfSigningKeyARN):
#   sudo ./generate.sh --fqdn ade-api.client.dev.notifichedigitali.it --keyid 03486de6-2da8-48bc-a498-b323d29ff3b6 --parameter-name /pn-national-registries/ade-api-cert --region eu-south-1 --e-mail test@pagopa.it --profile sso_pn-core-dev
#
#       anpr-auth (AnprAuthChannelSigningKeyARN):
#   sudo ./generate.sh --fqdn anpr-auth.client.dev.notifichedigitali.it --keyid b341f795-41cf-4b6c-acdd-8172c874f2c2 --parameter-name /pn-national-registries/anpr-auth-cert --region eu-south-1 --e-mail test@pagopa.it --profile sso_pn-core-dev
#
#       anpr-api (AnprIntegrityRestSigningKeyARN):
#   sudo ./generate.sh --fqdn anpr-api.client.dev.notifichedigitali.it --keyid 6a67324a-e0ea-4bb8-ae21-912cb1b2ba01 --parameter-name /pn-national-registries/anpr-api-cert --region eu-south-1 --e-mail test@pagopa.it --profile sso_pn-core-dev
#
#
# see generated certificate request with:
#   openssl req -noout -text -in LOCAL_CSR.csr
#   openssl req -noout -text -in new.csr
# or with: https://www.digicert.com/ssltools/view-csr/
#
# examine the generated certificate file with:
#   openssl x509 -in 0000_cert.pem -text -noout

# Python part:
#   python -m venv aws-kms-sign-csr/venv
#   source venv/bin/activate
#   pip install -r aws-kms-sign-csr/requirements.txt
#
# FQDN schema:
#   infocamere.client.<env>.notifichedigitali.it


# uncomment and set the profile name for executing locally, or make the wanted profile the default one, or pass the profile name as a parameter
# export AWS_PROFILE=sso_pn-core-dev

# Check if the user has provided the correct number of parameters (--profile <PROFILE> is optional)
if [ $# -ne 10 ] && [ $# -ne 12 ]; then
    echo "Usage: sudo ./generate.sh --fqdn <FQDN> --keyid <KEYID> --parameter-name <PARAMETER> --region <REGION> --e-mail <E-MAIL> (--profile <PROFILE>))"
    exit 1
fi

# read variable data from the command line
FQDN=$2
KEYID=$4
PARAMETER_NAME=$6
REGION=$8
EMAIL=${10}
# read optional profile from the command line
if [ $# -eq 12 ]; then
    PROFILE=${12}
    export AWS_PROFILE=${PROFILE}
fi


# fixed parameters
CSR_FILE=LOCAL_CSR.csr
PRIVATE_KEY_FILE=PRIVATEKEY.key
PASSPHRASE=test
NEW_CSR_FILE=new.csr
FIXED=/C=IT/ST=Italy/L=Rome/O=PagoPA/OU=SEND

# generate private key and CSR (e-mail address is optional)
openssl req -newkey rsa:2048 -keyout ${PRIVATE_KEY_FILE} -out ${CSR_FILE} -subj ${FIXED}/CN=${FQDN} -passout pass:${PASSPHRASE}

# sign the CSR with AWS KMS (https://github.com/g-a-d/aws-kms-sign-csr)
python3 aws-kms-sign-csr/aws-kms-sign-csr.py --region ${REGION} --keyid ${KEYID} --hashalgo sha256 --signalgo RSA ${CSR_FILE} > ${NEW_CSR_FILE}

# create the certificate from the certificate request, creating records on AWS Route53
    # comment these two lines for avoiding multiple requests to certbot (not two many for the same FQDN are allowed)
rm -f *.pem
certbot certonly --csr ${NEW_CSR_FILE} --dns-route53 -d ${FQDN} --non-interactive --agree-tos -m ${EMAIL} 

# Requesting a certificate for cert.dev.notifichedigitali.it
#Successfully received certificate.
#Certificate is saved at:            /Users/marcoiannaccone/dev/work/PagoPA/testcert/0000_cert.pem
#Intermediate CA chain is saved at:  /Users/marcoiannaccone/dev/work/PagoPA/testcert/0000_chain.pem
#Full certificate chain is saved at: /Users/marcoiannaccone/dev/work/PagoPA/testcert/0001_chain.pem
#This certificate expires on 2023-09-04.

# if file exists...
if [ -f "0000_cert.pem" ]; then
    # produce a json with this structure
    # {
    #   "keyId": "ID della chiave KMS usata per firmare il certificato",
    #   "cert": "certificato",
    #   "dns": "DNS da usare per la validazione del certificato e come common name nella CSR"
    # }
    parameter_json="{ \
        \"keyId\": \"${KEYID}\", \
        \"cert\": \"$(base64 -i 0000_cert.pem)\", \
        \"dns\": \"${FQDN}\" \
    }"
    #echo $parameter_json

    # convert the file to base64 and send that to AWS parameter store
        # -i works on Linux and Mac, without -i
    aws ssm put-parameter --name "${PARAMETER_NAME}" --type "String" --value "${parameter_json}" --overwrite --region ${REGION}
fi

# in case we need to read parameters from AWS parameter store:
# aws ssm --profile sso_pn-core-dev --region eu-south-1 get-parameter --name /certificates/cert1.dev.notifichedigitali.it/cert --output json --no-paginate | jq -r .Parameter.Name
