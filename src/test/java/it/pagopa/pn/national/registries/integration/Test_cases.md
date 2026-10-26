# Test case di integrazione - Scenari funzionali coperti

Questo documento riepiloga gli scenari funzionali coperti dagli integration test presenti nel package `it.pagopa.pn.national.registries.integration` e indica, per ogni scenario, il relativo test.

# Domicili Analogici - PF (ANPR) e PG (Registro Imprese)

| Scenario funzionale                              | Esito atteso                                                                     | Test                                                                           |
|--------------------------------------------------|----------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| PF physical - ANPR                               | Viene restituito l'indirizzo ANPR e il flusso termina senza ulteriori ricerche   | `PhysicalAddressTestIT#PF_Physical_ANPR()`                                     |
| PF physical - ANPR con campi completi estesi     | Vengono mappati anche i dettagli estesi dell'indirizzo                           | `PhysicalAddressTestIT#PF_Physical_ANPR_WithAllAddressDetails()`               |
| PF physical - ANPR con campi parziali            | I campi opzionali non forniti restano null e non vengono valorizzati con default | `PhysicalAddressTestIT#PF_Physical_ANPR_WithPartialAddressDetails()`           |
| PF physical - ANPR con caratteri accentati       | I caratteri accentati vengono preservati end-to-end                              | `PhysicalAddressTestIT#PF_Physical_ANPR_WithAccentedCharacters()`              |
| PF physical - ANPR con civico non standard       | Il mapping gestisce formati civici non convenzionali                             | `PhysicalAddressTestIT#PF_Physical_ANPR_WithNonStandardCivicNumber()`          |
| PF physical - ANPR con strada lunga              | La denominazione stradale viene preservata integralmente                         | `PhysicalAddressTestIT#PF_Physical_ANPR_WithVeryLongStreetName()`              |
| PF physical - ANPR not found                     | Viene pubblicato un output vuoto con registry `ANPR` e nessun indirizzo fisico   | `PhysicalAddressTestIT#PF_Physical_ANPR_NotFound()`                            |
| PF physical - ANPR technical error               | L'errore tecnico viene propagato e non vengono generate risposte positive        | `PhysicalAddressTestIT#PF_Physical_ANPR_error()`                               |
| PG physical - Registro Imprese                   | Viene restituito l'indirizzo fisico del Registro Imprese                         | `PhysicalAddressTestIT#PG_Physical_RegistroImprese()`                          |
| PG physical - Registro Imprese con tutti i campi | Vengono mappati anche CAP e stato estero                                         | `PhysicalAddressTestIT#PG_Physical_RegistroImprese_WithAllFields()`            |
| PG physical - Registro Imprese con paese estero  | Viene gestito correttamente un indirizzo estero                                  | `PhysicalAddressTestIT#PG_Physical_RegistroImprese_WithForeignCountry()`       |
| PG physical - Registro Imprese con accentati     | I caratteri accentati vengono preservati                                         | `PhysicalAddressTestIT#PG_Physical_RegistroImprese_WithAccentedCharacters()`   |
| PG physical - Registro Imprese sede secondaria   | Viene gestito un indirizzo di sede secondaria                                    | `PhysicalAddressTestIT#PG_Physical_RegistroImprese_WithSecondaryAddress()`     |
| Address mapping - formato CAP internazionale UK  | Viene preservato un CAP alfanumerico UK                                          | `PhysicalAddressTestIT#Physical_Address_WithInternationalPostalCode_UK()`      |
| Address mapping - formato CAP internazionale BE  | Viene preservato un CAP internazionale belga                                     | `PhysicalAddressTestIT#Physical_Address_WithInternationalPostalCode_Belgium()` |
| PF physical - ANPR con metrico civico            | Viene mappato il metrico del civico                                              | `PhysicalAddressTestIT#PF_Physical_ANPR_WithMetricoCivicNumber()`              |
| PF physical - ANPR con numero e lettera civico   | Vengono mappati numero e lettera del civico                                      | `PhysicalAddressTestIT#PF_Physical_ANPR_WithCivicNumberAndLetter()`            |
| PF physical - ANPR con SNC                       | Viene gestito un indirizzo senza numero civico                                   | `PhysicalAddressTestIT#PF_Physical_ANPR_WithSnCivic()`                         |
| PF physical - ANPR con colore e scala            | Vengono preservate le informazioni aggiuntive del civico                         | `PhysicalAddressTestIT#PF_Physical_ANPR_WithColorAndScale()`                   |
| PF physical - ANPR solo specie e numero          | Viene gestita la struttura minima dell'indirizzo                                 | `PhysicalAddressTestIT#PF_Physical_ANPR_WithOnlyStreetAndNumber()`             |
| PF physical - ANPR con struttura complessa       | Viene validato il mapping end-to-end della struttura completa                    | `PhysicalAddressTestIT#PF_Physical_ANPR_WithComplexAddressStructure()`         |
| PF physical - ANPR con specie numerica           | Viene gestita una specie/toponimo numerica                                       | `PhysicalAddressTestIT#PF_Physical_ANPR_WithNumericSpecies()`                  |
| PG physical - Registro Imprese not found         | Viene pubblicato un output vuoto con registry `REGISTRO_IMPRESE`                 | `PhysicalAddressTestIT#PG_Physical_RegistroImprese_NotFound()`                 |
| PG physical - Registro Imprese technical error   | L'errore tecnico viene propagato e non vengono generate risposte positive        | `PhysicalAddressTestIT#PG_Physical_RegistroImprese_error()`                    |

