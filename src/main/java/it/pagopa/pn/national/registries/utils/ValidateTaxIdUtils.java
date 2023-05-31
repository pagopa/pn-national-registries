package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.AddressErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * This class use it.pagopa.pn.commons.utils.ValidateUtils
 * Read the parameter <i>MapTaxIdWhiteList</i> to skip CF validation
 */
@Component
@lombok.CustomLog
public class ValidateTaxIdUtils {


    private final ValidateUtils validateUtils;

    ValidateTaxIdUtils(ValidateUtils validateUtils) {
        this.validateUtils = validateUtils;
    }

    public void validateTaxId(String taxId) {
        String process = "validating taxId";
        log.logChecking(process);
        if (!validateUtils.validate(taxId)) {
            log.logCheckingOutcome(process, false, "TaxId is not valid");
            throw new PnNationalRegistriesException("TaxId not valid", HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class);
        }
        log.logCheckingOutcome(process, true);
    }
}
