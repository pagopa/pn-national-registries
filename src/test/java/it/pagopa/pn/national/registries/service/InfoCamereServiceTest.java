package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IndirizzoLocalizzazioneDTO;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.LegaleRappresentanteLista200Response;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.RecuperoSedeImpresa200Response;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
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

    @MockBean
    InfoCamereConverter infoCamereConverter;
    @MockBean
    InfoCamereClient infoCamereClient;
    @MockBean
    IniPecBatchRequestRepository batchRequestRepository;

    @MockBean
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

        StepVerifier.create(infoCamereService.getIniPecDigitalAddress("clientId", requestBodyDto, null))
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

        RecuperoSedeImpresa200Response recuperoSedeImpresa200Response = new RecuperoSedeImpresa200Response();
        recuperoSedeImpresa200Response.setCf("cf");
        IndirizzoLocalizzazioneDTO indirizzoLocalizzazioneDTO = new IndirizzoLocalizzazioneDTO();
        indirizzoLocalizzazioneDTO.setToponimo("address");
        recuperoSedeImpresa200Response.setIndirizzoLocalizzazione(indirizzoLocalizzazioneDTO);

        when(infoCamereClient.getLegalAddress(any())).thenReturn(Mono.just(recuperoSedeImpresa200Response));
        when(infoCamereConverter.mapToResponseOkByResponse((RecuperoSedeImpresa200Response) any())).thenReturn(response);

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

        RecuperoSedeImpresa200Response recuperoSedeImpresa200Response = new RecuperoSedeImpresa200Response();
        recuperoSedeImpresa200Response.setCode("err-sede");
        recuperoSedeImpresa200Response.setDescription("description");
        recuperoSedeImpresa200Response.setAppName("appName");
        recuperoSedeImpresa200Response.setTimestamp(OffsetDateTime.now());

        when(infoCamereClient.getLegalAddress(any())).thenReturn(Mono.just(recuperoSedeImpresa200Response));
        when(infoCamereConverter.checkIfResponseIsInfoCamereError((RecuperoSedeImpresa200Response)any())).thenReturn(true);
        when(infoCamereConverter.mapToResponseOkByRequest((GetAddressRegistroImpreseRequestBodyDto) any())).thenReturn(response);

        StepVerifier.create(infoCamereService.getRegistroImpreseLegalAddress(request))
                .expectNext(response)
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

        LegaleRappresentanteLista200Response infoCamereLegalInstituionsResponse = new LegaleRappresentanteLista200Response();
        infoCamereLegalInstituionsResponse.setCfPersona("cf");


        when(infoCamereClient.getLegalInstitutions(any())).thenReturn(Mono.just(infoCamereLegalInstituionsResponse));
        when(infoCamereConverter.mapToResponseOkByResponse((LegaleRappresentanteLista200Response) any())).thenReturn(response);

        StepVerifier.create(infoCamereService.getLegalInstitutions(request))
                .expectNext(response)
                .verifyComplete();
    }
}

