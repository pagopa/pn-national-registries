package it.pagopa.pn.national.registries.repository;

import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = IniPecBatchRequestRepositoryImpl.class)
class IniPecBatchRequestRepositoryImplTest {

    @MockBean
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @MockBean
    private DynamoDbAsyncTable<Object> dynamoDbAsyncTable;

    @Test
    void testSetNewBatchIdToBatchRequests(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient);

        SdkPublisher<Page<Object>> sdkPublisher = mock(SdkPublisher.class);
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(index.query((QueryEnhancedRequest) any())).thenReturn(sdkPublisher);
        when(dynamoDbAsyncTable.index(any())).thenReturn(index);

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("taxId");
        batchRequest.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
        batchRequest.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchRequest.setRetry(0);
        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setTimeStamp(LocalDateTime.now());
        List<BatchRequest> batchRequests = new ArrayList<>();
        batchRequests.add(batchRequest);

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> null);
        UpdateItemEnhancedRequest<BatchRequest> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(BatchRequest.class)
                .item(batchRequest)
                .build();
        when(dynamoDbAsyncTable.updateItem(updateItemEnhancedRequest)).thenReturn(CompletableFuture.completedFuture(batchRequest));
        when(dynamoDbAsyncTable.updateItem(updateItemEnhancedRequest)).thenReturn(CompletableFuture.completedFuture(batchRequests));

        StepVerifier.create(iniPecBatchPollingRepository.setNewBatchIdToBatchRequests(batchRequests,"status"))
                .expectError().verify();
        //To check
    }


    @Test
    void testCreateBatchRequestByCf(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient);

        GetDigitalAddressIniPECRequestBodyDto requestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("DDDFGF52F52H501S");
        requestBodyDto.setFilter(filter);

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("DDDFGF52F52H501S");

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> null);
        when(dynamoDbAsyncTable.putItem((BatchRequest) any())).thenReturn(completableFuture);

        StepVerifier.create(iniPecBatchPollingRepository.createBatchRequestByCf(requestBodyDto))
                .expectNextCount(0);
    }

    @Test
    void testGetBatchRequestByNotBatchIdPageable(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient);

        Map<String, AttributeValue> lastKey = new HashMap<>();
        lastKey.put("chiave", AttributeValue.builder().s("valore").build());

        SdkPublisher<Page<Object>> sdkPublisher = mock(SdkPublisher.class);
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(index.query((QueryEnhancedRequest) any())).thenReturn(sdkPublisher);
        when(dynamoDbAsyncTable.index(any())).thenReturn(index);
        StepVerifier.create(iniPecBatchPollingRepository.getBatchRequestByNotBatchIdPageable(lastKey))
                .expectNextCount(0);
    }

    @Test
    void testGetBatchRequestsByBatchIdAndSetStatus(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient);

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("taxId");
        batchRequest.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
        batchRequest.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchRequest.setRetry(0);
        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setTimeStamp(LocalDateTime.now());

        SdkPublisher<Page<Object>> sdkPublisher = mock(SdkPublisher.class);
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);

        when(index.query((QueryEnhancedRequest) any())).thenReturn(sdkPublisher);
        when(dynamoDbAsyncTable.index(any())).thenReturn(index);

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> null);
        when(dynamoDbAsyncTable.updateItem((BatchRequest) any())).thenReturn(CompletableFuture.completedFuture(batchRequest));

        StepVerifier.create(iniPecBatchPollingRepository.getBatchRequestsByBatchIdAndSetStatus("batchId","status"))
                .expectNextCount(0);
    }

    @Test
    void testResetBatchIdForRecovery(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient);

        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);

        when(dynamoDbAsyncTable.index(any())).thenReturn(index);

        SdkPublisher<Page<Object>> sdkPublisher = mock(SdkPublisher.class);

        when(index.query((QueryEnhancedRequest) any())).thenReturn(sdkPublisher);

        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(BatchRequest::new);
        when(dynamoDbAsyncTable.updateItem((BatchRequest) any())).thenReturn(completableFuture);

        StepVerifier.create(iniPecBatchPollingRepository.resetBatchIdForRecovery());
    }

    @Test
    void testSetStatusToBatchRequests(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient);

        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(BatchRequest::new);
        when(dynamoDbAsyncTable.updateItem((BatchRequest) any())).thenReturn(completableFuture);

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("taxId");
        batchRequest.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
        batchRequest.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchRequest.setRetry(0);
        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setTimeStamp(LocalDateTime.now());
        List<BatchRequest> batchRequests = new ArrayList<>();
        batchRequests.add(batchRequest);

        StepVerifier.create(iniPecBatchPollingRepository.setStatusToBatchRequests(batchRequests,BatchStatus.NOT_WORKED.getValue()))
                .expectNextCount(1).verifyComplete();

    }

    @Test
    void testResetBatchIdToBatchRequests(){
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        IniPecBatchRequestRepositoryImpl iniPecBatchPollingRepository = new IniPecBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient);

        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(BatchRequest::new);
        when(dynamoDbAsyncTable.updateItem((BatchRequest) any())).thenReturn(completableFuture);

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("taxId");
        batchRequest.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
        batchRequest.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchRequest.setRetry(0);
        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setTimeStamp(LocalDateTime.now());
        List<BatchRequest> batchRequests = new ArrayList<>();
        batchRequests.add(batchRequest);

        StepVerifier.create(iniPecBatchPollingRepository.resetBatchIdToBatchRequests(batchRequests))
                .expectNextCount(1).verifyComplete();

    }

}
