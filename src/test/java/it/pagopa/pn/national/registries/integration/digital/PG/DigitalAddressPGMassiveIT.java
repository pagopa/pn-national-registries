package it.pagopa.pn.national.registries.integration.digital.PG;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS05Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS23Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.ResultDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS05ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS23ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.integration.AbstractNationalRegistriesComponentIT;
import it.pagopa.pn.national.registries.integration.PnNationalRegistriesTestApplication;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.PollingInputsHandler;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.GatewayResponse;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.service.InadPollingService;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static it.pagopa.pn.national.registries.converter.GatewayConverter.convertToGetIpaPecRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali PG DIGITAL
 *
 * Configurazione:
 *  - INAD massivo attivo
 *  - old IniPEC disattivo
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.dao.batchesTableName=pn-NationalRegistriesBatches",
        "pn.national-registries.inipec.batch-request-pk-separator=~",
        "pn.national-registries.enable-inad-massive-api=true",
        "pn.national-registries.enable-old-inipec-flow=false",
        "pn.national-registries.enable-pf-pec-fallback-flow=true",

        "pn.national-registries.inipec.max-batch-request-size=20",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.batch-request-max-retry=2",
        "pn.national-registries.inipec.request-retry-after=0",
        "pn.national-registries.inipec.batch-polling-max-retry=3",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=24",
        "pn.national-registries.inipec.polling-error-retry-after=0",
        "pn.national-registries.inipec.polling-in-progress-retry-after=0",

        "pn.national-registries.inad.max-batch-request-size=9",
        "pn.national-registries.query-limit=2",
        "pn.national-registries.inad.oldest-request-max-waiting-seconds=60",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inad.batch-polling-max-retry=5",
        "pn.national-registries.inad.batch-polling-in-progress-max-retry=10",
        "pn.national-registries.inad.polling-error-retry-after=1",
        "pn.national-registries.inad.polling-in-progress-retry-after=1",

        "spring.task.scheduling.enabled=false"
})
public class DigitalAddressPGMassiveIT extends AbstractNationalRegistriesComponentIT {

    private static final String PG_PROFESSIONAL = "30911420054";
    private static final String PG_BUSINESS_PROFESSIONAL = "12825810299";
    private static final String PG_INVALID = "38868390881";
    private static final String PG_ND = "32400700657";
    private static final String PG_NF = "97854290125";
    private static final String PG_MISSING = "34565000469";
    private static final String PG_ER = "59551540442";

    private static final String PG_INAD_FOUND = "38598790426";
    private static final String PG_INAD_NOT_FOUND = "10959831008";
    private static final String PG_INAD_PROFESSIONAL_ONLY = "12071426170";
    private static final String PG_INAD_BUSINESS_AND_PROFESSIONAL = "71935270349";
    private static final String PG_INAD_INVALID_BUSINESS = "01333214631";
    private static final String PG_INAD_MULTIPLE_PROFESSIONALS = "01113570442";
    private static final String PG_INAD_EMPTY = "05722930657";
    private static final String PG_INAD_EXPIRED_BUSINESS_AND_PROFESSIONAL = "20517490320";
    private static final String PG_INAD_EXPIRED_BUSINESS = "02313821007";

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private InipecPollingService inipecPollingService;

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private InadPollingService inadPollingService;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;

    @Autowired
    private PollingInputsHandler pollingInputsHandler;

    // -------------------------------------------------------------------------
    // IPA / INIPEC: stessi scenari PG della suite non-massiva
    // -------------------------------------------------------------------------

