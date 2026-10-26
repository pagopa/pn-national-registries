package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static it.pagopa.pn.national.registries.utils.GatewayUtils.formatInstant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.query-limit=2",
        "pn.national-registries.dao.batchesTableName=pn-NationalRegistriesBatches",
        "pn.national-registries.inipec.max-batch-request-size=3",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
})
class IniPecBatchRequestJobIT extends AbstractNationalRegistriesComponentIT {

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;

    @Test
    void sendsPendingInipecRequestsAndAssociatesThemWithBatch() {
        List<NationalRegistriesRequest> requests = persistRequests(3);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse("polling-id")));

        iniPecBatchRequestJob.batchRequestScheduled();

        ArgumentCaptor<IniPecBatchRequest> requestCaptor = ArgumentCaptor.forClass(IniPecBatchRequest.class);
        verify(infoCamereClient).callEServiceRequestId(requestCaptor.capture());
        assertEquals(requests.stream().map(NationalRegistriesRequest::getTaxId).toList(), requestCaptor.getValue().getElencoCf().stream().map(IniPecBatchRequest.IniPecCf::getCf).toList());

        List<Map<String, AttributeValue>> batches = findBatches();
        assertEquals(1, batches.size());
        Map<String, AttributeValue> batch = batches.getFirst();
        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("INIPEC", batch.get("registry").s());
        assertEquals("polling-id", batch.get("pollingId").s());
        assertEquals("3", batch.get("batchSize").n());
        assertNotNull(batch.get("attemptAfter").s());

        for (NationalRegistriesRequest request : requests) {
            Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(request.getCorrelationId()).getFirst();
            assertEquals("WORKING", storedRequest.get("status").s());
            assertEquals("INIPEC#WORKING", storedRequest.get("registryStatus").s());
            assertEquals(batch.get("batchId").s(), storedRequest.get("batchId").s());
        }
    }

    @Test
    void limitsBatchToConfiguredMaxSizeAndLeavesRemainingRequestsPending() {
        List<NationalRegistriesRequest> requests = persistRequests(4);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse("polling-id")));

        iniPecBatchRequestJob.batchRequestScheduled();

        ArgumentCaptor<IniPecBatchRequest> requestCaptor = ArgumentCaptor.forClass(IniPecBatchRequest.class);
        verify(infoCamereClient).callEServiceRequestId(requestCaptor.capture());
        assertEquals(3, requestCaptor.getValue().getElencoCf().size());
        assertEquals(1, findBatches().size());

        long working = requests.stream().filter(request -> "WORKING".equals(findNationalRegistriesRequestsByCorrelationId(request.getCorrelationId()).getFirst().get("status").s())).count();
        long notWorked = requests.stream().filter(request -> "NOT_WORKED".equals(findNationalRegistriesRequestsByCorrelationId(request.getCorrelationId()).getFirst().get("status").s())).count();
        assertEquals(3, working);
        assertEquals(1, notWorked);
    }

    @Test
    void doesNothingWhenThereAreNoPendingInipecRequests() {
        iniPecBatchRequestJob.batchRequestScheduled();

        verifyNoInteractions(infoCamereClient);
        assertEquals(0, findBatches().size());
    }

    @Test
    void marksAllRequestsAsErrorWhenInipecReturnsBadRequest() {
        List<NationalRegistriesRequest> requests = persistRequests(3);
        PnNationalRegistriesException badRequest = pnException("Invalid massive request", HttpStatus.BAD_REQUEST.value());
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.error(badRequest));

        iniPecBatchRequestJob.batchRequestScheduled();

        assertEquals(0, findBatches().size());
        requests.forEach(this::assertErrorWithoutBatchId);
    }

    @Test
    void propagatesNonBadRequestWithoutPersistingBatchOrChangingRequests() {
        List<NationalRegistriesRequest> requests = persistRequests(3);
        PnNationalRegistriesException providerError = pnException("INIPEC unavailable", HttpStatus.INTERNAL_SERVER_ERROR.value());
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.error(providerError));

        PnNationalRegistriesException thrown = assertThrows(PnNationalRegistriesException.class, () -> iniPecBatchRequestJob.batchRequestScheduled());

        assertEquals(providerError, thrown);
        assertEquals(0, findBatches().size());
        requests.forEach(this::assertNotWorkedWithoutBatchId);
    }

    @Test
    void propagatesUnexpectedProviderErrorWithoutPersistingBatchOrChangingRequests() {
        List<NationalRegistriesRequest> requests = persistRequests(3);
        IllegalStateException providerError = new IllegalStateException("INIPEC unavailable");
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.error(providerError));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> iniPecBatchRequestJob.batchRequestScheduled());

        assertEquals(providerError, thrown);
        assertEquals(0, findBatches().size());
        requests.forEach(this::assertNotWorkedWithoutBatchId);
    }

    @Test
    void rejectsInipecResponseWithoutPollingIdentifierWithoutPersistingChanges() {
        List<NationalRegistriesRequest> requests = persistRequests(3);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(new IniPecBatchResponse()));

        PnInternalException thrown = assertThrows(PnInternalException.class, () -> iniPecBatchRequestJob.batchRequestScheduled());

        assertNotNull(thrown.getProblem().getDetail());
        assertTrue(thrown.getProblem().getDetail().contains("INIPEC returned no polling identifier"));
        assertEquals(0, findBatches().size());
        requests.forEach(this::assertNotWorkedWithoutBatchId);
    }

    private List<NationalRegistriesRequest> persistRequests(int count) {
        Instant createdAt = Instant.now().minusSeconds(10).truncatedTo(ChronoUnit.MILLIS);
        return IntStream.range(0, count).mapToObj(index -> request(index, createdAt.plusMillis(index))).peek(request -> nationalRegistriesRequestsRepository.putRequest(request).block()).toList();
    }

    private NationalRegistriesRequest request(int index, Instant createdAt) {
        NationalRegistriesRequest request = new NationalRegistriesRequest();
        request.setCorrelationId("inipec-batch-job-" + index + "-" + java.util.UUID.randomUUID());
        request.setTaxId("1234567890" + index);
        request.setCreatedAt(createdAt);
        request.setAttemptAfter(formatInstant(createdAt.minusSeconds(1)));
        request.setRegistry(GatewayDownstreamService.INIPEC);
        request.setStatus(RequestStatusEnum.NOT_WORKED);
        request.setRegistryStatus("INIPEC#NOT_WORKED");
        return request;
    }

    private List<Map<String, AttributeValue>> findBatches() {
        return dynamoDbClient.scan(builder -> builder.tableName(NATIONAL_REGISTRIES_BATCHES_TABLE)).items();
    }

    private void assertNotWorkedWithoutBatchId(NationalRegistriesRequest request) {
        Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(request.getCorrelationId()).getFirst();
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    private void assertErrorWithoutBatchId(NationalRegistriesRequest request) {
        Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(request.getCorrelationId()).getFirst();
        assertEquals("ERROR", storedRequest.get("status").s());
        assertEquals("INIPEC#ERROR", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    private PnNationalRegistriesException pnException(String message, int statusCode) {
        return new PnNationalRegistriesException(message, statusCode, "error", null, null, Charset.defaultCharset(), null);
    }
}
