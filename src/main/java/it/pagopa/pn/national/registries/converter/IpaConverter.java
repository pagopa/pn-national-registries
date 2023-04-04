package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecOKDto;
import it.pagopa.pn.national.registries.model.ipa.DataWS23Dto;
import it.pagopa.pn.national.registries.model.ipa.WS23ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class IpaConverter {

    public IPAPecOKDto convertToIPAPecOKDto(WS23ResponseDto ws23ResponseDto) {
        IPAPecOKDto response = new IPAPecOKDto();
        List<IPAPecDto> ipaPecDtoList = Collections.emptyList();
        if (ws23ResponseDto.getData() != null) {
            ipaPecDtoList = ws23ResponseDto.getData().stream()
                    .map(this::convertToIpaPecDto)
                    .toList();
        }
        response.setDomiciliDigitali(ipaPecDtoList);
        return response;
    }

    private IPAPecDto convertToIpaPecDto(DataWS23Dto dataWS23Dto) {
        IPAPecDto ipaPecDto = new IPAPecDto();
        ipaPecDto.setCodEnte(dataWS23Dto.getCodEnte());
        ipaPecDto.setDenominazione(dataWS23Dto.getDenominazione());
        ipaPecDto.setTipo(dataWS23Dto.getType());
        ipaPecDto.setDomicilioDigitale(dataWS23Dto.getDomicilioDigitale());
        return ipaPecDto;
    }
}
