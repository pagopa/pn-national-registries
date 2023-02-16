package it.pagopa.pn.national.registries.utils;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.SpelEvaluationException;

import static it.pagopa.pn.national.registries.utils.JacksonCustomSpELSerializer.FILTER_NAME;
import static org.junit.jupiter.api.Assertions.*;

class JacksonCustomSpELSerializerTest {

    @Test
    void testThrowSerializeAsElement() {
        JacksonCustomSpELSerializer spELSerializer = new JacksonCustomSpELSerializer();
        assertThrows(UnsupportedOperationException.class, () -> spELSerializer.serializeAsElement(null, null, null, null));
    }

    @Test
    void testThrowDepositSchemaProperty1() {
        JacksonCustomSpELSerializer spELSerializer = new JacksonCustomSpELSerializer();
        assertThrows(UnsupportedOperationException.class, () -> spELSerializer.depositSchemaProperty(null, (ObjectNode) null, null));
    }

    @Test
    void testThrowDepositSchemaProperty2() {
        JacksonCustomSpELSerializer spELSerializer = new JacksonCustomSpELSerializer();
        assertThrows(UnsupportedOperationException.class, () -> spELSerializer.depositSchemaProperty(null, (JsonObjectFormatVisitor) null, null));
    }

    @Test
    void testThrowSerializeAsField() {
        ObjectMapper mapper = new ObjectMapper()
                .setFilterProvider(new SimpleFilterProvider()
                        .addFilter(FILTER_NAME, new JacksonCustomSpELSerializer()));
        try {
            mapper.writeValueAsString(new TestWrongSpElPojo());
            fail();
        } catch (JsonProcessingException e) {
            assertEquals(SpelEvaluationException.class, e.getCause().getClass());
        }
    }

    @Data
    @JsonFilter(FILTER_NAME)
    private static class TestWrongSpElPojo {
        @JsonFilterSpEL("#?")
        private String field;
    }

}