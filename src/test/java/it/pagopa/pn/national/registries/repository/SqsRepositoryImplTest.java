package it.pagopa.pn.national.registries.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.config.SqsConfiguration;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import javax.validation.constraints.AssertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SqsRepositoryImplTest.class)
class SqsRepositoryImplTest {

    @MockBean
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @MockBean
    private DynamoDbAsyncTable<Object> dynamoDbAsyncTable;

    @Test
    void testPush(){
        SqsClient sqsClient = mock(SqsClient.class);
        ObjectMapper mapper = mock(ObjectMapper.class);

        SqsRepositoryImpl sqsRepository = new SqsRepositoryImpl(sqsClient,mapper);

        List<CodeSqsDto> msges = new ArrayList<>();
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId("correlationId");
        msges.add(codeSqsDto);

        sqsRepository.push(msges);
        assertEquals(msges.size(),1);
    }

}
