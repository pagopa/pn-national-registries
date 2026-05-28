package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.EService;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.service.GatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
@lombok.CustomLog
public class DigitalAddressUtils {
    private final InfoCamereConverter infoCamereConverter;
    private final GatewayService gatewayService;

    private void populateBatchRequestSendFields(BatchRequest batchRequest, BatchStatus status, LocalDateTime now, CodeSqsDto codeSqsDto) {
        removeInvalidEmails(codeSqsDto);
        batchRequest.setMessage(gatewayService.convertCodeSqsDtoToString(codeSqsDto));
        batchRequest.setEservice(EService.INIPEC.name());
        batchRequest.setStatus(status.getValue());
        batchRequest.setSendStatus(BatchSendStatus.NOT_SENT.getValue());
        batchRequest.setLastReserved(now);
    }

    public static void removeInvalidEmails(CodeSqsDto sqsDto) {
        List<DigitalAddress> digitalAddresses = new ArrayList<>();
        if (!CollectionUtils.isEmpty(sqsDto.getDigitalAddress())) {
            digitalAddresses = sqsDto.getDigitalAddress().stream()
                    .filter(digitalAddress -> CheckEmailUtils.isValidEmail(digitalAddress.getAddress()))
                    .toList();
        }
        sqsDto.setDigitalAddress(digitalAddresses);
    }

    public Mono<BatchRequest> buildErrorBatchRequest(BatchStatus status, String error, BatchRequest batchRequest, LocalDateTime now) {
        CodeSqsDto sqsDto = infoCamereConverter.convertIniPecRequestToSqsDto(batchRequest, error);
        populateBatchRequestSendFields(batchRequest, status, now, sqsDto);
        return Mono.just(batchRequest);
    }
}
