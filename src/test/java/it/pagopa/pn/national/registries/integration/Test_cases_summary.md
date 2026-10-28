# pn-national-registries - Test cases summary E2E

Questo documento riepiloga **solo gli scenari end-to-end presenti nella cartella `integration/`**.

La struttura segue quella della dev validation: configurazioni C1-C8, separazione PF/PG, fisico/digitale e dettaglio degli scenari funzionali coperti.

## 1. Configurazioni

| Configurazione | INAD massive | PF fallback | Old IniPEC | Flusso principale |
|---|---:|---:|---:|---|
| **C1** | false | false | false | INAD sync, fallback PF OFF, nuovo IniPEC |
| **C2** | false | false | true | INAD sync, fallback PF OFF, legacy IniPEC |
| **C3** | false | true | false | INAD sync, fallback PF ON, nuovo IniPEC |
| **C4** | false | true | true | INAD sync, fallback PF ON, legacy IniPEC |
| **C5** | true | false | false | INAD massive, fallback PF OFF, nuovo IniPEC |
| **C6** | true | false | true | INAD massive, fallback PF OFF, legacy IniPEC |
| **C7** | true | true | false | INAD massive, fallback PF ON, nuovo IniPEC |
| **C8** | true | true | true | INAD massive, fallback PF ON, legacy IniPEC |

Gli scenari fisici non dipendono dai tre flag digitali.

---

## 2. PF - Persona fisica

### 2.1 PF fisico - valido per C1-C8

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| Residenza valida ANPR | Risultato `ANPR` con indirizzo fisico | `PhysicalAddressTestIT` |
| Nessuna residenza utile | Risultato ANPR senza indirizzo fisico | `AnprPhysicalScenariosIT` |
| CF non trovato / EN122 | Risultato vuoto senza fallback | `PhysicalAddressTestIT` |
| Residenza estera | Mapping coerente di stato/comune/provincia/CAP | `AnprPhysicalScenariosIT` |
| Più soggetti in risposta | Selezione del soggetto con CF corretto | `AnprPhysicalScenariosIT` |
| Campi completi | Mapping completo indirizzo | `PhysicalAddressTestIT` |
| Campi parziali | Campi opzionali coerenti | `PhysicalAddressTestIT` |
| Caratteri accentati | Mapping preservato | `PhysicalAddressTestIT` |
| Civico non standard | Mapping coerente | `PhysicalAddressTestIT` |
| Civico metrico | Mapping coerente | `PhysicalAddressTestIT` |
| Numero + lettera civico | Mapping coerente | `PhysicalAddressTestIT` |
| SNC | Mapping coerente | `PhysicalAddressTestIT` |
| Colore / scala | Mapping coerente | `PhysicalAddressTestIT` |
| Struttura indirizzo complessa | Mapping completo | `PhysicalAddressTestIT` |
| CAP internazionale | Valore preservato | `PhysicalAddressTestIT` |
| Contatore ANPR / id operazione | Incremento e mapping richiesta corretti | `AnprPhysicalScenariosIT` |
| Errore tecnico ANPR | Errore propagato | `PhysicalAddressTestIT` |
| Modalità `MINIMAL` | Composizione minimal | `PhysicalAddressTestIT` |
| Modalità `OLD` | Composizione legacy | `PhysicalAddressOldModeIT` |
| Modalità `FULL` | Composizione completa | `PhysicalAddressFullModeIT` |
| FULL oltre limite lunghezza | Rispetto del limite massimo | `PhysicalAddressFullModeIT` |
| Modalità `OLD_MINIMAL` | Composizione old-minimal | `PhysicalAddressOldMinimalModeIT` |
| Modalità sconosciuta | Fallback a `OLD` | `PhysicalAddressInvalidModeIT` |
| Batch physical PF/PG misto | Elementi indipendenti e ordinati | `PhysicalAddressesSyncIT` |
| Errore singolo elemento batch | Non blocca gli altri elementi | `PhysicalAddressesSyncIT` |

---

### 2.2 PF digitale - C1

