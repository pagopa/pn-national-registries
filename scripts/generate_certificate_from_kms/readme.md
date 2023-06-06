# install certbot with route53 plugin
pip install certbot-dns-route53

# generate private key with csr (interactive)
openssl req -newkey rsa:2048 -keyout PRIVATEKEY.key -out MYCSR.csr

Enter PEM pass phrase:
Verifying - Enter PEM pass phrase:
-----
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) []:IT
State or Province Name (full name) []:Italy
Locality Name (eg, city) []:Rome
Organization Name (eg, company) []:PagoPA
Organizational Unit Name (eg, section) []:SEND
Common Name (eg, fully qualified host name) []:cert.dev.notifichedigitali.it 
Email Address []:marco.iannaccone@pagopa.it

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:prova


openssl req -newkey rsa:2048 -keyout PRIVATEKEY.key -out MYCSR.csr -subj /C=IT/ST=Italy/L=Rome/O=PagoPA/OU=SEND/CN=${FQDN} -passout pass:${PASSPHRASE}
# email missing


# see the certificate request:
openssl req -noout -text -in MYCSR.csr
