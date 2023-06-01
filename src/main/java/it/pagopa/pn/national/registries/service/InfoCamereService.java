package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerification;
import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImprese;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_CHEKING_INFO_CAMERE_LEGAL;

@Service
@lombok.CustomLog
public class InfoCamereService {

    private final InfoCamereClient infoCamereClient;
    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository iniPecBatchRequestRepository;
    private final long iniPecTtl;

    public InfoCamereService(InfoCamereClient infoCamereClient,
                             InfoCamereConverter infoCamereConverter,
                             IniPecBatchRequestRepository iniPecBatchRequestRepository,
                             @Value("${pn.national.registries.inipec.ttl}") long iniPecTtl) {
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
                .doOnError(throwable -> log.info("Failed to get Legal Address for taxId: {}", MaskDataUtils.maskString(request.getFilter().getTaxId())))
                .flatMap(response -> processResponseLegalAddressOk(request, response));
    }

    private Mono<GetAddressRegistroImpreseOKDto> processResponseLegalAddressOk(GetAddressRegistroImpreseRequestBodyDto request, AddressRegistroImprese response) {
        if(infoCamereConverter.checkIfResponseIsInfoCamereError(response)) {
            log.info("Failed to get Legal Address for taxId: {}, with error : {}", MaskDataUtils.maskString(request.getFilter().getTaxId()), response.getDescription());
            return Mono.just(infoCamereConverter.mapToResponseOkByRequest(request));
        } else {
            log.info("Got Legal Address for taxId: {}", MaskDataUtils.maskString(request.getFilter().getTaxId()));
            return Mono.just(infoCamereConverter.mapToResponseOkByResponse(response));
        }
    }

    public Mono<BatchRequest> createBatchRequestByCf(String pnNationalRegistriesCxId, GetDigitalAddressIniPECRequestBodyDto dto) {
        BatchRequest batchRequest = createNewStartBatchRequest();
        batchRequest.setCorrelationId(dto.getFilter().getCorrelationId());
        batchRequest.setCf(dto.getFilter().getTaxId());
        batchRequest.setClientId(pnNationalRegistriesCxId);
        return iniPecBatchRequestRepository.create(batchRequest);
    }

    private BatchRequest createNewStartBatchRequest() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
        batchRequest.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchRequest.setRetry(0);
        batchRequest.setLastReserved(now);
        batchRequest.setCreatedAt(now);
        batchRequest.setTtl(now.plusSeconds(iniPecTtl).toEpochSecond(ZoneOffset.UTC));
        log.trace("New Batch Request: {}", batchRequest);
        return batchRequest;
    }

    public Mono<InfoCamereLegalOKDto> checkTaxIdAndVatNumber(InfoCamereLegalRequestBodyDto request) {
        log.logChecking(PROCESS_CHEKING_INFO_CAMERE_LEGAL);
        return infoCamereClient.checkTaxIdAndVatNumberInfoCamere(request.getFilter())
                .doOnNext(infoCamereVerification -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL,true))
                .doOnError(throwable -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL,false,throwable.getMessage()))
                .flatMap(response -> processResponseCheckTaxIdAndVatNumberOk(request, response));
    }

    private Mono<InfoCamereLegalOKDto> processResponseCheckTaxIdAndVatNumberOk(InfoCamereLegalRequestBodyDto request, InfoCamereVerification response) {
        String process = "validating taxId and vatNumber";
        if(infoCamereConverter.checkIfResponseIsInfoCamereError(response)) {
            log.logCheckingOutcome(process, false, "Failed to check taxId and vatNumber with error: "+response.getDescription());
            log.info("Failed to check tax id: {} and vat number: {}, with error : {}", MaskDataUtils.maskString(request.getFilter().getTaxId()), MaskDataUtils.maskString(request.getFilter().getVatNumber()), response.getDescription());
            return Mono.just(infoCamereConverter.infoCamereResponseToDtoByRequest(request));
        } else {
            log.logCheckingOutcome(process, true);
            log.info("Got Legal Address for taxId: {}", MaskDataUtils.maskString(request.getFilter().getTaxId()));
            return Mono.just(infoCamereConverter.infoCamereResponseToDtoByResponse(response));
        }
    }
}
