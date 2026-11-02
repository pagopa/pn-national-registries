package it.pagopa.pn.national.registries.integration.digital.PF;

import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.integration.AbstractNationalRegistriesComponentIT;
import it.pagopa.pn.national.registries.integration.PnNationalRegistriesTestApplication;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.PollingInputsHandler;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.service.InadPollingService;
import it.pagopa.pn.national.registries.service.IniPecBatchSqsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali PF DIGITAL.
 *
 * Configurazione:
 *  - fallback PF attivo
 *  - INAD massivo attivo
 *  - old IniPEC attivo
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-inad-massive-api=true",
        "pn.national-registries.enable-old-inipec-flow=true",
        "pn.national-registries.inad.max-batch-request-size=9",
        "pn.national-registries.query-limit=2",
        "pn.national-registries.inad.oldest-request-max-waiting-seconds=0",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inad.batch-polling-max-retry=5",
        "pn.national-registries.inad.batch-polling-in-progress-max-retry=10",
        "pn.national-registries.inad.polling-error-retry-after=1",
        "pn.national-registries.inad.polling-in-progress-retry-after=1",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.batch-request-max-retry=1",
        "pn.national-registries.inipec.batch.polling.max-retry=2",
        "pn.national-registries.inipec.batch.polling.inprogress.max-retry=2",
        "pn.national-registries.inipec.batch.polling.recovery.after=0",
        "pn.national-registries.inipec.batch-polling-max-retry=2",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=2",
        "pn.national-registries.inipec.batch-request-recovery-after=0",
        "spring.task.scheduling.enabled=false"
})
public class DigitalAddressPFMassiveFallbackOldInipecIT extends AbstractNationalRegistriesComponentIT {

    private static final String PF_FOUND = "KRSJSM88S03H501A";
    private static final String PF_NOT_FOUND = "JSMKRV95P54H501D";
    private static final String PF_PROFESSIONAL_ONLY = "KPRSMP91H12F205O";
    private static final String PF_PERSONAL_AND_PROFESSIONAL = "VRDMRA77E66I700E";
    private static final String PF_INVALID_PERSONAL = "NRELCU80E53H501M";
    private static final String PF_MULTIPLE_PROFESSIONALS = "NREMRA09A27F205P";
    private static final String PF_EMPTY = "BSMGPR92R62F205X";
    private static final String PF_EXPIRED_PERSONAL_AND_PROFESSIONAL = "DCNMRM85M49H501S";
    private static final String PF_EXPIRED_PERSONAL = "XLPJSM93B21H501O";
    private static final String PF_PROFESSIONAL = "WDSTKB60E09L538U";
    private static final String PF_BUSINESS_PROFESSIONAL = "PRPMGG80A41H501W";
    private static final String PF_INVALID = "FLPCPT69A65Z336P";
    private static final String PF_ND = "DLGMRA73D41F839C";
    private static final String PF_NF = "PRMRGG74T16H501R";
    private static final String PF_MISSING = "VDADGI80A01F839E";
    private static final String PF_ER = "RSSSRG80A01F839I";

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private InadPollingService inadPollingService;

    @Autowired
    private NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;

    @Autowired
    private PollingInputsHandler pollingInputsHandler;

    @Autowired
    private IniPecBatchSqsService iniPecBatchSqsService;

    @MockitoSpyBean
    private IniPecBatchRequestRepository iniPecBatchRequestRepository;


    /**
     * Inad pec personale valida.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalFound_NoInipecFallback() {
        String correlationId = "pf-massive-found-fallback-" + UUID.randomUUID();
        CodeSqsDto payload = runMassiveInadResolvedScenario(correlationId, PF_FOUND, inadResponse(PF_FOUND, "pf@pec.it", null));
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("pf@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad NotFound: il CF richiesto non compare nel risultato massivo e parte il fallback IniPEC.
     */
    @Test
    void PF_Digital_INAD_Massive_NotFound_InipecFallback() {
        String correlationId = "pf-massive-not-found-fallback-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, PF_NOT_FOUND, null);
        CodeSqsDto payload = completeLegacyFinalFromMassive(correlationId, PF_NOT_FOUND, "inipec-not-found-fallback@pec.it");
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("inipec-not-found-fallback@pec.it", payload.getDigitalAddress().getFirst().getAddress());
    }

    /**
     * Inad risposta con lista PEC vuota e fallback IniPEC.
     */
    @Test
    void PF_Digital_INAD_Massive_EmptyList_InipecFallback() {
        String correlationId = "pf-massive-empty-list-fallback-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, PF_EMPTY, inadResponse(PF_EMPTY, List.of()));
        CodeSqsDto payload = completeLegacyFinalFromMassive(correlationId, PF_EMPTY, "inipec-after-empty-list@pec.it");
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("inipec-after-empty-list@pec.it", payload.getDigitalAddress().getFirst().getAddress());
    }