# Domicili Digitali PF - INAD sincrono, fallback PF disabilitato

Configurazione principale: `enable-inad-massive-api=false`, `enable-pf-pec-fallback-flow=false`.

| Scenario funzionale                                              | Esito atteso                                                                               | Test                                                                                           |
|------------------------------------------------------------------|--------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| PF - domicilio personale valido                                  | Output INAD con la PEC personale e recipient `PERSONALE`                                   | `DigitalAddressPFTestIT#PF_Digital_INAD()`                                                     |
| PF - CF non trovato su INAD                                      | Output INAD con `digitalAddress` vuoto                                                     | `DigitalAddressPFTestIT#PF_Digital_INAD_NotFound_NoFallback()`                                 |
| PF - solo domicilio professionale valido                         | Output INAD vuoto; il professionale non viene usato con fallback disabilitato              | `DigitalAddressPFTestIT#PF_Digital_INAD_PersonalNotFound_ProfessionalFound_fallbackDisabled()` |
| PF - domicilio personale e professionale validi                  | Viene selezionato il domicilio personale                                                   | `DigitalAddressPFTestIT#PF_Digital_INAD_PersonalInsteadOfProfessional_fallbackDisabled()`      |
| PF - personale sintatticamente non valido e professionale valido | Output INAD vuoto; il professionale non sostituisce il personale                           | `DigitalAddressPFTestIT#PF_Digital_INAD_PersonalInvalid_ProfessionalValid_fallbackDisable()`   |
| PF - più domicili professionali e nessun personale               | Output INAD vuoto                                                                          | `DigitalAddressPFTestIT#PF_Digital_INAD_MultipleProfessionals_fallbackDisabled()`              |
| PF - lista `digitalAddress` vuota                                | Output INAD vuoto                                                                          | `DigitalAddressPFTestIT#PF_Digital_INAD_EmptyList_fallbackDisabled()`                          |
| PF - errore tecnico INAD                                         | L'errore viene propagato, nessun output positivo e nessuna request massiva persistita      | `DigitalAddressPFTestIT#PF_Digital_INAD_Error()`                                               |
| PF - personale scaduta e professionale valido                    | La PEC personale con `usageInfo.dateEndValidity` scaduta viene ignorata; output INAD vuoto | `DigitalAddressPFTestIT#PF_Digital_INAD_PersonalExpired_ProfessionalValid_fallbackDisabled()`  |
| PF - sola PEC personale scaduta                                  | La PEC con `usageInfo.dateEndValidity` scaduta viene ignorata; output INAD vuoto           | `DigitalAddressPFTestIT#PF_Digital_INAD_PersonalExpired_fallbackDisabled()`                    |

# Domicili Digitali PF - INAD massivo end-to-end, fallback PF disabilitato

Il test funzionale attraversa il flusso completo `Gateway -> NationalRegistriesRequest -> InadBatchRequestJob -> InadPollingService -> output` usando un unico batch di 9 request. Il converter INAD è reale; viene mockato il solo client INAD.

