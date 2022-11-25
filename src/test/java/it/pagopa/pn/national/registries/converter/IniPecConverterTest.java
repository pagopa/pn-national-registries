package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECOKDto;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.Pec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {IniPecConverter.class})
@ExtendWith(MockitoExtension.class)
class IniPecConverterTest {

    @InjectMocks
    private IniPecConverter iniPecConverter;

    @Test
    void testConvertToGetAddressIniPecOKDto() {
        BatchRequest requestCorrelation = new BatchRequest();
        requestCorrelation.setCorrelationId("correlationId");
        GetDigitalAddressIniPECOKDto response = iniPecConverter.convertToGetAddressIniPecOKDto(requestCorrelation);
        assertEquals(response.getCorrelationId(),"correlationId");
    }

    @Test
    void testCreateBatchPollingByBatchIdAndPollingId() {
        String batchId = "batchId";
        String pollingId = "pollingId";
        BatchPolling batchPolling = iniPecConverter.createBatchPollingByBatchIdAndPollingId(batchId, pollingId);
        assertEquals(batchPolling.getBatchId(),"batchId");
    }

    @Test
    void testConvertoResponsePecToCodeSqsDto1() {
        ArrayList<BatchRequest> batchRequests = new ArrayList<>();
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("Cf");
        batchRequest.setRetry(1);

        Pec pec = new Pec();
        pec.setCf("Cf");
        pec.setPecImpresa("Pec Impresa");
        pec.setPecProfessionistas(new ArrayList<>());

        ArrayList<Pec> pecList = new ArrayList<>();
        pecList.add(pec);
        ResponsePecIniPec responsePecIniPec = mock(ResponsePecIniPec.class);
        when(responsePecIniPec.getElencoPec()).thenReturn(pecList);
        doNothing().when(responsePecIniPec).setDataOraDownload(any());
        doNothing().when(responsePecIniPec).setElencoPec(any());
        doNothing().when(responsePecIniPec).setIdentificativoRichiesta(any());
        responsePecIniPec.setDataOraDownload("Data Ora Download");
        responsePecIniPec.setElencoPec(pecList);
        responsePecIniPec.setIdentificativoRichiesta("Identificativo Richiesta");
        List<CodeSqsDto> list = iniPecConverter.convertoResponsePecToCodeSqsDto(batchRequests,responsePecIniPec);
        assertEquals(list.size(),1);
    }

    @Test
    void testConvertoResponsePecToCodeSqsDto2() {
        ArrayList<BatchRequest> batchRequests = new ArrayList<>();
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("Cf");
        batchRequest.setRetry(1);
        batchRequests.add(batchRequest);

        Pec pec = new Pec();
        pec.setCf("Cf");
        pec.setPecImpresa("Pec Impresa");
        pec.setPecProfessionistas(new ArrayList<>());

        ArrayList<Pec> pecList = new ArrayList<>();
        pecList.add(pec);
        ResponsePecIniPec responsePecIniPec = mock(ResponsePecIniPec.class);
        when(responsePecIniPec.getElencoPec()).thenReturn(pecList);
        doNothing().when(responsePecIniPec).setDataOraDownload(any());
        doNothing().when(responsePecIniPec).setElencoPec(any());
        doNothing().when(responsePecIniPec).setIdentificativoRichiesta(any());
        responsePecIniPec.setDataOraDownload("Data Ora Download");
        responsePecIniPec.setElencoPec(pecList);
        responsePecIniPec.setIdentificativoRichiesta("Identificativo Richiesta");
        List<CodeSqsDto> list = iniPecConverter.convertoResponsePecToCodeSqsDto(batchRequests,responsePecIniPec);
        assertEquals(list.size(),1);
    }

    @Test
    void testConvertoResponsePecToCodeSqsDto3() {
        ArrayList<BatchRequest> batchRequests = new ArrayList<>();
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("Cf");
        batchRequest.setRetry(4);
        batchRequests.add(batchRequest);

        Pec pec = new Pec();
        pec.setCf("Cf");
        pec.setPecImpresa("Pec Impresa");
        pec.setPecProfessionistas(new ArrayList<>());

        ArrayList<Pec> pecList = new ArrayList<>();
        pecList.add(pec);
        ResponsePecIniPec responsePecIniPec = mock(ResponsePecIniPec.class);
        when(responsePecIniPec.getElencoPec()).thenReturn(pecList);
        doNothing().when(responsePecIniPec).setDataOraDownload(any());
        doNothing().when(responsePecIniPec).setElencoPec(any());
        doNothing().when(responsePecIniPec).setIdentificativoRichiesta(any());
        responsePecIniPec.setDataOraDownload("Data Ora Download");
        responsePecIniPec.setElencoPec(pecList);
        responsePecIniPec.setIdentificativoRichiesta("Identificativo Richiesta");
        List<CodeSqsDto> list = iniPecConverter.convertoResponsePecToCodeSqsDto(batchRequests,responsePecIniPec);
        assertEquals(list.size(),1);
    }
}

