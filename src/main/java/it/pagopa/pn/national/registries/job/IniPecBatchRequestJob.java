package it.pagopa.pn.national.registries.job;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchesStatusEnum;
import it.pagopa.pn.national.registries.entity.NationalRegistriesBatch;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.repository.NationalRegistriesBatchesRepository;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class IniPecBatchRequestJob {

    private final NationalRegistriesRequestsRepository requestsRepository;
    private final NationalRegistriesBatchesRepository batchesRepository;
    private final InfoCamereClient infoCamereClient;
    private final NationalRegistriesConfig config;

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch-request.delay}")
    @SchedulerLock(name = "inipecRequests",
            lockAtMostFor = "${pn.national-registries.inipec.batch-request.lock-at-most}",
            lockAtLeastFor = "${pn.national-registries.inipec.batch-request.lock-at-least}")
    public void batchRequestScheduled() {
        MDCUtils.clearMDCKeys();
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, UUID.randomUUID().toString());
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, "INIPEC_BATCH_REQUEST");
        Mono<Void> execution = collectBatchRequests()
                .filter(this::isEligible)
                .flatMap(requests -> execBatchRequest(requests, UUID.randomUUID().toString()))
                .doOnError(error -> log.error("INIPEC batch request job failed", error));
        MDCUtils.addMDCToContextAndExecute(execution).block();
    }

    private Mono<List<NationalRegistriesRequest>> collectBatchRequests() {
        return Flux.defer(() -> Flux.from(requestsRepository.getRequestsByRegistryStatus(
                        GatewayDownstreamService.INIPEC, config.getInipec().getQueryLimit())))
                .concatMapIterable(Page::items)
                .take(config.getInipec().getMaxBatchRequestSize())
                .collectList();
    }

    private boolean isEligible(List<NationalRegistriesRequest> requests) {
        if (requests.isEmpty()) {
            return false;
        }
        return requests.size() == config.getInipec().getMaxBatchRequestSize()
                || !requests.getFirst().getCreatedAt()
                .plusSeconds(config.getInipec().getOldestRequestMaxWaitingSeconds()).isAfter(Instant.now());
    }

    private Mono<Void> execBatchRequest(List<NationalRegistriesRequest> requests, String batchId) {
        IniPecBatchRequest payload = new IniPecBatchRequest();
        payload.setDataOraRichiesta(LocalDateTime.now(ZoneOffset.UTC).toString());
        payload.setElencoCf(requests.stream().map(request -> {
            IniPecBatchRequest.IniPecCf taxId = new IniPecBatchRequest.IniPecCf();
            taxId.setCf(request.getTaxId());
            return taxId;
        }).toList());

        log.info("INIPEC submitting batch {} with {} requests", batchId, requests.size());
        return infoCamereClient.callEServiceRequestId(payload)
                .switchIfEmpty(Mono.error(new PnInternalException(
                        "INIPEC returned an empty submission response for batch " + batchId,
                        500, ERROR_CODE_NATIONAL_REGISTRIES_MISSINGPOLLINGID)))
                .flatMap(response -> Mono.defer(() -> persistBatch(response, batchId, requests))
                        .onErrorMap(error -> {
                            String pollingId = response.getIdentificativoRichiesta();
                            log.error("INIPEC consistency error: batchId={}, pollingId={}, requestKeys={}",
                                    batchId, pollingId,
                                    requests.stream().map(request -> request.getCorrelationId() + "/" + request.getCreatedAt()).toList(),
                                    error);
                            return new PnInternalException(
                                    "INIPEC submission succeeded but local persistence failed",
                                    500, ERROR_CODE_NATIONAL_REGISTRIES_BATCH_CONSISTENCY);
                        }));
    }

    private Mono<Void> persistBatch(IniPecBatchResponse response, String batchId,
                                   List<NationalRegistriesRequest> requests) {
        if (!StringUtils.hasText(response.getIdentificativoRichiesta())) {
            return Mono.error(new PnInternalException("INIPEC returned no polling identifier",
                    500, ERROR_CODE_NATIONAL_REGISTRIES_MISSINGPOLLINGID));
        }
        Instant now = Instant.now();
        NationalRegistriesBatch batch = new NationalRegistriesBatch();
        batch.setBatchId(batchId);
        batch.setRegistry(GatewayDownstreamService.INIPEC.name());
        batch.setPollingId(response.getIdentificativoRichiesta());
        batch.setBatchSize(requests.size());
        batch.setStatus(BatchesStatusEnum.REQUEST_SENT);
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        batch.setAttemptAfter(now.plusSeconds(Math.round(config.getInipec().getPollingFirstAttemptDelaySecondsPerCf()
                * requests.size() + config.getInipec().getPollingFirstAttemptFixedDelaySeconds()))
                .truncatedTo(ChronoUnit.SECONDS));
        batch.setErrorRetryCount(0);
        batch.setInProgressRetryCount(0);

        return Mono.defer(() -> batchesRepository.putBatch(batch))
                .thenMany(Flux.fromIterable(requests)
                        .concatMap(request -> Mono.defer(() -> requestsRepository.updateStatusToWorking(
                                        request.getCorrelationId(), request.getCreatedAt().toString(),
                                        batchId, GatewayDownstreamService.INIPEC))))
                .then();
    }
}
