package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.inad.client.v1.dto.ElementDigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.inad.client.v1.dto.ResponseRequestDigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.DigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;


public class DigitalAddressInadConverter {

    private DigitalAddressInadConverter() { }

    public static GetDigitalAddressINADOKDto mapToResponseOk(ResponseRequestDigitalAddressDto elementDigitalAddressDto) {
        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();

        if (elementDigitalAddressDto != null) {

            int counter = 0;
            response.setSince(elementDigitalAddressDto.getSince());
            response.setTaxId(elementDigitalAddressDto.getCodiceFiscale());

            for (ElementDigitalAddressDto item : elementDigitalAddressDto.getDigitalAddress()) {
                response.addDigitalAddressItem(convertToGetDigitalAddressINADOKDigitalAddressInnerDto(elementDigitalAddressDto.getDigitalAddress().get(counter)));
                counter++;
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

        if (digitalAddress.getUsageInfo().getMotivation() != null
            && digitalAddress.getUsageInfo().getDateEndValidity() != null) {
            digitalAddress.setUsageInfo(digitalAddress.getUsageInfo());
        }

        return digitalAddress;
    }

}
