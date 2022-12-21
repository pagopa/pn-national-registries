package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
public class InfoCamereService {

    private final InfoCamereClient infoCamereClient;
    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository iniPecBatchRequestRepository;

    public InfoCamereService(InfoCamereClient infoCamereClient, InfoCamereConverter infoCamereConverter, IniPecBatchRequestRepository iniPecBatchRequestRepository) {
        this.infoCamereClient = infoCamereClient;
        this.infoCamereConverter = infoCamereConverter;
        this.iniPecBatchRequestRepository = iniPecBatchRequestRepository;
    }

    public Mono<GetDigitalAddressIniPECOKDto> getIniPecDigitalAddress(GetDigitalAddressIniPECRequestBodyDto getDigitalAddressIniPECRequestBodyDto) {
        return createBatchRequestByCf(getDigitalAddressIniPECRequestBodyDto)
                .doOnNext(batchRequest -> log.info("Created Batch Request for taxId: {}",getDigitalAddressIniPECRequestBodyDto.getFilter().getTaxId()))
                .map(infoCamereConverter::convertToGetAddressIniPecOKDto);
    }

    public Mono<GetAddressRegistroImpreseOKDto> getRegistroImpreseLegalAddress(GetAddressRegistroImpreseRequestBodyDto request) {
        return infoCamereClient.getLegalAddress(request.getFilter().getTaxId())
                .map(infoCamereConverter::mapToResponseOk);
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

    public Mono<InfoCamereLegalOKDto> checkTaxIdAndVatNumber(InfoCamereLegalRequestBodyDto request) {
        return infoCamereClient.checkTaxIdAndVatNumberInfoCamere(request.getFilter())
                .map(infoCamereConverter::infoCamereResponseToDto);
    }
}
