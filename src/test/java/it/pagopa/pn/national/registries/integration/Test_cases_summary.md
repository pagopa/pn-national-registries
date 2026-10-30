# pn-national-registries - Test cases summary E2E

Questo documento riepiloga **solo gli scenari presenti nella cartella `integration/`**, aggiornati allo stato corrente della suite e includendo i test aggiunti/corretti durante la revisione.

La suite contiene **150 test annotati `@Test` in 19 classi concrete di integrazione**. `AbstractNationalRegistriesComponentIT` e `PnNationalRegistriesTestApplication` sono classi di supporto e non vengono conteggiate come classi di test concrete.

La struttura segue quella della dev validation: configurazioni C1-C8, separazione PF/PG, fisico/digitale e dettaglio degli scenari funzionali coperti.

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

---

## 2. PF - Persona fisica

### 2.1 PF fisico - valido per C1-C8

| Scenario E2E                          | Esito atteso                                          | Test / classe                     |
|---------------------------------------|-------------------------------------------------------|-----------------------------------|
| Residenza valida ANPR                 | Risultato `ANPR` con indirizzo fisico                 | `PhysicalAddressTestIT`           |
| Nessuna residenza utile               | Risultato ANPR senza indirizzo fisico                 | `PhysicalAddressTestIT`           |
| CF non trovato / EN122                | Risultato vuoto senza fallback                        | `PhysicalAddressTestIT`           |
| Residenza estera                      | Mapping coerente di stato/comune/provincia/CAP        | `PhysicalAddressTestIT`           |
| Più soggetti in risposta              | Selezione del soggetto con CF corretto                | `PhysicalAddressTestIT`           |
| Campi completi                        | Mapping completo indirizzo                            | `PhysicalAddressTestIT`           |
| Campi parziali                        | Campi opzionali coerenti                              | `PhysicalAddressTestIT`           |
| Caratteri accentati                   | Mapping preservato                                    | `PhysicalAddressTestIT`           |
| Civico non standard                   | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| Civico metrico                        | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| Numero + lettera civico               | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| SNC                                   | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| Colore / scala                        | Mapping coerente                                      | `PhysicalAddressTestIT`           |
| Struttura indirizzo complessa         | Mapping completo                                      | `PhysicalAddressTestIT`           |
| CAP internazionale                    | Valore preservato                                     | `PhysicalAddressTestIT`           |
| Contatore ANPR / id operazione        | Incremento e mapping richiesta corretti               | `PhysicalAddressTestIT`           |
| Errore tecnico ANPR                   | Errore propagato                                      | `PhysicalAddressTestIT`           |
| Endpoint ANPR diretto vs Gateway      | Stessa fixture produce lo stesso indirizzo funzionale | `PhysicalAddressTestIT`           |
| Modalità `MINIMAL`                    | Composizione minimal                                  | `PhysicalAddressTestIT`           |
| Modalità `OLD`                        | Composizione legacy                                   | `PhysicalAddressOldModeIT`        |
| Modalità `FULL`                       | Composizione completa                                 | `PhysicalAddressFullModeIT`       |
| FULL oltre limite lunghezza           | Rispetto del limite massimo                           | `PhysicalAddressFullModeIT`       |
| Modalità `OLD_MINIMAL`                | Composizione old-minimal                              | `PhysicalAddressOldMinimalModeIT` |
| Modalità sconosciuta                  | Fallback a `OLD`                                      | `PhysicalAddressInvalidModeIT`    |
| Batch physical PF/PG misto            | Elementi indipendenti e ordinati                      | `PhysicalAddressTestIT`           |
| Lista physical vuota                  | Richiesta rifiutata come bad request                  | `PhysicalAddressTestIT`           |
| Errore ANPR singolo elemento batch    | Non blocca gli altri elementi                         | `PhysicalAddressTestIT`           |
| Too many requests su singolo elemento | Errore confinato all'elemento fallito                 | `PhysicalAddressTestIT`           |
| Errore generico su singolo elemento   | Errore confinato all'elemento fallito                 | `PhysicalAddressTestIT`           |

---

### 2.2 PF digitale - C1

**Configurazione:** INAD sync, fallback OFF, nuovo IniPEC.

