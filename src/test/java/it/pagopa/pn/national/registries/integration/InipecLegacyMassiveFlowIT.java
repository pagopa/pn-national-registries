package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.RequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseStatusListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.StatusProcessingRequest;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.service.InadPollingService;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-old-inipec-flow=true",
        "pn.national-registries.enable-inad-massive-api=true",
        "pn.national-registries.inad.max-batch-request-size=1",
        "pn.national-registries.inad.oldest-request-max-waiting-seconds=0",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "spring.task.scheduling.enabled=false"
})
class InipecLegacyMassiveFlowIT extends AbstractNationalRegistriesComponentIT {

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private InadPollingService inadPollingService;

    @Test
    void LegacyInipec_PgNotFoundCreatesMassiveInadRequestAndClosesLegacyRequest() {
        String correlationId = "legacy-pg-massive-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PG_PIVA);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();

        Pec pec = pecResponse(VALID_PG_PIVA, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pec));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(response));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> legacy = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", legacy.get("status").s());
        assertEquals("INAD", legacy.get("eservice").s());
        assertEquals("NOT_TO_SEND", legacy.get("sendStatus").s());
        assertNull(legacy.get("message"));

        List<Map<String, AttributeValue>> requests = findNationalRegistriesRequestsByCorrelationId(correlationId);
        assertEquals(1, requests.size());
        assertEquals("INAD", requests.getFirst().get("registry").s());
        assertEquals("NOT_WORKED", requests.getFirst().get("status").s());
        assertEquals("INAD#NOT_WORKED", requests.getFirst().get("registryStatus").s());
        assertEquals(VALID_PG_PIVA, requests.getFirst().get("taxId").s());
        assertEquals("PG", requests.getFirst().get("recipientType").s());
        verifyNoInteractions(inadClient);
    }

    @Test
    void LegacyInipec_PgNotFoundCompletesThroughMassiveInadResult() {
        String correlationId = "legacy-pg-massive-full-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PG_PIVA);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();

        Pec pec = pecResponse(VALID_PG_PIVA, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pec));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(response));
        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> inadRequest = findNationalRegistriesRequestsByCorrelationId(correlationId).getFirst();
        Assertions.assertEquals("NOT_WORKED", inadRequest.get("status").s());

        String inadPollingId = "inad-polling-" + UUID.randomUUID();
        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();
        batchResponse.setId(inadPollingId);
        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(batchResponse));
        inadBatchRequestJob.batchRequest();

        inadRequest = findNationalRegistriesRequestsByCorrelationId(correlationId).getFirst();
        String inadBatchId = inadRequest.get("batchId").s();
        pollingJob.polling();

        ResponseStatusListDigitalAddress status = new ResponseStatusListDigitalAddress();
        status.setState(StatusProcessingRequest.DISPONIBILE);
        ResponseListDigitalAddress result = new ResponseListDigitalAddress();
        result.setList(List.of(inadResponse(VALID_PG_PIVA, "legacy-massive-inad@pec.it", null)));
        when(inadClient.checkMassiveRequestStatus(inadPollingId)).thenReturn(Mono.just(status));
        when(inadClient.getMassiveRequestResult(inadPollingId)).thenReturn(Mono.just(result));

        inadPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INAD)
                .batchId(inadBatchId)
                .pollingId(inadPollingId)
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();

        inadRequest = findNationalRegistriesRequestsByCorrelationId(correlationId).getFirst();
        Assertions.assertEquals("WORKED", inadRequest.get("status").s());
        Assertions.assertNotNull(inadRequest.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(inadRequest.get("resultPayload").s());
        Assertions.assertEquals("INAD", payload.getRegistry());
        Assertions.assertEquals("legacy-massive-inad@pec.it", payload.getDigitalAddress().getFirst().getAddress());
    }
}