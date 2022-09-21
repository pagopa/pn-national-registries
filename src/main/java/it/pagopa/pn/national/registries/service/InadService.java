package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.client.InadClient;
import it.pagopa.pn.national.registries.generated.openapi.inad.client.v1.api.ApiEstrazioniPuntualiApi;
import it.pagopa.pn.national.registries.converter.DigitalAddressInadConverter;
import it.pagopa.pn.national.registries.generated.openapi.server.inad.extract.cf.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.inad.extract.cf.v1.dto.GetDigitalAddressINADRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.converter.DigitalAddressInadConverter.mapToResponseOk;


@Service
@Slf4j
public class InadService extends InadClient {

    public InadService(AccessTokenExpiringMap accessTokenExpiringMap,
                       @Value("${pdnd.c001.purpose-id}") String purposeId,
                       @Value("${pdnd.inad.base-path}") String inadBasePath) {
        super(accessTokenExpiringMap, purposeId, inadBasePath);
    }


    public Mono<GetDigitalAddressINADOKDto> getDigitalAddress(GetDigitalAddressINADRequestBodyDto request) {
        return super.getApiClient().flatMap(apiClient -> {
            ApiEstrazioniPuntualiApi apiEstrazioniPuntualiApi = new ApiEstrazioniPuntualiApi(apiClient);
            return callEService(apiEstrazioniPuntualiApi, request);
        });
    }


    private Mono<GetDigitalAddressINADOKDto> callEService(ApiEstrazioniPuntualiApi apiEstrazioniPuntualiApi, GetDigitalAddressINADRequestBodyDto request) {
            log.info("call Post ExtractDigitalAddress for CodiceFiscale with cf: {}", request.getFilter().getTaxId());
            return (apiEstrazioniPuntualiApi.recuperoDomicilioDigitale(
                        request.getFilter().getTaxId(),
                        request.getFilter().getPracticalReference())
                    .map(responseRequestDigitalAddressDto -> mapToResponseOk(responseRequestDigitalAddressDto)));
    }
}