| Scenario E2E                              | Esito atteso                                                           | Test / classe            |
|-------------------------------------------|------------------------------------------------------------------------|--------------------------|
| PEC personale valida                      | Risultato finale INAD                                                  | `DigitalAddressPFTestIT` |
| Personale + professionale valide          | Prevale la personale                                                   | `DigitalAddressPFTestIT` |
| Solo professionale valida                 | Risultato vuoto                                                        | `DigitalAddressPFTestIT` |
| Personale invalida + professionale valida | Risultato vuoto con fallback OFF                                       | `DigitalAddressPFTestIT` |
| Più professionali senza personale         | Risultato vuoto                                                        | `DigitalAddressPFTestIT` |
| Lista INAD vuota                          | Risultato vuoto                                                        | `DigitalAddressPFTestIT` |
| CF non trovato                            | Risultato vuoto                                                        | `DigitalAddressPFTestIT` |
| Personale scaduta                         | Risultato vuoto                                                        | `DigitalAddressPFTestIT` |
| Personale scaduta + professionale valida  | Risultato vuoto con fallback OFF                                       | `DigitalAddressPFTestIT` |
| Personale senza `usageInfo`               | PEC considerata valida                                                 | `DigitalAddressPFTestIT` |
| Personale senza `dateEndValidity`         | PEC considerata valida                                                 | `DigitalAddressPFTestIT` |
| Prima personale invalida, seconda valida  | La prima invalida viene filtrata e viene selezionata la seconda valida | `DigitalAddressPFTestIT` |
| Sola professionale scaduta                | Risultato vuoto                                                        | `DigitalAddressPFTestIT` |
| Errore tecnico INAD                       | Errore propagato, nessun fallback                                      | `DigitalAddressPFTestIT` |
| Endpoint INAD diretto vs Gateway          | Stessa fixture produce lo stesso indirizzo funzionale                  | `DigitalAddressPFTestIT` |
| Gateway JSON malformato                   | Bad request                                                            | `DigitalAddressPFTestIT` |
| Gateway senza clientId                    | Bad request                                                            | `DigitalAddressPFTestIT` |
| Gateway senza taxId                       | Bad request                                                            | `DigitalAddressPFTestIT` |

---

### 2.3 PF digitale - C2

**Configurazione:** INAD sync, fallback OFF, legacy IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| Tutti gli scenari INAD sync di C1 | Stesso risultato funzionale di C1 | `DigitalAddressPFTestIT` |
| INAD NotFound con `oldInipec=true` e fallback OFF | Nessuna request legacy creata | configurazione legacy PF |

---

### 2.4 PF digitale - C3

**Configurazione:** INAD sync, fallback ON, nuovo IniPEC.

| Scenario E2E                              | Esito atteso                           | Test / classe                    |
|-------------------------------------------|----------------------------------------|----------------------------------|
| Personale valida                          | Risultato INAD, nessun fallback        | `DigitalAddressPFFallbackTestIT` |
| Solo professionale valida                 | PEC professionale utilizzata           | `DigitalAddressPFFallbackTestIT` |
| Personale invalida + professionale valida | PEC professionale utilizzata           | `DigitalAddressPFFallbackTestIT` |
| Personale scaduta + professionale valida  | PEC professionale utilizzata           | `DigitalAddressPFFallbackTestIT` |
| INAD NotFound -> IniPEC impresa           | Risultato finale IniPEC                | `DigitalAddressPFFallbackTestIT` |
| INAD NotFound -> IniPEC professionisti    | Tutte le PEC valide mantenute          | `DigitalAddressPFFallbackTestIT` |
| INAD NotFound -> impresa + professionisti | Tutti gli indirizzi validi mantenuti   | `DigitalAddressPFFallbackTestIT` |
| INAD NotFound -> IniPEC senza PEC         | Risultato finale vuoto                 | `DigitalAddressPFFallbackTestIT` |
| INAD scaduto -> IniPEC valido             | Risultato finale IniPEC                | `DigitalAddressPFFallbackTestIT` |
| PEC IniPEC invalide                       | Filtrate; zero valide -> NotFound      | `DigitalAddressPFFallbackTestIT` |
| IniPEC `ND`                               | Risultato finale vuoto                 | `DigitalAddressPFFallbackTestIT` |
| IniPEC `NF`                               | Risultato finale vuoto                 | `DigitalAddressPFFallbackTestIT` |
| IniPEC CF assente                         | Gestione NotFound                      | `DigitalAddressPFFallbackTestIT` |
| IniPEC `ER`                               | Retry request                          | `DigitalAddressPFFallbackTestIT` |
| IniPEC in-progress                        | Retry in-progress                      | `DigitalAddressPFFallbackTestIT` |
| IniPEC metadata errore                    | Retry polling                          | `DigitalAddressPFFallbackTestIT` |
| Retry IniPEC esaurito                     | Request finale `ERROR`                 | `DigitalAddressPFFallbackTestIT` |
| Errore tecnico INAD                       | Nessun fallback improprio verso IniPEC | `DigitalAddressPFFallbackTestIT` |

