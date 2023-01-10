package it.pagopa.pn.national.registries.service;

import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SqsServiceTest {

    @Test
    void testPush() {
        AmazonSQSAsyncClient amazonSQS = mock(AmazonSQSAsyncClient.class);
        GetQueueUrlResult getQueueUrlResult = new GetQueueUrlResult();
        getQueueUrlResult.setQueueUrl("");
        when(amazonSQS.getQueueUrl((String) any())).thenReturn(getQueueUrlResult);
        SqsService sqsService = new SqsService("", amazonSQS, new ObjectMapper());

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId("corelationId");
        assertDoesNotThrow(() -> sqsService.push(codeSqsDto));
    }

}