    /**
     * IPA diretto e Gateway PG restituiscono la stessa PEC.
     */
    @Test
    void Ipa_DirectEndpointAndGateway_ReturnSameFunctionalAddress() {
        String taxId = VALID_PG_PIVA;
        String correlationId = "ipa-gateway-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(taxId), anyString())).thenReturn(Mono.just(ipaFoundResponse(taxId, "same-ipa@pec.it")));

        IPARequestBodyDto directRequest = convertToGetIpaPecRequest(buildGatewayRequest("ipa-direct-" + UUID.randomUUID(), taxId, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL));

        IPAPecDto direct = webTestClient.post().uri("/national-registries-private/ipa/pec").contentType(MediaType.APPLICATION_JSON).bodyValue(directRequest).exchange().expectStatus().isOk().expectBody(IPAPecDto.class).returnResult().getResponseBody();

        assertNotNull(direct);

        enqueueGatewayRequest("PG", taxId, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        Message output = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(output);

        CodeSqsDto gateway = readOutputMessage(output.body());

        assertEquals(direct.getDomicilioDigitale(), gateway.getDigitalAddress().getFirst().getAddress());
        assertEquals("IPA", gateway.getRegistry());
    }

    /**
     * IPA trovato.
     */
    @Test
    void PG_Digital_IPA() {
        String correlationId = "pg-digital-ipa-found-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any())).thenReturn(Mono.just(ipaFoundResponse(VALID_PG_PIVA, "pg@pec.it")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());

        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("IPA", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(), payload.getAddressType());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("pg@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA), any());
        verifyNoInteractions(infoCamereClient, inadClient);
    }

    /**
     * IPA con più risultati: usa WS05.
     */
    @Test
    void PG_Digital_IPA_MultipleResults_UsesWS05() {
        String correlationId = "pg-digital-ipa-multiple-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any())).thenReturn(Mono.just(ipaMultipleResponse()));
        when(ipaClient.callEServiceWS05(eq("cod-amm-1"), any())).thenReturn(Mono.just(ipaWs05Response("ws05@pec.it")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());

        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("IPA", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("ws05@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA), any());
        verify(ipaClient).callEServiceWS05(eq("cod-amm-1"), any());
        verifyNoInteractions(infoCamereClient, inadClient);
    }

    /**
     * IPA NotFound -> IniPEC trovato.
     */
    @Test
    void PG_Digital_IPA_NotFound_InipecFound() {
        String correlationId = "pg-digital-ipa-not-found-inipec-found-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, VALID_PG_PIVA);

        GatewayResponse payload = completeInipecFinal(correlationId, VALID_PG_PIVA, pecResponse(VALID_PG_PIVA, "inipec@pec.it"));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("inipec@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        verifyNoInteractions(inadClient);
    }

    /**
     * IPA con PEC invalida -> IniPEC trovato.
     */
    @Test
    void PG_Digital_IPA_InvalidPec_InipecFound() {
        String correlationId = "pg-digital-ipa-invalid-inipec-found-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any())).thenReturn(Mono.just(ipaFoundResponse(VALID_PG_PIVA, "invalid-pec")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertInipecPendingRequest(correlationId, VALID_PG_PIVA);

        GatewayResponse payload = completeInipecFinal(correlationId, VALID_PG_PIVA, pecResponse(VALID_PG_PIVA, "inipec-after-invalid-ipa@pec.it"));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals("inipec-after-invalid-ipa@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        verifyNoInteractions(inadClient);
    }

    /**
     * IPA errore tecnico: il flusso non prosegue.
     */
    @Test
    void PG_Digital_IPA_TechnicalError_DoesNotContinue() {
        String correlationId = "pg-digital-ipa-technical-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("IPA unavailable");
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any())).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        assertEquals(correlationId, response.getCorrelationId());

        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA), any());
        verifyNoInteractions(infoCamereClient, inadClient);
    }

    /**
     * IPA NotFound -> IniPEC con PEC professionale valida.
     */
    @Test
    void PG_Digital_FullFlow_InipecProfessionalPecIsReturned() {
        String correlationId = "pg-inipec-professional-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, PG_PROFESSIONAL);

        GatewayResponse payload = completeInipecFinal(correlationId, PG_PROFESSIONAL, pecResponseWithProfessionals(PG_PROFESSIONAL, null, "professional@pec.it"));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        verifyNoInteractions(inadClient);
    }

    /**
     * IPA NotFound -> IniPEC con PEC impresa e professionali valide.
     */
    @Test
    void PG_Digital_FullFlow_InipecBusinessAndAllProfessionalPecsAreReturned() {
        String correlationId = "pg-inipec-business-professionals-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, PG_BUSINESS_PROFESSIONAL);

        GatewayResponse payload = completeInipecFinal(correlationId, PG_BUSINESS_PROFESSIONAL, pecResponseWithProfessionals(PG_BUSINESS_PROFESSIONAL, "business@pec.it", "professional-one@pec.it", "professional-two@pec.it"));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(3, payload.getPayload().getDigitalAddress().size());
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "business@pec.it".equals(address.getAddress()) && "IMPRESA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-one@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-two@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        verifyNoInteractions(inadClient);
    }

    /**
     * IPA NotFound -> IniPEC ER fino al limite.
     */
    @Test
    void PG_Digital_FullFlow_InipecErRetriesThenEndsInErrorAtConfiguredLimit() {
        String correlationId = "pg-inipec-er-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, PG_ER);

        String firstPollingId = "polling-first-" + UUID.randomUUID();
        String firstBatchId = submitSingleInipecBatch(correlationId, firstPollingId);

        Pec er = pecResponse(PG_ER, "ignored@pec.it");
        er.setStatoImpresa(Pec.StatoImpresaEnum.ER);

        when(infoCamereClient.callEServiceRequestPec(firstPollingId)).thenReturn(Mono.just(inipecPollingResponse(er)));
        pollInipec(firstBatchId, firstPollingId, 1);

        Map<String, AttributeValue> request = request(correlationId, "INIPEC");
        assertEquals("NOT_WORKED", request.get("status").s());
        assertEquals("1", request.get("retryCount").n());
        assertTrue(requestsByRegistry(correlationId, "INAD").isEmpty());

        String secondPollingId = "polling-second-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(secondPollingId)));

        iniPecBatchRequestJob.batchRequestScheduled();

        request = request(correlationId, "INIPEC");
        String secondBatchId = request.get("batchId").s();

        pollingJob.polling();

        when(infoCamereClient.callEServiceRequestPec(secondPollingId)).thenReturn(Mono.just(inipecPollingResponse(er)));
        pollInipec(secondBatchId, secondPollingId, 1);

        request = request(correlationId, "INIPEC");

        assertEquals("ERROR", request.get("status").s());
        assertEquals("INIPEC#ERROR", request.get("registryStatus").s());
        assertNull(request.get("resultPayload"));
        assertTrue(requestsByRegistry(correlationId, "INAD").isEmpty());
    }

    /**
     * IPA NotFound -> IniPEC polling in progress.
     */
    @Test
    void PG_Digital_FullFlow_InipecPollingInProgressSchedulesRetry() {
        String correlationId = "pg-inipec-in-progress-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, VALID_PG_PIVA);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = submitSingleInipecBatch(correlationId, pollingId);

        PnNationalRegistriesException inProgress = new PnNationalRegistriesException("List PEC in progress", 404, "Not Found", null, null, StandardCharsets.UTF_8, null);

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(inProgress));

        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> batch = findBatch(batchId);

        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("1", batch.get("inProgressRetryCount").n());
        assertEquals("WORKING", request(correlationId, "INIPEC").get("status").s());
        assertTrue(requestsByRegistry(correlationId, "INAD").isEmpty());
    }

    /**
     * IPA NotFound -> IniPEC metadata di errore.
     */
    @Test
    void PG_Digital_FullFlow_InipecErrorMetadataSchedulesRetry() {
        String correlationId = "pg-inipec-metadata-error-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, VALID_PG_PIVA);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = submitSingleInipecBatch(correlationId, pollingId);

        IniPecPollingResponse pollingResponse = new IniPecPollingResponse();
        pollingResponse.setCode("123");
        pollingResponse.setDescription("temporary error");

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));

        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> batch = findBatch(batchId);

        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("1", batch.get("errorRetryCount").n());
        assertEquals("WORKING", request(correlationId, "INIPEC").get("status").s());
        assertTrue(requestsByRegistry(correlationId, "INAD").isEmpty());
    }

    /**
     * IPA NotFound -> IniPEC con più PEC professionali valide.
     */
    @Test
    void PG_Digital_FullFlow_Inipec_AllProfessionalPecsArePreserved() {
        String correlationId = "pg-inipec-professionals-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, PG_PROFESSIONAL);

        GatewayResponse payload = completeInipecFinal(correlationId, PG_PROFESSIONAL, pecResponseWithProfessionals(PG_PROFESSIONAL, null, "professional-one@pec.it", "professional-two@pec.it"));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(2, payload.getPayload().getDigitalAddress().size());
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-one@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-two@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        verifyNoInteractions(inadClient);
    }

    /**
     * IPA NotFound -> IniPEC con PEC impresa invalida e professionale valida.
     */
    @Test
    void PG_Digital_FullFlow_Inipec_InvalidBusinessPecKeepsValidProfessionalPec() {
        String correlationId = "pg-inipec-invalid-business-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, PG_BUSINESS_PROFESSIONAL);

        GatewayResponse payload = completeInipecFinal(correlationId, PG_BUSINESS_PROFESSIONAL, pecResponseWithProfessionals(PG_BUSINESS_PROFESSIONAL, "not-an-email", "professional@pec.it"));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        verifyNoInteractions(inadClient);
    }

    // -------------------------------------------------------------------------
    // TRANSIZIONE INIPEC -> INAD MASSIVO
    // -------------------------------------------------------------------------

