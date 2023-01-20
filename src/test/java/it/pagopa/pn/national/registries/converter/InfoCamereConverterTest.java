package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.InfoCamereLegalOKDto;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerificationResponse;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.Pec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;


import java.time.LocalDateTime;
import java.util.ArrayList;

import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.model.registroimprese.LegalAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {InfoCamereConverter.class})
@ExtendWith(SpringExtension.class)
class InfoCamereConverterTest {
    @Autowired
    private InfoCamereConverter infoCamereConverter;

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
        batchRequest.setTtl(0L);
        assertEquals("correlationId", infoCamereConverter.convertToGetAddressIniPecOKDto(batchRequest).getCorrelationId());
    }

    @Test
    void testCreateBatchPollingByBatchIdAndPollingId() {
        BatchPolling actualCreateBatchPollingByBatchIdAndPollingIdResult = infoCamereConverter
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
        batchRequest.setTtl(0L);

        ResponsePecIniPec responsePecIniPec = new ResponsePecIniPec();
        Pec pec = new Pec();
        pec.setCf("Cf");
        pec.setPecImpresa("pecImpresa");
        pec.setPecProfessionistas(new ArrayList<>());
        ArrayList<Pec> pecs = new ArrayList<>();
        pecs.add(pec);
        responsePecIniPec.setElencoPec(pecs);

        CodeSqsDto codeSqsDto = infoCamereConverter.convertoResponsePecToCodeSqsDto(batchRequest, responsePecIniPec);
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

        GetAddressRegistroImpreseOKDto actualMapToResponseOkResult = infoCamereConverter
                .mapToResponseOk(addressRegistroImpreseResponse);

        assertEquals("taxId", actualMapToResponseOkResult.getTaxId());
    }

    @Test
    void testInfoCamereResponseToDto() {

        InfoCamereVerificationResponse infoCamereVerificationResponse = new InfoCamereVerificationResponse();
        infoCamereVerificationResponse.setVerificationResult(true);
        infoCamereVerificationResponse.setVatNumber("vatNumber");
        infoCamereVerificationResponse.setTaxId("taxId");


        InfoCamereLegalOKDto actualResult = infoCamereConverter
                .infoCamereResponseToDto(infoCamereVerificationResponse);

        assertEquals("taxId", actualResult.getTaxId());
        assertEquals("vatNumber", actualResult.getVatNumber());
        assertEquals(true, actualResult.getVerificationResult());
    }
}

