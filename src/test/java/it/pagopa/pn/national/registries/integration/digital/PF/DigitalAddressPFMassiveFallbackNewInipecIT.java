package it.pagopa.pn.national.registries.integration.digital.PF;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
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
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.PollingInputsHandler;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.GatewayResponse;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.service.InadPollingService;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali PF DIGITAL
 *
 * Configurazione:
 *  - fallback PF attvivo
 *  - INAD massivo attivo
 *  - old IniPEC disattivo
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-inad-massive-api=true",
        "pn.national-registries.enable-old-inipec-flow=false",
        "pn.national-registries.inad.max-batch-request-size=9",
        "pn.national-registries.query-limit=2",
        "pn.national-registries.inad.oldest-request-max-waiting-seconds=0",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inad.batch-polling-max-retry=5",
        "pn.national-registries.inad.batch-polling-in-progress-max-retry=10",
        "pn.national-registries.inad.polling-error-retry-after=1",
        "pn.national-registries.inad.polling-in-progress-retry-after=1",
        "pn.national-registries.inipec.max-batch-request-size=20",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.batch-request-max-retry=2",
        "pn.national-registries.inipec.request-retry-after=0",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=2",
        "pn.national-registries.inipec.polling-error-retry-after=0",
        "pn.national-registries.inipec.polling-in-progress-retry-after=0",
        "spring.task.scheduling.enabled=false"
})
public class DigitalAddressPFMassiveFallbackNewInipecIT extends AbstractNationalRegistriesComponentIT {

    private static final String PF_FOUND = "RSSMRA80A01H501U";
    private static final String PF_NOT_FOUND = "RSSMRA80A02H501Z";
    private static final String PF_PROFESSIONAL_ONLY = "RSSMRA80A03H501B";
    private static final String PF_PERSONAL_AND_PROFESSIONAL = "RSSMRA80A04H501D";
    private static final String PF_INVALID_PERSONAL = "RSSMRA80A05H501H";
    private static final String PF_MULTIPLE_PROFESSIONALS = "RSSMRA80A06H501J";
    private static final String PF_EMPTY = "RSSMRA80A07H501L";
    private static final String PF_EXPIRED_PERSONAL_AND_PROFESSIONAL = "RSSMRA80A08H501N";
    private static final String PF_EXPIRED_PERSONAL = "RSSMRA80A09H501P";
    private static final String PF_PROFESSIONAL = "WDSTKB60E09L538U";
    private static final String PF_BUSINESS_PROFESSIONAL = "PRPMGG80A41H501W";
    private static final String PF_INVALID = "FLPCPT69A65Z336P";
    private static final String PF_ND = "DLGMRA73D41F839C";
    private static final String PF_NF = "PRMRGG74T16H501R";
    private static final String PF_MISSING = "VDADGI80A01F839E";
    private static final String PF_ER = "RSSSRG80A01F839I";

    private static final List<String> VALID_FILLER_CFS = List.of(
            "KRSJSM88S03H501A",
            "JSMKRV95P54H501D",
            "KPRSMP91H12F205O",
            "VRDMRA77E66I700E",
            "NRELCU80E53H501M",
            "NREMRA09A27F205P",
            "BSMGPR92R62F205X",
            "DCNMRM85M49H501S",
            "XLPJSM93B21H501O"
    );

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private InadPollingService inadPollingService;

    @Autowired
    private InipecPollingService inipecPollingService;

    @Autowired
    private NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;

    @Autowired
    private PollingInputsHandler pollingInputsHandler;