**Configurazione:** INAD sync, fallback OFF, nuovo IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| PEC personale valida | Risultato finale INAD | `DigitalAddressPFTestIT` |
| Personale + professionale valide | Prevale la personale | `DigitalAddressPFTestIT` |
| Solo professionale valida | Risultato vuoto | `DigitalAddressPFTestIT` |
| Personale invalida + professionale valida | Risultato vuoto | `DigitalAddressPFTestIT` |
| Più professionali senza personale | Risultato vuoto | `DigitalAddressPFTestIT` |
| Lista INAD vuota | Risultato vuoto | `DigitalAddressPFTestIT` |
| CF non trovato | Risultato vuoto | `DigitalAddressPFTestIT` |
| Personale scaduta | Risultato vuoto | `DigitalAddressPFTestIT` |
| Personale scaduta + professionale valida | Risultato vuoto | `DigitalAddressPFTestIT` |
| Personale senza `usageInfo` | PEC considerata valida | `DigitalAddressPFTestIT` |
| Personale senza `dateEndValidity` | PEC considerata valida | `DigitalAddressPFTestIT` |
| Prima personale invalida, seconda valida | Comportamento corrente verificato | `DigitalAddressPFTestIT` |
| Sola professionale scaduta | Risultato vuoto | `DigitalAddressPFTestIT` |
| Errore tecnico INAD | Errore propagato, nessun fallback | `DigitalAddressPFTestIT` |

---

### 2.3 PF digitale - C2

**Configurazione:** INAD sync, fallback OFF, legacy IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| Tutti gli scenari INAD sync di C1 | Stesso risultato di C1 | `DigitalAddressPFTestIT` |
| INAD NotFound con `oldInipec=true` e fallback OFF | Nessuna request legacy creata | `DigitalAddressPFTestIT` |

---

### 2.4 PF digitale - C3

**Configurazione:** INAD sync, fallback ON, nuovo IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| Personale valida | Risultato INAD, nessun fallback | `DigitalAddressPFFallbackTestIT` |
| Solo professionale valida | PEC professionale utilizzata | `DigitalAddressPFFallbackTestIT` |
| Personale invalida + professionale valida | PEC professionale utilizzata | `DigitalAddressPFFallbackTestIT` |
| Personale scaduta + professionale valida | PEC professionale utilizzata | `DigitalAddressPFFallbackTestIT` |
| INAD NotFound -> IniPEC impresa | Risultato finale IniPEC | `DigitalAddressPFFallbackTestIT` |
| INAD NotFound -> IniPEC professionisti | Tutte le PEC valide mantenute | `DigitalAddressPFFallbackTestIT` |
| INAD NotFound -> impresa + professionisti | Tutti gli indirizzi validi mantenuti | `DigitalAddressPFFallbackTestIT` |
| INAD NotFound -> IniPEC senza PEC | Risultato finale vuoto | `DigitalAddressPFFallbackTestIT` |
| INAD scaduto -> IniPEC valido | Risultato finale IniPEC | `DigitalAddressPFFallbackTestIT` |
| PEC IniPEC invalide | Filtrate; zero valide -> NotFound | `DigitalAddressPFFallbackTestIT` |
| IniPEC `ND` | Risultato finale vuoto | `DigitalAddressPFFallbackTestIT` |
| IniPEC `NF` | Risultato finale vuoto | `DigitalAddressPFFallbackTestIT` |
| IniPEC CF assente | Gestione NotFound | `DigitalAddressPFFallbackTestIT` |
| IniPEC `ER` | Retry request | `DigitalAddressPFFallbackTestIT` |
| IniPEC in-progress | Retry in-progress | `DigitalAddressPFFallbackTestIT` |
| IniPEC metadata errore | Retry polling | `DigitalAddressPFFallbackTestIT` |
| Retry IniPEC esaurito | Request finale `ERROR` | `DigitalAddressPFFallbackTestIT` |
| Errore tecnico INAD | Nessun fallback improprio verso IniPEC | `DigitalAddressPFFallbackTestIT` |

---

### 2.5 PF digitale - C4

