package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.converter.IpaConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecErrorDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.model.ipa.ResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.List;

@Service
@Slf4j
public class IpaService {

    private final IpaConverter ipaConverter;
    private final IpaClient ipaClient;

    public IpaService(IpaConverter ipaConverter,
                      IpaClient ipaClient) {
        this.ipaConverter = ipaConverter;
        this.ipaClient = ipaClient;
    }

    public Mono<IPAPecOKDto> getIpaPec(IPARequestBodyDto request) {
        return ipaClient.callEServiceWS23(request.getFilter().getTaxId())
                .map(ws23ResponseDto -> {
                    checkErrorWsResultDto(ws23ResponseDto.getResult());
                    return ipaConverter.convertToIPAPecOKDto(ws23ResponseDto);
                });
    }

    private void checkErrorWsResultDto(ResultDto resultDto){
        if(resultDto.getNumItems()<1 && resultDto.getCodError()!=0){
            throw new PnNationalRegistriesException(resultDto.getDescError(), HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null,
                    Charset.defaultCharset(), IPAPecErrorDto.class);
        }
    }

}
