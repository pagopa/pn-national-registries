package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.converter.InadConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_NAME_INAD_ADDRESS;

@Service
@Slf4j
public class InadService {

    private final InadClient inadClient;

    private final ValidateTaxIdUtils validateTaxIdUtils;

    public InadService(InadClient inadClient, ValidateTaxIdUtils validateTaxIdUtils) {
        this.inadClient = inadClient;
        this.validateTaxIdUtils = validateTaxIdUtils;
    }

    public Mono<GetDigitalAddressINADOKDto> callEService(GetDigitalAddressINADRequestBodyDto request) {
        validateTaxIdUtils.validateTaxId(request.getFilter().getTaxId(), PROCESS_NAME_INAD_ADDRESS);
        return inadClient.callEService(request.getFilter().getTaxId(), request.getFilter().getPracticalReference())
                .map(InadConverter::mapToResponseOk);
    }
}