**Configurazione:** INAD sync, fallback ON, legacy IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| INAD con PEC utilizzabile | Il flusso termina su INAD | `DigitalAddressPFFallbackOldInipecTestIT` |
| INAD NotFound -> legacy IniPEC | Creazione `BatchRequest` legacy | `DigitalAddressPFFallbackOldInipecTestIT` |
| Full-flow INAD sync -> legacy IniPEC -> PEC impresa | Risultato finale legacy | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy NotFound PF | Risultato finale vuoto | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy più PEC professionali | Tutte le PEC valide mantenute | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy impresa + professionisti | Tutti gli indirizzi validi mantenuti | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy PEC invalide | PEC invalide filtrate | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy missing tax-id | Risultato finale vuoto | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy `ND` | Risultato finale vuoto | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy `NF` | Risultato finale vuoto | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy `ER` | Retry request | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy polling in-progress | Retry in-progress | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy polling error | Retry normale | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy polling `BAD_REQUEST` | Errore immediato | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy submit error | Retry request | `DigitalAddressPFFallbackOldInipecTestIT` |
| Legacy submit `BAD_REQUEST` | Errore immediato | `DigitalAddressPFFallbackOldInipecTestIT` |
| Retry legacy esaurito | Request/polling finale `ERROR` | `DigitalAddressPFFallbackOldInipecTestIT` |

---

### 2.6 PF digitale - C5

**Configurazione:** INAD massive, fallback OFF, nuovo IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| Creazione request INAD massive | Request `INAD/NOT_WORKED` | `DigitalAddressPFMassiveIT` |
| Submit batch e polling | Flusso completa la request | `DigitalAddressPFMassiveIT` |
| PEC personale valida | `resultPayload` INAD | `DigitalAddressPFMassiveIT` |
| Solo professionale | Risultato vuoto | `DigitalAddressPFMassiveIT` |
| PEC personale/professionale invalida | Risultato vuoto | `DigitalAddressPFMassiveIT` |
| PEC scaduta | Risultato vuoto | `DigitalAddressPFMassiveIT` |
| Lista vuota / CF assente | Risultato vuoto | `DigitalAddressPFMassiveIT` |
| Più personali valide | Prima personale selezionata | `DigitalAddressPFMassiveIT` |
| CF duplicato nella risposta | Primo risultato mantenuto | `DigitalAddressPFMassiveIT` |
| Polling `PRESA_IN_CARICO` | Retry in-progress | `DigitalAddressPFMassiveIT` |
| Polling `IN_ELABORAZIONE` | Retry in-progress | `DigitalAddressPFMassiveIT` |
| Errore status/result | Retry normale | `DigitalAddressPFMassiveIT` |
| Retry polling esaurito | Request/batch `ERROR` | `DigitalAddressPFMassiveIT` |
| Bad request polling | `ERROR` immediato | `DigitalAddressPFMassiveIT` |

---

### 2.7 PF digitale - C6

**Configurazione:** INAD massive, fallback OFF, legacy IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| Tutti gli scenari INAD massive di C5 | Stesso comportamento di C5 | `DigitalAddressPFMassiveIT` |
| INAD NotFound | Risultato finale vuoto, nessuna legacy request | `DigitalAddressPFMassiveIT` |

---

### 2.8 PF digitale - C7

**Configurazione:** INAD massive, fallback ON, nuovo IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| PEC personale valida | Risultato finale INAD | `DigitalAddressPFMassiveFallbackIT` |
| Professionale valida senza personale | Risultato INAD professionale | `DigitalAddressPFMassiveFallbackIT` |
| Personale invalida + professionale valida | Professionale selezionata | `DigitalAddressPFMassiveFallbackIT` |
| INAD vuoto / CF assente -> IniPEC | Request INAD non finale + nuovo IniPEC | `DigitalAddressPFMassiveFallbackIT` |
| Full-flow INAD massive -> IniPEC found | Risultato finale IniPEC | `DigitalAddressPFMassiveFallbackIT` |
| Full-flow INAD massive -> IniPEC NotFound | Risultato finale vuoto | `DigitalAddressPFMassiveFallbackIT` |
| Full-flow INAD massive -> IniPEC `ER` | Retry request IniPEC | `DigitalAddressPFMassiveFallbackIT` |
| IniPEC metadata errore | Retry polling | `DigitalAddressPFMassiveFallbackIT` |
| IniPEC in-progress | Retry in-progress | `DigitalAddressPFMassiveFallbackIT` |
| Errori/retry INAD prima del fallback | Stato/retry coerente | `DigitalAddressPFMassiveFallbackIT` |

