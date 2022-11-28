package it.pagopa.pn.national.registries.utils;

public enum InipecScopeEnum {

    PEC("pec-pa"),
    SEDE("sede-impresa-pa"),
    LEGALE_RAPPRESENTANTE("lr-pa");

    private final String value;

    InipecScopeEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
