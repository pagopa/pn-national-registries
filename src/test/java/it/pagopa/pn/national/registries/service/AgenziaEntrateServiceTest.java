package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.exceptions.RuntimeJAXBException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.dto.Richiesta;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.dto.VerificaCodiceFiscale;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_NAME_AGENZIA_ENTRATE_CHECK_TAX_ID;
import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_NAME_AGENZIA_ENTRATE_LEGAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {AgenziaEntrateService.class})
@ExtendWith(MockitoExtension.class)
class AgenziaEntrateServiceTest {
    @InjectMocks
    AgenziaEntrateService agenziaEntrateService;
    @Mock
    AdELegalClient adELegalClient;
    @Mock
    AgenziaEntrateConverter agenziaEntrateConverter;
    @Mock
    ValidateTaxIdUtils validateTaxIdUtils;
    @Mock
    CheckCfClient checkCfClient;
    @Mock
    ValidateUtils validateUtils;

    @Test
    void callEService() {
        CheckTaxIdRequestBodyDto requestBodyDto = new CheckTaxIdRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("test");
        requestBodyDto.setFilter(filter);

        VerificaCodiceFiscale taxIdVerification = new VerificaCodiceFiscale();
        taxIdVerification.setCodiceFiscale("test");
        taxIdVerification.setMessaggio("trovato");
        taxIdVerification.setValido(true);

        CheckTaxIdOKDto checkTaxIdOKDto = new CheckTaxIdOKDto();
        checkTaxIdOKDto.setIsValid(true);
        checkTaxIdOKDto.setTaxId("test");
        when(validateUtils.taxIdIsInWhiteList(any())).thenReturn(false);
        when(checkCfClient.callEService(any())).thenReturn(Mono.just(taxIdVerification));
        when(agenziaEntrateConverter.convertToCfStatusDto(any())).thenReturn(checkTaxIdOKDto);

        StepVerifier.create(agenziaEntrateService.callEService(requestBodyDto)).expectNext(checkTaxIdOKDto).verifyComplete();

        ArgumentCaptor<Richiesta> requestCaptor = ArgumentCaptor.forClass(Richiesta.class);
        verify(checkCfClient).callEService(requestCaptor.capture());
        assertEquals("test", requestCaptor.getValue().getCodiceFiscale());
        verify(validateTaxIdUtils).validateTaxId("test", PROCESS_NAME_AGENZIA_ENTRATE_CHECK_TAX_ID, true);
        verify(agenziaEntrateConverter).convertToCfStatusDto(taxIdVerification);
    }

    @Test
    void callEService2() {
        CheckTaxIdRequestBodyDto requestBodyDto = new CheckTaxIdRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("test");
        requestBodyDto.setFilter(filter);

        CheckTaxIdOKDto checkTaxIdOKDto = new CheckTaxIdOKDto();
        checkTaxIdOKDto.setIsValid(true);
        checkTaxIdOKDto.setTaxId("test");
        when(validateUtils.taxIdIsInWhiteList(any())).thenReturn(true);

        StepVerifier.create(agenziaEntrateService.callEService(requestBodyDto))
                .assertNext(result -> {
                    assertEquals("test", result.getTaxId());
                    assertEquals(true, result.getIsValid());
                })
                .verifyComplete();

        verifyNoInteractions(validateTaxIdUtils, checkCfClient, agenziaEntrateConverter);
    }