    /**
     * Inad pec personale scaduta e fallback IniPEC.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalExpired_InipecFallback() {
        String correlationId = "pf-massive-expired-personal-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        startLegacyFallbackFromMassive(correlationId, PF_EXPIRED_PERSONAL, inadResponse(PF_EXPIRED_PERSONAL, List.of(expiredPersonal)));
        CodeSqsDto payload = completeLegacyFinalFromMassive(correlationId, PF_EXPIRED_PERSONAL, "inipec-after-expired-personal@pec.it");
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("inipec-after-expired-personal@pec.it", payload.getDigitalAddress().getFirst().getAddress());
    }

    /**
     * Inad pec personale senza usageInfo.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalWithoutUsageInfo_IsValid() {
        String correlationId = "pf-massive-personal-no-usage-info-fallback-" + UUID.randomUUID();
        ElementDigitalAddress personal = new ElementDigitalAddress();
        personal.setDigitalAddress("personal-no-usage@pec.it");
        personal.setPracticedProfession(null);
        CodeSqsDto payload = runMassiveInadResolvedScenario(correlationId, PF_FOUND, inadResponse(PF_FOUND, List.of(personal)));
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("personal-no-usage@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad pec personale senza data fine validita.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalWithoutEndValidity_IsValid() {
        String correlationId = "pf-massive-personal-no-end-validity-fallback-" + UUID.randomUUID();
        ElementDigitalAddress personal = inadAddress("personal-no-end@pec.it", null);
        personal.getUsageInfo().setDateEndValidity(null);
        CodeSqsDto payload = runMassiveInadResolvedScenario(correlationId, PF_FOUND, inadResponse(PF_FOUND, List.of(personal)));
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("personal-no-end@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Con piu PEC personali viene ignorata quella scaduta e selezionata la prima valida.
     */
    @Test
    void PF_Digital_INAD_Massive_FirstPersonalInvalid_SecondPersonalValid_SecondIsSelected() {
        String correlationId = "pf-massive-first-personal-invalid-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("first-expired@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        ResponseRequestDigitalAddress result = inadResponse(PF_FOUND, List.of(expiredPersonal, inadAddress("second-valid@pec.it", null)));
        CodeSqsDto payload = runMassiveInadResolvedScenario(correlationId, PF_FOUND, result);
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("second-valid@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad personale presente e professionale presente: viene preferita la personale.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalInsteadOfProfessional_FallbackEnabled() {
        String correlationId = "pf-massive-personal-preferred-fallback-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_PERSONAL_AND_PROFESSIONAL, List.of(inadAddress("personal@pec.it", null), inadAddress("professional@pec.it", "INGEGNERE")));
        CodeSqsDto payload = runMassiveInadResolvedScenario(correlationId, PF_PERSONAL_AND_PROFESSIONAL, result);
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("personal@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad personale assente e professionale presente: viene usata la professionale prima del fallback.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalNotFound_ProfessionalFound_FallbackEnabled() {
        String correlationId = "pf-massive-professional-only-fallback-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_PROFESSIONAL_ONLY, List.of(inadAddress("professional@pec.it", "INGEGNERE")));
        CodeSqsDto payload = runMassiveInadResolvedScenario(correlationId, PF_PROFESSIONAL_ONLY, result);
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad personale invalida e professionale valida: viene usata la professionale prima del fallback.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalInvalid_ProfessionalValid_FallbackEnabled() {
        String correlationId = "pf-massive-invalid-personal-professional-fallback-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_INVALID_PERSONAL, List.of(inadAddress("invalid-personal", null), inadAddress("professional@pec.it", "INGEGNERE")));
        CodeSqsDto payload = runMassiveInadResolvedScenario(correlationId, PF_INVALID_PERSONAL, result);
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad personale assente e piu PEC professionali valide: viene usata la prima professionale valida.
     */
    @Test
    void PF_Digital_INAD_Massive_MultipleProfessionals_FallbackEnabled() {
        String correlationId = "pf-massive-multiple-professionals-fallback-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_MULTIPLE_PROFESSIONALS, List.of(inadAddress("prof1@pec.it", "INGEGNERE"), inadAddress("prof2@pec.it", "ARCHITETTO")));
        CodeSqsDto payload = runMassiveInadResolvedScenario(correlationId, PF_MULTIPLE_PROFESSIONALS, result);
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("prof1@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Errore tecnico INAD massivo: programma il retry del batch e non crea fallback IniPEC.
     */
    @Test
    void PF_Digital_INAD_Massive_TechnicalError_NoInipecFallback() {
        List<TestRequest> requests = createNineRequests("pf-error");
        enqueueMassiveRequests(requests);
        String pollingId = "polling-error-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);
        pollingJob.polling();
        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.error(new RuntimeException("INAD unavailable")));
        pollInad(batchId, pollingId, 9);
        NationalRegistriesBatch batch = getBatch(batchId);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(1, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());
        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = requestByRegistry(testRequest.correlationId(), "INAD");
            assertEquals("WORKING", stored.get("status").s());
            assertNull(stored.get("resultPayload"));
            assertTrue(requestsByRegistry(testRequest.correlationId(), "INIPEC").isEmpty());
        }
    }

    /**
     * Inad pec personale scaduta e professionale valida: viene usata la professionale.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalExpired_ProfessionalValid_FallbackEnabled() {
        String correlationId = "pf-massive-expired-personal-professional-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        ResponseRequestDigitalAddress result = inadResponse(PF_EXPIRED_PERSONAL_AND_PROFESSIONAL, List.of(expiredPersonal, inadAddress("professional@pec.it", "INGEGNERE")));
        CodeSqsDto payload = runMassiveInadResolvedScenario(correlationId, PF_EXPIRED_PERSONAL_AND_PROFESSIONAL, result);
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad solo professionale scaduta: nessun candidato utilizzabile e fallback IniPEC.
     */
    @Test
    void PF_Digital_INAD_Massive_OnlyProfessionalExpired_InipecFallback() {
        String correlationId = "pf-massive-only-professional-expired-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredProfessional = inadAddress("expired-professional@pec.it", "AVVOCATO");
        expiredProfessional.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        startLegacyFallbackFromMassive(correlationId, PF_PROFESSIONAL_ONLY, inadResponse(PF_PROFESSIONAL_ONLY, List.of(expiredProfessional)));
        CodeSqsDto payload = completeLegacyFinalFromMassive(correlationId, PF_PROFESSIONAL_ONLY, "inipec-after-expired-professional@pec.it");
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("inipec-after-expired-professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
    }

    /**
     * INAD NotFound -> old IniPEC con PEC professionale valida.
     */
    @Test
    void PF_Digital_FullFlow_InipecProfessionalPecIsReturned() {
        String correlationId = "pf-old-inipec-professional-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, PF_PROFESSIONAL, null);

        CodeSqsDto payload = completeLegacyFinal(correlationId, PF_PROFESSIONAL,
                pecResponseWithProfessionals(PF_PROFESSIONAL, null, "professional@pec.it"));
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * INAD NotFound -> old IniPEC con PEC impresa e tutte le PEC professionali valide.
     */
    @Test
    void PF_Digital_FullFlow_InipecBusinessAndAllProfessionalPecsAreReturned() {
        String correlationId = "pf-old-inipec-business-professionals-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, PF_BUSINESS_PROFESSIONAL, null);

        CodeSqsDto payload = completeLegacyFinal(correlationId, PF_BUSINESS_PROFESSIONAL,
                pecResponseWithProfessionals(PF_BUSINESS_PROFESSIONAL, "business@pec.it", "professional-one@pec.it", "professional-two@pec.it"));
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals(3, payload.getDigitalAddress().size());
        assertTrue(payload.getDigitalAddress().stream().anyMatch(address -> "business@pec.it".equals(address.getAddress()) && "IMPRESA".equals(address.getRecipient())));
        assertTrue(payload.getDigitalAddress().stream().anyMatch(address -> "professional-one@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        assertTrue(payload.getDigitalAddress().stream().anyMatch(address -> "professional-two@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
    }

    /**
     * INAD NotFound -> old IniPEC con PEC non valida.
     */
    @Test
    void PF_Digital_FullFlow_InipecInvalidPecProducesEmptyFinalResult() {
        String correlationId = "pf-old-inipec-invalid-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, PF_INVALID, null);

        CodeSqsDto payload = completeLegacyFinal(correlationId, PF_INVALID, pecResponse(PF_INVALID, "not-an-email"));
        assertEquals("INIPEC", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> old IniPEC con stato ND.
     */
    @Test
    void PF_Digital_FullFlow_InipecNdProducesEmptyFinalResult() {
        String correlationId = "pf-old-inipec-nd-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, PF_ND, null);

        Pec nd = pecResponse(PF_ND, "ignored-nd@pec.it");
        nd.setStatoImpresa(Pec.StatoImpresaEnum.ND);
        CodeSqsDto payload = completeLegacyFinal(correlationId, PF_ND, nd);
        assertEquals("INIPEC", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> old IniPEC con stato NF.
     */
    @Test
    void PF_Digital_FullFlow_InipecNfProducesEmptyFinalResult() {
        String correlationId = "pf-old-inipec-nf-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, PF_NF, null);

        Pec nf = pecResponse(PF_NF, "ignored-nf@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        CodeSqsDto payload = completeLegacyFinal(correlationId, PF_NF, nf);
        assertEquals("INIPEC", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> old IniPEC quando la risposta non contiene il codice fiscale richiesto.
     */
    @Test
    void PF_Digital_FullFlow_InipecMissingTaxIdProducesEmptyFinalResult() {
        String correlationId = "pf-old-inipec-missing-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, PF_MISSING, null);

        CodeSqsDto payload = completeLegacyFinal(correlationId, PF_MISSING);
        assertEquals("INIPEC", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> old IniPEC con stato ER fino al limite configurato.
     */
    @Test
    void PF_Digital_FullFlow_InipecErRetriesThenEndsInErrorAtConfiguredLimit() {
        String correlationId = "pf-old-inipec-er-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, PF_ER, null);

        String pollingId = "pf-old-er-polling-" + UUID.randomUUID();
        submitLegacyBatch(correlationId, pollingId);
        Pec er = pecResponse(PF_ER, "ignored@pec.it");
        er.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(er)));
        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertEquals("1", request.get("retry").n());
        assertNull(request.get("message"));
    }

    /**
     * INAD NotFound -> old IniPEC con polling in-progress.
     */
    @Test
    void PF_Digital_FullFlow_InipecPollingInProgressSchedulesRetry() {
        String correlationId = "pf-old-inipec-in-progress-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, VALID_PF_CF, null);

        String pollingId = "pf-old-in-progress-polling-" + UUID.randomUUID();
        String batchId = submitLegacyBatch(correlationId, pollingId);
        PnNationalRegistriesException inProgress = new PnNationalRegistriesException(
                "List PEC in progress", HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(inProgress));
        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        assertEquals("WORKING", polling.get("status").s());
        assertEquals("1", polling.get("inProgressRetry").n());
        assertEquals("WORKING", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    /**
     * INAD NotFound -> old IniPEC quando il polling restituisce metadata di errore.
     */
    @Test
    void PF_Digital_FullFlow_InipecErrorMetadataSchedulesRetry() {
        String correlationId = "pf-old-inipec-metadata-error-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, VALID_PF_CF, null);

        String pollingId = "pf-old-metadata-polling-" + UUID.randomUUID();
        String batchId = submitLegacyBatch(correlationId, pollingId);
        IniPecPollingResponse pollingResponse = new IniPecPollingResponse();
        pollingResponse.setCode("123");
        pollingResponse.setDescription("temporary error");
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));
        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        assertEquals("WORKING", polling.get("status").s());
        assertEquals("1", polling.get("retry").n());
        assertEquals("WORKING", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    /**
     * INAD massive senza domicilio utilizzabile -> old IniPEC con piu PEC professionali valide e nessuna PEC impresa.
     */
    @Test
    void PF_Digital_FullFlow_Inipec_AllProfessionalPecsArePreserved() {
        String correlationId = "pf-massive-old-inipec-professionals-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, PF_PROFESSIONAL, null);
        CodeSqsDto payload = completeLegacyFinal(correlationId, PF_PROFESSIONAL, pecResponseWithProfessionals(PF_PROFESSIONAL, null, "professional-one@pec.it", "professional-two@pec.it"));
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals(2, payload.getDigitalAddress().size());
        assertTrue(payload.getDigitalAddress().stream().anyMatch(address -> "professional-one@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        assertTrue(payload.getDigitalAddress().stream().anyMatch(address -> "professional-two@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
    }

    /**
     * INAD massive senza domicilio utilizzabile -> old IniPEC con PEC impresa invalida e PEC professionale valida.
     */
    @Test
    void PF_Digital_FullFlow_Inipec_InvalidBusinessPecKeepsValidProfessionalPec() {
        String correlationId = "pf-massive-old-inipec-invalid-business-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, PF_BUSINESS_PROFESSIONAL, null);
        CodeSqsDto payload = completeLegacyFinal(correlationId, PF_BUSINESS_PROFESSIONAL, pecResponseWithProfessionals(PF_BUSINESS_PROFESSIONAL, "not-an-email", "professional@pec.it"));
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Verifica che, quando INAD restituisce piu risultati per lo stesso codice fiscale,
     * venga mantenuto il primo risultato restituito dal provider.
     */
    @Test
    void PF_Digital_INAD_Massive_DuplicateTaxIdKeepsFirstProviderResult() {
        List<TestRequest> requests = createNineRequests("duplicate-taxid");
        enqueueMassiveRequests(requests);
        String pollingId = "polling-duplicate-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);
        pollingJob.polling();
        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        List<ResponseRequestDigitalAddress> providerResults = new ArrayList<>();
        providerResults.add(inadResponse(requests.getFirst().taxId(), "first@pec.it", null));
        providerResults.add(inadResponse(requests.getFirst().taxId(), "second@pec.it", null));
        for (int i = 1; i < requests.size(); i++) {
            providerResults.add(inadResponse(requests.get(i).taxId(), "duplicate-filler-" + i + "@pec.it", null));
        }
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(providerResults);
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(result));
        pollInad(batchId, pollingId, 9);
        Map<String, AttributeValue> stored = requestByRegistry(requests.getFirst().correlationId(), "INAD");
        CodeSqsDto payload = readOutputMessage(stored.get("resultPayload").s());
        assertEquals("first@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertFalse(stored.get("resultPayload").s().contains("second@pec.it"));
    }

    /**
     * Simula il caso in cui il provider abbia accettato il batch ma la persistenza locale
     * del batch e dell'associazione delle request fallisca.
     * Al ciclo successivo le request ancora NOT_WORKED vengono riprese e inviate in un nuovo batch.
     */
    @Test
    void InadBatch_ProviderAcceptedThenPersistenceFails_RestartCreatesNewBatchForRequests() {
        List<TestRequest> requests = createNineRequests("batch-persist-failure");
        enqueueMassiveRequests(requests);
        String firstPollingId = "polling-persist-first-" + UUID.randomUUID();
        String firstBatchId = submitBatch(firstPollingId, requests);
        deleteBatch(firstBatchId);
        for (TestRequest testRequest : requests) {
            restoreRequestToNotWorked(testRequest.correlationId());
        }
        assertNull(nationalRegistriesBatchesRepository.getBatch(firstBatchId).block());
        String secondPollingId = "polling-persist-second-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress secondResponse = new ResponseRequestListDigitalAddress();
        secondResponse.setId(secondPollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(secondResponse));
        inadBatchRequestJob.batchRequest();
        verify(inadClient, times(2)).callMassiveEService(any(RequestListDigitalAddress.class));
        String secondBatchId = requestByRegistry(requests.getFirst().correlationId(), "INAD").get("batchId").s();
        assertNotEquals(firstBatchId, secondBatchId);
        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = requestByRegistry(testRequest.correlationId(), "INAD");
            assertEquals("WORKING", stored.get("status").s());
            assertEquals("INAD#WORKING", stored.get("registryStatus").s());
            assertEquals(secondBatchId, stored.get("batchId").s());
        }
        NationalRegistriesBatch secondBatch = getBatch(secondBatchId);
        assertNotNull(secondBatch);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, secondBatch.getStatus());
        assertEquals(secondPollingId, secondBatch.getPollingId());
    }

    /**
     * Simula un fallimento parziale durante l'aggiornamento delle request dopo la creazione
     * del primo batch. Le request gia aggiornate rimangono associate al batch originale,
     * mentre quella rimasta NOT_WORKED viene presa da un batch successivo.
     */
    @Test
    void InadBatch_PartialRequestWorkingUpdate_RestartCreatesNewBatchOnlyForRemainingRequest() {
        List<TestRequest> requests = createNineRequests("partial-working");
        enqueueMassiveRequests(requests);
        String firstPollingId = "polling-partial-working-first-" + UUID.randomUUID();
        String firstBatchId = submitBatch(firstPollingId, requests);
        TestRequest requestToRestore = requests.get(1);
        restoreRequestToNotWorked(requestToRestore.correlationId());
        Map<String, AttributeValue> beforeRestart = requestByRegistry(requestToRestore.correlationId(), "INAD");
        assertEquals("NOT_WORKED", beforeRestart.get("status").s());
        assertNull(beforeRestart.get("batchId"));
        String secondPollingId = "polling-partial-working-second-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress secondResponse = new ResponseRequestListDigitalAddress();
        secondResponse.setId(secondPollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(secondResponse));
        inadBatchRequestJob.batchRequest();
        verify(inadClient, times(2)).callMassiveEService(any(RequestListDigitalAddress.class));
        Map<String, AttributeValue> recovered = requestByRegistry(requestToRestore.correlationId(), "INAD");
        assertEquals("WORKING", recovered.get("status").s());
        assertEquals("INAD#WORKING", recovered.get("registryStatus").s());
        String secondBatchId = recovered.get("batchId").s();
        assertNotEquals(firstBatchId, secondBatchId);
        for (int i = 0; i < requests.size(); i++) {
            if (i == 1) {
                continue;
            }
            Map<String, AttributeValue> stored = requestByRegistry(requests.get(i).correlationId(), "INAD");
            assertEquals("WORKING", stored.get("status").s());
            assertEquals(firstBatchId, stored.get("batchId").s());
        }
        assertEquals(firstPollingId, getBatch(firstBatchId).getPollingId());
        assertEquals(secondPollingId, getBatch(secondBatchId).getPollingId());
    }

    /**
     * Verifica che due worker concorrenti tentino di acquisire lo stesso batch
     * senza produrre due lavorazioni concorrenti dello stesso elemento.
     */
    @Test
    void Polling_TwoWorkersCompeteForSameBatch_OnlyOneReservationWins() {
        List<TestRequest> requests = createNineRequests("polling-concurrent");
        enqueueMassiveRequests(requests);
        String pollingId = "polling-concurrent-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, getBatch(batchId).getStatus());
        CompletableFuture<Void> workerOne = CompletableFuture.runAsync(pollingJob::polling);
        CompletableFuture<Void> workerTwo = CompletableFuture.runAsync(pollingJob::polling);
        CompletableFuture.allOf(workerOne, workerTwo).join();
        NationalRegistriesBatch persisted = nationalRegistriesBatchesRepository.getBatch(batchId).block();
        assertNotNull(persisted);
        assertEquals(BatchesStatusEnum.WORKING, persisted.getStatus());
    }

    /**
     * Simula uno stato di recovery in cui alcune richieste del batch risultano gia WORKED
     * mentre una richiesta e ancora WORKING. Il replay completa solo la richiesta residua.
     */
    @Test
    void InadPolling_PartialRequestProcessing_ReplayCompletesOnlyRemainingRequest() {
        List<TestRequest> requests = createNineRequests("partial-poll");
        enqueueMassiveRequests(requests);
        String pollingId = "partial-polling-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);
        pollingJob.polling();
        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        List<ResponseRequestDigitalAddress> providerResults = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            providerResults.add(inadResponse(requests.get(i).taxId(), "partial-" + i + "@pec.it", null));
        }
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(providerResults);
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(result));
        PnPollingEvent.Payload payload = pollingPayload(GatewayDownstreamService.INAD, batchId, pollingId, 9, 0, 0);
        inadPollingService.handleMessage(payload).block();
        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
        Map<String, String> originalPayloads = new HashMap<>();
        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = requestByRegistry(testRequest.correlationId(), "INAD");
            assertEquals("WORKED", stored.get("status").s());
            originalPayloads.put(testRequest.correlationId(), stored.get("resultPayload").s());
        }
        TestRequest requestToReplay = requests.get(1);
        Map<String, AttributeValue> partial = new HashMap<>(requestByRegistry(requestToReplay.correlationId(), "INAD"));
        partial.put("status", AttributeValue.builder().s("WORKING").build());
        partial.put("registryStatus", AttributeValue.builder().s("INAD#WORKING").build());
        partial.remove("resultPayload");
        putRequestItem(partial);
        Map<String, AttributeValue> batch = new HashMap<>(findBatch(batchId));
        batch.put("status", AttributeValue.builder().s("WORKING").build());
        putBatchItem(batch);
        inadPollingService.handleMessage(payload).block();
        for (int i = 0; i < requests.size(); i++) {
            TestRequest testRequest = requests.get(i);
            Map<String, AttributeValue> stored = requestByRegistry(testRequest.correlationId(), "INAD");
            assertEquals("WORKED", stored.get("status").s());
            assertNotNull(stored.get("resultPayload"));
            if (!testRequest.correlationId().equals(requestToReplay.correlationId())) {
                assertEquals(originalPayloads.get(testRequest.correlationId()), stored.get("resultPayload").s());
            }
            CodeSqsDto finalPayload = readOutputMessage(stored.get("resultPayload").s());
            assertEquals("partial-" + i + "@pec.it", finalPayload.getDigitalAddress().getFirst().getAddress());
        }
        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
    }

    /**
     * Verifica l'esaurimento del retry INAD attraverso cicli reali fino alla chiusura in ERROR.
     */
    @Test
    void InadPolling_ErrorRetryExhaustion_AcrossRealCycles() {
        int maxRetry = 4;
        Flow flow = startWorkingInadFlow("inad-error-exhaustion-" + UUID.randomUUID(), VALID_PF_CF);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.error(new IllegalStateException("temporary provider error")));
        for (int retry = 0; retry < maxRetry; retry++) {
            pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(MessageBuilder.withPayload(pollingPayload(flow, retry, 0)).build());
            Map<String, AttributeValue> batch = findBatch(flow.batchId());
            assertEquals("REQUEST_SENT", batch.get("status").s());
            assertEquals(String.valueOf(retry + 1), batch.get("errorRetryCount").n());
            makeBatchImmediatelyRetryable(flow.batchId());
            pollingJob.polling();
            assertEquals("WORKING", findBatch(flow.batchId()).get("status").s());
        }
        pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(MessageBuilder.withPayload(pollingPayload(flow, maxRetry, 0)).build());
        assertEquals("ERROR", findBatch(flow.batchId()).get("status").s());
        assertEquals("ERROR", requestByRegistry(flow.correlationId(), "INAD").get("status").s());
        assertTrue(requestsByRegistry(flow.correlationId(), "INIPEC").isEmpty());
    }

    /**
     * Verifica che nel flusso INAD massivo la correlazione tra request e risultati
     * rimanga corretta con risposta provider fuori ordine, CF assente,
     * stesso CF associato a piu correlationId e recupero paginato delle request.
     * Con fallback attivo il CF assente genera una request IniPEC.
     */
    @Test
    void InadMassive_PaginationAndCorrelationRemainStableWithOutOfOrderAndMissingResults() {
        String sharedTaxId = "KRSJSM88S03H501A";
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("corr~separator-" + UUID.randomUUID(), sharedTaxId);
        cases.put("corr-same-cf-" + UUID.randomUUID(), sharedTaxId);
        cases.put("corr-b-" + UUID.randomUUID(), "PRMRGG74T16H501R");
        cases.put("corr-c-" + UUID.randomUUID(), "RSSMRA80A01H501C");
        cases.put("corr-missing-" + UUID.randomUUID(), "VRDMRA77E66I700E");
        cases.put("corr-filler-e-" + UUID.randomUUID(), "NRELCU80E53H501M");
        cases.put("corr-filler-f-" + UUID.randomUUID(), "NREMRA09A27F205P");
        cases.put("corr-filler-g-" + UUID.randomUUID(), "BNCMRC55H10G273Y");
        cases.put("corr-filler-h-" + UUID.randomUUID(), "DCNMRM85M49H501S");
        for (Map.Entry<String, String> entry : cases.entrySet()) {
            enqueueGatewayRequest("PF", entry.getValue(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, entry.getKey());
            processNextGatewayInputMessage();
        }
        String pollingId = "inad-correlation-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();
        batchResponse.setId(pollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(batchResponse));
        inadBatchRequestJob.batchRequest();
        String firstCorrelationId = cases.keySet().iterator().next();
        String batchId = requestByRegistry(firstCorrelationId, "INAD").get("batchId").s();
        pollingJob.polling();
        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of(
                inadResponse("RSSMRA80A01H501C", "c@pec.it", null),
                inadResponse(sharedTaxId, "shared@pec.it", null),
                inadResponse("PRMRGG74T16H501R", "b@pec.it", null),
                inadResponse("NRELCU80E53H501M", "filler-e@pec.it", null),
                inadResponse("NREMRA09A27F205P", "filler-f@pec.it", null),
                inadResponse("BNCMRC55H10G273Y", "filler-g@pec.it", null),
                inadResponse("DCNMRM85M49H501S", "filler-h@pec.it", null)
        ));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(result));
        inadPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INAD, batchId, pollingId, 9, 0, 0)).block();
        for (Map.Entry<String, String> entry : cases.entrySet()) {
            Map<String, AttributeValue> inadRequest = requestByRegistry(entry.getKey(), "INAD");
            assertEquals("WORKED", inadRequest.get("status").s());
            if ("VRDMRA77E66I700E".equals(entry.getValue())) {
                assertNull(inadRequest.get("resultPayload"));
                assertLegacyPendingRequest(entry.getKey(), entry.getValue());
                continue;
            }
            CodeSqsDto resultPayload = readOutputMessage(inadRequest.get("resultPayload").s());
            assertEquals(entry.getKey(), resultPayload.getCorrelationId());
            if (sharedTaxId.equals(entry.getValue())) {
                assertEquals("shared@pec.it", resultPayload.getDigitalAddress().getFirst().getAddress());
            }
            if ("PRMRGG74T16H501R".equals(entry.getValue())) {
                assertEquals("b@pec.it", resultPayload.getDigitalAddress().getFirst().getAddress());
            }
            if ("RSSMRA80A01H501C".equals(entry.getValue())) {
                assertEquals("c@pec.it", resultPayload.getDigitalAddress().getFirst().getAddress());
            }
        }
        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
    }

    /**
     * Simula il caso in cui tutte le request INAD siano gia WORKED e i fallback legacy siano gia stati creati,
     * ma l'update finale del batch a WORKED sia fallito lasciandolo WORKING.
     * Al replay non devono essere creati nuovi fallback legacy e il batch deve essere chiuso a WORKED.
     */
    @Test
    void InadToLegacyInipec_AllRequestsWorked_BatchUpdateFailed_ReplayOnlyClosesBatch() {
        String correlationId1 = "inad-legacy-batch-failure-1-" + UUID.randomUUID();
        String correlationId2 = "inad-legacy-batch-failure-2-" + UUID.randomUUID();
        InadFlow flow = startSingleInadFlow(correlationId1, VALID_PF_CF);
        addRequestToInadBatch(flow.batchId(), correlationId1, correlationId2);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of());
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.just(result));
        PnPollingEvent.Payload payload = pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 2, 0, 0);
        markInadRequestAsWorkedAndCreateLegacyFallback(correlationId1);
        markInadRequestAsWorkedAndCreateLegacyFallback(correlationId2);
        assertEquals(1, findBatchRequestsByCorrelationPrefix(correlationId1).size());
        assertEquals(1, findBatchRequestsByCorrelationPrefix(correlationId2).size());
        assertEquals("WORKING", findBatch(flow.batchId()).get("status").s());
        inadPollingService.handleMessage(payload).block();
        assertEquals(1, findBatchRequestsByCorrelationPrefix(correlationId1).size());
        assertEquals(1, findBatchRequestsByCorrelationPrefix(correlationId2).size());
        assertEquals("WORKED", requestByRegistry(correlationId1, "INAD").get("status").s());
        assertEquals("WORKED", requestByRegistry(correlationId2, "INAD").get("status").s());
        assertEquals("WORKED", findBatch(flow.batchId()).get("status").s());
    }

    /**
     * Simula il caso in cui il processing INAD si interrompa dopo aver completato solo una parte delle request.
     * Al replay deve essere creato il fallback legacy solo per la request ancora WORKING, senza duplicare quello gia creato.
     */
    @Test
    void InadToLegacyInipec_RequestUpdateFailed_ReplayProcessesOnlyWorkingRequests() {
        String correlationId1 = "inad-legacy-request-failure-1-" + UUID.randomUUID();
        String correlationId2 = "inad-legacy-request-failure-2-" + UUID.randomUUID();
        InadFlow flow = startSingleInadFlow(correlationId1, VALID_PF_CF);
        addRequestToInadBatch(flow.batchId(), correlationId1, correlationId2);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of());
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.just(result));
        PnPollingEvent.Payload payload = pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 2, 0, 0);
        markInadRequestAsWorkedAndCreateLegacyFallback(correlationId1);
        assertEquals("WORKED", requestByRegistry(correlationId1, "INAD").get("status").s());
        assertEquals("WORKING", requestByRegistry(correlationId2, "INAD").get("status").s());
        assertEquals(1, findBatchRequestsByCorrelationPrefix(correlationId1).size());
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId2).isEmpty());
        inadPollingService.handleMessage(payload).block();
        assertEquals(1, findBatchRequestsByCorrelationPrefix(correlationId1).size());
        assertEquals(1, findBatchRequestsByCorrelationPrefix(correlationId2).size());
        assertEquals("WORKED", requestByRegistry(correlationId1, "INAD").get("status").s());
        assertEquals("WORKED", requestByRegistry(correlationId2, "INAD").get("status").s());
        assertEquals("WORKED", findBatch(flow.batchId()).get("status").s());
    }

    /**
     * Legacy specifico: errore di submit al limite dei retry.
     */
    @Test
    void LegacyInipec_SubmitErrorExhaustsRequestRetryAndMarksError() {
        String correlationId = "legacy-submit-error-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, VALID_PF_CF, null);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class)))
                .thenReturn(Mono.error(new IllegalStateException("provider unavailable")));

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertEquals("1", request.get("retry").n());
        assertNotEquals("NO_BATCH_ID", request.get("batchId").s());
    }

    /**
     * Legacy specifico: bad request in submit.
     */
    @Test
    void LegacyInipec_SubmitBadRequestMarksRequestErrorImmediately() {
        String correlationId = "legacy-submit-bad-request-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, VALID_PF_CF, null);
        PnNationalRegistriesException badRequest = new PnNationalRegistriesException(
                "bad request", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.error(badRequest));

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertEquals("1", request.get("retry").n());
    }

    /**
     * Legacy specifico: esaurimento retry per errore polling.
     */
    @Test
    void LegacyInipec_PollingErrorExhaustsRetryAndMarksPollingAndRequestError() {
        String correlationId = "legacy-polling-exhausted-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, VALID_PF_CF, null);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(new IllegalStateException("polling unavailable")));

        digitalAddressBatchPollingService.batchPecPolling();
        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        assertEquals("WORKING", polling.get("status").s());
        assertEquals("1", polling.get("retry").n());

        recoverLegacyPolling(batchId);
        digitalAddressBatchPollingService.batchPecPolling();

        polling = findLegacyPolling(batchId);
        assertEquals("ERROR", polling.get("status").s());
        assertEquals("2", polling.get("retry").n());
        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertNotNull(request.get("message"));
    }

    /**
     * Legacy specifico: esaurimento retry in-progress.
     */
    @Test
    void LegacyInipec_PollingInProgressExhaustsRetryAndMarksPollingAndRequestError() {
        String correlationId = "legacy-in-progress-exhausted-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, VALID_PF_CF, null);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        PnNationalRegistriesException inProgress = new PnNationalRegistriesException(
                "List PEC in progress", HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(inProgress));

        digitalAddressBatchPollingService.batchPecPolling();
        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        assertEquals("WORKING", polling.get("status").s());
        assertEquals("1", polling.get("inProgressRetry").n());

        recoverLegacyPolling(batchId);
        digitalAddressBatchPollingService.batchPecPolling();

        polling = findLegacyPolling(batchId);
        assertEquals("ERROR", polling.get("status").s());
        assertEquals("2", polling.get("inProgressRetry").n());
        assertEquals("ERROR", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    /**
     * Legacy specifico: bad request durante il polling.
     */
    @Test
    void LegacyInipec_PollingBadRequestMarksPollingAndRequestErrorImmediately() {
        String correlationId = "legacy-polling-bad-request-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, VALID_PF_CF, null);
        String batchId = submitLegacyBatch(correlationId, pollingId);
        PnNationalRegistriesException badRequest = new PnNationalRegistriesException(
                "bad request", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(badRequest));

        digitalAddressBatchPollingService.batchPecPolling();

        assertEquals("ERROR", findLegacyPolling(batchId).get("status").s());
        assertEquals("ERROR", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    /**
     * Legacy specifico: output consumato e sendStatus aggiornato a SENT.
     */
    @Test
    void LegacyInipec_OutputIsReallyConsumedAndSendStatusBecomesSent() {
        String correlationId = "legacy-output-sent-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, VALID_PF_CF, null);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();
        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(Mono.just(iniPecPollingResponse(pecResponse(VALID_PF_CF, "legacy-output@pec.it"))));
        digitalAddressBatchPollingService.batchPecPolling();

        Message output = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(output);
        CodeSqsDto payload = readOutputMessage(output.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("legacy-output@pec.it", payload.getDigitalAddress().getFirst().getAddress());

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", request.get("status").s());
        assertEquals("SENT", request.get("sendStatus").s());
    }

    /**
     * Legacy specifico: recovery dopo fallimento della persistenza sendStatus=SENT.
     */
    @Test
    void LegacyInipec_OutputSentButSendStatusPersistenceFails_RecoveryCompletesDeliveryState() {
        String correlationId = "legacy-output-recovery-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyFallbackFromMassive(correlationId, VALID_PF_CF, null);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();

        AtomicBoolean failSentUpdateOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            BatchRequest request = invocation.getArgument(0);
            if (BatchSendStatus.SENT.getValue().equals(request.getSendStatus()) && failSentUpdateOnce.getAndSet(false)) {
                return Mono.error(new IllegalStateException("simulated sendStatus persistence failure"));
            }
            return invocation.callRealMethod();
        }).when(iniPecBatchRequestRepository).update(any(BatchRequest.class));

        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(Mono.just(iniPecPollingResponse(pecResponse(VALID_PF_CF, "legacy-recovery@pec.it"))));
        digitalAddressBatchPollingService.batchPecPolling();

        Message firstOutput = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(firstOutput);
        CodeSqsDto firstPayload = readOutputMessage(firstOutput.body());
        assertEquals(correlationId, firstPayload.getCorrelationId());
        assertEquals("INIPEC", firstPayload.getRegistry());
        assertEquals("legacy-recovery@pec.it", firstPayload.getDigitalAddress().getFirst().getAddress());

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", request.get("status").s());
        assertEquals("NOT_SENT", request.get("sendStatus").s());

        iniPecBatchSqsService.recoveryBatchSendToSqs();
        request = awaitSendStatus(correlationId, "SENT");
        assertEquals("SENT", request.get("sendStatus").s());

        Message recoveredOutput = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(recoveredOutput);
        CodeSqsDto recoveredPayload = readOutputMessage(recoveredOutput.body());
        assertEquals(correlationId, recoveredPayload.getCorrelationId());
        assertEquals("INIPEC", recoveredPayload.getRegistry());
        assertEquals("legacy-recovery@pec.it", recoveredPayload.getDigitalAddress().getFirst().getAddress());
    }

    private CodeSqsDto runMassiveInadResolvedScenario(String correlationId, String taxId, ResponseRequestDigitalAddress targetResult) {
        executeMassiveInadScenario(correlationId, taxId, targetResult);
        Map<String, AttributeValue> inadRequest = requestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertNotNull(inadRequest.get("resultPayload"));
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        verify(inadClient, never()).callEService(any(), any());
        return readOutputMessage(inadRequest.get("resultPayload").s());
    }

    private void startLegacyFallbackFromMassive(String correlationId, String taxId, ResponseRequestDigitalAddress targetResult) {
        executeMassiveInadScenario(correlationId, taxId, targetResult);
        Map<String, AttributeValue> inadRequest = requestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertNull(inadRequest.get("resultPayload"));
        assertLegacyPendingRequest(correlationId, taxId);
        verify(inadClient, never()).callEService(any(), any());
    }

    private void executeMassiveInadScenario(String correlationId, String taxId, ResponseRequestDigitalAddress targetResult) {
        List<TestRequest> requests = new ArrayList<>();
        requests.add(new TestRequest(taxId, correlationId));
        for (int i = 0; i < 8; i++) {
            requests.add(new TestRequest("RSSMRA80A01H50" + i + "U", "functional-filler-" + i + "-" + UUID.randomUUID()));
        }
        enqueueMassiveRequests(requests);
        String pollingId = "inad-functional-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);
        pollingJob.polling();
        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        List<ResponseRequestDigitalAddress> providerResults = new ArrayList<>();
        if (targetResult != null) {
            providerResults.add(targetResult);
        }
        for (int i = 1; i < requests.size(); i++) {
            TestRequest filler = requests.get(i);
            providerResults.add(inadResponse(filler.taxId(), "functional-filler-" + i + "@pec.it", null));
        }
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(providerResults);
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(result));
        pollInad(batchId, pollingId, requests.size());
        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
    }

    private CodeSqsDto completeLegacyFinalFromMassive(String correlationId, String taxId, String address) {
        return completeLegacyFinal(correlationId, taxId, pecResponse(taxId, address));
    }

    private void assertLegacyPendingRequest(String correlationId, String taxId) {
        List<Map<String, AttributeValue>> legacyRequests = findBatchRequestsByCorrelationPrefix(correlationId);
        assertEquals(1, legacyRequests.size());
        Map<String, AttributeValue> request = legacyRequests.getFirst();
        assertTrue(request.get("correlationId").s().startsWith(correlationId + "~"));
        assertEquals(taxId, request.get("cf").s());
        assertEquals("PF", request.get("recipientType").s());
        assertEquals("NOT_WORKED", request.get("status").s());
    }

    private CodeSqsDto completeLegacyFinal(String correlationId, String taxId, Pec... pecs) {
        String pollingId = "legacy-full-flow-" + UUID.randomUUID();
        submitLegacyBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pecs)));
        digitalAddressBatchPollingService.batchPecPolling();
        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals(taxId, request.get("cf").s());
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("message"));
        return readOutputMessage(request.get("message").s());
    }