| Scenario funzionale                                              | Esito atteso                                                                                | Test                                                                         |
|------------------------------------------------------------------|---------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| PF - domicilio personale valido                                  | Request finale `WORKED` e output con PEC personale                                          | `DigitalAddressPFMassiveIT#PF_Digital_INAD_Massive_AllFunctionalScenarios()` |
| PF - CF assente dalla risposta massiva                           | Request finale `WORKED` e output INAD vuoto                                                 | `DigitalAddressPFMassiveIT#PF_Digital_INAD_Massive_AllFunctionalScenarios()` |
| PF - solo domicilio professionale valido                         | Request finale `WORKED` e output INAD vuoto                                                 | `DigitalAddressPFMassiveIT#PF_Digital_INAD_Massive_AllFunctionalScenarios()` |
| PF - domicilio personale e professionale validi                  | Viene selezionato il domicilio personale                                                    | `DigitalAddressPFMassiveIT#PF_Digital_INAD_Massive_AllFunctionalScenarios()` |
| PF - personale sintatticamente non valido e professionale valido | Output INAD vuoto                                                                           | `DigitalAddressPFMassiveIT#PF_Digital_INAD_Massive_AllFunctionalScenarios()` |
| PF - più domicili professionali e nessun personale               | Output INAD vuoto                                                                           | `DigitalAddressPFMassiveIT#PF_Digital_INAD_Massive_AllFunctionalScenarios()` |
| PF - lista `digitalAddress` vuota                                | Output INAD vuoto                                                                           | `DigitalAddressPFMassiveIT#PF_Digital_INAD_Massive_AllFunctionalScenarios()` |
| PF - personale scaduta e professionale valido                    | La personale con `usageInfo.dateEndValidity` nel passato viene ignorata; output INAD vuoto  | `DigitalAddressPFMassiveIT#PF_Digital_INAD_Massive_AllFunctionalScenarios()` |
| PF - sola PEC personale scaduta                                  | La personale con `usageInfo.dateEndValidity` nel passato viene ignorata; output INAD vuoto  | `DigitalAddressPFMassiveIT#PF_Digital_INAD_Massive_AllFunctionalScenarios()` |
| Errore tecnico durante recupero risultato massivo                | Nessun output; le request rimangono `WORKING` nello stesso batch e l'errore viene propagato | `DigitalAddressPFMassiveIT#PF_Digital_INAD_Massive_Error()`                  |

# INAD Batch Request Job - Scenari funzionali

| Scenario funzionale                                                        | Esito atteso                                                                                                 | Test                                                                                                   |
|----------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| Aggregazione di tutte le request eleggibili attraverso più pagine DynamoDB | Vengono creati i batch attesi; tutte le request passano a `WORKING` e ricevono un `batchId`                  | `InadBatchRequestJobIT#sendsAllEligiblePagesAndAssociatesEachRequestWithItsBatch()`                    |
| Nessuna request INAD pendente                                              | Nessuna chiamata al provider e nessun batch creato                                                           | `InadBatchRequestJobIT#doesNothingWhenThereAreNoPendingInadRequests()`                                 |
| Gruppo incompleto e recente                                                | Nessun invio; request ancora `NOT_WORKED` senza `batchId`                                                    | `InadBatchRequestJobIT#leavesIncompleteRecentGroupAvailableForNextExecution()`                         |
| Batch completo seguito da residuo recente                                  | Viene inviato il batch completo; il residuo resta `NOT_WORKED`                                               | `InadBatchRequestJobIT#stopsAfterAnEligiblePageWhenTheNextIncompletePageIsStillRecent()`               |
| Gruppo incompleto oltre il tempo massimo di attesa                         | Il gruppo viene comunque inviato e la request passa a `WORKING`                                              | `InadBatchRequestJobIT#sendsIncompleteGroupWhenItsOldestRequestExceededTheMaximumWaitingTime()`        |
| Errore generico del client INAD durante submit                             | Errore propagato; nessun batch persistito; request invariate                                                 | `InadBatchRequestJobIT#propagatesInadErrorWithoutPersistingBatchesOrChangingRequests()`                |
| `PnNationalRegistriesException` non `BAD_REQUEST` durante submit           | Errore propagato; nessun batch persistito; request invariate                                                 | `InadBatchRequestJobIT#propagatesNonBadRequestPnExceptionWithoutPersistingBatchesOrChangingRequests()` |
| `BAD_REQUEST` INAD durante submit                                          | Tutte le request coinvolte vengono marcate `ERROR`; nessun batch creato                                      | `InadBatchRequestJobIT#marksAllRequestsAsErrorAndStopsFlowWhenInadReturnsBadRequest()`                 |
| Fallimento aggiornamento condizionale dopo accettazione provider           | L'errore viene propagato; il batch provider resta persistito e la concorrenza sulla request viene preservata | `InadBatchRequestJobIT#propagatesTheConditionalUpdateFailureAfterTheProviderAcceptedTheBatch()`        |
| Risposta INAD senza polling identifier                                     | `PnInternalException`; nessun batch persistito; request restano `NOT_WORKED`                                 | `InadBatchRequestJobIT#rejectsInadResponseWithoutPollingIdentifierWithoutPersistingChanges()`          |

