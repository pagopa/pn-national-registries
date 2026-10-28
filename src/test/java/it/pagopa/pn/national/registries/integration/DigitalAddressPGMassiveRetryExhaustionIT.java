package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.RequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseStatusListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.StatusProcessingRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.PollingInputsHandler;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-inad-massive-api=true",
        "pn.national-registries.inipec.max-batch-request-size=1",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inad.max-batch-request-size=1",
        "pn.national-registries.inad.oldest-request-max-waiting-seconds=0",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inad.batch-polling-max-retry=1",
        "pn.national-registries.inad.batch-polling-in-progress-max-retry=1",
        "pn.national-registries.inad.polling-error-retry-after=0",
        "pn.national-registries.inad.polling-in-progress-retry-after=0",
        "spring.task.scheduling.enabled=false"
})
class DigitalAddressPGMassiveRetryExhaustionIT extends AbstractNationalRegistriesComponentIT {

    private static final String IPA_AUTH_ID = "secure_Password123";

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private PollingInputsHandler pollingInputsHandler;

    @Test
    void PG_Digital_FullFlow_InadMassive_ErrorRetryExhaustionMarksRequestAndBatchError() {
        Flow flow = createInadPollingFlow("pg-massive-error-exhausted-");
        ResponseStatusListDigitalAddress status = new ResponseStatusListDigitalAddress();
        status.setState(StatusProcessingRequest.DISPONIBILE);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status));
        when(inadClient.getMassiveRequestResult(flow.pollingId())).thenReturn(Mono.error(new IllegalStateException("INAD unavailable")));

        consume(payload(flow));

        assertEquals("ERROR", findBatch(flow.batchId()).get("status").s());
        assertEquals("ERROR", findRequest(flow.correlationId(), "INAD").get("status").s());
        assertEquals("INAD#ERROR", findRequest(flow.correlationId(), "INAD").get("registryStatus").s());
    }

    @Test
    void PG_Digital_FullFlow_InadMassive_InProgressRetryExhaustionMarksRequestAndBatchError() {
        Flow flow = createInadPollingFlow("pg-massive-in-progress-exhausted-");
        ResponseStatusListDigitalAddress status = new ResponseStatusListDigitalAddress();
        status.setState(StatusProcessingRequest.IN_ELABORAZIONE);
        when(inadClient.checkMassiveRequestStatus(flow.pollingId())).thenReturn(Mono.just(status));

        consume(payload(flow));

        assertEquals("ERROR", findBatch(flow.batchId()).get("status").s());
        assertEquals("ERROR", findRequest(flow.correlationId(), "INAD").get("status").s());
        assertEquals("INAD#ERROR", findRequest(flow.correlationId(), "INAD").get("registryStatus").s());
    }

    private Flow createInadPollingFlow(String prefix) {
        String correlationId = prefix + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(VALID_PG_PIVA, IPA_AUTH_ID)).thenReturn(Mono.just(ipaNotFoundResponse()));
        enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        String inipecPollingId = "inipec-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(inipecPollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        Map<String, AttributeValue> inipecRequest = findRequest(correlationId, "INIPEC");
        String inipecBatchId = inipecRequest.get("batchId").s();
        pollingJob.polling();
        Pec pec = pecResponse(VALID_PG_PIVA, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        IniPecPollingResponse inipecResponse = new IniPecPollingResponse();
        inipecResponse.setElencoPec(List.of(pec));
        when(infoCamereClient.callEServiceRequestPec(inipecPollingId)).thenReturn(Mono.just(inipecResponse));
        consume(PnPollingEvent.Payload.builder().registry(GatewayDownstreamService.INIPEC).batchId(inipecBatchId).pollingId(inipecPollingId).batchSize(1).errorRetryCount(0).inProgressRetryCount(0).build());

        String inadPollingId = "inad-polling-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();
        batchResponse.setId(inadPollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(batchResponse));
        inadBatchRequestJob.batchRequest();
        Map<String, AttributeValue> inadRequest = findRequest(correlationId, "INAD");
        String inadBatchId = inadRequest.get("batchId").s();
        pollingJob.polling();
        assertEquals("WORKING", findBatch(inadBatchId).get("status").s());
        return new Flow(correlationId, inadBatchId, inadPollingId);
    }

    private void consume(PnPollingEvent.Payload payload) {
        pollingInputsHandler.pnNationalRegistriesPollingInputsConsumer(MessageBuilder.withPayload(payload).build());
    }

    private PnPollingEvent.Payload payload(Flow flow) {
        return PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INAD)
                .batchId(flow.batchId())
                .pollingId(flow.pollingId())
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build();
    }

    private Map<String, AttributeValue> findRequest(String correlationId, String registry) {
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

    private record Flow(String correlationId, String batchId, String pollingId) {
    }
}
