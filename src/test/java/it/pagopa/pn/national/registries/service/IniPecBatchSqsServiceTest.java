package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = {
        "pn.national-registries.inipec.batch.sqs.recovery.delay=30000",
        "pn.national.registries.inipec.batchrequest.pk.separator=~"

        })
@ExtendWith(MockitoExtension.class)
class IniPecBatchSqsServiceTest {

    @InjectMocks
    private IniPecBatchSqsService iniPecBatchSqsService;

    @Mock
    IniPecBatchRequestRepository batchRequestRepository;

    @Mock
    NationalRegistriesConfig nationalRegistriesConfig;

    @Mock
    SqsService sqsService;

    NationalRegistriesConfig.InfoCamere infoCamereConfig;

    @BeforeEach
    void setup() {
        infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        NationalRegistriesConfig.Inipec inipecConfig = new NationalRegistriesConfig.Inipec();
        inipecConfig.setBatchRequestPkSeparator("~");
        infoCamereConfig.setInipec(inipecConfig);
    }


    @Test
    void testRecoveryBatchSendToSqs() {
        when(batchRequestRepository.getBatchRequestToSend(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(List.of(new BatchRequest(), new BatchRequest()))));
        when(batchRequestRepository.update(any()))
                .thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()))
                .thenReturn(Mono.just(new BatchRequest()));

        iniPecBatchSqsService.recoveryBatchSendToSqs();

        verify(batchRequestRepository).getBatchRequestToSend(anyMap(), anyInt());
    }

    @Test
    void testRecoveryBatchSendToSqsEmpty() {
        when(batchRequestRepository.getBatchRequestToSend(anyMap(), anyInt()))
                .thenReturn(Mono.just(Page.create(Collections.emptyList())));

        iniPecBatchSqsService.recoveryBatchSendToSqs();

        verifyNoInteractions(sqsService);
        verify(batchRequestRepository, never()).update(any());
    }

    @Test
    void testBatchSendToSqsEmtpy() {
        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(Collections.emptyList()))
                .verifyComplete();
        verifyNoInteractions(batchRequestRepository);
        verifyNoInteractions(sqsService);
    }

    @Test
    void testBatchSendToSqsOk() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setMessage("message");
        batchRequest.setClientId("clientId");
        batchRequest.setCf("cf");

        when(batchRequestRepository.setNewReservationIdToBatchRequest(same(batchRequest)))
                .thenReturn(Mono.just(batchRequest));
        when(batchRequestRepository.update(same(batchRequest)))
                .thenReturn(Mono.just(batchRequest));

        when(sqsService.pushToOutputQueue(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest)))
                .verifyComplete();
        assertEquals(BatchSendStatus.SENT.getValue(), batchRequest.getSendStatus());
    }

    @Test
    void testBatchSendToSqsKo() {
        BatchRequest batchRequest1 = new BatchRequest();
        batchRequest1.setSendStatus(BatchSendStatus.NOT_SENT.getValue());
        BatchRequest batchRequest2 = new BatchRequest();
        batchRequest2.setSendStatus(BatchSendStatus.NOT_SENT.getValue());
        BatchRequest batchRequest3 = new BatchRequest();
        batchRequest3.setSendStatus(BatchSendStatus.NOT_SENT.getValue());
        batchRequest3.setMessage("message");
        batchRequest3.setClientId("clientId");
        batchRequest3.setCf("cf");

        when(batchRequestRepository.setNewReservationIdToBatchRequest(any()))
                .thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()))
                .thenReturn(Mono.just(batchRequest2))
                .thenReturn(Mono.just(batchRequest3));
        when(batchRequestRepository.update(any()))
                .thenReturn(Mono.just(new BatchRequest()));

        when(sqsService.pushToOutputQueue(any(), any()))
                .thenReturn(Mono.error(SqsException.builder().build()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(iniPecBatchSqsService.batchSendToSqs(List.of(batchRequest1, batchRequest2, batchRequest3)))
                .verifyComplete();

        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest1.getSendStatus());
        assertEquals(BatchSendStatus.NOT_SENT.getValue(), batchRequest2.getSendStatus());
        assertEquals(BatchSendStatus.SENT.getValue(), batchRequest3.getSendStatus());
        verify(batchRequestRepository).update(same(batchRequest3));
    }

    @Test
    void redriveToDLQqueue() {
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);

        BatchRequest request = new BatchRequest();
        request.setCorrelationId("correlationId");
        request.setReferenceRequestDate(LocalDateTime.now());
        when(sqsService.pushToInputDlqQueue(any(), any())).thenReturn(Mono.just(SendMessageResponse.builder().build()));
        StepVerifier.create(iniPecBatchSqsService.sendListToDlqQueue(List.of(request)))
                .verifyComplete();
    }

    @Test
    void redriveToqueue() {
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);

        BatchRequest request = new BatchRequest();
        request.setCorrelationId("correlationId");
        request.setReferenceRequestDate(LocalDateTime.now());
        request.setStatus(BatchSendStatus.ERROR.getValue());
        when(sqsService.pushToInputDlqQueue(any(), any())).thenReturn(Mono.just(SendMessageResponse.builder().build()));
        StepVerifier.create(iniPecBatchSqsService.sendListToDlqQueue(List.of(request)))
                .verifyComplete();
    }
}