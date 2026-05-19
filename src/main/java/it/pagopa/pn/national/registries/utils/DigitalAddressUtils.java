package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.service.GatewayService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.national.registries.model.EService.INIPEC;

@Component
@lombok.CustomLog
public class DigitalAddressUtils {
    private final InfoCamereConverter infoCamereConverter;
    private final GatewayService gatewayService;

    private DigitalAddressUtils(InfoCamereConverter infoCamereConverter,
                                GatewayService gatewayService) {
        this.infoCamereConverter = infoCamereConverter;
        this.gatewayService = gatewayService;
    }

    public Mono<BatchRequest> updateBatchRequestFields(BatchRequest batchRequest, BatchStatus status, LocalDateTime now, Pec pec) {
        CodeSqsDto codeSqsDto = infoCamereConverter.convertResponsePecToCodeSqsDto(batchRequest, pec);
        removeInvalidEmails(codeSqsDto);
        batchRequest.setMessage(gatewayService.convertCodeSqsDtoToString(codeSqsDto));
        batchRequest.setEservice(INIPEC.name());
        batchRequest.setStatus(status.getValue());
        batchRequest.setSendStatus(BatchSendStatus.NOT_SENT.getValue());
        batchRequest.setLastReserved(now);
        return Mono.just(batchRequest);
    }

    private static void removeInvalidEmails(CodeSqsDto sqsDto) {
        List<DigitalAddress> digitalAddresses = new ArrayList<>();
        if (!CollectionUtils.isEmpty(sqsDto.getDigitalAddress())) {
            digitalAddresses = sqsDto.getDigitalAddress().stream()
                    .filter(digitalAddress -> CheckEmailUtils.isValidEmail(digitalAddress.getAddress()))
                    .toList();
        }
        sqsDto.setDigitalAddress(digitalAddresses);
    }

    public Mono<BatchRequest> buildErrorBatchRequest(BatchStatus status, String error, BatchRequest batchRequest) {
        CodeSqsDto sqsDto = infoCamereConverter.convertIniPecRequestToSqsDto(batchRequest, error);
        batchRequest.setMessage(gatewayService.convertCodeSqsDtoToString(sqsDto));
        batchRequest.setEservice(INIPEC.name());
        batchRequest.setStatus(status.getValue());
        return Mono.just(batchRequest);
    }
}
