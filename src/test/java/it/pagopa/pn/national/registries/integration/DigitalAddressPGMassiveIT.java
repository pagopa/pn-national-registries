package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.PollingInputsHandler;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.dao.batchesTableName=pn-NationalRegistriesBatches",
        "pn.national-registries.inipec.batch-request-pk-separator=~",
        "pn.national-registries.enable-inad-massive-api=true",
        "pn.national-registries.inipec.max-batch-request-size=4",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inad.max-batch-request-size=4",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0"
})
public class DigitalAddressPGMassiveIT extends AbstractNationalRegistriesComponentIT {

    private static final String IPA_AUTH_ID = "secure_Password123";
    private static final String PG_INAD_FOUND = "75120855137";
    private static final String PG_INAD_NOT_FOUND = "33068456921";
    private static final String PG_INAD_EXPIRED = "49603127470";
    private static final String PG_IPA_INVALID = "32383524660";

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private PollingInputsHandler pollingInputsHandler;

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Test
    void PG_Digital_FullFlow_InipecFallback_InadMassive() {
        String foundCorrelationId = "pg-massive-found-" + UUID.randomUUID();
        String notFoundCorrelationId = "pg-massive-not-found-" + UUID.randomUUID();
        String expiredCorrelationId = "pg-massive-expired-" + UUID.randomUUID();
        String invalidIpaCorrelationId = "pg-massive-invalid-ipa-" + UUID.randomUUID();

        List<TestRequest> requests = List.of(
                new TestRequest(PG_INAD_FOUND, foundCorrelationId),
                new TestRequest(PG_INAD_NOT_FOUND, notFoundCorrelationId),
                new TestRequest(PG_INAD_EXPIRED, expiredCorrelationId),
                new TestRequest(PG_IPA_INVALID, invalidIpaCorrelationId)
        );

        when(ipaClient.callEServiceWS23(PG_INAD_FOUND, IPA_AUTH_ID)).thenReturn(Mono.just(ipaNotFoundResponse()));
        when(ipaClient.callEServiceWS23(PG_INAD_NOT_FOUND, IPA_AUTH_ID)).thenReturn(Mono.just(ipaNotFoundResponse()));
        when(ipaClient.callEServiceWS23(PG_INAD_EXPIRED, IPA_AUTH_ID)).thenReturn(Mono.just(ipaNotFoundResponse()));
        when(ipaClient.callEServiceWS23(PG_IPA_INVALID, IPA_AUTH_ID)).thenReturn(Mono.just(ipaFoundResponse(PG_IPA_INVALID, "invalid-ipa-pec")));

        for (TestRequest request : requests) {
            AddressOKDto response = enqueueGatewayRequest("PG", request.taxId(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, request.correlationId());
            processNextGatewayInputMessage();

            assertEquals(request.correlationId(), response.getCorrelationId());
            assertInipecPendingRequest(request);
        }

        String inipecPollingId = "inipec-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(inipecPollingId)));

        iniPecBatchRequestJob.batchRequestScheduled();

        ArgumentCaptor<IniPecBatchRequest> inipecRequestCaptor = ArgumentCaptor.forClass(IniPecBatchRequest.class);
        verify(infoCamereClient).callEServiceRequestId(inipecRequestCaptor.capture());
        assertEquals(Set.of(PG_INAD_FOUND, PG_INAD_NOT_FOUND, PG_INAD_EXPIRED, PG_IPA_INVALID), new HashSet<>(inipecRequestCaptor.getValue().getElencoCf().stream().map(IniPecBatchRequest.IniPecCf::getCf).toList()));

        String inipecBatchId = assertSameWorkingBatch(requests, "INIPEC");
        assertEquals("REQUEST_SENT", findBatch(inipecBatchId).get("status").s());

        pollingJob.polling();
        assertEquals("WORKING", findBatch(inipecBatchId).get("status").s());

        when(infoCamereClient.callEServiceRequestPec(inipecPollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponseWithState(PG_INAD_FOUND, "ignored-found@pec.it", Pec.StatoImpresaEnum.ND),
                pecResponseWithState(PG_INAD_NOT_FOUND, "ignored-not-found@pec.it", Pec.StatoImpresaEnum.NF),
                pecResponseWithState(PG_INAD_EXPIRED, "ignored-expired@pec.it", Pec.StatoImpresaEnum.ND),
                pecResponse(PG_IPA_INVALID, "invalid-inipec-pec")
        )));

        processPollingEvent(pollingPayload(GatewayDownstreamService.INIPEC, inipecBatchId, inipecPollingId, 4));
        assertEquals("WORKED", findBatch(inipecBatchId).get("status").s());

        for (TestRequest request : requests) {
            Map<String, AttributeValue> inipecRequest = findRequestByRegistry(request.correlationId(), "INIPEC");
            assertEquals("WORKED", inipecRequest.get("status").s());
            assertEquals("INIPEC#WORKED", inipecRequest.get("registryStatus").s());
            assertNull(inipecRequest.get("resultPayload"));

            Map<String, AttributeValue> inadRequest = findRequestByRegistry(request.correlationId(), "INAD");
            assertEquals("NOT_WORKED", inadRequest.get("status").s());
            assertEquals("INAD#NOT_WORKED", inadRequest.get("registryStatus").s());
            assertNull(inadRequest.get("batchId"));
        }

        String inadPollingId = "inad-polling-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress inadBatchResponse = new ResponseRequestListDigitalAddress();
        inadBatchResponse.setId(inadPollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(inadBatchResponse));

        inadBatchRequestJob.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> inadRequestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(inadRequestCaptor.capture());
        assertEquals(Set.of(PG_INAD_FOUND, PG_INAD_NOT_FOUND, PG_INAD_EXPIRED, PG_IPA_INVALID), new HashSet<>(inadRequestCaptor.getValue().getCodiciFiscali()));

        String inadBatchId = assertSameWorkingBatch(requests, "INAD");
        assertEquals("REQUEST_SENT", findBatch(inadBatchId).get("status").s());

        pollingJob.polling();
        assertEquals("WORKING", findBatch(inadBatchId).get("status").s());

        ResponseStatusListDigitalAddress statusResponse = new ResponseStatusListDigitalAddress();
        statusResponse.setState(StatusProcessingRequest.DISPONIBILE);
        when(inadClient.checkMassiveRequestStatus(inadPollingId)).thenReturn(Mono.just(statusResponse));

        ResponseRequestDigitalAddress foundResponse = inadResponse(PG_INAD_FOUND, "inad-found@pec.it", null);
        ResponseRequestDigitalAddress invalidIpaResponse = inadResponse(PG_IPA_INVALID, "inad-after-invalid@pec.it", null);

        ElementDigitalAddress expiredAddress = inadAddress("expired@pec.it", null);
        expiredAddress.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        ResponseRequestDigitalAddress expiredResponse = inadResponse(PG_INAD_EXPIRED, List.of(expiredAddress));

        ResponseListDigitalAddress massiveResponse = new ResponseListDigitalAddress();
        massiveResponse.setList(List.of(foundResponse, expiredResponse, invalidIpaResponse));
        when(inadClient.getMassiveRequestResult(inadPollingId)).thenReturn(Mono.just(massiveResponse));

        processPollingEvent(pollingPayload(GatewayDownstreamService.INAD, inadBatchId, inadPollingId, 4));
        assertEquals("WORKED", findBatch(inadBatchId).get("status").s());

        assertInadFinalResult(foundCorrelationId, "inad-found@pec.it");
        assertInadEmptyResult(notFoundCorrelationId);
        assertInadEmptyResult(expiredCorrelationId);
        assertInadFinalResult(invalidIpaCorrelationId, "inad-after-invalid@pec.it");

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(infoCamereClient).callEServiceRequestPec(inipecPollingId);
        verify(inadClient).checkMassiveRequestStatus(inadPollingId);
        verify(inadClient).getMassiveRequestResult(inadPollingId);
        verify(inadClient, never()).callEService(any(), any());
    }

