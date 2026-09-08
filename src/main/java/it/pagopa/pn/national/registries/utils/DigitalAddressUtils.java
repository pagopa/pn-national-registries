package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressSQSMessageDigitalAddressInnerDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.national.registries.model.EService;
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

    public Mono<BatchRequest> updateBatchRequestFields(BatchRequest batchRequest, BatchStatus status, LocalDateTime now, Pec pec) {
        AddressSQSMessageDto codeSqsDto = infoCamereConverter.convertResponsePecToCodeSqsDto(batchRequest, pec);
        populateBatchRequestSendFields(batchRequest, status, now, codeSqsDto);
        return Mono.just(batchRequest);
    }

    private void populateBatchRequestSendFields(BatchRequest batchRequest, BatchStatus status, LocalDateTime now, AddressSQSMessageDto codeSqsDto) {
        removeInvalidEmails(codeSqsDto);
        batchRequest.setMessage(gatewayService.convertCodeSqsDtoToString(codeSqsDto));
        batchRequest.setEservice(EService.INIPEC.name());
        batchRequest.setStatus(status.getValue());
        batchRequest.setSendStatus(BatchSendStatus.NOT_SENT.getValue());
        batchRequest.setLastReserved(now);
    }

    private static void removeInvalidEmails(AddressSQSMessageDto sqsDto) {
        List<AddressSQSMessageDigitalAddressInnerDto> digitalAddresses = new ArrayList<>();
        if (!CollectionUtils.isEmpty(sqsDto.getDigitalAddress())) {
            digitalAddresses = sqsDto.getDigitalAddress().stream()
                    .filter(digitalAddress -> CheckEmailUtils.isValidEmail(digitalAddress.getAddress()))
                    .toList();
        }
        sqsDto.setDigitalAddress(digitalAddresses);
    }

    public Mono<BatchRequest> buildErrorBatchRequest(BatchStatus status, String error, BatchRequest batchRequest, LocalDateTime now) {
        AddressSQSMessageDto sqsDto = infoCamereConverter.convertIniPecRequestToSqsDto(batchRequest, error);
        populateBatchRequestSendFields(batchRequest, status, now, sqsDto);
        return Mono.just(batchRequest);
    }
}
