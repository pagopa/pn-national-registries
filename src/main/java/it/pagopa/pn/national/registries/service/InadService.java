package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.converter.InadConverter;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.utils.FeatureEnabledUtils;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_NAME_INAD_ADDRESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class InadService{

    private final InadClient inadClient;

    private final ValidateTaxIdUtils validateTaxIdUtils;
    private final FeatureEnabledUtils featureEnabledUtils;


    public Mono<GetDigitalAddressINADOKDto> callEService(GetDigitalAddressINADRequestBodyDto request, RecipientType recipientType, Instant referenceRequestDate) {
        validateTaxIdUtils.validateTaxId(request.getFilter().getTaxId(), PROCESS_NAME_INAD_ADDRESS, false);
        boolean newWorkflowEnabled = Objects.nonNull(referenceRequestDate) && featureEnabledUtils.isPfNewWorkflowEnabled(referenceRequestDate);
        return inadClient.callEService(request.getFilter().getTaxId(), request.getFilter().getPracticalReference())
                .map(responseRequestDigitalAddressDto -> InadConverter.mapToResponseOk(responseRequestDigitalAddressDto, recipientType, request.getFilter().getTaxId(), newWorkflowEnabled));
    }
}
