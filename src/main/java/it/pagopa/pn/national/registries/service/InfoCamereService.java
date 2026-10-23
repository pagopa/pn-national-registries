package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.model.inipec.InipecEvaluationResult;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Objects;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.*;
import static it.pagopa.pn.national.registries.utils.DigitalAddressUtils.removeInvalidEmails;
import static it.pagopa.pn.national.registries.utils.MetricUtils.logBatchRequestMetrics;
import static it.pagopa.pn.national.registries.utils.MetricUtils.logCfWithAddressMetricFromBatchRequest;

@Service
@lombok.CustomLog
public class InfoCamereService {

    private final InfoCamereClient infoCamereClient;
    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository iniPecBatchRequestRepository;
    private final long iniPecTtl;
    private final ValidateTaxIdUtils validateTaxIdUtils;
    private final String batchRequestPkSeparator;

    public InfoCamereService(InfoCamereClient infoCamereClient,
                             InfoCamereConverter infoCamereConverter,
                             IniPecBatchRequestRepository iniPecBatchRequestRepository,
                             @Value("${pn.national-registries.ttl}") long iniPecTtl,
                             @Value("${pn.national-registries.inipec.batchrequest-pk-separator}") String batchRequestPkSeparator,
                             ValidateTaxIdUtils validateTaxIdUtils) {
        this.infoCamereClient = infoCamereClient;
        this.infoCamereConverter = infoCamereConverter;
        this.iniPecBatchRequestRepository = iniPecBatchRequestRepository;
        this.iniPecTtl = iniPecTtl;
        this.validateTaxIdUtils = validateTaxIdUtils;
        this.batchRequestPkSeparator = batchRequestPkSeparator;
    }

