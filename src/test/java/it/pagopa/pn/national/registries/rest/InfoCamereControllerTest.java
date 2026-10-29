package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.service.InfoCamereService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class InfoCamereControllerTest {
    private ServerWebExchange serverWebExchange;
    private HttpHeaders httpHeaders;
    private static final String AWS_XRAY_TRACE_ID = "AWS-XRAY-TRACE-ID";

    @InjectMocks
    InfoCamereController infoCamereController;

    @Mock
    InfoCamereService infoCamereService;

    @BeforeEach
    void init() {
        serverWebExchange = mock(ServerWebExchange.class);
        ServerHttpResponse serverHttpResponse = mock(ServerHttpResponse.class);
        httpHeaders = new HttpHeaders();

        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.getHeaders()).thenReturn(httpHeaders);
        infoCamereController = new InfoCamereController(infoCamereService);
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void getDigitalAddressINAD() {
        String correlationId = "correlationId";
        GetDigitalAddressIniPECRequestBodyDto requestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto dto = new GetDigitalAddressIniPECRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        dto.setCorrelationId(correlationId);
        requestBodyDto.setFilter(dto);

        GetDigitalAddressIniPECOKDto getDigitalAddressINADOKDto = new GetDigitalAddressIniPECOKDto();
        getDigitalAddressINADOKDto.setCorrelationId(correlationId);

        when(infoCamereService.getIniPecDigitalAddress(anyString(), eq(requestBodyDto), any(Date.class), any()))
                .thenReturn(Mono.fromSupplier(() -> {
                    assertEquals(correlationId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
                    return getDigitalAddressINADOKDto;
                }));

        StepVerifier.create(infoCamereController.digitalAddressIniPEC(Mono.just(requestBodyDto), "cxId", serverWebExchange))
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(getDigitalAddressINADOKDto, response.getBody());
                    assertEquals(correlationId, httpHeaders.getFirst("X-Request-ID"));
                })
                .verifyComplete();
    }

    @Test
    void addressRegistroImprese() {

        GetAddressRegistroImpreseOKDto response = new GetAddressRegistroImpreseOKDto();
        response.setTaxId("PPPPLT80A01H501V");

        GetAddressRegistroImpreseRequestBodyDto body = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto dto = new GetAddressRegistroImpreseRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        body.setFilter(dto);

        when(infoCamereService.getRegistroImpreseLegalAddress(body))
                .thenReturn(Mono.fromSupplier(() -> response));

        StepVerifier.create(infoCamereController.addressRegistroImprese(Mono.just(body), serverWebExchange))
                .assertNext(res -> {
                    assertEquals(200, res.getStatusCode().value());
                    assertEquals(response, res.getBody());
                })
                .verifyComplete();
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

        when(infoCamereService.checkTaxIdAndVatNumber(body))
                .thenReturn(Mono.fromSupplier(() -> response));

        StepVerifier.create(infoCamereController.infoCamereLegal(Mono.just(body), serverWebExchange))
                .assertNext(res -> {
                    assertEquals(200, res.getStatusCode().value());
                    assertEquals(response, res.getBody());
                })
                .verifyComplete();
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

        when(infoCamereService.getLegalInstitutions(body))
                .thenReturn(Mono.fromSupplier(() -> response));


        StepVerifier.create(infoCamereController.infoCamereLegalInstitutions(Mono.just(body), serverWebExchange))
                .assertNext(res -> {
                    assertEquals(200, res.getStatusCode().value());
                    assertEquals(response, res.getBody());
                })
                .verifyComplete();
    }
}
