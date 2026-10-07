package it.pagopa.pn.national.registries.constant;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_INVALID_DOMICILETYPE;

@Slf4j
public enum DomicileType {
    PHYSICAL,
    DIGITAL;

    public static DomicileType fromString(String value) {
        try {
            return DomicileType.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid DomicileType [{}], allowed values are: PHYSICAL, DIGITAL", value);
            throw new PnInternalException("Invalid recipientType, allowed values are: PG, PF", ERROR_CODE_INVALID_DOMICILETYPE);
        }
    }
}
