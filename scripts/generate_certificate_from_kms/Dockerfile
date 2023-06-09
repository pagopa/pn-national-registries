#   cd generate_certificate_from_kms
#   docker build -t generate_certificate_from_kms .
#   docker run -it --rm generate_certificate_from_kms /bin/bash ./generate.sh --fqdn test1.client.dev.notifichedigitali.it --keyid 50431e00-79d4-4966-ad70-881d965bdb07 --parameter-name infocamere-client-test --region eu-south-1 --e-mail test@pagopa.it

# local test, after AWS login:
#   docker run -it -v $HOME/.aws/:/root/.aws --rm generate_certificate_from_kms /bin/bash ./generate.sh --fqdn test3.client.dev.notifichedigitali.it --keyid 50431e00-79d4-4966-ad70-881d965bdb07 --parameter-name infocamere-client-test --region eu-south-1 --e-mail test@pagopa.it --profile sso_pn-core-dev


FROM alpine:3.18.0
RUN apk add --no-cache ca-certificates bash jq aws-cli coreutils openssl python3 py3-pip

COPY . .

RUN pip install -r requirements.txt
RUN chmod +x generate.sh

#ENTRYPOINT ["/bin/bash", "generate_certificate.sh"]
