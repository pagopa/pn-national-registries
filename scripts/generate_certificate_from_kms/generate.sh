# preliminary AWS login:
#   aws sso login --profile sso_pn-core-dev
#
# command must be in the form (sudo is needed for certbot):
#   sudo ./generate.sh --fqdn cert4.dev.notifichedigitali.it --profile sso_pn-core-dev --region eu-south-1 --keyid 50431e00-79d4-4966-ad70-881d965bdb07
#
# see generated certificate request with:
#   openssl req -noout -text -in LOCAL_CSR.csr
#   openssl req -noout -text -in new.csr
# or with: https://www.digicert.com/ssltools/view-csr/

# Python part:
#   python -m venv aws-kms-sign-csr/venv
#   source venv/bin/activate
#   pip install -r aws-kms-sign-csr/requirements.txt


# Check if the user has provided a FQDN #and a passphrase
if [ $# -ne 8 ]; then
    echo "Usage: ./generate.sh --fqdn <FQDN> --profile <PROFILE> --region <REGION> --keyid <KEYID>"
    exit 1
fi

# read variable data from the command line
FQDN=$2
PROFILE=$4
REGION=$6
KEYID=$8

# fixed parameterså
CSR_FILE=LOCAL_CSR.csr
PRIVATE_KEY_FILE=PRIVATEKEY.key
PASSPHRASE=test
NEW_CSR_FILE=new.csr
FIXED=/C=IT/ST=Italy/L=Rome/O=PagoPA/OU=SEND

# generate private key and CSR (e-mail address is optional)
openssl req -newkey rsa:2048 -keyout ${PRIVATE_KEY_FILE} -out ${CSR_FILE} -subj ${FIXED}/CN=${FQDN} -passout pass:${PASSPHRASE}

# sign the CSR with AWS KMS (https://github.com/g-a-d/aws-kms-sign-csr)
python3 aws-kms-sign-csr/aws-kms-sign-csr.py --region ${REGION} --profile ${PROFILE} --keyid ${KEYID} --hashalgo sha256 --signalgo RSA ${CSR_FILE} > ${NEW_CSR_FILE}

#read ~/.aws/config file and get, in the content, the directive with [profile ${PROFILE}]
config_file=~/.aws/config
target_block="[profile ${PROFILE}]"
# set a flag to indicate if the target block is found
found_block=false
# read the file line by line
echo "" > temp_aws_config_file
while IFS= read -r line; do
    # sheck if the current line matches the target block
    if [[ $line == "$target_block" ]]; then
        # set the flag to indicate that the target block is found
        found_block=true
    elif [[ $line == [* ]]; then
        # check if the current line starts a new block
        if [[ $found_block == true ]]; then
            # if the target block has already been found and a new block is encountered,
            # break the loop as we have reached the end of the target block
            break
        fi
    fi

    # if the target block is found, print the current line to the file
    if [[ $found_block == true ]]; then
        # if the line is equal to the target block, print it to the file
        # print [profile default] to the file
        if [[ $line == "$target_block" ]]; then
            echo "[default]" >> temp_aws_config_file
        else
            echo "$line" >> temp_aws_config_file
        fi
    fi
done < "$config_file"

export AWS_CONFIG_FILE=temp_aws_config_file
# there's also AWS_PROFILE
# create the certificate from the certificate request, creating records on AWS Route53
    # comment these two lines for avoiding multiple requests to certbot (not two many for the same FQDN are allowed)
#rm -f *.pem
#certbot certonly --csr ${NEW_CSR_FILE} --dns-route53 -d ${FQDN}

# Requesting a certificate for cert.dev.notifichedigitali.it
#Successfully received certificate.
#Certificate is saved at:            /Users/marcoiannaccone/dev/work/PagoPA/testcert/0000_cert.pem
#Intermediate CA chain is saved at:  /Users/marcoiannaccone/dev/work/PagoPA/testcert/0000_chain.pem
#Full certificate chain is saved at: /Users/marcoiannaccone/dev/work/PagoPA/testcert/0001_chain.pem
#This certificate expires on 2023-09-04.

# examinate the certificate file with:
#   openssl x509 -in 0000_cert.pem -text -noout

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
    aws ssm put-parameter --name "/certificates/${FQDN}/cert" --type "SecureString" --value "${parameter_json}" --overwrite --region ${REGION} # now default profile

    # send the file to AWS ACM
    # PROBLEM: the private key can't be retrieved from KMS and passed to the next command
    #aws acm import-certificate --certificate fileb://0000_cert.pem --private-key fileb://kms_key_file.pem --certificate-chain fileb://0001_chain.pem --region ${REGION}
fi


# profile rimosso
# leggere parameter store per keyId e dns
# aws ssm --profile sso_pn-core-dev --region eu-south-1 get-parameter --name /certificates/cert1.dev.notifichedigitali.it/cert --output json --no-paginate | 
# aws ssm --profile sso_pn-core-dev --region eu-south-1 get-parameter --name /certificates/cert1.dev.notifichedigitali.it/cert --output json --no-paginate | jq -r .Parameter.Name
# parameter no securestring
# rimuovere mail


# chiave kms da leggere:

