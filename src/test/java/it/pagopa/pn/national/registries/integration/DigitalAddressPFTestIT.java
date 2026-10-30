package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.national.registries.converter.GatewayConverter.convertToGetDigitalAddressInadRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali: Copre il flusso digitale PF sincrono su INAD senza fallback, con casi di successo, assenza indirizzo, professionisti, validità PEC, liste vuote ed errori.
 *
 * <p>Configurazione specifica della classe:</p>
 * <ul>
 *   <li>{@code pn.national-registries.enable-pf-pec-fallback-flow=false}</li>
 *   <li>{@code pn.national-registries.enable-inad-massive-api=false}</li>
 *   <li>{@code spring.task.scheduling.enabled=false}</li>
 * </ul>
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=false",
        "pn.national-registries.enable-inad-massive-api=false",
        "spring.task.scheduling.enabled=false"
})
public class DigitalAddressPFTestIT extends AbstractNationalRegistriesComponentIT {

    /**
     * Verifica che la stessa risposta INAD produca lo stesso domicilio digitale tramite endpoint diretto e tramite Gateway PF.
     */
    @Test
    void Inad_DirectEndpointAndGateway_ReturnSameFunctionalAddress() {
        String directCorrelation = "inad-direct-" + UUID.randomUUID();
        String gatewayCorrelation = "inad-gateway-" + UUID.randomUUID();
        ResponseRequestDigitalAddress providerResponse = inadResponse(VALID_PF_CF, "same-inad@pec.it", null);
        when(inadClient.callEService(eq(VALID_PF_CF), anyString())).thenReturn(Mono.just(providerResponse));

        GetDigitalAddressINADRequestBodyDto directRequest = convertToGetDigitalAddressInadRequest(directCorrelation, VALID_PF_CF);
        GetDigitalAddressINADOKDto direct = webTestClient.post()
                .uri("/national-registries-private/PF/inad/digital-address")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(directRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GetDigitalAddressINADOKDto.class)
                .returnResult().getResponseBody();
        assertNotNull(direct);

        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, gatewayCorrelation);
        processNextGatewayInputMessage();
        Message output = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(output);
        CodeSqsDto gateway = readOutputMessage(output.body());

        assertEquals(direct.getDigitalAddress().getDigitalAddress(), gateway.getDigitalAddress().getFirst().getAddress());
        assertEquals("INAD", gateway.getRegistry());
    }

    /**
            * Verifica che un JSON malformato venga rifiutato prima della presa in carico asincrona.
     */
    @Test
    void Gateway_MalformedJson_ReturnsBadRequest() {
        webTestClient.post()
                .uri("/national-registries-private/PF/addresses")
                .header("pn-national-registries-cx-id", CX_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"filter\": {\"taxId\": \"" + VALID_PF_CF + "\",")
                .exchange()
                .expectStatus().isBadRequest();
    }

    /**
     * Verifica che una richiesta senza clientId non venga accettata quando la validazione del client è attiva.
     */
    @Test
    void Gateway_MissingClientId_ReturnsBadRequest() {
        webTestClient.post()
                .uri("/national-registries-private/PF/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildGatewayRequest("missing-client-" + UUID.randomUUID(), VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL))
                .exchange()
                .expectStatus().isBadRequest();
    }

    /**
     * Verifica che l'assenza del taxId obbligatorio venga rifiutata a livello HTTP e non produca una presa in carico valida.
     */
    @Test
    void Gateway_MissingTaxId_ReturnsBadRequest() {
        String correlationId = "missing-taxid-" + UUID.randomUUID();
        var request = buildGatewayRequest(correlationId, VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        request.getFilter().setTaxId(null);

        webTestClient.post()
                .uri("/national-registries-private/PF/addresses")
                .header("pn-national-registries-cx-id", CX_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    /**
     * Verifica il caso base di recupero dell’indirizzo digitale PF tramite INAD.
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(), payload.getAddressType());
        assertNotNull(payload.getDigitalAddress());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("pf@pec.it", payload.getDigitalAddress().get(0).getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().get(0).getRecipient());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica che un not found INAD produca risultato vuoto quando il fallback PF è disabilitato.
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica il comportamento quando manca la PEC personale ma è presente una PEC professionale, con fallback disabilitato.
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica la selezione della PEC personale rispetto a quella professionale con fallback disabilitato.
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("personal@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
    }

    /**
     * Verifica il comportamento con PEC personale invalida e professionale valida quando il fallback è disabilitato.
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica la gestione di più PEC professionali INAD con fallback disabilitato.
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica il risultato funzionale quando INAD restituisce una lista vuota e il fallback è disabilitato.
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica la propagazione/mappatura funzionale di un errore tecnico INAD nel flusso PF.
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
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica la gestione di PEC personale scaduta e PEC professionale valida con fallback disabilitato.
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica che una PEC personale scaduta non venga usata quando il fallback è disabilitato.
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("INAD", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(inadClient).callEService(VALID_PF_CF, correlationId);
        verifyNoInteractions(ipaClient, anprClient);
    }

    /**
     * Verifica che una PEC personale senza usageInfo sia considerata valida.
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
        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("personal-no-usage@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Verifica che una PEC personale senza data di fine validità sia considerata valida.
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
        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("personal-no-end@pec.it", payload.getDigitalAddress().getFirst().getAddress());
        assertEquals("PERSONALE", payload.getDigitalAddress().getFirst().getRecipient());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Verifica il comportamento corrente con più PEC personali: viene selezionata la prima anche se invalida.
     */
    /**
     * Verifica che con più PEC personali venga ignorata quella invalida
     * e selezionata la prima PEC valida disponibile.
     */
    @Test
    void PF_Digital_INAD_FirstPersonalInvalid_SecondPersonalValid_SecondIsSelected() {
        String correlationId = "pf-digital-first-personal-invalid-" + UUID.randomUUID();

        when(inadClient.callEService(eq(VALID_PF_CF), eq(correlationId)))
                .thenReturn(Mono.just(inadResponse(VALID_PF_CF, List.of(
                        inadAddress("invalid-personal", null),
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

        CodeSqsDto payload = readOutputMessage(outputMessage.body());

        assertEquals("INAD", payload.getRegistry());
        assertEquals(1, payload.getDigitalAddress().size());
        assertEquals("second-valid@pec.it", payload.getDigitalAddress().getFirst().getAddress());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

    /**
     * Verifica il risultato quando l’unica PEC professionale disponibile è scaduta e non è previsto fallback.
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
        CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals("INAD", payload.getRegistry());
        assertTrue(payload.getDigitalAddress().isEmpty());
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
    }

}