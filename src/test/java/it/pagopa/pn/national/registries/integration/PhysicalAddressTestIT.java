package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RichiestaE002;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RispostaE002OK;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoDatiSoggettiEnte;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoListaSoggetti;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoResidenza;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.AddressRegistroImprese;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import static it.pagopa.pn.national.registries.converter.GatewayConverter.convertToGetAddressAnprRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali PHYSICAL con configurazione standard per recipient PF e PG:
 * <ul>
 *   <li>PF/ANPR: successo, not found, errori, residenze italiane/estere, più soggetti e varianti di composizione indirizzo;</li>
 *   <li>PG/Registro Imprese: successo, not found, errori, indirizzi italiani/esteri e campi opzionali;</li>
 *   <li>endpoint sincrono multi-address: richieste PF/PG miste, errori puntuali e indipendenza degli elementi;</li>
 *   <li>verifica dei campi funzionali inviati ai registri e della corretta propagazione del risultato.</li>
 * </ul>
 *
 * <p>Configurazione specifica della classe:</p>
 * <ul>
 *   <li>Nessun override locale: usa la configurazione comune definita in {@link AbstractNationalRegistriesComponentIT}.</li>
 * </ul>
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
public class PhysicalAddressTestIT extends AbstractNationalRegistriesComponentIT {

    /**
     * Verifica che la stessa risposta ANPR produca lo stesso indirizzo fisico tramite endpoint diretto e tramite Gateway PF.
     */
    @Test
    void Anpr_DirectEndpointAndGateway_ReturnSameFunctionalAddress() {
        String correlationId = "anpr-gateway-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(VALID_PF_CF, "Via", "Roma", "10", "Roma", "RM", "00100")));

        GetAddressANPRRequestBodyDto directRequest = convertToGetAddressAnprRequest(buildGatewayRequest("anpr-direct-" + UUID.randomUUID(), VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL));
        GetAddressANPROKDto direct = webTestClient.post()
                .uri("/national-registries-private/anpr/address")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(directRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GetAddressANPROKDto.class)
                .returnResult().getResponseBody();
        assertNotNull(direct);

        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();
        Message output = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(output);
        CodeSqsDto gateway = readOutputMessage(output.body());

        assertEquals(direct.getResidentialAddresses().getFirst().getAddress(), gateway.getPhysicalAddress().getAddress());
        assertEquals(direct.getResidentialAddresses().getFirst().getMunicipality(), gateway.getPhysicalAddress().getMunicipality());
        assertEquals("ANPR", gateway.getRegistry());
    }

