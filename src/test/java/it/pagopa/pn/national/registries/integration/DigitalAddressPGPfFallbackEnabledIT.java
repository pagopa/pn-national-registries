package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.enable-old-inipec-flow=false",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "spring.task.scheduling.enabled=false"
})
class DigitalAddressPGPfFallbackEnabledIT extends AbstractNationalRegistriesComponentIT {

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private InipecPollingService inipecPollingService;

    @Test
    void PG_Digital_FullFlow_IsUnchangedWhenPfFallbackIsEnabled() {
        String correlationId = "pg-pf-fallback-enabled-" + UUID.randomUUID();
        String pollingId = "inipec-polling-" + UUID.randomUUID();
        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any())).thenReturn(Mono.just(ipaNotFoundResponse()));

        enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        Map<String, AttributeValue> pending = requestByRegistry(correlationId, "INIPEC");
        assertEquals("NOT_WORKED", pending.get("status").s());

        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        Map<String, AttributeValue> inipecRequest = requestByRegistry(correlationId, "INIPEC");
        String batchId = inipecRequest.get("batchId").s();
        pollingJob.polling();

        Pec pec = pecResponse(VALID_PG_PIVA, "ignored@pec.it");
        pec.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        IniPecPollingResponse pollingResponse = new IniPecPollingResponse();
        pollingResponse.setElencoPec(List.of(pec));
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));
        when(inadClient.callEService(eq(VALID_PG_PIVA), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PG_PIVA, "pg-pf-fallback-enabled@pec.it", null)));

        inipecPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INIPEC)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();

        Map<String, AttributeValue> inadRequest = requestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", inadRequest.get("status").s());
        assertNotNull(inadRequest.get("resultPayload"));
        CodeSqsDto payload = readOutputMessage(inadRequest.get("resultPayload").s());
        assertEquals("INAD", payload.getRegistry());
        assertEquals("pg-pf-fallback-enabled@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    private Map<String, AttributeValue> requestByRegistry(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
    }
}
