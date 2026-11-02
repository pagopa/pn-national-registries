# pn-national-registries - Test cases summary E2E

Questo documento riepiloga **solo gli scenari presenti nella cartella `integration/`**

La struttura segue le configurazioni C1-C8, separando PF/PG, fisico/digitale e i principali scenari funzionali, 
di resilienza e recovery effettivamente coperti.

## 1. Configurazioni

| Configurazione | INAD massive | PF fallback | Old IniPEC | Flusso principale                            |
|----------------|-------------:|------------:|-----------:|----------------------------------------------|
| **C1**         |        false |       false |      false | INAD sync, fallback PF OFF, nuovo IniPEC     |
| **C2**         |        false |       false |       true | INAD sync, fallback PF OFF, legacy IniPEC    |
| **C3**         |        false |        true |      false | INAD sync, fallback PF ON, nuovo IniPEC      |
| **C4**         |        false |        true |       true | INAD sync, fallback PF ON, legacy IniPEC     |
| **C5**         |         true |       false |      false | INAD massive, fallback PF OFF, nuovo IniPEC  |
| **C6**         |         true |       false |       true | INAD massive, fallback PF OFF, legacy IniPEC |
| **C7**         |         true |        true |      false | INAD massive, fallback PF ON, nuovo IniPEC   |
| **C8**         |         true |        true |       true | INAD massive, fallback PF ON, legacy IniPEC  |

Gli scenari fisici non dipendono dai tre flag digitali.

## 2. PF - Persona fisica

### 2.1 PF fisico - valido per C1-C8

| Scenario E2E                          | Esito atteso                                          | Test / classe                     |
|---------------------------------------|-------------------------------------------------------|-----------------------------------|
| ANPR diretto vs Gateway               | Stessa fixture produce lo stesso indirizzo funzionale | `PhysicalAddressTestIT`           |
| Residenza valida ANPR                 | Risultato `ANPR` con indirizzo fisico                 | `PhysicalAddressTestIT`           |
| Tutti i dettagli indirizzo            | Mapping completo                                      | `PhysicalAddressTestIT`           |
| Dettagli parziali                     | Campi opzionali coerenti                              | `PhysicalAddressTestIT`           |
| Caratteri accentati                   | Valori preservati                                     | `PhysicalAddressTestIT`           |
| Civico non standard                   | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| Denominazione stradale molto lunga    | Gestione coerente                                     | `PhysicalAddressTestIT`           |
| ANPR NotFound                         | Risultato funzionale senza indirizzo                  | `PhysicalAddressTestIT`           |
| Errore tecnico ANPR                   | Errore propagato                                      | `PhysicalAddressTestIT`           |
| Residenza estera                      | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| Più soggetti in risposta              | Selezione del soggetto con CF richiesto               | `PhysicalAddressTestIT`           |
| Nessuna residenza utile               | Nessun indirizzo fisico valido                        | `PhysicalAddressTestIT`           |
| Civico metrico                        | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| Numero + lettera civico               | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| SNC                                   | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| Colore / scala                        | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| Solo strada e numero                  | Composizione minima coerente                          | `PhysicalAddressTestIT`           |
| Struttura indirizzo complessa         | Mapping completo                                      | `PhysicalAddressTestIT`           |
| Specie/denominazione numerica         | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| CAP internazionale                    | Valore preservato                                     | `PhysicalAddressTestIT`           |
| Contatore ANPR / campi richiesta      | Mapping e incremento corretti                         | `PhysicalAddressTestIT`           |
| Batch physical PF/PG misto            | Elementi indipendenti e correlati all'input           | `PhysicalAddressTestIT`           |
| Lista physical vuota                  | Bad request                                           | `PhysicalAddressTestIT`           |
| ANPR NotFound su un elemento batch    | Gli altri elementi non falliscono                     | `PhysicalAddressTestIT`           |
| Too many requests su singolo elemento | Errore confinato all'elemento                         | `PhysicalAddressTestIT`           |
| Errore generico su singolo elemento   | Errore confinato all'elemento                         | `PhysicalAddressTestIT`           |
| Modalità `FULL`                       | Composizione completa                                 | `PhysicalAddressFullModeIT`       |
| `FULL` oltre limite                   | Rispetto del limite massimo                           | `PhysicalAddressFullModeIT`       |
| Modalità `OLD`                        | Composizione legacy                                   | `PhysicalAddressOldModeIT`        |
| Modalità `OLD_MINIMAL`                | Composizione old-minimal                              | `PhysicalAddressOldMinimalModeIT` |
| Modalità sconosciuta                  | Fallback funzionale a `OLD`                           | `PhysicalAddressInvalidModeIT`    |

