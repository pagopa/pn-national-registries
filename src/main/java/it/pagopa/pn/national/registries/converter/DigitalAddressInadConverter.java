package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.inad.client.v1.dto.ElementDigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.inad.client.v1.dto.ResponseRequestDigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.server.inad.extract.cf.v1.dto.GetDigitalAddressINADOKDigitalAddressInnerDto;
import it.pagopa.pn.national.registries.generated.openapi.server.inad.extract.cf.v1.dto.GetDigitalAddressINADOKDto;


public class DigitalAddressInadConverter {

    private DigitalAddressInadConverter() { }

    public static GetDigitalAddressINADOKDto mapToResponseOk(ResponseRequestDigitalAddressDto elementDigitalAddressDto) {
        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();
        if (elementDigitalAddressDto != null //TODO - Capire se la gestione sia corretta
                && elementDigitalAddressDto.getDigitalAddress() != null
                && elementDigitalAddressDto.getCodiceFiscale() != null
                && elementDigitalAddressDto.getSince() != null) {

            int counter = 0;
            response.setSince(elementDigitalAddressDto.getSince());
            response.setTaxId(elementDigitalAddressDto.getCodiceFiscale());

            for (ElementDigitalAddressDto item : elementDigitalAddressDto.getDigitalAddress()) { //TODO - Capire se la gestione sia corretta
                if (item.getDigitalAddress() != null && item.getPracticedProfession() != null) {
                    response.addDigitalAddressItem(convertToGetDigitalAddressINADOKDigitalAddressInnerDto(elementDigitalAddressDto.getDigitalAddress().get(counter)));
                }
                counter++;
            }
        }
        return response;
    }

    private static GetDigitalAddressINADOKDigitalAddressInnerDto convertToGetDigitalAddressINADOKDigitalAddressInnerDto(ElementDigitalAddressDto item) {
        GetDigitalAddressINADOKDigitalAddressInnerDto digitalAddress = new GetDigitalAddressINADOKDigitalAddressInnerDto();

        if (item.getDigitalAddress() != null) {
            digitalAddress.setDigitalAddress(item.getDigitalAddress());
        }

        if (item.getPracticedProfession() != null) {
            digitalAddress.setPracticedProfession(item.getPracticedProfession());
        }

        if (item.getUsageInfo() != null
                && item.getUsageInfo().getMotivazione() != null
                && item.getUsageInfo().getDateEndValidity() != null) {
            digitalAddress.getUsageInfo().setMotivation(item.getUsageInfo().getMotivazione().getValue());
        }
        return digitalAddress;
    }

}
