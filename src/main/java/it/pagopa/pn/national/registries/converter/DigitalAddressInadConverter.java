package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.DigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.model.inad.ElementDigitalAddressDto;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;


public class DigitalAddressInadConverter {

    private DigitalAddressInadConverter() { }

    public static GetDigitalAddressINADOKDto mapToResponseOk(ResponseRequestDigitalAddressDto elementDigitalAddressDto) {
        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();

        if (elementDigitalAddressDto != null) {

            response.setSince(elementDigitalAddressDto.getSince());
            response.setTaxId(elementDigitalAddressDto.getCodiceFiscale());

            for (ElementDigitalAddressDto item : elementDigitalAddressDto.getDigitalAddress()) {
                response.addDigitalAddressItem(convertToGetDigitalAddressINADOKDigitalAddressInnerDto(item));
            }
        }
        return response;
    }

    private static DigitalAddressDto convertToGetDigitalAddressINADOKDigitalAddressInnerDto(ElementDigitalAddressDto item) {
        DigitalAddressDto digitalAddress = new DigitalAddressDto();

        digitalAddress.setDigitalAddress(item.getDigitalAddress());

        if (item.getPracticedProfession() != null) {
            digitalAddress.setPracticedProfession(item.getPracticedProfession());
        }

        return digitalAddress;
    }

}
