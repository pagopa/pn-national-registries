package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
public class InfoCamereService {

    private final InfoCamereClient infoCamereClient;
    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository iniPecBatchRequestRepository;
    private final Long iniPecTtl;

    public InfoCamereService(InfoCamereClient infoCamereClient,
                             InfoCamereConverter infoCamereConverter,
                             IniPecBatchRequestRepository iniPecBatchRequestRepository,
                             @Value("${pn.national.registries.inipec.ttl}") Long iniPecTtl) {
        this.infoCamereClient = infoCamereClient;
        this.infoCamereConverter = infoCamereConverter;
        this.iniPecBatchRequestRepository = iniPecBatchRequestRepository;
        this.iniPecTtl = iniPecTtl;
    }

    public Mono<GetDigitalAddressIniPECOKDto> getIniPecDigitalAddress(String pnNationalRegistriesCxId, GetDigitalAddressIniPECRequestBodyDto getDigitalAddressIniPECRequestBodyDto) {
        return createBatchRequestByCf(pnNationalRegistriesCxId, getDigitalAddressIniPECRequestBodyDto)
                .doOnNext(batchRequest -> log.info("Created Batch Request for taxId: {} and correlationId: {}", MaskDataUtils.maskString(getDigitalAddressIniPECRequestBodyDto.getFilter().getTaxId()),getDigitalAddressIniPECRequestBodyDto.getFilter().getCorrelationId()))
                .doOnError(throwable -> log.info("Failed to create Batch Request for taxId: {} and correlationId: {}", MaskDataUtils.maskString(getDigitalAddressIniPECRequestBodyDto.getFilter().getTaxId()),getDigitalAddressIniPECRequestBodyDto.getFilter().getCorrelationId()))
                .map(infoCamereConverter::convertToGetAddressIniPecOKDto);
    }

    public Mono<GetAddressRegistroImpreseOKDto> getRegistroImpreseLegalAddress(GetAddressRegistroImpreseRequestBodyDto request) {
        return infoCamereClient.getLegalAddress(request.getFilter().getTaxId())
                .doOnNext(address -> log.info("Got Legal Address for taxId: {}", MaskDataUtils.maskString(request.getFilter().getTaxId())))
                .doOnError(throwable -> log.info("Failed to get Legal Address for taxId: {}", MaskDataUtils.maskString(request.getFilter().getTaxId())))
                .map(infoCamereConverter::mapToResponseOk);
    }

    public Mono<BatchRequest> createBatchRequestByCf(String pnNationalRegistriesCxId, GetDigitalAddressIniPECRequestBodyDto requestCf) {
        BatchRequest batchRequest = createNewStartBatchRequest();
        batchRequest.setCorrelationId(requestCf.getFilter().getCorrelationId());
        batchRequest.setCf(requestCf.getFilter().getTaxId());
        batchRequest.setClientId(pnNationalRegistriesCxId);
        return iniPecBatchRequestRepository.createBatchRequest(batchRequest);
    }

    private BatchRequest createNewStartBatchRequest() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
        batchRequest.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchRequest.setRetry(0);
        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setTimeStamp(LocalDateTime.now());
        batchRequest.setTtl(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(iniPecTtl).toEpochSecond(ZoneOffset.UTC));
        return batchRequest;
    }

    public Mono<InfoCamereLegalOKDto> checkTaxIdAndVatNumber(InfoCamereLegalRequestBodyDto request) {
        return infoCamereClient.checkTaxIdAndVatNumberInfoCamere(request.getFilter())
                .map(infoCamereConverter::infoCamereResponseToDto);
    }
}
