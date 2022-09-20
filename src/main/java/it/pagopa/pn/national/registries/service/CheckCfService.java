package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.client.CheckCfClient;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.api.VerificheApi;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.dto.VerificaCodiceFiscale;
import it.pagopa.pn.national.registries.generated.openapi.server.check.cf.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.check.cf.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.utils.taxcode.CheckCfUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.utils.taxcode.CheckCfUtils.createRequest;

@Component
@Slf4j
public class CheckCfService extends CheckCfClient {

    public CheckCfService(AccessTokenExpiringMap accessTokenExpiringMap,
                          @Value("${pdnd.b001.purpose-id}") String purposeId,
                          @Value("${pdnd.agenzia-entrate.base-path}") String anprBasePath) {
        super(accessTokenExpiringMap, purposeId, anprBasePath);
    }

    public Mono<CheckTaxIdOKDto> getCfStatus(Mono<CheckTaxIdRequestBodyDto> request) {
        return super.getApiClient().flatMap(apiClient -> {
            VerificheApi verificheApi = new VerificheApi(apiClient);
            return callEService(verificheApi, request).map(CheckCfUtils::mapToCfStatusDto);
        });
    }

    private Mono<VerificaCodiceFiscale> callEService(VerificheApi verificheApi, Mono<CheckTaxIdRequestBodyDto> request) {
        return request.flatMap(item -> {
            log.info("call PostVerificaCodiceFiscale with cf: {}", item.getFilter().getTaxId());
            return (verificheApi.postVerificaCodiceFiscale(createRequest(item)));
        });
    }
}
