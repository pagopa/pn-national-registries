package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerification;
import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImprese;
import it.pagopa.pn.national.registries.model.registroimprese.LegalAddress;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

    @MockBean
    InfoCamereConverter infoCamereConverter;
    @MockBean
    InfoCamereClient infoCamereClient;
    @MockBean
    IniPecBatchRequestRepository batchRequestRepository;

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

        StepVerifier.create(infoCamereService.getIniPecDigitalAddress("clientId", requestBodyDto))
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
        addressRegistroImpreseResponse.setTaxId("cf");
        LegalAddress legalAddress = new LegalAddress();
        legalAddress.setToponym("address");
        addressRegistroImpreseResponse.setAddress(legalAddress);

        when(infoCamereClient.getLegalAddress(any())).thenReturn(Mono.just(addressRegistroImpreseResponse));
        when(infoCamereConverter.mapToResponseOkByResponse(any())).thenReturn(response);

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
        addressRegistroImpreseResponse.setTimestamp("2022-10-01T10:00:00");

        when(infoCamereClient.getLegalAddress(any())).thenReturn(Mono.just(addressRegistroImpreseResponse));
        when(infoCamereConverter.checkIfResponseIsInfoCamereError(any())).thenReturn(true);
        when(infoCamereConverter.mapToResponseOkByRequest(any())).thenReturn(response);

        StepVerifier.create(infoCamereService.getRegistroImpreseLegalAddress(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void checkTaxIdAndVatNumber() {
        InfoCamereVerification response = new InfoCamereVerification();
        response.setTaxId("taxId");
        response.setVatNumber("vatNumber");

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
        response.setTimestamp("2022-10-01T10:00:00");

        InfoCamereLegalRequestBodyDto body = new InfoCamereLegalRequestBodyDto();
        InfoCamereLegalRequestBodyFilterDto dto = new InfoCamereLegalRequestBodyFilterDto();
        dto.setTaxId("taxId");
        dto.setVatNumber("vatNumber");
        body.setFilter(dto);

        InfoCamereLegalOKDto infoCamereLegalOKDto = new InfoCamereLegalOKDto();
        infoCamereLegalOKDto.setTaxId("taxId");
        infoCamereLegalOKDto.setVatNumber("vatNumber");
        infoCamereLegalOKDto.setVerificationResult(false);


        when(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(body.getFilter())).thenReturn(Mono.just(response));
        when(infoCamereConverter.checkIfResponseIsInfoCamereError(any())).thenReturn(true);
        when(infoCamereConverter.infoCamereResponseToDtoByRequest(body)).thenReturn(infoCamereLegalOKDto);

        StepVerifier.create(infoCamereService.checkTaxIdAndVatNumber(body))
                .expectNext(infoCamereLegalOKDto)
                .verifyComplete();
    }
}

