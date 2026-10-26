package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.service.InfoCamereService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_NATIONAL_REGISTRIES_MISSINGPOLLINGID;
import static it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService.INIPEC;
import static it.pagopa.pn.national.registries.utils.GatewayUtils.formatInstant;

@Component
@Slf4j
@RequiredArgsConstructor
public class IniPecBatchRequestJob {

    private final NationalRegistriesRequestsRepository nationalRegistriesRequestsRepository;
    private final NationalRegistriesBatchesRepository nationalRegistriesBatchesRepository;
    private final NationalRegistriesConfig config;
    private final InfoCamereService infoCamereService;
    private final InfoCamereConverter infoCamereConverter;

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch-request.delay}")
    @SchedulerLock(name = "inipecRequests",
            lockAtMostFor = "${pn.national-registries.inipec.batch-request.lock-at-most}",
            lockAtLeastFor = "${pn.national-registries.inipec.batch-request.lock-at-least}")
    public void batchRequestScheduled() {
        MDCUtils.clearMDCKeys();
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, UUID.randomUUID().toString());
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, "INIPEC_BATCH_REQUEST");
        Mono<Void> execution = collectBatchRequests()
                .flatMap(requests -> execBatchRequest(requests, UUID.randomUUID().toString()))
                .doOnError(error -> log.error("INIPEC batch request job failed", error));
        MDCUtils.addMDCToContextAndExecute(execution).block();
    }

    private Mono<List<NationalRegistriesRequest>> collectBatchRequests() {
        return Flux.defer(() -> Flux.from(nationalRegistriesRequestsRepository.getRequestsByRegistryStatus(
                        GatewayDownstreamService.INIPEC, RequestStatusEnum.NOT_WORKED, config.getQueryLimit())))
                .concatMapIterable(Page::items)
                .take(config.getInipec().getMaxBatchRequestSize())
                .collectList();
    }


    private Mono<Void> execBatchRequest(List<NationalRegistriesRequest> requests, String batchId) {
        if(CollectionUtils.isEmpty(requests)) {
            log.info("No INIPEC requests to process");
            return Mono.empty();
        }
        IniPecBatchRequest iniPecBatchRequest = infoCamereConverter.createIniPecRequest(requests.stream().map(NationalRegistriesRequest::getTaxId).toList());
        log.info("INIPEC submitting batch {} with {} requests", batchId, requests.size());
        return infoCamereService.callEService(iniPecBatchRequest, batchId)
                .onErrorResume(PnNationalRegistriesException.class, throwable -> checkIfIsBadRequestAndMarkRequestAsError(throwable, requests))
                .flatMap(iniPecBatchResponse -> persistBatch(iniPecBatchResponse, batchId, requests))
                .then();
    }

    private Mono<IniPecBatchResponse> checkIfIsBadRequestAndMarkRequestAsError(PnNationalRegistriesException throwable, List<NationalRegistriesRequest> requests) {
        boolean badRequest = throwable instanceof PnNationalRegistriesException exception && exception.getStatusCode() == HttpStatus.BAD_REQUEST;
        if(badRequest){
            return Flux.fromIterable(requests)
                    .concatMap(request -> nationalRegistriesRequestsRepository.updateStatusToError(
                            request.getCorrelationId(),
                            request.getCreatedAt().toString(),
                            GatewayDownstreamService.INIPEC))
                    .then(Mono.empty());
        }
        return Mono.error(throwable);

    }

    private Mono<Void> persistBatch(IniPecBatchResponse response, String batchId, List<NationalRegistriesRequest> requests) {
        if (!StringUtils.hasText(response.getIdentificativoRichiesta())) {
            return Mono.error(new PnInternalException("INIPEC returned no polling identifier", 500, ERROR_CODE_NATIONAL_REGISTRIES_MISSINGPOLLINGID));
        }
        Instant now = Instant.now();
        NationalRegistriesBatch batch = GatewayConverter.constructBatchEntity(batchId, requests, now, INIPEC, response.getIdentificativoRichiesta());
        batch.setAttemptAfter(formatInstant(infoCamereConverter.calculateFirstAttemptAfter(requests.size())));

        return nationalRegistriesBatchesRepository.putBatch(batch)
                .thenMany(Flux.fromIterable(requests)
                        .concatMap(request -> nationalRegistriesRequestsRepository.updateStatusToWorking(
                                request.getCorrelationId(), request.getCreatedAt().toString(),
                                batchId, GatewayDownstreamService.INIPEC)))
                .then();
    }
}
