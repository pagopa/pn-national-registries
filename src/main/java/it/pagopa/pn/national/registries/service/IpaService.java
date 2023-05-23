package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.converter.IpaConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecErrorDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.model.ipa.ResultDto;
import it.pagopa.pn.national.registries.model.ipa.WS05ResponseDto;
import it.pagopa.pn.national.registries.model.ipa.WS23ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Predicate;

@Service
@Slf4j
public class IpaService {

    private final IpaConverter ipaConverter;
    private final IpaClient ipaClient;
    private final Predicate<Throwable> isResponseDataEmpty = throwable ->
            throwable instanceof PnNationalRegistriesException exception
                    && exception.getStatusCode() == HttpStatus.NOT_FOUND
                    && (Objects.requireNonNull(exception.getMessage()).equalsIgnoreCase("Service WS23 responded with 0 items - IPA PEC not found")
                    || Objects.requireNonNull(exception.getMessage()).equalsIgnoreCase("Service WS05 responded with 0 items - IPA PEC not found"));


    public IpaService(IpaConverter ipaConverter, IpaClient ipaClient) {
        this.ipaConverter = ipaConverter;
        this.ipaClient = ipaClient;
    }

    public Mono<IPAPecDto> getIpaPec(IPARequestBodyDto request) {
        return callWS23(request.getFilter().getTaxId())
                .flatMap(ws23ResponseDto -> {
                    if (ws23ResponseDto.getResult().getNumItems() > 1) {
                        String codAmm = ws23ResponseDto.getData().get(0).getCodEnte();
                        return callWS05(codAmm).map(ipaConverter::convertToIPAPecDtoFromWS05);
                    } else {
                        return Mono.just(ipaConverter.convertToIpaPecDtoFromWS23(ws23ResponseDto));
                    }
                })
                .onErrorResume(isResponseDataEmpty, throwable -> {
                    IPAPecDto emptyIpaPecDto = new IPAPecDto();
                    return Mono.just(emptyIpaPecDto);
                })
                .doOnError(throwable -> log.error("Error while calling IPA service", throwable));
    }

    private Mono<WS23ResponseDto> callWS23(String cf) {
        return ipaClient.callEServiceWS23(cf)
                .map(ws23ResponseDto -> {
                    checkErrorWsResultDto(ws23ResponseDto.getResult());
                    checkNumItemsResultDto(ws23ResponseDto.getResult(), "WS23");
                    return ws23ResponseDto;
                });
    }

    private Mono<WS05ResponseDto> callWS05(String codAmm) {
        return ipaClient.callEServiceWS05(codAmm)
                .map(ws05ResponseDto -> {
                    checkErrorWsResultDto(ws05ResponseDto.getResult());
                    checkNumItemsResultDto(ws05ResponseDto.getResult(), "WS05");
                    return ws05ResponseDto;
                });
    }

    private void checkErrorWsResultDto(ResultDto resultDto) {
        if (resultDto.getCodError() != 0) {
            throw new PnNationalRegistriesException(resultDto.getDescError(), HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null,
                    Charset.defaultCharset(), IPAPecErrorDto.class);
        }
    }

    private void checkNumItemsResultDto(ResultDto resultDto, String service){
        if (resultDto.getNumItems() == 0) {
            throw new PnNationalRegistriesException("Service " + service + " responded with 0 items - IPA PEC not found", HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(), null, null,
                    Charset.defaultCharset(), IPAPecErrorDto.class);
        }
    }
}