# INAD Polling - fallback PF disabilitato

| Scenario funzionale                                | Esito atteso                                                       | Test                                                                               |
|----------------------------------------------------|--------------------------------------------------------------------|------------------------------------------------------------------------------------|
| PF, solo domicilio personale valido                | Request INAD `WORKED` con domicilio personale                      | `InadPollingIT#shouldTakePersonalWhenOnlyPersonalExists()`                         |
| PF, domicilio personale e professionale validi     | Seleziona il domicilio personale                                   | `InadPollingIT#shouldPreferPersonalWhenPersonalAndProfessionalExist()`             |
| PF, solo domicilio professionale valido            | Request `WORKED` con payload senza indirizzi                       | `InadPollingIT#shouldReturnNotFoundWhenOnlyProfessionalExists()`                   |
| PF, personale non valido e professionale valido    | Request `WORKED` con payload senza indirizzi                       | `InadPollingIT#shouldReturnNotFoundWhenPersonalInvalidAndProfessionalValid()`      |
| PF, personale valido e professionale non valido    | Seleziona il domicilio personale                                   | `InadPollingIT#shouldTakePersonalWhenPersonalValidAndProfessionalInvalid()`        |
| PF, personale e professionale entrambi non validi  | Request `WORKED` con payload senza indirizzi                       | `InadPollingIT#shouldReturnNotFoundWhenPersonalAndProfessionalInvalid()`           |
| PF, lista `digitalAddress` vuota                   | Request `WORKED` con payload senza indirizzi                       | `InadPollingIT#shouldReturnNotFoundWhenDigitalAddressListIsEmpty()`                |
| PF, CF assente dalla risposta massiva              | Request `WORKED` con payload senza indirizzi                       | `InadPollingIT#shouldReturnNotFoundWhenTaxIdIsMissingFromMassiveResponse()`        |
| PF, più professionali e nessun personale           | Request `WORKED` con payload senza indirizzi                       | `InadPollingIT#shouldReturnNotFoundWhenMultipleProfessionalsExistAndNoPersonal()`  |
| Polling INAD `PRESA_IN_CARICO` / `IN_ELABORAZIONE` | Batch resta schedulato e viene incrementato `inProgressRetryCount` | `InadPollingIT#shouldScheduleInProgressRetryWithoutTouchingRequests()`             |
| Errore retryable durante verifica stato            | Viene schedulato un retry normale senza finalizzare le request     | `InadPollingIT#shouldScheduleNormalRetryOnStatusError()`                           |
| Errore retryable durante recupero risultato        | Viene schedulato un retry normale senza finalizzare le request     | `InadPollingIT#shouldScheduleNormalRetryOnResultError()`                           |
| Retry esauriti / bad request                       | Request e batch vengono finalizzati in `ERROR` secondo il caso     | `InadPollingIT#shouldFinalizeRequestsAndBatchOnRetryExhaustionOrBadRequest()`      |
| Errore non gestito / infrastrutturale              | Errore propagato e stato applicativo lasciato invariato            | `InadPollingIT#shouldPropagateUnhandledConsumerErrorAndLeaveStateUntouched()`      |
| PG, un solo domicilio senza `practicedProfession`  | PEC accettata e request `WORKED`                                   | `InadPollingIT#shouldTakePecForPgWhenSingleAddressWithoutPracticedProfession()`    |
| PG, un solo domicilio con `practicedProfession`    | Payload senza indirizzi                                            | `InadPollingIT#shouldReturnNotFoundForPgWhenSingleAddressHasPracticedProfession()` |
| PG, più domicili                                   | Payload senza indirizzi                                            | `InadPollingIT#shouldReturnNotFoundForPgWhenMultipleAddressesExist()`              |
| PG, PEC sintatticamente non valida                 | Payload senza indirizzi                                            | `InadPollingIT#shouldReturnNotFoundForPgWhenSingleAddressIsInvalid()`              |
| PG, lista indirizzi vuota                          | Payload senza indirizzi                                            | `InadPollingIT#shouldReturnNotFoundForPgWhenAddressListIsEmpty()`                  |
| PG, CF assente dalla risposta massiva              | Payload senza indirizzi                                            | `InadPollingIT#shouldReturnNotFoundForPgWhenTaxIdIsMissingFromMassiveResponse()`   |

