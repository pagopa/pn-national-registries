package it.pagopa.pn.national.registries.integration.digital.PF;

import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.integration.AbstractNationalRegistriesComponentIT;
import it.pagopa.pn.national.registries.integration.PnNationalRegistriesTestApplication;
import it.pagopa.pn.national.registries.model.GatewayResponse;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.service.IniPecBatchSqsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali PF DIGITAL.
 *
 * Configurazione:
 *  - fallback PF attivo
 *  - INAD massivo disattivato
 *  - old IniPEC attivo
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.enable-old-inipec-flow=true",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "pn.national-registries.inipec.batch-request-max-retry=1",
        "pn.national-registries.inipec.batch.polling.recovery.after=0",
        "pn.national-registries.inipec.batch-polling-max-retry=2",
        "pn.national-registries.inipec.batch-polling-in-progress-max-retry=2",
        "spring.task.scheduling.enabled=false",
        "pn.national-registries.inipec.batch-request-recovery-after=0"
})
public class DigitalAddressPFFallbackOldInipecTestIT extends AbstractNationalRegistriesComponentIT {

    private static final String PF_PROFESSIONAL = "WDSTKB60E09L538U";
    private static final String PF_BUSINESS_PROFESSIONAL = "PRPMGG80A41H501W";
    private static final String PF_INVALID = "FLPCPT69A65Z336P";
    private static final String PF_ND = "DLGMRA73D41F839C";
    private static final String PF_NF = "PRMRGG74T16H501R";
    private static final String PF_MISSING = "VDADGI80A01F839E";
    private static final String PF_ER = "RSSSRG80A01F839I";

    @Autowired
    private IniPecBatchSqsService iniPecBatchSqsService;

    @MockitoSpyBean
    private IniPecBatchRequestRepository iniPecBatchRequestRepository;

