package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.checkcf.CheckCfClient;
import it.pagopa.pn.national.registries.converter.CheckCfConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.checkcf.VerificaCodiceFiscale;
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
class CheckCfServiceTest {
    @InjectMocks
    CheckCfService checkCfService;
    @Mock
    CheckCfClient checkCfClient;
    @Mock
    CheckCfConverter checkCfConverter;

    @Test
    void callEService() {
        CheckTaxIdRequestBodyDto requestBodyDto = new CheckTaxIdRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("test");
        requestBodyDto.setFilter(filter);

        VerificaCodiceFiscale verificaCodiceFiscale = new VerificaCodiceFiscale();
        verificaCodiceFiscale.setCodiceFiscale("test");
        verificaCodiceFiscale.setMessaggio("trovato");
        verificaCodiceFiscale.setValido(true);

        CheckTaxIdOKDto checkTaxIdOKDto = new CheckTaxIdOKDto();
        checkTaxIdOKDto.setIsValid(true);
        checkTaxIdOKDto.setTaxId("test");
        checkTaxIdOKDto.setErrorCode(CheckTaxIdOKDto.ErrorCodeEnum.ERR01);
        when(checkCfClient.callEService(any())).thenReturn(Mono.just(verificaCodiceFiscale));

        when(checkCfConverter.convertToCfStatusDto(any())).thenReturn(checkTaxIdOKDto);

        StepVerifier.create(checkCfService.callEService(requestBodyDto)).expectNext(checkTaxIdOKDto).verifyComplete();

    }
}