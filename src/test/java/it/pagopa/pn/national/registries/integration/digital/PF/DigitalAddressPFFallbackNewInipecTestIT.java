package it.pagopa.pn.national.registries.integration.digital.PF;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.integration.AbstractNationalRegistriesComponentIT;
import it.pagopa.pn.national.registries.integration.PnNationalRegistriesTestApplication;
import it.pagopa.pn.national.registries.job.IniPecBatchRequestJob;
import it.pagopa.pn.national.registries.job.PollingJob;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnPollingEvent;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.GatewayResponse;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.service.InipecPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import java.util.HashMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Scenari funzionali PF DIGITAL
 *
 * Configurazione:
 *  - fallback PF attivo
 *  - INAD massivo disattivato
 *  - old IniPEC disattivo
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.enable-old-inipec-flow=false",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "spring.task.scheduling.enabled=false",
        "pn.national-registries.inipec.max-batch-request-size=20",
        "pn.national-registries.inipec.request-retry-after=0",
        "pn.national-registries.inipec.batch-request-max-retry=2",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=2",
        "pn.national-registries.inipec.polling-error-retry-after=0",
        "pn.national-registries.inipec.polling-in-progress-retry-after=0"
})
public class DigitalAddressPFFallbackNewInipecTestIT extends AbstractNationalRegistriesComponentIT {

    @Autowired
    private IniPecBatchRequestJob iniPecBatchRequestJob;

    @Autowired
    private PollingJob pollingJob;

    @Autowired
    private InipecPollingService inipecPollingService;
    private static final String PF_PROFESSIONAL = "WDSTKB60E09L538U";
    private static final String PF_BUSINESS_PROFESSIONAL = "PRPMGG80A41H501W";
    private static final String PF_INVALID = "FLPCPT69A65Z336P";
    private static final String PF_ND = "DLGMRA73D41F839C";
    private static final String PF_NF = "PRMRGG74T16H501R";
    private static final String PF_MISSING = "VDADGI80A01F839E";
    private static final String PF_ER = "RSSSRG80A01F839I";

