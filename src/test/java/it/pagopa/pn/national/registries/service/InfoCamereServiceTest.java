package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerificationResponse;
import it.pagopa.pn.national.registries.model.registroImprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.model.registroImprese.LegalAddress;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {InfoCamereService.class})
@ExtendWith(MockitoExtension.class)
@Slf4j
class InfoCamereServiceTest {
    @InjectMocks
    InfoCamereService infoCamereService;
    @Mock
    InfoCamereConverter infoCamereConverter;

    @Mock
    InfoCamereClient infoCamereClient;
    @Mock
    IniPecBatchRequestRepositoryImpl iniPecBatchRequestRepository;

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
        when(iniPecBatchRequestRepository.createBatchRequest(any()))
                .thenReturn(Mono.just(batchRequest));


        GetDigitalAddressIniPECOKDto getDigitalAddressIniPECOKDto = new GetDigitalAddressIniPECOKDto();
        getDigitalAddressIniPECOKDto.setCorrelationId("correlationId");
        when(infoCamereConverter.convertToGetAddressIniPecOKDto(any()))
                .thenReturn(getDigitalAddressIniPECOKDto);
        StepVerifier.create( infoCamereService.getIniPecDigitalAddress(requestBodyDto))
                .expectNext(getDigitalAddressIniPECOKDto).verifyComplete();
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

        AddressRegistroImpreseResponse addressRegistroImpreseResponse = new AddressRegistroImpreseResponse();
        addressRegistroImpreseResponse.setTaxId("cf");
        LegalAddress legalAddress = new LegalAddress();
        legalAddress.setToponym("address");
        addressRegistroImpreseResponse.setAddress(legalAddress);

        when(infoCamereClient.getLegalAddress(any())).thenReturn(Mono.just(addressRegistroImpreseResponse));
        when(infoCamereConverter.mapToResponseOk(any())).thenReturn(response);

        StepVerifier.create(infoCamereService.getRegistroImpreseLegalAddress(request))
                .expectNext(response).verifyComplete();
    }

    @Test
    void checkTaxIdAndVatNumber() {

        InfoCamereVerificationResponse response = new InfoCamereVerificationResponse();
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
        when(infoCamereConverter.infoCamereResponseToDto(response)).thenReturn(infoCamereLegalOKDto);

        StepVerifier.create(infoCamereService.checkTaxIdAndVatNumber(body))
                .expectNext(infoCamereLegalOKDto);
    }
}

