package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.EService;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.repository.IniPecBatchPollingRepository;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INIPEC_RETRY_EXHAUSTED_TO_SQS;

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
                                             @Value("${pn.national.registries.inipec.batchrequest.pk.separator}") String batchRequestPkSeparator) {
        this.infoCamereConverter = infoCamereConverter;
        this.batchRequestRepository = batchRequestRepository;
        this.batchPollingRepository = batchPollingRepository;
        this.infoCamereClient = infoCamereClient;
        this.iniPecBatchSqsService = iniPecBatchSqsService;
        this.inadService = inadService;
        this.maxRetry = maxRetry;
        this.inProgressMaxRetry = inProgressMaxRetry;
        this.batchRequestPkSeparator = batchRequestPkSeparator;
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
        int nextRetry = polling.getRetry() != null ? polling.getRetry() + 1 : 1;
        int inProgressNextRetry = polling.getInProgressRetry() != null ? polling.getInProgressRetry() + 1 : 1;
        if(isPollingResponseNotReady(throwable)){
            polling.setInProgressRetry(inProgressNextRetry);
        }else{
            polling.setRetry(nextRetry);
        }
        if (nextRetry >= maxRetry || inProgressNextRetry >= inProgressMaxRetry ||
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
                .doOnNext(request -> {
                    CodeSqsDto sqsDto = sqsDtoProvider.apply(request);
                    if (CollectionUtils.isEmpty(sqsDto.getDigitalAddress()) && !StringUtils.hasText(sqsDto.getError())) {
                        //if IniPec doesn't retrieve pec try to call INAD
                        callInadEservice(request);
                        request.setEservice(EService.INAD.name());
                    } else {
                        request.setMessage(convertCodeSqsDtoToString(sqsDto));
                        request.setEservice(EService.INIPEC.name());
                        request.setStatus(status.getValue());
                    }
                    request.setSendStatus(BatchSendStatus.NOT_SENT.getValue());
                    request.setLastReserved(now);
                })
                .flatMap(batchRequestRepository::update)
                .doOnNext(r -> log.debug("IniPEC - correlationId {} - set status in {}", r.getCorrelationId(), r.getStatus()))
                .doOnError(e -> log.warn("IniPEC - batchId {} - failed to set request in status {}", polling.getBatchId(), status, e))
                .collectList()
                .filter(l -> !l.isEmpty())
                .flatMap(iniPecBatchSqsService::batchSendToSqs);
    }

    private void callInadEservice(BatchRequest request) {
        String correlationId = request.getCorrelationId().split(batchRequestPkSeparator)[0];
        inadService.callEService(convertToGetDigitalAddressInadRequest(request), "PG")
                .doOnNext(inadResponse -> {
                    request.setMessage(convertCodeSqsDtoToString(inadToSqsDto(correlationId, inadResponse, DigitalAddressRecipientType.IMPRESA)));
                    request.setStatus(BatchStatus.WORKED.getValue());
                })
                .doOnNext(sendMessageResponse -> log.info("retrieved digital address from INAD for correlationId: {}", request.getCorrelationId()))
                .onErrorResume(e -> {
                    logEServiceError(e);
                    CodeSqsDto codeSqsDto = errorInadToSqsDto(correlationId, e);
                    if(codeSqsDto != null) {
                        request.setMessage(convertCodeSqsDtoToString(codeSqsDto));
                        request.setStatus(BatchStatus.WORKED.getValue());
                    }else{
                        request.setStatus(BatchStatus.ERROR.getValue());
                    }
                    return Mono.empty();
                }).block();
    }
    private Function<BatchRequest, CodeSqsDto> getSqsOk(IniPecPollingResponse response) {
        return request -> infoCamereConverter.convertResponsePecToCodeSqsDto(request, response);
    }

    private Function<BatchRequest, CodeSqsDto> getSqsKo(String error) {
        return request -> infoCamereConverter.convertIniPecRequestToSqsDto(request, error);
    }

    private void logEServiceError(Throwable throwable) {
        String message = "can not retrieve digital address from INAD: {}";
        if (CheckExceptionUtils.isForLogLevelWarn(throwable)) {
            log.warn(message, throwable.getMessage());
        } else {
            log.error(message, throwable.getMessage());
        }
    }
}