# INAD Polling - fallback PF abilitato

| Scenario funzionale                                | Esito atteso                                                       | Test                                                                                          |
|----------------------------------------------------|--------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| PF, solo domicilio personale valido                | Seleziona il domicilio personale INAD                              | `InadPollingPFFallbackIT#shouldTakePersonalWhenOnlyPersonalExists()`                          |
| PF, domicilio personale e professionale validi     | Seleziona il domicilio personale INAD                              | `InadPollingPFFallbackIT#shouldPreferPersonalWhenPersonalAndProfessionalExist()`              |
| PF, solo domicilio professionale valido            | Seleziona il domicilio professionale INAD                          | `InadPollingPFFallbackIT#shouldTakeProfessionalWhenOnlyProfessionalExists()`                  |
| PF, personale non valido e professionale valido    | Seleziona il domicilio professionale INAD                          | `InadPollingPFFallbackIT#shouldTakeProfessionalWhenPersonalInvalidAndProfessionalValid()`     |
| PF, personale valido e professionale non valido    | Seleziona il domicilio personale INAD                              | `InadPollingPFFallbackIT#shouldTakePersonalWhenPersonalValidAndProfessionalInvalid()`         |
| PF, personale e professionale entrambi non validi  | INAD non finale e nuova request INIPEC `NOT_WORKED`                | `InadPollingPFFallbackIT#shouldFallbackToInipecWhenPersonalAndProfessionalInvalid()`          |
| PF, lista `digitalAddress` vuota                   | Fallback a IniPEC                                                  | `InadPollingPFFallbackIT#shouldFallbackToInipecWhenDigitalAddressListIsEmpty()`               |
| PF, CF assente dalla risposta massiva              | Fallback a IniPEC                                                  | `InadPollingPFFallbackIT#shouldFallbackToInipecWhenTaxIdIsMissingFromMassiveResponse()`       |
| PF, più professionali e nessun personale           | Seleziona un domicilio professionale INAD                          | `InadPollingPFFallbackIT#shouldTakeProfessionalWhenMultipleProfessionalsExistAndNoPersonal()` |
| Polling INAD `PRESA_IN_CARICO` / `IN_ELABORAZIONE` | Batch resta schedulato e viene incrementato `inProgressRetryCount` | `InadPollingPFFallbackIT#shouldScheduleInProgressRetryWithoutTouchingRequests()`              |
| Errore retryable durante verifica stato            | Viene schedulato un retry normale                                  | `InadPollingPFFallbackIT#shouldScheduleNormalRetryOnStatusError()`                            |
| Errore retryable durante recupero risultato        | Viene schedulato un retry normale                                  | `InadPollingPFFallbackIT#shouldScheduleNormalRetryOnResultError()`                            |
| Retry esauriti / bad request                       | Request e batch vengono finalizzati in `ERROR` secondo il caso     | `InadPollingPFFallbackIT#shouldFinalizeRequestsAndBatchOnRetryExhaustionOrBadRequest()`       |
| Errore non gestito / infrastrutturale              | Errore propagato e stato applicativo lasciato invariato            | `InadPollingPFFallbackIT#shouldPropagateUnhandledConsumerErrorAndLeaveStateUntouched()`       |
| PG, un solo domicilio senza `practicedProfession`  | PEC accettata e request `WORKED`                                   | `InadPollingPFFallbackIT#shouldTakePecForPgWhenSingleAddressWithoutPracticedProfession()`     |
| PG, un solo domicilio con `practicedProfession`    | Payload senza indirizzi; nessun fallback PF                        | `InadPollingPFFallbackIT#shouldReturnNotFoundForPgWhenSingleAddressHasPracticedProfession()`  |
| PG, più domicili                                   | Payload senza indirizzi                                            | `InadPollingPFFallbackIT#shouldReturnNotFoundForPgWhenMultipleAddressesExist()`               |
| PG, PEC sintatticamente non valida                 | Payload senza indirizzi                                            | `InadPollingPFFallbackIT#shouldReturnNotFoundForPgWhenSingleAddressIsInvalid()`               |
| PG, lista indirizzi vuota                          | Payload senza indirizzi                                            | `InadPollingPFFallbackIT#shouldReturnNotFoundForPgWhenAddressListIsEmpty()`                   |
| PG, CF assente dalla risposta massiva              | Payload senza indirizzi                                            | `InadPollingPFFallbackIT#shouldReturnNotFoundForPgWhenTaxIdIsMissingFromMassiveResponse()`    |

