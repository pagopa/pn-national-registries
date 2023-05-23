package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.converter.IpaConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.model.ipa.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

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
        List<DataWS23Dto> list = new ArrayList<>();
        list.add(dataWS23Dto);
        ws23ResponseDto.setResult(resultDto);
        ws23ResponseDto.setData(list);
        when(ipaClient.callEServiceWS23(any())).thenReturn(Mono.just(ws23ResponseDto));
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("42");
        ipaRequestBodyDto.setFilter(filter);

        IPAPecDto ipaPecOKDto = new IPAPecDto();
        ipaPecOKDto.setDomicilioDigitale("domicilioDigitale");
        ipaPecOKDto.setTipo("tipo");
        ipaPecOKDto.setCodEnte("codEnte");
        ipaPecOKDto.setDenominazione("denominazione");

        when(ipaConverter.convertToIpaPecDtoFromWS23(any())).thenReturn(ipaPecOKDto);

        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto)).expectNext(ipaPecOKDto).expectComplete().verify();
    }

    @Test
    void testGetIpaPec2() {
        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ResultDto resultDto = new ResultDto();
        resultDto.setNumItems(2);
        resultDto.setCodError(0);
        resultDto.setDescError("");
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        dataWS23Dto.setDenominazione("denominazione");
        dataWS23Dto.setType("type");
        dataWS23Dto.setDomicilioDigitale("domicilio digitale");
        dataWS23Dto.setCodEnte("codEnte");
        List<DataWS23Dto> list = new ArrayList<>();
        list.add(dataWS23Dto);
        list.add(dataWS23Dto);

        ws23ResponseDto.setResult(resultDto);
        ws23ResponseDto.setData(list);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("acronimo");
        dataWS05Dto.setCf("codiceFiscale");
        dataWS05Dto.setCap("cap");
        dataWS05Dto.setCategoria("categoria");
        dataWS05Dto.setDataAccreditamento("dataAccreditamento");
        dataWS05Dto.setComune("comune");
        dataWS05Dto.setCodAmm("codiceAmministrazione");
        dataWS05Dto.setIndirizzo("indirizzo");
        dataWS05Dto.setIndirizzo("indirizzo");
        dataWS05Dto.setProvincia("provincia");
        dataWS05Dto.setLivAccessibilita("livelloAccessibilita");
        dataWS05Dto.setMail1("mail1");
        dataWS05Dto.setMail2("mail2");
        dataWS05Dto.setMail3("mail3");
        dataWS05Dto.setMail4("mail4");
        dataWS05Dto.setMail5("mail5");
        dataWS05Dto.setCognResp("cognomeResponsabile");
        dataWS05Dto.setSitoIstituzionale("sitoIstituzionale");
        dataWS05Dto.setTitoloResp("titoloResponsabile");
        dataWS05Dto.setTitoloResp("titoloResponsabile");
        dataWS05Dto.setRegione("regione");
        dataWS05Dto.setDesAmm("descrizioneAmministrazione");

        ResultDto resultDto1 = new ResultDto();
        resultDto1.setCodError(0);
        resultDto1.setDescError("no error");
        resultDto1.setNumItems(1);
        ws05ResponseDto.setData(dataWS05Dto);
        ws05ResponseDto.setResult(resultDto1);

        when(ipaClient.callEServiceWS23(any())).thenReturn(Mono.just(ws23ResponseDto));
        when(ipaClient.callEServiceWS05(any())).thenReturn(Mono.just(ws05ResponseDto));
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("42");
        ipaRequestBodyDto.setFilter(filter);

        IPAPecDto ipaPecOKDto = new IPAPecDto();
        ipaPecOKDto.setDomicilioDigitale("domicilioDigitale");
        ipaPecOKDto.setTipo("tipo");
        ipaPecOKDto.setCodEnte("codEnte");
        ipaPecOKDto.setDenominazione("denominazione");

        when(ipaConverter.convertToIpaPecDtoFromWS23(any())).thenReturn(ipaPecOKDto);
        when(ipaConverter.convertToIPAPecDtoFromWS05(any())).thenReturn(ipaPecOKDto);

        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto)).expectNext(ipaPecOKDto).expectComplete().verify();
    }


    @Test
    void testGetIpaPec3() {
        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ResultDto resultDto = new ResultDto();
        resultDto.setNumItems(0);
        resultDto.setCodError(0);
        resultDto.setDescError("");
        ws23ResponseDto.setResult(resultDto);



        when(ipaClient.callEServiceWS23(any())).thenReturn(Mono.just(ws23ResponseDto));
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("42");
        ipaRequestBodyDto.setFilter(filter);

        IPAPecDto ipaPecOKDto = new IPAPecDto();
        ipaPecOKDto.setDomicilioDigitale("domicilioDigitale");
        ipaPecOKDto.setTipo("tipo");
        ipaPecOKDto.setCodEnte("codEnte");
        ipaPecOKDto.setDenominazione("denominazione");

        when(ipaConverter.convertToIpaPecDtoFromWS23(any())).thenReturn(ipaPecOKDto);

        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto)).expectNext(new IPAPecDto()).expectComplete().verify();
    }

    @Test
    void testGetIpaPec4() {
        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ResultDto resultDto = new ResultDto();
        resultDto.setNumItems(0);
        resultDto.setCodError(1);
        resultDto.setDescError("Error");
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        ws23ResponseDto.setResult(resultDto);
        List<DataWS23Dto> list = new ArrayList<>();
        list.add(dataWS23Dto);
        ws23ResponseDto.setData(list);
        when(ipaClient.callEServiceWS23(any())).thenReturn(Mono.just(ws23ResponseDto));
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("42");
        ipaRequestBodyDto.setFilter(filter);

        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto)).expectError().verify();
    }

    @Test
    void testGetIpaPec5() {
        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ResultDto resultDto = new ResultDto();
        resultDto.setNumItems(2);
        resultDto.setCodError(0);
        resultDto.setDescError("");
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        dataWS23Dto.setDenominazione("denominazione");
        dataWS23Dto.setType("type");
        dataWS23Dto.setDomicilioDigitale("domicilio digitale");
        dataWS23Dto.setCodEnte("codEnte");
        List<DataWS23Dto> list = new ArrayList<>();
        list.add(dataWS23Dto);
        list.add(dataWS23Dto);

        ws23ResponseDto.setResult(resultDto);
        ws23ResponseDto.setData(list);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("acronimo");
        dataWS05Dto.setCf("codiceFiscale");
        dataWS05Dto.setCap("cap");
        dataWS05Dto.setCategoria("categoria");
        dataWS05Dto.setDataAccreditamento("dataAccreditamento");
        dataWS05Dto.setComune("comune");
        dataWS05Dto.setCodAmm("codiceAmministrazione");
        dataWS05Dto.setIndirizzo("indirizzo");
        dataWS05Dto.setIndirizzo("indirizzo");
        dataWS05Dto.setProvincia("provincia");
        dataWS05Dto.setLivAccessibilita("livelloAccessibilita");
        dataWS05Dto.setMail1("mail1");
        dataWS05Dto.setMail2("mail2");
        dataWS05Dto.setMail3("mail3");
        dataWS05Dto.setMail4("mail4");
        dataWS05Dto.setMail5("mail5");
        dataWS05Dto.setCognResp("cognomeResponsabile");
        dataWS05Dto.setSitoIstituzionale("sitoIstituzionale");
        dataWS05Dto.setTitoloResp("titoloResponsabile");
        dataWS05Dto.setTitoloResp("titoloResponsabile");
        dataWS05Dto.setRegione("regione");
        dataWS05Dto.setDesAmm("descrizioneAmministrazione");

        ResultDto resultDto1 = new ResultDto();
        resultDto1.setCodError(0);
        resultDto1.setDescError("no error");
        resultDto1.setNumItems(0);
        ws05ResponseDto.setData(dataWS05Dto);
        ws05ResponseDto.setResult(resultDto1);

        when(ipaClient.callEServiceWS23(any())).thenReturn(Mono.just(ws23ResponseDto));
        when(ipaClient.callEServiceWS05(any())).thenReturn(Mono.just(ws05ResponseDto));
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("42");
        ipaRequestBodyDto.setFilter(filter);

        IPAPecDto ipaPecOKDto = new IPAPecDto();

        when(ipaConverter.convertToIpaPecDtoFromWS23(any())).thenReturn(ipaPecOKDto);
        when(ipaConverter.convertToIPAPecDtoFromWS05(any())).thenReturn(ipaPecOKDto);

        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto)).expectNext(new IPAPecDto()).expectComplete().verify();
    }
}