    private void assertInipecPendingRequest(TestRequest request) {
        Map<String, AttributeValue> storedRequest = findRequestByRegistry(request.correlationId(), "INIPEC");
        assertEquals(request.taxId(), storedRequest.get("taxId").s());
        assertEquals("PG", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    private String assertSameWorkingBatch(List<TestRequest> requests, String registry) {
        String batchId = null;
        for (TestRequest request : requests) {
            Map<String, AttributeValue> storedRequest = findRequestByRegistry(request.correlationId(), registry);
            assertEquals("WORKING", storedRequest.get("status").s());
            assertEquals(registry + "#WORKING", storedRequest.get("registryStatus").s());
            assertNotNull(storedRequest.get("batchId"));
            if (batchId == null) {
                batchId = storedRequest.get("batchId").s();
            } else {
                assertEquals(batchId, storedRequest.get("batchId").s());
            }
        }
        assertNotNull(batchId);
        return batchId;
    }

    private void assertInadFinalResult(String correlationId, String expectedAddress) {
        Map<String, AttributeValue> inadRequest = findRequestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertEquals("INAD#WORKED", inadRequest.get("registryStatus").s());
        assertNotNull(inadRequest.get("resultPayload"));

        CodeSqsDto payload = readOutputMessage(inadRequest.get("resultPayload").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals(expectedAddress, payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    private void assertInadEmptyResult(String correlationId) {
        Map<String, AttributeValue> inadRequest = findRequestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertEquals("INAD#WORKED", inadRequest.get("registryStatus").s());
        assertNotNull(inadRequest.get("resultPayload"));

        CodeSqsDto payload = readOutputMessage(inadRequest.get("resultPayload").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
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

    private void processPollingEvent(PnPollingEvent.Payload payload) {
        pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(MessageBuilder.withPayload(payload).build());
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

    private record TestRequest(String taxId, String correlationId) {
    }
}