---

### 2.5 PF digitale - C4

**Configurazione:** INAD sync, fallback ON, legacy IniPEC.

| Scenario E2E                                        | Esito atteso                                                                   | Test / classe                             |
|-----------------------------------------------------|--------------------------------------------------------------------------------|-------------------------------------------|
| INAD NotFound -> legacy IniPEC                      | Creazione flusso legacy                                                        | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy submit error                                 | Retry request fino a soglia                                                    | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy submit `BAD_REQUEST`                         | Errore immediato                                                               | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy `ER` a soglia                                | Request finale `ERROR`                                                         | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy polling error a soglia                       | Polling/request finali `ERROR`                                                 | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy polling in-progress a soglia                 | Polling/request finali `ERROR`                                                 | `DigitalAddressPFFallbackOldInipecTestIT` |
| Output legacy realmente consumato                   | `sendStatus` diventa `SENT`                                                    | `DigitalAddressPFFallbackOldInipecTestIT` |
| Output inviato ma persistenza `sendStatus` fallisce | Recovery completa lo stato di delivery senza duplicare il risultato funzionale | `DigitalAddressPFFallbackOldInipecTestIT` |
| Happy path PF legacy                                | Risultato finale IniPEC                                                        | `InipecLegacyFlowIT`                      |
| Legacy NotFound PF                                  | Risultato finale vuoto                                                         | `InipecLegacyFlowIT`                      |
| Legacy più PEC professionali                        | Tutte le PEC valide mantenute                                                  | `InipecLegacyFlowIT`                      |
| Legacy impresa + professionisti                     | Tutti gli indirizzi validi mantenuti                                           | `InipecLegacyFlowIT`                      |
| Legacy PEC invalide                                 | PEC invalide filtrate                                                          | `InipecLegacyFlowIT`                      |
| Legacy missing tax-id                               | Risultato finale vuoto                                                         | `InipecLegacyFlowIT`                      |
| Legacy `ND`                                         | Risultato finale vuoto                                                         | `InipecLegacyFlowIT`                      |
| Legacy `ER`                                         | Retry request                                                                  | `InipecLegacyFlowIT`                      |
| Legacy polling error/in-progress                    | Retry coerente                                                                 | `InipecLegacyFlowIT`                      |

---

### 2.6 PF digitale - C5

**Configurazione:** INAD massive, fallback OFF, nuovo IniPEC.

| Scenario E2E                                        | Esito atteso                                                                                 | Test / classe               |
|-----------------------------------------------------|----------------------------------------------------------------------------------------------|-----------------------------|
| Creazione request INAD massive                      | Request `INAD/NOT_WORKED`                                                                    | `DigitalAddressPFMassiveIT` |
| Submit batch e polling                              | Flusso completa la request                                                                   | `DigitalAddressPFMassiveIT` |
| Scenari funzionali INAD massive PF                  | PEC personale valida / professionale / invalida / scaduta / CF assente gestiti correttamente | `DigitalAddressPFMassiveIT` |
| CF duplicato nella risposta                         | Primo risultato provider mantenuto                                                           | `DigitalAddressPFMassiveIT` |
| Provider accetta batch, persistenza locale fallisce | Restart non duplica la submission al provider                                                | `DigitalAddressPFMassiveIT` |
| Aggiornamento request del batch parziale            | Restart mantiene un solo batch coerente                                                      | `DigitalAddressPFMassiveIT` |
| Due worker competono sullo stesso batch             | Una sola reservation vince                                                                   | `DigitalAddressPFMassiveIT` |
| Persistenza parziale durante polling risultato      | Replay completa solo la request rimanente                                                    | `DigitalAddressPFMassiveIT` |
| Caratteri speciali nel payload                      | Preservati nell'intero flusso async                                                          | `DigitalAddressPFMassiveIT` |
| Error retry exhaustion su cicli reali               | Contatori persistiti e batch/request finali `ERROR`                                          | `DigitalAddressPFMassiveIT` |
| Paginazione/correlazione massive                    | Matching stabile con duplicati, ordine diverso e CF assente                                  | `DigitalAddressPFMassiveIT` |

