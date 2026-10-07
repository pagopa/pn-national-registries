package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.config.ipa.IpaSecretConfig;
import it.pagopa.pn.national.registries.converter.IpaConverter;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS05ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS23ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_NAME_IPA_ADDRESS;

@Service
@lombok.CustomLog
public class IpaService {

    private final IpaConverter ipaConverter;
    private final IpaClient ipaClient;
    private final ValidateTaxIdUtils validateTaxIdUtils;
    private final PnNationalRegistriesSecretService pnNationalRegistriesSecretService;
    private final IpaSecretConfig ipaSecretConfig;


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

    public Mono<Object> getIpaPec(IPARequestBodyDto request) {
        validateTaxIdUtils.validateTaxId(request.getFilter().getTaxId(), PROCESS_NAME_IPA_ADDRESS, false);
        String authId = pnNationalRegistriesSecretService.getIpaSecret(ipaSecretConfig.getIpaSecret()).getAuthId();
        return callWS23(request.getFilter().getTaxId(), authId)
                .flatMap(ws23ResponseDto -> {
                    if (ws23ResponseDto.getResult() != null &&
                            ws23ResponseDto.getResult().getNumItems() != null &&
                            ws23ResponseDto.getResult().getNumItems() > 1 &&
                            ws23ResponseDto.getData() != null &&
                            !ws23ResponseDto.getData().isEmpty()) {
                        String codAmm = ws23ResponseDto.getData().getFirst().getCodAmm();
                        return callWS05(codAmm, authId).map(ipaConverter::convertToIPAPecDtoFromWS05);
                    } else {
                        return Mono.just(ws23ResponseDto);
                    }
                })
                .doOnError(throwable -> log.error("Error while calling IPA service", throwable));
    }

    private Mono<WS23ResponseDto> callWS23(String cf, String authId) {
        return ipaClient.callEServiceWS23(cf, authId)
                .doOnNext(ws23ResponseDto -> log.info("Got WS23Response"))
                .doOnError(throwable -> log.info("Failed to callWS23"));
    }

    private Mono<WS05ResponseDto> callWS05(String codAmm, String authId) {
        return ipaClient.callEServiceWS05(codAmm, authId)
                .doOnNext(ws05ResponseDto -> log.info("Got WS05Response for codAmm: {}", codAmm))
                .doOnError(throwable -> log.info("Failed to callWS05 for codAmm: {}", codAmm));
    }
}
