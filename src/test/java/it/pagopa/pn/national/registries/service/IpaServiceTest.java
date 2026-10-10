package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.config.ipa.IpaSecretConfig;
import it.pagopa.pn.national.registries.converter.IpaConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.model.ipa.IpaSecret;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpaServiceTest {

    private static final String TAX_ID = "12345678901";
    private static final String AUTH_ID = "authId";

    @Mock
    private IpaClient ipaClient;

    @Mock
    private IpaConverter ipaConverter;

    @Mock
    ValidateTaxIdUtils validateTaxIdUtils;

    @Mock
    IpaSecretConfig ipaSecretConfig;

    @Mock
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @Test
    void getIpaPec_shouldReturnWs23ConversionWhenSingleItemIsReturned() {
        IpaService ipaService = buildService();
        WS23ResponseDto ws23ResponseDto = ws23Response(1, 0, "no error", List.of(ws23Data("cod-amm")));
        IPAPecDto expected = new IPAPecDto();
        expected.setDomicilioDigitale("ipa@pec.it");

        when(ipaClient.callEServiceWS23(TAX_ID, AUTH_ID)).thenReturn(Mono.just(ws23ResponseDto));
        when(ipaConverter.convertToIpaPecDtoFromWS23(ws23ResponseDto)).thenReturn(expected);

        StepVerifier.create(ipaService.getIpaPec(request(TAX_ID)))
                .expectNext(expected)
                .verifyComplete();

        verify(ipaClient).callEServiceWS23(TAX_ID, AUTH_ID);
        verify(ipaClient, never()).callEServiceWS05(any(), any());
    }

    @Test
    void getIpaPec_shouldQueryWs05WhenWs23ReturnsMultipleItems() {
        IpaService ipaService = buildService();
        WS23ResponseDto ws23ResponseDto = ws23Response(2, 0, "", List.of(ws23Data("cod-amm"), ws23Data("cod-amm-2")));
        WS05ResponseDto ws05ResponseDto = ws05Response(1, 0, "", "mail1@pec.it");
        IPAPecDto expected = new IPAPecDto();
        expected.setDomicilioDigitale("mail1@pec.it");

        when(ipaClient.callEServiceWS23(TAX_ID, AUTH_ID)).thenReturn(Mono.just(ws23ResponseDto));
        when(ipaClient.callEServiceWS05("cod-amm", AUTH_ID)).thenReturn(Mono.just(ws05ResponseDto));
        when(ipaConverter.convertToIPAPecDtoFromWS05(ws05ResponseDto)).thenReturn(expected);

        StepVerifier.create(ipaService.getIpaPec(request(TAX_ID)))
                .expectNext(expected)
                .verifyComplete();

        verify(ipaClient).callEServiceWS23(TAX_ID, AUTH_ID);
        verify(ipaClient).callEServiceWS05("cod-amm", AUTH_ID);
    }


    @Test
    void getIpaPec_shouldNormalizeWs23ZeroItemsToEmptyDto() {
        IpaService ipaService = buildService();
        WS23ResponseDto ws23ResponseDto = ws23Response(0, 0, "", List.of());

        when(ipaClient.callEServiceWS23(TAX_ID, AUTH_ID)).thenReturn(Mono.just(ws23ResponseDto));

        StepVerifier.create(ipaService.getIpaPec(request(TAX_ID)))
                .assertNext(response -> {
                    assertEquals(null, response.getDomicilioDigitale());
                    assertEquals(null, response.getCodEnte());
                })
                .verifyComplete();

        verify(ipaClient, never()).callEServiceWS05(any(), any());
    }

    @Test
    void getIpaPec_shouldNormalizeWs05ZeroItemsToEmptyDto() {
        IpaService ipaService = buildService();
        WS23ResponseDto ws23ResponseDto = ws23Response(2, 0, "", List.of(ws23Data("cod-amm"), ws23Data("cod-amm-2")));
        WS05ResponseDto ws05ResponseDto = ws05Response(0, 0, "", "mail1@pec.it");

        when(ipaClient.callEServiceWS23(TAX_ID, AUTH_ID)).thenReturn(Mono.just(ws23ResponseDto));
        when(ipaClient.callEServiceWS05("cod-amm", AUTH_ID)).thenReturn(Mono.just(ws05ResponseDto));

        StepVerifier.create(ipaService.getIpaPec(request(TAX_ID)))
                .assertNext(response -> assertEquals(null, response.getDomicilioDigitale()))
                .verifyComplete();
    }

    @Test
    void getIpaPec_shouldPropagateWsFunctionalError() {
        IpaService ipaService = buildService();
        WS23ResponseDto ws23ResponseDto = ws23Response(1, 1, "Error", List.of(ws23Data("cod-amm")));

        when(ipaClient.callEServiceWS23(TAX_ID, AUTH_ID)).thenReturn(Mono.just(ws23ResponseDto));

        StepVerifier.create(ipaService.getIpaPec(request(TAX_ID)))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(PnNationalRegistriesException.class, error);
                    PnNationalRegistriesException exception = (PnNationalRegistriesException) error;
                    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                    assertEquals("Error", exception.getMessage());
                })
                .verify();
    }

    @Test
    void getIpaPec_shouldPropagateTechnicalClientErrorWithoutNormalizingIt() {
        IpaService ipaService = buildService();
        RuntimeException technicalError = new RuntimeException("IPA unavailable");

        when(ipaClient.callEServiceWS23(TAX_ID, AUTH_ID)).thenReturn(Mono.error(technicalError));

        StepVerifier.create(ipaService.getIpaPec(request(TAX_ID)))
                .expectErrorMatches(error -> error == technicalError)
                .verify();
    }

    private IpaService buildService() {
        IpaSecret ipaSecret = new IpaSecret();
        ipaSecret.setAuthId(AUTH_ID);
        when(ipaSecretConfig.getIpaSecret()).thenReturn("ipaSecret");
        when(pnNationalRegistriesSecretService.getIpaSecret("ipaSecret")).thenReturn(ipaSecret);
        return new IpaService(ipaConverter, ipaClient, validateTaxIdUtils, pnNationalRegistriesSecretService, ipaSecretConfig);
    }

    private IPARequestBodyDto request(String taxId) {
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId(taxId);
        ipaRequestBodyDto.setFilter(filter);
        return ipaRequestBodyDto;
    }

    private WS23ResponseDto ws23Response(int numItems, int codErr, String descErr, List<DataWS23Dto> data) {
        WS23ResponseDto response = new WS23ResponseDto();
        ResultDto result = new ResultDto();
        result.setNumItems(numItems);
        result.setCodErr(codErr);
        result.setDescErr(descErr);
        response.setResult(result);
        response.setData(data);
        return response;
    }

    private WS05ResponseDto ws05Response(int numItems, int codErr, String descErr, String email) {
        WS05ResponseDto response = new WS05ResponseDto();
        ResultDto result = new ResultDto();
        result.setNumItems(numItems);
        result.setCodErr(codErr);
        result.setDescErr(descErr);
        response.setResult(result);
        DataWS05Dto data = new DataWS05Dto();
        data.setMail1(email);
        response.setData(data);
        return response;
    }

    private DataWS23Dto ws23Data(String codAmm) {
        DataWS23Dto data = new DataWS23Dto();
        data.setCodAmm(codAmm);
        data.setDomicilioDigitale("ipa@pec.it");
        data.setDesAmm("denominazione");
        data.setTipo("PA");
        return data;
    }
}

