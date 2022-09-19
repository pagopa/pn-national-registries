package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.client.CheckCfClient;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.api.VerificheApi;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.dto.Richiesta;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.dto.VerificaCodiceFiscale;
import it.pagopa.pn.national.registries.generated.openapi.server.check.cf.v1.dto.CheckCodiceFiscaleOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.check.cf.v1.dto.CheckCodiceFiscaleRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CheckCfService extends CheckCfClient{

    public CheckCfService(AccessTokenExpiringMap accessTokenExpiringMap,
                       @Value("${pdnd.b001.purpose-id}") String purposeId,
                       @Value("${pdnd.agenzia-entrate.base-path}") String anprBasePath) {
        super(accessTokenExpiringMap, purposeId, anprBasePath);
    }

    public Mono<CheckCodiceFiscaleOKDto> getCfStatus(Mono<CheckCodiceFiscaleRequestBodyDto> request) {
        return super.getApiClient().map(apiClient -> {
            VerificheApi verificheApi = new VerificheApi(apiClient);
            return callPostVerificaCodiceFiscale(request,verificheApi);
        });
    }

    private CheckCodiceFiscaleOKDto callPostVerificaCodiceFiscale(Mono<CheckCodiceFiscaleRequestBodyDto> request, VerificheApi verificheApi) {
        request.map(item -> {
            Richiesta richiesta = createRequest(item);
            return verificheApi.postVerificaCodiceFiscale(richiesta).map(this::mapToCfStatusDto);
        });
        return null;
    }

    private Richiesta createRequest(CheckCodiceFiscaleRequestBodyDto taxCodeRequestDto) {
        Richiesta richiesta = new Richiesta();
        richiesta.setCodiceFiscale(taxCodeRequestDto.getFilter().getCodiceFiscale());
        return richiesta;
    }

    private CheckCodiceFiscaleOKDto mapToCfStatusDto(VerificaCodiceFiscale s) {
        CheckCodiceFiscaleOKDto cfStatusDto = new CheckCodiceFiscaleOKDto();
        cfStatusDto.setValido(String.valueOf(s.getValido()));
        cfStatusDto.setMessaggio(s.getMessaggio());
        cfStatusDto.setCodiceFiscale(s.getCodiceFiscale());
        return cfStatusDto;
    }
}
