package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereCommonError;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereInstitution;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereLegalInstituionsResponse;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerification;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.Pec;
import it.pagopa.pn.national.registries.model.inipec.IniPecPollingResponse;

import java.util.Collections;
import java.util.List;

import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImprese;
import it.pagopa.pn.national.registries.model.registroimprese.LegalAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;

@TestPropertySource(properties = {
        "pn.national.registries.inipec.ttl=0",
        "pn.national.registries.inipec.batchrequest.pk.separator=~"
})
@ContextConfiguration(classes = InfoCamereConverter.class)
@ExtendWith(SpringExtension.class)
class InfoCamereConverterTest {

    @Autowired
    private InfoCamereConverter infoCamereConverter;

    @MockBean
    private ObjectMapper objectMapper;

    @Test
    void testConvertToGetAddressIniPecOKDto() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("Cf");
        batchRequest.setCorrelationId("correlationId");
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
    void testConvertResponsePecToCodeSqsDtoCfNotFound() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setCf("cf");

        Pec pec = new Pec();
        pec.setCf("cf");
        IniPecPollingResponse iniPecPollingResponse = new IniPecPollingResponse();
        iniPecPollingResponse.setElencoPec(List.of(pec));

        CodeSqsDto codeSqsDto = infoCamereConverter.convertResponsePecToCodeSqsDto(batchRequest, iniPecPollingResponse);
        assertEquals("correlationId", codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getError());
        assertTrue(CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress()));
    }

    @Test
    void testConvertResponsePecToCodeSqsDto1() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("Cf");
        batchRequest.setCorrelationId("correlationId");

        Pec pec = new Pec();
        pec.setCf("Cf");
        pec.setPecImpresa("pecImpresa");
        pec.setPecProfessionista(Collections.emptyList());
        IniPecPollingResponse iniPecPollingResponse = new IniPecPollingResponse();
        iniPecPollingResponse.setElencoPec(List.of(pec));

        CodeSqsDto codeSqsDto = infoCamereConverter.convertResponsePecToCodeSqsDto(batchRequest, iniPecPollingResponse);
        assertNotNull(codeSqsDto);
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertEquals(1, codeSqsDto.getDigitalAddress().size());
        assertEquals("pecImpresa", codeSqsDto.getDigitalAddress().get(0).getAddress());
    }

    @Test
    void testConvertResponsePecToCodeSqsDto2() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("cf");
        batchRequest.setCorrelationId("correlationId");

        Pec pec = new Pec();
        pec.setCf("altro-cf");
        IniPecPollingResponse iniPecPollingResponse = new IniPecPollingResponse();
        iniPecPollingResponse.setElencoPec(List.of(pec));

        CodeSqsDto codeSqsDto = infoCamereConverter.convertResponsePecToCodeSqsDto(batchRequest, iniPecPollingResponse);
        assertEquals("correlationId", codeSqsDto.getCorrelationId());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertTrue(codeSqsDto.getDigitalAddress().isEmpty());
    }

    /**
     * Method under test: {@link InfoCamereConverter#infoCamereResponseToDtoByRequest(InfoCamereLegalRequestBodyDto)}
     */
    @Test
    void testInfoCamereResponseToDtoByRequest() {

        InfoCamereLegalRequestBodyDto infoCamereLegalRequestBodyDto = new InfoCamereLegalRequestBodyDto();
        InfoCamereLegalRequestBodyFilterDto filter = new InfoCamereLegalRequestBodyFilterDto();
        filter.setTaxId("taxId");
        filter.setVatNumber("vatNumber");
        infoCamereLegalRequestBodyDto.filter(filter);

        InfoCamereLegalOKDto actualInfoCamereResponseToDtoByRequestResult = infoCamereConverter.infoCamereResponseToDtoByRequest(infoCamereLegalRequestBodyDto);

        assertEquals("taxId", actualInfoCamereResponseToDtoByRequestResult.getTaxId());
        assertEquals("vatNumber", actualInfoCamereResponseToDtoByRequestResult.getVatNumber());
        assertFalse(actualInfoCamereResponseToDtoByRequestResult.getVerificationResult());
    }


    /**
     * Method under test: {@link InfoCamereConverter#mapToResponseOkByRequest(GetAddressRegistroImpreseRequestBodyDto)}
     */
    @Test
    void testMapToResponseOkByRequest() {

        GetAddressRegistroImpreseRequestBodyDto getAddressRegistroImpreseRequestBodyDto = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto filter = new GetAddressRegistroImpreseRequestBodyFilterDto();
        filter.setTaxId("taxId");
        getAddressRegistroImpreseRequestBodyDto.filter(filter);

        GetAddressRegistroImpreseOKDto response = infoCamereConverter.mapToResponseOkByRequest(getAddressRegistroImpreseRequestBodyDto);

        assertEquals("taxId", response.getTaxId());
        assertNull(response.getProfessionalAddress());
        assertNotNull(response.getTaxId());
        assertNotNull(response.getDateTimeExtraction());
    }

    /**
     * Method under test: {@link InfoCamereConverter#checkIfResponseIsInfoCamereError(InfoCamereCommonError)}
     */
    @Test
    void testCheckIfResponseIsInfoCamereError() {
        InfoCamereCommonError infoCamereCommonError = new InfoCamereCommonError();
        infoCamereCommonError.setAppName("appName");
        assertTrue(infoCamereConverter.checkIfResponseIsInfoCamereError(infoCamereCommonError));
    }

    @Test
    void testConvertIniPecRequestToSqsDto1() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setCf("cf");
        CodeSqsDto codeSqsDto = infoCamereConverter.convertIniPecRequestToSqsDto(batchRequest, null);
        assertEquals("correlationId", codeSqsDto.getCorrelationId());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertTrue(codeSqsDto.getDigitalAddress().isEmpty());
    }

    @Test
    void testConvertIniPecRequestToSqsDto2() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setCf("cf");
        CodeSqsDto codeSqsDto = infoCamereConverter.convertIniPecRequestToSqsDto(batchRequest, "error");
        assertEquals("correlationId", codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getDigitalAddress());
        assertEquals("error", codeSqsDto.getError());
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

        AddressRegistroImprese addressRegistroImpreseResponse = new AddressRegistroImprese();
        addressRegistroImpreseResponse.setAddress(legalAddress);
        addressRegistroImpreseResponse.setDate("2020-03-01");
        addressRegistroImpreseResponse.setTaxId("taxId");

        GetAddressRegistroImpreseOKDto actualMapToResponseOkResult = infoCamereConverter
                .mapToResponseOkByResponse(addressRegistroImpreseResponse);

        assertEquals("taxId", actualMapToResponseOkResult.getTaxId());
    }

    @Test
    void testInfoCamereResponseToDto() {
        InfoCamereVerification infoCamereVerificationResponse = new InfoCamereVerification();
        infoCamereVerificationResponse.setVerificationResult("OK");
        infoCamereVerificationResponse.setVatNumber("vatNumber");
        infoCamereVerificationResponse.setTaxId("taxId");

        InfoCamereLegalOKDto actualResult = infoCamereConverter
                .infoCamereResponseToDtoByResponse(infoCamereVerificationResponse);

        assertEquals("taxId", actualResult.getTaxId());
        assertEquals("vatNumber", actualResult.getVatNumber());
        assertEquals(true, actualResult.getVerificationResult());
    }

    @Test
    void mapToResponseOkByResponse() {
        InfoCamereLegalInstituionsResponse response = new InfoCamereLegalInstituionsResponse();
        response.setLegalTaxId("taxId");
        InfoCamereInstitution businessDto = new InfoCamereInstitution();
        businessDto.setBusinessTaxId("businessTaxId");
        businessDto.setBusinessName("businessName");
        response.setBusinessList(List.of(businessDto));
        response.setDateTimeExtraction("2020-03-01");

        InfoCamereLegalInstitutionsOKDto actualResult = infoCamereConverter
                .mapToResponseOkByResponse(response);

        assertEquals("taxId", actualResult.getLegalTaxId());
        assertEquals(1, actualResult.getBusinessList().size());
    }

    @Test
    void mapToResponseOkByResponse2() {
        InfoCamereLegalInstituionsResponse response = new InfoCamereLegalInstituionsResponse();
        response.setLegalTaxId("taxId");
        response.setDateTimeExtraction("2020-03-01");

        InfoCamereLegalInstitutionsOKDto actualResult = infoCamereConverter
                .mapToResponseOkByResponse(response);

        assertEquals("taxId", actualResult.getLegalTaxId());
        assertEquals(0, actualResult.getBusinessList().size());
    }
}
