package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.job.InadBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.service.InadPollingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.*;

import static it.pagopa.pn.national.registries.entity.NationalRegistriesBatch.SK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=false",
        "pn.national-registries.enable-inad-massive-api=true",
        "pn.national-registries.inad.max-batch-request-size=9",
        "pn.national-registries.query-limit=2",
        "pn.national-registries.inad.oldest-request-max-waiting-seconds=60",
        "pn.national-registries.inad.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inad.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inad.batch-polling-max-retry=5",
        "pn.national-registries.inad.batch-polling-in-progress-max-retry=10",
        "pn.national-registries.inad.polling-error-retry-after=1",
        "pn.national-registries.inad.polling-in-progress-retry-after=1"
})
public class DigitalAddressPFMassiveIT extends AbstractNationalRegistriesComponentIT {

    private static final String PF_FOUND = "RSSMRA80A01H501A";
    private static final String PF_NOT_FOUND = "RSSMRA80A01H501B";
    private static final String PF_PROFESSIONAL_ONLY = "RSSMRA80A01H501C";
    private static final String PF_PERSONAL_AND_PROFESSIONAL = "RSSMRA80A01H501D";
    private static final String PF_INVALID_PERSONAL = "RSSMRA80A01H501E";
    private static final String PF_MULTIPLE_PROFESSIONALS = "RSSMRA80A01H501F";
    private static final String PF_EMPTY = "RSSMRA80A01H501G";
    private static final String PF_EXPIRED_PERSONAL_AND_PROFESSIONAL = "RSSMRA80A01H501H";
    private static final String PF_EXPIRED_PERSONAL = "RSSMRA80A01H501I";

    @Autowired
    private InadBatchRequestJob inadBatchRequestJob;

    @Autowired
    private InadPollingService inadPollingService;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;

