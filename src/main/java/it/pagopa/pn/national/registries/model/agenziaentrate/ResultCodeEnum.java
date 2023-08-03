package it.pagopa.pn.national.registries.model.agenziaentrate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Values:  _'00'_: Richiesta correttamente eseguita  _'01'_: Richiesta andata in errore  _'02'_: Formato dati in input non corretto
 */
public enum ResultCodeEnum {
    CODE_00("00"),

    CODE_01("01"),

    CODE_02("02"),

    CODE_03("03"),

    UNKNOWN("UNKNOWN");

    private final String value;

    ResultCodeEnum(String value) {
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
    public static String fromValue(String value) {
        for (ResultCodeEnum b : ResultCodeEnum.values()) {
            if (b.value.equals(value)) {
                return b.value;
            }
        }
        return ResultCodeEnum.UNKNOWN.value;
    }
}
