package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.Pec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {IniPecConverter.class})
@ExtendWith(SpringExtension.class)
class IniPecConverterTest {
    @Autowired
    private IniPecConverter iniPecConverter;

    @Test
    void testConvertToGetAddressIniPecOKDto() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setBatchId("batchId");
        batchRequest.setCf("Cf");
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setRetry(1);
        batchRequest.setStatus("Status");
        batchRequest.setTimeStamp(LocalDateTime.now());
        batchRequest.setTtl(LocalDateTime.now());
        assertEquals("correlationId", iniPecConverter.convertToGetAddressIniPecOKDto(batchRequest).getCorrelationId());
    }

    @Test
    void testCreateBatchPollingByBatchIdAndPollingId() {
        BatchPolling actualCreateBatchPollingByBatchIdAndPollingIdResult = iniPecConverter
                .createBatchPollingByBatchIdAndPollingId("batchId", "pollingId");
        assertEquals("batchId", actualCreateBatchPollingByBatchIdAndPollingIdResult.getBatchId());
        assertEquals("NOT_WORKED", actualCreateBatchPollingByBatchIdAndPollingIdResult.getStatus());
        assertEquals("pollingId", actualCreateBatchPollingByBatchIdAndPollingIdResult.getPollingId());
    }

    @Test
    void testConvertoResponsePecToCodeSqsDto5() {
        BatchRequest batchRequest = new BatchRequest();

        batchRequest.setBatchId("batchId");
        batchRequest.setCf("Cf");
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setLastReserved(LocalDateTime.now());
        batchRequest.setRetry(1);
        batchRequest.setStatus("Status");
        batchRequest.setTimeStamp(LocalDateTime.now());
        batchRequest.setTtl(LocalDateTime.now());

        ResponsePecIniPec responsePecIniPec = new ResponsePecIniPec();
        Pec pec = new Pec();
        pec.setCf("Cf");
        pec.setPecImpresa("pecImpresa");
        pec.setPecProfessionistas(new ArrayList<>());
        ArrayList<Pec> pecs = new ArrayList<>();
        pecs.add(pec);
        responsePecIniPec.setElencoPec(pecs);

        CodeSqsDto codeSqsDto = iniPecConverter.convertoResponsePecToCodeSqsDto(batchRequest, responsePecIniPec);
        assertNotNull(codeSqsDto);
    }
}