    /**
     * Verifica il caso base di recupero dell’indirizzo fisico PF tramite ANPR.
     */
    @Test
    void PF_Physical_ANPR() {
        String correlationId = "pf-physical-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100")));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("ANPR", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(), payload.getAddressType());
        assertPhysicalAddress(payload,
                "VIA ROMA 10",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    /**
     * Verifica la composizione dell’indirizzo ANPR quando sono presenti tutti i dettagli disponibili.
     */
    @Test
    void PF_Physical_ANPR_WithAllAddressDetails() {
        String correlationId = "pf-physical-full-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA MAGGIORE", "10", "Roma", "RM", "00100",
                "Scala A", "Presso Mario Rossi", "Roma Capitale"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("ANPR", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(), payload.getAddressType());
        assertPhysicalAddress(payload,
                "VIA ROMA MAGGIORE 10",
                "Scala A",
                "Presso Mario Rossi",
                "00100",
                "Roma",
                "RM",
                null,
                "Roma Capitale");

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    /**
     * Verifica la composizione dell’indirizzo ANPR con informazioni parziali.
     */
    @Test
    void PF_Physical_ANPR_WithPartialAddressDetails() {
        String correlationId = "pf-physical-partial-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100",
                "Scala B", null, null
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA ROMA 10",
                "Scala B",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    /**
     * Verifica la corretta gestione di caratteri accentati nell’indirizzo ANPR.
     */
    @Test
    void PF_Physical_ANPR_WithAccentedCharacters() {
        String correlationId = "pf-physical-accented-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "SANT'AGATA", "22", "Napoli", "NA", "80100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA SANT'AGATA 22",
                "",
                null,
                "80100",
                "Napoli",
                "NA",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    /**
     * Verifica la gestione di un numero civico non standard nella risposta ANPR.
     */
    @Test
    void PF_Physical_ANPR_WithNonStandardCivicNumber() {
        String correlationId = "pf-physical-nonstandard-civic-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA", "22", "B", null, "1", null,
                null, null, null, null, "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA ROMA 22/B SNC",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    /**
     * Verifica la gestione di una denominazione stradale molto lunga.
     */
    @Test
    void PF_Physical_ANPR_WithVeryLongStreetName() {
        String correlationId = "pf-physical-long-street-" + UUID.randomUUID();
        String longStreetName = "MOLTO LUNGA DEL COMUNE DI ROMA CON NOME PARTICOLARMENTE LUNGO E COMPLESSO CHE CONTIENE MOLTE PAROLE";
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", longStreetName, "100", "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA 100",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    /**
     * Verifica il risultato funzionale quando ANPR non trova il soggetto richiesto.
     */
    @Test
    void PF_Physical_ANPR_NotFound() {
        String correlationId = "pf-physical-not-found-" + UUID.randomUUID();

        PnNationalRegistriesException exception = new PnNationalRegistriesException("Not Found", 404, "Not Found", HttpHeaders.EMPTY, "{\"codiceErroreAnomalia\":\"EN122\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8, AnprResponseKO.class);
        when(anprClient.callEService(any())).thenReturn(Mono.error(exception));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("ANPR", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(), payload.getAddressType());
        assertNull(payload.getPhysicalAddress());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    /**
     * Verifica la gestione di un errore tecnico ANPR nel flusso fisico PF.
     */
    @Test
    void PF_Physical_ANPR_error() {
        String correlationId = "pf-physical-technical-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("ANPR unavailable");
        when(anprClient.callEService(any())).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertEquals(correlationId, response.getCorrelationId());
        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(anprClient).callEService(any());
        verifyNoInteractions(infoCamereClient);
    }

    /**
     * Verifica il caso base di recupero dell’indirizzo fisico PG tramite Registro Imprese.
     */
    @Test
    void PG_Physical_RegistroImprese() {
        String correlationId = "pg-physical-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "Milano", "25", "Milano", "MI", "20100")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("REGISTRO_IMPRESE", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(), payload.getAddressType());
        assertPhysicalAddress(payload,
                "VIA Milano 25",
                null,
                null,
                "20100",
                "Milano",
                "MI",
                "IT",
                null);

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);
    }

    /**
     * Verifica la composizione dell’indirizzo Registro Imprese con tutti i campi disponibili.
     */
    @Test
    void PG_Physical_RegistroImprese_WithAllFields() {
        String correlationId = "pg-physical-full-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "REPUBBLICA", "100", "Milano", "MI", "20100", "IT")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA REPUBBLICA 100",
                null,
                null,
                "20100",
                "Milano",
                "MI",
                "IT",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);
    }

    /**
     * Verifica la gestione di un indirizzo estero restituito da Registro Imprese.
     */
    @Test
    void PG_Physical_RegistroImprese_WithForeignCountry() {
        String correlationId = "pg-physical-foreign-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "AVENUE", "EUROPA", "5", "Bruxelles", "BE", "1000", "BE")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "AVENUE EUROPA 5",
                null,
                null,
                "1000",
                "Bruxelles",
                "BE",
                "BE",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
    }

    /**
     * Verifica la gestione di caratteri accentati nell’indirizzo Registro Imprese.
     */
    @Test
    void PG_Physical_RegistroImprese_WithAccentedCharacters() {
        String correlationId = "pg-physical-accented-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "SANT'AGATA D'ESARO", "15", "Reggio Calabria", "RC", "89100")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA SANT'AGATA D'ESARO 15",
                null,
                null,
                "89100",
                "Reggio Calabria",
                "RC",
                "IT",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
    }

    /**
     * Verifica la gestione della presenza di un indirizzo secondario nella risposta Registro Imprese.
     */
    @Test
    void PG_Physical_RegistroImprese_WithSecondaryAddress() {
        String correlationId = "pg-physical-secondary-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "SECONDARIA", "50", "Milano", "MI", "20100")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA SECONDARIA 50",
                null,
                null,
                "20100",
                "Milano",
                "MI",
                "IT",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);
    }

    /**
     * Verifica la gestione di un CAP internazionale del Regno Unito.
     */
    @Test
    void Physical_Address_WithInternationalPostalCode_UK() {
        String correlationId = "physical-international-uk-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "STREET", "LONDON ROAD", "42", "London", "LDN", "SW1A1AA", "UK")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "STREET LONDON ROAD 42",
                null,
                null,
                "SW1A1AA",
                "London",
                "LDN",
                "UK",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);
    }

    /**
     * Verifica la gestione di un CAP internazionale del Belgio.
     */
    @Test
    void Physical_Address_WithInternationalPostalCode_Belgium() {
        String correlationId = "physical-international-be-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "AVENUE", "DE CORTENBERG", "280", "Bruxelles", "BXL", "1040", "BE")));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "AVENUE DE CORTENBERG 280",
                null,
                null,
                "1040",
                "Bruxelles",
                "BXL",
                "BE",
                null);

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);
    }

    /**
     * Verifica la gestione di un civico metrico restituito da ANPR.
     */
    @Test
    void PF_Physical_ANPR_WithMetricoCivicNumber() {
        String correlationId = "pf-physical-metrico-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "STRADA PROVINCIALE", "5", null, "5", null, null,
                null, null, null, null, "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA STRADA PROVINCIALE 5 KM 5",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    /**
     * Verifica la gestione di un civico composto da numero e lettera.
     */
    @Test
    void PF_Physical_ANPR_WithCivicNumberAndLetter() {
        String correlationId = "pf-physical-civic-letter-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA", "10", "A", null, null, null,
                null, null, null, null, "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA ROMA 10/A",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    /**
     * Verifica la gestione di un indirizzo ANPR senza numero civico (SNC).
     */
    @Test
    void PF_Physical_ANPR_WithSnCivic() {
        String correlationId = "pf-physical-sn-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA", "", null, null, "1", null,
                null, null, null, null, "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA ROMA SNC",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    /**
     * Verifica la composizione dell’indirizzo con informazioni aggiuntive di colore e scala.
     */
    @Test
    void PF_Physical_ANPR_WithColorAndScale() {
        String correlationId = "pf-physical-color-scale-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "MILANO", "25", null, null, null, "1",
                "A", null, null, null, "Milano", "MI", "20100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA MILANO 25",
                "ROSSO Scala A",
                null,
                "20100",
                "Milano",
                "MI",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    /**
     * Verifica la composizione minima quando sono disponibili solo strada e numero civico.
     */
    @Test
    void PF_Physical_ANPR_WithOnlyStreetAndNumber() {
        String correlationId = "pf-physical-minimal-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA ROMA 10",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    /**
     * Verifica la composizione di una struttura di indirizzo ANPR complessa.
     */
    @Test
    void PF_Physical_ANPR_WithComplexAddressStructure() {
        String correlationId = "pf-physical-complex-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "VIA", "NAZIONALE DELLE COMUNICAZIONI", "100", null, null, null, "2",
                "B", "C", "Presso Palazzo Centrale", "Roma Capitale", "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "VIA NAZIONALE DELLE COMUNICAZIONI 100",
                "NERO Scala B Scala est. C",
                "Presso Palazzo Centrale",
                "00100",
                "Roma",
                "RM",
                null,
                "Roma Capitale");

        verify(anprClient).callEService(any());
    }

    /**
     * Verifica la gestione di una specie/denominazione stradale numerica.
     */
    @Test
    void PF_Physical_ANPR_WithNumericSpecies() {
        String correlationId = "pf-physical-numeric-species-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(
                VALID_PF_CF, "PIAZZA", "DELLA REPUBBLICA", "15", "Roma", "RM", "00100"
        )));

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertPhysicalAddress(payload,
                "PIAZZA DELLA REPUBBLICA 15",
                "",
                null,
                "00100",
                "Roma",
                "RM",
                null,
                null);

        verify(anprClient).callEService(any());
    }

    /**
     * Verifica il risultato funzionale quando Registro Imprese non trova la sede del PG.
     */
    @Test
    void PG_Physical_RegistroImprese_NotFound() {
        AddressRegistroImprese addressRegistroImprese = new AddressRegistroImprese();
        addressRegistroImprese.setCf(VALID_PG_PIVA);
        addressRegistroImprese.setCode("ERR_00");
        addressRegistroImprese.setDescription("Sede not found");
        String correlationId = "pg-physical-not-found-" + UUID.randomUUID();
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA))
                .thenReturn(Mono.just(addressRegistroImprese));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        it.pagopa.pn.national.registries.model.CodeSqsDto payload = readOutputMessage(outputMessage.body());
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("REGISTRO_IMPRESE", payload.getRegistry());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(), payload.getAddressType());
        assertNull(payload.getPhysicalAddress());

        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());
        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);

    }


    /**
     * Verifica la gestione di un errore tecnico Registro Imprese nel flusso fisico PG.
     */
    @Test
    void PG_Physical_RegistroImprese_error() {
        String correlationId = "pg-physical-technical-" + UUID.randomUUID();
        RuntimeException technicalError = new RuntimeException("Registro Imprese unavailable");
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest("PG", VALID_PG_PIVA, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        RuntimeException thrown = processNextGatewayInputMessageExpectingFailure();

        assertEquals(correlationId, response.getCorrelationId());
        assertSame(technicalError, thrown.getCause() != null ? thrown.getCause() : thrown);
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));
        assertTrue(findNationalRegistriesRequestsByCorrelationId(correlationId).isEmpty());

        verify(infoCamereClient).getLegalAddress(VALID_PG_PIVA);
        verifyNoInteractions(anprClient);

    }

    private void assertPhysicalAddress(it.pagopa.pn.national.registries.model.CodeSqsDto payload,
                                       String address,
                                       String addressDetails,
                                       String at,
                                       String zip,
                                       String municipality,
                                       String province,
                                       String foreignState,
                                       String municipalityDetails) {
        assertNotNull(payload.getPhysicalAddress());
        assertEquals(address, payload.getPhysicalAddress().getAddress());
        if (addressDetails == null) {
            assertNull(payload.getPhysicalAddress().getAddressDetails());
        } else {
            assertEquals(addressDetails, payload.getPhysicalAddress().getAddressDetails());
        }
        if (at == null) {
            assertNull(payload.getPhysicalAddress().getAt());
        } else {
            assertEquals(at, payload.getPhysicalAddress().getAt());
        }
        assertEquals(zip, payload.getPhysicalAddress().getZip());
        assertEquals(municipality, payload.getPhysicalAddress().getMunicipality());
        if (province == null) {
            assertNull(payload.getPhysicalAddress().getProvince());
        } else {
            assertEquals(province, payload.getPhysicalAddress().getProvince());
        }
        if (foreignState == null) {
            assertNull(payload.getPhysicalAddress().getForeignState());
        } else {
            assertEquals(foreignState, payload.getPhysicalAddress().getForeignState());
        }
        if (municipalityDetails == null) {
            assertNull(payload.getPhysicalAddress().getMunicipalityDetails());
        } else {
            assertEquals(municipalityDetails, payload.getPhysicalAddress().getMunicipalityDetails());
        }
    }

