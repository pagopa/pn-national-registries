#! /bin/sh -e

# Path: scripts/generate_certificate_from_kms/create_kms_and_generate_cert.sh
#
#
# preliminary AWS login:
#   aws sso login --profile sso_pn-core-dev
#
#   cd scripts/generate_certificate_from_kms
#
#   Python part:
#     source venv/bin/activate
#
#
# sudo ./create_kms_and_generate_cert.sh --keyalias testcomplete --fqdn test8.client.dev.notifichedigitali.it --parameter-name testcert8 --region eu-south-1 --e-mail test@pagopa.it --profile sso_pn-core-dev
#
# sudo ./create_kms_and_generate_cert.sh --keyalias testcert --fqdn test10.client.dev.notifichedigitali.it --parameter-name testcert10 --region eu-south-1 --e-mail test@pagopa.it --profile sso_pn-core-dev
#
# UAT:
#   aws sso login --profile sso_pn-core-uat
#
#   infocamere:
# sudo ./create_kms_and_generate_cert.sh --keyalias testcert --fqdn infocamere.client.dev.notifichedigitali.it --parameter-name /pn-national-registries/infocamere-cert --region eu-south-1 --e-mail test@pagopa.it --profile sso_pn-core-uat


if [ $# -ne 10 ] && [ $# -ne 12 ]; then
    echo "Usage: sudo ./create_kms_and_generate_cert.sh --keyalias <KEYALIAS(without alias/)> --fqdn <FQDN> --parameter-name <PARAMETER> --region <REGION> --e-mail <E-MAIL> (--profile <PROFILE>))"
    exit 1
fi

# fixed parameters
DAYS_FOR_ROTATION=365

# read variable data from the command line
KEYALIAS=$2
FQDN=$4
PARAMETER_NAME=$6
REGION=$8
EMAIL=${10}

# read optional profile from the command line
if [ $# -eq 12 ]; then
    PROFILE=${12}
    export AWS_PROFILE=${PROFILE}
fi

# in aws KMS, locate the key id from the passed alias
KEYID=$(aws kms list-aliases --region ${REGION} | jq -r ".Aliases[] | select(.AliasName==\"alias/${KEYALIAS}\") | .TargetKeyId")
# echo $(aws kms list-aliases --region eu-south-1 --profile sso_pn-core-dev | jq -r ".Aliases[] | select(.AliasName==\"alias/testcert\") | .TargetKeyId")

# exit in case of error or empty key id
if [ -z ${KEYID} ]; then
    echo "Error locating key id"
    exit 1
else
    echo "Found key id: ${KEYID}"
fi
# should we create a new key? in case we don't find the key id?

# in AWS KMS, get key creation date from key id
KEYDATE=$(aws kms describe-key --key-id ${KEYID} --region ${REGION} | jq -r ".KeyMetadata.CreationDate")
# echo $(aws kms describe-key --key-id 50431e00-79d4-4966-ad70-881d965bdb07 --region eu-south-1 --profile sso_pn-core-dev | jq -r ".KeyMetadata.CreationDate")

# exit in case of error or empty key date
if [ -z ${KEYDATE} ]; then
    echo "Error getting key creation date"
    exit 1
else
    echo "Found key date: ${KEYDATE}"
fi

# how long have passed between now and KEYDATE, in Linux and macOS
# check if we are Linux or MacOS
if [ "$(uname)" = "Darwin" ]; then
    KEYAGE_DAYS=$((($(date +%s) - $(date -j -f "%Y-%m-%dT%H:%M:%S" "${KEYDATE%.*}" +%s)) / 86400))
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    KEYAGE_DAYS=$(echo $((($(date +%s) - $(date -d ${KEYDATE} +%s)) / 86400)))
fi

# how long have passed between now and KEYDATE, in Linux and macOS
#KEYAGE_DAYS=$(echo $((($(date +%s) - $(date -d ${KEYDATE} +%s)) / 86400)))
# test in macOS
# KEYAGE_DAYS=$((($(date +%s) - $(date -j -f "%Y-%m-%dT%H:%M:%S" "${KEYDATE%.*}" +%s)) / 86400))

# exit in case of empty key age
if [ -z ${KEYAGE_DAYS} ]; then
    echo "Error calculating key age"
    exit 1
else
    echo "Found key age: ${KEYAGE_DAYS} days"
fi


# if the key is older than 365 days, rotate it
if [ ${KEYAGE_DAYS} -gt ${DAYS_FOR_ROTATION} ]; then
    echo "Rotating key ${KEYID}..."

    # create a new key asymmetric key
    NEWKEYID=$(aws kms create-key --key-spec RSA_2048 --key-usage SIGN_VERIFY --region ${REGION} | jq -r ".KeyMetadata.KeyId")

    # continue only in case of success
    if [ $? -ne 0 ]; then
        echo "Error creating new key"
        exit 1
    else
        echo "New key id: ${NEWKEYID}"
    fi

    # launch the generate script with the new key id
    ./generate.sh --fqdn ${FQDN} --keyid ${NEWKEYID} --parameter-name ${PARAMETER_NAME} --region ${REGION} --e-mail ${EMAIL}

    # continue only in case of success
    if [ $? -ne 0 ]; then
        echo "Error generating certificate"
        exit 1
    fi

    aws kms update-alias --alias-name alias/${KEYALIAS} --target-key-id ${NEWKEYID} --region ${REGION}

    # exit in case of error
    if [ $? -ne 0 ]; then
        echo "Error updating alias"
        exit 1
    else
        echo "Alias updated"
    fi

    #aws kms schedule-key-deletion --key-id ${KEYID} --pending-window-in-days 7 --region ${REGION}

    # exit in case of error or give success message
    #if [ $? -ne 0 ]; then
    #    echo "Error scheduling key deletion"
    #    exit 1
    #else
    #    echo "New Key ${NEWKEYID} created and associated to alias. Certificate generated. Key ${KEYID} scheduled for deletion in 7 days"
    #fi
else
    echo "Key ${KEYID} is ${KEYAGE_DAYS} days old, not rotating it."

    # launch the generate script with the existing key id
    ./generate.sh --fqdn ${FQDN} --keyid ${KEYID} --parameter-name ${PARAMETER_NAME} --region ${REGION} --e-mail ${EMAIL}

    # error message in case of error or success message
    if [ $? -ne 0 ]; then
        echo "Error generating certificate"
        exit 1
    else
        echo "Certificate generated"
    fi
fi


# alla fine del generate.sh, spostare l'alias (se ho ruotato)
# aws kms update-alias --alias-name alias/testcert --target-key-id 50431e00-79d4-4966-ad70-881d965bdb07 --region eu-south-1 --profile sso_pn-core-dev

# schedulare anche la cancellazione della chiave (se ho ruotato)
# aws kms schedule-key-deletion --key-id 50431e00-79d4-4966-ad70-881d965bdb07 --pending-window-in-days 7 --region eu-south-1 --profile sso_pn-core-dev