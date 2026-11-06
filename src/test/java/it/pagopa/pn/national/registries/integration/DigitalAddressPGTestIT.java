package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS05Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS23Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.ResultDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS05ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS23ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.model.ipa.IpaSecret;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.dao.batchesTableName=pn-NationalRegistriesBatches",
        "pn.national-registries.inipec.batch-request-pk-separator=~",
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.batch-polling-max-retry=3",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=24",
        "pn.national-registries.inipec.polling-error-retry-after=1",
        "pn.national-registries.inipec.polling-in-progress-retry-after=1"
})
public class DigitalAddressPGTestIT extends AbstractNationalRegistriesComponentIT {

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private InipecPollingService inipecPollingService;

    @Autowired
    private PollingJob pollingJob;

    @Test
    void PG_Digital_IPA() {
        String correlationId = "pg-digital-ipa-found-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA),any())).thenReturn(Mono.just(ipaFoundResponse(VALID_PG_PIVA, "pg@pec.it")));

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
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA),any());
        verifyNoInteractions(infoCamereClient, inadClient);
    }

    @Test
    void PG_Digital_IPA_MultipleResults_UsesWS05() {
        String correlationId = "pg-digital-ipa-multiple-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA),any())).thenReturn(Mono.just(ipaMultipleResponse()));
        when(ipaClient.callEServiceWS05(eq("cod-amm-1"),any())).thenReturn(Mono.just(ipaWs05Response("ws05@pec.it")));

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
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA),any());
        verify(ipaClient).callEServiceWS05(eq("cod-amm-1"),any());
        verifyNoInteractions(infoCamereClient, inadClient);
    }

    @Test
    void PG_Digital_IPA_NotFound_InipecFound() {
        String correlationId = "pg-digital-ipa-not-found-inipec-found-" + UUID.randomUUID();
        String pollingId = "inipec-polling-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA),any())).thenReturn(Mono.just(ipaNotFoundResponse()));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertInipecPendingRequest(correlationId, VALID_PG_PIVA);

        String batchId = submitInipecBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponse(VALID_PG_PIVA, "inipec@pec.it")
        )));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, batchId, pollingId, 1)).block();

        Map<String, AttributeValue> inipecRequest = findRequestByRegistry(correlationId, "INIPEC");
        assertEquals("WORKED", inipecRequest.get("status").s());
        assertEquals("INIPEC#WORKED", inipecRequest.get("registryStatus").s());
        assertEquals(batchId, inipecRequest.get("batchId").s());
        assertNotNull(inipecRequest.get("resultPayload"));

        CodeSqsDto payload = readOutputMessage(inipecRequest.get("resultPayload").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("inipec@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());

        assertEquals("WORKED", findBatch(batchId).get("status").s());
        assertEquals(1, findNationalRegistriesRequestsByCorrelationId(correlationId).size());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA),any());
        verify(infoCamereClient).callEServiceRequestId(any(IniPecBatchRequest.class));
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
        verifyNoInteractions(inadClient);
    }

    @Test
    void PG_Digital_IPA_InvalidPec_InipecFound() {
        String correlationId = "pg-digital-ipa-invalid-inipec-found-" + UUID.randomUUID();
        String pollingId = "inipec-polling-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA),any())).thenReturn(Mono.just(ipaFoundResponse(VALID_PG_PIVA, "invalid-pec")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertInipecPendingRequest(correlationId, VALID_PG_PIVA);

        String batchId = submitInipecBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponse(VALID_PG_PIVA, "inipec-after-invalid-ipa@pec.it")
        )));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, batchId, pollingId, 1)).block();

        Map<String, AttributeValue> inipecRequest = findRequestByRegistry(correlationId, "INIPEC");
        assertEquals("WORKED", inipecRequest.get("status").s());
        assertNotNull(inipecRequest.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(inipecRequest.get("resultPayload").s());
        assertEquals("inipec-after-invalid-ipa@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());

        assertEquals("WORKED", findBatch(batchId).get("status").s());
        assertEquals(1, findNationalRegistriesRequestsByCorrelationId(correlationId).size());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA),any());
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
        verifyNoInteractions(inadClient);
    }

    @Test
    void PG_Digital_IPA_NotFound_InipecNotFound_InadSync() {
        String correlationId = "pg-digital-full-sync-" + UUID.randomUUID();
        String pollingId = "inipec-polling-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA),any())).thenReturn(Mono.just(ipaNotFoundResponse()));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertInipecPendingRequest(correlationId, VALID_PG_PIVA);

        String batchId = submitInipecBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponseWithState(VALID_PG_PIVA, "ignored@pec.it", Pec.StatoImpresaEnum.NF)
        )));
        when(inadClient.callEService(eq(VALID_PG_PIVA), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PG_PIVA, "inad-sync@pec.it", null)));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, batchId, pollingId, 1)).block();

        Map<String, AttributeValue> inipecRequest = findRequestByRegistry(correlationId, "INIPEC");
        assertEquals("WORKED", inipecRequest.get("status").s());
        assertEquals("INIPEC#WORKED", inipecRequest.get("registryStatus").s());
        assertNull(inipecRequest.get("resultPayload"));

        Map<String, AttributeValue> inadRequest = findRequestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertEquals("INAD#WORKED", inadRequest.get("registryStatus").s());
        assertNotNull(inadRequest.get("resultPayload"));
        assertNull(inadRequest.get("batchId"));

        CodeSqsDto payload = readOutputMessage(inadRequest.get("resultPayload").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("inad-sync@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());

        assertEquals("WORKED", findBatch(batchId).get("status").s());
        assertEquals(2, findNationalRegistriesRequestsByCorrelationId(correlationId).size());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(ipaClient).callEServiceWS23(eq(VALID_PG_PIVA),any());
        verify(infoCamereClient).callEServiceRequestPec(pollingId);
        verify(inadClient).callEService(VALID_PG_PIVA, correlationId);
    }

    private String submitInipecBatch(String correlationId, String pollingId) {
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();

        Map<String, AttributeValue> request = findRequestByRegistry(correlationId, "INIPEC");
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
        Map<String, AttributeValue> storedRequest = findRequestByRegistry(correlationId, "INIPEC");
        assertEquals(taxId, storedRequest.get("taxId").s());
        assertEquals("PG", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    private Map<String, AttributeValue> findRequestByRegistry(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
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

    private IniPecPollingResponse inipecPollingResponse(Pec... pecs) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pecs));
        return response;
    }

    private Pec pecResponseWithState(String taxId, String pecAddress, Pec.StatoImpresaEnum statoImpresa) {
        Pec pec = pecResponse(taxId, pecAddress);
        pec.setStatoImpresa(statoImpresa);
        return pec;
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
