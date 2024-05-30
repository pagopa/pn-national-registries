package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.TaxIdVerification;
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
import static org.mockito.ArgumentMatchers.any;
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
    ValidateUtils validateUtils;


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