/**
     * Verifica che una risposta ANPR senza residenze utilizzabili non produca un indirizzo fisico valido.
     */
    @Test
    void PF_Physical_ANPR_WithoutUsefulResidences() {
        String correlationId = "corr-no-residence-" + UUID.randomUUID();
        RispostaE002OK response = anprResponse("RSSMRA80A01H501U", "VIA", "ROMA", "10", "Roma", "RM", "00100");
        when(anprClient.callEService(any())).thenReturn(Mono.just(response));

        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        CodeSqsDto payload = output();
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("ANPR", payload.getRegistry());
        assertNull(payload.getPhysicalAddress());
    }

    /**
     * Verifica la corretta gestione funzionale di una residenza estera restituita da ANPR.
     */
    @Test
    void PF_Physical_ANPR_ForeignResidence() throws Exception {
        String correlationId = "corr-foreign-" + UUID.randomUUID();
        RispostaE002OK response = anprResponse(VALID_PF_CF, "VIA", "IGNORED", "1", "Roma", "RM", "00100");
        TipoResidenza residence = response.getListaSoggetti().getDatiSoggetto().getFirst().getResidenza().getFirst();
        residence.setIndirizzo(null);

        Object foreignLocation = newForSetter(residence, "setLocalitaEstera");
        Object foreignAddress = newForSetter(foreignLocation, "setIndirizzoEstero");
        Object foreignToponym = newForSetter(foreignAddress, "setToponimo");
        Object locality = newForSetter(foreignAddress, "setLocalita");
        set(foreignToponym, "setDenominazione", "RUE DE PARIS");
        set(foreignToponym, "setNumeroCivico", "12");
        set(foreignAddress, "setCap", "75001");
        set(locality, "setDescrizioneStato", "FRANCIA");
        set(locality, "setDescrizioneLocalita", "Parigi");
        set(locality, "setProvinciaContea", "Ile-de-France");
        set(foreignAddress, "setToponimo", foreignToponym);
        set(foreignAddress, "setLocalita", locality);
        set(foreignLocation, "setIndirizzoEstero", foreignAddress);
        set(residence, "setLocalitaEstera", foreignLocation);
        when(anprClient.callEService(any())).thenReturn(Mono.just(response));

        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        CodeSqsDto payload = output();
        assertNotNull(payload.getPhysicalAddress());
        assertEquals("RUE DE PARIS 12", payload.getPhysicalAddress().getAddress());
        assertEquals("75001", payload.getPhysicalAddress().getZip());
        assertEquals("Parigi", payload.getPhysicalAddress().getMunicipality());
        assertEquals("Ile-de-France", payload.getPhysicalAddress().getProvince());
        assertEquals("FRANCIA", payload.getPhysicalAddress().getForeignState());
    }

    /**
     * Verifica che, con più soggetti ANPR, venga utilizzato solo quello associato al codice fiscale richiesto.
     */
    @Test
    void PF_Physical_ANPR_MultipleSubjectsUsesOnlyRequestedTaxId() {
        String correlationId = "corr-multiple-subjects-" + UUID.randomUUID();
        RispostaE002OK wrong = anprResponse("RSSMRA80A01H501U", "VIA", "SBAGLIATA", "1", "Roma", "RM", "00100");
        RispostaE002OK correct = anprResponse(VALID_PF_CF, "VIA", "CORRETTA", "99", "Milano", "MI", "20100");
        TipoDatiSoggettiEnte wrongSubject = wrong.getListaSoggetti().getDatiSoggetto().getFirst();
        TipoDatiSoggettiEnte correctSubject = correct.getListaSoggetti().getDatiSoggetto().getFirst();
        TipoListaSoggetti subjects = new TipoListaSoggetti();
        subjects.setDatiSoggetto(List.of(wrongSubject, correctSubject));
        correct.setListaSoggetti(subjects);
        when(anprClient.callEService(any())).thenReturn(Mono.just(correct));

        enqueueGatewayRequest("PF", VALID_PF_CF.toLowerCase(), AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        CodeSqsDto payload = output();
        assertNotNull(payload.getPhysicalAddress());
        assertEquals("VIA CORRETTA 99", payload.getPhysicalAddress().getAddress());
    }

    /**
     * Verifica la corretta mappatura dei campi di richiesta e del contatore nel flusso ANPR.
     */
    @Test
    void PF_Physical_ANPR_CounterAndRequestFieldsAreMapped() {
        String correlationId1 = "corr-request-1-" + UUID.randomUUID();
        String correlationId2 = "corr-request-2-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(
                Mono.just(anprResponse(VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100")),
                Mono.just(anprResponse(VALID_PF_CF, "VIA", "MILANO", "20", "Milano", "MI", "20100"))
        );

        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId1);
        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId2);
        processNextGatewayInputMessage();
        processNextGatewayInputMessage();
        assertNotNull(output());
        assertNotNull(output());

        ArgumentCaptor<RichiestaE002> captor = ArgumentCaptor.forClass(RichiestaE002.class);
        verify(anprClient, times(2)).callEService(captor.capture());
        List<RichiestaE002> requests = captor.getAllValues();
        assertEquals(2, requests.size());
        assertEquals(2, Set.of(requests.get(0).getIdOperazioneClient(), requests.get(1).getIdOperazioneClient()).size());
        assertEquals(Set.of(correlationId1, correlationId2), Set.of(
                requests.get(0).getDatiRichiesta().getMotivoRichiesta(),
                requests.get(1).getDatiRichiesta().getMotivoRichiesta()
        ));
        assertTrue(requests.stream().allMatch(request -> "C001".equals(request.getDatiRichiesta().getCasoUso())));
        assertTrue(requests.stream().allMatch(request -> "2026-10-10".equals(request.getDatiRichiesta().getDataRiferimentoRichiesta())));
        assertTrue(requests.stream().allMatch(request -> VALID_PF_CF.equalsIgnoreCase(request.getCriteriRicerca().getCodiceFiscale())));
    }

    private CodeSqsDto output() {
        Message message = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(message);
        return readOutputMessage(message.body());
    }

    private Object newForSetter(Object target, String setterName) throws Exception {
        Method setter = findSetter(target, setterName);
        return setter.getParameterTypes()[0].getDeclaredConstructor().newInstance();
    }

    private void set(Object target, String setterName, Object value) throws Exception {
        findSetter(target, setterName).invoke(target, value);
    }

    private Method findSetter(Object target, String setterName) {
        return Arrays.stream(target.getClass().getMethods())
                .filter(method -> setterName.equals(method.getName()) && method.getParameterCount() == 1)
                .findFirst()
                .orElseThrow();
    }

