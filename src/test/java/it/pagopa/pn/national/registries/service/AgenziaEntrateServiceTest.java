package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import it.pagopa.pn.national.registries.model.agenziaentrate.TaxIdVerification;
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

import javax.xml.bind.JAXBException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {AgenziaEntrateService.class})
@ExtendWith(MockitoExtension.class)
class AgenziaEntrateServiceTest {
    @InjectMocks
    AgenziaEntrateService agenziaEntrateService;
    @Mock
    CheckCfClient checkCfClient;
    @Mock
    AdELegalClient adELegalClient;
    @Mock
    AgenziaEntrateConverter agenziaEntrateConverter;

    @Test
    void callEService() {
        CheckTaxIdRequestBodyDto requestBodyDto = new CheckTaxIdRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("test");
        requestBodyDto.setFilter(filter);

        TaxIdVerification taxIdVerification = new TaxIdVerification();
        taxIdVerification.setCodiceFiscale("test");
        taxIdVerification.setMessaggio("trovato");
        taxIdVerification.setValido(true);

        CheckTaxIdOKDto checkTaxIdOKDto = new CheckTaxIdOKDto();
        checkTaxIdOKDto.setIsValid(true);
        checkTaxIdOKDto.setTaxId("test");
        checkTaxIdOKDto.setErrorCode(CheckTaxIdOKDto.ErrorCodeEnum.ERR01);
        when(checkCfClient.callEService(any())).thenReturn(Mono.just(taxIdVerification));

        when(agenziaEntrateConverter.convertToCfStatusDto(any())).thenReturn(checkTaxIdOKDto);

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
        assertThrows(IllegalArgumentException.class, () -> agenziaEntrateService.callEService(checkTaxIdRequestBodyDto));
        verify(checkCfClient).callEService(org.mockito.Mockito.any());
    }

    @Test
    void checkTaxIdAndVatNumber() {
        ADELegalRequestBodyDto adeLegalRequestBodyDto = new ADELegalRequestBodyDto();
        ADELegalRequestBodyFilterDto adeLegalRequestBodyFilterDto = new ADELegalRequestBodyFilterDto();
        adeLegalRequestBodyFilterDto.setTaxId("testTaxId");
        adeLegalRequestBodyFilterDto.setVatNumber("testVatNumber");
        adeLegalRequestBodyDto.setFilter(adeLegalRequestBodyFilterDto);

        CheckValidityRappresentanteResp checkValidityRappresentanteRespType = new CheckValidityRappresentanteResp();
        checkValidityRappresentanteRespType.setCodiceRitorno("00");
        checkValidityRappresentanteRespType.setDettaglioEsito("XX00");
        checkValidityRappresentanteRespType.setValido(true);

        ADELegalOKDto adeLegalOKDto = new ADELegalOKDto();
        adeLegalOKDto.setResultCode(ADELegalOKDto.ResultCodeEnum.fromValue(checkValidityRappresentanteRespType.getCodiceRitorno()));
        adeLegalOKDto.setVerificationResult(checkValidityRappresentanteRespType.getValido());
        adeLegalOKDto.setResultDetail(ADELegalOKDto.ResultDetailEnum.fromValue(checkValidityRappresentanteRespType.getDettaglioEsito()));
        when(adELegalClient.checkTaxIdAndVatNumberAdE(any())).thenReturn(Mono.just("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:anag=\"http://anagrafica.verifica.rappresentante.ente\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<checkValidityRappresentanteResp>" +
                "<valido>true</valido>" +
                "<dettaglioEsito>XX00</dettaglioEsito>" +
                "<codiceRitorno>00</codiceRitorno>" +
                "</checkValidityRappresentanteResp>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>"));

        when(agenziaEntrateConverter.adELegalResponseToDto(any())).thenReturn(adeLegalOKDto);

        StepVerifier.create(agenziaEntrateService.checkTaxIdAndVatNumber(adeLegalRequestBodyDto)).expectNext(adeLegalOKDto).verifyComplete();
    }


    /**
     * Method under test: {@link AgenziaEntrateService#checkTaxIdAndVatNumber(ADELegalRequestBodyDto)}
     */
    @Test
    void testCheckTaxIdAndVatNumber3() {
        ADELegalRequestBodyDto adeLegalRequestBodyDto = new ADELegalRequestBodyDto();
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
    void testCheckTaxIdAndVatNumber4() {
        when(adELegalClient.checkTaxIdAndVatNumberAdE(org.mockito.Mockito.any()))
                .thenReturn((Mono<String>) mock(Mono.class));
        agenziaEntrateService.checkTaxIdAndVatNumber(new ADELegalRequestBodyDto());
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
    void unmarshallerSuccess() throws JAXBException {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:anag=\"http://anagrafica.verifica.rappresentante.ente\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<checkValidityRappresentanteResp>" +
                "<valido>true</valido>" +
                "<dettaglioEsito>XX00</dettaglioEsito>" +
                "<codiceRitorno>00</codiceRitorno>" +
                "</checkValidityRappresentanteResp>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        CheckValidityRappresentanteResp checkValidityRappresentanteResp = new CheckValidityRappresentanteResp();
        checkValidityRappresentanteResp.setValido(true);
        checkValidityRappresentanteResp.setDettaglioEsito("XX00");
        checkValidityRappresentanteResp.setCodiceRitorno("00");

        CheckValidityRappresentanteResp responseObj = agenziaEntrateService.unmarshaller(response);

        Assertions.assertEquals(responseObj, checkValidityRappresentanteResp);
    }
}
