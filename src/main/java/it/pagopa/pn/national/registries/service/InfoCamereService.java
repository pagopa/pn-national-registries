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

    public Mono<GetDigitalAddressIniPECOKDto> getIniPecDigitalAddress(String pnNationalRegistriesCxId, GetDigitalAddressIniPECRequestBodyDto dto) {
        String cf = dto.getFilter().getTaxId();
        String correlationId = dto.getFilter().getCorrelationId();
        return createBatchRequestByCf(pnNationalRegistriesCxId, dto)
                .doOnNext(batchRequest -> log.info("Created Batch Request for taxId: {} and correlationId: {}", MaskDataUtils.maskString(cf), correlationId))
                .doOnError(throwable -> log.info("Failed to create Batch Request for taxId: {} and correlationId: {}", MaskDataUtils.maskString(cf), correlationId))
                .map(infoCamereConverter::convertToGetAddressIniPecOKDto);
    }

    public Mono<GetAddressRegistroImpreseOKDto> getRegistroImpreseLegalAddress(GetAddressRegistroImpreseRequestBodyDto request) {
        return infoCamereClient.getLegalAddress(request.getFilter().getTaxId())
                .doOnNext(address -> log.info("Got Legal Address for taxId: {}", MaskDataUtils.maskString(request.getFilter().getTaxId())))
                .doOnError(throwable -> log.info("Failed to get Legal Address for taxId: {}", MaskDataUtils.maskString(request.getFilter().getTaxId())))
                .map(infoCamereConverter::mapToResponseOk);
    }

    public Mono<BatchRequest> createBatchRequestByCf(String pnNationalRegistriesCxId, GetDigitalAddressIniPECRequestBodyDto dto) {
        BatchRequest batchRequest = createNewStartBatchRequest();
        batchRequest.setCorrelationId(dto.getFilter().getCorrelationId());
        batchRequest.setCf(dto.getFilter().getTaxId());
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
        log.trace("New Batch Request: {}", batchRequest);
        return batchRequest;
    }

    public Mono<InfoCamereLegalOKDto> checkTaxIdAndVatNumber(InfoCamereLegalRequestBodyDto request) {
        return infoCamereClient.checkTaxIdAndVatNumberInfoCamere(request.getFilter())
                .map(infoCamereConverter::infoCamereResponseToDto);
    }
}
