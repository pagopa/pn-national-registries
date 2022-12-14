package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseOKProfessionalAddressDto;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.Pec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.model.registroImprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.model.registroImprese.LegalAddress;

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

    @Test
    void testMapToResponseOk() {
        LegalAddress legalAddress = new LegalAddress();
        legalAddress.setAddress("42 Main St");
        legalAddress.setMunicipality("Municipality");
        legalAddress.setPostalCode("Postal Code");
        legalAddress.setProvince("Province");
        legalAddress.setStreet("Street");
        legalAddress.setStreetNumber("42");
        legalAddress.setToponym("Toponym");

        AddressRegistroImpreseResponse addressRegistroImpreseResponse = new AddressRegistroImpreseResponse();
        addressRegistroImpreseResponse.setAddress(legalAddress);
        addressRegistroImpreseResponse.setDate("2020-03-01");
        addressRegistroImpreseResponse.setTaxId("taxId");

        GetAddressRegistroImpreseOKDto actualMapToResponseOkResult = iniPecConverter
                .mapToResponseOk(addressRegistroImpreseResponse);

        assertEquals("taxId", actualMapToResponseOkResult.getTaxId());
    }

}

