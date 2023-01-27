package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.model.agenziaentrate.TaxIdVerification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
  /*  @Test
    void checkTaxIdAndVatNumber() {
        ADELegalRequestBodyDto adeLegalRequestBodyDto = new ADELegalRequestBodyDto();
        ADELegalRequestBodyFilterDto adeLegalRequestBodyFilterDto = new ADELegalRequestBodyFilterDto();
        adeLegalRequestBodyFilterDto.setTaxId("testTaxId");
        adeLegalRequestBodyFilterDto.setVatNumber("testVatNumber");
        adeLegalRequestBodyDto.setFilter(adeLegalRequestBodyFilterDto);

        CheckValidityRappresentanteRespType checkValidityRappresentanteRespType = new CheckValidityRappresentanteRespType();
        checkValidityRappresentanteRespType.setCodiceRitorno("00");
        checkValidityRappresentanteRespType.setDettaglioEsito("XX00");
        checkValidityRappresentanteRespType.setValido(true);

        //ResponseEnvelope responseEnvelope = new ResponseEnvelope(new ResponseBody(checkValidityRappresentanteRespType));

     //   String response = agenziaEntrateService.marshaller(responseEnvelope);

        ADELegalOKDto adeLegalOKDto = new ADELegalOKDto();
        adeLegalOKDto.setResultCode(ADELegalOKDto.ResultCodeEnum.fromValue(checkValidityRappresentanteRespType.getCodiceRitorno()));
        adeLegalOKDto.setVerificationResult(checkValidityRappresentanteRespType.isValido());
        adeLegalOKDto.setResultDetail(ADELegalOKDto.ResultDetailEnum.fromValue(checkValidityRappresentanteRespType.getDettaglioEsito()));
        when(adELegalClient.checkTaxIdAndVatNumberAdE(any())).thenReturn(Mono.just(response));

        when(agenziaEntrateConverter.adELegalResponseToDto(any(CheckValidityRappresentanteRespType.class))).thenReturn(adeLegalOKDto);

        StepVerifier.create(agenziaEntrateService.checkTaxIdAndVatNumber(adeLegalRequestBodyDto)).expectNext(adeLegalOKDto).verifyComplete();

    } */

}