---

### 2.9 PF digitale - C8

**Configurazione:** INAD massive, fallback ON, legacy IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| PEC INAD utilizzabile | Risultato finale INAD | `InadPollingPFFallbackOldInipecIT` |
| INAD NotFound -> legacy | Creazione `BatchRequest` legacy | `InadPollingPFFallbackOldInipecIT` |
| Request INAD originale | `WORKED` senza `resultPayload` | `InadPollingPFFallbackOldInipecIT` |
| Full-flow INAD massive -> legacy IniPEC -> risultato | Risultato finale legacy | `InadPollingPFFallbackOldInipecIT` |
| Legacy `ND` / `NF` | Risultato finale vuoto | `InadPollingPFFallbackOldInipecIT` |
| Legacy `ER` | Retry / `ERROR` a soglia | `InadPollingPFFallbackOldInipecIT` |
| Polling legacy error/in-progress | Retry corretto | `InadPollingPFFallbackOldInipecIT` |

---

## 3. PG - Persona giuridica

### 3.1 PG fisico - valido per C1-C8

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| Sede legale valida | `REGISTRO_IMPRESE` con indirizzo | `PhysicalAddressTestIT` |
| Nessuna sede | Risultato senza `physicalAddress` | `PhysicalAddressTestIT` |
| Tutti i campi | Mapping completo | `PhysicalAddressTestIT` |
| Paese estero | Mapping coerente | `PhysicalAddressTestIT` |
| Caratteri accentati | Mapping preservato | `PhysicalAddressTestIT` |
| Sede secondaria | Mapping coerente | `PhysicalAddressTestIT` |
| Errore tecnico | Errore propagato | `PhysicalAddressTestIT` |
| Batch PF/PG misto | Elementi indipendenti | `PhysicalAddressesSyncIT` |

---

### 3.2 PG digitale - C1

**Configurazione:** INAD sync, nuovo IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| IPA PEC valida | Risultato finale IPA | `DigitalAddressPGTestIT` |
| IPA più risultati | Uso WS05 e risultato IPA | `DigitalAddressPGTestIT` |
| IPA NotFound -> IniPEC found | Risultato finale IniPEC | `DigitalAddressPGTestIT` |
| IPA PEC invalida -> IniPEC found | Fallback IniPEC | `DigitalAddressPGTestIT` |
| Errore tecnico IPA | Errore propagato, nessun fallback improprio | `DigitalAddressPGTestIT` |
| IniPEC impresa valida | Risultato finale IniPEC | `DigitalAddressPGTestIT` |
| IniPEC professionisti validi | Risultato finale IniPEC | `DigitalAddressPGTestIT` |
| IniPEC NotFound -> INAD sync | Risultato INAD o vuoto | `DigitalAddressPGTestIT` |
| IniPEC `ND` -> INAD sync | Fallback INAD | `DigitalAddressPGTestIT` |
| IniPEC `NF` -> INAD sync | Fallback INAD | `DigitalAddressPGTestIT` |
| IniPEC CF assente -> INAD sync | Fallback INAD | `DigitalAddressPGTestIT` |
| IniPEC `ER` | Retry request IniPEC | `DigitalAddressPGTestIT` |
| INAD singolo indirizzo valido | Risultato finale INAD | `DigitalAddressPGTestIT` |
| INAD più indirizzi | Risultato vuoto | `DigitalAddressPGTestIT` |
| INAD con professione | Risultato vuoto | `DigitalAddressPGTestIT` |
| INAD PEC invalida | Risultato vuoto | `DigitalAddressPGTestIT` |
| INAD PEC scaduta | Risultato vuoto | `DigitalAddressPGTestIT` |
| INAD una PEC valida + una scaduta | Viene restituita la PEC valida | `DigitalAddressPGTestIT` |
| INAD NotFound | Risultato finale vuoto | `DigitalAddressPGTestIT` |
| Errore tecnico INAD nel fallback | Request finale in errore | `DigitalAddressPGTestIT` |