    public InipecEvaluationResult evaluateInipecStatoImpresa(String correlationId, String taxId, String recipientType, Pec pec) {
        log.info("evaluateStatoImpresa for correlationId: {} with statoImpresa: {}", correlationId, pec.getStatoImpresa());
        if (Objects.isNull(pec.getStatoImpresa())) {
            log.debug("IniPEC - correlationId {} - statoImpresa is null", correlationId);
            CodeSqsDto codeSqsDto = infoCamereConverter.convertResponsePecToCodeSqsDto(correlationId, pec);
            int foundPecCount = CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress()) ? 0 : codeSqsDto.getDigitalAddress().size();
            removeInvalidEmails(codeSqsDto);
            int validPecCount = CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress()) ? 0 : codeSqsDto.getDigitalAddress().size();
            log.info("IniPEC - correlationId {} - statoImpresa is null - pec found: {}, valid pec: {}", correlationId, foundPecCount, validPecCount);
            if (validPecCount == 0) {
                return new InipecEvaluationResult.NotFound();
            }
            logCfWithAddressMetricFromBatchRequest(codeSqsDto, correlationId, taxId, recipientType, GatewayDownstreamService.INIPEC);
            return new InipecEvaluationResult.Found(codeSqsDto);
        }

        return switch (pec.getStatoImpresa()) {
            case ER -> new InipecEvaluationResult.Retry();
            case ND, NF -> new InipecEvaluationResult.NotFound();
        };
    }

    public Mono<IniPecBatchResponse> callEService(IniPecBatchRequest iniPecBatchRequest, String batchId) {
        return infoCamereClient.callEServiceRequestId(iniPecBatchRequest)
                .doOnNext(res -> logBatchRequestMetrics(batchId, iniPecBatchRequest, false))
                .doOnError(t -> logBatchRequestMetrics(batchId, iniPecBatchRequest, true))
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to call EService", batchId, e));
    }

    public Mono<IniPecPollingResponse> callIniPecEService(String batchId, String pollingId) {
        return infoCamereClient.callEServiceRequestPec(pollingId)
                .doOnNext(response -> {
                    if (infoCamereConverter.checkIfResponseIsInfoCamereError(response)) {
                        throw new PnNationalRegistriesException(response.getDescription(), HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(), null, null,
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    } else {
                        log.info("IniPEC - batchId {} - pollingId {} - response pec size: {}", batchId, pollingId, response.getElencoPec().size());
                    }
                })
                .doOnError(e -> log.warn("IniPEC - pollingId {} - failed to call EService", pollingId, e));
    }

    public Mono<GetDigitalAddressIniPECOKDto> getIniPecDigitalAddress(String pnNationalRegistriesCxId, GetDigitalAddressIniPECRequestBodyDto dto, Date referenceRequestDate, RecipientType recipientType) {
        String cf = dto.getFilter().getTaxId();
        String correlationId = dto.getFilter().getCorrelationId();
        validateTaxIdUtils.validateTaxId(cf, PROCESS_NAME_INIPEC_PEC, false);
        return createBatchRequestByCf(pnNationalRegistriesCxId, dto, MDC.get("AWS_messageId"), referenceRequestDate, recipientType)
                .doOnNext(batchRequest -> log.info("Created Batch Request for correlationId: {}", correlationId))
                .doOnError(throwable -> log.info("Failed to create Batch Request for correlationId: {}", correlationId))
                .map(infoCamereConverter::convertToGetAddressIniPecOKDto);
    }

    public Mono<GetAddressRegistroImpreseOKDto> getRegistroImpreseLegalAddress(GetAddressRegistroImpreseRequestBodyDto request) {
        String cf = request.getFilter().getTaxId();
        validateTaxIdUtils.validateTaxId(cf, PROCESS_NAME_REGISTRO_IMPRESE_ADDRESS, false);
        return infoCamereClient.getLegalAddress(cf)
                .doOnError(throwable -> log.info("Failed to get Legal Address"))
                .flatMap(response -> processResponseLegalAddressOk(request, response));
    }

    public Mono<InfoCamereLegalInstitutionsOKDto> getLegalInstitutions(InfoCamereLegalInstitutionsRequestBodyDto infoCamereLegalInstitutionsRequestBodyDto) {
        log.logChecking(PROCESS_CHEKING_INFO_CAMERE_LEGAL_INSTITUTIONS);
        validateTaxIdUtils.validateTaxId(infoCamereLegalInstitutionsRequestBodyDto.getFilter().getTaxId(), PROCESS_NAME_INFO_CAMERE_LEGAL_INSTITUTIONS, false);

        return infoCamereClient.getLegalInstitutions(infoCamereLegalInstitutionsRequestBodyDto.getFilter())
                .doOnNext(infoCamereLegalInstitutions -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL_INSTITUTIONS,true))
                .doOnError(throwable -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL_INSTITUTIONS,false,throwable.getMessage()))
                .map(infoCamereConverter::mapToResponseOkByResponse);
    }

    private Mono<GetAddressRegistroImpreseOKDto> processResponseLegalAddressOk(GetAddressRegistroImpreseRequestBodyDto request, AddressRegistroImprese response) {
        if(infoCamereConverter.checkIfResponseIsInfoCamereError(response)) {
            log.info("Failed to get Legal Address with error : {}",  response.getDescription());
            return Mono.just(infoCamereConverter.mapToResponseOkByRequest(request));
        } else {
            log.info("Got Legal Address");
            return Mono.just(infoCamereConverter.mapToResponseOkByResponse(response));
        }
    }

    public Mono<BatchRequest> createBatchRequestByCf(String pnNationalRegistriesCxId, GetDigitalAddressIniPECRequestBodyDto dto, String messageId, Date referenceRequestDate, RecipientType recipientType) {
        BatchRequest batchRequest = createNewStartBatchRequest();
        batchRequest.setCorrelationId(dto.getFilter().getCorrelationId() + batchRequestPkSeparator + batchRequest.getCreatedAt());
        batchRequest.setCf(dto.getFilter().getTaxId());
        batchRequest.setClientId(pnNationalRegistriesCxId);
        batchRequest.setAwsMessageId(messageId);
        batchRequest.setRecipientType(recipientType.name());
        if (referenceRequestDate != null) {
            batchRequest.setReferenceRequestDate(referenceRequestDate.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
        }
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

        validateTaxIdUtils.validateTaxId(request.getFilter().getTaxId(), PROCESS_NAME_INFO_CAMERE_LEGAL, false);

        return infoCamereClient.checkTaxIdAndVatNumberInfoCamere(request.getFilter())
                .doOnNext(infoCamereVerification -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL,true))
                .doOnError(throwable -> log.logCheckingOutcome(PROCESS_CHEKING_INFO_CAMERE_LEGAL,false,throwable.getMessage()))
                .flatMap(response -> processResponseCheckTaxIdAndVatNumberOk(request, response));
    }

    private Mono<InfoCamereLegalOKDto> processResponseCheckTaxIdAndVatNumberOk(InfoCamereLegalRequestBodyDto request, InfoCamereVerification response) {
        String process = "validating taxId and vatNumber";
        if(infoCamereConverter.checkIfResponseIsInfoCamereError(response)) {
            log.logCheckingOutcome(process, false, "Failed to check taxId and vatNumber with error: "+response.getDescription());
            log.info("Failed to check tax id and vat number, with error : {}", response.getDescription());
            return Mono.just(infoCamereConverter.infoCamereResponseToDtoByRequest(request));
        } else {
            log.logCheckingOutcome(process, true);
            log.info("Got Legal Address");
            return Mono.just(infoCamereConverter.infoCamereResponseToDtoByResponse(response));
        }
    }
}
