package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.AddressRegistroImprese;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.InfoCamereLegalInstituionsResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.InfoCamereVerification;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.LegalAddress;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.Date;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@Slf4j
@TestPropertySource(properties = {
        "pn.national.registries.inipec.ttl=0"
})
@ContextConfiguration(classes = {InfoCamereService.class})
@ExtendWith(SpringExtension.class)
class InfoCamereServiceTest {

    @Autowired
    InfoCamereService infoCamereService;

    @MockitoBean
    InfoCamereConverter infoCamereConverter;
    @MockitoBean
    InfoCamereClient infoCamereClient;
    @MockitoBean
    IniPecBatchRequestRepository batchRequestRepository;

    @MockitoBean
    ValidateTaxIdUtils validateTaxIdUtils;

    @Test
    void testGetDigitalAddress() {
        GetDigitalAddressIniPECRequestBodyDto requestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto dto = new GetDigitalAddressIniPECRequestBodyFilterDto();
        dto.setCorrelationId("correlationId");
        dto.setTaxId("taxId");
        requestBodyDto.setFilter(dto);

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("taxId");
        batchRequest.setCorrelationId("correlationId");
        when(batchRequestRepository.create(any()))
                .thenReturn(Mono.just(batchRequest));

        GetDigitalAddressIniPECOKDto getDigitalAddressIniPECOKDto = new GetDigitalAddressIniPECOKDto();
        getDigitalAddressIniPECOKDto.setCorrelationId("correlationId");
        when(infoCamereConverter.convertToGetAddressIniPecOKDto(any()))
                .thenReturn(getDigitalAddressIniPECOKDto);

        StepVerifier.create(infoCamereService.getIniPecDigitalAddress("clientId", requestBodyDto, new Date()))
                .expectNext(getDigitalAddressIniPECOKDto)
                .verifyComplete();
    }

