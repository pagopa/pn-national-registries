#! /bin/sh -e

# Path: scripts/generate_certificate_from_kms/create_kms_and_generate_cert.sh
#
#
# preliminary AWS login:
#   aws sso login --profile sso_pn-core-dev


if [ $# -ne 10 ] && [ $# -ne 12 ]; then
    echo "Usage: sudo ./create_kms_and_generate_cert.sh --keyalias <alias/KEYALIAS> --fqdn <FQDN> --parameter-name <PARAMETER> --region <REGION> --e-mail <E-MAIL> (--profile <PROFILE>))"
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

# in AWS KMS, get key creation date from key id
KEYDATE=$(aws kms describe-key --key-id ${KEYID} --region ${REGION} | jq -r ".KeyMetadata.CreationDate")
# echo $(aws kms describe-key --key-id 50431e00-79d4-4966-ad70-881d965bdb07 --region eu-south-1 --profile sso_pn-core-dev | jq -r ".KeyMetadata.CreationDate")


# how long have passed between now and KEYDATE, in Linux and macOS
# check if we are Linux or MacOS
if [ "$(uname)" = "Darwin" ]; then
    # MacOS
    KEYAGE_DAYS=$((($(date +%s) - $(date -j -f "%Y-%m-%dT%H:%M:%S" "${KEYDATE%.*}" +%s)) / 86400))
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    # Linux
    KEYAGE_DAYS=$(echo $((($(date +%s) - $(date -d ${KEYDATE} +%s)) / 86400)))
fi

# how long have passed between now and KEYDATE, in Linux and macOS
#KEYAGE_DAYS=$(echo $((($(date +%s) - $(date -d ${KEYDATE} +%s)) / 86400)))
# test in macOS
# KEYAGE_DAYS=$((($(date +%s) - $(date -j -f "%Y-%m-%dT%H:%M:%S" "${KEYDATE%.*}" +%s)) / 86400))

# if the key is older than 365 days, rotate it
if [ ${KEYAGE_DAYS} -gt ${DAYS_FOR_ROTATION} ]; then
    echo "Rotating key ${KEYID}..."
    # create a new key asymmetric key
    NEWKEYID=$(aws kms create-key --region ${REGION} | jq -r ".KeyMetadata.KeyId")

    # launch the generate script with the new key id
    ./generate.sh --fqdn ${FQDN} --keyid ${NEWKEYID} --parameter-name ${PARAMETER_NAME} --region ${REGION} --e-mail ${EMAIL}

    aws kms update-alias --alias-name alias/${KEYALIAS} --target-key-id ${KEYID} --region ${REGION}
    aws kms schedule-key-deletion --key-id ${KEYID} --pending-window-in-days 7 --region ${REGION}
else
    echo "Key ${KEYID} is ${KEYAGE_DAYS} days old, not rotating it."

    # launch the generate script with the existing key id
    ./generate.sh --fqdn ${FQDN} --keyid ${KEYID} --parameter-name ${PARAMETER_NAME} --region ${REGION} --e-mail ${EMAIL}
fi


# alla fine del generate.sh, spostare l'alias (se ho ruotato)
# aws kms update-alias --alias-name alias/testcert --target-key-id 50431e00-79d4-4966-ad70-881d965bdb07 --region eu-south-1 --profile sso_pn-core-dev

# schedulare anche la cancellazione della chiave (se ho ruotato)
# aws kms schedule-key-deletion --key-id 50431e00-79d4-4966-ad70-881d965bdb07 --pending-window-in-days 7 --region eu-south-1 --profile sso_pn-core-dev