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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
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

        ElencoPecElement pec = new ElencoPecElement();
        pec.setCf("cf");
        GetElencoPec200Response elencoPec200Response = new GetElencoPec200Response();
        elencoPec200Response.setElencoPec(List.of(pec));

        CodeSqsDto codeSqsDto = infoCamereConverter.convertResponsePecToCodeSqsDto(batchRequest, elencoPec200Response);
        assertEquals("correlationId", codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getError());
        assertTrue(CollectionUtils.isEmpty(codeSqsDto.getDigitalAddress()));
    }

    @Test
    void testConvertResponsePecToCodeSqsDto1() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("Cf");
        batchRequest.setCorrelationId("correlationId");

        ElencoPecElement pec = new ElencoPecElement();
        pec.setCf("Cf");
        pec.setPecImpresa("pecImpresa");
        pec.setPecProfessionistas(Collections.emptyList());
        GetElencoPec200Response iniPecPollingResponse = new GetElencoPec200Response();
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

        ElencoPecElement pec = new ElencoPecElement();
        pec.setCf("altro-cf");
        GetElencoPec200Response elencoPec200Response = new GetElencoPec200Response();
        elencoPec200Response.setElencoPec(List.of(pec));

        CodeSqsDto codeSqsDto = infoCamereConverter.convertResponsePecToCodeSqsDto(batchRequest, elencoPec200Response);
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
     * Method under test: {@link InfoCamereConverter#checkIfResponseIsInfoCamereError(GetElencoPec200Response)}
     */
    @Test
    void testCheckIfResponseIsInfoCamereError() {
        GetElencoPec200Response iniPecPollingResponse = new GetElencoPec200Response();
        iniPecPollingResponse.setAppName("appName");
        assertTrue(infoCamereConverter.checkIfResponseIsInfoCamereError(iniPecPollingResponse));
    }

    @Test
    void testCheckIfResponseIsInfoCamereErrorAddressRegistroImprese() {
        RecuperoSedeImpresa200Response recuperoSedeImpresa200Response = new RecuperoSedeImpresa200Response();
        recuperoSedeImpresa200Response.setAppName("appName");
        assertTrue(infoCamereConverter.checkIfResponseIsInfoCamereError(recuperoSedeImpresa200Response));
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
        IndirizzoLocalizzazioneDTO indirizzoLocalizzazioneDTO = new IndirizzoLocalizzazioneDTO();
        indirizzoLocalizzazioneDTO.setVia("42 Main St");
        indirizzoLocalizzazioneDTO.setComune("Municipality");
        indirizzoLocalizzazioneDTO.setCap("Postal Code");
        indirizzoLocalizzazioneDTO.setProvincia("Province");
        indirizzoLocalizzazioneDTO.setVia("Street");
        indirizzoLocalizzazioneDTO.setGetnCivico("42");
        indirizzoLocalizzazioneDTO.setToponimo("Toponym");

        RecuperoSedeImpresa200Response recuperoSedeImpresa200Response = new RecuperoSedeImpresa200Response();
        recuperoSedeImpresa200Response.setIndirizzoLocalizzazione(indirizzoLocalizzazioneDTO);
        recuperoSedeImpresa200Response.setDataOraEstrazione(OffsetDateTime.now());
        recuperoSedeImpresa200Response.setCf("taxId");

        GetAddressRegistroImpreseOKDto actualMapToResponseOkResult = infoCamereConverter
                .mapToResponseOkByResponse(recuperoSedeImpresa200Response);

        assertEquals("taxId", actualMapToResponseOkResult.getTaxId());
    }

    @Test
    void mapToResponseOkByResponse() {
        LegaleRappresentanteLista200Response response = new LegaleRappresentanteLista200Response();
        response.setCfPersona("taxId");
        ElencoImpreseRappresentate elencoImpreseRappresentate = new ElencoImpreseRappresentate();
        elencoImpreseRappresentate.setCfImpresa("businessTaxId");
        elencoImpreseRappresentate.setDenominazione("businessName");
        response.setElencoImpreseRappresentate(List.of(elencoImpreseRappresentate));
        response.setDataOraEstrazione(new Date());

        InfoCamereLegalInstitutionsOKDto actualResult = infoCamereConverter
                .mapToResponseOkByResponse(response);

        assertEquals("taxId", actualResult.getLegalTaxId());
        assertEquals(1, actualResult.getBusinessList().size());
    }

    @Test
    void mapToResponseOkByResponse2() {
        LegaleRappresentanteLista200Response response = new LegaleRappresentanteLista200Response();
        response.setCfPersona("taxId");
        response.setDataOraEstrazione(new Date());

        InfoCamereLegalInstitutionsOKDto actualResult = infoCamereConverter
                .mapToResponseOkByResponse(response);

        assertEquals("taxId", actualResult.getLegalTaxId());
        assertEquals(0, actualResult.getBusinessList().size());
    }
}
