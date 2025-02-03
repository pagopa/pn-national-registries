package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.converter.InadConverter;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.EService;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.utils.CheckEmailUtils;
import it.pagopa.pn.national.registries.utils.CheckExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INIPEC_RETRY_EXHAUSTED_TO_SQS;
import static it.pagopa.pn.national.registries.model.EService.*;

@Slf4j
@Service
public class DigitalAddressBatchPollingService extends GatewayConverter {

    private final InfoCamereConverter infoCamereConverter;
    private final IniPecBatchRequestRepository batchRequestRepository;
    private final IniPecBatchPollingRepository batchPollingRepository;
    private final InfoCamereClient infoCamereClient;
    private final IniPecBatchSqsService iniPecBatchSqsService;
    private final InadService inadService;

    private final int maxRetry;
    private final int inProgressMaxRetry;
    private final String batchRequestPkSeparator;

    private final IpaService ipaService;

    private static final int MAX_BATCH_POLLING_SIZE = 1;
    private static final Pattern PEC_REQUEST_IN_PROGRESS_PATTERN = Pattern.compile(".*(List PEC in progress).*");

    public DigitalAddressBatchPollingService(InfoCamereConverter infoCamereConverter,
                                             IniPecBatchRequestRepository batchRequestRepository,
                                             IniPecBatchPollingRepository batchPollingRepository,
                                             InfoCamereClient infoCamereClient,
                                             IniPecBatchSqsService iniPecBatchSqsService,
                                             InadService inadService,
                                             @Value("${pn.national-registries.inipec.batch.polling.max-retry}") int maxRetry,
                                             @Value("${pn.national-registries.inipec.batch.polling.inprogress.max-retry}") int inProgressMaxRetry,
                                             @Value("${pn.national.registries.inipec.batchrequest.pk.separator}") String batchRequestPkSeparator, IpaService ipaService) {
        this.infoCamereConverter = infoCamereConverter;
        this.batchRequestRepository = batchRequestRepository;
        this.batchPollingRepository = batchPollingRepository;
        this.infoCamereClient = infoCamereClient;
        this.iniPecBatchSqsService = iniPecBatchSqsService;
        this.inadService = inadService;
        this.maxRetry = maxRetry;
        this.inProgressMaxRetry = inProgressMaxRetry;
        this.batchRequestPkSeparator = batchRequestPkSeparator;
        this.ipaService = ipaService;
    }

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.polling.delay}")
    public void batchPecPolling() {
        log.trace("IniPEC - batchPecPolling start");
        Page<BatchPolling> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        do {
            page = getBatchPolling(lastEvaluatedKey);
            lastEvaluatedKey = page.lastEvaluatedKey();
            if (!page.items().isEmpty()) {
                String reservationId = UUID.randomUUID().toString();
                execBatchPolling(page.items(), reservationId)
                        .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + reservationId))
                        .block();
            } else {
                log.info("IniPEC - no batch polling available");
            }
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));
        log.trace("IniPEC - batchPecPolling end");
    }

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.polling.recovery.delay}")
    public void recoveryBatchPolling() {
        log.trace("IniPEC - recoveryBatchPolling start");
        batchPollingRepository.getBatchPollingToRecover()
                .flatMapIterable(polling -> polling)
                .doOnNext(polling -> {
                    polling.setStatus(BatchStatus.NOT_WORKED.getValue());
                    polling.setReservationId(null);
                })
                .flatMap(polling -> batchPollingRepository.resetBatchPollingForRecovery(polling)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("IniPEC - conditional check failed - skip recovery pollingId {} and batchId {}", polling.getPollingId(), polling.getBatchId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .count()
                .doOnNext(c -> batchPecPolling())
                .subscribe(c -> log.info("IniPEC - executed batch recovery on {} polling", c),
                        e -> log.error("IniPEC - failed execution of batch polling recovery", e));
        log.trace("IniPEC - recoveryBatchPolling end");
    }

    private Page<BatchPolling> getBatchPolling(Map<String, AttributeValue> lastEvaluatedKey) {
        return batchPollingRepository.getBatchPollingWithoutReservationIdAndStatusNotWorked(lastEvaluatedKey, MAX_BATCH_POLLING_SIZE)
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn("IniPEC - can not get batch polling - DynamoDB Mono<Page> is null");
                    return new DigitalAddressException("IniPEC - can not get batch polling");
                });
    }

    private Mono<Void> execBatchPolling(List<BatchPolling> items, String reservationId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(items.stream())
                .map(item -> {
                    item.setStatus(BatchStatus.WORKING.getValue());
                    item.setReservationId(reservationId);
                    item.setLastReserved(now);
                    return item;
                })
                .flatMap(item -> batchPollingRepository.setNewReservationIdToBatchPolling(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("IniPEC - conditional check failed - skip pollingId: {}", item.getPollingId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .flatMap(this::handlePolling)
                .doOnError(e -> log.error("IniPEC - reservationId {} - failed to execute polling on {} items", reservationId, items.size(), e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    private Mono<Void> handlePolling(BatchPolling polling) {
        return callIniPecEService(polling.getBatchId(), polling.getPollingId())
                .onErrorResume(t -> incrementAndCheckRetry(polling, t).then(Mono.error(t)))
                .flatMap(response -> handleSuccessfulPolling(polling, response))
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<IniPecPollingResponse> callIniPecEService(String batchId, String pollingId) {
        return infoCamereClient.callEServiceRequestPec(pollingId)
                .doOnNext(response -> {
                    if(infoCamereConverter.checkIfResponseIsInfoCamereError(response)) {
                        throw new PnNationalRegistriesException(response.getDescription(), HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase() , null, null,
                                Charset.defaultCharset(), InfocamereResponseKO.class);
                    }
                    else {
                        log.info("IniPEC - batchId {} - pollingId {} - response pec size: {}", batchId, pollingId, response.getElencoPec().size());
                    }
                })
                .doOnError(e -> log.warn("IniPEC - pollingId {} - failed to call EService", pollingId, e));
    }

    private boolean isPollingResponseNotReady(Throwable throwable) {
        return throwable instanceof PnNationalRegistriesException exception
                && exception.getStatusCode() == HttpStatus.NOT_FOUND
                && checkPecRequestInProgressPattern(exception.getMessage());
    }

    private boolean checkPecRequestInProgressPattern(String message) {
        if(message == null) {
            return false;
        }

        Matcher matcher = PEC_REQUEST_IN_PROGRESS_PATTERN.matcher(message);
        return matcher.find();
    }

    private Mono<Void> handleSuccessfulPolling(BatchPolling polling, IniPecPollingResponse response) {
        polling.setStatus(BatchStatus.WORKED.getValue());
        return batchPollingRepository.update(polling)
                .doOnNext(p -> log.debug("IniPEC - batchId {} - pollingId {} - updated status to WORKED", polling.getBatchId(), polling.getPollingId()))
                .doOnError(e -> log.warn("IniPEC - batchId {} - pollingId {} - failed to update status to WORKED", polling.getBatchId(), polling.getPollingId(), e))
                .flatMap(p -> updateBatchRequest(p, BatchStatus.WORKED, getSqsOk(response)));
    }

    private Mono<Void> incrementAndCheckRetry(BatchPolling polling, Throwable throwable) {
        if(isPollingResponseNotReady(throwable)){
            polling.setInProgressRetry(polling.getInProgressRetry() != null ? polling.getInProgressRetry() + 1 : 1);
        }else{
            polling.setRetry(polling.getRetry() != null ? polling.getRetry() + 1 : 1);
        }
        if (maxRetry <= Optional.ofNullable(polling.getRetry()).orElse(0) || inProgressMaxRetry <= Optional.ofNullable(polling.getInProgressRetry()).orElse(0) ||
                (throwable instanceof PnNationalRegistriesException exception && exception.getStatusCode() == HttpStatus.BAD_REQUEST)) {
            polling.setStatus(BatchStatus.ERROR.getValue());
            log.debug("IniPEC - batchId {} - polling {} status in {} (retry: {})", polling.getBatchId(), polling.getPollingId(), polling.getStatus(), polling.getRetry());
        }
        return batchPollingRepository.update(polling)
                .doOnNext(p -> log.debug("IniPEC - batchId {} - pollingId {} - retry incremented", polling.getBatchId(), polling.getPollingId()))
                .doOnError(e -> log.warn("IniPEC - batchId {} - pollingId {} - failed to increment retry", polling.getBatchId(), polling.getPollingId(), e))
                .filter(p -> BatchStatus.ERROR.getValue().equals(p.getStatus()))
                .flatMap(p -> {
                    String error;
                    if (throwable instanceof PnNationalRegistriesException exception) {
                        error = exception.getMessage();
                    } else {
                        error = ERROR_MESSAGE_INIPEC_RETRY_EXHAUSTED_TO_SQS;
                    }
                    return updateBatchRequest(p, BatchStatus.ERROR, getSqsKo(error));
                });
    }

    private Mono<Void> updateBatchRequest(BatchPolling polling, BatchStatus status, Function<BatchRequest, CodeSqsDto> sqsDtoProvider) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return batchRequestRepository.getBatchRequestByBatchIdAndStatus(polling.getBatchId(), BatchStatus.WORKING)
                .doOnNext(requests -> log.debug("IniPEC - batchId {} - updating {} requests in status {}", polling.getBatchId(), requests.size(), status))
                .flatMapIterable(requests -> requests)
                .flatMap(request -> {
                    CodeSqsDto sqsDto = sqsDtoProvider.apply(request);
                    removeInvalidEmails(sqsDto);
                    request.setSendStatus(BatchSendStatus.NOT_SENT.getValue());
                    request.setLastReserved(now);
                    if (CollectionUtils.isEmpty(sqsDto.getDigitalAddress()) && !StringUtils.hasText(sqsDto.getError())) {
                        log.info("digital Address not found for [{}] on {} - Step {} - nextSource: [{}]", request.getCorrelationId(), INIPEC, INIPEC.getStepNumber(), INIPEC.getNextStep());
                        return callIpaEservice(request)
                                .thenReturn(request);
                    } else {
                        request.setMessage(convertCodeSqsDtoToString(sqsDto));
                        request.setEservice(INIPEC.name());
                        request.setStatus(status.getValue());
                        return Mono.just(request);
                    }
                })
                .flatMap(batchRequestRepository::update)
                .doOnNext(r -> log.debug("IniPEC - correlationId {} - set status in {}", r.getCorrelationId(), r.getStatus()))
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to set request in status {}", polling.getBatchId(), status, e))
                .collectList()
                .filter(l -> !l.isEmpty())
                .flatMap(iniPecBatchSqsService::batchSendToSqs);
    }

    private Mono<Void> callIpaEservice(BatchRequest request) {
        log.info("START retrieve digital address for [{}] on [{}] - Step {} - nextSource: [{}]", request.getCorrelationId(), IPA, IPA.getStepNumber(), IPA.getNextStep());
        return ipaService.getIpaPec(convertToGetIpaPecRequest(request))
                .doOnNext(sendMessageResponse -> log.info("retrieved digital address from IPA for correlationId: {}", request.getCorrelationId()))
                .onErrorResume(e -> {
                    logEServiceError(e, "can not retrieve digital address from IPA: {}");
                    request.setStatus(BatchStatus.ERROR.getValue());
                    return Mono.empty();
                })
                .flatMap(response -> {
                    if ((response.getDomicilioDigitale() == null &&
                            response.getDenominazione() == null &&
                            response.getCodEnte() == null &&
                            response.getTipo() == null) ||
                            !CheckEmailUtils.isValidEmail(response.getDomicilioDigitale())) {
                        log.info("digital Address not found for [{}] on {} - Step {} - nextSource: [{}]", request.getCorrelationId(), IPA, IPA.getStepNumber(), IPA.getNextStep());
                        return callInadEservice(request);
                    }else{
                        request.setMessage(convertCodeSqsDtoToString(ipaToSqsDto(request.getCorrelationId(), response)));
                        request.setStatus(BatchStatus.WORKED.getValue());
                        request.setEservice(IPA.name());
                        return Mono.empty();
                    }
                });
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

    private Mono<Void> callInadEservice(BatchRequest request) {

        RecipientType recipientType = InadConverter.retrieveRecipientType(request);
        log.info("START retrieve digital address for [{}] on [{}] - Step {} - nextSource: [{}]", request.getCorrelationId(), INAD, INAD.getStepNumber(), INAD.getNextStep());
        String correlationId = request.getCorrelationId().split(batchRequestPkSeparator)[0];
        return inadService.callEService(convertToGetDigitalAddressInadRequest(request), recipientType, request.getReferenceRequestDate().toInstant(ZoneOffset.UTC))
                .flatMap(this::emailValidation)
                .doOnNext(inadResponse -> {
                    request.setMessage(convertCodeSqsDtoToString(inadToSqsDto(correlationId, inadResponse, PF.equals(recipientType) ? DigitalAddressRecipientType.PERSONA_FISICA : DigitalAddressRecipientType.IMPRESA)));
                    request.setStatus(BatchStatus.WORKED.getValue());
                    request.setEservice(EService.INAD.name());
                })
                .doOnNext(sendMessageResponse -> log.info("retrieved digital address from INAD for correlationId: {}", request.getCorrelationId()))
                .onErrorResume(e -> {
                    logEServiceError(e, "can not retrieve digital address from INAD: {}");
                    CodeSqsDto codeSqsDto = errorInadToSqsDto(correlationId, e);
                    if(codeSqsDto != null) {
                        log.info("digital Address not found for [{}] on {} - Step {} - nextSource: [{}] - return empty list", request.getCorrelationId(), INAD, INAD.getStepNumber(), INAD.getNextStep());
                        request.setMessage(convertCodeSqsDtoToString(codeSqsDto));
                        request.setStatus(BatchStatus.WORKED.getValue());
                    }else{
                        request.setStatus(BatchStatus.ERROR.getValue());
                    }
                    return Mono.empty();
                })
                .then();
    }
    private Function<BatchRequest, CodeSqsDto> getSqsOk(IniPecPollingResponse response) {
        return request -> infoCamereConverter.convertResponsePecToCodeSqsDto(request, response);
    }

    private Function<BatchRequest, CodeSqsDto> getSqsKo(String error) {
        return request -> infoCamereConverter.convertIniPecRequestToSqsDto(request, error);
    }

    private void logEServiceError(Throwable throwable, String message) {
        if (CheckExceptionUtils.isForLogLevelWarn(throwable)) {
            log.warn(message, throwable.getMessage());
        } else {
            log.error(message, throwable.getMessage());
        }
    }
}
