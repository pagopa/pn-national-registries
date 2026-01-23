package it.pagopa.pn.national.registries.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
        "pn.national.registries.inipec.ttl=0",
        "pn.national.registries.inipec.batchrequest.pk.separator=~"
})
@ContextConfiguration(classes = InfoCamereConverter.class)
@ExtendWith(SpringExtension.class)
class InfoCamereConverterTest {

    @Autowired
    private InfoCamereConverter infoCamereConverter;

    @MockitoBean
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
     * Method under test: {@link InfoCamereConverter#checkIfResponseIsInfoCamereError(IniPecPollingResponse)}
     */
    @Test
    void testCheckIfResponseIsInfoCamereError() {
        IniPecPollingResponse iniPecPollingResponse = new IniPecPollingResponse();
        iniPecPollingResponse.setAppName("appName");
        assertTrue(infoCamereConverter.checkIfResponseIsInfoCamereError(iniPecPollingResponse));
    }

    @Test
    void testCheckIfResponseIsInfoCamereErrorAddressRegistroImprese() {
        AddressRegistroImprese addressRegistroImprese = new AddressRegistroImprese();
        addressRegistroImprese.setAppName("appName");
        assertTrue(infoCamereConverter.checkIfResponseIsInfoCamereError(addressRegistroImprese));
    }

    @Test
    void testCheckIfResponseIsInfoCamereErrorInfoCamereVerification() {
        InfoCamereVerification infoCamereVerification = new InfoCamereVerification();
        infoCamereVerification.setAppName("appName");
        assertTrue(infoCamereConverter.checkIfResponseIsInfoCamereError(infoCamereVerification));
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
        legalAddress.setVia("42 Main St");
        legalAddress.setComune("Municipality");
        legalAddress.setCap("Postal Code");
        legalAddress.setProvincia("Province");
        legalAddress.setVia("Street");
        legalAddress.setnCivico("42");
        legalAddress.setToponimo("Toponym");

        AddressRegistroImprese addressRegistroImpreseResponse = new AddressRegistroImprese();
        addressRegistroImpreseResponse.setIndirizzoLocalizzazione(legalAddress);
        addressRegistroImpreseResponse.setDataOraEstrazione(OffsetDateTime.now().toString());
        addressRegistroImpreseResponse.setCf("taxId");

        GetAddressRegistroImpreseOKDto actualMapToResponseOkResult = infoCamereConverter
                .mapToResponseOkByResponse(addressRegistroImpreseResponse);

        assertEquals("taxId", actualMapToResponseOkResult.getTaxId());
    }

    @Test
    void testMapToResponseWith_Toponimo_Via_nCivico_Null() {
        LegalAddress legalAddress = new LegalAddress();
        legalAddress.setVia(null);
        legalAddress.setComune("Municipality");
        legalAddress.setCap("Postal Code");
        legalAddress.setProvincia("Province");
        legalAddress.setnCivico(null);
        legalAddress.setToponimo(null);

        AddressRegistroImprese addressRegistroImpreseResponse = new AddressRegistroImprese();
        addressRegistroImpreseResponse.setIndirizzoLocalizzazione(legalAddress);
        addressRegistroImpreseResponse.setDataOraEstrazione(OffsetDateTime.now().toString());
        addressRegistroImpreseResponse.setCf("taxId");

        GetAddressRegistroImpreseOKDto actualMapToResponseOkResult = infoCamereConverter
                .mapToResponseOkByResponse(addressRegistroImpreseResponse);

        assertEquals("taxId", actualMapToResponseOkResult.getTaxId());
        assertEquals("", actualMapToResponseOkResult.getProfessionalAddress().getAddress());
    }

    @Test
    void testMapToResponseWith_Toponimo_Via_nCivico_VoidString() {
        LegalAddress legalAddress = new LegalAddress();
        legalAddress.setVia("");
        legalAddress.setComune("Municipality");
        legalAddress.setCap("Postal Code");
        legalAddress.setProvincia("Province");
        legalAddress.setnCivico("");
        legalAddress.setToponimo("");

        AddressRegistroImprese addressRegistroImpreseResponse = new AddressRegistroImprese();
        addressRegistroImpreseResponse.setIndirizzoLocalizzazione(legalAddress);
        addressRegistroImpreseResponse.setDataOraEstrazione(OffsetDateTime.now().toString());
        addressRegistroImpreseResponse.setCf("taxId");

        GetAddressRegistroImpreseOKDto actualMapToResponseOkResult = infoCamereConverter
                .mapToResponseOkByResponse(addressRegistroImpreseResponse);

        assertEquals("taxId", actualMapToResponseOkResult.getTaxId());
        assertEquals("", actualMapToResponseOkResult.getProfessionalAddress().getAddress());
    }

    @Test
    void testMapToResponseWithout_nCivico() {
        LegalAddress legalAddress = new LegalAddress();
        legalAddress.setVia("Via");
        legalAddress.setComune("Municipality");
        legalAddress.setCap("Postal Code");
        legalAddress.setProvincia("Province");
        legalAddress.setToponimo("Toponimo");

        AddressRegistroImprese addressRegistroImpreseResponse = new AddressRegistroImprese();
        addressRegistroImpreseResponse.setIndirizzoLocalizzazione(legalAddress);
        addressRegistroImpreseResponse.setDataOraEstrazione(OffsetDateTime.now().toString());
        addressRegistroImpreseResponse.setCf("taxId");

        GetAddressRegistroImpreseOKDto actualMapToResponseOkResult = infoCamereConverter
                .mapToResponseOkByResponse(addressRegistroImpreseResponse);

        assertEquals("taxId", actualMapToResponseOkResult.getTaxId());
        assertEquals("Toponimo Via", actualMapToResponseOkResult.getProfessionalAddress().getAddress());
    }

    @Test
    void testInfoCamereResponseToDto() {
        InfoCamereVerification infoCamereVerificationResponse = new InfoCamereVerification();
        infoCamereVerificationResponse.setEsitoVerifica("OK");
        infoCamereVerificationResponse.setCfImpresa("vatNumber");
        infoCamereVerificationResponse.setCfPersona("taxId");

        InfoCamereLegalOKDto actualResult = infoCamereConverter
                .infoCamereResponseToDtoByResponse(infoCamereVerificationResponse);

        assertEquals("taxId", actualResult.getTaxId());
        assertEquals("vatNumber", actualResult.getVatNumber());
        assertEquals(true, actualResult.getVerificationResult());
    }

    @Test
    void mapToResponseOkByResponse() {
        InfoCamereLegalInstituionsResponse response = new InfoCamereLegalInstituionsResponse();
        response.setCfPersona("taxId");
        InfoCamereInstitution businessDto = new InfoCamereInstitution();
        businessDto.setCfImpresa("businessTaxId");
        businessDto.setDenominazione("businessName");
        response.setElencoImpreseRappresentate(List.of(businessDto));
        response.setDataOraEstrazione(OffsetDateTime.now().toString());

        InfoCamereLegalInstitutionsOKDto actualResult = infoCamereConverter
                .mapToResponseOkByResponse(response);

        assertEquals("taxId", actualResult.getLegalTaxId());
        assertEquals(1, actualResult.getBusinessList().size());
    }

    @Test
    void mapToResponseOkByResponse2() {
        InfoCamereLegalInstituionsResponse response = new InfoCamereLegalInstituionsResponse();
        response.setCfPersona("taxId");
        response.setDataOraEstrazione(OffsetDateTime.now().toString());

        InfoCamereLegalInstitutionsOKDto actualResult = infoCamereConverter
                .mapToResponseOkByResponse(response);

        assertEquals("taxId", actualResult.getLegalTaxId());
        assertEquals(0, actualResult.getBusinessList().size());
    }
}