    /**
     * IPA NotFound -> IniPEC con PEC invalida -> INAD massivo.
     */
    @Test
    void PG_Digital_FullFlow_InipecInvalidPec_ContinuesToInadMassive() {
        GatewayResponse payload = runMassiveFunctionalScenario("pg-inipec-invalid-inad-massive-" + UUID.randomUUID(), PG_INVALID, pecResponse(PG_INVALID, "not-an-email"), inadResponse(PG_INVALID, "inad-after-invalid-inipec@pec.it", null));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("inad-after-invalid-inipec@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * IPA NotFound -> IniPEC ND -> INAD massivo.
     */
    @Test
    void PG_Digital_FullFlow_InipecNd_ContinuesToInadMassive() {
        Pec nd = pecResponse(PG_ND, "ignored-nd@pec.it");
        nd.setStatoImpresa(Pec.StatoImpresaEnum.ND);

        GatewayResponse payload = runMassiveFunctionalScenario("pg-inipec-nd-inad-massive-" + UUID.randomUUID(), PG_ND, nd, inadResponse(PG_ND, "inad-after-nd@pec.it", null));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals("inad-after-nd@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
    }

    /**
     * IPA NotFound -> IniPEC NF -> INAD massivo.
     */
    @Test
    void PG_Digital_FullFlow_InipecNf_ContinuesToInadMassive() {
        Pec nf = pecResponse(PG_NF, "ignored-nf@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);

        GatewayResponse payload = runMassiveFunctionalScenario("pg-inipec-nf-inad-massive-" + UUID.randomUUID(), PG_NF, nf, inadResponse(PG_NF, "inad-after-nf@pec.it", null));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals("inad-after-nf@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
    }

    /**
     * IPA NotFound -> IniPEC senza CF richiesto -> INAD massivo.
     */
    @Test
    void PG_Digital_FullFlow_InipecMissingTaxId_ContinuesToInadMassive() {
        GatewayResponse payload = runMassiveFunctionalScenario("pg-inipec-missing-inad-massive-" + UUID.randomUUID(), PG_MISSING, null, inadResponse(PG_MISSING, "inad-after-missing@pec.it", null));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals("inad-after-missing@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
    }

    // -------------------------------------------------------------------------
    // INAD MASSIVO: stessi scenari funzionali della suite PF, senza fallback
    // -------------------------------------------------------------------------

    /**
     * INAD massivo con PEC impresa valida.
     */
    @Test
    void PG_Digital_INAD_Massive_BusinessFound() {
        String correlationId = "pg-massive-found-" + UUID.randomUUID();
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_FOUND, nf(PG_INAD_FOUND), inadResponse(PG_INAD_FOUND, "pg@pec.it", null));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("pg@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * INAD massivo NotFound: la P.IVA richiesta non compare nel risultato.
     */
    @Test
    void PG_Digital_INAD_Massive_NotFound() {
        String correlationId = "pg-massive-not-found-" + UUID.randomUUID();
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_NOT_FOUND, nf(PG_INAD_NOT_FOUND), null);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
        assertInadIsTerminal(correlationId);
    }

    /**
     * INAD massivo con lista PEC vuota.
     */
    @Test
    void PG_Digital_INAD_Massive_EmptyList() {
        String correlationId = "pg-massive-empty-list-" + UUID.randomUUID();
        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_EMPTY, nf(PG_INAD_EMPTY), inadResponse(PG_INAD_EMPTY, List.of()));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
        assertInadIsTerminal(correlationId);
    }

    /**
     * INAD massivo con PEC impresa scaduta.
     */
    @Test
    void PG_Digital_INAD_Massive_BusinessExpired() {
        String correlationId = "pg-massive-expired-business-" + UUID.randomUUID();

        ElementDigitalAddress expiredBusiness = inadAddress("expired-business@pec.it", null);
        expiredBusiness.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_EXPIRED_BUSINESS, nf(PG_INAD_EXPIRED_BUSINESS), inadResponse(PG_INAD_EXPIRED_BUSINESS, List.of(expiredBusiness)));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
        assertInadIsTerminal(correlationId);
    }

    /**
     * INAD massivo con PEC impresa senza usageInfo.
     */
    @Test
    void PG_Digital_INAD_Massive_BusinessWithoutUsageInfo_IsValid() {
        String correlationId = "pg-massive-business-no-usage-info-" + UUID.randomUUID();

        ElementDigitalAddress business = new ElementDigitalAddress();
        business.setDigitalAddress("business-no-usage@pec.it");
        business.setPracticedProfession(null);

        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_FOUND, nf(PG_INAD_FOUND), inadResponse(PG_INAD_FOUND, List.of(business)));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("business-no-usage@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * INAD massivo con PEC impresa senza data fine validità.
     */
    @Test
    void PG_Digital_INAD_Massive_BusinessWithoutEndValidity_IsValid() {
        String correlationId = "pg-massive-business-no-end-validity-" + UUID.randomUUID();

        ElementDigitalAddress business = inadAddress("business-no-end@pec.it", null);
        business.getUsageInfo().setDateEndValidity(null);

        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_FOUND, nf(PG_INAD_FOUND), inadResponse(PG_INAD_FOUND, List.of(business)));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("business-no-end@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * Con più PEC impresa viene ignorata quella scaduta e selezionata la prima valida.
     */
    @Test
    void PG_Digital_INAD_Massive_FirstBusinessInvalid_SecondBusinessValid_SecondIsSelected() {
        String correlationId = "pg-massive-first-business-invalid-" + UUID.randomUUID();

        ElementDigitalAddress expiredBusiness = inadAddress("first-expired@pec.it", null);
        expiredBusiness.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        ResponseRequestDigitalAddress result = inadResponse(PG_INAD_FOUND, List.of(expiredBusiness, inadAddress("second-valid@pec.it", null)));

        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_FOUND, nf(PG_INAD_FOUND), result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("second-valid@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * INAD massivo con PEC impresa e professionale: viene preferita la PEC impresa.
     */
    @Test
    void PG_Digital_INAD_Massive_BusinessAndProfessional_BusinessIsSelected() {
        String correlationId = "pg-massive-business-preferred-" + UUID.randomUUID();

        ResponseRequestDigitalAddress result = inadResponse(PG_INAD_BUSINESS_AND_PROFESSIONAL, List.of(inadAddress("business@pec.it", null), inadAddress("professional@pec.it", "INGEGNERE")));

        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_BUSINESS_AND_PROFESSIONAL, nf(PG_INAD_BUSINESS_AND_PROFESSIONAL), result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
        assertInadIsTerminal(correlationId);
    }

    /**
     * INAD massivo con PEC impresa assente e professionale presente.
     */
    @Test
    void PG_Digital_INAD_Massive_BusinessMissing_ProfessionalPresent() {
        String correlationId = "pg-massive-professional-only-" + UUID.randomUUID();

        ResponseRequestDigitalAddress result = inadResponse(PG_INAD_PROFESSIONAL_ONLY, List.of(inadAddress("professional@pec.it", "INGEGNERE")));

        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_PROFESSIONAL_ONLY, nf(PG_INAD_PROFESSIONAL_ONLY), result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
        assertInadIsTerminal(correlationId);
    }

    /**
     * INAD massivo con PEC impresa invalida e professionale valida.
     */
    @Test
    void PG_Digital_INAD_Massive_BusinessInvalid_ProfessionalValid() {
        String correlationId = "pg-massive-invalid-business-professional-" + UUID.randomUUID();

        ResponseRequestDigitalAddress result = inadResponse(PG_INAD_INVALID_BUSINESS, List.of(inadAddress("2026-01-01", null), inadAddress("professional@pec.it", "INGEGNERE")));

        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_INVALID_BUSINESS, nf(PG_INAD_INVALID_BUSINESS), result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
        assertInadIsTerminal(correlationId);
    }

    /**
     * INAD massivo con PEC impresa assente e più PEC professionali.
     */
    @Test
    void PG_Digital_INAD_Massive_MultipleProfessionals() {
        String correlationId = "pg-massive-multiple-professionals-" + UUID.randomUUID();

        ResponseRequestDigitalAddress result = inadResponse(PG_INAD_MULTIPLE_PROFESSIONALS, List.of(inadAddress("prof1@pec.it", "INGEGNERE"), inadAddress("prof2@pec.it", "ARCHITETTO")));

        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_MULTIPLE_PROFESSIONALS, nf(PG_INAD_MULTIPLE_PROFESSIONALS), result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
        assertInadIsTerminal(correlationId);
    }

    /**
     * Errore tecnico INAD massivo durante il recupero del risultato.
     */
    @Test
    void PG_Digital_INAD_Massive_Error() {
        List<TestRequest> requests = createNineRequests("pg-error");
        enqueueMassiveRequests(requests);

        String pollingId = "polling-error-" + UUID.randomUUID();
        String batchId = submitInadBatch(pollingId, requests);

        pollingJob.polling();

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.error(new RuntimeException("INAD unavailable")));

        pollInad(batchId, pollingId, 9);

        NationalRegistriesBatch batch = getBatch(batchId);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(1, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = request(testRequest.correlationId(), "INAD");
            assertEquals("WORKING", stored.get("status").s());
            assertNull(stored.get("resultPayload"));
        }
    }

    /**
     * INAD massivo con PEC impresa scaduta e professionale valida.
     */
    @Test
    void PG_Digital_INAD_Massive_BusinessExpired_ProfessionalValid() {
        String correlationId = "pg-massive-expired-business-professional-" + UUID.randomUUID();

        ElementDigitalAddress expiredBusiness = inadAddress("expired-business@pec.it", null);
        expiredBusiness.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        ResponseRequestDigitalAddress result = inadResponse(PG_INAD_EXPIRED_BUSINESS_AND_PROFESSIONAL, List.of(expiredBusiness, inadAddress("professional@pec.it", "INGEGNERE")));

        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_EXPIRED_BUSINESS_AND_PROFESSIONAL, nf(PG_INAD_EXPIRED_BUSINESS_AND_PROFESSIONAL), result);
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
        assertInadIsTerminal(correlationId);
    }

    /**
     * INAD massivo con sola PEC professionale scaduta.
     */
    @Test
    void PG_Digital_INAD_Massive_OnlyProfessionalExpired() {
        String correlationId = "pg-massive-only-professional-expired-" + UUID.randomUUID();

        ElementDigitalAddress expiredProfessional = inadAddress("expired-professional@pec.it", "AVVOCATO");
        expiredProfessional.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        GatewayResponse payload = runMassiveFunctionalScenario(correlationId, PG_INAD_PROFESSIONAL_ONLY, nf(PG_INAD_PROFESSIONAL_ONLY), inadResponse(PG_INAD_PROFESSIONAL_ONLY, List.of(expiredProfessional)));
        assertEquals(CX_ID, payload.getClientId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
        assertInadIsTerminal(correlationId);
    }

    /**
     * INAD restituisce più risultati per la stessa P.IVA: viene preso il primo.
     */
    @Test
    void PG_Digital_INAD_Massive_DuplicateTaxIdKeepsFirstProviderResult() {
        List<TestRequest> requests = createNineRequests("duplicate-taxid");
        enqueueMassiveRequests(requests);

        String pollingId = "polling-duplicate-" + UUID.randomUUID();
        String batchId = submitInadBatch(pollingId, requests);

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

        Map<String, AttributeValue> stored = request(requests.getFirst().correlationId(), "INAD");
        GatewayResponse payload = readNewOutputMessage(stored.get("resultPayload").s());

        assertEquals("first@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertFalse(stored.get("resultPayload").s().contains("second@pec.it"));
    }

    // -------------------------------------------------------------------------
    // INAD MASSIVO: resilienza / recovery / concorrenza / retry
    // -------------------------------------------------------------------------

    /**
     * Provider accetta il batch ma la persistenza locale fallisce: al retry viene creato un nuovo batch.
     */
    @Test
    void InadBatch_ProviderAcceptedThenPersistenceFails_RestartCreatesNewBatchForRequests() {
        List<TestRequest> requests = createNineRequests("batch-persist-failure");
        enqueueMassiveRequests(requests);

        String firstPollingId = "polling-persist-first-" + UUID.randomUUID();
        String firstBatchId = submitInadBatch(firstPollingId, requests);

        dynamoDbClient.deleteItem(builder -> builder.tableName(NATIONAL_REGISTRIES_BATCHES_TABLE).key(Map.of("batchId", AttributeValue.builder().s(firstBatchId).build(), "sk", AttributeValue.builder().s("-").build())));

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> restored = new HashMap<>(request(testRequest.correlationId(), "INAD"));

            restored.put("status", AttributeValue.builder().s("NOT_WORKED").build());
            restored.put("registryStatus", AttributeValue.builder().s("INAD#NOT_WORKED").build());
            restored.remove("batchId");

            putRequestItem(restored);
        }

        assertNull(nationalRegistriesBatchesRepository.getBatch(firstBatchId).block());

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = request(testRequest.correlationId(), "INAD");
            assertEquals("NOT_WORKED", stored.get("status").s());
            assertNull(stored.get("batchId"));
        }

        String secondPollingId = "polling-persist-second-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress secondResponse = new ResponseRequestListDigitalAddress();
        secondResponse.setId(secondPollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(secondResponse));

        inadBatchRequestJob.batchRequest();

        verify(inadClient, times(2)).callMassiveEService(any(RequestListDigitalAddress.class));

        String secondBatchId = request(requests.getFirst().correlationId(), "INAD").get("batchId").s();
        assertNotEquals(firstBatchId, secondBatchId);

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = request(testRequest.correlationId(), "INAD");
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
     * Aggiornamento parziale delle request: il nuovo batch prende solo quella rimasta NOT_WORKED.
     */
    @Test
    void InadBatch_PartialRequestWorkingUpdate_RestartCreatesNewBatchOnlyForRemainingRequest() {
        List<TestRequest> requests = createNineRequests("partial-working");
        enqueueMassiveRequests(requests);

        String firstPollingId = "polling-partial-working-first-" + UUID.randomUUID();
        String firstBatchId = submitInadBatch(firstPollingId, requests);

        TestRequest requestToRestore = requests.get(1);

        Map<String, AttributeValue> partialRequest = new HashMap<>(request(requestToRestore.correlationId(), "INAD"));

        partialRequest.put("status", AttributeValue.builder().s("NOT_WORKED").build());
        partialRequest.put("registryStatus", AttributeValue.builder().s("INAD#NOT_WORKED").build());
        partialRequest.remove("batchId");
        putRequestItem(partialRequest);

        Map<String, AttributeValue> beforeRestart = request(requestToRestore.correlationId(), "INAD");
        assertEquals("NOT_WORKED", beforeRestart.get("status").s());
        assertNull(beforeRestart.get("batchId"));

        List<TestRequest> fillers = createNineRequests("partial-working-second-batch").subList(0, 8);
        enqueueMassiveRequests(fillers);

        String secondPollingId = "polling-partial-working-second-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress secondResponse = new ResponseRequestListDigitalAddress();
        secondResponse.setId(secondPollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(secondResponse));

        inadBatchRequestJob.batchRequest();

        verify(inadClient, times(2)).callMassiveEService(any(RequestListDigitalAddress.class));

        Map<String, AttributeValue> recovered = request(requestToRestore.correlationId(), "INAD");
        assertEquals("WORKING", recovered.get("status").s());
        assertEquals("INAD#WORKING", recovered.get("registryStatus").s());
        assertNotNull(recovered.get("batchId"));

        String secondBatchId = recovered.get("batchId").s();
        assertNotEquals(firstBatchId, secondBatchId);

        for (int i = 0; i < requests.size(); i++) {
            if (i == 1) {
                continue;
            }

            Map<String, AttributeValue> stored = request(requests.get(i).correlationId(), "INAD");
            assertEquals("WORKING", stored.get("status").s());
            assertEquals("INAD#WORKING", stored.get("registryStatus").s());
            assertEquals(firstBatchId, stored.get("batchId").s());
        }

        for (TestRequest filler : fillers) {
            Map<String, AttributeValue> stored = request(filler.correlationId(), "INAD");
            assertEquals("WORKING", stored.get("status").s());
            assertEquals(secondBatchId, stored.get("batchId").s());
        }

        NationalRegistriesBatch firstBatch = getBatch(firstBatchId);
        NationalRegistriesBatch secondBatch = getBatch(secondBatchId);

        assertNotNull(firstBatch);
        assertNotNull(secondBatch);
        assertEquals(firstPollingId, firstBatch.getPollingId());
        assertEquals(secondPollingId, secondBatch.getPollingId());
    }

    /**
     * Due worker concorrenti tentano di acquisire lo stesso batch: una sola reservation ha successo.
     */
    @Test
    void Polling_TwoWorkersCompeteForSameBatch_OnlyOneReservationWins() {
        List<TestRequest> requests = createNineRequests("polling-concurrent");
        enqueueMassiveRequests(requests);

        String pollingId = "polling-concurrent-" + UUID.randomUUID();
        String batchId = submitInadBatch(pollingId, requests);

        assertEquals(BatchesStatusEnum.REQUEST_SENT, getBatch(batchId).getStatus());

        CompletableFuture<Void> workerOne = CompletableFuture.runAsync(pollingJob::polling);
        CompletableFuture<Void> workerTwo = CompletableFuture.runAsync(pollingJob::polling);

        CompletableFuture.allOf(workerOne, workerTwo).join();

        NationalRegistriesBatch persisted = nationalRegistriesBatchesRepository.getBatch(batchId).block();

        assertNotNull(persisted);
        assertEquals(BatchesStatusEnum.WORKING, persisted.getStatus());
    }

    /**
     * Replay del polling dopo persistenza parziale: completa solo la request rimasta WORKING.
     */
    @Test
    void InadPolling_PartialRequestProcessing_ReplayCompletesOnlyRemainingRequest() {
        List<TestRequest> requests = createNineRequests("partial-poll");
        enqueueMassiveRequests(requests);

        String pollingId = "partial-polling-" + UUID.randomUUID();
        String batchId = submitInadBatch(pollingId, requests);

        pollingJob.polling();

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));

        List<ResponseRequestDigitalAddress> providerResults = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            providerResults.add(inadResponse(requests.get(i).taxId(), "partial-" + i + "@pec.it", null));
        }

        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(providerResults);

        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(result));

        PnPollingEvent.Payload payload = PnPollingEvent.Payload.builder().registry(GatewayDownstreamService.INAD).batchId(batchId).pollingId(pollingId).batchSize(9).errorRetryCount(0).inProgressRetryCount(0).build();

        inadPollingService.handleMessage(payload).block();

        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());

        Map<String, String> originalPayloads = new HashMap<>();

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = request(testRequest.correlationId(), "INAD");
            assertEquals("WORKED", stored.get("status").s());
            originalPayloads.put(testRequest.correlationId(), stored.get("resultPayload").s());
        }

        TestRequest requestToReplay = requests.get(1);

        Map<String, AttributeValue> partial = new HashMap<>(request(requestToReplay.correlationId(), "INAD"));
        partial.put("status", AttributeValue.builder().s("WORKING").build());
        partial.put("registryStatus", AttributeValue.builder().s("INAD#WORKING").build());
        partial.remove("resultPayload");
        putRequestItem(partial);

        Map<String, AttributeValue> batch = new HashMap<>(findBatch(batchId));
        batch.put("status", AttributeValue.builder().s("WORKING").build());
        putBatchItem(batch);

        assertEquals("WORKING", request(requestToReplay.correlationId(), "INAD").get("status").s());

        inadPollingService.handleMessage(payload).block();

        for (int i = 0; i < requests.size(); i++) {
            TestRequest testRequest = requests.get(i);
            Map<String, AttributeValue> stored = request(testRequest.correlationId(), "INAD");

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
     * Esaurimento dei retry INAD attraverso cicli reali di polling.
     */
    @Test
    void InadPolling_ErrorRetryExhaustion_AcrossRealCycles() {
        int maxRetry = 4;

        Flow flow = startWorkingInadFlow("inad-error-exhaustion-" + UUID.randomUUID(), PG_INAD_FOUND);

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
        assertEquals("ERROR", request(flow.correlationId(), "INAD").get("status").s());
        assertInadIsTerminal(flow.correlationId());
    }

    /**
     * Correlazione stabile con paginazione, risultati fuori ordine e risultati mancanti.
     */
    @Test
    void InadMassive_PaginationAndCorrelationRemainStableWithOutOfOrderAndMissingResults() {
        String sharedTaxId = "34565000469";

        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("corr~separator-" + UUID.randomUUID(), sharedTaxId);
        cases.put("corr-same-cf-" + UUID.randomUUID(), sharedTaxId);
        cases.put("corr-b-" + UUID.randomUUID(), "59551540442");
        cases.put("corr-c-" + UUID.randomUUID(), "38598790426");
        cases.put("corr-missing-" + UUID.randomUUID(), "10959831008");
        cases.put("corr-filler-e-" + UUID.randomUUID(), "12071426170");
        cases.put("corr-filler-f-" + UUID.randomUUID(), "71935270349");
        cases.put("corr-filler-g-" + UUID.randomUUID(), "01333214631");
        cases.put("corr-filler-h-" + UUID.randomUUID(), "01113570442");

        List<TestRequest> requests = cases.entrySet().stream().map(entry -> new TestRequest(entry.getValue(), entry.getKey())).toList();

        enqueueMassiveRequests(requests);

        String pollingId = "inad-correlation-" + UUID.randomUUID();

        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();
        batchResponse.setId(pollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(batchResponse));

        inadBatchRequestJob.batchRequest();

        String batchId = request(cases.keySet().iterator().next(), "INAD").get("batchId").s();

        pollingJob.polling();

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));

        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of(inadResponse("38598790426", "c@pec.it", null), inadResponse(sharedTaxId, "shared@pec.it", null), inadResponse("59551540442", "b@pec.it", null), inadResponse("12071426170", "filler-e@pec.it", null), inadResponse("71935270349", "filler-f@pec.it", null), inadResponse("01333214631", "filler-g@pec.it", null), inadResponse("01113570442", "filler-h@pec.it", null)));

        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(result));

