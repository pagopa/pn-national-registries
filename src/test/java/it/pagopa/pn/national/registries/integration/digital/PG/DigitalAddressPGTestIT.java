package it.pagopa.pn.national.registries.integration.digital.PG;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS05Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS23Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.ResultDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS05ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS23ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.integration.AbstractNationalRegistriesComponentIT;
import it.pagopa.pn.national.registries.integration.PnNationalRegistriesTestApplication;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.GatewayResponse;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
 *  - INAD massivo disattivato
 *  - old IniPEC disattivo
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.dao.batchesTableName=pn-NationalRegistriesBatches",
        "pn.national-registries.inipec.batch-request-pk-separator=~",
        "pn.national-registries.enable-inad-massive-api=false",
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
        "spring.task.scheduling.enabled=false"
})
public class DigitalAddressPGTestIT extends AbstractNationalRegistriesComponentIT {

    private static final String PG_PROFESSIONAL = "56815563368";
    private static final String PG_BUSINESS_PROFESSIONAL = "28223913097";
    private static final String PG_INVALID = "39274018561";
    private static final String PG_ND = "83016259471";
    private static final String PG_NF = "12825810299";
    private static final String PG_MISSING = "00534280912";
    private static final String PG_ER = "03649090796";

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private InipecPollingService inipecPollingService;

    @Autowired
    private PollingJob pollingJob;

