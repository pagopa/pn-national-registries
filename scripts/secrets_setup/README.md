# ISTRUZIONI

Lo script va eseguito nell'ambito del riallineamento degli alias e della creazione dei certificati, una volta eliminate le chiavi KMS e gli Alias dal template `storage.yaml`.

Le chiavi KMS hanno la retention impostata quindi rimarrano, gli Alias verranno eliminati quindi questo script permette di:
- ricollegare l'alias alla chiave KMS esistente (l'alias viene creato se non esiste)
- creare la chiave KMS se non esiste e collegare l'alias

`./setup.sh --region <region> --profile <profile-aws>`