---

### 2.7 PF digitale - C6

**Configurazione:** INAD massive, fallback OFF, legacy IniPEC.

| Scenario E2E                         | Esito atteso                                                    | Test / classe               |
|--------------------------------------|-----------------------------------------------------------------|-----------------------------|
| Tutti gli scenari INAD massive di C5 | Stesso comportamento funzionale di C5                           | `DigitalAddressPFMassiveIT` |
| INAD NotFound                        | Risultato finale vuoto, nessuna legacy request con fallback OFF | configurazione C6           |

---

### 2.8 PF digitale - C7

**Configurazione:** INAD massive, fallback ON, nuovo IniPEC.

| Scenario E2E                                            | Esito atteso                                                           | Test / classe                       |
|---------------------------------------------------------|------------------------------------------------------------------------|-------------------------------------|
| Full-flow INAD massive -> IniPEC found                  | Risultato finale IniPEC                                                | `DigitalAddressPFMassiveFallbackIT` |
| Full-flow INAD massive -> IniPEC NotFound               | Risultato finale vuoto                                                 | `DigitalAddressPFMassiveFallbackIT` |
| Full-flow INAD massive -> IniPEC `ER`                   | Retry request IniPEC                                                   | `DigitalAddressPFMassiveFallbackIT` |
| Batch INAD già completato ma chiusura batch fallita     | Replay chiude solo il batch senza duplicare fallback                   | `DigitalAddressPFMassiveFallbackIT` |
| Processing INAD interrotto dopo una parte delle request | Replay processa solo le request ancora `WORKING`                       | `DigitalAddressPFMassiveFallbackIT` |
| Fallback INAD -> IniPEC                                 | CorrelationId e referenceRequestDate preservati                        | `DigitalAddressPFMassiveFallbackIT` |
| Fallback ON con professionale valida                    | La professionale viene selezionata prima di creare fallback IniPEC     | `DigitalAddressPFMassiveFallbackIT` |
| Personale invalida + professionale valida               | La professionale viene selezionata e non viene avviato fallback IniPEC | `DigitalAddressPFMassiveFallbackIT` |

---

### 2.9 PF digitale - C8

**Configurazione:** INAD massive, fallback ON, legacy IniPEC.

| Scenario E2E                            | Esito atteso                     | Test / classe                                                   |
|-----------------------------------------|----------------------------------|-----------------------------------------------------------------|
| INAD massive NotFound -> legacy IniPEC  | Fallback legacy creato           | `DigitalAddressPFMassiveOldInipecIT`                            |
| Full-flow INAD massive -> legacy IniPEC | Risultato finale legacy          | `DigitalAddressPFMassiveOldInipecIT`                            |
| Retry/errori legacy                     | Gestiti secondo le regole legacy | `DigitalAddressPFFallbackOldInipecTestIT`, `InipecLegacyFlowIT` |

---

## 3. PG - Persona giuridica

### 3.1 PG fisico - valido per C1-C8

| Scenario E2E                   | Esito atteso                                | Test / classe           |
|--------------------------------|---------------------------------------------|-------------------------|
| Sede legale valida             | `REGISTRO_IMPRESE` con indirizzo            | `PhysicalAddressTestIT` |
| Nessuna sede                   | Risultato senza `physicalAddress`           | `PhysicalAddressTestIT` |
| Tutti i campi                  | Mapping completo                            | `PhysicalAddressTestIT` |
| Paese estero                   | Mapping coerente                            | `PhysicalAddressTestIT` |
| Caratteri accentati            | Mapping preservato                          | `PhysicalAddressTestIT` |
| Sede secondaria                | Mapping coerente                            | `PhysicalAddressTestIT` |
| Errore tecnico                 | Errore propagato                            | `PhysicalAddressTestIT` |
| Batch PF/PG misto              | Elementi indipendenti                       | `PhysicalAddressTestIT` |
| PG senza sede legale nel batch | Elemento restituito senza `physicalAddress` | `PhysicalAddressTestIT` |