    @Test
    void PF_Digital_INAD_Massive_AllFunctionalScenarios() {
        String foundCorrelationId = "pf-found-" + UUID.randomUUID();
        String notFoundCorrelationId = "pf-not-found-" + UUID.randomUUID();
        String professionalOnlyCorrelationId = "pf-professional-only-" + UUID.randomUUID();
        String personalAndProfessionalCorrelationId = "pf-personal-professional-" + UUID.randomUUID();
        String invalidPersonalCorrelationId = "pf-invalid-personal-" + UUID.randomUUID();
        String multipleProfessionalsCorrelationId = "pf-multiple-prof-" + UUID.randomUUID();
        String emptyCorrelationId = "pf-empty-" + UUID.randomUUID();
        String expiredPersonalAndProfessionalCorrelationId = "pf-expired-personal-professional-" + UUID.randomUUID();
        String expiredPersonalCorrelationId = "pf-expired-personal-" + UUID.randomUUID();

        List<TestRequest> testRequests = List.of(
                new TestRequest(PF_FOUND, foundCorrelationId),
                new TestRequest(PF_NOT_FOUND, notFoundCorrelationId),
                new TestRequest(PF_PROFESSIONAL_ONLY, professionalOnlyCorrelationId),
                new TestRequest(PF_PERSONAL_AND_PROFESSIONAL, personalAndProfessionalCorrelationId),
                new TestRequest(PF_INVALID_PERSONAL, invalidPersonalCorrelationId),
                new TestRequest(PF_MULTIPLE_PROFESSIONALS, multipleProfessionalsCorrelationId),
                new TestRequest(PF_EMPTY, emptyCorrelationId),
                new TestRequest(PF_EXPIRED_PERSONAL_AND_PROFESSIONAL, expiredPersonalAndProfessionalCorrelationId),
                new TestRequest(PF_EXPIRED_PERSONAL, expiredPersonalCorrelationId)
        );

        for (TestRequest testRequest : testRequests) {
            AddressOKDto response = enqueueGatewayRequest(
                    "PF",
                    testRequest.taxId(),
                    AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                    testRequest.correlationId()
            );

            processNextGatewayInputMessage();

            assertEquals(testRequest.correlationId(), response.getCorrelationId());

            List<Map<String, AttributeValue>> storedRequests = findNationalRegistriesRequestsByCorrelationId(testRequest.correlationId());
            assertEquals(1, storedRequests.size());

            Map<String, AttributeValue> storedRequest = storedRequests.getFirst();
            assertEquals(testRequest.taxId(), storedRequest.get("taxId").s());
            assertEquals("PF", storedRequest.get("recipientType").s());
            assertEquals("DIGITAL", storedRequest.get("domicileType").s());
            assertEquals("INAD", storedRequest.get("registry").s());
            assertEquals("NOT_WORKED", storedRequest.get("status").s());
            assertEquals("INAD#NOT_WORKED", storedRequest.get("registryStatus").s());
            assertNull(storedRequest.get("batchId"));
            assertNull(storedRequest.get("resultPayload"));
        }

        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        String pollingId = "polling-" + UUID.randomUUID();

        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();
        batchResponse.setId(pollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(batchResponse));

        inadBatchRequestJob.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> batchRequestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(batchRequestCaptor.capture());

        assertEquals(
                List.of(
                        PF_FOUND,
                        PF_NOT_FOUND,
                        PF_PROFESSIONAL_ONLY,
                        PF_PERSONAL_AND_PROFESSIONAL,
                        PF_INVALID_PERSONAL,
                        PF_MULTIPLE_PROFESSIONALS,
                        PF_EMPTY,
                        PF_EXPIRED_PERSONAL_AND_PROFESSIONAL,
                        PF_EXPIRED_PERSONAL
                ),
                batchRequestCaptor.getValue().getCodiciFiscali()
        );

        String batchId = null;

        for (TestRequest testRequest : testRequests) {
            Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(testRequest.correlationId()).getFirst();

            assertEquals("WORKING", storedRequest.get("status").s());
            assertEquals("INAD#WORKING", storedRequest.get("registryStatus").s());
            assertNotNull(storedRequest.get("batchId"));
            assertNull(storedRequest.get("resultPayload"));

            if (batchId == null) {
                batchId = storedRequest.get("batchId").s();
            } else {
                assertEquals(batchId, storedRequest.get("batchId").s());
            }
        }

        assertNotNull(batchId);

        NationalRegistriesBatch batch = getBatch(batchId);
        assertNotNull(batch);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(GatewayDownstreamService.INAD.name(), batch.getRegistry());
        assertEquals(pollingId, batch.getPollingId());
        assertEquals(9, batch.getBatchSize());
        assertEquals(0, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());
        assertNotNull(batch.getAttemptAfter());
        assertEquals(batchId, batch.getPracticalReferenceValue());

        pollingJob.polling();

        batch = getBatch(batchId);
        assertNotNull(batch);
        assertEquals(BatchesStatusEnum.WORKING, batch.getStatus());
        assertEquals(GatewayDownstreamService.INAD.name(), batch.getRegistry());
        assertEquals(pollingId, batch.getPollingId());
        assertEquals(9, batch.getBatchSize());
        assertEquals(0, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());

        for (TestRequest testRequest : testRequests) {
            Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(testRequest.correlationId()).getFirst();
            assertEquals("WORKING", storedRequest.get("status").s());
            assertEquals("INAD#WORKING", storedRequest.get("registryStatus").s());
            assertEquals(batchId, storedRequest.get("batchId").s());
            assertNull(storedRequest.get("resultPayload"));
        }

        ResponseStatusListDigitalAddress statusResponse = new ResponseStatusListDigitalAddress();
        statusResponse.setState(StatusProcessingRequest.DISPONIBILE);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(statusResponse));

        ResponseRequestDigitalAddress foundResponse = inadResponse(PF_FOUND, "pf@pec.it", null);

        ElementDigitalAddress expiredPersonalWithProfessional = inadAddress("expired-personal@pec.it", null);
        expiredPersonalWithProfessional.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        ResponseRequestDigitalAddress expiredPersonalAndProfessionalResponse = inadResponse(
                PF_EXPIRED_PERSONAL_AND_PROFESSIONAL,
                List.of(
                        expiredPersonalWithProfessional,
                        inadAddress("professional-expired-case@pec.it", "INGEGNERE")
                )
        );

        ElementDigitalAddress expiredPersonal = inadAddress("expired-only@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        ResponseRequestDigitalAddress expiredPersonalResponse = inadResponse(
                PF_EXPIRED_PERSONAL,
                List.of(
                        expiredPersonal
                )
        );

        ResponseRequestDigitalAddress professionalOnlyResponse = inadResponse(
                PF_PROFESSIONAL_ONLY,
                List.of(
                        inadAddress("professional@pec.it", "INGEGNERE")
                )
        );

        ResponseRequestDigitalAddress personalAndProfessionalResponse = inadResponse(
                PF_PERSONAL_AND_PROFESSIONAL,
                List.of(
                        inadAddress("personal@pec.it", null),
                        inadAddress("professional@pec.it", "INGEGNERE")
                )
        );

