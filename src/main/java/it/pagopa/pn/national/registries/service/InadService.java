package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.converter.DigitalAddressInadConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class InadService{

    private final InadClient inadClient;

    public InadService(InadClient inadClient) {
        this.inadClient = inadClient;
    }

    public Mono<GetDigitalAddressINADOKDto> callEService(GetDigitalAddressINADRequestBodyDto request) {
            return inadClient.callEService(request.getFilter().getTaxId(), request.getFilter().getPracticalReference())
                    .map(DigitalAddressInadConverter::mapToResponseOk);
    }
}
