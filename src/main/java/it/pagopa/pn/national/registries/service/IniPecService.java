package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.IniPecConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
public class IniPecService {

    private final IniPecConverter iniPecConverter;
    private final IniPecBatchRequestRepository iniPecBatchRequestRepository;

    public IniPecService(IniPecConverter iniPecConverter,
                         IniPecBatchRequestRepository iniPecBatchRequestRepository) {
        this.iniPecConverter = iniPecConverter;
        this.iniPecBatchRequestRepository = iniPecBatchRequestRepository;
    }

    public Mono<GetDigitalAddressIniPECOKDto> getDigitalAddress(GetDigitalAddressIniPECRequestBodyDto getDigitalAddressIniPECRequestBodyDto) {
        return createBatchRequestByCf(getDigitalAddressIniPECRequestBodyDto)
                .doOnNext(batchRequest -> log.info("Created Batch Request for taxId: {}",getDigitalAddressIniPECRequestBodyDto.getFilter().getTaxId()))
                .map(iniPecConverter::convertToGetAddressIniPecOKDto);
    }

    public Mono<BatchRequest> createBatchRequestByCf(GetDigitalAddressIniPECRequestBodyDto requestCf) {
        BatchRequest batchRequest = createNewStartBatchRequest();
        batchRequest.setCorrelationId(requestCf.getFilter().getCorrelationId());
        batchRequest.setCf(requestCf.getFilter().getTaxId());
        return iniPecBatchRequestRepository.createBatchRequest(batchRequest);
    }

    private BatchRequest createNewStartBatchRequest(){
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
        batchRequest.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchRequest.setRetry(0);
        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setTimeStamp(LocalDateTime.now());
        return batchRequest;
    }
}