# IniPEC Polling - fallback INAD sincrono

| Scenario funzionale                                                                                             | Esito atteso                                                                                 | Test                                                                            |
|-----------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| PF - insieme degli outcome IniPEC: PEC valida, invalida, vuota, `ND`, `NF`, `ER`                                | Per PF gli esiti non validi/non trovati sono finali IniPEC; `ER` resta retryable             | `InipecPollingIT#shouldHandleAllInipecOutcomesForPf()`                          |
| PG - insieme degli outcome IniPEC: PEC valida, invalida, vuota, `ND`, `NF`, `ER`, fallback INAD not found/error | Gli esiti che richiedono fallback passano a INAD sync; `ER` resta retryable                  | `InipecPollingIT#shouldHandleAllInipecOutcomesForPg()`                          |
| Retry polling normale e in-progress                                                                             | I due contatori vengono aggiornati senza modificare le request applicative                   | `InipecPollingIT#shouldScheduleBothPollingRetryKindsWithoutTouchingRequests()`  |
| Retry esauriti / bad request                                                                                    | Request e batch vengono finalizzati in `ERROR` secondo il caso                               | `InipecPollingIT#shouldFinalizeRequestsAndBatchOnRetryExhaustionOrBadRequest()` |
| Metadata di errore nella risposta InfoCamere                                                                    | Trattato come polling retryable, senza processamento come successo                           | `InipecPollingIT#shouldTreatInfoCamereErrorMetadataAsNormalPollingRetry()`      |
| Errore consumer non gestito                                                                                     | Trattato come errore di polling e sottoposto al comportamento di retry previsto dal servizio | `InipecPollingIT#shouldTreatUnhandledConsumerErrorAsNormalPollingRetry()`       |

# IniPEC Polling - fallback verso INAD massivo

| Scenario funzionale                                          | Esito atteso                                                                                    | Test                                                                                       |
|--------------------------------------------------------------|-------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| PF - insieme degli outcome IniPEC con INAD massivo abilitato | Gli esiti PF restano gestiti secondo le regole IniPEC senza fallback INAD non previsto          | `InipecPollingInadMassiveIT#shouldHandleAllInipecOutcomesForPf()`                          |
| PG - outcome IniPEC che richiedono fallback                  | Viene creata una nuova `NationalRegistriesRequest` INAD `NOT_WORKED` invece della chiamata sync | `InipecPollingInadMassiveIT#shouldHandleAllInipecOutcomesForPg()`                          |
| Retry polling normale e in-progress                          | I due contatori vengono aggiornati senza modificare le request applicative                      | `InipecPollingInadMassiveIT#shouldScheduleBothPollingRetryKindsWithoutTouchingRequests()`  |
| Retry esauriti / bad request                                 | Request e batch vengono finalizzati in `ERROR` secondo il caso                                  | `InipecPollingInadMassiveIT#shouldFinalizeRequestsAndBatchOnRetryExhaustionOrBadRequest()` |
| Metadata di errore nella risposta InfoCamere                 | Trattato come polling retryable                                                                 | `InipecPollingInadMassiveIT#shouldTreatInfoCamereErrorMetadataAsNormalPollingRetry()`      |
| Errore consumer non gestito                                  | Trattato come errore di polling secondo la logica di retry del servizio                         | `InipecPollingInadMassiveIT#shouldTreatUnhandledConsumerErrorAsNormalPollingRetry()`       |

