package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.RequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseStatusListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.StatusProcessingRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.service.InadPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-old-inipec-flow=true",
        "pn.national-registries.inad.max-batch-request-size=1",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "spring.task.scheduling.enabled=false"
})
class DigitalAddressPFMassiveOldInipecIT extends AbstractNationalRegistriesComponentIT {

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private InadPollingService inadPollingService;

    @Test
    void PF_Digital_FullFlow_InadMassiveNotFoundFallsBackToLegacyInipecAndCompletes() {
        String correlationId = "pf-massive-old-inipec-" + UUID.randomUUID();
        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        Map<String, AttributeValue> inadRequest = requestByRegistry(correlationId, "INAD");
        assertEquals("NOT_WORKED", inadRequest.get("status").s());

        String inadPollingId = "inad-polling-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress submitResponse = new ResponseRequestListDigitalAddress();
        submitResponse.setId(inadPollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(submitResponse));
        inadBatchRequestJob.batchRequest();

        inadRequest = requestByRegistry(correlationId, "INAD");
        String inadBatchId = inadRequest.get("batchId").s();
        pollingJob.polling();
        ResponseStatusListDigitalAddress status = new ResponseStatusListDigitalAddress();
        status.setState(StatusProcessingRequest.DISPONIBILE);
        ResponseListDigitalAddress emptyResult = new ResponseListDigitalAddress();
        emptyResult.setList(List.of());
        when(inadClient.checkMassiveRequestStatus(inadPollingId)).thenReturn(Mono.just(status));
        when(inadClient.getMassiveRequestResult(inadPollingId)).thenReturn(Mono.just(emptyResult));

        inadPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INAD)
                .batchId(inadBatchId)
                .pollingId(inadPollingId)
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();

        inadRequest = requestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertNull(inadRequest.get("resultPayload"));
        List<Map<String, AttributeValue>> legacyRequests = findBatchRequestsByCorrelationPrefix(correlationId);
        assertEquals(1, legacyRequests.size());
        assertEquals("NOT_WORKED", legacyRequests.getFirst().get("status").s());

        String inipecPollingId = "legacy-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(inipecPollingId)));
        iniPecBatchRequestService.batchPecRequest();
        Map<String, AttributeValue> legacyRequest = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKING", legacyRequest.get("status").s());

        IniPecPollingResponse inipecResponse = new IniPecPollingResponse();
        inipecResponse.setElencoPec(List.of(pecResponse(VALID_PF_CF, "legacy-final@pec.it")));
        when(infoCamereClient.callEServiceRequestPec(inipecPollingId)).thenReturn(Mono.just(inipecResponse));
        digitalAddressBatchPollingService.batchPecPolling();

        legacyRequest = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", legacyRequest.get("status").s());
        assertNotNull(legacyRequest.get("message"));
        CodeSqsDto payload = readOutputMessage(legacyRequest.get("message").s());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("legacy-final@pec.it", payload.getDigitalAddress().getFirst().getAddress());
    }

    private Map<String, AttributeValue> requestByRegistry(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
    }
}