### 2.2 PG fisico - valido per C1-C8

| Scenario E2E                    | Esito atteso                     | Test / classe           |
|---------------------------------|----------------------------------|-------------------------|
| Sede legale valida              | `REGISTRO_IMPRESE` con indirizzo | `PhysicalAddressTestIT` |
| Tutti i campi                   | Mapping completo                 | `PhysicalAddressTestIT` |
| Paese estero                    | Mapping coerente                 | `PhysicalAddressTestIT` |
| Caratteri accentati             | Mapping preservato               | `PhysicalAddressTestIT` |
| Sede secondaria                 | Mapping coerente                 | `PhysicalAddressTestIT` |
| Nessuna sede legale             | Elemento senza `physicalAddress` | `PhysicalAddressTestIT` |
| Errore tecnico Registro Imprese | Errore propagato                 | `PhysicalAddressTestIT` |
| Batch PF/PG misto               | Elementi indipendenti            | `PhysicalAddressTestIT` |

---

## 3. PF digitale

### 3.1 C1 - INAD sync, fallback OFF, nuovo IniPEC

**Classe principale:** `DigitalAddressPFNoFallbackTestIT`

| Scenario E2E                                       | Esito atteso                                |
|----------------------------------------------------|---------------------------------------------|
| PEC personale valida                               | Risultato finale INAD                       |
| INAD NotFound                                      | Risultato vuoto, nessun fallback            |
| Lista INAD vuota                                   | Risultato vuoto                             |
| PEC personale scaduta                              | Risultato vuoto                             |
| Personale senza `usageInfo`                        | PEC considerata valida                      |
| Personale senza `dateEndValidity`                  | PEC considerata valida                      |
| Prima personale non valida/scaduta, seconda valida | Selezionata la prima PEC valida disponibile |
| Personale + professionale valide                   | Prevale la personale                        |
| Solo professionale valida                          | Risultato vuoto                             |
| Personale invalida + professionale valida          | Risultato vuoto                             |
| Più professionali senza personale                  | Risultato vuoto                             |
| Errore tecnico INAD                                | Errore propagato                            |
| Personale scaduta + professionale valida           | Risultato vuoto                             |
| Sola professionale scaduta                         | Risultato vuoto                             |

### 3.2 C2 - INAD sync, fallback OFF, legacy IniPEC

Con fallback PF disabilitato, il flag old/new IniPEC non viene attraversato dal flusso PF: gli scenari funzionali 
restano quelli di `DigitalAddressPFNoFallbackTestIT` e un INAD non risolutivo termina senza creare fallback IniPEC.

### 3.3 C3 - INAD sync, fallback ON, nuovo IniPEC

**Classe:** `DigitalAddressPFFallbackNewInipecTestIT`

| Scenario E2E                                       | Esito atteso                                  |
|----------------------------------------------------|-----------------------------------------------|
| Personale valida                                   | Risultato INAD, nessun fallback               |
| INAD NotFound / lista vuota / personale scaduta    | Avvio fallback nuovo IniPEC                   |
| Personale senza `usageInfo` / `dateEndValidity`    | PEC considerata valida                        |
| Prima personale invalida, seconda valida           | Selezione della PEC valida                    |
| Personale + professionale                          | Prevale la personale                          |
| Solo professionale valida                          | Professionale utilizzata senza avviare IniPEC |
| Personale invalida/scaduta + professionale valida  | Professionale utilizzata                      |
| Più professionali valide                           | Gestione coerente senza fallback improprio    |
| Errore tecnico INAD                                | Nessun fallback improprio verso IniPEC        |
| IniPEC con sola PEC professionale                  | Risultato finale IniPEC                       |
| IniPEC con impresa + professionisti                | Tutti gli indirizzi validi mantenuti          |
| IniPEC con PEC invalida                            | Risultato finale vuoto                        |
| IniPEC `ND` / `NF`                                 | Risultato finale vuoto                        |
| IniPEC senza CF richiesto                          | Risultato finale vuoto                        |
| IniPEC `ER`                                        | Retry fino a `ERROR` al limite                |
| IniPEC polling in-progress                         | Retry in-progress                             |
| IniPEC metadata errore                             | Retry polling                                 |
| Più PEC professionali IniPEC                       | Tutte preservate                              |
| PEC impresa IniPEC invalida + professionale valida | Professionale preservata                      |