    private String submitLegacyBatch(String correlationId, String pollingId) {
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();
        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKING", request.get("status").s());
        return request.get("batchId").s();
    }

    private void recoverLegacyPolling(String batchId) {
        digitalAddressBatchPollingService.recoveryBatchPolling();
        for (int i = 0; i < 50; i++) {
            if ("NOT_WORKED".equals(findLegacyPolling(batchId).get("status").s())) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }
        fail("Legacy polling not recovered for batchId " + batchId);
    }

    private Map<String, AttributeValue> findLegacyPolling(String batchId) {
        return dynamoDbClient.scan(ScanRequest.builder().tableName(BATCH_POLLING_TABLE).filterExpression("batchId = :batchId").expressionAttributeValues(Map.of(":batchId", AttributeValue.builder().s(batchId).build())).build()).items().stream().findFirst().orElseThrow();
    }

    private Map<String, AttributeValue> awaitSendStatus(String correlationId, String expected) {
        Map<String, AttributeValue> request = null;
        for (int i = 0; i < 100; i++) {
            request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
            if (request.get("sendStatus") != null && expected.equals(request.get("sendStatus").s())) {
                return request;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }
        fail("sendStatus non aggiornato a " + expected + " per correlationId " + correlationId);
        return request;
    }

    private Pec pecResponseWithProfessionals(String taxId, String pecImpresa, String... professionalPecs) {
        Map<String, Object> value = new HashMap<>();
        value.put("cf", taxId);
        if (pecImpresa != null) {
            value.put("pecImpresa", pecImpresa);
        }
        value.put("pecProfessionista", Arrays.stream(professionalPecs).map(pec -> Map.of("pec", pec)).toList());
        return objectMapper.convertValue(value, Pec.class);
    }

    private InadFlow startSingleInadFlow(String correlationId, String taxId) {
        enqueueGatewayRequest("PF", taxId, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();
        String pollingId = "inad-polling-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress response = new ResponseRequestListDigitalAddress();
        response.setId(pollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(response));
        inadBatchRequestJob.batchRequest();
        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INAD");
        String batchId = request.get("batchId").s();
        pollingJob.polling();
        return new InadFlow(batchId, pollingId);
    }

    private void addRequestToInadBatch(String batchId, String sourceCorrelationId, String newCorrelationId) {
        Map<String, AttributeValue> source = requestByRegistry(sourceCorrelationId, "INAD");
        Map<String, AttributeValue> newRequest = new HashMap<>(source);
        newRequest.put("correlationId", AttributeValue.builder().s(newCorrelationId).build());
        newRequest.put("batchId", AttributeValue.builder().s(batchId).build());
        newRequest.put("status", AttributeValue.builder().s("WORKING").build());
        newRequest.put("registryStatus", AttributeValue.builder().s("INAD#WORKING").build());
        newRequest.remove("resultPayload");
        putRequestItem(newRequest);
        Map<String, AttributeValue> batch = new HashMap<>(findBatch(batchId));
        batch.put("batchSize", AttributeValue.builder().n("2").build());
        putBatchItem(batch);
    }

    private void markInadRequestAsWorkedAndCreateLegacyFallback(String correlationId) {
        Map<String, AttributeValue> inadRequest = requestByRegistry(correlationId, "INAD");
        Map<String, AttributeValue> worked = new HashMap<>(inadRequest);
        worked.put("status", AttributeValue.builder().s("WORKED").build());
        worked.put("registryStatus", AttributeValue.builder().s("INAD#WORKED").build());
        worked.remove("resultPayload");
        putRequestItem(worked);
        String legacyCorrelationId = correlationId + "~" + UUID.randomUUID();
        Map<String, AttributeValue> legacy = new HashMap<>();
        legacy.put("correlationId", AttributeValue.builder().s(legacyCorrelationId).build());
        legacy.put("cf", AttributeValue.builder().s(inadRequest.get("taxId").s()).build());
        legacy.put("recipientType", AttributeValue.builder().s("PF").build());
        legacy.put("status", AttributeValue.builder().s("NOT_WORKED").build());
        legacy.put("batchId", AttributeValue.builder().s("NO_BATCH_ID").build());
        legacy.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());
        dynamoDbClient.putItem(builder -> builder.tableName(BATCH_REQUESTS_TABLE).item(legacy));
    }

    private Flow startWorkingInadFlow(String correlationId, String taxId) {
        List<TestRequest> requests = new ArrayList<>();
        requests.add(new TestRequest(taxId, correlationId));
        for (int i = 0; i < 8; i++) {
            requests.add(new TestRequest("RSSMRA80A01H50" + i + "U", "inad-flow-filler-" + i + "-" + UUID.randomUUID()));
        }
        enqueueMassiveRequests(requests);
        String pollingId = "inad-polling-" + UUID.randomUUID();
        String batchId = submitBatch(pollingId, requests);
        pollingJob.polling();
        assertEquals("WORKING", findBatch(batchId).get("status").s());
        return new Flow(correlationId, batchId, pollingId);
    }

    private void makeBatchImmediatelyRetryable(String batchId) {
        Map<String, AttributeValue> batch = new HashMap<>(findBatch(batchId));
        batch.put("attemptAfter", AttributeValue.builder().s(Instant.now().minusSeconds(1).toString()).build());
        putBatchItem(batch);
    }

    private void restoreRequestToNotWorked(String correlationId) {
        Map<String, AttributeValue> restored = new HashMap<>(requestByRegistry(correlationId, "INAD"));
        restored.put("status", AttributeValue.builder().s("NOT_WORKED").build());
        restored.put("registryStatus", AttributeValue.builder().s("INAD#NOT_WORKED").build());
        restored.remove("batchId");
        putRequestItem(restored);
    }

    private void deleteBatch(String batchId) {
        dynamoDbClient.deleteItem(builder -> builder.tableName(NATIONAL_REGISTRIES_BATCHES_TABLE).key(Map.of("batchId", AttributeValue.builder().s(batchId).build(), "sk", AttributeValue.builder().s("-").build())));
    }

    private List<TestRequest> createNineRequests(String prefix) {
        List<TestRequest> requests = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            requests.add(new TestRequest("RSSMRA80A01H50" + i + "U", prefix + "-" + i + "-" + UUID.randomUUID()));
        }
        return requests;
    }

