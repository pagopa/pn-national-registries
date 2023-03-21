package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.converter.IpaConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.model.ipa.DataWS23Dto;
import it.pagopa.pn.national.registries.model.ipa.ResultDto;
import it.pagopa.pn.national.registries.model.ipa.WS23ResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class IpaServiceTest {
    @Mock
    private IpaClient ipaClient;

    @Mock
    private IpaConverter ipaConverter;

    @InjectMocks
    private IpaService ipaService;

    @Test
    void testGetIpaPec() {
        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ResultDto resultDto = new ResultDto();
        resultDto.setNumItems(1);
        resultDto.setCodError(0);
        resultDto.setDescError("no error");
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        dataWS23Dto.setDenominazione("denominazione");
        dataWS23Dto.setType("type");
        dataWS23Dto.setDomicilioDigitale("domicilio digitale");
        dataWS23Dto.setCodEnte("cod ente");
        ws23ResponseDto.setResult(resultDto);
        ws23ResponseDto.setData(dataWS23Dto);
        when(ipaClient.callEServiceWS23(any())).thenReturn(Mono.just(ws23ResponseDto));
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        ipaRequestBodyDto.taxId("42");

        IPAPecOKDto ipaPecOKDto = new IPAPecOKDto();
        ipaPecOKDto.setTipo("type");
        ipaPecOKDto.setDenominazione("denominazione");
        ipaPecOKDto.setCodEnte("cod ente");
        ipaPecOKDto.setDomicilioDigitale("domicilio digitale");

        when(ipaConverter.convertToIPAPecOKDto(any())).thenReturn(ipaPecOKDto);

        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto)).expectNext(ipaPecOKDto).expectComplete().verify();
    }


    @Test
    void testGetIpaPec2() {
        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ResultDto resultDto = new ResultDto();
        resultDto.setNumItems(0);
        resultDto.setCodError(1);
        resultDto.setDescError("Error");
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        ws23ResponseDto.setResult(resultDto);
        ws23ResponseDto.setData(dataWS23Dto);
        when(ipaClient.callEServiceWS23(any())).thenReturn(Mono.just(ws23ResponseDto));
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        ipaRequestBodyDto.taxId("42");

        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto)).expectError().verify();
    }
}

