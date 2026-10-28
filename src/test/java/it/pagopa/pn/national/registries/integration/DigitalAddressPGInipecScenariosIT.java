package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
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

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.enable-old-inipec-flow=false",
        "pn.national-registries.inipec.max-batch-request-size=20",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.request-retry-after=0",
        "spring.task.scheduling.enabled=false"
})
class DigitalAddressPGInipecScenariosIT extends AbstractNationalRegistriesComponentIT {

    private static final String PG_INVALID = "56815563368";
    private static final String PG_EMPTY = "28223913097";
    private static final String PG_ND = "39274018561";
    private static final String PG_NF = "83016259471";
    private static final String PG_MISSING = "12825810299";
    private static final String PG_EXPIRED = "00534280912";
    private static final String PG_VALID_EXPIRED = "03649090796";
    private static final String PG_ER = "17492068394";

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private InipecPollingService inipecPollingService;

    @Test
    void PG_Digital_FullFlow_InipecNotFoundVariantsReachInadSyncAndApplyPgSelectionRules() {
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("pg-invalid-" + UUID.randomUUID(), PG_INVALID);
        cases.put("pg-empty-" + UUID.randomUUID(), PG_EMPTY);
        cases.put("pg-nd-" + UUID.randomUUID(), PG_ND);
        cases.put("pg-nf-" + UUID.randomUUID(), PG_NF);
        cases.put("pg-missing-" + UUID.randomUUID(), PG_MISSING);
        cases.put("pg-expired-" + UUID.randomUUID(), PG_EXPIRED);
        cases.put("pg-valid-expired-" + UUID.randomUUID(), PG_VALID_EXPIRED);
        String pollingId = "pg-inipec-polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, cases);

        Pec nd = pecResponse(PG_ND, "ignored@pec.it");
        nd.setStatoImpresa(Pec.StatoImpresaEnum.ND);
        Pec nf = pecResponse(PG_NF, "ignored@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        Pec expiredFallback = pecResponse(PG_EXPIRED, "ignored@pec.it");
        expiredFallback.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        Pec validExpiredFallback = pecResponse(PG_VALID_EXPIRED, "ignored@pec.it");
        validExpiredFallback.setStatoImpresa(Pec.StatoImpresaEnum.ND);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponse(PG_INVALID, "not-an-email"),
                pecResponse(PG_EMPTY, ""),
                nd,
                nf,
                expiredFallback,
                validExpiredFallback
        )));

        when(inadClient.callEService(eq(PG_INVALID), any())).thenReturn(Mono.just(inadResponse(PG_INVALID, "pg-invalid-fallback@pec.it", null)));
        when(inadClient.callEService(eq(PG_EMPTY), any())).thenReturn(Mono.just(inadResponse(PG_EMPTY, "pg-empty-fallback@pec.it", null)));
        when(inadClient.callEService(eq(PG_ND), any())).thenReturn(Mono.just(inadResponse(PG_ND, List.of(
                inadAddress("first@pec.it", null), inadAddress("second@pec.it", null)
        ))));
        when(inadClient.callEService(eq(PG_NF), any())).thenReturn(Mono.just(inadResponse(PG_NF, List.of(
                inadAddress("professional@pec.it", "INGEGNERE")
        ))));
        when(inadClient.callEService(eq(PG_MISSING), any())).thenReturn(Mono.just(inadResponse(PG_MISSING, List.of(
                inadAddress("not-an-email", null)
        ))));
        ElementDigitalAddress expired = inadAddress("expired@pec.it", null);
        expired.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        when(inadClient.callEService(eq(PG_EXPIRED), any())).thenReturn(Mono.just(inadResponse(PG_EXPIRED, List.of(expired))));
        ElementDigitalAddress anotherExpired = inadAddress("expired-second@pec.it", null);
        anotherExpired.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        when(inadClient.callEService(eq(PG_VALID_EXPIRED), any())).thenReturn(Mono.just(inadResponse(PG_VALID_EXPIRED, List.of(
                inadAddress("valid@pec.it", null), anotherExpired
        ))));

        pollInipec(batchId, pollingId, cases.size());

        assertInadFinal(correlationFor(cases, PG_INVALID), "pg-invalid-fallback@pec.it");
        assertInadFinal(correlationFor(cases, PG_EMPTY), "pg-empty-fallback@pec.it");
        assertInadEmpty(correlationFor(cases, PG_ND));
        assertInadEmpty(correlationFor(cases, PG_NF));
        assertInadEmpty(correlationFor(cases, PG_MISSING));
        assertInadEmpty(correlationFor(cases, PG_EXPIRED));
        assertInadFinal(correlationFor(cases, PG_VALID_EXPIRED), "valid@pec.it");
    }

    @Test
    void PG_Digital_FullFlow_InipecErSchedulesRequestRetryWithoutCreatingInad() {
        String correlationId = "pg-er-" + UUID.randomUUID();
        String pollingId = "pg-er-polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, Map.of(correlationId, PG_ER));
        Pec er = pecResponse(PG_ER, "ignored@pec.it");
        er.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(er)));

        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("NOT_WORKED", request.get("status").s());
        assertEquals("1", request.get("retryCount").n());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).stream().noneMatch(item -> "INAD".equals(item.get("registry").s())));
    }

    private String startInipecBatch(String pollingId, Map<String, String> cases) {
        for (Map.Entry<String, String> entry : cases.entrySet()) {
            when(ipaClient.callEServiceWS23(eq(entry.getValue()), any())).thenReturn(Mono.just(ipaNotFoundResponse()));
            enqueueGatewayRequest("PG", entry.getValue(), AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, entry.getKey());
            processNextGatewayInputMessage();
            assertEquals("NOT_WORKED", requestByRegistry(entry.getKey(), "INIPEC").get("status").s());
        }
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        String batchId = null;
        for (String correlationId : cases.keySet()) {
            Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
            assertEquals("WORKING", request.get("status").s());
            if (batchId == null) batchId = request.get("batchId").s();
            else assertEquals(batchId, request.get("batchId").s());
        }
        assertNotNull(batchId);
        pollingJob.polling();
        return batchId;
    }

    private void pollInipec(String batchId, String pollingId, int batchSize) {
        inipecPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INIPEC)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(batchSize)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();
    }

    private String correlationFor(Map<String, String> cases, String taxId) {
        return cases.entrySet().stream().filter(entry -> taxId.equals(entry.getValue())).map(Map.Entry::getKey).findFirst().orElseThrow();
    }

    private void assertInadFinal(String correlationId, String expectedAddress) {
        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", request.get("status").s());
        CodeSqsDto payload = readOutputMessage(request.get("resultPayload").s());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(expectedAddress, payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getDigitalAddress().getFirst().getRecipient());
    }

    private void assertInadEmpty(String correlationId) {
        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INAD");
        assertEquals("WORKED", request.get("status").s());
        CodeSqsDto payload = readOutputMessage(request.get("resultPayload").s());
        assertEquals("INAD", payload.getRegistry());
        assertNotNull(payload.getDigitalAddress());
        assertTrue(payload.getDigitalAddress().isEmpty());
    }

    private Map<String, AttributeValue> requestByRegistry(String correlationId, String registry) {
        return findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(request -> registry.equals(request.get("registry").s()))
                .findFirst()
                .orElseThrow();
    }

    private IniPecPollingResponse inipecPollingResponse(Pec... pecs) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pecs));
        return response;
    }
}
