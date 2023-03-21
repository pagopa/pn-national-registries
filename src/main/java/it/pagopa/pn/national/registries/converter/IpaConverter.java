package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecOKDto;
import it.pagopa.pn.national.registries.model.ipa.WS23ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IpaConverter {


    public IPAPecOKDto convertToIPAPecOKDto(WS23ResponseDto ws23ResponseDto) {
        IPAPecOKDto response = new IPAPecOKDto();
        response.setCodEnte(ws23ResponseDto.getData().getCodEnte());
        response.setDenominazione(ws23ResponseDto.getData().getDenominazione());
        response.setTipo(ws23ResponseDto.getData().getType());
        response.setDomicilioDigitale(ws23ResponseDto.getData().getDomicilioDigitale());
        return response;
    }

}
