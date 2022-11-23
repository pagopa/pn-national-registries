package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.model.inipec.Pec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IniPecConverterTest {

    @InjectMocks
    private IniPecConverter iniPecConverter;

    @Test
    void testConvertToGetAddressIniPecOKDto(){
        BatchRequest requestCorrelation = new BatchRequest();
        requestCorrelation.setCorrelationId("correlationId");

        assertNotNull(iniPecConverter.convertToGetAddressIniPecOKDto(requestCorrelation).getCorrelationId());
    }

    @Test
    void testCreateBatchPollingByBatchIdAndPollingId(){
        String batchId = "batchId";
        String pollingId = "pollingId";

        assertNotNull(iniPecConverter.createBatchPollingByBatchIdAndPollingId(batchId,pollingId).getClass());
    }

    @Test
    void testConvertoResponsePecToCodeSqsDto(){
        ResponsePecIniPec responsePecIniPec = new ResponsePecIniPec();
        String status = "status";
        String description = "description";
        List<Pec> pecs = new ArrayList<>();
        Pec pec = new Pec();
        pec.setCf("cf");
        pec.setPecImpresa("pecImpresa");
        pec.setPecProfessionistas(new ArrayList<>());
        pecs.add(pec);

        responsePecIniPec.setElencoPec(pecs);
        responsePecIniPec.setDataOraDownload("oraDownload");
        responsePecIniPec.setIdentificativoRichiesta("identificativoRichiesta");

        assertNotNull(iniPecConverter.convertoResponsePecToCodeSqsDto(responsePecIniPec,status,description).getClass());
    }

    @Test
    void testConvertoResponsePecToCodeSqsDtoTwo(){
        ResponsePecIniPec responsePecIniPec = new ResponsePecIniPec();
        String status = "status";
        String description = "description";

        responsePecIniPec.setElencoPec(null);
        responsePecIniPec.setDataOraDownload("oraDownload");
        responsePecIniPec.setIdentificativoRichiesta("identificativoRichiesta");

        assertNotNull(iniPecConverter.convertoResponsePecToCodeSqsDto(responsePecIniPec,status,description).getClass());
    }
}

