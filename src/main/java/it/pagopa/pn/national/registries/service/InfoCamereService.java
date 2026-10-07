package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.AddressRegistroImprese;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.InfoCamereLegalInstituionsResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.InfoCamereVerification;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressRegistroImpreseRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.InfoCamereLegalInstitutionsRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.InfoCamereLegalRequestBodyDto;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.*;

@Service
@lombok.CustomLog
public class InfoCamereService {

    private final InfoCamereClient infoCamereClient;
    private final ValidateTaxIdUtils validateTaxIdUtils;

    public InfoCamereService(InfoCamereClient infoCamereClient,
                             ValidateTaxIdUtils validateTaxIdUtils) {
        this.infoCamereClient = infoCamereClient;
        this.validateTaxIdUtils = validateTaxIdUtils;
    }

    public Mono<AddressRegistroImprese> getRegistroImpreseLegalAddress(GetAddressRegistroImpreseRequestBodyDto request) {
        String cf = request.getFilter().getTaxId();
        validateTaxIdUtils.validateTaxId(cf, PROCESS_NAME_REGISTRO_IMPRESE_ADDRESS, false);
        return infoCamereClient.getLegalAddress(cf)
                .doOnError(throwable -> log.info("Failed to get Legal Address"));
    }

    public Mono<InfoCamereLegalInstituionsResponse> getLegalInstitutions(InfoCamereLegalInstitutionsRequestBodyDto infoCamereLegalInstitutionsRequestBodyDto) {
        log.logChecking(PROCESS_CHEKING_INFO_CAMERE_LEGAL_INSTITUTIONS);
        validateTaxIdUtils.validateTaxId(infoCamereLegalInstitutionsRequestBodyDto.getFilter().getTaxId(), PROCESS_NAME_INFO_CAMERE_LEGAL_INSTITUTIONS, false);

        return infoCamereClient.getLegalInstitutions(infoCamereLegalInstitutionsRequestBodyDto.getFilter())
                .doOnNext(infoCamereLegalInstitutions -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL_INSTITUTIONS,true))
                .doOnError(throwable -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL_INSTITUTIONS,false,throwable.getMessage()));
    }


    public Mono<InfoCamereVerification> checkTaxIdAndVatNumber(InfoCamereLegalRequestBodyDto request) {
        log.logChecking(PROCESS_CHEKING_INFO_CAMERE_LEGAL);

        validateTaxIdUtils.validateTaxId(request.getFilter().getTaxId(), PROCESS_NAME_INFO_CAMERE_LEGAL, false);

        return infoCamereClient.checkTaxIdAndVatNumberInfoCamere(request.getFilter())
                .doOnNext(infoCamereVerification -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL,true))
                .doOnError(throwable -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL,false,throwable.getMessage()));
    }
}