---

### 3.2 PG digitale - C1

**Configurazione:** INAD sync, nuovo IniPEC.

| Scenario E2E                                            | Esito atteso                                          | Test / classe            |
|---------------------------------------------------------|-------------------------------------------------------|--------------------------|
| IPA PEC valida                                          | Risultato finale IPA                                  | `DigitalAddressPGTestIT` |
| IPA più risultati                                       | Uso WS05 e risultato IPA                              | `DigitalAddressPGTestIT` |
| IPA NotFound -> IniPEC found                            | Risultato finale IniPEC                               | `DigitalAddressPGTestIT` |
| IPA PEC invalida -> IniPEC found                        | Fallback IniPEC                                       | `DigitalAddressPGTestIT` |
| Errore tecnico IPA                                      | Errore propagato, nessun fallback improprio           | `DigitalAddressPGTestIT` |
| IniPEC NotFound / `ND` / `NF` / CF assente -> INAD sync | Fallback INAD                                         | `DigitalAddressPGTestIT` |
| IniPEC `ER`                                             | Retry request IniPEC senza creare INAD                | `DigitalAddressPGTestIT` |
| INAD singolo indirizzo valido                           | Risultato finale INAD                                 | `DigitalAddressPGTestIT` |
| INAD più indirizzi                                      | Risultato vuoto                                       | `DigitalAddressPGTestIT` |
| INAD con professione                                    | Risultato vuoto                                       | `DigitalAddressPGTestIT` |
| INAD PEC invalida/scaduta                               | Risultato vuoto                                       | `DigitalAddressPGTestIT` |
| INAD una PEC valida + una scaduta                       | Viene restituita la PEC valida                        | `DigitalAddressPGTestIT` |
| Endpoint IPA diretto vs Gateway                         | Stessa fixture produce lo stesso indirizzo funzionale | `DigitalAddressPGTestIT` |
| PF fallback flag abilitato                              | Comportamento PG invariato                            | `DigitalAddressPGTestIT` |

---

### 3.3 PG digitale - C2

**Configurazione:** INAD sync, legacy IniPEC.

| Scenario E2E                    | Esito atteso                    | Test / classe                     |
|---------------------------------|---------------------------------|-----------------------------------|
| IPA NotFound -> legacy IniPEC   | Creazione flusso legacy         | `DigitalAddressPGOldInipecTestIT` |
| Legacy PG NotFound -> INAD sync | Fallback INAD                   | `InipecLegacyFlowIT`              |
| Legacy PG INAD NotFound         | Risultato finale vuoto          | `InipecLegacyFlowIT`              |
| Legacy PG INAD technical error  | Request legacy finale in errore | `InipecLegacyFlowIT`              |
| Retry/errori legacy             | Retry request/polling coerenti  | `InipecLegacyFlowIT`              |

---

### 3.4 PG digitale - C3

**Configurazione:** INAD sync, PF fallback ON, nuovo IniPEC.

| Scenario E2E                       | Esito atteso           | Test / classe            |
|------------------------------------|------------------------|--------------------------|
| Tutti i casi di C1                 | Stesso risultato di C1 | `DigitalAddressPGTestIT` |
| Invarianza rispetto a `pfFallback` | Risultato PG invariato | `DigitalAddressPGTestIT` |

---

### 3.5 PG digitale - C4

**Configurazione:** INAD sync, PF fallback ON, legacy IniPEC.

| Scenario E2E                       | Esito atteso           | Test / classe                                           |
|------------------------------------|------------------------|---------------------------------------------------------|
| Tutti i casi legacy di C2          | Stesso risultato di C2 | `DigitalAddressPGOldInipecTestIT`, `InipecLegacyFlowIT` |
| Invarianza rispetto a `pfFallback` | Risultato PG invariato | copertura PG legacy                                     |

---

### 3.6 PG digitale - C5

**Configurazione:** INAD massive, nuovo IniPEC.

