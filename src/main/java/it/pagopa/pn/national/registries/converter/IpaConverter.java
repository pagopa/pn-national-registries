package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecDto;

import it.pagopa.pn.national.registries.model.ipa.DataWS05Dto;
import it.pagopa.pn.national.registries.model.ipa.DataWS23Dto;
import it.pagopa.pn.national.registries.model.ipa.WS05ResponseDto;
import it.pagopa.pn.national.registries.model.ipa.WS23ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IpaConverter {

    public static final String ADDRESS_TYPE = "PEC";

    public IPAPecDto convertToIPAPecDtoFromWS05(WS05ResponseDto ws05ResponseDto) {
        IPAPecDto response = new IPAPecDto();
        DataWS05Dto dataWS05Dto = ws05ResponseDto.getData();

        response.setCodEnte(dataWS05Dto.getCodAmm());
        response.setDenominazione(dataWS05Dto.getDesAmm());
        response.setTipo(ADDRESS_TYPE);
        response.setDomicilioDigitale(dataWS05Dto.getMail1());

        return response;
    }

    public IPAPecDto convertToIpaPecDtoFromWS23(WS23ResponseDto ws23ResponseDto) {
        IPAPecDto ipaPecDto = new IPAPecDto();
        DataWS23Dto dataWS23Dto = ws23ResponseDto.getData().get(0);
        ipaPecDto.setCodEnte(dataWS23Dto.getCodEnte());
        ipaPecDto.setDenominazione(dataWS23Dto.getDenominazione());
        ipaPecDto.setTipo(dataWS23Dto.getType());
        ipaPecDto.setDomicilioDigitale(dataWS23Dto.getDomicilioDigitale());
        return ipaPecDto;
    }
}
