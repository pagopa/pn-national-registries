package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.client.AnprClient;
import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.api.E002ServiceApi;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.GetAddressANPRRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.utils.anpr.AddressAnprUtils.createRequest;
import static it.pagopa.pn.national.registries.utils.anpr.AddressAnprUtils.mapToResponseOk;

@Service
@Slf4j
public class AnprService extends AnprClient {

    public AnprService(AccessTokenExpiringMap accessTokenExpiringMap,
                       @Value("${pdnd.c001.purpose-id}") String purposeId,
                       @Value("${pdnd.anpr.base-path}") String anprBasePath) {
        super(accessTokenExpiringMap, purposeId, anprBasePath);
    }

    public Mono<GetAddressANPROKDto> getAddressANPR(Mono<GetAddressANPRRequestBodyDto> request) {
        return super.getApiClient().flatMap(apiClient -> {
            E002ServiceApi e002ServiceApi = new E002ServiceApi(apiClient);
            return callEService(e002ServiceApi, request);
        });
    }

    private Mono<GetAddressANPROKDto> callEService(E002ServiceApi e002ServiceApi, Mono<GetAddressANPRRequestBodyDto> request) {
        return request.flatMap(item -> {
            log.info("call PostVerificaCodiceFiscale with cf: {}", item.getFilter().getTaxId());
            return (e002ServiceApi.e002(createRequest(item)).map(rispostaE002OKDto -> mapToResponseOk(rispostaE002OKDto,item.getFilter().getTaxId())));
        });
    }
}