| Scenario E2E                                                             | Esito atteso                                                                                             | Test / classe                              |
|--------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|--------------------------------------------|
| IPA NotFound / invalida -> IniPEC                                        | Prosecuzione su IniPEC                                                                                   | `DigitalAddressPGMassiveIT`                |
| IniPEC NotFound / `ND` / `NF` -> INAD massive                            | Creazione request INAD                                                                                   | `DigitalAddressPGMassiveIT`                |
| Full-flow IPA -> IniPEC -> INAD massive                                  | Risultato finale INAD                                                                                    | `DigitalAddressPGMassiveIT`                |
| INAD PEC invalida / multipla / professionale                             | Risultato vuoto secondo regole PG                                                                        | `DigitalAddressPGMassiveIT`                |
| INAD una PEC valida + una scaduta                                        | Viene restituita la PEC valida                                                                           | `DigitalAddressPGMassiveIT`                |
| Retry in-progress INAD                                                   | `attemptAfter` e contatore aggiornati                                                                    | `DigitalAddressPGMassiveIT`                |
| Errore result INAD                                                       | Retry normale                                                                                            | `DigitalAddressPGMassiveIT`                |
| Error retry exhaustion                                                   | Request/batch `ERROR`                                                                                    | `DigitalAddressPGMassiveRetryExhaustionIT` |
| In-progress retry exhaustion                                             | Request/batch `ERROR`                                                                                    | `DigitalAddressPGMassiveRetryExhaustionIT` |
| Batch IniPEC già lavorato ma chiusura batch fallita                      | Replay chiude solo il batch senza duplicare fallback INAD                                                | `DigitalAddressPGMassiveIT`                |
| Processing IniPEC interrotto dopo una parte delle request                | Replay processa solo le request ancora `WORKING`                                                         | `DigitalAddressPGMassiveIT`                |
| Batch IniPEC misto FOUND / NF / ER                                       | Ogni CF segue il proprio esito; solo ER viene riprocessato                                               | `DigitalAddressPGMassiveIT`                |
| Preparazione batch IniPEC con request già `WORKING` e altra `NOT_WORKED` | Al nuovo ciclo viene sottomessa solo la request rimanente; la già `WORKING` conserva il batch precedente | `DigitalAddressPGMassiveIT`                |

---

### 3.7 PG digitale - C6

**Configurazione:** INAD massive, legacy IniPEC.

| Scenario E2E                              | Esito atteso                                          | Test / classe               |
|-------------------------------------------|-------------------------------------------------------|-----------------------------|
| Legacy IniPEC PG NotFound -> INAD massive | Creazione request `INAD/NOT_WORKED` e chiusura legacy | `InipecLegacyMassiveFlowIT` |
| Full-flow legacy -> INAD massive          | Risultato finale INAD                                 | `InipecLegacyMassiveFlowIT` |
| Retry/errori legacy prima del fallback    | Gestiti dal flusso legacy                             | `InipecLegacyFlowIT`        |

---

### 3.8 PG digitale - C7

**Configurazione:** INAD massive, PF fallback ON, nuovo IniPEC.

| Scenario E2E                       | Esito atteso           | Test / classe               |
|------------------------------------|------------------------|-----------------------------|
| Tutti i casi di C5                 | Stesso risultato di C5 | `DigitalAddressPGMassiveIT` |
| Invarianza rispetto a `pfFallback` | Risultato PG invariato | `DigitalAddressPGMassiveIT` |

---

### 3.9 PG digitale - C8

**Configurazione:** INAD massive, PF fallback ON, legacy IniPEC.

| Scenario E2E                       | Esito atteso           | Test / classe               |
|------------------------------------|------------------------|-----------------------------|
| Tutti i casi di C6                 | Stesso risultato di C6 | `InipecLegacyMassiveFlowIT` |
| Full-flow legacy -> INAD massive   | Risultato finale INAD  | `InipecLegacyMassiveFlowIT` |
| Invarianza rispetto a `pfFallback` | Risultato PG invariato | copertura PG legacy/massive |

---

## 4. Flussi digitali condivisi coperti E2E

