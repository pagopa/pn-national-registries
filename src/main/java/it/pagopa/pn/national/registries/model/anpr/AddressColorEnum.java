package it.pagopa.pn.national.registries.model.anpr;

import lombok.Getter;

import java.util.Arrays;

@Getter
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
        return Arrays.stream(AddressColorEnum.values()).filter(elem -> elem.getValue().equals(value)).findFirst().orElseThrow().getCode();
    }
}
