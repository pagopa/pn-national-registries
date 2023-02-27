package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.service.InfoCamereService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class InfoCamereControllerTest {

    @InjectMocks
    InfoCamereController infoCamereController;

    @Mock
    InfoCamereService infoCamereService;

    @Mock
    ServerWebExchange serverWebExchange;

    @Mock
    ValidateTaxIdUtils validateTaxIdUtils;

    @Test
    void getDigitalAddressINAD() {
        GetDigitalAddressIniPECRequestBodyDto requestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto dto = new GetDigitalAddressIniPECRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        requestBodyDto.setFilter(dto);

        GetDigitalAddressIniPECOKDto getDigitalAddressINADOKDto = new GetDigitalAddressIniPECOKDto();
        getDigitalAddressINADOKDto.setCorrelationId("correlationId");

        when(infoCamereService.getIniPecDigitalAddress("clientId",requestBodyDto)).thenReturn(Mono.just(getDigitalAddressINADOKDto));

        StepVerifier.create(infoCamereController.digitalAddressIniPEC(requestBodyDto,"clientId",serverWebExchange))
                .expectNext(ResponseEntity.ok().body(getDigitalAddressINADOKDto));
    }

    @Test
    void addressRegistroImprese() {

        GetAddressRegistroImpreseOKDto response = new GetAddressRegistroImpreseOKDto();
        response.setTaxId("PPPPLT80A01H501V");

        GetAddressRegistroImpreseRequestBodyDto body = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto dto = new GetAddressRegistroImpreseRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        body.setFilter(dto);
        when(infoCamereService.getRegistroImpreseLegalAddress(body)).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereController.addressRegistroImprese(body,serverWebExchange))
                .expectNext(ResponseEntity.ok().body(response));
    }

    @Test
    void checkTaxIdAndVatNumber() {

        InfoCamereLegalOKDto response = new InfoCamereLegalOKDto();
        response.setTaxId("PPPPLT80A01H501V");
        response.setVatNumber("vatNumber");

        InfoCamereLegalRequestBodyDto body = new InfoCamereLegalRequestBodyDto();
        InfoCamereLegalRequestBodyFilterDto dto = new InfoCamereLegalRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        dto.setVatNumber("vatNumber");
        body.setFilter(dto);
        when(infoCamereService.checkTaxIdAndVatNumber(body)).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereController.infoCamereLegal(body,serverWebExchange))
                .expectNext(ResponseEntity.ok().body(response));
    }
}
