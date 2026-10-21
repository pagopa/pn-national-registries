# Domicili Analogici - PF(ANPR) e PG(Registro Imprese) - Scenari funzionali di test

| Scenario                                         | Esito atteso                                                                                          |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| PF physical - ANPR                               | Viene restituito l'indirizzo ANPR e il flusso termina senza ulteriori ricerche                        |
| PF physical - ANPR not found                     | Viene pubblicato un output vuoto con registry `ANPR` e nessun indirizzo fisico                        |
| PF physical - ANPR technical error               | L'errore tecnico viene propagato e non vengono generate risposte positive                             |
| PG physical - Registro Imprese                   | Viene restituito l'indirizzo fisico e il flusso termina                                               |
| PG physical - Registro Imprese not found         | Viene pubblicato un output vuoto con registry `REGISTRO_IMPRESE` e nessun indirizzo fisico            |
| PG physical - Registro Imprese technical error   | L'errore tecnico viene propagato e non vengono generate risposte positive                             |
| PF physical - ANPR con campi completi estesi     | Nel test originale mancavano asserzioni su addressDetails, at, municipalityDetails, foreignState, zip |
| PF physical - ANPR con campi parziali            | Garantisce che i campi opzionali siano null quando non forniti da ANPR, non defaultati                |
| PF physical - ANPR con caratteri accentati       | Garantisce correttezza end-to-end del mapping anche con indirizzi in lingue con diacritici            |
| PF physical - ANPR con civico non standard       | Valida robustezza del mapping su formati civici non convenzionali tipici di aree rurali               |
| PF physical - ANPR con strada lunga              | Garantisce che lunghe denominazioni stradali siano preservate integralmente                           |
| PF physical - ANPR con metrico civico (KM)       | Scenario reale per indirizzi su strade provinciali e nazionali                                        |
| PF physical - ANPR con numero e lettera civico   | Pattern classico italiano per civici con più ingressi                                                 |
| PF physical - ANPR con SNC (senza numero civico) | Scenario frequente in aree rurali e montane                                                           |
| PF physical - ANPR con colore e scala            | ExtraInfo civico completo di identificativi aggiuntivi                                                |
| PF physical - ANPR solo specie e numero          | Caso base di mappatura per validare format minimale                                                   |
| PF physical - ANPR con struttura complessa       | Scenario end-to-end massimale con IndirizzoDetails completo                                           |
| PF physical - ANPR con specie PIAZZA             | Valida genericità della costruzione per diversi tipi di ubicazione                                    |
| PG physical - Registro Imprese con tutti i campi | Nel test originale mancavano asserzioni su zip e foreignState                                         |
| PG physical - Registro Imprese con paese estero  | Copre scenario reale di aziende estere registrate presso Registro Imprese                             |
| PG physical - Registro Imprese con accentati     | Garantisce preserve dei diacritici anche nei nomi di città non comuni                                 |
| PG physical - Registro Imprese sede secondaria   | Copre scenario reale di aziende con sedi operative in aggiunta alla sede legale                       |
| Address mapping - formato CAP internazionale UK  | Valida supporto per indirizzi UK dove CAP ha formato alfanumerico                                     |
| Address mapping - formato CAP internazionale BE  | Valida supporto per indirizzi belghe e altri stati esteri con formati CAP variabili                   |

# Domicili Digitali - PF e PG - Scenari funzionali di test


# Inad Polling - Scenari funzionali di test

-----

# IniPEC Polling - Scenari funzionali di test

Questa sezione descrive gli scenari funzionali coperti dai test del flusso IniPEC.

## Scenari funzionali coperti