    /**
     * Method under test: {@link AgenziaEntrateService#callEService(CheckTaxIdRequestBodyDto)}
     */
    @Test
    void testCallEService3() {
        when(checkCfClient.callEService(Mockito.any())).thenThrow(new IllegalArgumentException());

        CheckTaxIdRequestBodyFilterDto checkTaxIdRequestBodyFilterDto = new CheckTaxIdRequestBodyFilterDto();
        checkTaxIdRequestBodyFilterDto.taxId("42");

        CheckTaxIdRequestBodyDto checkTaxIdRequestBodyDto = new CheckTaxIdRequestBodyDto();
        checkTaxIdRequestBodyDto.filter(checkTaxIdRequestBodyFilterDto);
        when(validateUtils.taxIdIsInWhiteList(any())).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> agenziaEntrateService.callEService(checkTaxIdRequestBodyDto));
        verify(checkCfClient).callEService(org.mockito.Mockito.any());
    }

    @Test
    void callEServiceShouldPropagateReactiveClientError() {
        CheckTaxIdRequestBodyDto requestBodyDto = new CheckTaxIdRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("test");
        requestBodyDto.setFilter(filter);
        RuntimeException error = new RuntimeException("service unavailable");

        when(validateUtils.taxIdIsInWhiteList("test")).thenReturn(false);
        when(checkCfClient.callEService(any())).thenReturn(Mono.error(error));

        StepVerifier.create(agenziaEntrateService.callEService(requestBodyDto))
                .expectErrorMatches(throwable -> throwable == error)
                .verify();

        verify(validateTaxIdUtils).validateTaxId("test", PROCESS_NAME_AGENZIA_ENTRATE_CHECK_TAX_ID, true);
        verify(agenziaEntrateConverter, never()).convertToCfStatusDto(any());
    }

    /**
     * Method under test: {@link AgenziaEntrateService#checkTaxIdAndVatNumber(ADELegalRequestBodyDto)}
     */
    @Test
    void testCheckTaxIdAndVatNumber3() {
        ADELegalRequestBodyFilterDto adeLegalRequestBodyFilterDto = new ADELegalRequestBodyFilterDto();
        adeLegalRequestBodyFilterDto.setTaxId("42");
        ADELegalRequestBodyDto adeLegalRequestBodyDto = new ADELegalRequestBodyDto();
        adeLegalRequestBodyDto.setFilter(adeLegalRequestBodyFilterDto);
        when(adELegalClient.checkTaxIdAndVatNumberAdE(org.mockito.Mockito.any()))
                .thenThrow(new IllegalArgumentException());
        assertThrows(IllegalArgumentException.class,
                () -> agenziaEntrateService.checkTaxIdAndVatNumber(adeLegalRequestBodyDto));
        verify(adELegalClient).checkTaxIdAndVatNumberAdE(Mockito.any());
    }

    /**
     * Method under test: {@link AgenziaEntrateService#checkTaxIdAndVatNumber(ADELegalRequestBodyDto)}
     */
    @Test
    void testCheckTaxIdAndVatNumber5() {
        when(adELegalClient.checkTaxIdAndVatNumberAdE(Mockito.any()))
                .thenReturn((Mono<String>) mock(Mono.class));

        ADELegalRequestBodyDto adeLegalRequestBodyDto = new ADELegalRequestBodyDto();
        adeLegalRequestBodyDto.filter(new ADELegalRequestBodyFilterDto());
        agenziaEntrateService.checkTaxIdAndVatNumber(adeLegalRequestBodyDto);
        verify(adELegalClient).checkTaxIdAndVatNumberAdE(Mockito.any());
    }

    @Test
    void checkTaxIdAndVatNumberShouldConvertValidSoapResponse() {
        ADELegalRequestBodyFilterDto filter = new ADELegalRequestBodyFilterDto();
        filter.setTaxId("RSSMRA80A01H501U");
        filter.setVatNumber("00123456789");
        ADELegalRequestBodyDto request = new ADELegalRequestBodyDto();
        request.setFilter(filter);
        ADELegalOKDto expected = new ADELegalOKDto();

        when(adELegalClient.checkTaxIdAndVatNumberAdE(filter)).thenReturn(Mono.just(validSoapResponse()));
        when(agenziaEntrateConverter.adELegalResponseToDto(any())).thenReturn(expected);

        StepVerifier.create(agenziaEntrateService.checkTaxIdAndVatNumber(request))
                .expectNext(expected)
                .verifyComplete();

        verify(validateTaxIdUtils).validateTaxId("RSSMRA80A01H501U", PROCESS_NAME_AGENZIA_ENTRATE_LEGAL, false);
        verify(validateTaxIdUtils).validateTaxId("00123456789", PROCESS_NAME_AGENZIA_ENTRATE_LEGAL, false);
        verify(adELegalClient).checkTaxIdAndVatNumberAdE(filter);
        verify(agenziaEntrateConverter).adELegalResponseToDto(any());
    }

    @Test
    void checkTaxIdAndVatNumberShouldWrapInvalidSoapResponse() {
        ADELegalRequestBodyFilterDto filter = new ADELegalRequestBodyFilterDto();
        filter.setTaxId("RSSMRA80A01H501U");
        filter.setVatNumber("00123456789");
        ADELegalRequestBodyDto request = new ADELegalRequestBodyDto();
        request.setFilter(filter);

        when(adELegalClient.checkTaxIdAndVatNumberAdE(filter)).thenReturn(Mono.just("<invalid/>"));

        StepVerifier.create(agenziaEntrateService.checkTaxIdAndVatNumber(request))
                .expectError(RuntimeJAXBException.class)
                .verify();

        verify(agenziaEntrateConverter, never()).adELegalResponseToDto(any());
    }

    @Test
    void checkTaxIdAndVatNumberShouldPropagateReactiveClientError() {
        ADELegalRequestBodyFilterDto filter = new ADELegalRequestBodyFilterDto();
        filter.setTaxId("RSSMRA80A01H501U");
        filter.setVatNumber("00123456789");
        ADELegalRequestBodyDto request = new ADELegalRequestBodyDto();
        request.setFilter(filter);
        RuntimeException error = new RuntimeException("service unavailable");

        when(adELegalClient.checkTaxIdAndVatNumberAdE(filter)).thenReturn(Mono.error(error));

        StepVerifier.create(agenziaEntrateService.checkTaxIdAndVatNumber(request))
                .expectErrorMatches(throwable -> throwable == error)
                .verify();

        verify(agenziaEntrateConverter, never()).adELegalResponseToDto(any());
    }

    @Test
    void unmarshallerSuccess(){
        Assertions.assertDoesNotThrow(() -> agenziaEntrateService.unmarshaller(validSoapResponse()));
    }

    @Test
    void unmarshallerShouldFailWhenSoapBodyIsMissing() {
        assertThrows(jakarta.xml.bind.JAXBException.class, () -> agenziaEntrateService.unmarshaller("<invalid/>"));
    }

    private String validSoapResponse() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:anag=\"http://anagrafica.verifica.rappresentante.ente\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<checkValidityRappresentanteResp xmlns=\"http://anagrafica.verifica.rappresentante.ente\">" +
                "<valido>true</valido>" +
                "<dettaglioEsito>XX00</dettaglioEsito>" +
                "<codiceRitorno>00</codiceRitorno>" +
                "</checkValidityRappresentanteResp>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";
    }
}