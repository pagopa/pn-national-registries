package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
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

import java.util.ArrayList;


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

        StepVerifier.create(infoCamereController.digitalAddressIniPEC(Mono.just(requestBodyDto), "clientId", serverWebExchange))
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

        StepVerifier.create(infoCamereController.addressRegistroImprese(Mono.just(body), serverWebExchange))
                .expectNext(ResponseEntity.ok().body(response));
    }

    @Test
    void getLegalInstitutions() {

        InfoCamereLegalInstitutionsOKDto response = new InfoCamereLegalInstitutionsOKDto();
        response.setBusinessList(new ArrayList<>());
        response.setLegalTaxId("PPPPLT80A01H501V");

        InfoCamereLegalInstitutionsRequestBodyDto body = new InfoCamereLegalInstitutionsRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto dto = new CheckTaxIdRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        body.setFilter(dto);

        StepVerifier.create(infoCamereController.infoCamereLegalInstitutions(Mono.just(body), serverWebExchange))
                .expectNext(ResponseEntity.ok().body(response));
    }
}
