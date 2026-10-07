package it.pagopa.pn.national.registries.constant;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_INVALID_RECIPIENTTYPE;

@Slf4j
public enum RecipientType {

    PG,
    PF;

    public static RecipientType fromString(String value) {
        try {
            return RecipientType.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid recipientType [{}], allowed values are: PG, PF", value);
            throw new PnInternalException("Invalid recipientType, allowed values are: PG, PF", ERROR_CODE_INVALID_RECIPIENTTYPE);
        }
    }

}