    /**
     * Inad pec personale valida.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalFound_NoInipecFallback() {
        String correlationId = "pf-massive-found-fallback-" + UUID.randomUUID();
        GatewayResponse payload = runMassiveInadResolvedScenario(correlationId, PF_FOUND, inadResponse(PF_FOUND, "pf@pec.it", null));
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("pf@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad NotFound: il CF richiesto non compare nel risultato massivo e parte il fallback IniPEC.
     */
    @Test
    void PF_Digital_INAD_Massive_NotFound_InipecFallback() {
        String correlationId = "pf-massive-not-found-fallback-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, PF_NOT_FOUND, null);
        GatewayResponse payload = completeInipecFinal(correlationId, PF_NOT_FOUND, "inipec-not-found-fallback@pec.it");
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals("inipec-not-found-fallback@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
    }

    /**
     * Inad risposta con lista PEC vuota e fallback IniPEC.
     */
    @Test
    void PF_Digital_INAD_Massive_EmptyList_InipecFallback() {
        String correlationId = "pf-massive-empty-list-fallback-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, PF_EMPTY, inadResponse(PF_EMPTY, List.of()));
        GatewayResponse payload = completeInipecFinal(correlationId, PF_EMPTY, "inipec-after-empty-list@pec.it");
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals("inipec-after-empty-list@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
    }

    /**
     * Inad pec personale scaduta e fallback IniPEC.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalExpired_InipecFallback() {
        String correlationId = "pf-massive-expired-personal-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        startInipecFallbackFromMassive(correlationId, PF_EXPIRED_PERSONAL, inadResponse(PF_EXPIRED_PERSONAL, List.of(expiredPersonal)));
        GatewayResponse payload = completeInipecFinal(correlationId, PF_EXPIRED_PERSONAL, "inipec-after-expired-personal@pec.it");
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals("inipec-after-expired-personal@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
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
        GatewayResponse payload = runMassiveInadResolvedScenario(correlationId, PF_FOUND, inadResponse(PF_FOUND, List.of(personal)));
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("personal-no-usage@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad pec personale senza data fine validita.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalWithoutEndValidity_IsValid() {
        String correlationId = "pf-massive-personal-no-end-validity-fallback-" + UUID.randomUUID();
        ElementDigitalAddress personal = inadAddress("personal-no-end@pec.it", null);
        personal.getUsageInfo().setDateEndValidity(null);
        GatewayResponse payload = runMassiveInadResolvedScenario(correlationId, PF_FOUND, inadResponse(PF_FOUND, List.of(personal)));
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("personal-no-end@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
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
        GatewayResponse payload = runMassiveInadResolvedScenario(correlationId, PF_FOUND, result);
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("second-valid@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad personale presente e professionale presente: viene preferita la personale.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalInsteadOfProfessional_FallbackEnabled() {
        String correlationId = "pf-massive-personal-preferred-fallback-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_PERSONAL_AND_PROFESSIONAL, List.of(inadAddress("personal@pec.it", null), inadAddress("professional@pec.it", "INGEGNERE")));
        GatewayResponse payload = runMassiveInadResolvedScenario(correlationId, PF_PERSONAL_AND_PROFESSIONAL, result);
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("personal@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad personale assente e professionale presente: viene usata la professionale prima del fallback.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalNotFound_ProfessionalFound_FallbackEnabled() {
        String correlationId = "pf-massive-professional-only-fallback-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_PROFESSIONAL_ONLY, List.of(inadAddress("professional@pec.it", "INGEGNERE")));
        GatewayResponse payload = runMassiveInadResolvedScenario(correlationId, PF_PROFESSIONAL_ONLY, result);
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad personale invalida e professionale valida: viene usata la professionale prima del fallback.
     */
    @Test
    void PF_Digital_INAD_Massive_PersonalInvalid_ProfessionalValid_FallbackEnabled() {
        String correlationId = "pf-massive-invalid-personal-professional-fallback-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_INVALID_PERSONAL, List.of(inadAddress("invalid-personal", null), inadAddress("professional@pec.it", "INGEGNERE")));
        GatewayResponse payload = runMassiveInadResolvedScenario(correlationId, PF_INVALID_PERSONAL, result);
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad personale assente e piu PEC professionali valide: viene usata la prima professionale valida.
     */
    @Test
    void PF_Digital_INAD_Massive_MultipleProfessionals_FallbackEnabled() {
        String correlationId = "pf-massive-multiple-professionals-fallback-" + UUID.randomUUID();
        ResponseRequestDigitalAddress result = inadResponse(PF_MULTIPLE_PROFESSIONALS, List.of(inadAddress("prof1@pec.it", "INGEGNERE"), inadAddress("prof2@pec.it", "ARCHITETTO")));
        GatewayResponse payload = runMassiveInadResolvedScenario(correlationId, PF_MULTIPLE_PROFESSIONALS, result);
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("prof1@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
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
        GatewayResponse payload = runMassiveInadResolvedScenario(correlationId, PF_EXPIRED_PERSONAL_AND_PROFESSIONAL, result);
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Inad solo professionale scaduta: nessun candidato utilizzabile e fallback IniPEC.
     */
    @Test
    void PF_Digital_INAD_Massive_OnlyProfessionalExpired_InipecFallback() {
        String correlationId = "pf-massive-only-professional-expired-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredProfessional = inadAddress("expired-professional@pec.it", "AVVOCATO");
        expiredProfessional.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        startInipecFallbackFromMassive(correlationId, PF_PROFESSIONAL_ONLY, inadResponse(PF_PROFESSIONAL_ONLY, List.of(expiredProfessional)));
        GatewayResponse payload = completeInipecFinal(correlationId, PF_PROFESSIONAL_ONLY, "inipec-after-expired-professional@pec.it");
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals("inipec-after-expired-professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
    }

    /**
     * INAD massive senza risultato utilizzabile -> IniPEC con PEC professionale valida.
     */
    @Test
    void PF_Digital_FullFlow_InipecProfessionalPecIsReturned() {
        String correlationId = "pf-massive-inipec-professional-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, PF_PROFESSIONAL, null);
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(pecResponseWithProfessionals(PF_PROFESSIONAL, null, "professional@pec.it"))));
        pollInipec(batchId, pollingId, 1);
        GatewayResponse payload = finalInipecPayload(correlationId);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * INAD massive senza risultato utilizzabile -> IniPEC con PEC impresa e tutte le PEC professionali valide.
     */
    @Test
    void PF_Digital_FullFlow_InipecBusinessAndAllProfessionalPecsAreReturned() {
        String correlationId = "pf-massive-inipec-business-professionals-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, PF_BUSINESS_PROFESSIONAL, null);
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(pecResponseWithProfessionals(PF_BUSINESS_PROFESSIONAL, "business@pec.it", "professional-one@pec.it", "professional-two@pec.it"))));
        pollInipec(batchId, pollingId, 1);
        GatewayResponse payload = finalInipecPayload(correlationId);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(3, payload.getPayload().getDigitalAddress().size());
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "business@pec.it".equals(address.getAddress()) && "IMPRESA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-one@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-two@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
    }

    /**
     * INAD massive senza risultato utilizzabile -> IniPEC con PEC non valida.
     */
    @Test
    void PF_Digital_FullFlow_InipecInvalidPecProducesEmptyFinalResult() {
        String correlationId = "pf-massive-inipec-invalid-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, PF_INVALID, null);
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(pecResponse(PF_INVALID, "not-an-email"))));
        pollInipec(batchId, pollingId, 1);
        GatewayResponse payload = finalInipecPayload(correlationId);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD massive senza risultato utilizzabile -> IniPEC con stato ND.
     */
    @Test
    void PF_Digital_FullFlow_InipecNdProducesEmptyFinalResult() {
        String correlationId = "pf-massive-inipec-nd-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, PF_ND, null);
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        Pec nd = pecResponse(PF_ND, "ignored-nd@pec.it");
        nd.setStatoImpresa(Pec.StatoImpresaEnum.ND);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(nd)));
        pollInipec(batchId, pollingId, 1);
        GatewayResponse payload = finalInipecPayload(correlationId);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD massive senza risultato utilizzabile -> IniPEC con stato NF.
     */
    @Test
    void PF_Digital_FullFlow_InipecNfProducesEmptyFinalResult() {
        String correlationId = "pf-massive-inipec-nf-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, PF_NF, null);
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        Pec nf = pecResponse(PF_NF, "ignored-nf@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(nf)));
        pollInipec(batchId, pollingId, 1);
        GatewayResponse payload = finalInipecPayload(correlationId);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD massive senza risultato utilizzabile -> IniPEC quando la risposta non contiene il CF richiesto.
     */
    @Test
    void PF_Digital_FullFlow_InipecMissingTaxIdProducesEmptyFinalResult() {
        String correlationId = "pf-massive-inipec-missing-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, PF_MISSING, null);
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse()));
        pollInipec(batchId, pollingId, 1);
        GatewayResponse payload = finalInipecPayload(correlationId);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD massive senza risultato utilizzabile -> IniPEC con stato ER, retry e passaggio a ERROR al limite configurato.
     */
    @Test
    void  PF_Digital_FullFlow_InipecErRetriesThenEndsInErrorAtConfiguredLimit() {
        String correlationId = "pf-massive-inipec-er-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, PF_ER, null);
        String firstPollingId = "polling-first-" + UUID.randomUUID();
        String firstBatchId = startInipecBatch(firstPollingId, correlationId);
        Pec er = pecResponse(PF_ER, "ignored@pec.it");
        er.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        when(infoCamereClient.callEServiceRequestPec(firstPollingId)).thenReturn(Mono.just(inipecPollingResponse(er)));
        pollInipec(firstBatchId, firstPollingId, 1);
        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("NOT_WORKED", request.get("status").s());
        assertEquals("1", request.get("retryCount").n());
        String secondPollingId = "polling-second-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(secondPollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        request = requestByRegistry(correlationId, "INIPEC");
        String secondBatchId = request.get("batchId").s();
        pollingJob.polling();
        when(infoCamereClient.callEServiceRequestPec(secondPollingId)).thenReturn(Mono.just(inipecPollingResponse(er)));
        pollInipec(secondBatchId, secondPollingId, 1);
        request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("ERROR", request.get("status").s());
        assertEquals("INIPEC#ERROR", request.get("registryStatus").s());
        assertNull(request.get("resultPayload"));
    }

    /**
     * INAD massive senza risultato utilizzabile -> IniPEC con polling ancora in progress e retry programmato.
     */
    @Test
    void PF_Digital_FullFlow_InipecPollingInProgressSchedulesRetry() {
        String correlationId = "pf-massive-inipec-in-progress-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, VALID_PF_CF, null);
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        PnNationalRegistriesException inProgress = new PnNationalRegistriesException("List PEC in progress", 404, "Not Found", null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(inProgress));
        pollInipec(batchId, pollingId, 1);
        Map<String, AttributeValue> batch = findBatch(batchId);
        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("1", batch.get("inProgressRetryCount").n());
        assertEquals("WORKING", requestByRegistry(correlationId, "INIPEC").get("status").s());
    }

    /**
     * INAD massive senza risultato utilizzabile -> IniPEC con metadata di errore e retry programmato.
     */
    @Test
    void PF_Digital_FullFlow_InipecErrorMetadataSchedulesRetry() {
        String correlationId = "pf-massive-inipec-metadata-error-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, VALID_PF_CF, null);
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        IniPecPollingResponse pollingResponse = new IniPecPollingResponse();
        pollingResponse.setCode("123");
        pollingResponse.setDescription("temporary error");
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));
        pollInipec(batchId, pollingId, 1);
        Map<String, AttributeValue> batch = findBatch(batchId);
        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("1", batch.get("errorRetryCount").n());
        assertEquals("WORKING", requestByRegistry(correlationId, "INIPEC").get("status").s());
    }