| Scenario E2E                                      | Esito atteso                                        | Test / classe                                                                                                                |
|---------------------------------------------------|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| INAD massive: batch + polling fino al risultato   | Request finale `WORKED` / `ERROR`                   | `DigitalAddressPFMassiveIT`, `DigitalAddressPGMassiveIT`, `DigitalAddressPGMassiveRetryExhaustionIT`                         |
| IniPEC nuovo: submit + polling fino al risultato  | Request finale `WORKED` / `ERROR`                   | `DigitalAddressPFFallbackTestIT`, `DigitalAddressPFMassiveFallbackIT`, `DigitalAddressPGTestIT`, `DigitalAddressPGMassiveIT` |
| IniPEC legacy: submit + polling fino al risultato | Request/batch legacy finale coerente                | `InipecLegacyFlowIT`, `DigitalAddressPFFallbackOldInipecTestIT`                                                              |
| Retry polling normale                             | Nuovo tentativo con `attemptAfter` aggiornato       | test PF/PG massive e IniPEC                                                                                                  |
| Retry polling in-progress                         | Nuovo tentativo con contatore dedicato              | test PF/PG massive e IniPEC                                                                                                  |
| Retry request IniPEC `ER`                         | Request riportata a `NOT_WORKED` / `ERROR` a soglia | test PF/PG IniPEC                                                                                                            |
| Bad request nel polling legacy                    | Errore immediato                                    | `InipecLegacyFlowIT`, `DigitalAddressPFFallbackOldInipecTestIT`                                                              |
| Risultati finali senza dipendere dalla sola SQS   | Stato persistito e `resultPayload` verificati       | full-flow nuovi                                                                                                              |

---

## 4.1 Resilienza, replay, recovery e concorrenza

| Scenario E2E                                                    | Esito atteso                                                                     | Test / classe                             |
|-----------------------------------------------------------------|----------------------------------------------------------------------------------|-------------------------------------------|
| Gateway JSON malformato                                         | Richiesta rifiutata dall'entry point reale                                       | `DigitalAddressPFTestIT`                  |
| Gateway senza clientId                                          | Richiesta rifiutata dall'entry point reale                                       | `DigitalAddressPFTestIT`                  |
| Gateway senza taxId                                             | Richiesta rifiutata dall'entry point reale                                       | `DigitalAddressPFTestIT`                  |
| INAD batch accettato, persistenza batch fallita                 | Restart non duplica la submission provider                                       | `DigitalAddressPFMassiveIT`               |
| INAD aggiornamento request parziale durante preparazione batch  | Restart mantiene il batch coerente                                               | `DigitalAddressPFMassiveIT`               |
| Due worker competono sullo stesso batch                         | Una sola prenotazione effettiva                                                  | `DigitalAddressPFMassiveIT`               |
| INAD processing risultato parziale                              | Replay completa solo gli elementi rimanenti                                      | `DigitalAddressPFMassiveIT`               |
| INAD error retry su cicli reali                                 | Contatori persistiti fino a esaurimento                                          | `DigitalAddressPFMassiveIT`               |
| INAD massive paginato/correlato                                 | Matching stabile con duplicati e CF mancanti                                     | `DigitalAddressPFMassiveIT`               |
| Fallback INAD -> IniPEC: batch già completato, chiusura fallita | Replay chiude il batch senza duplicare fallback                                  | `DigitalAddressPFMassiveFallbackIT`       |
| Fallback INAD -> IniPEC: processing parziale                    | Replay processa solo le request ancora `WORKING`                                 | `DigitalAddressPFMassiveFallbackIT`       |
| Fallback INAD -> IniPEC: identità richiesta                     | CorrelationId e referenceRequestDate preservati                                  | `DigitalAddressPFMassiveFallbackIT`       |
| Fallback IniPEC -> INAD: batch già completato, chiusura fallita | Replay chiude il batch senza duplicare fallback                                  | `DigitalAddressPGMassiveIT`               |
| Fallback IniPEC -> INAD: processing parziale                    | Replay processa solo le request ancora `WORKING`                                 | `DigitalAddressPGMassiveIT`               |
| IniPEC batch misto FOUND/NF/ER                                  | Solo ER resta retryabile e viene sottomesso al ciclo successivo                  | `DigitalAddressPGMassiveIT`               |
| Recovery preparazione batch IniPEC parziale                     | Request già `WORKING` non viene reinviata; viene sottomessa solo la `NOT_WORKED` | `DigitalAddressPGMassiveIT`               |
| Legacy output inviato ma persistence `sendStatus` fallita       | Recovery completa lo stato di delivery                                           | `DigitalAddressPFFallbackOldInipecTestIT` |
| Special characters nel payload async                            | Nessuna perdita/corruzione nel ciclo completo                                    | `DigitalAddressPFMassiveIT`               |

