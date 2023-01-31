package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.service.InfoCamereService;
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

    @Test
    void getDigitalAddressINAD() {
        GetDigitalAddressIniPECRequestBodyDto requestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto dto = new GetDigitalAddressIniPECRequestBodyFilterDto();
        dto.setTaxId("DDDFGF52F52H501S");
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
        response.setTaxId("cf");

        GetAddressRegistroImpreseRequestBodyDto body = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto dto = new GetAddressRegistroImpreseRequestBodyFilterDto();
        dto.setTaxId("cf");
        body.setFilter(dto);
        when(infoCamereService.getRegistroImpreseLegalAddress(body)).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereController.addressRegistroImprese(body,serverWebExchange))
                .expectNext(ResponseEntity.ok().body(response));
    }

    @Test
    void checkTaxIdAndVatNumber() {

        InfoCamereLegalOKDto response = new InfoCamereLegalOKDto();
        response.setTaxId("taxId");
        response.setVatNumber("vatNumber");

        InfoCamereLegalRequestBodyDto body = new InfoCamereLegalRequestBodyDto();
        InfoCamereLegalRequestBodyFilterDto dto = new InfoCamereLegalRequestBodyFilterDto();
        dto.setTaxId("taxId");
        dto.setVatNumber("vatNumber");
        body.setFilter(dto);
        when(infoCamereService.checkTaxIdAndVatNumber(body)).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereController.infoCamereLegal(body,serverWebExchange))
                .expectNext(ResponseEntity.ok().body(response));
    }
}