    private void enqueueMassiveRequests(List<TestRequest> requests) {
        for (TestRequest testRequest : requests) {
            AddressOKDto response = enqueueGatewayRequest("PF", testRequest.taxId(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, testRequest.correlationId());
            processNextGatewayInputMessage();
            assertEquals(testRequest.correlationId(), response.getCorrelationId());
            Map<String, AttributeValue> stored = requestByRegistry(testRequest.correlationId(), "INAD");
            assertEquals("NOT_WORKED", stored.get("status").s());
            assertEquals("INAD#NOT_WORKED", stored.get("registryStatus").s());
            assertNull(stored.get("batchId"));
            assertNull(stored.get("resultPayload"));
        }
    }

    private String submitBatch(String pollingId, List<TestRequest> requests) {
        ResponseRequestListDigitalAddress response = new ResponseRequestListDigitalAddress();
        response.setId(pollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(response));
        inadBatchRequestJob.batchRequest();
        String batchId = assertSameWorkingBatch(requests);
        NationalRegistriesBatch batch = getBatch(batchId);
        assertNotNull(batch);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(pollingId, batch.getPollingId());
        return batchId;
    }

    private String assertSameWorkingBatch(List<TestRequest> requests) {
        String batchId = null;
        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = requestByRegistry(testRequest.correlationId(), "INAD");
            assertEquals("WORKING", stored.get("status").s());
            assertEquals("INAD#WORKING", stored.get("registryStatus").s());
            assertNotNull(stored.get("batchId"));
            if (batchId == null) {
                batchId = stored.get("batchId").s();
            } else {
                assertEquals(batchId, stored.get("batchId").s());
            }
        }
        assertNotNull(batchId);
        return batchId;
    }

