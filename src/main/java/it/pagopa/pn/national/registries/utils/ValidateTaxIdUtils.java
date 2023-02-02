package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.AddressErrorDto;
import org.springframework.http.HttpStatus;

import java.nio.charset.Charset;

public class ValidateTaxIdUtils {

    public static void validateTaxId(String taxId){
        if(!ValidateUtils.validate(taxId)){
            throw new PnNationalRegistriesException("TaxId not valid", HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class);
        }
    }
}