        ResponseRequestDigitalAddress invalidPersonalResponse = inadResponse(
                PF_INVALID_PERSONAL,
                List.of(
                        inadAddress("2026-01-01", null),
                        inadAddress("professional@pec.it", "INGEGNERE")
                )
        );

        ResponseRequestDigitalAddress multipleProfessionalsResponse = inadResponse(
                PF_MULTIPLE_PROFESSIONALS,
                List.of(
                        inadAddress("prof1@pec.it", "INGEGNERE"),
                        inadAddress("prof2@pec.it", "ARCHITETTO")
                )
        );

        ResponseRequestDigitalAddress emptyResponse = inadResponse(PF_EMPTY, List.of());

        ResponseListDigitalAddress massiveResponse = new ResponseListDigitalAddress();

        massiveResponse.setList(
                List.of(
                        foundResponse,
                        professionalOnlyResponse,
                        personalAndProfessionalResponse,
                        invalidPersonalResponse,
                        multipleProfessionalsResponse,
                        emptyResponse,
                        expiredPersonalAndProfessionalResponse,
                        expiredPersonalResponse
                )
        );

        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.just(massiveResponse));

        inadPollingService.handleMessage(
                PnPollingEvent.Payload.builder()
                        .registry(GatewayDownstreamService.INAD)
                        .batchId(batchId)
                        .pollingId(pollingId)
                        .batchSize(9)
                        .errorRetryCount(0)
                        .inProgressRetryCount(0)
                        .build()
        ).block();

        batch = getBatch(batchId);
        assertNotNull(batch);
        assertEquals(BatchesStatusEnum.WORKED, batch.getStatus());
        assertEquals(GatewayDownstreamService.INAD.name(), batch.getRegistry());
        assertEquals(pollingId, batch.getPollingId());
        assertEquals(9, batch.getBatchSize());
        assertEquals(0, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());

        verify(inadClient).checkMassiveRequestStatus(pollingId);
        verify(inadClient).getMassiveRequestResult(pollingId);

        Map<String, AttributeValue> foundRequest = findNationalRegistriesRequestsByCorrelationId(foundCorrelationId).getFirst();
        assertEquals("WORKED", foundRequest.get("status").s());
        assertEquals("INAD#WORKED", foundRequest.get("registryStatus").s());
        assertEquals(batchId, foundRequest.get("batchId").s());
        assertNotNull(foundRequest.get("resultPayload"));
        assertTrue(foundRequest.get("resultPayload").s().contains("pf@pec.it"));

        Map<String, AttributeValue> notFoundRequest = findNationalRegistriesRequestsByCorrelationId(notFoundCorrelationId).getFirst();
        assertEquals("WORKED", notFoundRequest.get("status").s());
        assertEquals("INAD#WORKED", notFoundRequest.get("registryStatus").s());
        assertEquals(batchId, notFoundRequest.get("batchId").s());
        assertNotNull(notFoundRequest.get("resultPayload"));
        assertFalse(notFoundRequest.get("resultPayload").s().contains("@pec.it"));

        Map<String, AttributeValue> professionalOnlyRequest = findNationalRegistriesRequestsByCorrelationId(professionalOnlyCorrelationId).getFirst();
        assertEquals("WORKED", professionalOnlyRequest.get("status").s());
        assertEquals("INAD#WORKED", professionalOnlyRequest.get("registryStatus").s());
        assertEquals(batchId, professionalOnlyRequest.get("batchId").s());
        assertNotNull(professionalOnlyRequest.get("resultPayload"));
        assertFalse(professionalOnlyRequest.get("resultPayload").s().contains("professional@pec.it"));

        Map<String, AttributeValue> personalAndProfessionalRequest = findNationalRegistriesRequestsByCorrelationId(personalAndProfessionalCorrelationId).getFirst();
        assertEquals("WORKED", personalAndProfessionalRequest.get("status").s());
        assertEquals("INAD#WORKED", personalAndProfessionalRequest.get("registryStatus").s());
        assertEquals(batchId, personalAndProfessionalRequest.get("batchId").s());
        assertNotNull(personalAndProfessionalRequest.get("resultPayload"));
        assertTrue(personalAndProfessionalRequest.get("resultPayload").s().contains("personal@pec.it"));
        assertFalse(personalAndProfessionalRequest.get("resultPayload").s().contains("professional@pec.it"));

        Map<String, AttributeValue> invalidPersonalRequest = findNationalRegistriesRequestsByCorrelationId(invalidPersonalCorrelationId).getFirst();
        assertEquals("WORKED", invalidPersonalRequest.get("status").s());
        assertEquals("INAD#WORKED", invalidPersonalRequest.get("registryStatus").s());
        assertEquals(batchId, invalidPersonalRequest.get("batchId").s());
        assertNotNull(invalidPersonalRequest.get("resultPayload"));
        assertFalse(invalidPersonalRequest.get("resultPayload").s().contains("2026-01-01"));
        assertFalse(invalidPersonalRequest.get("resultPayload").s().contains("professional@pec.it"));

        Map<String, AttributeValue> multipleProfessionalsRequest = findNationalRegistriesRequestsByCorrelationId(multipleProfessionalsCorrelationId).getFirst();
        assertEquals("WORKED", multipleProfessionalsRequest.get("status").s());
        assertEquals("INAD#WORKED", multipleProfessionalsRequest.get("registryStatus").s());
        assertEquals(batchId, multipleProfessionalsRequest.get("batchId").s());
        assertNotNull(multipleProfessionalsRequest.get("resultPayload"));
        assertFalse(multipleProfessionalsRequest.get("resultPayload").s().contains("prof1@pec.it"));
        assertFalse(multipleProfessionalsRequest.get("resultPayload").s().contains("prof2@pec.it"));

        Map<String, AttributeValue> emptyRequest = findNationalRegistriesRequestsByCorrelationId(emptyCorrelationId).getFirst();
        assertEquals("WORKED", emptyRequest.get("status").s());
        assertEquals("INAD#WORKED", emptyRequest.get("registryStatus").s());
        assertEquals(batchId, emptyRequest.get("batchId").s());
        assertNotNull(emptyRequest.get("resultPayload"));
        assertFalse(emptyRequest.get("resultPayload").s().contains("@pec.it"));

        Map<String, AttributeValue> expiredPersonalAndProfessionalRequest = findNationalRegistriesRequestsByCorrelationId(expiredPersonalAndProfessionalCorrelationId).getFirst();
        assertEquals("WORKED", expiredPersonalAndProfessionalRequest.get("status").s());
        assertEquals("INAD#WORKED", expiredPersonalAndProfessionalRequest.get("registryStatus").s());
        assertEquals(batchId, expiredPersonalAndProfessionalRequest.get("batchId").s());
        assertNotNull(expiredPersonalAndProfessionalRequest.get("resultPayload"));
        assertFalse(expiredPersonalAndProfessionalRequest.get("resultPayload").s().contains("expired-personal@pec.it"));
        assertFalse(expiredPersonalAndProfessionalRequest.get("resultPayload").s().contains("professional-expired-case@pec.it"));

        Map<String, AttributeValue> expiredPersonalRequest = findNationalRegistriesRequestsByCorrelationId(expiredPersonalCorrelationId).getFirst();
        assertEquals("WORKED", expiredPersonalRequest.get("status").s());
        assertEquals("INAD#WORKED", expiredPersonalRequest.get("registryStatus").s());
        assertEquals(batchId, expiredPersonalRequest.get("batchId").s());
        assertNotNull(expiredPersonalRequest.get("resultPayload"));
        assertFalse(expiredPersonalRequest.get("resultPayload").s().contains("expired-only@pec.it"));

        for (TestRequest testRequest : testRequests) {
            Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(testRequest.correlationId()).getFirst();

            assertEquals("WORKED", storedRequest.get("status").s());
            assertEquals("INAD#WORKED", storedRequest.get("registryStatus").s());
            assertEquals(batchId, storedRequest.get("batchId").s());
            assertNotNull(storedRequest.get("resultPayload"));
        }

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        verify(inadClient, never()).callEService(any(), any());
        verifyNoInteractions(ipaClient, anprClient);
    }

    @Test
    void PF_Digital_INAD_Massive_Error() {
        List<TestRequest> testRequests = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            String taxId = "RSSMRA80A01H50" + i + "U";
            String correlationId = "pf-error-" + i + "-" + UUID.randomUUID();

            testRequests.add(new TestRequest(taxId, correlationId));

            AddressOKDto response = enqueueGatewayRequest(
                    "PF",
                    taxId,
                    AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                    correlationId
            );

            processNextGatewayInputMessage();

            assertEquals(correlationId, response.getCorrelationId());

            Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(correlationId).getFirst();
            assertEquals("NOT_WORKED", storedRequest.get("status").s());
            assertEquals("INAD#NOT_WORKED", storedRequest.get("registryStatus").s());
            assertNull(storedRequest.get("batchId"));
            assertNull(storedRequest.get("resultPayload"));
        }

        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        String pollingId = "polling-error-" + UUID.randomUUID();

        ResponseRequestListDigitalAddress batchResponse = new ResponseRequestListDigitalAddress();
        batchResponse.setId(pollingId);

        when(inadClient.callMassiveEService(any(RequestListDigitalAddress.class))).thenReturn(Mono.just(batchResponse));

        inadBatchRequestJob.batchRequest();

        ArgumentCaptor<RequestListDigitalAddress> batchRequestCaptor = ArgumentCaptor.forClass(RequestListDigitalAddress.class);
        verify(inadClient).callMassiveEService(batchRequestCaptor.capture());
        assertEquals(9, batchRequestCaptor.getValue().getCodiciFiscali().size());
        assertEquals(
                testRequests.stream().map(TestRequest::taxId).toList(),
                batchRequestCaptor.getValue().getCodiciFiscali()
        );

        String batchId = findNationalRegistriesRequestsByCorrelationId(testRequests.getFirst().correlationId())
                .getFirst()
                .get("batchId")
                .s();

        assertNotNull(batchId);

        NationalRegistriesBatch batch = getBatch(batchId);
        assertNotNull(batch);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(GatewayDownstreamService.INAD.name(), batch.getRegistry());
        assertEquals(pollingId, batch.getPollingId());
        assertEquals(9, batch.getBatchSize());
        assertEquals(0, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());
        assertNotNull(batch.getAttemptAfter());
        assertEquals(batchId, batch.getPracticalReferenceValue());

        for (TestRequest testRequest : testRequests) {
            Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(testRequest.correlationId()).getFirst();

            assertEquals("WORKING", storedRequest.get("status").s());
            assertEquals("INAD#WORKING", storedRequest.get("registryStatus").s());
            assertEquals(batchId, storedRequest.get("batchId").s());
            assertNull(storedRequest.get("resultPayload"));
        }

        pollingJob.polling();

        batch = getBatch(batchId);
        assertNotNull(batch);
        assertEquals(BatchesStatusEnum.WORKING, batch.getStatus());
        assertEquals(0, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());

        for (TestRequest testRequest : testRequests) {
            Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(testRequest.correlationId()).getFirst();

            assertEquals("WORKING", storedRequest.get("status").s());
            assertEquals("INAD#WORKING", storedRequest.get("registryStatus").s());
            assertEquals(batchId, storedRequest.get("batchId").s());
            assertNull(storedRequest.get("resultPayload"));
        }

        ResponseStatusListDigitalAddress statusResponse = new ResponseStatusListDigitalAddress();
        statusResponse.setState(StatusProcessingRequest.DISPONIBILE);

        when(inadClient.checkMassiveRequestStatus(pollingId)).thenReturn(Mono.just(statusResponse));
        when(inadClient.getMassiveRequestResult(pollingId)).thenReturn(Mono.error(new RuntimeException("INAD unavailable")));

        inadPollingService.handleMessage(
                PnPollingEvent.Payload.builder()
                        .registry(GatewayDownstreamService.INAD)
                        .batchId(batchId)
                        .pollingId(pollingId)
                        .batchSize(9)
                        .errorRetryCount(0)
                        .inProgressRetryCount(0)
                        .build()
        ).block();

        batch = getBatch(batchId);
        assertNotNull(batch);
        assertEquals(BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(GatewayDownstreamService.INAD.name(), batch.getRegistry());
        assertEquals(pollingId, batch.getPollingId());
        assertEquals(9, batch.getBatchSize());
        assertEquals(1, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());
        assertNotNull(batch.getAttemptAfter());

        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        for (TestRequest testRequest : testRequests) {
            Map<String, AttributeValue> storedRequest = findNationalRegistriesRequestsByCorrelationId(testRequest.correlationId()).getFirst();

            assertEquals("WORKING", storedRequest.get("status").s());
            assertEquals("INAD#WORKING", storedRequest.get("registryStatus").s());
            assertEquals(batchId, storedRequest.get("batchId").s());
            assertNull(storedRequest.get("resultPayload"));
        }

        verify(inadClient).checkMassiveRequestStatus(pollingId);
        verify(inadClient).getMassiveRequestResult(pollingId);
        verify(inadClient, never()).callEService(any(), any());
        verifyNoInteractions(ipaClient, anprClient);
    }

    private NationalRegistriesBatch getBatch(String batchId) {
        return nationalRegistriesBatchesRepository.getBatch(batchId).block();
    }

    private record TestRequest(String taxId, String correlationId) {
    }
}