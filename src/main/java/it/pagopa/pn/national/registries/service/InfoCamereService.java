package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.AddressRegistroImprese;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.InfoCamereLegalInstituionsResponse;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressRegistroImpreseRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.InfoCamereLegalInstitutionsRequestBodyDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_CHEKING_INFO_CAMERE_LEGAL_INSTITUTIONS;

@Service
@lombok.CustomLog
public class InfoCamereService {

    private final InfoCamereClient infoCamereClient;
    public InfoCamereService(InfoCamereClient infoCamereClient) {
        this.infoCamereClient = infoCamereClient;
    }

    public Mono<AddressRegistroImprese> getRegistroImpreseLegalAddress(GetAddressRegistroImpreseRequestBodyDto request) {
        String cf = request.getFilter().getTaxId();
        return infoCamereClient.getLegalAddress(cf)
                .doOnError(throwable -> log.info("Failed to get Legal Address"));
    }

    public Mono<InfoCamereLegalInstituionsResponse> getLegalInstitutions(InfoCamereLegalInstitutionsRequestBodyDto infoCamereLegalInstitutionsRequestBodyDto) {
        log.logChecking(PROCESS_CHEKING_INFO_CAMERE_LEGAL_INSTITUTIONS);

        return infoCamereClient.getLegalInstitutions(infoCamereLegalInstitutionsRequestBodyDto.getFilter())
                .doOnNext(infoCamereLegalInstitutions -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL_INSTITUTIONS,true))
                .doOnError(throwable -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL_INSTITUTIONS,false,throwable.getMessage()));
    }
}
