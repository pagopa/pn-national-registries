package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.config.ipa.IpaSecretConfig;
import it.pagopa.pn.national.registries.converter.IpaConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.ResultDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS05ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS23ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecErrorDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Predicate;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.*;

@Service
@lombok.CustomLog
public class IpaService {

    private final IpaConverter ipaConverter;
    private final IpaClient ipaClient;
    private final ValidateTaxIdUtils validateTaxIdUtils;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;
    private final IpaSecretConfig ipaSecretConfig;
    private final Predicate<Throwable> isResponseDataEmpty = throwable ->
            throwable instanceof PnNationalRegistriesException exception
                    && exception.getStatusCode() == HttpStatus.NOT_FOUND
                    && ("Service WS23 responded with 0 items - IPA PEC not found".equalsIgnoreCase(Objects.requireNonNull(exception.getMessage()))
                    || "Service WS05 responded with 0 items - IPA PEC not found".equalsIgnoreCase(Objects.requireNonNull(exception.getMessage())));


    public IpaService(IpaConverter ipaConverter,
                      IpaClient ipaClient,
                      ValidateTaxIdUtils validateTaxIdUtils,
                      PnNationalRegistriesSecretService pnNationalRegistriesSecretService,
                      IpaSecretConfig ipaSecretConfig) {
        this.ipaConverter = ipaConverter;
        this.ipaClient = ipaClient;
        this.validateTaxIdUtils = validateTaxIdUtils;
        this.pnNationalRegistriesSecretService = pnNationalRegistriesSecretService;
        this.ipaSecretConfig = ipaSecretConfig;
    }

    public Mono<IPAPecDto> getIpaPec(IPARequestBodyDto request) {
        validateTaxIdUtils.validateTaxId(request.getFilter().getTaxId(), PROCESS_NAME_IPA_ADDRESS, false);
        String authId = pnNationalRegistriesSecretService.getIpaSecret(ipaSecretConfig.getIpaSecret()).getAuthId();
        return callWS23(request.getFilter().getTaxId(), authId)
                .flatMap(ws23ResponseDto -> {
                    if (ws23ResponseDto.getResult().getNumItems() > 1) {
                        String codAmm = ws23ResponseDto.getData().get(0).getCodAmm();
                        return callWS05(codAmm, authId).map(ipaConverter::convertToIPAPecDtoFromWS05);
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

    private Mono<WS23ResponseDto> callWS23(String cf, String authId) {
        return ipaClient.callEServiceWS23(cf, authId)
                .doOnNext(ws23ResponseDto -> log.info("Got WS23Response"))
                .doOnError(throwable -> log.info("Failed to callWS23"))
                .map(ws23ResponseDto -> {
                    checkErrorWsResultDto(ws23ResponseDto.getResult());
                    checkNumItemsResultDto(ws23ResponseDto.getResult(), "WS23");
                    return ws23ResponseDto;
                });
    }

    private Mono<WS05ResponseDto> callWS05(String codAmm, String authId) {
        return ipaClient.callEServiceWS05(codAmm, authId)
                .doOnNext(ws05ResponseDto -> log.info("Got WS05Response for codAmm: {}", codAmm))
                .doOnError(throwable -> log.info("Failed to callWS05 for codAmm: {}", codAmm))
                .map(ws05ResponseDto -> {
                    checkErrorWsResultDto(ws05ResponseDto.getResult());
                    checkNumItemsResultDto(ws05ResponseDto.getResult(), "WS05");
                    return ws05ResponseDto;
                });
    }

    private void checkErrorWsResultDto(ResultDto resultDto) {
        log.logChecking(PROCESS_CHECKING_ERROR_IPA);
        if (resultDto.getCodErr() != 0) {
            log.logCheckingOutcome(PROCESS_CHECKING_ERROR_IPA,false,resultDto.getDescErr());
            throw new PnNationalRegistriesException(resultDto.getDescErr(), HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null,
                    Charset.defaultCharset(), IPAPecErrorDto.class);
        }
        log.logCheckingOutcome(PROCESS_CHECKING_ERROR_IPA,true);
    }

    private void checkNumItemsResultDto(ResultDto resultDto, String service){
        log.logChecking(PROCESS_CHECKING_ITEMS_IPA);
        if (resultDto.getNumItems() == 0) {
            log.logCheckingOutcome(PROCESS_CHECKING_ITEMS_IPA,false,resultDto.getDescErr());
            throw new PnNationalRegistriesException("Service " + service + " responded with 0 items - IPA PEC not found", HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(), null, null,
                    Charset.defaultCharset(), IPAPecErrorDto.class);
        }
        log.logCheckingOutcome(PROCESS_CHECKING_ITEMS_IPA,true);
    }
}