### 3.4 C4 - INAD sync, fallback ON, legacy IniPEC

**Classe:** `DigitalAddressPFFallbackOldInipecTestIT`

Copre gli stessi scenari INAD funzionali della configurazione C3, ma il fallback viene eseguito tramite **legacy IniPEC**. 
In aggiunta sono coperti i comportamenti specifici del flusso legacy:

| Scenario E2E                                        | Esito atteso                           |
|-----------------------------------------------------|----------------------------------------|
| INAD NotFound / lista vuota / personale scaduta     | Avvio legacy IniPEC                    |
| Legacy IniPEC professionale                         | Risultato finale IniPEC                |
| Legacy impresa + professionisti                     | Tutti gli indirizzi validi mantenuti   |
| Legacy PEC invalida / `ND` / `NF` / CF assente      | Risultato finale vuoto                 |
| Legacy `ER`                                         | Retry fino al limite configurato       |
| Legacy polling in-progress                          | Retry coerente                         |
| Legacy polling metadata errore                      | Retry coerente                         |
| Submit legacy: errore a soglia                      | Request finale `ERROR`                 |
| Submit legacy: `BAD_REQUEST`                        | Errore immediato                       |
| Polling legacy: error retry exhaustion              | Polling e request finali `ERROR`       |
| Polling legacy: in-progress retry exhaustion        | Polling e request finali `ERROR`       |
| Polling legacy: `BAD_REQUEST`                       | Errore immediato                       |
| Output legacy consumato                             | `sendStatus=SENT`                      |
| Output inviato ma persistenza `sendStatus` fallisce | Recovery completa lo stato di delivery |
| Più professionali legacy                            | Tutte preservate                       |
| Impresa legacy invalida + professionale valida      | Professionale preservata               |

### 3.5 C5 - INAD massive, fallback OFF, nuovo IniPEC

**Classe:** `DigitalAddressPFMassiveNoFallbackIT`

| Scenario E2E                                        | Esito atteso                                                        |
|-----------------------------------------------------|---------------------------------------------------------------------|
| PEC personale valida                                | Risultato finale INAD                                               |
| NotFound / lista vuota                              | Risultato vuoto                                                     |
| Personale scaduta                                   | Risultato vuoto                                                     |
| Personale senza `usageInfo` / `dateEndValidity`     | PEC valida                                                          |
| Prima personale invalida, seconda valida            | Selezione della PEC valida                                          |
| Personale + professionale                           | Prevale personale                                                   |
| Solo professionale / più professionali              | Risultato vuoto                                                     |
| Personale invalida/scaduta + professionale valida   | Risultato vuoto                                                     |
| Sola professionale scaduta                          | Risultato vuoto                                                     |
| Errore tecnico risultato INAD massive               | Retry del batch                                                     |
| CF duplicato nella risposta                         | Primo risultato provider mantenuto                                  |
| Provider accetta batch, persistenza locale fallisce | Restart ricrea il batch per le request rimaste recuperabili         |
| Aggiornamento request parziale                      | Nuovo batch prende solo le request `NOT_WORKED`                     |
| Due worker sullo stesso batch                       | Una sola reservation effettiva                                      |
| Persistenza parziale del risultato                  | Replay completa solo la request rimasta `WORKING`                   |
| Error retry exhaustion su cicli reali               | Batch/request finali `ERROR`                                        |
| Paginazione/correlazione massive                    | Matching stabile con ordine diverso, duplicati e risultati mancanti |

### 3.6 C6 - INAD massive, fallback OFF, legacy IniPEC

Con fallback PF disabilitato, il legacy IniPEC non viene attraversato. Gli scenari funzionali 
sono quelli di `DigitalAddressPFMassiveNoFallbackIT`; un esito INAD massive non risolutivo termina senza creare fallback legacy.

### 3.7 C7 - INAD massive, fallback ON, nuovo IniPEC

**Classe:** `DigitalAddressPFMassiveFallbackNewInipecIT`

La classe combina gli scenari funzionali INAD massive della C5 con il fallback nuovo IniPEC 
e aggiunge copertura full-flow e recovery della transizione INAD -> IniPEC.