    /**
     * Inad pec personale valida
     */
    @Test
    void PF_Digital_INAD_PersonalFound_NoInipecFallback() {
        String correlationId = "pf-digital-personal-found-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, "personal@pec.it", null)));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("personal@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Inad 404 NotFound
     */
    @Test
    void PF_Digital_INAD_NotFound_InipecFallback() {
        String correlationId = "pf-digital-inad-not-found-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId);
        completeInipecFinal(correlationId, "inipec-not-found-fallback@pec.it");
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Inad 200 con lista vuota
     */
    @Test
    void PF_Digital_INAD_EmptyList_FallbackEnabled() {
        String correlationId = "pf-digital-empty-list-enabled-" + UUID.randomUUID();

        ResponseRequestDigitalAddress emptyResponse = new ResponseRequestDigitalAddress();
        emptyResponse.setCodiceFiscale(VALID_PF_CF);
        emptyResponse.setSince(Date.from(Instant.parse("2026-10-10T10:15:30Z")));
        emptyResponse.setDigitalAddress(List.of());

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(emptyResponse));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId);
        completeInipecFinal(correlationId, "inipec-after-empty-list@pec.it");
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Inad pec personale scaduta
     */
    @Test
    void shouldFallbackToInipecWhenPersonalExpired() {
        String correlationId = "pf-digital-expired-personal-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                expiredPersonal
        ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId);
        completeInipecFinal(correlationId, "inipec-after-expired-pending@pec.it");
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Inad pec personale senza usageInfo
     */
    @Test
    void PF_Digital_INAD_PersonalWithoutUsageInfo_IsValid() {
        String correlationId = "pf-digital-personal-no-usage-info-fallback-" + UUID.randomUUID();
        ElementDigitalAddress personal = new ElementDigitalAddress();
        personal.setDigitalAddress("personal-no-usage@pec.it");
        personal.setPracticedProfession(null);

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(personal))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("personal-no-usage@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Inad pec personale senza data fine validità
     */
    @Test
    void PF_Digital_INAD_PersonalWithoutEndValidity_IsValid() {
        String correlationId = "pf-digital-personal-no-end-validity-fallback-" + UUID.randomUUID();
        ElementDigitalAddress personal = inadAddress("personal-no-end@pec.it", null);
        personal.getUsageInfo().setDateEndValidity(null);

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(personal))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("personal-no-end@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Verifica che con più PEC personali venga ignorata quella scaduta
     * e selezionata la prima PEC valida disponibile (da linee guida è possbile avere solo una pec personale valida censita).
     */
    @Test
    void PF_Digital_INAD_FirstPersonalInvalid_SecondPersonalValid_SecondIsSelected() {
        String correlationId = "pf-digital-first-personal-invalid-fallback-" + UUID.randomUUID();
        ElementDigitalAddress elementDigitalAddress = inadAddress("first-expired@pec.it", null);
        elementDigitalAddress.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        elementDigitalAddress,
                        inadAddress("second-valid@pec.it", null)
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("second-valid@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Inad Personale presente, Inad professionale presente
     */
    @Test
    void PF_Digital_INAD_PersonalInsteadOfProfessional_FallbackEnabled() {
        String correlationId = "pf-digital-personal-preferred-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("personal@pec.it", null),
                        inadAddress("professional@pec.it", "INGEGNERE")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("personal@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Inad Personale non presente, Inad professionale Presente
     */
    @Test
    void PF_Digital_INAD_PersonalNotFound_ProfessionalFound_FallbackEnabled() {
        String correlationId = "pf-digital-professional-only-enabled-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("professional@pec.it", "INGEGNERE")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Inad Personale non valida, Inad professionale presente e valida
     */
    @Test
    void PF_Digital_INAD_PersonalInvalid_ProfessionalValid_FallbackEnabled() {
        String correlationId = "pf-digital-invalid-personal-professional-fallback-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                inadAddress("invalid-personal", null),
                inadAddress("professional@pec.it", "INGEGNERE")
        ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Inad Personale non valida, presenti 2 pec professionali valide
     */
    @Test
    void PF_Digital_INAD_MultipleProfessionals_FallbackEnabled() {
        String correlationId = "pf-digital-multiple-prof-enabled-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("prof1@pec.it", "INGEGNERE"),
                        inadAddress("prof2@pec.it", "ARCHITETTO")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("prof1@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Inad errore tecnico (eccezione diversa da 404 CF NotFound)
     */
    @Test
    void PF_Digital_INAD_TechnicalError_NoInipecFallback() {
        String correlationId = "pf-digital-technical-fallback-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("INAD unavailable");
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        assertEquals(correlationId, response.getCorrelationId());
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Inad pec personale scaduta, pec professionale valida
     */
    @Test
    void shouldTakeProfessionalWhenPersonalExpired() {
        String correlationId = "pf-digital-expired-personal-professional-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                expiredPersonal,
                inadAddress("professional@pec.it", "INGEGNERE")
        ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getDigitalAddress().getFirst().getRecipient());

        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Inad personale non valida, Inad professionale scaduta
     */
    @Test
    void PF_Digital_INAD_OnlyProfessionalExpired_FallbackEnabled() {
        String correlationId = "pf-digital-only-professional-expired-fallback-" + UUID.randomUUID();
        ElementDigitalAddress expiredProfessional = inadAddress("expired-professional@pec.it", "AVVOCATO");
        expiredProfessional.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(expiredProfessional))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId);
        completeInipecFinal(correlationId, "inipec-after-expired-professional@pec.it");
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * INAD NotFound -> IniPEC con PEC professionale valida.
     */
    @Test
    void PF_Digital_FullFlow_InipecProfessionalPecIsReturned() {
        String correlationId = "pf-inipec-professional-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_PROFESSIONAL), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_PROFESSIONAL, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId, PF_PROFESSIONAL);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponseWithProfessionals(PF_PROFESSIONAL, null, "professional@pec.it")
        )));
        pollInipec(batchId, pollingId, 1);

        GatewayResponse payload = finalPayload(correlationId);
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * INAD NotFound -> IniPEC con PEC impresa e tutte le PEC professionali valide.
     */
    @Test
    void PF_Digital_FullFlow_InipecBusinessAndAllProfessionalPecsAreReturned() {
        String correlationId = "pf-inipec-business-professionals-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_BUSINESS_PROFESSIONAL), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_BUSINESS_PROFESSIONAL, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId, PF_BUSINESS_PROFESSIONAL);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponseWithProfessionals(PF_BUSINESS_PROFESSIONAL, "business@pec.it", "professional-one@pec.it", "professional-two@pec.it")
        )));
        pollInipec(batchId, pollingId, 1);

        GatewayResponse payload = finalPayload(correlationId);
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(3, payload.getPayload().getDigitalAddress().size());
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "business@pec.it".equals(address.getAddress()) && "IMPRESA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-one@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-two@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
    }

    /**
     * INAD NotFound -> IniPEC con PEC non valida.
     */
    @Test
    void PF_Digital_FullFlow_InipecInvalidPecProducesEmptyFinalResult() {
        String correlationId = "pf-inipec-invalid-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_INVALID), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_INVALID, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId, PF_INVALID);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(
                pecResponse(PF_INVALID, "not-an-email")
        )));
        pollInipec(batchId, pollingId, 1);

        GatewayResponse payload = finalPayload(correlationId);
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> IniPEC con stato ND.
     */
    @Test
    void PF_Digital_FullFlow_InipecNdProducesEmptyFinalResult() {
        String correlationId = "pf-inipec-nd-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_ND), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_ND, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId, PF_ND);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        Pec nd = pecResponse(PF_ND, "ignored-nd@pec.it");
        nd.setStatoImpresa(Pec.StatoImpresaEnum.ND);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(nd)));
        pollInipec(batchId, pollingId, 1);

        GatewayResponse payload = finalPayload(correlationId);
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> IniPEC con stato NF.
     */
    @Test
    void PF_Digital_FullFlow_InipecNfProducesEmptyFinalResult() {
        String correlationId = "pf-inipec-nf-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_NF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_NF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId, PF_NF);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        Pec nf = pecResponse(PF_NF, "ignored-nf@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse(nf)));
        pollInipec(batchId, pollingId, 1);

        GatewayResponse payload = finalPayload(correlationId);
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> IniPEC quando la risposta non contiene il codice fiscale richiesto.
     */
    @Test
    void PF_Digital_FullFlow_InipecMissingTaxIdProducesEmptyFinalResult() {
        String correlationId = "pf-inipec-missing-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_MISSING), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_MISSING, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId, PF_MISSING);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(inipecPollingResponse()));
        pollInipec(batchId, pollingId, 1);

        GatewayResponse payload = finalPayload(correlationId);
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> IniPEC con stato ER, retry e passaggio a ERROR al limite configurato.
     */
    @Test
    void PF_Digital_FullFlow_InipecErRetriesThenEndsInErrorAtConfiguredLimit() {
        String correlationId = "pf-inipec-er-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_ER), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_ER, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId, PF_ER);

        String firstPollingId = "polling-first-" + UUID.randomUUID();
        String firstBatchId = startInipecBatch(firstPollingId, correlationId);
        Pec er = pecResponse(PF_ER, "ignored@pec.it");
        er.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        when(infoCamereClient.callEServiceRequestPec(firstPollingId)).thenReturn(Mono.just(inipecPollingResponse(er)));
        pollInipec(firstBatchId, firstPollingId, 1);

        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("NOT_WORKED", request.get("status").s());
        assertEquals("1", request.get("retryCount").n());

        String secondPollingId = "polling-second-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(secondPollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        request = requestByRegistry(correlationId, "INIPEC");
        String secondBatchId = request.get("batchId").s();
        pollingJob.polling();
        when(infoCamereClient.callEServiceRequestPec(secondPollingId)).thenReturn(Mono.just(inipecPollingResponse(er)));
        pollInipec(secondBatchId, secondPollingId, 1);

        request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("ERROR", request.get("status").s());
        assertEquals("INIPEC#ERROR", request.get("registryStatus").s());
        assertNull(request.get("resultPayload"));
    }

    /**
     * INAD NotFound -> IniPEC quando il polling e ancora in progress e programma il retry.
     */
    @Test
    void PF_Digital_FullFlow_InipecPollingInProgressSchedulesRetry() {
        String correlationId = "pf-inipec-in-progress-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId, VALID_PF_CF);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(
                new it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException("List PEC in progress", 404, "Not Found", null, null, java.nio.charset.StandardCharsets.UTF_8, null)
        ));
        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> batch = findBatch(batchId);
        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("1", batch.get("inProgressRetryCount").n());
        assertEquals("WORKING", requestByRegistry(correlationId, "INIPEC").get("status").s());
    }

    /**
     * INAD NotFound -> IniPEC quando il polling restituisce metadata di errore e programma il retry.
     */
    @Test
    void PF_Digital_FullFlow_InipecErrorMetadataSchedulesRetry() {
        String correlationId = "pf-inipec-metadata-error-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId, VALID_PF_CF);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);
        IniPecPollingResponse pollingResponse = new IniPecPollingResponse();
        pollingResponse.setCode("123");
        pollingResponse.setDescription("temporary error");
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));
        pollInipec(batchId, pollingId, 1);

