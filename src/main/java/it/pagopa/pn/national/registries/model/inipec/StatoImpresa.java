package it.pagopa.pn.national.registries.model.inipec;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_INVALID_STATO_IMPRESA;

@Getter
@Slf4j
public enum StatoImpresa {
    ER("ER"),
    ND("ND"),
    NF("NF");

    private final String value;

    StatoImpresa(String value) {
        this.value = value;
    }

    public static StatoImpresa fromString(String value) {
        for (StatoImpresa stato : StatoImpresa.values()) {
            if (stato.getValue().equals(value)) {
                return stato;
            }
        }
        log.warn("Invalid statoImpresa value: {}", value);
        throw new PnInternalException("Invalid statoImpresa value. Allowed values: ER, ND, NF", ERROR_CODE_INVALID_STATO_IMPRESA);
    }
}