| Scenario E2E                                            | Esito atteso                                        |
|---------------------------------------------------------|-----------------------------------------------------|
| INAD massive personale valida                           | Risultato INAD, nessun fallback                     |
| INAD massive NotFound / lista vuota / personale scaduta | Avvio nuovo IniPEC                                  |
| Professionale valida disponibile                        | Selezionata prima di creare fallback IniPEC         |
| Full-flow INAD massive -> IniPEC professionale          | Risultato finale IniPEC                             |
| Full-flow -> impresa + professionisti                   | Tutti gli indirizzi validi mantenuti                |
| IniPEC invalida / `ND` / `NF` / CF assente              | Risultato finale vuoto                              |
| IniPEC `ER`                                             | Retry fino a errore al limite                       |
| IniPEC polling in-progress / metadata errore            | Retry coerente                                      |
| Duplicate tax-id INAD                                   | Primo risultato provider mantenuto                  |
| Recovery batch INAD dopo persistenza fallita            | Request recuperate senza perdere correlazione       |
| Aggiornamento parziale request                          | Solo request rimaste `NOT_WORKED` nel nuovo batch   |
| Concorrenza polling                                     | Una sola reservation                                |
| Replay processing parziale                              | Solo request ancora `WORKING` completate            |
| Error retry exhaustion                                  | Batch/request `ERROR`                               |
| Paginazione/correlazione                                | Matching stabile                                    |
| Batch INAD lavorato ma chiusura batch fallita           | Replay chiude il batch senza duplicare fallback     |
| Processing INAD interrotto dopo parte delle request     | Replay processa solo request ancora `WORKING`       |
| Identità fallback                                       | `correlationId` e `referenceRequestDate` preservati |

### 3.8 C8 - INAD massive, fallback ON, legacy IniPEC

**Classe:** `DigitalAddressPFMassiveFallbackOldInipecIT`

La classe combina gli scenari INAD massive della C5 con il fallback legacy e include anche i casi specifici del legacy IniPEC.

| Scenario E2E                                            | Esito atteso                                           |
|---------------------------------------------------------|--------------------------------------------------------|
| INAD massive NotFound / lista vuota / personale scaduta | Avvio legacy IniPEC                                    |
| Full-flow -> legacy IniPEC                              | Risultato finale coerente                              |
| Legacy professionale / impresa + professionisti         | Indirizzi validi preservati                            |
| Legacy invalida / `ND` / `NF` / CF assente              | Risultato finale vuoto                                 |
| Legacy `ER`, in-progress, metadata errore               | Retry coerenti                                         |
| Duplicate tax-id INAD                                   | Primo risultato provider mantenuto                     |
| Recovery batch / request INAD                           | Stato coerente dopo restart                            |
| Concorrenza polling                                     | Una sola reservation                                   |
| Replay processing parziale                              | Solo request ancora `WORKING` completate               |
| Error retry exhaustion INAD                             | Batch/request `ERROR`                                  |
| Paginazione/correlazione                                | Matching stabile                                       |
| Batch INAD lavorato ma chiusura fallita                 | Replay chiude il batch senza duplicare fallback legacy |
| Processing INAD parziale                                | Replay processa solo request ancora `WORKING`          |
| Submit legacy error / bad request                       | Retry o errore immediato secondo il caso               |
| Polling legacy error / in-progress exhaustion           | Polling e request `ERROR`                              |
| Polling legacy bad request                              | Errore immediato                                       |
| Output legacy consumato                                 | `sendStatus=SENT`                                      |
| Persistenza `sendStatus` fallita                        | Recovery completa la delivery                          |

---

## 4. PG - Persona giuridica digitale

Per PG la catena è sempre **IPA -> IniPEC -> INAD**. 

### 4.1 C1 / C3 - INAD sync, nuovo IniPEC

**Classe:** `DigitalAddressPGTestIT`

La classe è eseguita con `pfFallback=true`; il test dedicato verifica esplicitamente che il flag PF non modifichi il comportamento PG. 
Funzionalmente copre quindi gli scenari PG sync con nuovo IniPEC.

