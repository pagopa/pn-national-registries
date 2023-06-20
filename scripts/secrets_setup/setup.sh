#!/usr/bin/env bash
    
set -Eeuo pipefail
trap cleanup SIGINT SIGTERM ERR EXIT

cleanup() {
  trap - SIGINT SIGTERM ERR EXIT
  # script cleanup here
}

scriptDir=$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd -P)


usage() {
      cat <<EOF
    Usage: $(basename "${BASH_SOURCE[0]}") [-h] [-v] -r <aws-region> -p <aws-profile>
    [-h]                      : this help message
    [-v]                      : verbose mode
    [-r <aws-region>]
    -p <aws-profile>                profilo AWS per accedere all'account pn-core
    This script require following executable configured in the PATH variable:
     - aws cli 2.0 
     - jq
EOF
  exit 1
}

parse_params() {
  # default values of variables set from params
  region="eu-south-1"

  while :; do
    case "${1-}" in
    -h | --help) usage ;;
    -v | --verbose) set -x ;;
    -r | --region ) 
      region="${2-}"
      shift
      ;;
    -p | --profile ) 
      profile="${2-}"
      shift
      ;;
    -?*) usage ;;
    *) break ;;
    esac
    shift
  done

  # check required params and arguments
  [[ -z "${profile-}" ]] && usage 

  args=("$@")


  

  return 0
}

dump_params(){
  echo ""
  echo "######      PARAMETERS      ######"
  echo "##################################"
  echo "AWS Region:          ${region}"
  echo "Zone Profile:        ${profile}"
}


# START SCRIPT

parse_params "$@"
dump_params

function prepare_key_and_alias(){
    keyArn=$1
    alias=$2
    description=$3
    if ( [ -z "$keyArn" ] ) then
        keyArn=$(aws --profile ${profile} --region ${region} kms create-key \
        --description $3 \
        --key-usage SIGN_VERIFY \
        --key-spec RSA_2048 | jq -r '.KeyMetadata.Arn' )
        echo "Created key ${keyArn} for ${description}"
    fi

    aliasExists=$(aws --profile ${profile} --region ${region} kms list-aliases | jq -r --arg aliasName "alias/$alias" '.Aliases[] | select(.AliasName == $aliasName)' | jq -r '.AliasName' )
    echo "Alias exists: "$aliasExists

    if ( [ -z "$aliasExists" ] ) then
        aws --profile ${profile} --region ${region} kms create-alias \
            --alias-name alias/${alias} \
            --target-key-id $keyArn
        
        echo "Created alias alias/${alias}"
    else
        aws --profile ${profile} --region ${region} kms update-alias \
            --alias-name alias/${alias} \
            --target-key-id $keyArn
        echo "Updated alias alias/${alias}"
    fi
}

read -p "Enter AnprPDNDSessionTokenSigningKeyARN or leave it empty to create a new one: " AnprPDNDSessionTokenSigningKeyARN
read -p "Enter AnprPDNDSessionTokenSigningKeyAlias (without alias/ prefix): " AnprPDNDSessionTokenSigningKeyAlias
echo "You entered AnprPDNDSessionTokenSigningKeyARN: ${AnprPDNDSessionTokenSigningKeyARN}"
echo "You entered AnprPDNDSessionTokenSigningKeyAlias: ${AnprPDNDSessionTokenSigningKeyAlias}"
prepare_key_and_alias "$AnprPDNDSessionTokenSigningKeyARN" "$AnprPDNDSessionTokenSigningKeyAlias" "AnprPDNDSessionTokenSigningKey"

read -p "Enter CheckCfPDNDSessionTokenSigningKeyARN or leave it empty to create a new one: " CheckCfPDNDSessionTokenSigningKeyARN
read -p "Enter CheckCfPDNDSessionTokenSigningKeyAlias (without alias/ prefix): " CheckCfPDNDSessionTokenSigningKeyAlias
echo "You entered CheckCfPDNDSessionTokenSigningKeyARN: ${CheckCfPDNDSessionTokenSigningKeyARN}"
echo "You entered CheckCfPDNDSessionTokenSigningKeyAlias: ${CheckCfPDNDSessionTokenSigningKeyAlias}"
prepare_key_and_alias "$CheckCfPDNDSessionTokenSigningKeyARN" "$CheckCfPDNDSessionTokenSigningKeyAlias" "CheckCfPDNDSessionTokenSigningKey"

read -p "Enter InadPDNDSessionTokenSigningKeyARN or leave it empty to create a new one: " InadPDNDSessionTokenSigningKeyARN
read -p "Enter InadPDNDSessionTokenSigningKeyAlias (without alias/ prefix): " InadPDNDSessionTokenSigningKeyAlias
echo "You entered InadPDNDSessionTokenSigningKeyARN: ${InadPDNDSessionTokenSigningKeyARN}"
echo "You entered InadPDNDSessionTokenSigningKeyAlias: ${InadPDNDSessionTokenSigningKeyAlias}"
prepare_key_and_alias "$InadPDNDSessionTokenSigningKeyARN" "$InadPDNDSessionTokenSigningKeyAlias" "InadPDNDSessionTokenSigningKey"

read -p "Enter CheckCfSigningKeyARN or leave it empty to create a new one: " CheckCfSigningKeyARN
read -p "Enter CheckCfSigningKeyAlias (without alias/ prefix): " CheckCfSigningKeyAlias
echo "You entered CheckCfSigningKeyARN: ${CheckCfSigningKeyARN}"
echo "You entered CheckCfSigningKeyAlias: ${CheckCfSigningKeyAlias}"
prepare_key_and_alias "$CheckCfSigningKeyARN" "$CheckCfSigningKeyAlias" "CheckCfSigningKey"

read -p "Enter InfoCamereSigningKeyARN or leave it empty to create a new one: " InfoCamereSigningKeyARN
read -p "Enter InfoCamereSigningKeyAlias (without alias/ prefix): " InfoCamereSigningKeyAlias
echo "You entered InfoCamereSigningKeyARN: ${InfoCamereSigningKeyARN}"
echo "You entered InfoCamereSigningKeyAlias: ${InfoCamereSigningKeyAlias}"
prepare_key_and_alias "$InfoCamereSigningKeyARN" "$InfoCamereSigningKeyAlias" "InfoCamereSigningKey"