---

## 4.2 Coerenza endpoint diretti / Gateway e endpoint sincroni

| Scenario E2E                                       | Esito atteso                               | Test / classe                   |
|----------------------------------------------------|--------------------------------------------|---------------------------------|
| ANPR diretto vs Gateway                            | Stesso risultato funzionale                | `PhysicalAddressTestIT`         |
| INAD diretto vs Gateway                            | Stesso risultato funzionale                | `DigitalAddressPFTestIT`        |
| IPA diretto vs Gateway                             | Stesso risultato funzionale                | `DigitalAddressPGTestIT`        |
| Agenzia Entrate tax-id valido                      | Check CF positivo                          | `AgenziaEntrateSyncEndpointsIT` |
| Agenzia Entrate tax-id inesistente                 | Esito negativo con errore provider mappato | `AgenziaEntrateSyncEndpointsIT` |
| Agenzia Entrate rappresentante legale matching     | Esito positivo                             | `AgenziaEntrateSyncEndpointsIT` |
| Agenzia Entrate rappresentante legale non matching | Esito negativo                             | `AgenziaEntrateSyncEndpointsIT` |
| Agenzia Entrate XML malformato                     | `RuntimeJAXBException`                     | `AgenziaEntrateSyncEndpointsIT` |
| Registro Imprese indirizzo legale                  | Indirizzo restituito                       | `InfoCamereSyncEndpointsIT`     |
| Registro Imprese errore applicativo                | Request restituita senza indirizzo         | `InfoCamereSyncEndpointsIT`     |
| InfoCamere rappresentante legale matching          | `true`                                     | `InfoCamereSyncEndpointsIT`     |
| InfoCamere rappresentante legale non matching      | `false`                                    | `InfoCamereSyncEndpointsIT`     |
| InfoCamere errore applicativo rappresentante       | Esito negativo                             | `InfoCamereSyncEndpointsIT`     |
| InfoCamere legal institutions                      | Lista imprese rappresentate                | `InfoCamereSyncEndpointsIT`     |
| InfoCamere legal institutions vuota                | Lista vuota                                | `InfoCamereSyncEndpointsIT`     |

---

## 5. Perimetro non E2E / non incluso nel summary

Questo file non include i test tecnici presenti fuori da `integration/`, ad esempio:

- paginazione interna dei repository/job non attraversata da un flusso applicativo completo;
- max batch size isolato;
- missing polling id isolato;
- conditional update failure isolato;
- classificazione retry HTTP/network a livello client;
- configurazione `retry.on-timeout` isolata;
- test isolati di `PollingJob` / `PollingInputsHandler`;
- contract test HTTP/SOAP verso registri esterni reali;
- EventBridge Pipe / SQS / DLQ infrastrutturali non esercitati da uno scenario completo.

Tali scenari non sono considerati E2E in questo riepilogo finché non vengono verificati all'interno di un flusso completo partendo dal relativo ingresso applicativo.

---

## 6. Stato finale

- `integration/` contiene **150 test `@Test` in 19 classi concrete di integrazione**.
- Gli scenari PHYSICAL con configurazione standard sono consolidati in `PhysicalAddressTestIT`; le classi con property differenti restano separate.
- I flussi digitali PF/PG coprono combinazioni sync/massive, fallback nuovo/legacy e le principali transizioni di retry/error.
- La suite copre ora anche recovery/replay su processing parziale, concorrenza batch, correlazione massive e recovery della preparazione batch IniPEC parzialmente persistita.
- Il caso PF massive fallback ON con PEC professionale valida è coperto da `PF_Massive_FallbackOn_SelectsProfessionalBeforeStartingInipecFallback`; non è più considerato gap.
- Il caso di recovery preparazione batch IniPEC parziale è coperto da `InipecBatch_PartialRequestWorkingUpdate_RestartProcessesOnlyRemainingRequest`.
- I test tecnici fuori da `integration/` non sono inclusi nel conteggio.
- I contract test verso registri esterni e gli scenari infrastrutturali DLQ restano fuori dal perimetro di questo summary.
