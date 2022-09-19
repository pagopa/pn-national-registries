package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.client.AnprClient;
import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.api.E002ServiceApi;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.ConsultaResidenzaANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.ConsultaResidenzaANPRRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AnprService extends AnprClient {

    public AnprService(AccessTokenExpiringMap accessTokenExpiringMap,
                       @Value("${pdnd.c001.purpose-id}") String purposeId,
                       @Value("${pdnd.anpr.base-path}") String anprBasePath) {
        super(accessTokenExpiringMap, purposeId, anprBasePath);
    }

    public Mono<ConsultaResidenzaANPROKDto> getResidence(Mono<ConsultaResidenzaANPRRequestBodyDto> richiestaE002Dto) {
        return super.getApiClient().map(apiClient -> {
            E002ServiceApi e002ServiceApi = new E002ServiceApi(apiClient);
            return callToE002Service(richiestaE002Dto, e002ServiceApi);
        });
    }

    private ConsultaResidenzaANPROKDto callToE002Service(Mono<ConsultaResidenzaANPRRequestBodyDto> richiestaE002Dto, E002ServiceApi e002ServiceApi) {
      return new ConsultaResidenzaANPROKDto();
    }
}