        inadPollingService.handleMessage(payload(GatewayDownstreamService.INAD, batchId, pollingId, 9, 0, 0)).block();

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            Map<String, AttributeValue> stored = request(entry.getKey(), "INAD");

            assertEquals("WORKED", stored.get("status").s());
            assertNotNull(stored.get("resultPayload"));

            GatewayResponse resultPayload = readNewOutputMessage(stored.get("resultPayload").s());
            assertEquals(entry.getKey(), resultPayload.getPayload().getCorrelationId());

            if (sharedTaxId.equals(entry.getValue())) {
                assertEquals(1, resultPayload.getPayload().getDigitalAddress().size());
                assertEquals("shared@pec.it", resultPayload.getPayload().getDigitalAddress().getFirst().getAddress());
            }

            if ("59551540442".equals(entry.getValue())) {
                assertEquals(1, resultPayload.getPayload().getDigitalAddress().size());
                assertEquals("b@pec.it", resultPayload.getPayload().getDigitalAddress().getFirst().getAddress());
            }

            if ("38598790426".equals(entry.getValue())) {
                assertEquals(1, resultPayload.getPayload().getDigitalAddress().size());
                assertEquals("c@pec.it", resultPayload.getPayload().getDigitalAddress().getFirst().getAddress());
            }