---

### 3.3 PG digitale - C2

**Configurazione:** INAD sync, legacy IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| IPA valida | Risultato finale IPA | `DigitalAddressPGOldInipecTestIT` |
| IPA NotFound -> legacy IniPEC | Creazione `BatchRequest` legacy | `DigitalAddressPGOldInipecTestIT` |
| Legacy IniPEC valido | Risultato finale IniPEC | `DigitalAddressPGOldInipecTestIT` |
| Legacy NotFound -> INAD sync | Fallback INAD | `DigitalAddressPGOldInipecTestIT` |
| Legacy `ND` / `NF` -> INAD sync | Fallback INAD | `DigitalAddressPGOldInipecTestIT` |
| Legacy missing tax-id -> INAD sync | Fallback INAD | `DigitalAddressPGOldInipecTestIT` |
| Legacy `ER` | Retry request | `DigitalAddressPGOldInipecTestIT` |
| Legacy submit error | Retry coerente | `DigitalAddressPGOldInipecTestIT` |
| Legacy polling error | Retry normale | `DigitalAddressPGOldInipecTestIT` |
| Legacy polling in-progress | Retry in-progress | `DigitalAddressPGOldInipecTestIT` |
| Retry legacy esaurito | Request/polling `ERROR` | `DigitalAddressPGOldInipecTestIT` |
| INAD sync valido | Risultato finale INAD | `DigitalAddressPGOldInipecTestIT` |
| INAD sync NotFound | Risultato finale vuoto | `DigitalAddressPGOldInipecTestIT` |
| INAD sync tecnico KO | Legacy request finale in errore | `DigitalAddressPGOldInipecTestIT` |

---

### 3.4 PG digitale - C3

**Configurazione:** INAD sync, PF fallback ON, nuovo IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| Tutti i casi di C1 | Stesso risultato di C1 | `DigitalAddressPGTestIT` |
| Invarianza rispetto a `pfFallback` | Risultato PG invariato | `DigitalAddressPGTestIT` |

---

### 3.5 PG digitale - C4

**Configurazione:** INAD sync, PF fallback ON, legacy IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| Tutti i casi di C2 | Stesso risultato di C2 | `DigitalAddressPGOldInipecTestIT` |
| Full-flow IPA -> legacy IniPEC -> INAD sync | Risultato finale INAD | `DigitalAddressPGOldInipecTestIT` |
| Invarianza rispetto a `pfFallback` | Risultato PG invariato | `DigitalAddressPGOldInipecTestIT` |

---

### 3.6 PG digitale - C5

**Configurazione:** INAD massive, nuovo IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| IPA valida | Risultato finale IPA | `DigitalAddressPGMassiveIT` |
| IPA NotFound / invalida -> IniPEC | Prosecuzione su IniPEC | `DigitalAddressPGMassiveIT` |
| IniPEC NotFound / `ND` / `NF` -> INAD massive | Creazione request INAD | `DigitalAddressPGMassiveIT` |
| Full-flow IPA -> IniPEC -> INAD massive | Risultato finale INAD | `DigitalAddressPGMassiveIT` |
| INAD PEC valida | Risultato finale INAD | `DigitalAddressPGMassiveIT` |
| INAD PEC invalida | Risultato vuoto | `DigitalAddressPGMassiveIT` |
| INAD più indirizzi | Risultato vuoto | `DigitalAddressPGMassiveIT` |
| INAD con professione | Risultato vuoto | `DigitalAddressPGMassiveIT` |
| INAD PEC scaduta | Risultato vuoto | `DigitalAddressPGMassiveIT` |
| INAD una PEC valida + una scaduta | Viene restituita la PEC valida | `DigitalAddressPGMassiveIT` |
| INAD CF assente | Risultato vuoto | `DigitalAddressPGMassiveIT` |
| Retry normale | `attemptAfter` aggiornato | `DigitalAddressPGMassiveIT` |
| Retry in-progress | `attemptAfter` aggiornato | `DigitalAddressPGMassiveIT` |
| Error retry exhaustion | Request/batch `ERROR` | `DigitalAddressPGMassiveRetryExhaustionIT` |
| In-progress retry exhaustion | Request/batch `ERROR` | `DigitalAddressPGMassiveRetryExhaustionIT` |