    /**
     * IPA diretto e Gateway PG restituiscono la stessa PEC.
     */
    @Test
    void Ipa_DirectEndpointAndGateway_ReturnSameFunctionalAddress() {
        String taxId = VALID_PG_PIVA;
        String correlationId = "ipa-gateway-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(taxId), anyString())).thenReturn(Mono.just(ipaFoundResponse(taxId, "same-ipa@pec.it")));

        IPARequestBodyDto directRequest = convertToGetIpaPecRequest(buildGatewayRequest("ipa-direct-" + UUID.randomUUID(), taxId, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL));
        IPAPecDto direct = webTestClient.post()
                .uri("/national-registries-private/ipa/pec")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(directRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(IPAPecDto.class)
                .returnResult().getResponseBody();

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

        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals("inipec-after-invalid-ipa@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        verifyNoInteractions(inadClient);
    }

    /**
     * IPA errore tecnico: nessun fallback.
     */
    @Test
    void PG_Digital_IPA_TechnicalError_DoesNotFallback() {
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

        GatewayResponse payload = completeInipecFinal(
                correlationId,
                PG_BUSINESS_PROFESSIONAL,
                pecResponseWithProfessionals(PG_BUSINESS_PROFESSIONAL, "business@pec.it", "professional-one@pec.it", "professional-two@pec.it")
        );

        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(3, payload.getPayload().getDigitalAddress().size());
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "business@pec.it".equals(address.getAddress()) && "IMPRESA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-one@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-two@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        verifyNoInteractions(inadClient);
    }

    /**
     * IPA NotFound -> IniPEC con PEC invalida -> INAD sync.
     */
    @Test
    void PG_Digital_FullFlow_InipecInvalidPecFallsBackToInadSync() {
        String correlationId = "pg-inipec-invalid-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, PG_INVALID);
        when(inadClient.callEService(eq(PG_INVALID), eq(correlationId))).thenReturn(Mono.just(inadResponse(PG_INVALID, "inad-after-invalid-inipec@pec.it", null)));

        completeInipecPolling(correlationId, inipecPollingResponse(pecResponse(PG_INVALID, "not-an-email")));

        assertInadFinal(correlationId, "inad-after-invalid-inipec@pec.it");
    }

    /**
     * IPA NotFound -> IniPEC ND -> INAD sync.
     */
    @Test
    void PG_Digital_FullFlow_InipecNdFallsBackToInadSync() {
        String correlationId = "pg-inipec-nd-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, PG_ND);

        Pec nd = pecResponse(PG_ND, "ignored-nd@pec.it");
        nd.setStatoImpresa(Pec.StatoImpresaEnum.ND);

        when(inadClient.callEService(eq(PG_ND), eq(correlationId))).thenReturn(Mono.just(inadResponse(PG_ND, "inad-after-nd@pec.it", null)));

        completeInipecPolling(correlationId, inipecPollingResponse(nd));

        assertInadFinal(correlationId, "inad-after-nd@pec.it");
    }

    /**
     * IPA NotFound -> IniPEC NF -> INAD sync.
     */
    @Test
    void PG_Digital_FullFlow_InipecNfFallsBackToInadSync() {
        String correlationId = "pg-inipec-nf-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, PG_NF);

        Pec nf = pecResponse(PG_NF, "ignored-nf@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);

        when(inadClient.callEService(eq(PG_NF), eq(correlationId))).thenReturn(Mono.just(inadResponse(PG_NF, "inad-after-nf@pec.it", null)));

        completeInipecPolling(correlationId, inipecPollingResponse(nf));

        assertInadFinal(correlationId, "inad-after-nf@pec.it");
    }

    /**
     * IPA NotFound -> IniPEC senza CF richiesto -> INAD sync.
     */
    @Test
    void PG_Digital_FullFlow_InipecMissingTaxIdFallsBackToInadSync() {
        String correlationId = "pg-inipec-missing-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, PG_MISSING);
        when(inadClient.callEService(eq(PG_MISSING), eq(correlationId))).thenReturn(Mono.just(inadResponse(PG_MISSING, "inad-after-missing@pec.it", null)));

        completeInipecPolling(correlationId, inipecPollingResponse());

        assertInadFinal(correlationId, "inad-after-missing@pec.it");
    }

    /**
     * IPA NotFound -> IniPEC ER fino al limite.
     */
    @Test
    void PG_Digital_FullFlow_InipecErRetriesThenEndsInErrorAtConfiguredLimit() {
        String correlationId = "pg-inipec-er-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, PG_ER);

        String firstPollingId = "polling-first-" + UUID.randomUUID();
        String firstBatchId = submitInipecBatch(correlationId, firstPollingId);
        Pec er = pecResponse(PG_ER, "ignored@pec.it");
        er.setStatoImpresa(Pec.StatoImpresaEnum.ER);

        when(infoCamereClient.callEServiceRequestPec(firstPollingId)).thenReturn(Mono.just(inipecPollingResponse(er)));
        pollInipec(firstBatchId, firstPollingId, 1);

        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("NOT_WORKED", request.get("status").s());
        assertEquals("1", request.get("retryCount").n());
        assertTrue(requestsByRegistry(correlationId, "INAD").isEmpty());

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
        String batchId = submitInipecBatch(correlationId, pollingId);
        PnNationalRegistriesException inProgress = new PnNationalRegistriesException("List PEC in progress", 404, "Not Found", null, null, StandardCharsets.UTF_8, null);

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(inProgress));

        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> batch = findBatch(batchId);

        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("1", batch.get("inProgressRetryCount").n());
        assertEquals("WORKING", requestByRegistry(correlationId, "INIPEC").get("status").s());
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
        String batchId = submitInipecBatch(correlationId, pollingId);
        IniPecPollingResponse pollingResponse = new IniPecPollingResponse();
        pollingResponse.setCode("123");
        pollingResponse.setDescription("temporary error");

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));

        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> batch = findBatch(batchId);

        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("1", batch.get("errorRetryCount").n());
        assertEquals("WORKING", requestByRegistry(correlationId, "INIPEC").get("status").s());
        assertTrue(requestsByRegistry(correlationId, "INAD").isEmpty());
    }

    /**
     * IPA NotFound -> IniPEC con più PEC professionali valide.
     */
    @Test
    void PG_Digital_FullFlow_Inipec_AllProfessionalPecsArePreserved() {
        String correlationId = "pg-inipec-professionals-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, PG_PROFESSIONAL);

        GatewayResponse payload = completeInipecFinal(
                correlationId,
                PG_PROFESSIONAL,
                pecResponseWithProfessionals(PG_PROFESSIONAL, null, "professional-one@pec.it", "professional-two@pec.it")
        );

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

        GatewayResponse payload = completeInipecFinal(
                correlationId,
                PG_BUSINESS_PROFESSIONAL,
                pecResponseWithProfessionals(PG_BUSINESS_PROFESSIONAL, "not-an-email", "professional@pec.it")
        );

        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        verifyNoInteractions(inadClient);
    }

