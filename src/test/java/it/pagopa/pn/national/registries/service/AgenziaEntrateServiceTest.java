package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.dto.VerificaCodiceFiscale;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

        StepVerifier.create(agenziaEntrateService.callEService(requestBodyDto)).expectNext(checkTaxIdOKDto).verifyComplete();

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
    void unmarshallerSuccess(){
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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


        Assertions.assertDoesNotThrow(() -> agenziaEntrateService.unmarshaller(response));
    }
}