# Regole funzionali PF INAD

## Fallback PF disabilitato

```text
domicilio personale INAD valido e non scaduto
        ↓ no
NotFound INAD / output vuoto
```

Un domicilio professionale non sostituisce il personale quando `enable-pf-pec-fallback-flow=false`. Una PEC personale con `usageInfo.dateEndValidity` antecedente alla data di riferimento viene trattata come non utilizzabile.

## Fallback PF abilitato

```text
domicilio personale INAD valido e non scaduto
        ↓ no
domicilio professionale INAD valido
        ↓ no
IniPEC
```


| Priorità | Scenario non coperto / coperto solo parzialmente                           | Perché serve                                                                                                                      | Test che aggiungerei                                          |
|----------|----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| **P0**   | **PG DIGITAL: IPA trova PEC valida**                                       | Non vedo nessun test gateway PG digitale. Deve terminare immediatamente su IPA senza creare request IniPEC                        | `PG_Digital_IPA()`                                            |
| **P0**   | **PG DIGITAL: IPA non trova PEC → IniPEC**                                 | È il percorso principale `IPA -> INIPEC` e attualmente non è E2E                                                                  | `PG_Digital_IPA_NotFound_InipecFallback()`                    |
| **P0**   | **PG DIGITAL: IPA restituisce PEC non valida → IniPEC**                    | `DigitalAddressService` considera NotFound anche una PEC sintatticamente invalida                                                 | `PG_Digital_IPA_InvalidPec_InipecFallback()`                  |
| **P0**   | **IPA WS23 con più risultati → chiamata WS05**                             | `IpaService` ha un ramo specifico `numItems > 1` che non risulta coperto funzionalmente                                           | `PG_Digital_IPA_MultipleResults_UsesWS05()`                   |
| **P0**   | **PF sync con `enable-pf-pec-fallback-flow=true`: INAD NotFound → IniPEC** | Hai testato bene il polling con fallback, ma non l'ingresso reale dal gateway                                                     | `PF_Digital_INAD_NotFound_InipecFallback()`                   |
| **P0**   | **PF fallback=true: personale scaduta + professionale valida**             | I nuovi test di scadenza sono solo con fallback disabilitato. Con fallback=true il risultato deve essere la professionale         | `shouldTakeProfessionalWhenPersonalExpired()`                 |
| **P0**   | **PF fallback=true: solo personale scaduta**                               | Dopo il filtro temporale non rimane nessun INAD valido e deve partire fallback IniPEC                                             | `shouldFallbackToInipecWhenPersonalExpired()`                 |
| **P1**   | **IniPEC: CF completamente assente dalla risposta massiva**                | `InipecPollingService` ha un ramo esplicito `pec == null`; i test coprono ND/NF/empty PEC ma non “CF non presente nella response” | `shouldHandleMissingTaxIdFromInipecResponse()`                |
| **P1**   | **IniPEC `pecProfessionista`**                                             | `InfoCamereConverter` gestisce `getPecProfessionista()`, ma nei test vedo solo `pecImpresa`                                       | `shouldReturnProfessionalPecFromInipec()`                     |
| **P1**   | **IniPEC con PEC impresa + professionista / una valida e una invalida**    | Verifica realmente `removeInvalidEmails()` su una risposta multi-indirizzo                                                        | `shouldKeepOnlyValidInipecAddresses()`                        |
| **P1**   | **ER IniPEC fino al max retry della singola request**                      | Oggi controlli ER al primo retry; non vedo il ramo `retry >= batchRequestMaxRetry` che porta la singola request a `ERROR`         | `shouldMarkInipecRequestAsErrorWhenRequestRetryIsExhausted()` |
| **P2**   | **INAD polling con status nullo**                                          | `InadPollingService` genera `empty polling response`; non vedo un test specifico                                                  | `shouldRejectEmptyInadPollingStatus()`                        |
| **P2**   | **INAD PF con due domicili personali validi**                              | `retrievePersonalAddress()` prende il primo; oggi non verifichi questa scelta                                                     | `shouldTakeFirstPersonalWhenMultiplePersonalExist()`          |
| **P2**   | **INAD PG con PEC scaduta**                                                | Il filtro `dateEndValidity` è comune PF/PG, ma la scadenza è testata solo per PF                                                  | `shouldReturnNotFoundForPgWhenAddressExpired()`               |