            if ("10959831008".equals(entry.getValue())) {
                assertNotNull(resultPayload.getPayload().getDigitalAddress());
                assertTrue(resultPayload.getPayload().getDigitalAddress().isEmpty());
            }
        }

        assertEquals(BatchesStatusEnum.WORKED, getBatch(batchId).getStatus());
    }
    
    /**
     * INAD massivo in progress fino all'esaurimento dei retry.
     */
    @Test
    void PG_Digital_FullFlow_InadMassive_InProgressRetryExhaustionMarksRequestAndBatchError() {
        int maxRetry = 10;

        Flow flow = startWorkingInadFlow(
                "pg-massive-in-progress-exhausted-" + UUID.randomUUID(),
                VALID_PG_PIVA
        );

        ResponseStatusListDigitalAddress status = new ResponseStatusListDigitalAddress();
        status.setState(StatusProcessingRequest.IN_ELABORAZIONE);

        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status));

        for (int retry = 0; retry < maxRetry - 1; retry++) {
            pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(
                    MessageBuilder.withPayload(pollingPayload(flow, 0, retry)).build()
            );

            Map<String, AttributeValue> batch = findBatch(flow.batchId());

            assertEquals("REQUEST_SENT", batch.get("status").s());
            assertEquals(String.valueOf(retry + 1), batch.get("inProgressRetryCount").n());

            makeBatchImmediatelyRetryable(flow.batchId());
            pollingJob.polling();

            assertEquals("WORKING", findBatch(flow.batchId()).get("status").s());
        }

        pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(
                MessageBuilder.withPayload(pollingPayload(flow, 0, maxRetry - 1)).build()
        );

        assertEquals("ERROR", findBatch(flow.batchId()).get("status").s());

        Map<String, AttributeValue> request = requestByRegistry(flow.correlationId(), "INAD");

        assertEquals("ERROR", request.get("status").s());
        assertEquals("INAD#ERROR", request.get("registryStatus").s());
    }

    // -------------------------------------------------------------------------
    // Helper PG -> INIPEC -> INAD MASSIVO
    // -------------------------------------------------------------------------

    private Map<String, AttributeValue> requestByRegistry(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
    }

    private GatewayResponse runMassiveFunctionalScenario(
            String correlationId,
            String taxId,
            Pec targetInipecResult,
            ResponseRequestDigitalAddress targetInadResult
    ) {
        List<TestRequest> requests = new ArrayList<>();
        requests.add(new TestRequest(taxId, correlationId));

        for (int i = 0; i < 8; i++) {
            requests.add(new TestRequest(pgFillerTaxId(i), "functional-filler-" + i + "-" + UUID.randomUUID()));
        }

        enqueueMassiveRequests(requests, targetInipecResult);

        String pollingId = "polling-functional-" + UUID.randomUUID();
        String batchId = submitInadBatch(pollingId, requests);

        pollingJob.polling();

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(status(StatusProcessingRequest.DISPONIBILE)));

        List<ResponseRequestDigitalAddress> providerResults = new ArrayList<>();

        if (targetInadResult != null) {
            providerResults.add(targetInadResult);
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

        Map<String, AttributeValue> stored = request(correlationId, "INAD");
        assertEquals("WORKED", stored.get("status").s());
        assertEquals("INAD#WORKED", stored.get("registryStatus").s());
        assertEquals(batchId, stored.get("batchId").s());
        assertNotNull(stored.get("resultPayload"));

        verify(inadClient, never()).callEService(any(), any());
        assertInadIsTerminal(correlationId);

        return readNewOutputMessage(stored.get("resultPayload").s());
    }

    private void enqueueMassiveRequests(List<TestRequest> requests) {
        enqueueMassiveRequests(requests, null);
    }

    /**
     * Porta un gruppo PG fino a INAD#NOT_WORKED.
     *
     * Per il primo elemento puo' essere passato un risultato IniPEC specifico
     * (invalid / ND / NF / null). Tutti i filler ricevono NF, quindi proseguono a INAD.
     */
    private void enqueueMassiveRequests(List<TestRequest> requests, Pec firstInipecResult) {
        for (TestRequest testRequest : requests) {
            when(ipaClient.callEServiceWS23(eq(testRequest.taxId()), any())).thenReturn(Mono.just(ipaNotFoundResponse()));

            AddressOKDto response = enqueueGatewayRequest("PG", testRequest.taxId(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, testRequest.correlationId());

            processNextGatewayInputMessage();

            assertEquals(testRequest.correlationId(), response.getCorrelationId());
            assertInipecPendingRequest(testRequest.correlationId(), testRequest.taxId());
        }

        String inipecPollingId = "inipec-massive-preparation-" + UUID.randomUUID();
        String inipecBatchId = submitInipecBatchForRequests(inipecPollingId, requests);

        List<Pec> inipecResults = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            TestRequest testRequest = requests.get(i);

            if (i == 0 && firstInipecResult == null) {
                // target assente dalla risposta IniPEC -> continua a INAD
                continue;
            }

            if (i == 0) {
                inipecResults.add(firstInipecResult);
                continue;
            }

            inipecResults.add(nf(testRequest.taxId()));
        }

        when(infoCamereClient.callEServiceRequestPec(inipecPollingId)).thenReturn(Mono.just(inipecPollingResponse(inipecResults.toArray(Pec[]::new))));

        pollInipec(inipecBatchId, inipecPollingId, requests.size());

        assertEquals("WORKED", findBatch(inipecBatchId).get("status").s());

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> inipec = request(testRequest.correlationId(), "INIPEC");
            assertEquals("WORKED", inipec.get("status").s());
            assertEquals("INIPEC#WORKED", inipec.get("registryStatus").s());
            assertNull(inipec.get("resultPayload"));

            Map<String, AttributeValue> inad = request(testRequest.correlationId(), "INAD");
            assertEquals("NOT_WORKED", inad.get("status").s());
            assertEquals("INAD#NOT_WORKED", inad.get("registryStatus").s());
            assertNull(inad.get("batchId"));
            assertNull(inad.get("resultPayload"));
        }
    }

    private String submitInipecBatchForRequests(String pollingId, List<TestRequest> requests) {
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));

        iniPecBatchRequestJob.batchRequestScheduled();

        String batchId = null;

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = request(testRequest.correlationId(), "INIPEC");

            assertEquals("WORKING", stored.get("status").s());
            assertEquals("INIPEC#WORKING", stored.get("registryStatus").s());
            assertNotNull(stored.get("batchId"));

            if (batchId == null) {
                batchId = stored.get("batchId").s();
            } else {
                assertEquals(batchId, stored.get("batchId").s());
            }
        }

        assertNotNull(batchId);
        assertEquals("REQUEST_SENT", findBatch(batchId).get("status").s());

        pollingJob.polling();
        assertEquals("WORKING", findBatch(batchId).get("status").s());

        return batchId;
    }

    private Pec nf(String taxId) {
        Pec nf = pecResponse(taxId, "ignored@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        return nf;
    }

    // -------------------------------------------------------------------------
    // Helper INAD MASSIVO
    // -------------------------------------------------------------------------

    private String submitInadBatch(String pollingId, List<TestRequest> requests) {
        ResponseRequestListDigitalAddress response = new ResponseRequestListDigitalAddress();
        response.setId(pollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(response));

        inadBatchRequestJob.batchRequest();

        String batchId = assertSameWorkingInadBatch(requests);
        NationalRegistriesBatch batch = getBatch(batchId);

        assertNotNull(batch);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(pollingId, batch.getPollingId());

        return batchId;
    }

    private String assertSameWorkingInadBatch(List<TestRequest> requests) {
        String batchId = null;

        for (TestRequest testRequest : requests) {
            Map<String, AttributeValue> stored = request(testRequest.correlationId(), "INAD");

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
        inadPollingService.handleMessage(PnPollingEvent.Payload.builder().registry(GatewayDownstreamService.INAD).batchId(batchId).pollingId(pollingId).batchSize(batchSize).errorRetryCount(0).inProgressRetryCount(0).build()).block();
    }

    private ResponseStatusListDigitalAddress status(StatusProcessingRequest state) {
        ResponseStatusListDigitalAddress response = new ResponseStatusListDigitalAddress();
        response.setState(state);
        return response;
    }

    private Flow startWorkingInadFlow(String correlationId, String taxId) {
        List<TestRequest> requests = new ArrayList<>();
        requests.add(new TestRequest(taxId, correlationId));

        for (int i = 0; i < 8; i++) {
            requests.add(new TestRequest(pgFillerTaxId(i), "inad-flow-filler-" + i + "-" + UUID.randomUUID()));
        }

        enqueueMassiveRequests(requests);

        String pollingId = "inad-polling-" + UUID.randomUUID();
        String batchId = submitInadBatch(pollingId, requests);

        pollingJob.polling();
        assertEquals("WORKING", findBatch(batchId).get("status").s());

        return new Flow(correlationId, batchId, pollingId);
    }

    private PnPollingEvent.Payload pollingPayload(
            Flow flow,
            int errorRetryCount,
            int inProgressRetryCount
    ) {
        return PnPollingEvent.Payload.builder().registry(GatewayDownstreamService.INAD).batchId(flow.batchId()).pollingId(flow.pollingId()).batchSize(9).errorRetryCount(errorRetryCount).inProgressRetryCount(inProgressRetryCount).build();
    }

    private PnPollingEvent.Payload payload(
            GatewayDownstreamService registry,
            String batchId,
            String pollingId,
            int batchSize,
            int errorRetry,
            int inProgressRetry
    ) {
        return PnPollingEvent.Payload.builder().registry(registry).batchId(batchId).pollingId(pollingId).batchSize(batchSize).errorRetryCount(errorRetry).inProgressRetryCount(inProgressRetry).build();
    }

    private void makeBatchImmediatelyRetryable(String batchId) {
        Map<String, AttributeValue> batch = new HashMap<>(findBatch(batchId));
        batch.put("attemptAfter", AttributeValue.builder().s(Instant.now().minusSeconds(1).toString()).build());
        putBatchItem(batch);
    }

    private void assertInadIsTerminal(String correlationId) {
        List<Map<String, AttributeValue>> all = findNationalRegistriesRequestsByCorrelationId(correlationId);

        assertFalse(all.isEmpty());
        assertEquals(1, requestsByRegistry(correlationId, "INAD").size());

        // Per PG non deve comparire alcun registry successivo a INAD.
        // Le sole request persistite ammesse sono quelle della catena IniPEC -> INAD.
        assertTrue(all.stream().allMatch(item -> {
            String registry = item.get("registry").s();
            return "INIPEC".equals(registry) || "INAD".equals(registry);
        }));
    }

    private List<TestRequest> createNineRequests(String prefix) {
        List<TestRequest> requests = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            requests.add(new TestRequest(pgFillerTaxId(i), prefix + "-" + i + "-" + UUID.randomUUID()));
        }

        return requests;
    }

    private static final List<String> PG_FILLER_TAX_IDS = List.of(
            "70000000003",
            "70000000011",
            "70000000029",
            "70000000037",
            "70000000045",
            "70000000052",
            "70000000060",
            "70000000078",
            "70000000086"
    );

    private String pgFillerTaxId(int index) {
        return PG_FILLER_TAX_IDS.get(index);
    }

    // -------------------------------------------------------------------------
    // Helper originali PG / IniPEC
    // -------------------------------------------------------------------------

    private void startInipecFromIpaNotFound(String correlationId, String taxId) {
        when(ipaClient.callEServiceWS23(eq(taxId), any())).thenReturn(Mono.just(ipaNotFoundResponse()));

        AddressOKDto response = enqueueGatewayRequest("PG", taxId, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertInipecPendingRequest(correlationId, taxId);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
    }

    private GatewayResponse completeInipecFinal(String correlationId, String taxId, Pec... pecs) {
        String pollingId = "inipec-full-flow-" + UUID.randomUUID();
        String batchId = submitSingleInipecBatch(correlationId, pollingId);

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(pecs)));

        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> request = request(correlationId, "INIPEC");

        assertEquals(taxId, request.get("taxId").s());
        assertEquals("WORKED", request.get("status").s());
        assertEquals("INIPEC#WORKED", request.get("registryStatus").s());
        assertNotNull(request.get("resultPayload"));

        return readNewOutputMessage(request.get("resultPayload").s());
    }

    private String submitSingleInipecBatch(String correlationId, String pollingId) {
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));

        iniPecBatchRequestJob.batchRequestScheduled();

        Map<String, AttributeValue> request = request(correlationId, "INIPEC");

        assertEquals("WORKING", request.get("status").s());
        assertEquals("INIPEC#WORKING", request.get("registryStatus").s());
        assertNotNull(request.get("batchId"));

        String batchId = request.get("batchId").s();

        assertEquals("REQUEST_SENT", findBatch(batchId).get("status").s());

        pollingJob.polling();
        assertEquals("WORKING", findBatch(batchId).get("status").s());

        return batchId;
    }

    private void assertInipecPendingRequest(String correlationId, String taxId) {
        Map<String, AttributeValue> storedRequest = request(correlationId, "INIPEC");

        assertEquals(taxId, storedRequest.get("taxId").s());
        assertEquals("PG", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    private void pollInipec(String batchId, String pollingId, int batchSize) {
        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, batchId, pollingId, batchSize)).block();
    }

    private PnPollingEvent.Payload pollingPayload(
            GatewayDownstreamService registry,
            String batchId,
            String pollingId,
            int batchSize
    ) {
        return PnPollingEvent.Payload.builder().registry(registry).batchId(batchId).pollingId(pollingId).batchSize(batchSize).errorRetryCount(0).inProgressRetryCount(0).build();
    }

    private IniPecPollingResponse inipecPollingResponse(Pec... pecs) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pecs));
        return response;
    }

    private Pec pecResponseWithProfessionals(
            String taxId,
            String pecImpresa,
            String... professionalPecs
    ) {
        Map<String, Object> value = new HashMap<>();
        value.put("cf", taxId);

        if (pecImpresa != null) {
            value.put("pecImpresa", pecImpresa);
        }

        value.put("pecProfessionista", Arrays.stream(professionalPecs).map(pec -> Map.of("pec", pec)).toList());

        return objectMapper.convertValue(value, Pec.class);
    }

    private WS23ResponseDto ipaMultipleResponse() {
        ResultDto result = new ResultDto();
        result.setNumItems(2);
        result.setCodErr(0);
        result.setDescErr("");

        DataWS23Dto first = new DataWS23Dto();
        first.setCodAmm("cod-amm-1");
        first.setDesAmm("Ente 1");
        first.setTipo("PA");
        first.setDomicilioDigitale("first@pec.it");

        DataWS23Dto second = new DataWS23Dto();
        second.setCodAmm("cod-amm-2");
        second.setDesAmm("Ente 2");
        second.setTipo("PA");
        second.setDomicilioDigitale("second@pec.it");

        WS23ResponseDto response = new WS23ResponseDto();
        response.setResult(result);
        response.setData(List.of(first, second));

        return response;
    }

    private WS05ResponseDto ipaWs05Response(String email) {
        ResultDto result = new ResultDto();
        result.setNumItems(1);
        result.setCodErr(0);
        result.setDescErr("");

        DataWS05Dto data = new DataWS05Dto();
        data.setCodAmm("cod-amm-1");
        data.setDesAmm("Ente 1");
        data.setMail1(email);

        WS05ResponseDto response = new WS05ResponseDto();
        response.setResult(result);
        response.setData(data);

        return response;
    }

    // -------------------------------------------------------------------------
    // Dynamo helpers
    // -------------------------------------------------------------------------

    private List<Map<String, AttributeValue>> requestsByRegistry(
            String correlationId,
            String registry
    ) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream().filter(request -> registry.equals(request.get("registry").s())).toList();
    }

    private Map<String, AttributeValue> request(
            String correlationId,
            String registry
    ) {
        return requestsByRegistry(correlationId, registry).stream().findFirst().orElseThrow();
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

    private record Flow(
            String correlationId,
            String batchId,
            String pollingId
    ) {
    }

    private record TestRequest(
            String taxId,
            String correlationId
    ) {
    }
}
