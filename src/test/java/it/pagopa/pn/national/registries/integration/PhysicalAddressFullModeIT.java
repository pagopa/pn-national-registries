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

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = "pn.national-registries.address-composition-mode=FULL")
class PhysicalAddressFullModeIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void PF_Physical_ANPR_AddressComposition_FULL() {
        String correlationId = "corr-full-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(VALID_PF_CF, "VIA", "ROMA", "10", "B", null, null, "1", "A", "EST", null, null, "Roma", "RM", "00100")));

        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        CodeSqsDto payload = readOutputMessage(receiveSingleMessage(OUTPUT_QUEUE).body());
        assertNotNull(payload.getPhysicalAddress());
        assertEquals("VIA ROMA 10/B", payload.getPhysicalAddress().getAddress());
        assertTrue(payload.getPhysicalAddress().getAddressDetails().contains("ROSSO"));
        assertTrue(payload.getPhysicalAddress().getAddressDetails().contains("Scala A"));
        assertTrue(payload.getPhysicalAddress().getAddressDetails().contains("Scala est. EST"));
    }

    @Test
    void PF_Physical_ANPR_AddressComposition_FULL_RespectsMaxLength() {
        String correlationId = "corr-full-max-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(Mono.just(anprResponse(VALID_PF_CF, "VIA", "DENOMINAZIONE STRADALE MOLTO LUNGA CHE NON DEVE SUPERARE IL LIMITE", "123", "Roma", "RM", "00100")));

        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        CodeSqsDto payload = readOutputMessage(receiveSingleMessage(OUTPUT_QUEUE).body());
        assertNotNull(payload.getPhysicalAddress());
        assertTrue(payload.getPhysicalAddress().getAddress().length() <= 44);
    }
}
