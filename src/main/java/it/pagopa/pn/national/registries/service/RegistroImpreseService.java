package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inipec.IniPecClient;
import it.pagopa.pn.national.registries.converter.IniPecConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RegistroImpreseService {

    private final IniPecClient inipecClient;
    private final IniPecConverter iniPecConverter;

    public RegistroImpreseService(IniPecClient inipecClient, IniPecConverter iniPecConverter) {
        this.inipecClient = inipecClient;
        this.iniPecConverter = iniPecConverter;
    }

    public Mono<GetAddressRegistroImpreseOKDto> getAddress(GetAddressRegistroImpreseRequestBodyDto request) {
        return inipecClient.getLegalAddress(request.getFilter().getTaxId())
                .map(iniPecConverter::mapToResponseOk);
    }
}
