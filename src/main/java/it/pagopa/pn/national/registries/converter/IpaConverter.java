package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS05Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS23Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS05ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS23ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class IpaConverter {

    public static final String ADDRESS_TYPE = "PEC";

    public IPAPecDto convertToIPAPecDtoFromWS05(WS05ResponseDto ws05ResponseDto) {
        IPAPecDto response = new IPAPecDto();
        DataWS05Dto dataWS05Dto = ws05ResponseDto.getData();
        if(Objects.nonNull(dataWS05Dto)) {
            response.setCodEnte(dataWS05Dto.getCodAmm());
            response.setDenominazione(dataWS05Dto.getDesAmm());
            response.setTipo(ADDRESS_TYPE);
            response.setDomicilioDigitale(dataWS05Dto.getMail1());
        }
        return response;
    }

    public IPAPecDto convertToIpaPecDtoFromWS23(WS23ResponseDto ws23ResponseDto) {
        IPAPecDto ipaPecDto = new IPAPecDto();
        if (Objects.nonNull(ws23ResponseDto.getData()) &&
                !ws23ResponseDto.getData().isEmpty()) {
            DataWS23Dto dataWS23Dto = ws23ResponseDto.getData().getFirst();
            ipaPecDto.setCodEnte(dataWS23Dto.getCodAmm());
            ipaPecDto.setDenominazione(dataWS23Dto.getDesAmm());
            ipaPecDto.setTipo(dataWS23Dto.getTipo());
            ipaPecDto.setDomicilioDigitale(dataWS23Dto.getDomicilioDigitale());
        }
        return ipaPecDto;
    }
}