| Scenario E2E                                | Esito atteso                                     |
|---------------------------------------------|--------------------------------------------------|
| IPA diretto vs Gateway                      | Stesso indirizzo funzionale                      |
| IPA PEC valida                              | Risultato finale IPA                             |
| IPA con più risultati                       | Uso WS05 e risultato IPA                         |
| IPA NotFound -> IniPEC found                | Risultato finale IniPEC                          |
| IPA PEC invalida -> IniPEC found            | Prosecuzione su IniPEC                           |
| Errore tecnico IPA                          | Errore propagato, nessun proseguimento improprio |
| IniPEC solo professionale                   | Risultato IniPEC professionale                   |
| IniPEC impresa + professionisti             | Tutti gli indirizzi validi mantenuti             |
| IniPEC PEC invalida                         | Prosecuzione a INAD sync                         |
| IniPEC `ND` / `NF`                          | Prosecuzione a INAD sync                         |
| IniPEC senza CF richiesto                   | Prosecuzione a INAD sync                         |
| IniPEC `ER`                                 | Retry fino a `ERROR`, senza creare INAD          |
| IniPEC polling in-progress                  | Retry in-progress                                |
| IniPEC metadata errore                      | Retry polling                                    |
| Più PEC professionali IniPEC                | Tutte preservate                                 |
| PEC impresa invalida + professionale valida | Professionale preservata                         |
| PF fallback attivo                          | Comportamento PG invariato                       |

### 4.2 C2 / C4 - INAD sync, legacy IniPEC

**Classe:** `DigitalAddressPGOldInipecTestIT`

| Scenario E2E                                   | Esito atteso                                           |
|------------------------------------------------|--------------------------------------------------------|
| IPA diretto vs Gateway                         | Stesso indirizzo funzionale                            |
| IPA PEC valida                                 | Risultato finale IPA                                   |
| IPA con più risultati                          | Uso WS05                                               |
| IPA NotFound -> old IniPEC found               | Risultato finale IniPEC                                |
| IPA PEC invalida -> old IniPEC found           | Prosecuzione su legacy IniPEC                          |
| Errore tecnico IPA                             | Nessun proseguimento                                   |
| Legacy IniPEC professionale                    | Risultato professionale                                |
| Legacy impresa + professionisti                | Tutti gli indirizzi validi mantenuti                   |
| Legacy IniPEC PEC invalida                     | Prosecuzione a INAD sync                               |
| Legacy `ND` / `NF`                             | Prosecuzione a INAD sync                               |
| Legacy senza CF richiesto                      | Prosecuzione a INAD sync                               |
| Più PEC professionali legacy                   | Tutte preservate                                       |
| Impresa legacy invalida + professionale valida | Professionale preservata                               |
| PF fallback attivo                             | Comportamento PG legacy invariato                      |
| Legacy NF -> INAD NotFound                     | Risultato finale INAD vuoto                            |
| Legacy NF -> errore tecnico INAD               | Request legacy finale `ERROR`, nessun messaggio finale |

### 4.3 C5 / C7 - INAD massive, nuovo IniPEC

**Classe:** `DigitalAddressPGMassiveIT`

| Scenario E2E                                        | Esito atteso                                                        |
|-----------------------------------------------------|---------------------------------------------------------------------|
| IPA diretto vs Gateway                              | Stesso indirizzo funzionale                                         |
| IPA found / multiple / technical error              | Comportamento coerente con flusso sync                              |
| IPA NotFound / PEC invalida -> IniPEC               | Prosecuzione su nuovo IniPEC                                        |
| IniPEC professionale                                | Risultato finale IniPEC                                             |
| IniPEC impresa + professionisti                     | Tutti gli indirizzi validi mantenuti                                |
| IniPEC `ER`                                         | Retry fino a `ERROR`, senza creare INAD                             |
| IniPEC polling in-progress / metadata errore        | Retry coerente                                                      |
| IniPEC PEC invalida / `ND` / `NF` / CF assente      | Creazione request `INAD#NOT_WORKED`                                 |
| INAD massive PEC impresa valida                     | Risultato finale INAD                                               |
| INAD massive NotFound / lista vuota                 | Risultato finale vuoto; INAD terminale                              |
| PEC impresa scaduta                                 | Risultato vuoto                                                     |
| Impresa senza `usageInfo` / `dateEndValidity`       | PEC valida                                                          |
| Prima impresa scaduta, seconda valida               | Selezionata la valida                                               |
| Impresa + professionale                             | Prevale l'indirizzo impresa                                         |
| Solo professionale                                  | Risultato vuoto                                                     |
| Impresa invalida + professionale valida             | Risultato vuoto                                                     |
| Più professionali                                   | Risultato vuoto                                                     |
| Impresa scaduta + professionale valida              | Risultato vuoto                                                     |
| Sola professionale scaduta                          | Risultato vuoto                                                     |
| Errore tecnico risultato INAD                       | Retry normale                                                       |
| Duplicate tax-id                                    | Primo risultato provider mantenuto                                  |
| Provider accetta batch, persistenza locale fallisce | Recovery delle request                                              |
| Aggiornamento request parziale                      | Nuovo batch solo per request `NOT_WORKED`                           |
| Due worker sullo stesso batch                       | Una sola reservation                                                |
| Replay processing parziale                          | Completa solo request ancora `WORKING`                              |
| Error retry exhaustion                              | Batch/request `ERROR`, INAD terminale                               |
| In-progress retry exhaustion                        | Batch/request `ERROR`, `INAD#ERROR`                                 |
| Paginazione/correlazione                            | Matching stabile con duplicati, ordine diverso e risultato mancante |

