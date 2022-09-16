package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.PdndTokenBaseClient;
import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.api.E002ServiceApi;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.ConsultaResidenzaANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.ConsultaResidenzaANPRRequestBodyDto;
import it.pagopa.pn.national.registries.utils.anpr.ResidenceMapperMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import javax.annotation.PostConstruct;
import static it.pagopa.pn.national.registries.utils.anpr.ResidenceMapperMethods.convertToRichiestaE002;

@Service
@Slf4j
public class AnprService extends PdndTokenBaseClient {

    private E002ServiceApi e002ServiceApi;
    private String purposeId;
    private final String anprBasePath;

    protected AnprService(@Value("${pdnd.c001.purpose-id}") String purposeId,
                          @Value("${pdnd.anpr.base-path}") String anprBasePath) {
        super(purposeId);
        this.anprBasePath = anprBasePath;
    }

    @PostConstruct
    public void init() {
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(anprBasePath);
        this.e002ServiceApi = new E002ServiceApi(apiClient);
    }

    public Mono<ConsultaResidenzaANPROKDto> getResidence(Mono<ConsultaResidenzaANPRRequestBodyDto> richiestaE002Dto) {
        return richiestaE002Dto.flatMap(useCaseRequestDto ->
                e002ServiceApi.e002(convertToRichiestaE002(useCaseRequestDto))
                        .flatMap(ResidenceMapperMethods::mapToResponseOk));
    }
}