        Map<String, AttributeValue> batch = findBatch(batchId);
        assertEquals("REQUEST_SENT", batch.get("status").s());
        assertEquals("1", batch.get("errorRetryCount").n());
        assertEquals("WORKING", requestByRegistry(correlationId, "INIPEC").get("status").s());
    }

    /**
     * INAD NotFound -> IniPEC con più PEC professionali valide e nessuna PEC impresa.
     */
    @Test
    void PF_Digital_FullFlow_Inipec_AllProfessionalPecsArePreserved() {
        String correlationId = "pf-inipec-professionals-" + UUID.randomUUID();

        when(inadClient.callEService(eq(PF_PROFESSIONAL), eq(correlationId)))
                .thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest(
                "PF",
                PF_PROFESSIONAL,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId, PF_PROFESSIONAL);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);

        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(Mono.just(inipecPollingResponse(
                        pecResponseWithProfessionals(
                                PF_PROFESSIONAL,
                                null,
                                "professional-one@pec.it",
                                "professional-two@pec.it"
                        )
                )));

        pollInipec(batchId, pollingId, 1);

        GatewayResponse payload = finalPayload(correlationId);

        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(2, payload.getPayload().getDigitalAddress().size());
        assertTrue(payload.getPayload().getDigitalAddress().stream()
                .anyMatch(address -> "professional-one@pec.it".equals(address.getAddress())
                        && "PROFESSIONISTA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream()
                .anyMatch(address -> "professional-two@pec.it".equals(address.getAddress())
                        && "PROFESSIONISTA".equals(address.getRecipient())));
    }

    /**
     * INAD NotFound -> IniPEC con PEC impresa invalida e PEC professionale valida.
     */
    @Test
    void PF_Digital_FullFlow_Inipec_InvalidBusinessPecKeepsValidProfessionalPec() {
        String correlationId = "pf-inipec-invalid-business-" + UUID.randomUUID();

        when(inadClient.callEService(eq(PF_BUSINESS_PROFESSIONAL), eq(correlationId)))
                .thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest(
                "PF",
                PF_BUSINESS_PROFESSIONAL,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertInipecPendingRequest(correlationId, PF_BUSINESS_PROFESSIONAL);

        String pollingId = "polling-" + UUID.randomUUID();
        String batchId = startInipecBatch(pollingId, correlationId);

        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(Mono.just(inipecPollingResponse(
                        pecResponseWithProfessionals(
                                PF_BUSINESS_PROFESSIONAL,
                                "not-an-email",
                                "professional@pec.it"
                        )
                )));

        pollInipec(batchId, pollingId, 1);

        GatewayResponse payload = finalPayload(correlationId);

        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    private void completeInipecFinal(String correlationId, String address) {
        String pollingId = "inipec-full-flow-" + UUID.randomUUID();
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        Map<String, AttributeValue> request = findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(item -> "INIPEC".equals(item.get("registry").s()))
                .findFirst()
                .orElseThrow();
        String batchId = request.get("batchId").s();
        pollingJob.polling();
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pecResponse(VALID_PF_CF, address))));
        inipecPollingService.handleMessage(PnPollingEvent.Payload.builder()
                .registry(GatewayDownstreamService.INIPEC)
                .batchId(batchId)
                .pollingId(pollingId)
                .batchSize(1)
                .errorRetryCount(0)
                .inProgressRetryCount(0)
                .build()).block();
        request = findNationalRegistriesRequestsByCorrelationId(correlationId).stream()
                .filter(item -> "INIPEC".equals(item.get("registry").s()))
                .findFirst()
                .orElseThrow();
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("resultPayload"));
        GatewayResponse payload = readNewOutputMessage(request.get("resultPayload").s());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(address, payload.getPayload().getDigitalAddress().getFirst().getAddress());
    }

    private void assertInipecPendingRequest(String correlationId) {
        assertInipecPendingRequest(correlationId, VALID_PF_CF);
    }

    private void assertInipecPendingRequest(String correlationId, String taxId) {
        List<Map<String, AttributeValue>> storedRequests = findNationalRegistriesRequestsByCorrelationId(correlationId);
        assertEquals(1, storedRequests.size());
        Map<String, AttributeValue> storedRequest = storedRequests.getFirst();
        assertEquals(taxId, storedRequest.get("taxId").s());
        assertEquals("PF", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals("INIPEC", storedRequest.get("registry").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INIPEC#NOT_WORKED", storedRequest.get("registryStatus").s());
        assertNull(storedRequest.get("batchId"));
    }

    private String startInipecBatch(String pollingId, String... correlationIds) {
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestJob.batchRequestScheduled();
        String batchId = null;
        for (String correlationId : correlationIds) {
            Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
            assertEquals("WORKING", request.get("status").s());
            if (batchId == null) {
                batchId = request.get("batchId").s();
            } else {
                assertEquals(batchId, request.get("batchId").s());
            }
        }
        assertNotNull(batchId);
        pollingJob.polling();
        assertEquals("WORKING", findBatch(batchId).get("status").s());
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

    private GatewayResponse finalPayload(String correlationId) {
        Map<String, AttributeValue> request = requestByRegistry(correlationId, "INIPEC");
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("resultPayload"));
        return readNewOutputMessage(request.get("resultPayload").s());
    }

    private Map<String, AttributeValue> requestByRegistry(String correlationId, String registry) {
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

    private IniPecPollingResponse inipecPollingResponse(Pec... pecs) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pecs));
        return response;
    }

    private Pec pecResponseWithProfessionals(String taxId, String pecImpresa, String... professionalPecs) {
        Map<String, Object> value = new HashMap<>();
        value.put("cf", taxId);
        if (pecImpresa != null) {
            value.put("pecImpresa", pecImpresa);
        }
        value.put("pecProfessionista", List.of(professionalPecs).stream().map(pec -> Map.of("pec", pec)).toList());
        return objectMapper.convertValue(value, Pec.class);
    }
}