### 4.4 C6 / C8 - INAD massive, legacy IniPEC

**Classe:** `DigitalAddressPGOldInipecMassiveIT`

| Scenario E2E                                    | Esito atteso                              |
|-------------------------------------------------|-------------------------------------------|
| IPA diretto vs Gateway                          | Stesso indirizzo funzionale               |
| IPA found / multiple / technical error          | Comportamento coerente                    |
| IPA NotFound / PEC invalida -> old IniPEC       | Prosecuzione sul legacy                   |
| Legacy IniPEC professionale                     | Risultato finale IniPEC                   |
| Legacy impresa + professionisti                 | Tutti gli indirizzi validi mantenuti      |
| Più professionali legacy                        | Tutte preservate                          |
| Impresa legacy invalida + professionale valida  | Professionale preservata                  |
| Legacy PEC invalida / `ND` / `NF` / CF assente  | Creazione request `INAD#NOT_WORKED`       |
| PF fallback attivo                              | Flusso PG legacy invariato                |
| INAD massive PEC impresa valida                 | Risultato finale INAD                     |
| INAD NotFound / lista vuota                     | Risultato finale vuoto; INAD terminale    |
| PEC impresa scaduta                             | Risultato vuoto                           |
| Impresa senza `usageInfo` / `dateEndValidity`   | PEC valida                                |
| Prima impresa scaduta, seconda valida           | Selezione della valida                    |
| Impresa + professionale                         | Prevale impresa                           |
| Solo professionale / più professionali          | Risultato vuoto                           |
| Impresa invalida/scaduta + professionale valida | Risultato vuoto                           |
| Sola professionale scaduta                      | Risultato vuoto                           |
| Errore tecnico risultato INAD                   | Retry normale                             |
| Duplicate tax-id                                | Primo risultato provider mantenuto        |
| Recovery batch dopo persistenza fallita         | Request recuperate                        |
| Aggiornamento request parziale                  | Nuovo batch solo per request `NOT_WORKED` |
| Concorrenza polling                             | Una sola reservation                      |
| Replay processing parziale                      | Completa solo request ancora `WORKING`    |
| Error retry exhaustion                          | Batch/request finali `ERROR`              |
| Paginazione/correlazione                        | Matching stabile                          |

---

## 5. Flussi digitali condivisi coperti E2E

| Scenario E2E                    | Esito atteso                                   | Classi principali                                                                                                                                                                     |
|---------------------------------|------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| INAD sync                       | Risultato finale o errore coerente             | `DigitalAddressPFNoFallbackTestIT`, `DigitalAddressPFFallbackNewInipecTestIT`, `DigitalAddressPFFallbackOldInipecTestIT`, `DigitalAddressPGTestIT`, `DigitalAddressPGOldInipecTestIT` |
| INAD massive: batch + polling   | Request finale `WORKED` / `ERROR`              | tutte le classi `*Massive*IT`                                                                                                                                                         |
| Nuovo IniPEC: submit + polling  | Request finale `WORKED` / `ERROR`              | `DigitalAddressPFFallbackNewInipecTestIT`, `DigitalAddressPFMassiveFallbackNewInipecIT`, `DigitalAddressPGTestIT`, `DigitalAddressPGMassiveIT`                                        |
| Legacy IniPEC: submit + polling | Request/batch legacy finale coerente           | `DigitalAddressPFFallbackOldInipecTestIT`, `DigitalAddressPFMassiveFallbackOldInipecIT`, `DigitalAddressPGOldInipecTestIT`, `DigitalAddressPGOldInipecMassiveIT`                      |
| Retry polling normale           | Nuovo tentativo con contatori/stato aggiornati | classi PF/PG massive e IniPEC                                                                                                                                                         |
| Retry polling in-progress       | Contatore dedicato e retry / errore a soglia   | classi PF/PG massive e IniPEC                                                                                                                                                         |
| Retry request IniPEC `ER`       | `NOT_WORKED` fino alla soglia, poi `ERROR`     | classi PF/PG IniPEC                                                                                                                                                                   |
| Risultato finale persistito     | `resultPayload` coerente con il flusso         | full-flow nuovi e massive                                                                                                                                                             |
| INAD terminale per PG           | Nessun registro successivo a INAD              | `DigitalAddressPGTestIT`, `DigitalAddressPGOldInipecTestIT`, `DigitalAddressPGMassiveIT`, `DigitalAddressPGOldInipecMassiveIT`                                                        |

