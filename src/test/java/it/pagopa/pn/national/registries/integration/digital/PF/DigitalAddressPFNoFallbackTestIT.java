package it.pagopa.pn.national.registries.integration.digital.PF;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.integration.AbstractNationalRegistriesComponentIT;
import it.pagopa.pn.national.registries.integration.PnNationalRegistriesTestApplication;
import it.pagopa.pn.national.registries.model.GatewayResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali PF DIGITAL
 *
 * Configurazione:
 *  - fallback PF disattivo
 *  - INAD massivo disattivato
 *  - old IniPEC disattivo
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=false",
        "pn.national-registries.enable-inad-massive-api=false",
        "spring.task.scheduling.enabled=false"
})
public class DigitalAddressPFNoFallbackTestIT extends AbstractNationalRegistriesComponentIT {

    /**
     * Inad pec personale valida
     */
    @Test
    void PF_Digital_INAD() {
        String correlationId = "pf-digital-found-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, "pf@pec.it", null)));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(), payload.getPayload().getAddressType());
        assertNotNull(payload.getPayload().getDigitalAddress());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("pf@pec.it", payload.getPayload().getDigitalAddress().get(0).getAddress());
        assertEquals("PERSONALE", payload.getPayload().getDigitalAddress().get(0).getRecipient());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);
    }

    /**
     * Inad 404 NotFound
     */
    @Test
    void PF_Digital_INAD_NotFound_NoFallback() {
        String correlationId = "pf-digital-not-found-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(inadNotFoundException()));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);
    }

    /**
     * Inad 200 con lista vuota
     */
    @Test
    void PF_Digital_INAD_EmptyList_fallbackDisabled() {
        String correlationId = "pf-digital-empty-list-disabled-" + UUID.randomUUID();

        ResponseRequestDigitalAddress emptyResponse = new ResponseRequestDigitalAddress();
        emptyResponse.setCodiceFiscale(VALID_PF_CF);
        emptyResponse.setSince(Date.from(Instant.parse("2026-10-10T10:15:30Z")));
        emptyResponse.setDigitalAddress(List.of());

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.just(emptyResponse));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);
    }

    /**
     * Inad pec personale scaduta
     */
    @Test
    void PF_Digital_INAD_PersonalExpired_fallbackDisabled() {
        String correlationId = "pf-digital-expired-personal-" + UUID.randomUUID();

        ElementDigitalAddress expiredPersonal = inadAddress("expired-personal@pec.it", null);
        expiredPersonal.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        expiredPersonal
                ))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);
    }

    /**
     * Inad pec personale senza usageInfo
     */
    @Test
    void PF_Digital_INAD_PersonalWithoutUsageInfo_IsValid() {
        String correlationId = "pf-digital-personal-no-usage-info-" + UUID.randomUUID();
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
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);

    }

    /**
     * Inad pec personale senza data fine validità
     */
    @Test
    void PF_Digital_INAD_PersonalWithoutEndValidity_IsValid() {
        String correlationId = "pf-digital-personal-no-end-validity-" + UUID.randomUUID();
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
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);

    }

    /**
     * Verifica che con più PEC personali venga ignorata quella scaduta
     * e selezionata la prima PEC valida disponibile (da linee guida è possbile avere solo una pec personale valida censita).
     */
    @Test
    void PF_Digital_INAD_FirstPersonalInvalid_SecondPersonalValid_SecondIsSelected() {
        String correlationId = "pf-digital-first-personal-invalid-" + UUID.randomUUID();
        ElementDigitalAddress elementDigitalAddress = inadAddress("first-expired@pec.it", null);
        elementDigitalAddress.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        elementDigitalAddress,
                        inadAddress("second-valid@pec.it", null)
                ))));

        AddressOKDto response = enqueueGatewayRequest(
                "PF",
                VALID_PF_CF,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        GatewayResponse payload = readNewOutputMessage(outputMessage.body());

        assertEquals("INAD", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals("second-valid@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);

    }

    /**
     * Inad Personale presente, Inad professionale presente
     */
    @Test
    void PF_Digital_INAD_PersonalInsteadOfProfessional_fallbackDisabled() {
        String correlationId = "pf-digital-personal-preferred-" + UUID.randomUUID();
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
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);

    }


    /**
     * Inad Personale non presente, Inad professionale Presente
     */
    @Test
    void PF_Digital_INAD_PersonalNotFound_ProfessionalFound_fallbackDisabled() {
        String correlationId = "pf-digital-professional-only-disabled-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
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
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);
    }

    /**
     * Inad Personale non valida, Inad professionale presente e valida
     */
    @Test
    void PF_Digital_INAD_PersonalInvalid_ProfessionalValid_fallbackDisable() {
        String correlationId = "pf-digital-valid-pec-" + UUID.randomUUID();
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("2026-01-01", null),
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
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);
    }

    /**
     * Inad Personale non presente, presenti 2 pec professionali valide
     */
    @Test
    void PF_Digital_INAD_MultipleProfessionals_fallbackDisabled() {
        String correlationId = "pf-digital-multiple-prof-disabled-" + UUID.randomUUID();
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
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);
    }

    /**
     * Inad errore tecnico (eccezione diversa da 404 CF NotFound)
     */
    @Test
    void PF_Digital_INAD_Error() {
        String correlationId = "pf-digital-technical-disabled-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("INAD unavailable");
        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId))).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertEquals(correlationId, response.getCorrelationId());
        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);
    }

    /**
     * Inad pec personale scaduta, pec professionale valida
     */
    @Test
    void PF_Digital_INAD_PersonalExpired_ProfessionalValid_fallbackDisabled() {
        String correlationId = "pf-digital-expired-personal-professional-" + UUID.randomUUID();

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
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);
    }

    /**
     * Inad personale non presente, Inad professionale scaduta
     */
    @Test
    void PF_Digital_INAD_OnlyProfessionalExpired_NoFallback() {
        String correlationId = "pf-digital-only-professional-expired-" + UUID.randomUUID();
        ElementDigitalAddress expiredProfessional = inadAddress("expired-professional@pec.it", "AVVOCATO");
        expiredProfessional.getUsageInfo().setDateEndValidity(Date.from(Instant.now().minusSeconds(3600)));

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(expiredProfessional))));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);
        GatewayResponse payload = readNewOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getPayload().getRegistry());
        assertTrue(payload.getPayload().getDigitalAddress().isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verifyNoInteractions(ipaClient, infoCamereClient, anprClient);
    }

}