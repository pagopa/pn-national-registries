package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.RequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseStatusListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.StatusProcessingRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.service.InadPollingService;
import it.pagopa.pn.national.registries.service.InipecPollingService;
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
        "pn.national-registries.enable-old-inipec-flow=false",
        "pn.national-registries.inad.max-batch-request-size=4",
        "pn.national-registries.inad.oldest-request-max-waiting-seconds=0",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.max-batch-request-size=4",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.batch-request-max-retry=3",
        "pn.national-registries.inipec.request-retry-after=60",
        "spring.task.scheduling.enabled=false"
})
class DigitalAddressPFMassiveFallbackIT extends AbstractNationalRegistriesComponentIT {

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

    @Test
    void PF_Digital_INAD_Massive_Fallback_InipecFound_FullFlow() {
        Flow flow = createInipecFallback("pf-massive-inipec-found-");
        when(infoCamereClient.callEServiceRequestPec(flow.inipecPollingId())).thenReturn(Mono.just(inipecResponse(pecResponse(VALID_PF_CF, "pf-final@pec.it"))));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, flow.inipecBatchId(), flow.inipecPollingId())).block();

        Map<String, AttributeValue> request = findRequest(flow.correlationId(), "INIPEC");
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(request.get("resultPayload").s());
        assertEquals("INIPEC", payload.getRegistry());
        assertEquals("pf-final@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    @Test
    void PF_Digital_INAD_Massive_Fallback_InipecNotFound_FullFlow() {
        Flow flow = createInipecFallback("pf-massive-inipec-not-found-");
        Pec pec = pecResponse(VALID_PF_CF, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        when(infoCamereClient.callEServiceRequestPec(flow.inipecPollingId())).thenReturn(Mono.just(inipecResponse(pec)));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, flow.inipecBatchId(), flow.inipecPollingId())).block();

        Map<String, AttributeValue> request = findRequest(flow.correlationId(), "INIPEC");
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(request.get("resultPayload").s());
        assertEquals("INIPEC", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
    }

    @Test
    void PF_Digital_INAD_Massive_Fallback_InipecErSchedulesRequestRetry_FullFlow() {
        Flow flow = createInipecFallback("pf-massive-inipec-er-");
        Pec pec = pecResponse(VALID_PF_CF, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        when(infoCamereClient.callEServiceRequestPec(flow.inipecPollingId())).thenReturn(Mono.just(inipecResponse(pec)));

        inipecPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INIPEC, flow.inipecBatchId(), flow.inipecPollingId())).block();

        Map<String, AttributeValue> request = findRequest(flow.correlationId(), "INIPEC");
        assertEquals("NOT_WORKED", request.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", request.get("registryStatus").s());
        assertEquals("1", request.get("retryCount").n());
        assertNotNull(request.get("attemptAfter"));
        assertNull(request.get("resultPayload"));
    }

    private Flow createInipecFallback(String prefix) {
        String correlationId = prefix + UUID.randomUUID();
        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();
        assertEquals("NOT_WORKED", findRequest(correlationId, "INAD").get("status").s());

        String inadPollingId = "inad-polling-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress inadBatchResponse = new ResponseRequestListDigitalAddress();
        inadBatchResponse.setId(inadPollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(inadBatchResponse));
        inadBatchRequestJob.batchRequest();
        Map<String, AttributeValue> inadRequest = findRequest(correlationId, "INAD");
        String inadBatchId = inadRequest.get("batchId").s();
        pollingJob.polling();

        ResponseStatusListDigitalAddress status = new ResponseStatusListDigitalAddress();
        status.setState(StatusProcessingRequest.DISPONIBILE);
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of());
        when(inadClient.checkMassiveRequestStatus(inadPollingId)).thenReturn(Mono.just(status));
        when(inadClient.getMassiveRequestResult(inadPollingId)).thenReturn(Mono.just(result));
        inadPollingService.handleMessage(pollingPayload(GatewayDownstreamService.INAD, inadBatchId, inadPollingId)).block();

        inadRequest = findRequest(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertNull(inadRequest.get("resultPayload"));
        assertEquals("NOT_WORKED", findRequest(correlationId, "INIPEC").get("status").s());

        String inipecPollingId = "inipec-polling-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(inipecPollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        Map<String, AttributeValue> inipecRequest = findRequest(correlationId, "INIPEC");
        String inipecBatchId = inipecRequest.get("batchId").s();
        pollingJob.polling();
        return new Flow(correlationId, inipecBatchId, inipecPollingId);
    }

    private Map<String, AttributeValue> findRequest(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
    }

    private PnPollingEvent.Payload pollingPayload(GatewayDownstreamService registry, String batchId, String pollingId) {
        return PnPollingEvent.Payload.builder()
                .registry(registry)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build();
    }

    private IniPecPollingResponse inipecResponse(Pec pec) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pec));
        return response;
    }

    private record Flow(String correlationId, String inipecBatchId, String inipecPollingId) {
    }
}
