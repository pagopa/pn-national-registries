package it.pagopa.pn.national.registries.model.anpr;

import io.netty.util.internal.StringUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Getter
@Slf4j
public enum AddressColorEnum {
    ROSSO("1", "ROSSO"),
    NERO("2", "NERO"),
    BLU("3", "BLU"),
    RESIDENZIALE("4", "Res."),
    NON_RESIDENZIALE("5", "Non res.");

    private final String value;
    private final String code;

    AddressColorEnum(String number, String color) {
        this.value = number;
        this.code = color;
    }

    public static String getCodeFromValue(String value) {
        try {
            return Arrays.stream(AddressColorEnum.values()).filter(elem -> elem.getValue().equals(value)).findFirst().orElseThrow().getCode();
        } catch (Exception e) {
            log.warn("Value {} is not a valid color for AddressColorEnum", value);
            return StringUtil.EMPTY_STRING;
        }
    }
}