    private void pollInad(String batchId, String pollingId, int batchSize) {
        inadPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INAD, batchId, pollingId, batchSize, 0, 0)).block();
    }

    private PnPollingEvent.Payload pollingPayload(GatewayDownstreamService registry, String batchId, String pollingId, int batchSize, int errorRetry, int inProgressRetry) {
        return PnPollingEvent.Payload.builder().registry(registry).batchId(batchId).pollingId(pollingId).batchSize(batchSize).errorRetryCount(errorRetry).inProgressRetryCount(inProgressRetry).build();
    }

    private PnPollingEvent.Payload pollingPayload(Flow flow, int errorRetryCount, int inProgressRetryCount) {
        return pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 9, errorRetryCount, inProgressRetryCount);
    }

    private ResponseStatusListDigitalAddress status(StatusProcessingRequest state) {
        ResponseStatusListDigitalAddress response = new ResponseStatusListDigitalAddress();
        response.setState(state);
        return response;
    }

    private List<Map<String, AttributeValue>> requestsByRegistry(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream().filter(request -> registry.equals(request.get("registry").s())).toList();
    }

    private Map<String, AttributeValue> requestByRegistry(String correlationId, String registry) {
        return requestsByRegistry(correlationId, registry).getFirst();
    }

    private NationalRegistriesBatch getBatch(String batchId) {
        return nationalRegistriesBatchesRepository.getBatch(batchId).block();
    }

    private Map<String, AttributeValue> findBatch(String batchId) {
        return dynamoDbClient.getItem(builder -> builder.tableName(NATIONAL_REGISTRIES_BATCHES_TABLE).key(Map.of("batchId", AttributeValue.builder().s(batchId).build(), "sk", AttributeValue.builder().s("-").build()))).item();
    }

    private void putRequestItem(Map<String, AttributeValue> item) {
        dynamoDbClient.putItem(builder -> builder.tableName(NATIONAL_REGISTRIES_REQUESTS_TABLE).item(item));
    }

    private void putBatchItem(Map<String, AttributeValue> item) {
        dynamoDbClient.putItem(builder -> builder.tableName(NATIONAL_REGISTRIES_BATCHES_TABLE).item(item));
    }

    private record TestRequest(String taxId, String correlationId) {
    }

    private record Flow(String correlationId, String batchId, String pollingId) {
    }

    private record InadFlow(String batchId, String pollingId) {
    }
}