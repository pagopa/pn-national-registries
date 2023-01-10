package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AgenziaEntrateService {

    private final AgenziaEntrateConverter agenziaEntrateConverter;
    private final CheckCfClient checkCfClient;

    public AgenziaEntrateService(AgenziaEntrateConverter agenziaEntrateConverter,
                                 CheckCfClient checkCfClient) {
        this.checkCfClient = checkCfClient;
        this.agenziaEntrateConverter = agenziaEntrateConverter;
    }

    public Mono<CheckTaxIdOKDto> callEService(CheckTaxIdRequestBodyDto request) {
        return checkCfClient.callEService(createRequest(request))
                .map(agenziaEntrateConverter::convertToCfStatusDto);
    }

    private Request createRequest(CheckTaxIdRequestBodyDto taxCodeRequestDto) {
        Request richiesta = new Request();
        richiesta.setCodiceFiscale(taxCodeRequestDto.getFilter().getTaxId());
        return richiesta;
    }
}
