package it.pagopa.pn.national.registries.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;

@Slf4j
public final class JacksonCustomSpELSerializer implements PropertyFilter {

    public static final String FILTER_NAME = "SpEL";

    private final ExpressionParser expressionParser;

    public JacksonCustomSpELSerializer() {
        expressionParser = new SpelExpressionParser();
    }

    @Override
    public void serializeAsField(Object o, JsonGenerator generator, SerializerProvider serializerProvider, PropertyWriter propertyWriter) throws Exception {
        final Field field = o.getClass().getDeclaredField(propertyWriter.getName());

        Boolean value = true;

        if (field.isAnnotationPresent(JsonFilterSpEL.class)) {
            log.trace("field: {}, class: {}", field.getName(), o.getClass().getName());
            JsonFilterSpEL jfSpEL = field.getAnnotation(JsonFilterSpEL.class);

            final String expToParse = replaceRefAtThisField(jfSpEL.value(), field);
            final Expression expression = expressionParser.parseExpression(expToParse);
            final StandardEvaluationContext context = new StandardEvaluationContext(o);

            checkExpressionReturnType(expression, context);

            value = expression.getValue(context, Boolean.class);
        }

        if (Boolean.TRUE.equals(value)) {
            propertyWriter.serializeAsField(o, generator, serializerProvider);
        } else {
            log.trace("skip serialization of field {}", field.getName());
        }
    }

    @Override
    public void serializeAsElement(Object o, JsonGenerator generator, SerializerProvider serializerProvider, PropertyWriter propertyWriter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void depositSchemaProperty(PropertyWriter writer, ObjectNode propertiesNode, SerializerProvider provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void depositSchemaProperty(PropertyWriter writer, JsonObjectFormatVisitor objectVisitor, SerializerProvider provider) {
        throw new UnsupportedOperationException();
    }

    private void checkExpressionReturnType(final Expression expression, final StandardEvaluationContext context) {
        final Class<?> type = expression.getValueType(context);

        if (type == null || !type.equals(Boolean.class)) {
            log.warn("return type of expression: '{}' is {} but should be Boolean", expression.getExpressionString(), type);
            throw new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR);
        }
    }

    private String replaceRefAtThisField(final String expression, final Field field) {
        return expression.replace("#?", "#this." + field.getName());
    }
}
