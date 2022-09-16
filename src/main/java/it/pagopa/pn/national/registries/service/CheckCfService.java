package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.PdndTokenBaseClient;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.api.VerificheApi;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.dto.Richiesta;
import it.pagopa.pn.national.registries.generated.openapi.agenzia_entrate.client.v1.dto.VerificaCodiceFiscale;
import it.pagopa.pn.national.registries.generated.openapi.server.check.cf.v1.dto.CheckCodiceFiscaleOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.check.cf.v1.dto.CheckCodiceFiscaleRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class CheckCfService extends PdndTokenBaseClient {

    private String purposeId;
    private VerificheApi verificheApi;

    protected CheckCfService(@Value("${pdnd.b001.purpose-id}") String purposeId) {
        super(purposeId);
    }

    @Value("${pdnd.agenzia-entrate.base-path}")
    String agenziaEntrateBasePath;

    @PostConstruct
    public void init() {
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(agenziaEntrateBasePath);
        this.verificheApi = new VerificheApi(apiClient);
    }

    public Mono<CheckCodiceFiscaleOKDto> getCfStatus(Mono<CheckCodiceFiscaleRequestBodyDto> request) {
        return request.flatMap(taxCodeRequestDto -> {
            Richiesta richiesta = createRequest(taxCodeRequestDto);
            return verificheApi.postVerificaCodiceFiscale(richiesta).map(this::mapToCfStatusDto);
        });
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
