# Configurazione client

In questa guida vengono riportati i passi da seguire per la corretta configurazione
dei client per quanto riguarda la parte di autenticazione.

Questa guida non è da considerarsi esaustiva, fare sempre riferimento alla documentazione
ufficiale.

## ANPR

* Generazione KeyPair da CF su KMS (vedi _storage.yml_ - PDNDSessionTokenSigningKey)
* Prelevare la chiave pubblica da KMS
* Creare il client su PDND fornendo la chiave pubblica
* Creare manualmente un secret su SecretManager (nome di esempio: _pn-national-registries/pdnd/ANPR_)
* Popolare il secret con le informazioni ottenute da PDND; struttura di base:
```json
{
  "client_id": "",
  "jwtConfig": {
    "kid": "",
    "iss": "",
    "sub": "",
    "aud": "",
    "purposeId": ""
  },
  "keyId": "<identificativo della chiave su KMS>"
}
```
* Valorizzare nel file _microservice-xxx-cfg.json_ le variabili:
    * __PDNDANPRSecret__ con nome del secret creato
    * __ANPRPurposeId__ con il purposeId ottenuto da PDND
* Creare manualmente un secret su SecretManager (nome di esempio: _pn-national-registries/anpr/auth-rest_)
* Generare/ottenere un certificato per mTLS
* Popolare il secret con certificato, chiave pubblica e chiave privata codificati in base64; struttura di base:
```json
{
  "cert": "",
  "key": "",
  "pub": "",
  "trust": "<opzionale, utilizzato per il trust in caso ANPR usi un certificato 'insicuro'>"
}
```
* Valorizzare nel file _microservice-xxx-cfg.json_ la variabile:
    * __ANPRSSLSecret__ con nome del secret creato
* Creare manualmente un secret su SecretManager (nome di esempio: _pn-national-registries/anpr/integrity-rest_)
* Generare/ottenere un certificato per _integrity_
* Popolare il secret con certificato, chiave pubblica e chiave privata codificati in base64; struttura di base:
```json
{
  "cert": "",
  "key": "",
  "pub": ""
}
```
* Valorizzare nel file _microservice-xxx-cfg.json_ la variabile:
    * __ANPRIntegritySecret__ con nome del secret creato

Necessaria la comunicazione con ANPR per fornire loro chiavi pubbliche e certificati per mTLS e _integrity_.

## INAD

Il procedimento descritto per ANPR sulla generazione delle chiavi per il client PDND
vale anche per INAD:
* Nel file _storage.yml_ fare riferimento a InadPDNDSessionTokenSigningKey
* Nome di esempio per il secret: _pn-national-registries/pdnd/INAD_
* Variabili da valorizzare in _microservice-xxx-cfg.json_:
    * __INADPurposeId__ con purposeId ottenuto da PDND
    * __PDNDInadSecret__ con nome del secret creato

__NB__: rispetto ad ANPR, non si applica tutta la parte su mTLS e _integrity_.

## AdE Check CF

Vale la stessa procedura descritta per ANPR (eccezion fatta per l'_integrity_):
* Nel file _storage.yml_ fare riferimento a CheckCfPDNDSessionTokenSigningKey
* Nome di esempio per il secret di PDND: _pn-national-registries/pdnd/CheckCF_
* Nome di esempio per il secret per mTLS: _pn-national-registries/CheckCF/auth-rest_
* Variabili da valorizzare in _microservice-xxx-cfg.json_:
    * __AdECheckCfPurposeId__ con purposeId ottenuto da PDND
    * __PDNDAdECheckCfSecret__ con nome del secret per PDND
    * __AdESSLSecret__ con nome del secret per mTLS

Necessaria la comunicazione con AdE per fornire loro chiave pubblica e certificato per mTLS.

## AdE Check Legale Rappresentante

TODO

## InfoCamere

L'autenticazione presso InfoCamere può considerarsi per certi aspetti simile a quella
che avviene presso PDND. Viene costruito un JWS con (riportate solo alcune informazioni
salienti del payload):
* __aud__ corrisponde al base URL
* __iss__ e __sub__ valorizzati con un clientId fornito da InfoCamere
* __scope__ varia in base al servizio che si desidera invocare (possibili valori forniti da InfoCamere)

Il token JWS viene firmato e viene utilizzato per l'autenticazione.

I passi da seguire sono:
* Generare/ottenere un certificato per la firma del token
* Creare manualmente un secret su SecretManager
* Popolare il secret con certificato, chiave pubblica e chiave privata codificati in base64; struttura di base:
```json
{
  "cert": "",
  "key": "",
  "pub": ""
}
```
* Valorizzare nel file _microservice-xxx-cfg.json_ le variabili:
    * __InfoCamereAuthSecret__ con nome del secret creato
    * __InfoCamereClientId__ con clientId fornito da InfoCamere

Necessaria la comunicazione con InfoCaemere per fornire loro chiave pubblica e certificato.
