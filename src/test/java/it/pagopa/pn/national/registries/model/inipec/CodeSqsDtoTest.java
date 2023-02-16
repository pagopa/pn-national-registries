package it.pagopa.pn.national.registries.model.inipec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.utils.JacksonCustomSpELSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static it.pagopa.pn.national.registries.utils.JacksonCustomSpELSerializer.FILTER_NAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeSqsDtoTest {

    private static ObjectMapper mapper;

    @BeforeAll
    public static void init() {
        mapper = new ObjectMapper()
                .setFilterProvider(new SimpleFilterProvider()
                        .addFilter(FILTER_NAME, new JacksonCustomSpELSerializer()));
    }

    @Test
    void testError() throws JsonProcessingException {
        CodeSqsDto dto = newCodeSqsDto();
        dto.setError("xxx");

        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("error"));
        assertTrue(json.contains("xxx"));
        assertFalse(json.contains("physicalAddress"));
        assertFalse(json.contains("digitalAddress"));
    }

    @Test
    void testPhysical() throws JsonProcessingException {
        CodeSqsDto dto = newCodeSqsDto();
        dto.setPhysicalAddress(new PhysicalAddress());

        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("physicalAddress"));
        assertFalse(json.contains("digitalAddress"));
        assertFalse(json.contains("error"));
    }

    @Test
    void testPhysicalNull() throws JsonProcessingException {
        CodeSqsDto dto = newCodeSqsDto();
        dto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());

        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("physicalAddress"));
        assertFalse(json.contains("digitalAddress"));
        assertFalse(json.contains("error"));
    }

    @Test
    void testDigital() throws JsonProcessingException {
        CodeSqsDto dto = newCodeSqsDto();
        dto.setDigitalAddress(Collections.emptyList());

        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("digitalAddress"));
        assertFalse(json.contains("physicalAddress"));
        assertFalse(json.contains("error"));
    }

    @Test
    void testDigitalNull() throws JsonProcessingException {
        CodeSqsDto dto = newCodeSqsDto();
        dto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.toString());

        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("digitalAddress"));
        assertFalse(json.contains("physicalAddress"));
        assertFalse(json.contains("error"));
    }

    private CodeSqsDto newCodeSqsDto() {
        CodeSqsDto dto = new CodeSqsDto();
        dto.setTaxId("taxId");
        dto.setCorrelationId("correlationId");
        return dto;
    }
}