    /**
     * INAD massive senza risultato utilizzabile -> IniPEC con piu PEC professionali valide e nessuna PEC impresa.
     */
    @Test
    void PF_Digital_FullFlow_Inipec_AllProfessionalPecsArePreserved() {
        String correlationId = "pf-massive-inipec-professionals-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, PF_PROFESSIONAL, null);
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(pecResponseWithProfessionals(PF_PROFESSIONAL, null, "professional-one@pec.it", "professional-two@pec.it"))));
        pollInipec(batchId, pollingId, 1);
        GatewayResponse payload = finalInipecPayload(correlationId);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(2, payload.getPayload().getDigitalAddress().size());
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-one@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-two@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
    }

    /**
     * INAD massive senza risultato utilizzabile -> IniPEC con PEC impresa invalida e PEC professionale valida.
     */
    @Test
    void PF_Digital_FullFlow_Inipec_InvalidBusinessPecKeepsValidProfessionalPec() {
        String correlationId = "pf-massive-inipec-invalid-business-" + UUID.randomUUID();
        startInipecFallbackFromMassive(correlationId, PF_BUSINESS_PROFESSIONAL, null);
        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(pecResponseWithProfessionals(PF_BUSINESS_PROFESSIONAL, "not-an-email", "professional@pec.it"))));
        pollInipec(batchId, pollingId, 1);
        GatewayResponse payload = finalInipecPayload(correlationId);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
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
        GatewayResponse payload = readNewOutputMessage(stored.get("resultPayload").s());
        assertEquals("first@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
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
            GatewayResponse finalPayload = readNewOutputMessage(stored.get("resultPayload").s());
            assertEquals(CX_ID, finalPayload.getClientId());
            assertEquals("partial-" + i + "@pec.it", finalPayload.getPayload().getDigitalAddress().getFirst().getAddress());
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
        String sharedTaxId = "RSSMRA80A01H501U";
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("corr~separator-" + UUID.randomUUID(), sharedTaxId);
        cases.put("corr-same-cf-" + UUID.randomUUID(), sharedTaxId);
        cases.put("corr-b-" + UUID.randomUUID(), "RSSMRA80A02H501Z");
        cases.put("corr-c-" + UUID.randomUUID(), "RSSMRA80A03H501B");
        cases.put("corr-missing-" + UUID.randomUUID(), "RSSMRA80A04H501D");
        cases.put("corr-filler-e-" + UUID.randomUUID(), "RSSMRA80A05H501H");
        cases.put("corr-filler-f-" + UUID.randomUUID(), "RSSMRA80A06H501J");
        cases.put("corr-filler-g-" + UUID.randomUUID(), "RSSMRA80A07H501L");
        cases.put("corr-filler-h-" + UUID.randomUUID(), "RSSMRA80A08H501N");
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
                inadResponse("RSSMRA80A03H501B", "c@pec.it", null),
                inadResponse(sharedTaxId, "shared@pec.it", null),
                inadResponse("RSSMRA80A02H501Z", "b@pec.it", null),
                inadResponse("RSSMRA80A05H501H", "filler-e@pec.it", null),
                inadResponse("RSSMRA80A06H501J", "filler-f@pec.it", null),
                inadResponse("RSSMRA80A07H501L", "filler-g@pec.it", null),
                inadResponse("RSSMRA80A08H501N", "filler-h@pec.it", null)
        ));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(result));
        inadPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INAD, batchId, pollingId, 9, 0, 0)).block();
        for (Map.Entry<String, String> entry : cases.entrySet()) {
            Map<String, AttributeValue> inadRequest = requestByRegistry(entry.getKey(), "INAD");
            assertEquals("WORKED", inadRequest.get("status").s());
            if ("RSSMRA80A04H501D".equals(entry.getValue())) {
                assertNull(inadRequest.get("resultPayload"));
                assertInipecPendingRequest(entry.getKey(), entry.getValue());
                continue;
            }
            GatewayResponse resultPayload = readNewOutputMessage(inadRequest.get("resultPayload").s());
            assertEquals(CX_ID, resultPayload.getClientId());
            assertEquals(entry.getKey(), resultPayload.getPayload().getCorrelationId());
            if (sharedTaxId.equals(entry.getValue())) {
                assertEquals("shared@pec.it", resultPayload.getPayload().getDigitalAddress().getFirst().getAddress());
            }
            if ("RSSMRA80A02H501Z".equals(entry.getValue())) {
                assertEquals("b@pec.it", resultPayload.getPayload().getDigitalAddress().getFirst().getAddress());
            }
            if ("RSSMRA80A03H501B".equals(entry.getValue())) {
                assertEquals("c@pec.it", resultPayload.getPayload().getDigitalAddress().getFirst().getAddress());
            }
        }
        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
    }

    /**
     * Tutte le request INAD sono gia WORKED e i fallback IniPEC sono gia stati creati,
     * ma l'update finale del batch a WORKED e fallito. Il replay chiude solo il batch.
     */
    @Test
    void InadToInipec_AllRequestsWorked_BatchUpdateFailed_ReplayOnlyClosesBatch() {
        String correlationId1 = "inad-inipec-batch-failure-1-" + UUID.randomUUID();
        String correlationId2 = "inad-inipec-batch-failure-2-" + UUID.randomUUID();
        InadFlow flow = startSingleInadFlow(correlationId1, VALID_PF_CF);
        addRequestToInadBatch(flow.batchId(), correlationId1, correlationId2);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of());
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.just(result));
        PnPollingEvent.Payload payload = pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 2, 0, 0);
        markInadRequestAsWorkedAndCreateInipecFallback(correlationId1);
        markInadRequestAsWorkedAndCreateInipecFallback(correlationId2);
        assertEquals(1, requestsByRegistry(correlationId1, "INIPEC").size());
        assertEquals(1, requestsByRegistry(correlationId2, "INIPEC").size());
        assertEquals("WORKING", findBatch(flow.batchId()).get("status").s());
        inadPollingService.handleMessage(payload).block();
        assertEquals(1, requestsByRegistry(correlationId1, "INIPEC").size());
        assertEquals(1, requestsByRegistry(correlationId2, "INIPEC").size());
        assertEquals("WORKED", requestByRegistry(correlationId1, "INAD").get("status").s());
        assertEquals("WORKED", requestByRegistry(correlationId2, "INAD").get("status").s());
        assertEquals("WORKED", findBatch(flow.batchId()).get("status").s());
    }

    /**
     * Il processing si interrompe dopo aver completato solo una parte delle request INAD.
     * Il replay crea il fallback solo per la request ancora WORKING senza duplicare quello gia creato.
     */
    @Test
    void InadToInipec_RequestUpdateFailed_ReplayProcessesOnlyWorkingRequests() {
        String correlationId1 = "inad-inipec-request-failure-1-" + UUID.randomUUID();
        String correlationId2 = "inad-inipec-request-failure-2-" + UUID.randomUUID();
        InadFlow flow = startSingleInadFlow(correlationId1, VALID_PF_CF);
        addRequestToInadBatch(flow.batchId(), correlationId1, correlationId2);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of());
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.just(result));
        PnPollingEvent.Payload payload = pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 2, 0, 0);
        markInadRequestAsWorkedAndCreateInipecFallback(correlationId1);
        assertEquals("WORKED", requestByRegistry(correlationId1, "INAD").get("status").s());
        assertEquals("WORKING", requestByRegistry(correlationId2, "INAD").get("status").s());
        assertEquals(1, requestsByRegistry(correlationId1, "INIPEC").size());
        assertTrue(requestsByRegistry(correlationId2, "INIPEC").isEmpty());
        inadPollingService.handleMessage(payload).block();
        assertEquals(1, requestsByRegistry(correlationId1, "INIPEC").size());
        assertEquals(1, requestsByRegistry(correlationId2, "INIPEC").size());
        assertEquals("WORKED", requestByRegistry(correlationId1, "INAD").get("status").s());
        assertEquals("WORKED", requestByRegistry(correlationId2, "INAD").get("status").s());
        assertEquals("WORKED", findBatch(flow.batchId()).get("status").s());
    }

    /**
     * Verifica che correlationId, clientId e referenceRequestDate restino invariati
     * dalla richiesta INAD alla richiesta IniPEC di fallback.
     */
    @Test
    void InadToInipec_FallbackPreservesRequestIdentityAndReferenceDate() {
        String correlationId = "fallback-identity-" + UUID.randomUUID();
        InadFlow flow = startSingleInadFlow(correlationId, VALID_PF_CF);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of());
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.just(result));
        inadPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INAD, flow.batchId(), flow.pollingId(), 1, 0, 0)).block();
        Map<String, AttributeValue> source = requestByRegistry(correlationId, "INAD");
        Map<String, AttributeValue> fallback = requestByRegistry(correlationId, "INIPEC");
        assertEquals(source.get("correlationId").s(), fallback.get("correlationId").s());
        assertEquals(source.get("clientId").s(), fallback.get("clientId").s());
        assertEquals(source.get("referenceRequestDate").s(), fallback.get("referenceRequestDate").s());
        assertEquals(source.get("taxId").s(), fallback.get("taxId").s());
        assertEquals(source.get("recipientType").s(), fallback.get("recipientType").s());
    }

    private GatewayResponse runMassiveInadResolvedScenario(String correlationId, String taxId, ResponseRequestDigitalAddress targetResult) {
        executeMassiveInadScenario(correlationId, taxId, targetResult);
        Map<String, AttributeValue> inadRequest = requestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertNotNull(inadRequest.get("resultPayload"));
        assertTrue(requestsByRegistry(correlationId, "INIPEC").isEmpty());
        verify(inadClient, never()).callEService(any(), any());
        return readNewOutputMessage(inadRequest.get("resultPayload").s());
    }

    private void startInipecFallbackFromMassive(String correlationId, String taxId, ResponseRequestDigitalAddress targetResult) {
        executeMassiveInadScenario(correlationId, taxId, targetResult);
        Map<String, AttributeValue> inadRequest = requestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertNull(inadRequest.get("resultPayload"));
        assertInipecPendingRequest(correlationId, taxId);
        verify(inadClient, never()).callEService(any(), any());
    }

    private void executeMassiveInadScenario(String correlationId, String taxId, ResponseRequestDigitalAddress targetResult) {
        List<TestRequest> requests = new ArrayList<>();
        requests.add(new TestRequest(taxId, correlationId));
        for (int i = 0; i < 8; i++) {
            requests.add(new TestRequest(
                    VALID_FILLER_CFS.get(i),
                    "functional-filler-" + i + "-" + UUID.randomUUID()
            ));
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

    private GatewayResponse completeInipecFinal(String correlationId, String taxId, String address) {
        String pollingId = "inipec-full-flow-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(pecResponse(taxId, address))));
        pollInipec(batchId, pollingId, 1);
        GatewayResponse payload = finalInipecPayload(correlationId);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals(address, payload.getPayload().getDigitalAddress().getFirst().getAddress());
        return payload;
    }

    private void assertInipecPendingRequest(String correlationId, String taxId) {
        Map<String, AttributeValue> storedRequest = requestByRegistry(correlationId, "INIPEC");
        assertEquals(taxId, storedRequest.get("taxId").s());
        assertEquals("PF", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals("INIPEC", storedRequest.get("registry").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    private String startInipecBatch(String pollingId, String... correlationIds) {
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        String batchId = null;
        for (String correlationId : correlationIds) {
            Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
            assertEquals("WORKING", request.get("status").s());
            if (batchId == null) {
                batchId = request.get("batchId").s();
            } else {
                assertEquals(batchId, request.get("batchId").s());
            }
        }
        assertNotNull(batchId);
        pollingJob.polling();
        assertEquals("WORKING", findBatch(batchId).get("status").s());
        return batchId;
    }

    private void pollInipec(String batchId, String pollingId, int batchSize) {
        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, batchId, pollingId, batchSize, 0, 0)).block();
    }

    private GatewayResponse finalInipecPayload(String correlationId) {
        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("resultPayload"));
        return readNewOutputMessage(request.get("resultPayload").s());
    }

    private IniPecPollingResponse inipecPollingResponse(Pec... pecs) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pecs));
        return response;
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

    private void markInadRequestAsWorkedAndCreateInipecFallback(String correlationId) {
        Map<String, AttributeValue> inadRequest = requestByRegistry(correlationId, "INAD");
        Map<String, AttributeValue> workedInadRequest = new HashMap<>(inadRequest);
        workedInadRequest.put("status", AttributeValue.builder().s("WORKED").build());
        workedInadRequest.put("registryStatus", AttributeValue.builder().s("INAD#WORKED").build());
        workedInadRequest.remove("resultPayload");
        putRequestItem(workedInadRequest);
        Map<String, AttributeValue> inipecRequest = new HashMap<>(inadRequest);
        String fallbackCreatedAt = Instant.parse(inadRequest.get("createdAt").s()).plusNanos(1).toString();
        inipecRequest.put("createdAt", AttributeValue.builder().s(fallbackCreatedAt).build());
        inipecRequest.put("attemptAfter", AttributeValue.builder().s(fallbackCreatedAt).build());
        inipecRequest.put("registry", AttributeValue.builder().s("INIPEC").build());
        inipecRequest.put("status", AttributeValue.builder().s("NOT_WORKED").build());
        inipecRequest.put("registryStatus", AttributeValue.builder().s("INIPEC#NOT_WORKED").build());
        inipecRequest.remove("batchId");
        inipecRequest.remove("resultPayload");
        inipecRequest.remove("updatedAt");
        inipecRequest.remove("retryCount");
        putRequestItem(inipecRequest);
    }

    private Flow startWorkingInadFlow(String correlationId, String taxId) {
        List<TestRequest> requests = new ArrayList<>();
        requests.add(new TestRequest(taxId, correlationId));
        for (int i = 0; i < 8; i++) {
            requests.add(new TestRequest(
                    VALID_FILLER_CFS.get(i),
                    "inad-flow-filler-" + i + "-" + UUID.randomUUID()
            ));
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
            requests.add(new TestRequest(
                    VALID_FILLER_CFS.get(i),
                    prefix + "-" + i + "-" + UUID.randomUUID()
            ));
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