    /**
     * Inad pec personale valida
     */
    @Test
    void PF_Digital_INAD_PersonalFound_NoInipecFallback() {
        String correlationId = "pf-old-personal-found-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, "personal@pec.it", null)));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("personal@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Inad 404 NotFound
     */
    @Test
    void PF_Digital_INAD_NotFound_OldInipecFallback() {
        String correlationId = "pf-old-inad-not-found-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, VALID_PF_CF);
        GatewayResponse payload = completeLegacyFinal(correlationId, VALID_PF_CF, pecResponse(VALID_PF_CF, "inipec-not-found-fallback@pec.it"));
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("inipec-not-found-fallback@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verify(ipaClient, never()).callEServiceWS23(any(), any());
        verifyNoInteractions(anprClient);
    }

    /**
     * Inad 200 con lista vuota
     */
    @Test
    void PF_Digital_INAD_EmptyList_FallbackEnabled() {
        String correlationId = "pf-old-empty-list-" + UUID.randomUUID();
        ResponseRequestDigitalAddress emptyResponse = new ResponseRequestDigitalAddress();
        emptyResponse.setCodiceFiscale(VALID_PF_CF);
        emptyResponse.setSince(Date.from(Instant.parse("2026-10-10T10:15:30Z")));
        emptyResponse.setDigitalAddress(List.of());
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(emptyResponse));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, VALID_PF_CF);
        GatewayResponse payload = completeLegacyFinal(correlationId, VALID_PF_CF, pecResponse(VALID_PF_CF, "inipec-after-empty-list@pec.it"));
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals("inipec-after-empty-list@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Inad pec personale scaduta
     */
    @Test
    void shouldFallbackToInipecWhenPersonalExpired() {
        String correlationId = "pf-old-expired-personal-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(expiredPersonal))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, VALID_PF_CF);
        GatewayResponse payload = completeLegacyFinal(correlationId, VALID_PF_CF, pecResponse(VALID_PF_CF, "inipec-after-expired@pec.it"));
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals("inipec-after-expired@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Inad pec personale senza usageInfo
     */
    @Test
    void PF_Digital_INAD_PersonalWithoutUsageInfo_IsValid() {
        String correlationId = "pf-old-personal-no-usage-" + UUID.randomUUID();
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
        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("personal-no-usage@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Inad pec personale senza data fine validità
     */
    @Test
    void PF_Digital_INAD_PersonalWithoutEndValidity_IsValid() {
        String correlationId = "pf-old-personal-no-end-" + UUID.randomUUID();
        ElementDigitalAddress personal = inadAddress("personal-no-end@pec.it", null);
        personal.getUsageInfo().setDateEndValidity(null);
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(personal))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("personal-no-end@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Prima PEC personale scaduta e seconda PEC personale valida
     */
    @Test
    void PF_Digital_INAD_FirstPersonalInvalid_SecondPersonalValid_SecondIsSelected() {
        String correlationId = "pf-old-first-personal-invalid-" + UUID.randomUUID();
        ElementDigitalAddress expired = inadAddress("first-expired@pec.it", null);
        expired.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(expired, inadAddress("second-valid@pec.it", null)))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("second-valid@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Inad personale presente e Inad professionale presente
     */
    @Test
    void PF_Digital_INAD_PersonalInsteadOfProfessional_FallbackEnabled() {
        String correlationId = "pf-old-personal-preferred-" + UUID.randomUUID();
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
        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals("personal@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Inad personale non presente e Inad professionale presente
     */
    @Test
    void PF_Digital_INAD_PersonalNotFound_ProfessionalFound_FallbackEnabled() {
        String correlationId = "pf-old-professional-only-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(inadAddress("professional@pec.it", "INGEGNERE")))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Inad personale non valida e Inad professionale valida
     */
    @Test
    void PF_Digital_INAD_PersonalInvalid_ProfessionalValid_FallbackEnabled() {
        String correlationId = "pf-old-invalid-personal-professional-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("invalid-personal", null),
                        inadAddress("professional@pec.it", "INGEGNERE")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Inad personale non presente e due PEC professionali valide
     */
    @Test
    void PF_Digital_INAD_MultipleProfessionals_FallbackEnabled() {
        String correlationId = "pf-old-multiple-professionals-" + UUID.randomUUID();
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
        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("prof1@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Inad errore tecnico diverso da NotFound
     */
    @Test
    void PF_Digital_INAD_TechnicalError_NoInipecFallback() {
        String correlationId = "pf-old-technical-error-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("INAD unavailable");
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        assertEquals(correlationId, response.getCorrelationId());
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(infoCamereClient, ipaClient, anprClient);
    }

    /**
     * Inad pec personale scaduta e pec professionale valida
     */
    @Test
    void shouldTakeProfessionalWhenPersonalExpired() {
        String correlationId = "pf-old-expired-personal-professional-" + UUID.randomUUID();
        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        expiredPersonal,
                        inadAddress("professional@pec.it", "INGEGNERE")
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertTrue(findBatchRequestsByCorrelationPrefix(correlationId).isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Inad sola professionale scaduta
     */
    @Test
    void PF_Digital_INAD_OnlyProfessionalExpired_FallbackEnabled() {
        String correlationId = "pf-old-only-professional-expired-" + UUID.randomUUID();
        ElementDigitalAddress expiredProfessional = inadAddress("expired-professional@pec.it", "AVVOCATO");
        expiredProfessional.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(expiredProfessional))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, VALID_PF_CF);
        GatewayResponse payload = completeLegacyFinal(correlationId, VALID_PF_CF, pecResponse(VALID_PF_CF, "inipec-after-expired-professional@pec.it"));
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals("inipec-after-expired-professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * INAD NotFound -> old IniPEC con PEC professionale valida.
     */
    @Test
    void PF_Digital_FullFlow_InipecProfessionalPecIsReturned() {
        String correlationId = "pf-old-inipec-professional-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_PROFESSIONAL), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_PROFESSIONAL, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, PF_PROFESSIONAL);
        GatewayResponse payload = completeLegacyFinal(correlationId, PF_PROFESSIONAL,
                pecResponseWithProfessionals(PF_PROFESSIONAL, null, "professional@pec.it"));
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    /**
     * INAD NotFound -> old IniPEC con PEC impresa e tutte le PEC professionali valide.
     */
    @Test
    void PF_Digital_FullFlow_InipecBusinessAndAllProfessionalPecsAreReturned() {
        String correlationId = "pf-old-inipec-business-professionals-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_BUSINESS_PROFESSIONAL), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_BUSINESS_PROFESSIONAL, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, PF_BUSINESS_PROFESSIONAL);
        GatewayResponse payload = completeLegacyFinal(correlationId, PF_BUSINESS_PROFESSIONAL,
                pecResponseWithProfessionals(PF_BUSINESS_PROFESSIONAL, "business@pec.it", "professional-one@pec.it", "professional-two@pec.it"));
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(3, payload.getPayload().getDigitalAddress().size());
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "business@pec.it".equals(address.getAddress()) && "IMPRESA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-one@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
        assertTrue(payload.getPayload().getDigitalAddress().stream().anyMatch(address -> "professional-two@pec.it".equals(address.getAddress()) && "PROFESSIONISTA".equals(address.getRecipient())));
    }

    /**
     * INAD NotFound -> old IniPEC con PEC non valida.
     */
    @Test
    void PF_Digital_FullFlow_InipecInvalidPecProducesEmptyFinalResult() {
        String correlationId = "pf-old-inipec-invalid-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_INVALID), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_INVALID, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, PF_INVALID);
        GatewayResponse payload = completeLegacyFinal(correlationId, PF_INVALID, pecResponse(PF_INVALID, "not-an-email"));
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> old IniPEC con stato ND.
     */
    @Test
    void PF_Digital_FullFlow_InipecNdProducesEmptyFinalResult() {
        String correlationId = "pf-old-inipec-nd-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_ND), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_ND, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, PF_ND);
        Pec nd = pecResponse(PF_ND, "ignored-nd@pec.it");
        nd.setStatoImpresa(Pec.StatoImpresaEnum.ND);
        GatewayResponse payload = completeLegacyFinal(correlationId, PF_ND, nd);
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> old IniPEC con stato NF.
     */
    @Test
    void PF_Digital_FullFlow_InipecNfProducesEmptyFinalResult() {
        String correlationId = "pf-old-inipec-nf-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_NF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_NF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, PF_NF);
        Pec nf = pecResponse(PF_NF, "ignored-nf@pec.it");
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);
        GatewayResponse payload = completeLegacyFinal(correlationId, PF_NF, nf);
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> old IniPEC quando la risposta non contiene il codice fiscale richiesto.
     */
    @Test
    void PF_Digital_FullFlow_InipecMissingTaxIdProducesEmptyFinalResult() {
        String correlationId = "pf-old-inipec-missing-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_MISSING), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_MISSING, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, PF_MISSING);
        GatewayResponse payload = completeLegacyFinal(correlationId, PF_MISSING);
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
    }

    /**
     * INAD NotFound -> old IniPEC con stato ER fino al limite configurato.
     */
    @Test
    void PF_Digital_FullFlow_InipecErRetriesThenEndsInErrorAtConfiguredLimit() {
        String correlationId = "pf-old-inipec-er-" + UUID.randomUUID();
        when(inadClient.callEService(eq(PF_ER), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", PF_ER, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, PF_ER);
        String pollingId = "pf-old-er-polling-" + UUID.randomUUID();
        submitLegacyBatch(correlationId, pollingId);
        Pec er = pecResponse(PF_ER, "ignored@pec.it");
        er.setStatoImpresa(Pec.StatoImpresaEnum.ER);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(er)));
        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertEquals("1", request.get("retry").n());
        assertNull(request.get("message"));
    }

    /**
     * INAD NotFound -> old IniPEC con polling in-progress.
     */
    @Test
    void PF_Digital_FullFlow_InipecPollingInProgressSchedulesRetry() {
        String correlationId = "pf-old-inipec-in-progress-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, VALID_PF_CF);
        String pollingId = "pf-old-in-progress-polling-" + UUID.randomUUID();
        String batchId = submitLegacyBatch(correlationId, pollingId);
        PnNationalRegistriesException inProgress = new PnNationalRegistriesException(
                "List PEC in progress", HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(inProgress));
        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        assertEquals("WORKING", polling.get("status").s());
        assertEquals("1", polling.get("inProgressRetry").n());
        assertEquals("WORKING", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    /**
     * INAD NotFound -> old IniPEC quando il polling restituisce metadata di errore.
     */
    @Test
    void PF_Digital_FullFlow_InipecErrorMetadataSchedulesRetry() {
        String correlationId = "pf-old-inipec-metadata-error-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertLegacyPendingRequest(correlationId, VALID_PF_CF);
        String pollingId = "pf-old-metadata-polling-" + UUID.randomUUID();
        String batchId = submitLegacyBatch(correlationId, pollingId);
        IniPecPollingResponse pollingResponse = new IniPecPollingResponse();
        pollingResponse.setCode("123");
        pollingResponse.setDescription("temporary error");
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(pollingResponse));
        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        assertEquals("WORKING", polling.get("status").s());
        assertEquals("1", polling.get("retry").n());
        assertEquals("WORKING", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    /**
     * Legacy specifico: errore di submit al limite dei retry.
     */
    @Test
    void LegacyInipec_SubmitErrorExhaustsRequestRetryAndMarksError() {
        String correlationId = "legacy-submit-error-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class)))
                .thenReturn(Mono.error(new IllegalStateException("provider unavailable")));

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertEquals("1", request.get("retry").n());
        assertNotEquals("NO_BATCH_ID", request.get("batchId").s());
    }

    /**
     * Legacy specifico: bad request in submit.
     */
    @Test
    void LegacyInipec_SubmitBadRequestMarksRequestErrorImmediately() {
        String correlationId = "legacy-submit-bad-request-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        PnNationalRegistriesException badRequest = new PnNationalRegistriesException(
                "bad request", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.error(badRequest));

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertEquals("1", request.get("retry").n());
    }

    /**
     * Legacy specifico: esaurimento retry per errore polling.
     */
    @Test
    void LegacyInipec_PollingErrorExhaustsRetryAndMarksPollingAndRequestError() {
        String correlationId = "legacy-polling-exhausted-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        String batchId = submitLegacyBatchFromGateway(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(new IllegalStateException("polling unavailable")));

        digitalAddressBatchPollingService.batchPecPolling();
        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        assertEquals("WORKING", polling.get("status").s());
        assertEquals("1", polling.get("retry").n());

        recoverLegacyPolling(batchId);
        digitalAddressBatchPollingService.batchPecPolling();

        polling = findLegacyPolling(batchId);
        assertEquals("ERROR", polling.get("status").s());
        assertEquals("2", polling.get("retry").n());
        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("ERROR", request.get("status").s());
        assertNotNull(request.get("message"));
    }

    /**
     * Legacy specifico: esaurimento retry in-progress.
     */
    @Test
    void LegacyInipec_PollingInProgressExhaustsRetryAndMarksPollingAndRequestError() {
        String correlationId = "legacy-in-progress-exhausted-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        String batchId = submitLegacyBatchFromGateway(correlationId, pollingId);
        PnNationalRegistriesException inProgress = new PnNationalRegistriesException(
                "List PEC in progress", HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(inProgress));

        digitalAddressBatchPollingService.batchPecPolling();
        Map<String, AttributeValue> polling = findLegacyPolling(batchId);
        assertEquals("WORKING", polling.get("status").s());
        assertEquals("1", polling.get("inProgressRetry").n());

        recoverLegacyPolling(batchId);
        digitalAddressBatchPollingService.batchPecPolling();

        polling = findLegacyPolling(batchId);
        assertEquals("ERROR", polling.get("status").s());
        assertEquals("2", polling.get("inProgressRetry").n());
        assertEquals("ERROR", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    /**
     * Legacy specifico: bad request durante il polling.
     */
    @Test
    void LegacyInipec_PollingBadRequestMarksPollingAndRequestErrorImmediately() {
        String correlationId = "legacy-polling-bad-request-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        String batchId = submitLegacyBatchFromGateway(correlationId, pollingId);
        PnNationalRegistriesException badRequest = new PnNationalRegistriesException(
                "bad request", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null, null, StandardCharsets.UTF_8, null);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.error(badRequest));

        digitalAddressBatchPollingService.batchPecPolling();

        assertEquals("ERROR", findLegacyPolling(batchId).get("status").s());
        assertEquals("ERROR", findBatchRequestsByCorrelationPrefix(correlationId).getFirst().get("status").s());
    }

    /**
     * Legacy specifico: output consumato e sendStatus aggiornato a SENT.
     */
    @Test
    void LegacyInipec_OutputIsReallyConsumedAndSendStatusBecomesSent() {
        String correlationId = "legacy-output-sent-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();
        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(Mono.just(iniPecPollingResponse(pecResponse(VALID_PF_CF, "legacy-output@pec.it"))));
        digitalAddressBatchPollingService.batchPecPolling();

        Message output = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(output);
        GatewayResponse payload = readNewOutputMessage(output.body());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals("legacy-output@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", request.get("status").s());
        assertEquals("SENT", request.get("sendStatus").s());
    }

    /**
     * Legacy specifico: recovery dopo fallimento della persistenza sendStatus=SENT.
     */
    @Test
    void LegacyInipec_OutputSentButSendStatusPersistenceFails_RecoveryCompletesDeliveryState() {
        String correlationId = "legacy-output-recovery-" + UUID.randomUUID();
        String pollingId = "legacy-polling-" + UUID.randomUUID();
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();

        AtomicBoolean failSentUpdateOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            BatchRequest request = invocation.getArgument(0);
            if (BatchSendStatus.SENT.getValue().equals(request.getSendStatus()) && failSentUpdateOnce.getAndSet(false)) {
                return Mono.error(new IllegalStateException("simulated sendStatus persistence failure"));
            }
            return invocation.callRealMethod();
        }).when(iniPecBatchRequestRepository).update(any(BatchRequest.class));

        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(Mono.just(iniPecPollingResponse(pecResponse(VALID_PF_CF, "legacy-recovery@pec.it"))));
        digitalAddressBatchPollingService.batchPecPolling();

        Message firstOutput = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(firstOutput);
        GatewayResponse firstPayload = readNewOutputMessage(firstOutput.body());
        assertEquals(correlationId, firstPayload.getPayload().getCorrelationId());
        assertEquals("INIPEC", firstPayload.getPayload().getRegistry());
        assertEquals("legacy-recovery@pec.it", firstPayload.getPayload().getDigitalAddress().getFirst().getAddress());

        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKED", request.get("status").s());
        assertEquals("NOT_SENT", request.get("sendStatus").s());

        iniPecBatchSqsService.recoveryBatchSendToSqs();
        request = awaitSendStatus(correlationId, "SENT");
        assertEquals("SENT", request.get("sendStatus").s());

        Message recoveredOutput = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(recoveredOutput);
        GatewayResponse recoveredPayload = readNewOutputMessage(recoveredOutput.body());
        assertEquals(correlationId, recoveredPayload.getPayload().getCorrelationId());
        assertEquals("INIPEC", recoveredPayload.getPayload().getRegistry());
        assertEquals("legacy-recovery@pec.it", recoveredPayload.getPayload().getDigitalAddress().getFirst().getAddress());
    }

    /**
     * INAD NotFound -> legacy IniPEC con più PEC professionali valide e nessuna PEC impresa.
     */
    @Test
    void PF_Digital_FullFlow_OldInipec_AllProfessionalPecsArePreserved() {
        String correlationId = "pf-old-inipec-professionals-" + UUID.randomUUID();
        String pollingId = "old-inipec-polling-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest(
                "PF",
                VALID_PF_CF,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));

        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class)))
                .thenReturn(Mono.just(iniPecBatchResponse(pollingId)));

        iniPecBatchRequestService.batchPecRequest();

        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(Mono.just(iniPecPollingResponse(
                        pecResponseWithProfessionals(
                                VALID_PF_CF,
                                null,
                                "professional-one@pec.it",
                                "professional-two@pec.it"
                        )
                )));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> request =
                findBatchRequestsByCorrelationPrefix(correlationId).getFirst();

        assertEquals("WORKED", request.get("status").s());

        GatewayResponse payload = readNewOutputMessage(request.get("message").s());

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
     * INAD NotFound -> legacy IniPEC con PEC impresa invalida e PEC professionale valida.
     */
    @Test
    void PF_Digital_FullFlow_OldInipec_InvalidBusinessPecKeepsValidProfessionalPec() {
        String correlationId = "pf-old-inipec-invalid-business-" + UUID.randomUUID();
        String pollingId = "old-inipec-polling-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest(
                "PF",
                VALID_PF_CF,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));

        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class)))
                .thenReturn(Mono.just(iniPecBatchResponse(pollingId)));

        iniPecBatchRequestService.batchPecRequest();

        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(Mono.just(iniPecPollingResponse(
                        pecResponseWithProfessionals(
                                VALID_PF_CF,
                                "not-an-email",
                                "professional@pec.it"
                        )
                )));

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> request =
                findBatchRequestsByCorrelationPrefix(correlationId).getFirst();

        assertEquals("WORKED", request.get("status").s());

        GatewayResponse payload = readNewOutputMessage(request.get("message").s());

        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());
        assertEquals("PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient());
    }

    private void assertLegacyPendingRequest(String correlationId, String taxId) {
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        List<Map<String, AttributeValue>> legacyRequests = findBatchRequestsByCorrelationPrefix(correlationId);
        assertEquals(1, legacyRequests.size());
        Map<String, AttributeValue> request = legacyRequests.getFirst();
        assertTrue(request.get("correlationId").s().startsWith(correlationId + "~"));
        assertEquals(taxId, request.get("cf").s());
        assertEquals("PF", request.get("recipientType").s());
        assertEquals("NOT_WORKED", request.get("status").s());
    }

    private GatewayResponse completeLegacyFinal(String correlationId, String taxId, Pec... pecs) {
        String pollingId = "legacy-full-flow-" + UUID.randomUUID();
        submitLegacyBatch(correlationId, pollingId);
        when(infoCamereClient.callEServiceRequestPec(pollingId)).thenReturn(Mono.just(iniPecPollingResponse(pecs)));
        digitalAddressBatchPollingService.batchPecPolling();
        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals(taxId, request.get("cf").s());
        assertEquals("WORKED", request.get("status").s());
        assertNotNull(request.get("message"));
        return readNewOutputMessage(request.get("message").s());
    }

    private String submitLegacyBatch(String correlationId, String pollingId) {
        when(infoCamereClient.callEServiceRequestId(any(IniPecBatchRequest.class))).thenReturn(Mono.just(iniPecBatchResponse(pollingId)));
        iniPecBatchRequestService.batchPecRequest();
        Map<String, AttributeValue> request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
        assertEquals("WORKING", request.get("status").s());
        return request.get("batchId").s();
    }

    private String submitLegacyBatchFromGateway(String correlationId, String pollingId) {
        startLegacyInipecFromGateway(correlationId, VALID_PF_CF);
        return submitLegacyBatch(correlationId, pollingId);
    }

    private void recoverLegacyPolling(String batchId) {
        digitalAddressBatchPollingService.recoveryBatchPolling();
        for (int i = 0; i < 50; i++) {
            if ("NOT_WORKED".equals(findLegacyPolling(batchId).get("status").s())) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }
        fail("Legacy polling not recovered for batchId " + batchId);
    }

    private Map<String, AttributeValue> findLegacyPolling(String batchId) {
        return dynamoDbClient.scan(ScanRequest.builder()
                        .tableName(BATCH_POLLING_TABLE)
                        .filterExpression("batchId = :batchId")
                        .expressionAttributeValues(Map.of(":batchId", AttributeValue.builder().s(batchId).build()))
                        .build())
                .items()
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private Map<String, AttributeValue> awaitSendStatus(String correlationId, String expected) {
        Map<String, AttributeValue> request = null;
        for (int i = 0; i < 100; i++) {
            request = findBatchRequestsByCorrelationPrefix(correlationId).getFirst();
            if (request.get("sendStatus") != null && expected.equals(request.get("sendStatus").s())) {
                return request;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }
        fail("sendStatus non aggiornato a " + expected + " per correlationId " + correlationId);
        return request;
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