    /**
     * Fallback PF attivo: il flusso PG non cambia.
     */
    @Test
    void PG_Digital_FullFlow_IsUnchangedWhenPfFallbackIsEnabled() {
        String correlationId = "pg-pf-fallback-enabled-" + UUID.randomUUID();
        startInipecFromIpaNotFound(correlationId, VALID_PG_PIVA);

        Pec nf = pecResponse(VALID_PG_PIVA, "ignored@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);

        when(inadClient.callEService(eq(VALID_PG_PIVA), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PG_PIVA, "pg-pf-fallback-enabled@pec.it", null)));

        completeInipecPolling(correlationId, inipecPollingResponse(nf));

        assertInadFinal(correlationId, "pg-pf-fallback-enabled@pec.it");
    }

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
        String batchId = submitInipecBatch(correlationId, pollingId);

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(pecs)));

        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");

        assertEquals(taxId, request.get("taxId").s());
        assertEquals("WORKED", request.get("status").s());
        assertEquals("INIPEC#WORKED", request.get("registryStatus").s());
        assertNotNull(request.get("resultPayload"));

        return readNewOutputMessage(request.get("resultPayload").s());
    }

    private void completeInipecPolling(String correlationId, IniPecPollingResponse response) {
        String pollingId = "inipec-full-flow-" + UUID.randomUUID();
        String batchId = submitInipecBatch(correlationId, pollingId);

        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(response));

        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> inipecRequest = requestByRegistry(correlationId, "INIPEC");

        assertEquals("WORKED", inipecRequest.get("status").s());
        assertEquals("INIPEC#WORKED", inipecRequest.get("registryStatus").s());
        assertNull(inipecRequest.get("resultPayload"));
        assertEquals("WORKED", findBatch(batchId).get("status").s());
    }

    private String submitInipecBatch(String correlationId, String pollingId) {
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));

        iniPecBatchRequestJob.batchRequestScheduled();

        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");

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
        Map<String, AttributeValue> storedRequest = requestByRegistry(correlationId, "INIPEC");

        assertEquals(taxId, storedRequest.get("taxId").s());
        assertEquals("PG", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    private void assertInadFinal(String correlationId, String expectedAddress) {
        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INAD");

        assertEquals("WORKED", request.get("status").s());
        assertEquals("INAD#WORKED", request.get("registryStatus").s());
        assertNotNull(request.get("resultPayload"));
        assertNull(request.get("batchId"));

        GatewayResponse payload = readNewOutputMessage(request.get("resultPayload").s());

        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals(expectedAddress, payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    private void pollInipec(String batchId, String pollingId, int batchSize) {
        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, batchId, pollingId, batchSize)).block();
    }

    private PnPollingEvent.Payload pollingPayload(GatewayDownstreamService registry, String batchId, String pollingId, int batchSize) {
        return PnPollingEvent.Payload.builder()
                .registry(registry)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(batchSize)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build();
    }

    private List<Map<String, AttributeValue>> requestsByRegistry(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
                .toList();
    }

    private Map<String, AttributeValue> requestByRegistry(String correlationId, String registry) {
        return requestsByRegistry(correlationId, registry).stream()
                .findFirst()
                .orElseThrow();
    }

    private Map<String, AttributeValue> findBatch(String batchId) {
        return dynamoDbClient.getItem(builder -> builder
                        .tableName(NATIONAL_REGISTRIES_BATCHES_TABLE)
                        .key(Map.of(
                                "batchId", AttributeValue.builder().s(batchId).build(),
                                "sk", AttributeValue.builder().s("-").build()
                        )))
                .item();
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
}
