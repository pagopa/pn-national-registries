package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inad.EstrazioniPuntualiApiCustom;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.generated.openapi.inad.client.v1.api.ApiEstrazioniPuntualiApi;
import it.pagopa.pn.national.registries.converter.DigitalAddressInadConverter;
import it.pagopa.pn.national.registries.generated.openapi.server.inad.extract.cf.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.inad.extract.cf.v1.dto.GetDigitalAddressINADRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;



@Service
@Slf4j
public class InadService{

    private final EstrazioniPuntualiApiCustom estrazioniPuntualiApiCustom;
    private final InadClient inadClient;

    public InadService(EstrazioniPuntualiApiCustom estrazioniPuntualiApiCustom,
                       InadClient inadClient) {
        this.estrazioniPuntualiApiCustom = estrazioniPuntualiApiCustom;
        this.inadClient = inadClient;
    }


    public Mono<GetDigitalAddressINADOKDto> getDigitalAddress(GetDigitalAddressINADRequestBodyDto request) {
        return inadClient.getApiClient().flatMap(client -> {
            estrazioniPuntualiApiCustom.setApiClient(client);
            return callEService(estrazioniPuntualiApiCustom, request);
        });
    }


    private Mono<GetDigitalAddressINADOKDto> callEService(ApiEstrazioniPuntualiApi apiEstrazioniPuntualiApi, GetDigitalAddressINADRequestBodyDto request) {
            log.info("call Post ExtractDigitalAddress for CodiceFiscale with cf: {}", request.getFilter().getTaxId());
            return (apiEstrazioniPuntualiApi.recuperoDomicilioDigitale(request.getFilter().getTaxId(), request.getFilter().getPracticalReference())
                    .map(DigitalAddressInadConverter::mapToResponseOk));
    }
}
