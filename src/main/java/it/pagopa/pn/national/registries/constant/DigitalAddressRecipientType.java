package it.pagopa.pn.national.registries.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DigitalAddressRecipientType {

    IMPRESA("IMPRESA"),
    PROFESSIONISTA("PROFESSIONISTA"),
    PERSONA_FISICA("PERSONA_FISICA");

    private final String value;

    DigitalAddressRecipientType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static DigitalAddressRecipientType fromValue(String value) {
        for (DigitalAddressRecipientType b : DigitalAddressRecipientType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
