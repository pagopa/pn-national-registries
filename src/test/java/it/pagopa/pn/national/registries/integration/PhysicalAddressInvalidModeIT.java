package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Scenari funzionali: Verifica il comportamento di fallback della composizione indirizzo quando viene configurata una modalità non riconosciuta.
 *
 * <p>Configurazione specifica della classe:</p>
 * <ul>
 *   <li>{@code pn.national-registries.address-composition-mode=UNKNOWN}</li>
 * </ul>
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = "pn.national-registries.address-composition-mode=UNKNOWN")
class PhysicalAddressInvalidModeIT extends AbstractNationalRegistriesComponentIT {

    /**
     * Verifica che una modalità di composizione non valida ricada funzionalmente sulla modalità OLD.
     */
    @Test
    void PF_Physical_ANPR_InvalidAddressCompositionFallsBackToOLD() {
        String correlationId = "corr-invalid-mode-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(VALID_PF_CF, "VIA", "ROMA", "10", "B", null, null, "1", "A", "EST", null, null, "Roma", "RM", "00100")));

        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        CodeSqsDto payload = readOutputMessage(receiveSingleMessage(OUTPUT_QUEUE).body());
        assertNotNull(payload.getPhysicalAddress());
        assertEquals("VIA ROMA 10/B", payload.getPhysicalAddress().getAddress());
        assertEquals("A", payload.getPhysicalAddress().getAddressDetails());
    }
}
