#!/bin/bash

apt -y install jq

keyID=$(aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 kms create-key --key-spec RSA_2048 --key-usage SIGN_VERIFY | jq ".KeyMetadata.KeyId")

echo "######## KeyId : " $keyID

echo "### CREATE SECRET FOR NATIONAL-REGISTRY ###"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    secretsmanager create-secret \
    --name "pn-national-registries/pdnd/ANPR" \
	--secret-string "{
    \"client_id\":\"5df9f218-df34-4f05-a9c4-e4a74b2a8e3f\",
    \"jwtConfig\":{
      \"kid\":\"kid123\",
      \"iss\":\"0a455cfd-eb86-46bc-a386-96f1baf94438\",
      \"sub\":\"0a455cfd-eb86-46bc-a386-96f1baf94438\",
      \"aud\":\"auth.uat.interop.pagopa.it/client-assertion\",
      \"purposeId\":\"58708207-d9b5-4223-b362-3f6ad4256256\"},
    \"keyId\":"$keyID",
    \"eservice_audience\":\"http://pn-ex-Appli-1TF83JFIR9AF3-439926467.eu-south-1.elb.amazonaws.com:8080/nationalregistriesmock\"
    }"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    secretsmanager create-secret \
    --name "pn-national-registries/ade/auth" \
	--secret-string "{\"trust\":\"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURFVENDQWZrQ0ZDMkQ5THBvekRxdmN5YVM5RDlMendmREZKN0dNQTBHQ1NxR1NJYjNEUUVCQ3dVQU1FVXgKQ3pBSkJnTlZCQVlUQWtGVk1STXdFUVlEVlFRSURBcFRiMjFsTFZOMFlYUmxNU0V3SHdZRFZRUUtEQmhKYm5SbApjbTVsZENCWGFXUm5hWFJ6SUZCMGVTQk1kR1F3SGhjTk1qUXhNakE1TURnME56TXpXaGNOTWpVd01UQTRNRGcwCk56TXpXakJGTVFzd0NRWURWUVFHRXdKQlZURVRNQkVHQTFVRUNBd0tVMjl0WlMxVGRHRjBaVEVoTUI4R0ExVUUKQ2d3WVNXNTBaWEp1WlhRZ1YybGtaMmwwY3lCUWRIa2dUSFJrTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQwpBUThBTUlJQkNnS0NBUUVBOXJzNE5reHZ0ejBWVFQ0U3U2Wkhob21pK1ZSUkJUL0RBVms1THpvTGRSSXJzSHJQCjFJVlFvQ3JxYTM0blB2NDF1WGw3V2VkKzM4NTQzTGI0TmJoaW5QYXZKV3lpSjNWU1hYNlZTeWt5T1J5WnEzVzkKVW1PSTlLS09OUHJ0STUvRWJBdUFJWmx0bVRHbDdnTTdOWWZxTGprSmZXK3djRE9xSXVzeGpCUzlYWjJNUmQxcQp3RlFlMy9xVTNWTUM5MzQ0Z0tyZ21jZkF4RTdYMmJid2tjU2cwOEluM3g4V0NwdVREN1dFVHBMY1VvZVlrOUdTCnYvUTJJcHVGaWFVMjR5bkNFeWdsRnkrdVZldTVOazNON3duRVUzb05wU0tmcWEwWlZyNGdjQWtwU3J2OFpvdmIKdTdxU0lSYW40VExvdnlJSlkzQkZDNTAwTFIyTGVsN3g0U2JqTFFJREFRQUJNQTBHQ1NxR1NJYjNEUUVCQ3dVQQpBNElCQVFDdXEwaTdRVUR0a1RKQTB6NUwzdjNxUitFeklXNXZ4c0QzMGFnbVNGZHdaMDArTVpHQkdRdFJodTFNCklrdmh4OXByS1I4ZEp6NTVzMVIrYWlHUzdBWU9xSzUwaFA2OGp3c2tVMnozbkJIbXZ4TGUvZ3hpUUJONmVtei8KRDA5NDA1alJBcXpkTEhIb1RmdUk0Y3J0NUY0b2JzQUkzdlNJdVRpWE9XMStmeEMwaFdnWE5IY1R2LzJNU0h1dQorRW9FMVFiZEpHcWEzeXM0cWxMSXd2QTVia1JDR2JoNmNudG44QU4xbGk0Yk5VRW5wR1FtUE9IVVBwa1NieFVJCndva0NKWmRUWHJLRUpManBpQTRiMkFpbzNvOWpyTGNvOGIydGJSdDFZa1ZFTGt6NFhkam5XNjNkRmwveVVES1gKaHVxSC9zYy9abnZMMCtJcDc2R0E4R0VCM2JTegotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\"}"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    secretsmanager create-secret \
    --name "pn-national-registries/CheckCF/auth-rest" \
	--secret-string "{\"trust\":\"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURFVENDQWZrQ0ZDMkQ5THBvekRxdmN5YVM5RDlMendmREZKN0dNQTBHQ1NxR1NJYjNEUUVCQ3dVQU1FVXgKQ3pBSkJnTlZCQVlUQWtGVk1STXdFUVlEVlFRSURBcFRiMjFsTFZOMFlYUmxNU0V3SHdZRFZRUUtEQmhKYm5SbApjbTVsZENCWGFXUm5hWFJ6SUZCMGVTQk1kR1F3SGhjTk1qUXhNakE1TURnME56TXpXaGNOTWpVd01UQTRNRGcwCk56TXpXakJGTVFzd0NRWURWUVFHRXdKQlZURVRNQkVHQTFVRUNBd0tVMjl0WlMxVGRHRjBaVEVoTUI4R0ExVUUKQ2d3WVNXNTBaWEp1WlhRZ1YybGtaMmwwY3lCUWRIa2dUSFJrTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQwpBUThBTUlJQkNnS0NBUUVBOXJzNE5reHZ0ejBWVFQ0U3U2Wkhob21pK1ZSUkJUL0RBVms1THpvTGRSSXJzSHJQCjFJVlFvQ3JxYTM0blB2NDF1WGw3V2VkKzM4NTQzTGI0TmJoaW5QYXZKV3lpSjNWU1hYNlZTeWt5T1J5WnEzVzkKVW1PSTlLS09OUHJ0STUvRWJBdUFJWmx0bVRHbDdnTTdOWWZxTGprSmZXK3djRE9xSXVzeGpCUzlYWjJNUmQxcQp3RlFlMy9xVTNWTUM5MzQ0Z0tyZ21jZkF4RTdYMmJid2tjU2cwOEluM3g4V0NwdVREN1dFVHBMY1VvZVlrOUdTCnYvUTJJcHVGaWFVMjR5bkNFeWdsRnkrdVZldTVOazNON3duRVUzb05wU0tmcWEwWlZyNGdjQWtwU3J2OFpvdmIKdTdxU0lSYW40VExvdnlJSlkzQkZDNTAwTFIyTGVsN3g0U2JqTFFJREFRQUJNQTBHQ1NxR1NJYjNEUUVCQ3dVQQpBNElCQVFDdXEwaTdRVUR0a1RKQTB6NUwzdjNxUitFeklXNXZ4c0QzMGFnbVNGZHdaMDArTVpHQkdRdFJodTFNCklrdmh4OXByS1I4ZEp6NTVzMVIrYWlHUzdBWU9xSzUwaFA2OGp3c2tVMnozbkJIbXZ4TGUvZ3hpUUJONmVtei8KRDA5NDA1alJBcXpkTEhIb1RmdUk0Y3J0NUY0b2JzQUkzdlNJdVRpWE9XMStmeEMwaFdnWE5IY1R2LzJNU0h1dQorRW9FMVFiZEpHcWEzeXM0cWxMSXd2QTVia1JDR2JoNmNudG44QU4xbGk0Yk5VRW5wR1FtUE9IVVBwa1NieFVJCndva0NKWmRUWHJLRUpManBpQTRiMkFpbzNvOWpyTGNvOGIydGJSdDFZa1ZFTGt6NFhkam5XNjNkRmwveVVES1gKaHVxSC9zYy9abnZMMCtJcDc2R0E4R0VCM2JTegotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\"}"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    secretsmanager create-secret \
    --name "pn-national-registries/ade-api-cert" \
	--secret-string "-----BEGIN PRIVATE KEY-----
MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQD2uzg2TG+3PRVN
PhK7pkeGiaL5VFEFP8MBWTkvOgt1Eiuwes/UhVCgKuprfic+/jW5eXtZ537fznjc
tvg1uGKc9q8lbKIndVJdfpVLKTI5HJmrdb1SY4j0oo40+u0jn8RsC4AhmW2ZMaXu
Azs1h+ouOQl9b7BwM6oi6zGMFL1dnYxF3WrAVB7f+pTdUwL3fjiAquCZx8DETtfZ
tvCRxKDTwiffHxYKm5MPtYROktxSh5iT0ZK/9DYim4WJpTbjKcITKCUXL65V67k2
Tc3vCcRTeg2lIp+prRlWviBwCSlKu/xmi9u7upIhFqfhMui/IgljcEULnTQtHYt6
XvHhJuMtAgMBAAECgf9JoiV9uDuOP3WDaFS+Tz4a3dF38ll/NrOWGFdYfG4UcLU0
Si8SEJ/vYlhQxwszwlam+oz4lVMzGl7+P4wRjbSN2Vpc65DUAhxFGFGT1DgA1Ql/
guGSsaZuDkqodTf5pO8NBJpVD0jAykGPhbdm8KzKAyUkcyGrR3GLmLRabVCuHBgy
LhEbyhQcPmeExUWgZRXUflGtxKJ92fMCWm7TGl6U5dlZ4o8BVo9ymVwLxsTqsR48
zZ6JQtX/xcfn9YGdnbW5eDQCxbopWmgPUnvW2jErXdM2LaijZdK3DA9cmralee2t
z0ZEhWqt8qcbMGxk4WpGltrd0OGBzY/462QJnU0CgYEA+OuwNFJH5tjUDixKB3hp
n+3EHwvJOuR9cYMteI55cy0k10pmfRe6ue3P7vzhwJ8JLBRwtnu7QWpNvDvi7WfC
TLNA1w9vIk0/3FdOD3E0mW0zcDuo15HOpaKgiA48g00n99zimop06e7vzXWFmCAC
316NcJgepRthrYlYPiK+8BMCgYEA/b+XagnVDVP9PIQPOoSHo+F3vzhqh8EoFD90
Rpts5deb7SJSf/vmvv1VeFRsVTkBUFtAxXbJV2vWuc0hNwKNa3gccF+CqpY25g1u
vC6twmhWwRYT0OH1WsHSkc5NFB5GfJyzUjM73ZVM0Fye7FEOxtxpQHOEQF+3+loL
3AgBx78CgYEAjL2+OCcZrmWEqKrUBJtJpXprPF6OnbTZVJgl2pc8BipNPUk835fO
JRhYTDlBb4a8SecYv6UAAIGZiEeYDEFxc6qai76XSJ7C79OROFv6sJE01010Lsar
P63O9H6QKnEymwuSOGz2o9pMTjAXpCbdWOx+Ll5kXVg5B0Sz1xYQPW8CgYAvqYsx
WcNxC3OnqE6L+VTZDYLGojwwX7G9i+W1VYBw87zQIABLFe/TtBYX0hO/NkPu6hUS
gitzhkgBAmzYvlv98cO1v9r1W93X/HHuWv48ZAjXn+P8+lRxxbJdvNhxjPZEddKh
AAqgpmiVQrB/RwJ1i/UhLBGNXe1a0wsjO0XKVQKBgQCuPoeugwG49WhSDcdTPfNR
qUr0BhqmXsXoXnQ9hwGr5JPL3R/IV+XwYe2Q4703eb2wZPGcl319747s9WBc1BEK
efyPP1TwPc0XXjm95AuYFXi8d2XCXxSfM65Xo1lSociwGpMkox7VDz6tH+xODceL
wvPNOVLoswDqL3CZRUlVEQ==
-----END PRIVATE KEY-----
"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    secretsmanager create-secret \
    --name "pn-national-registries/anpr/auth" \
	  --secret-string "{\"trust\":\"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURFVENDQWZrQ0ZDMkQ5THBvekRxdmN5YVM5RDlMendmREZKN0dNQTBHQ1NxR1NJYjNEUUVCQ3dVQU1FVXgKQ3pBSkJnTlZCQVlUQWtGVk1STXdFUVlEVlFRSURBcFRiMjFsTFZOMFlYUmxNU0V3SHdZRFZRUUtEQmhKYm5SbApjbTVsZENCWGFXUm5hWFJ6SUZCMGVTQk1kR1F3SGhjTk1qUXhNakE1TURnME56TXpXaGNOTWpVd01UQTRNRGcwCk56TXpXakJGTVFzd0NRWURWUVFHRXdKQlZURVRNQkVHQTFVRUNBd0tVMjl0WlMxVGRHRjBaVEVoTUI4R0ExVUUKQ2d3WVNXNTBaWEp1WlhRZ1YybGtaMmwwY3lCUWRIa2dUSFJrTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQwpBUThBTUlJQkNnS0NBUUVBOXJzNE5reHZ0ejBWVFQ0U3U2Wkhob21pK1ZSUkJUL0RBVms1THpvTGRSSXJzSHJQCjFJVlFvQ3JxYTM0blB2NDF1WGw3V2VkKzM4NTQzTGI0TmJoaW5QYXZKV3lpSjNWU1hYNlZTeWt5T1J5WnEzVzkKVW1PSTlLS09OUHJ0STUvRWJBdUFJWmx0bVRHbDdnTTdOWWZxTGprSmZXK3djRE9xSXVzeGpCUzlYWjJNUmQxcQp3RlFlMy9xVTNWTUM5MzQ0Z0tyZ21jZkF4RTdYMmJid2tjU2cwOEluM3g4V0NwdVREN1dFVHBMY1VvZVlrOUdTCnYvUTJJcHVGaWFVMjR5bkNFeWdsRnkrdVZldTVOazNON3duRVUzb05wU0tmcWEwWlZyNGdjQWtwU3J2OFpvdmIKdTdxU0lSYW40VExvdnlJSlkzQkZDNTAwTFIyTGVsN3g0U2JqTFFJREFRQUJNQTBHQ1NxR1NJYjNEUUVCQ3dVQQpBNElCQVFDdXEwaTdRVUR0a1RKQTB6NUwzdjNxUitFeklXNXZ4c0QzMGFnbVNGZHdaMDArTVpHQkdRdFJodTFNCklrdmh4OXByS1I4ZEp6NTVzMVIrYWlHUzdBWU9xSzUwaFA2OGp3c2tVMnozbkJIbXZ4TGUvZ3hpUUJONmVtei8KRDA5NDA1alJBcXpkTEhIb1RmdUk0Y3J0NUY0b2JzQUkzdlNJdVRpWE9XMStmeEMwaFdnWE5IY1R2LzJNU0h1dQorRW9FMVFiZEpHcWEzeXM0cWxMSXd2QTVia1JDR2JoNmNudG44QU4xbGk0Yk5VRW5wR1FtUE9IVVBwa1NieFVJCndva0NKWmRUWHJLRUpManBpQTRiMkFpbzNvOWpyTGNvOGIydGJSdDFZa1ZFTGt6NFhkam5XNjNkRmwveVVES1gKaHVxSC9zYy9abnZMMCtJcDc2R0E4R0VCM2JTegotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\"}"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    secretsmanager create-secret \
    --name "pn-national-registries/pdnd/CheckCF" \
    --secret-string "{
      \"client_id\":\"5df9f218-df34-4f05-a9c4-e4a74b2a8e3f\",
      \"jwtConfig\":{
        \"kid\":\"kid123\",
        \"iss\":\"0a455cfd-eb86-46bc-a386-96f1baf94438\",
        \"sub\":\"0a455cfd-eb86-46bc-a386-96f1baf94438\",
        \"aud\":\"auth.uat.interop.pagopa.it/client-assertion\",
        \"purposeId\":\"58708207-d9b5-4223-b362-3f6ad4256256\"},
      \"keyId\":"$keyID"
      }"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    secretsmanager create-secret \
    --name "pn-national-registries/pdnd/INAD" \
    --secret-string "{
      \"client_id\":\"5df9f218-df34-4f05-a9c4-e4a74b2a8e3f\",
      \"jwtConfig\":{
        \"kid\":\"kid123\",
        \"iss\":\"0a455cfd-eb86-46bc-a386-96f1baf94438\",
        \"sub\":\"0a455cfd-eb86-46bc-a386-96f1baf94438\",
        \"aud\":\"auth.uat.interop.pagopa.it/client-assertion\",
        \"purposeId\":\"58708207-d9b5-4223-b362-3f6ad4256256\"},
      \"keyId\":"$keyID"
      }"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    secretsmanager create-secret \
    --name "pn-national-registries/ipa" \
    --secret-string "{\"AUTH_ID\":\"secure_Password123\"}"


echo "### CREATE PARAMETER FOR NATIONAL-REGISTRIES ###"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
	ssm put-parameter \
	--name "/pn-national-registries/infocamere-cert" \
	--value "{\"keyId\":"$keyID",
	\"cert\":\"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURFVENDQWZrQ0ZDMkQ5THBvekRxdmN5YVM5RDlMendmREZKN0dNQTBHQ1NxR1NJYjNEUUVCQ3dVQU1FVXgKQ3pBSkJnTlZCQVlUQWtGVk1STXdFUVlEVlFRSURBcFRiMjFsTFZOMFlYUmxNU0V3SHdZRFZRUUtEQmhKYm5SbApjbTVsZENCWGFXUm5hWFJ6SUZCMGVTQk1kR1F3SGhjTk1qUXhNakE1TURnME56TXpXaGNOTWpVd01UQTRNRGcwCk56TXpXakJGTVFzd0NRWURWUVFHRXdKQlZURVRNQkVHQTFVRUNBd0tVMjl0WlMxVGRHRjBaVEVoTUI4R0ExVUUKQ2d3WVNXNTBaWEp1WlhRZ1YybGtaMmwwY3lCUWRIa2dUSFJrTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQwpBUThBTUlJQkNnS0NBUUVBOXJzNE5reHZ0ejBWVFQ0U3U2Wkhob21pK1ZSUkJUL0RBVms1THpvTGRSSXJzSHJQCjFJVlFvQ3JxYTM0blB2NDF1WGw3V2VkKzM4NTQzTGI0TmJoaW5QYXZKV3lpSjNWU1hYNlZTeWt5T1J5WnEzVzkKVW1PSTlLS09OUHJ0STUvRWJBdUFJWmx0bVRHbDdnTTdOWWZxTGprSmZXK3djRE9xSXVzeGpCUzlYWjJNUmQxcQp3RlFlMy9xVTNWTUM5MzQ0Z0tyZ21jZkF4RTdYMmJid2tjU2cwOEluM3g4V0NwdVREN1dFVHBMY1VvZVlrOUdTCnYvUTJJcHVGaWFVMjR5bkNFeWdsRnkrdVZldTVOazNON3duRVUzb05wU0tmcWEwWlZyNGdjQWtwU3J2OFpvdmIKdTdxU0lSYW40VExvdnlJSlkzQkZDNTAwTFIyTGVsN3g0U2JqTFFJREFRQUJNQTBHQ1NxR1NJYjNEUUVCQ3dVQQpBNElCQVFDdXEwaTdRVUR0a1RKQTB6NUwzdjNxUitFeklXNXZ4c0QzMGFnbVNGZHdaMDArTVpHQkdRdFJodTFNCklrdmh4OXByS1I4ZEp6NTVzMVIrYWlHUzdBWU9xSzUwaFA2OGp3c2tVMnozbkJIbXZ4TGUvZ3hpUUJONmVtei8KRDA5NDA1alJBcXpkTEhIb1RmdUk0Y3J0NUY0b2JzQUkzdlNJdVRpWE9XMStmeEMwaFdnWE5IY1R2LzJNU0h1dQorRW9FMVFiZEpHcWEzeXM0cWxMSXd2QTVia1JDR2JoNmNudG44QU4xbGk0Yk5VRW5wR1FtUE9IVVBwa1NieFVJCndva0NKWmRUWHJLRUpManBpQTRiMkFpbzNvOWpyTGNvOGIydGJSdDFZa1ZFTGt6NFhkam5XNjNkRmwveVVES1gKaHVxSC9zYy9abnZMMCtJcDc2R0E4R0VCM2JTegotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\"}
	\"dns\":\"fake.it\"}"\
	--type String \

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
	ssm put-parameter \
	--name "/pn-national-registries/ade-api-cert" \
	--value "{\"secretid\":\"pn-national-registries/ade-api-cert\",
	\"cert\":\"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURFVENDQWZrQ0ZDMkQ5THBvekRxdmN5YVM5RDlMendmREZKN0dNQTBHQ1NxR1NJYjNEUUVCQ3dVQU1FVXgKQ3pBSkJnTlZCQVlUQWtGVk1STXdFUVlEVlFRSURBcFRiMjFsTFZOMFlYUmxNU0V3SHdZRFZRUUtEQmhKYm5SbApjbTVsZENCWGFXUm5hWFJ6SUZCMGVTQk1kR1F3SGhjTk1qUXhNakE1TURnME56TXpXaGNOTWpVd01UQTRNRGcwCk56TXpXakJGTVFzd0NRWURWUVFHRXdKQlZURVRNQkVHQTFVRUNBd0tVMjl0WlMxVGRHRjBaVEVoTUI4R0ExVUUKQ2d3WVNXNTBaWEp1WlhRZ1YybGtaMmwwY3lCUWRIa2dUSFJrTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQwpBUThBTUlJQkNnS0NBUUVBOXJzNE5reHZ0ejBWVFQ0U3U2Wkhob21pK1ZSUkJUL0RBVms1THpvTGRSSXJzSHJQCjFJVlFvQ3JxYTM0blB2NDF1WGw3V2VkKzM4NTQzTGI0TmJoaW5QYXZKV3lpSjNWU1hYNlZTeWt5T1J5WnEzVzkKVW1PSTlLS09OUHJ0STUvRWJBdUFJWmx0bVRHbDdnTTdOWWZxTGprSmZXK3djRE9xSXVzeGpCUzlYWjJNUmQxcQp3RlFlMy9xVTNWTUM5MzQ0Z0tyZ21jZkF4RTdYMmJid2tjU2cwOEluM3g4V0NwdVREN1dFVHBMY1VvZVlrOUdTCnYvUTJJcHVGaWFVMjR5bkNFeWdsRnkrdVZldTVOazNON3duRVUzb05wU0tmcWEwWlZyNGdjQWtwU3J2OFpvdmIKdTdxU0lSYW40VExvdnlJSlkzQkZDNTAwTFIyTGVsN3g0U2JqTFFJREFRQUJNQTBHQ1NxR1NJYjNEUUVCQ3dVQQpBNElCQVFDdXEwaTdRVUR0a1RKQTB6NUwzdjNxUitFeklXNXZ4c0QzMGFnbVNGZHdaMDArTVpHQkdRdFJodTFNCklrdmh4OXByS1I4ZEp6NTVzMVIrYWlHUzdBWU9xSzUwaFA2OGp3c2tVMnozbkJIbXZ4TGUvZ3hpUUJONmVtei8KRDA5NDA1alJBcXpkTEhIb1RmdUk0Y3J0NUY0b2JzQUkzdlNJdVRpWE9XMStmeEMwaFdnWE5IY1R2LzJNU0h1dQorRW9FMVFiZEpHcWEzeXM0cWxMSXd2QTVia1JDR2JoNmNudG44QU4xbGk0Yk5VRW5wR1FtUE9IVVBwa1NieFVJCndva0NKWmRUWHJLRUpManBpQTRiMkFpbzNvOWpyTGNvOGIydGJSdDFZa1ZFTGt6NFhkam5XNjNkRmwveVVES1gKaHVxSC9zYy9abnZMMCtJcDc2R0E4R0VCM2JTegotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\"}
	\"dns\":\"fake.it\"}"\
	--type String \


echo "### CREATE QUEUES FOR NATIONAL REGISTRIES ###"

queues="pn-national_registry_gateway_inputs-DLQ pn-national_registry_gateway_inputs pn-national_registry_validation_gateway_inputs pn-national_registry_validation_gateway_inputs-DLQ pn-national_registry_gateway_outputs"
for qn in $(echo $queues | tr " " "\n");do
  echo creating queue $qn
  aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 sqs create-queue --queue-name $qn
done


echo "### CREATE PN-COUNTER TABLE ###"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-counter \
    --attribute-definitions \
        AttributeName=eservice,AttributeType=S \
    --key-schema \
        AttributeName=eservice,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=10,WriteCapacityUnits=5 \

echo "### CREATE BATCH REQUESTS TABLE ###"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-batchRequests \
    --attribute-definitions \
        AttributeName=correlationId,AttributeType=S \
        AttributeName=batchId,AttributeType=S \
        AttributeName=status,AttributeType=S \
        AttributeName=lastReserved,AttributeType=S \
        AttributeName=sendStatus,AttributeType=S \
    --key-schema \
        AttributeName=correlationId,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    '[
        {
            "IndexName": "sendStatus-index",
            "KeySchema": [
                {"AttributeName": "sendStatus", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"},
            "ProvisionedThroughput": {"ReadCapacityUnits": 5, "WriteCapacityUnits": 5}
        },
        {
            "IndexName": "batchId-lastReserved-index",
            "KeySchema": [
                {"AttributeName": "batchId", "KeyType": "HASH"},
                {"AttributeName": "lastReserved", "KeyType": "RANGE"}
            ],
            "Projection": {"ProjectionType": "ALL"},
            "ProvisionedThroughput": {"ReadCapacityUnits": 5, "WriteCapacityUnits": 5}
        },
        {
            "IndexName": "status-index",
            "KeySchema": [
                {"AttributeName": "status", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"},
            "ProvisionedThroughput": {"ReadCapacityUnits": 5, "WriteCapacityUnits": 5}
        },
        {
            "IndexName": "sendStatus-lastReserved-index",
            "KeySchema": [
                {"AttributeName": "sendStatus", "KeyType": "HASH"},
                {"AttributeName": "lastReserved", "KeyType": "RANGE"}
            ],
            "Projection": {"ProjectionType": "ALL"},
            "ProvisionedThroughput": {"ReadCapacityUnits": 5, "WriteCapacityUnits": 5}
        },
        {
            "IndexName": "lastReserved-index",
            "KeySchema": [
                {"AttributeName": "lastReserved", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"},
            "ProvisionedThroughput": {"ReadCapacityUnits": 5, "WriteCapacityUnits": 5}
        },
        {
            "IndexName": "batchId-index",
            "KeySchema": [
                {"AttributeName": "batchId", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"},
            "ProvisionedThroughput": {"ReadCapacityUnits": 5, "WriteCapacityUnits": 5}
        }
    ]'

echo "### CREATE POLLING REQUESTS TABLE ###"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-batchPolling \
    --attribute-definitions \
        AttributeName=batchId,AttributeType=S \
        AttributeName=status,AttributeType=S \
    --key-schema \
        AttributeName=batchId,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    '[
        {
            "IndexName": "status-index",
            "KeySchema": [
                {"AttributeName": "status", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"},
            "ProvisionedThroughput": {"ReadCapacityUnits": 5, "WriteCapacityUnits": 5}
        }
    ]'

echo "### CREATE GATEWAY REQUESTS TRACKER TABLE ###"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-gatewayRequestTracker \
    --attribute-definitions \
        AttributeName=correlationId,AttributeType=S \
    --key-schema \
        AttributeName=correlationId,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \

echo "Initialization terminated"