/**
     * Verifica che una richiesta multi-indirizzo senza elementi restituisca bad request.
     */
    @Test
    void PhysicalAddresses_EmptyAddressesReturnsBadRequest() {
        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"correlationId":"corr-empty","referenceRequestDate":"2026-10-10T10:15:30.000Z","addresses":[]}
                        """)
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(anprClient, infoCamereClient);
    }

    /**
     * Verifica che elementi PF e PG siano elaborati indipendentemente mantenendo la corretta correlazione con l’input.
     */
    @Test
    void PhysicalAddresses_MixedPfPgReturnsIndependentResultsInInputOrder() {
        String correlationId = "physical-mixed-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100")));
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "MILANO", "20", "Milano", "MI", "20100")));

        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(correlationId,
                        "{\"recIndex\":1,\"taxId\":\"" + VALID_PF_CF + "\",\"recipientType\":\"PF\"}",
                        "{\"recIndex\":2,\"taxId\":\"" + VALID_PG_PIVA + "\",\"recipientType\":\"PG\"}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.correlationId").isEqualTo(correlationId)
                .jsonPath("$.addresses.length()").isEqualTo(2)
                .jsonPath("$.addresses[?(@.recIndex == 1)].registry").isEqualTo(List.of("ANPR"))
                .jsonPath("$.addresses[?(@.recIndex == 1)].physicalAddress.address").isEqualTo(List.of("VIA ROMA 10"))
                .jsonPath("$.addresses[?(@.recIndex == 2)].registry").isEqualTo(List.of("REGISTRO_IMPRESE"))
                .jsonPath("$.addresses[?(@.recIndex == 2)].physicalAddress.address").isEqualTo(List.of("VIA MILANO 20"));
    }

    /**
     * Verifica che un not found ANPR su un elemento non faccia fallire gli altri elementi della richiesta.
     */
    @Test
    void PhysicalAddresses_AnprNotFoundDoesNotFailOtherElements() {
        String correlationId = "physical-anpr-not-found-" + UUID.randomUUID();
        PnNationalRegistriesException notFound = new PnNationalRegistriesException("Not Found", 404, "Not Found", HttpHeaders.EMPTY,
                "{\"codiceErroreAnomalia\":\"EN122\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8, AnprResponseKO.class);
        when(anprClient.callEService(any())).thenReturn(Mono.error(notFound));
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "MILANO", "20", "Milano", "MI", "20100")));

        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(correlationId,
                        "{\"recIndex\":1,\"taxId\":\"" + VALID_PF_CF + "\",\"recipientType\":\"PF\"}",
                        "{\"recIndex\":2,\"taxId\":\"" + VALID_PG_PIVA + "\",\"recipientType\":\"PG\"}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.addresses[?(@.recIndex == 1)].registry").isEqualTo(List.of("ANPR"))
                .jsonPath("$.addresses[?(@.recIndex == 1)].physicalAddress").isEqualTo(List.of())
                .jsonPath("$.addresses[?(@.recIndex == 1)].error").isEqualTo(List.of())
                .jsonPath("$.addresses[?(@.recIndex == 2)].physicalAddress.address").isEqualTo(List.of("VIA MILANO 20"));
    }

    /**
     * Verifica che un errore too many requests sia riportato solo sull’elemento interessato.
     */
    @Test
    void PhysicalAddresses_TooManyRequestsIsReportedOnlyOnFailedElement() {
        String correlationId = "physical-429-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.error(new PnNationalRegistriesException(
                "too many", 429, "Too Many Requests", null, null, StandardCharsets.UTF_8, AnprResponseKO.class)));
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "MILANO", "20", "Milano", "MI", "20100")));

        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(correlationId,
                        "{\"recIndex\":1,\"taxId\":\"" + VALID_PF_CF + "\",\"recipientType\":\"PF\"}",
                        "{\"recIndex\":2,\"taxId\":\"" + VALID_PG_PIVA + "\",\"recipientType\":\"PG\"}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.addresses[?(@.recIndex == 1)].error").isEqualTo(List.of("DOWNSTREAM_TOO_MANY_REQUESTS"))
                .jsonPath("$.addresses[?(@.recIndex == 1)].errorStatus").isEqualTo(List.of(429))
                .jsonPath("$.addresses[?(@.recIndex == 2)].physicalAddress.address").isEqualTo(List.of("VIA MILANO 20"));
    }

    /**
     * Verifica che un errore generico sia riportato solo sull’elemento interessato senza compromettere gli altri.
     */
    @Test
    void PhysicalAddresses_GenericFailureIsReportedOnlyOnFailedElement() {
        String correlationId = "physical-generic-error-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.error(new IllegalStateException("ANPR unavailable")));
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(registroImpreseAddress(VALID_PG_PIVA, "VIA", "MILANO", "20", "Milano", "MI", "20100")));

        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(correlationId,
                        "{\"recIndex\":1,\"taxId\":\"" + VALID_PF_CF + "\",\"recipientType\":\"PF\"}",
                        "{\"recIndex\":2,\"taxId\":\"" + VALID_PG_PIVA + "\",\"recipientType\":\"PG\"}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.addresses[?(@.recIndex == 1)].error").isEqualTo(List.of("DOWNSTREAM_REQUEST_ERROR"))
                .jsonPath("$.addresses[?(@.recIndex == 1)].errorStatus").isEqualTo(List.of(500))
                .jsonPath("$.addresses[?(@.recIndex == 2)].physicalAddress.address").isEqualTo(List.of("VIA MILANO 20"));
    }

    /**
     * Verifica che un PG senza sede legale produca l’elemento di risposta senza indirizzo fisico.
     */
    @Test
    void PhysicalAddresses_PgWithoutLegalAddressReturnsElementWithoutPhysicalAddress() {
        String correlationId = "physical-pg-empty-" + UUID.randomUUID();
        AddressRegistroImprese response = new AddressRegistroImprese();
        response.setCf(VALID_PG_PIVA);
        when(infoCamereClient.getLegalAddress(VALID_PG_PIVA)).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/national-registries-private/physical-addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(correlationId, "{\"recIndex\":7,\"taxId\":\"" + VALID_PG_PIVA + "\",\"recipientType\":\"PG\"}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.addresses[0].recIndex").isEqualTo(7)
                .jsonPath("$.addresses[0].registry").isEqualTo("REGISTRO_IMPRESE")
                .jsonPath("$.addresses[0].physicalAddress").isMap()
                .jsonPath("$.addresses[0].physicalAddress").isEmpty();
    }

    private String request(String correlationId, String... addresses) {
        return "{\"correlationId\":\"" + correlationId + "\",\"referenceRequestDate\":\"2026-10-10T10:15:30.000Z\",\"addresses\":[" + String.join(",", addresses) + "]}";
    }
}
