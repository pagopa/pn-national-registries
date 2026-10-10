package it.pagopa.pn.national.registries.component;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = {
        "pn.national-registries.enable-pf-pec-fallback-flow=false",
        "pn.national-registries.inad-batch-enabled=true"
})
class NationalRegistriesInadBatchIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void PF_Digital_INAD_batch() {
        String correlationId = "pf-inad-batch-" + UUID.randomUUID();

        AddressOKDto response = enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL, correlationId);
        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertNull(receiveSingleMessage(OUTPUT_QUEUE));
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        List<Map<String, AttributeValue>> requests = findNationalRegistriesRequestsByCorrelationId(correlationId);
        assertEquals(1, requests.size());
        Map<String, AttributeValue> storedRequest = requests.get(0);
        assertEquals(correlationId, storedRequest.get("correlationId").s());
        assertEquals(VALID_PF_CF, storedRequest.get("taxId").s());
        assertEquals("PF", storedRequest.get("recipientType").s());
        assertEquals("DIGITAL", storedRequest.get("domicileType").s());
        assertEquals(CX_ID, storedRequest.get("clientId").s());
        assertEquals("INAD", storedRequest.get("registry").s());
        assertEquals("NOT_WORKED", storedRequest.get("status").s());
        assertEquals("INAD#NOT_WORKED", storedRequest.get("registryStatus").s());

        verifyNoInteractions(inadClient, infoCamereClient, ipaClient, anprClient, checkCfClient, adELegalClient);
    }
}