### 5.1 Resilienza, replay, recovery e concorrenza

| Scenario E2E                                                | Esito atteso                                                        | Test / classe                                                                           |
|-------------------------------------------------------------|---------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| INAD batch accettato, persistenza batch fallita             | Recovery senza perdere le request                                   | PF/PG massive                                                                           |
| Aggiornamento request parziale durante preparazione batch   | Solo request rimaste `NOT_WORKED` vengono riprese                   | PF/PG massive                                                                           |
| Due worker competono sullo stesso batch                     | Una sola reservation effettiva                                      | PF/PG massive                                                                           |
| Processing risultato parziale                               | Replay completa solo gli elementi ancora `WORKING`                  | PF/PG massive                                                                           |
| Error retry exhaustion su cicli reali                       | Contatori persistiti e batch/request finali `ERROR`                 | PF/PG massive                                                                           |
| In-progress retry exhaustion PG massive nuovo IniPEC        | Batch/request finali `ERROR`, `INAD#ERROR`                          | `DigitalAddressPGMassiveIT`                                                             |
| Paginazione/correlazione massive                            | Matching stabile con duplicati, ordine diverso e risultati mancanti | PF/PG massive                                                                           |
| INAD -> nuovo IniPEC: batch già lavorato, chiusura fallita  | Replay chiude solo il batch                                         | `DigitalAddressPFMassiveFallbackNewInipecIT`                                            |
| INAD -> nuovo IniPEC: processing parziale                   | Replay solo sulle request ancora `WORKING`                          | `DigitalAddressPFMassiveFallbackNewInipecIT`                                            |
| INAD -> nuovo IniPEC: identità richiesta                    | `correlationId` e `referenceRequestDate` preservati                 | `DigitalAddressPFMassiveFallbackNewInipecIT`                                            |
| INAD -> legacy IniPEC: batch già lavorato, chiusura fallita | Replay chiude solo il batch                                         | `DigitalAddressPFMassiveFallbackOldInipecIT`                                            |
| INAD -> legacy IniPEC: processing parziale                  | Replay solo sulle request ancora `WORKING`                          | `DigitalAddressPFMassiveFallbackOldInipecIT`                                            |
| Legacy output inviato ma persistenza `sendStatus` fallisce  | Recovery completa la delivery                                       | `DigitalAddressPFFallbackOldInipecTestIT`, `DigitalAddressPFMassiveFallbackOldInipecIT` |

---

## 6. Endpoint sincroni coperti

### 6.1 Agenzia Entrate

**Classe:** `AgenziaEntrateSyncEndpointsIT`

| Scenario E2E                       | Esito atteso                               |
|------------------------------------|--------------------------------------------|
| Tax-id valido                      | Check CF positivo                          |
| Tax-id inesistente                 | Esito negativo con errore provider mappato |
| Rappresentante legale matching     | Esito positivo                             |
| Rappresentante legale non matching | Esito negativo                             |
| XML malformato                     | `RuntimeJAXBException`                     |

### 6.2 InfoCamere / Registro Imprese

**Classe:** `InfoCamereSyncEndpointsIT`

| Scenario E2E                       | Esito atteso                |
|------------------------------------|-----------------------------|
| Indirizzo sede legale              | Indirizzo restituito        |
| Errore applicativo indirizzo       | Request senza indirizzo     |
| Rappresentante legale matching     | `true`                      |
| Rappresentante legale non matching | `false`                     |
| Errore applicativo rappresentante  | Esito negativo              |
| Legal institutions                 | Lista imprese rappresentate |
| Legal institutions vuota           | Lista vuota                 |

