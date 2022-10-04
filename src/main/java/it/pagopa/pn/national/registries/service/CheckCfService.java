package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.checkcf.CheckCfClient;
import it.pagopa.pn.national.registries.converter.CheckCfConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.model.checkcf.Richiesta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CheckCfService {

    private final CheckCfConverter checkCfConverter;
    private final CheckCfClient checkCfClient;

    public CheckCfService(CheckCfConverter checkCfConverter,
                          CheckCfClient checkCfClient) {
        this.checkCfClient = checkCfClient;
        this.checkCfConverter = checkCfConverter;
    }

    public Mono<CheckTaxIdOKDto> callEService(CheckTaxIdRequestBodyDto request) {
        return checkCfClient.callEService(createRequest(request))
                .map(checkCfConverter::convertToCfStatusDto);
    }

    private Richiesta createRequest(CheckTaxIdRequestBodyDto taxCodeRequestDto) {
        Richiesta richiesta = new Richiesta();
        richiesta.setCodiceFiscale(taxCodeRequestDto.getFilter().getTaxId());
        return richiesta;
    }
}
