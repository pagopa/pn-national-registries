package it.pagopa.pn.national.registries.constant;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum DigitalAddressRecipientType {

    IMPRESA("IMPRESA"),
    PROFESSIONISTA("PROFESSIONISTA"),
    PERSONALE("PERSONALE"),
    PERSONA_GIURIDICA("PERSONA_GIURIDICA");

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

    public static DigitalAddressRecipientType fromValue(String value) {
        for (DigitalAddressRecipientType b : DigitalAddressRecipientType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        log.warn("Unexpected value '{}' for DigitalAddressRecipientType", value);
        return null;
    }

}