---

### 3.7 PG digitale - C6

**Configurazione:** INAD massive, legacy IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| IPA valida | Risultato finale IPA | `InipecLegacyMassiveFlowIT` |
| IPA NotFound -> legacy IniPEC | Flusso legacy | `InipecLegacyMassiveFlowIT` |
| Legacy IniPEC NotFound -> INAD massive | Creazione request `INAD/NOT_WORKED` | `InipecLegacyMassiveFlowIT` |
| Full-flow legacy -> INAD massive | Risultato finale INAD | `InipecLegacyMassiveFlowIT` |
| INAD risultato valido | `resultPayload` finale | `InipecLegacyMassiveFlowIT` |
| INAD risultato inutilizzabile | Risultato finale vuoto | `InipecLegacyMassiveFlowIT` |
| Retry/errori legacy | Retry prima del fallback INAD | `InipecLegacyMassiveFlowIT` |

---

### 3.8 PG digitale - C7

**Configurazione:** INAD massive, PF fallback ON, nuovo IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| Tutti i casi di C5 | Stesso risultato di C5 | `DigitalAddressPGMassiveIT` |
| Invarianza rispetto a `pfFallback` | Risultato PG invariato | `DigitalAddressPGMassiveIT` |

---

### 3.9 PG digitale - C8

**Configurazione:** INAD massive, PF fallback ON, legacy IniPEC.

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| Tutti i casi di C6 | Stesso risultato di C6 | `InipecLegacyMassiveFlowIT` |
| Full-flow IPA -> legacy -> INAD massive | Risultato finale INAD | `InipecLegacyMassiveFlowIT` |
| Retry/errori legacy e INAD | Gestiti nei rispettivi stadi | `InipecLegacyMassiveFlowIT` |
| Invarianza rispetto a `pfFallback` | Risultato PG invariato | `InipecLegacyMassiveFlowIT` |

---

## 4. Flussi digitali condivisi coperti E2E

| Scenario E2E | Esito atteso | Test / classe |
|---|---|---|
| INAD massive: batch + polling fino al risultato | Request finale `WORKED` / `ERROR` | test PF/PG massive |
| IniPEC nuovo: submit + polling fino al risultato | Request finale `WORKED` / `ERROR` | test PF/PG nuovo IniPEC |
| IniPEC legacy: submit + polling fino al risultato | BatchRequest finale `WORKED` / `ERROR` | test PF/PG legacy |
| Retry polling normale | Nuovo tentativo con `attemptAfter` aggiornato | test PF/PG massive/IniPEC |
| Retry polling in-progress | Nuovo tentativo con contatore dedicato | test PF/PG massive/IniPEC |
| Retry request IniPEC `ER` | Request riportata a `NOT_WORKED` / `ERROR` a soglia | test PF/PG IniPEC |
| Bad request nel polling legacy | Errore immediato | test legacy |
| Bad request nel polling nuovo | Request/batch `ERROR` | test massive |
| Risultati finali verificati senza Pipe/SQS | Stato persistito e `resultPayload` | tutti i full-flow nuovi |

---

## 5. Perimetro non E2E / non incluso nel summary

Questo file non include i test tecnici spostati fuori da `integration/`, ad esempio:

- paginazione interna dei batch job;
- max batch size isolato;
- missing polling id isolato;
- conditional update failure isolato;
- classificazione retry HTTP/network;
- configurazione `retry.on-timeout`;
- test isolati di `PollingJob` / `PollingInputsHandler`;
- EventBridge Pipe / SQS / DLQ infrastrutturali.

Tali scenari non sono considerati test E2E finché non vengono verificati all'interno di un flusso completo partendo dal relativo ingresso applicativo.

---

## 6. Stato finale

- `integration/` contiene esclusivamente test end-to-end.
- Ogni scenario qui riportato è associato a un flusso E2E della suite.
- I test tecnici non E2E non sono inclusi in questo riepilogo.
- I flussi nuovi verificano l'esito tramite stato persistito e `resultPayload`.