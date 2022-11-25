package it.pagopa.pn.national.registries.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SqsRepositoryImplTest.class)
class SqsRepositoryImplTest {

    /*
    @Test
    void testPush(){
        SqsClient sqsClient = mock(SqsClient.class);
        ObjectMapper mapper = mock(ObjectMapper.class);

        SqsRepositoryImpl sqsRepository = new SqsRepositoryImpl(sqsClient,mapper);

        List<List<CodeSqsDto>> ms = new ArrayList<>();
        List<CodeSqsDto> msges = new ArrayList<>();
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId("correlationId");
        msges.add(codeSqsDto);
        ms.add(msges);

        sqsRepository.push(ms);
        assertEquals(msges.size(),1);
    }
    */
}
