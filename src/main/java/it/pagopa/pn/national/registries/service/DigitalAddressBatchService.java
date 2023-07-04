package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.converter.GatewayConverter;
import it.pagopa.pn.national.registries.utils.CheckExceptionUtils;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DigitalAddressBatchService extends GatewayConverter {

    private final IniPecPollingService iniPecPollingService;
    private final InadService inadService;
    private final SqsService sqsService;

    public DigitalAddressBatchService(IniPecPollingService iniPecPollingService,
                                      InadService inadService,
                                      SqsService sqsService) {
        this.iniPecPollingService = iniPecPollingService;
        this.inadService = inadService;
        this.sqsService = sqsService;
    }

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.polling.delay}")
    public void batchDigitalAddress() {
        iniPecPollingService.batchPecPolling()
                .doOnNext(batchRequest ->
                        inadService.callEService(convertToGetDigitalAddressInadRequest(batchRequest), "PG")
                                .flatMap(inadResponse -> sqsService.push(inadToSqsDto(batchRequest.getCorrelationId(), inadResponse), batchRequest.getClientId()))
                                .doOnNext(sendMessageResponse -> log.info("retrieved digital address from INAD for correlationId: {} - cf: {}", batchRequest.getCorrelationId(), MaskDataUtils.maskString(batchRequest.getCf())))
                                .doOnError(this::logEServiceError)
                                .onErrorResume(e -> sqsService.push(errorInadToSqsDto(batchRequest.getCorrelationId(), e), batchRequest.getClientId()))
                                .map(sqs -> mapToAddressesOKDto(batchRequest.getCorrelationId()))
                                .then());
    }

    private void logEServiceError(Throwable throwable) {
        String message = "can not retrieve digital address from INAD: {}";
        if (CheckExceptionUtils.isForLogLevelWarn(throwable)) {
            log.warn(message, MaskDataUtils.maskInformation(throwable.getMessage()));
        } else {
            log.error(message, MaskDataUtils.maskInformation(throwable.getMessage()));
        }
    }
}
