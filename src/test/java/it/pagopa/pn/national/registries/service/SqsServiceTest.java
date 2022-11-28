package it.pagopa.pn.national.registries.service;

import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;

import org.junit.jupiter.api.Test;

class SqsServiceTest {

    @Test
    void testPush() {
        AmazonSQSAsyncClient amazonSQS = new AmazonSQSAsyncClient();
        SqsService sqsService = new SqsService(amazonSQS, new ObjectMapper());

        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId("corelationId");
        sqsService.push(codeSqsDto);
    }

}