    @Test
    void getAddress() {
        GetAddressRegistroImpreseRequestBodyDto request = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto dto = new GetAddressRegistroImpreseRequestBodyFilterDto();
        dto.setTaxId("cf");
        request.setFilter(dto);

        GetAddressRegistroImpreseOKDto response = new GetAddressRegistroImpreseOKDto();
        response.setTaxId("cf");
        GetAddressRegistroImpreseOKProfessionalAddressDto professional = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        professional.setAddress("address");
        response.setProfessionalAddress(professional);

        AddressRegistroImprese addressRegistroImpreseResponse = new AddressRegistroImprese();
        addressRegistroImpreseResponse.setCf("cf");
        LegalAddress legalAddress = new LegalAddress();
        legalAddress.setToponimo("address");
        addressRegistroImpreseResponse.setIndirizzoLocalizzazione(legalAddress);

        when(infoCamereClient.getLegalAddress(any())).thenReturn(Mono.just(addressRegistroImpreseResponse));
        when(infoCamereConverter.mapToResponseOkByResponse((AddressRegistroImprese) any())).thenReturn(response);

        StepVerifier.create(infoCamereService.getRegistroImpreseLegalAddress(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void getAddressWhenNotFound() {
        GetAddressRegistroImpreseRequestBodyDto request = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto dto = new GetAddressRegistroImpreseRequestBodyFilterDto();
        dto.setTaxId("cf");
        request.setFilter(dto);

        GetAddressRegistroImpreseOKDto response = new GetAddressRegistroImpreseOKDto();
        response.setTaxId("cf");
        GetAddressRegistroImpreseOKProfessionalAddressDto professional = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        response.setProfessionalAddress(professional);
        response.setDateTimeExtraction(new Date());

        AddressRegistroImprese addressRegistroImpreseResponse = new AddressRegistroImprese();
        addressRegistroImpreseResponse.setCode("err-sede");
        addressRegistroImpreseResponse.setDescription("description");
        addressRegistroImpreseResponse.setAppName("appName");
        addressRegistroImpreseResponse.setTimestamp(OffsetDateTime.now().toString());

        when(infoCamereClient.getLegalAddress(any())).thenReturn(Mono.just(addressRegistroImpreseResponse));
        when(infoCamereConverter.checkIfResponseIsInfoCamereError((AddressRegistroImprese)any())).thenReturn(true);
        when(infoCamereConverter.mapToResponseOkByRequest((GetAddressRegistroImpreseRequestBodyDto) any())).thenReturn(response);

        StepVerifier.create(infoCamereService.getRegistroImpreseLegalAddress(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void checkTaxIdAndVatNumber() {
        InfoCamereVerification response = new InfoCamereVerification();
        response.setCfPersona("taxId");
        response.setCfImpresa("vatNumber");

        InfoCamereLegalRequestBodyDto body = new InfoCamereLegalRequestBodyDto();
        InfoCamereLegalRequestBodyFilterDto dto = new InfoCamereLegalRequestBodyFilterDto();
        dto.setTaxId("taxId");
        dto.setVatNumber("vatNumber");
        body.setFilter(dto);

        InfoCamereLegalOKDto infoCamereLegalOKDto = new InfoCamereLegalOKDto();
        infoCamereLegalOKDto.setTaxId("taxId");
        infoCamereLegalOKDto.setVatNumber("vatNumber");
        infoCamereLegalOKDto.setVerificationResult(true);


        when(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(body.getFilter())).thenReturn(Mono.just(response));
        when(infoCamereConverter.infoCamereResponseToDtoByResponse(response)).thenReturn(infoCamereLegalOKDto);

        StepVerifier.create(infoCamereService.checkTaxIdAndVatNumber(body))
                .expectNext(infoCamereLegalOKDto)
                .verifyComplete();
    }

    @Test
    void checkTaxIdAndVatNumberWhenNotFound() {
        InfoCamereVerification response = new InfoCamereVerification();
        response.setCode("err-lrpunt");
        response.setDescription("description");
        response.setAppName("appName");
        response.setTimestamp(OffsetDateTime.now().toString());

        InfoCamereLegalRequestBodyDto body = new InfoCamereLegalRequestBodyDto();
        InfoCamereLegalRequestBodyFilterDto dto = new InfoCamereLegalRequestBodyFilterDto();
        dto.setTaxId("taxIdTest");
        dto.setVatNumber("vatNumber");
        body.setFilter(dto);

        InfoCamereLegalOKDto infoCamereLegalOKDto = new InfoCamereLegalOKDto();
        infoCamereLegalOKDto.setTaxId("taxIdTest");
        infoCamereLegalOKDto.setVatNumber("vatNumber");
        infoCamereLegalOKDto.setVerificationResult(false);


        when(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(body.getFilter())).thenReturn(Mono.just(response));
        when(infoCamereConverter.checkIfResponseIsInfoCamereError((InfoCamereVerification) any())).thenReturn(true);
        when(infoCamereConverter.infoCamereResponseToDtoByRequest(body)).thenReturn(infoCamereLegalOKDto);

        StepVerifier.create(infoCamereService.checkTaxIdAndVatNumber(body))
                .expectNext(infoCamereLegalOKDto)
                .verifyComplete();
    }

    @Test
    void getInstitutions() {
        InfoCamereLegalInstitutionsRequestBodyDto request = new InfoCamereLegalInstitutionsRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto dto = new CheckTaxIdRequestBodyFilterDto();
        dto.setTaxId("cf");
        request.setFilter(dto);

        InfoCamereLegalInstitutionsOKDto response = new InfoCamereLegalInstitutionsOKDto();
        response.setLegalTaxId("cf");

        InfoCamereLegalInstituionsResponse infoCamereLegalInstituionsResponse = new InfoCamereLegalInstituionsResponse();
        infoCamereLegalInstituionsResponse.setCfPersona("cf");


        when(infoCamereClient.getLegalInstitutions(any())).thenReturn(Mono.just(infoCamereLegalInstituionsResponse));
        when(infoCamereConverter.mapToResponseOkByResponse((InfoCamereLegalInstituionsResponse) any())).thenReturn(response);

        StepVerifier.create(infoCamereService.getLegalInstitutions(request))
                .expectNext(response)
                .verifyComplete();
    }
}