| Scenario funzionale                                       | Esito tecnico                                                                         | Esito funzionale atteso                       |
|-----------------------------------------------------------|---------------------------------------------------------------------------------------|-----------------------------------------------|
| PF, `statoImpresa = null`, PEC valida                     | Request INIPEC `WORKED` con `resultPayload` valorizzato                               | Risposta finale IniPEC con PEC valida         |
| PF, `statoImpresa = null`, PEC sintatticamente non valida | Request INIPEC `WORKED` con payload finale senza indirizzi                            | Nessun fallback INAD                          |
| PF, `statoImpresa = null`, PEC vuota                      | Request INIPEC `WORKED` con payload senza indirizzi                                   | Nessun fallback INAD                          |
| PF, `statoImpresa = ND`                                   | Request INIPEC `WORKED` con payload senza indirizzi                                   | Esito finale IniPEC senza indirizzo           |
| PF, `statoImpresa = NF`                                   | Request INIPEC `WORKED` con payload senza indirizzi                                   | Esito finale IniPEC senza indirizzo           |
| PF, `statoImpresa = ER`                                   | Request INIPEC `NOT_WORKED` con `retryCount` incrementato                             | La singola request viene ritentata            |
| PG, `statoImpresa = null`, PEC valida                     | Request INIPEC `WORKED` finale con `resultPayload`                                    | Risposta finale IniPEC                        |
| PG, `statoImpresa = null`, PEC sintatticamente non valida | INIPEC `WORKED` non finale + nuova request INAD `WORKED`                              | Fallback INAD sync riuscito                   |
| PG, `statoImpresa = null`, PEC vuota                      | INIPEC `WORKED` non finale + nuova request INAD `WORKED`                              | Fallback INAD sync riuscito                   |
| PG, `statoImpresa = ND`                                   | INIPEC `WORKED` non finale + nuova request INAD `WORKED`                              | Fallback INAD sync                            |
| PG, `statoImpresa = NF`                                   | INIPEC `WORKED` non finale + nuova request INAD `WORKED`                              | Fallback INAD sync                            |
| PG, `statoImpresa = ER`                                   | INIPEC `NOT_WORKED`, retry incrementato, nessuna request INAD                         | Retry della singola request                   |
| INAD sync con successo                                    | Nuova request INAD `WORKED` con `resultPayload`; INIPEC `WORKED` senza payload        | Risultato finale proveniente da INAD          |
| INAD sync - CF non trovato                                | Nuova request INAD `WORKED` con `resultPayload` e `digitalAddress` vuoto              | Esito funzionale valido, non errore           |
| INAD sync - errore reale                                  | Nuova request INAD `ERROR`; INIPEC `WORKED` non finale                                | Fallimento definitivo del fallback INAD       |
| Polling IniPEC ancora in progress                         | Batch `REQUEST_SENT`, incremento `inProgressRetryCount`, aggiornamento `attemptAfter` | Nessuna request applicativa modificata        |
| Errore polling retryable normale                          | Batch `REQUEST_SENT`, incremento `retryCount`, aggiornamento `attemptAfter`           | Nuovo tentativo pianificato                   |
| Retry polling normale esaurito                            | Request `WORKING` → `ERROR`, batch → `ERROR`                                          | Chiusura definitiva in errore                 |
| Retry polling in-progress esaurito                        | Request `WORKING` → `ERROR`, batch → `ERROR`                                          | Chiusura definitiva in errore                 |
| `BAD_REQUEST` durante il polling                          | Chiusura immediata delle request e del batch in `ERROR`                               | Nessun ulteriore retry                        |
| Metadata di errore nella risposta InfoCamere              | Trattato come errore polling e sottoposto a retry                                     | Nessun processing delle request come successo |
| Errore non gestito / infrastrutturale                     | L'errore viene propagato al consumer per consentire il requeue                        | Scenario specifico del nuovo flusso           |

## Differenze strutturali tra nuovo flusso e legacy

| Aspetto                   | Nuovo flusso                                                                 | Legacy                                      |
|---------------------------|------------------------------------------------------------------------------|---------------------------------------------|
| Persistenza request       | `NationalRegistriesRequest`                                                  | `BatchRequest`                              |
| Persistenza polling/batch | `NationalRegistriesBatch`                                                    | `BatchPolling`                              |
| Fallback INAD sync        | Crea una nuova request con `registry = INAD`                                 | Aggiorna la stessa `BatchRequest`           |
| Risultato finale          | `resultPayload`                                                              | `message`                                   |
| Stato registry            | `registry` + `registryStatus`                                                | `eservice`                                  |
| Retry singola request     | `retryCount` sulla `NationalRegistriesRequest`                               | `retryCount` sulla `pn-batchRequest`        |
| Retry polling             | `errorRetryCount` / `inProgressRetryCount` sulla `NationalRegistriesBatches` | Contatori su `pn-BatchPolling`              |
| Invio risultato verso SQS | Gestito tramite DynamoDB Stream + EventBridge Pipe                           | Gestito direttamente dal servizio legacy    |
| Invio errori verso DLQ    | Gestito tramite DynamoDB Stream + EventBridge Pipe                           | Gestito dal flusso legacy                   |
| Paginazione DynamoDB      | `SdkPublisher<Page<...>>` gestito dall'AWS SDK                               | Gestione manuale tramite `lastEvaluatedKey` |
