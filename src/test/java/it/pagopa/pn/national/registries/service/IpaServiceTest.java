package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.config.ipa.IpaSecretConfig;
import it.pagopa.pn.national.registries.converter.IpaConverter;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.model.ipa.*;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class IpaServiceTest {
    @Mock
    private IpaClient ipaClient;

    @Mock
    private IpaConverter ipaConverter;

    @InjectMocks
    private IpaService ipaService;

    @Mock
    ValidateTaxIdUtils validateTaxIdUtils;

    @Mock
    IpaSecretConfig ipaSecretConfig;

    @Mock
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    PnAuditLogEventType type = PnAuditLogEventType.AUD_NR_PF_PHYSICAL;
    Map<String, String> mdc = new HashMap<>();
    String message = "message";
    Object[] arguments = new Object[] {"arg1", "arg2"};
    PnAuditLogEvent logEvent;

    @BeforeEach
    public void setup() {
        mdc.put("key", "value");
        logEvent = new PnAuditLogEvent(type, mdc, message, arguments);
    }

    @Test
    void testGetIpaPec() {
        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ResultDto resultDto = new ResultDto();
        resultDto.setNumItems(1);
        resultDto.setCodErr(0);
        resultDto.setDescErr("no error");
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        dataWS23Dto.setDesAmm("denominazione");
        dataWS23Dto.setTipo("type");
        dataWS23Dto.setDomicilioDigitale("domicilio digitale");
        dataWS23Dto.setCodAmm("cod ente");
        List<DataWS23Dto> list = new ArrayList<>();
        list.add(dataWS23Dto);
        ws23ResponseDto.setResult(resultDto);
        ws23ResponseDto.setData(list);
        when(ipaClient.callEServiceWS23(any(), any(), any())).thenReturn(Mono.just(ws23ResponseDto));
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("42");
        ipaRequestBodyDto.setFilter(filter);

        IPAPecDto ipaPecOKDto = new IPAPecDto();
        ipaPecOKDto.setDomicilioDigitale("domicilioDigitale");
        ipaPecOKDto.setTipo("tipo");
        ipaPecOKDto.setCodEnte("codEnte");
        ipaPecOKDto.setDenominazione("denominazione");
        IpaSecret ipaSecret = new IpaSecret();
        ipaSecret.setAuthId("authId");
        when(pnNationalRegistriesSecretService.getIpaSecret(any())).thenReturn(ipaSecret);
        when(ipaConverter.convertToIpaPecDtoFromWS23(any())).thenReturn(ipaPecOKDto);
        when(ipaSecretConfig.getIpaSecret()).thenReturn("ipaSecret");
        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto, logEvent)).expectNext(ipaPecOKDto).expectComplete().verify();
    }

    @Test
    void testGetIpaPec2() {
        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ResultDto resultDto = new ResultDto();
        resultDto.setNumItems(2);
        resultDto.setCodErr(0);
        resultDto.setDescErr("");
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        dataWS23Dto.setDesAmm("denominazione");
        dataWS23Dto.setTipo("type");
        dataWS23Dto.setDomicilioDigitale("domicilio digitale");
        dataWS23Dto.setCodAmm("codEnte");
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
        resultDto1.setCodErr(0);
        resultDto1.setDescErr("no error");
        resultDto1.setNumItems(1);
        ws05ResponseDto.setData(dataWS05Dto);
        ws05ResponseDto.setResult(resultDto1);

        when(ipaClient.callEServiceWS23(any(), any(), any())).thenReturn(Mono.just(ws23ResponseDto));
        when(ipaClient.callEServiceWS05(any(), any(), any())).thenReturn(Mono.just(ws05ResponseDto));
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("42");
        ipaRequestBodyDto.setFilter(filter);

        IPAPecDto ipaPecOKDto = new IPAPecDto();
        ipaPecOKDto.setDomicilioDigitale("domicilioDigitale");
        ipaPecOKDto.setTipo("tipo");
        ipaPecOKDto.setCodEnte("codEnte");
        ipaPecOKDto.setDenominazione("denominazione");
        IpaSecret ipaSecret = new IpaSecret();
        ipaSecret.setAuthId("authId");
        when(pnNationalRegistriesSecretService.getIpaSecret(any())).thenReturn(ipaSecret);
        when(ipaConverter.convertToIpaPecDtoFromWS23(any())).thenReturn(ipaPecOKDto);
        when(ipaConverter.convertToIPAPecDtoFromWS05(any())).thenReturn(ipaPecOKDto);
        when(ipaSecretConfig.getIpaSecret()).thenReturn("ipaSecret");

        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto, logEvent)).expectNext(ipaPecOKDto).expectComplete().verify();
    }


    @Test
    void testGetIpaPec3() {
        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ResultDto resultDto = new ResultDto();
        resultDto.setNumItems(0);
        resultDto.setCodErr(0);
        resultDto.setDescErr("");
        ws23ResponseDto.setResult(resultDto);



        when(ipaClient.callEServiceWS23(any(), any(), any())).thenReturn(Mono.just(ws23ResponseDto));
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("42");
        ipaRequestBodyDto.setFilter(filter);

        IPAPecDto ipaPecOKDto = new IPAPecDto();
        ipaPecOKDto.setDomicilioDigitale("domicilioDigitale");
        ipaPecOKDto.setTipo("tipo");
        ipaPecOKDto.setCodEnte("codEnte");
        ipaPecOKDto.setDenominazione("denominazione");

        IpaSecret ipaSecret = new IpaSecret();
        ipaSecret.setAuthId("authId");
        when(pnNationalRegistriesSecretService.getIpaSecret(any())).thenReturn(ipaSecret);
        when(ipaSecretConfig.getIpaSecret()).thenReturn("ipaSecret");
        when(ipaConverter.convertToIpaPecDtoFromWS23(any())).thenReturn(ipaPecOKDto);

        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto, logEvent)).expectNext(new IPAPecDto()).expectComplete().verify();
    }

    @Test
    void testGetIpaPec4() {
        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ResultDto resultDto = new ResultDto();
        resultDto.setNumItems(0);
        resultDto.setCodErr(1);
        resultDto.setDescErr("Error");
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        ws23ResponseDto.setResult(resultDto);
        List<DataWS23Dto> list = new ArrayList<>();
        list.add(dataWS23Dto);
        ws23ResponseDto.setData(list);
        when(ipaClient.callEServiceWS23(any(), any(), any())).thenReturn(Mono.just(ws23ResponseDto));
        when(ipaSecretConfig.getIpaSecret()).thenReturn("ipaSecret");
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("42");
        ipaRequestBodyDto.setFilter(filter);
        IpaSecret ipaSecret = new IpaSecret();
        ipaSecret.setAuthId("authId");
        when(pnNationalRegistriesSecretService.getIpaSecret(any())).thenReturn(ipaSecret);

        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto, logEvent)).expectError().verify();
    }

    @Test
    void testGetIpaPec5() {
        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ResultDto resultDto = new ResultDto();
        resultDto.setNumItems(2);
        resultDto.setCodErr(0);
        resultDto.setDescErr("");
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        dataWS23Dto.setDesAmm("denominazione");
        dataWS23Dto.setTipo("type");
        dataWS23Dto.setDomicilioDigitale("domicilio digitale");
        dataWS23Dto.setCodAmm("codEnte");
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
        resultDto1.setCodErr(0);
        resultDto1.setDescErr("no error");
        resultDto1.setNumItems(0);
        ws05ResponseDto.setData(dataWS05Dto);
        ws05ResponseDto.setResult(resultDto1);

        when(ipaClient.callEServiceWS23(any(), any(), any())).thenReturn(Mono.just(ws23ResponseDto));
        when(ipaClient.callEServiceWS05(any(), any(), any())).thenReturn(Mono.just(ws05ResponseDto));
        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("42");
        ipaRequestBodyDto.setFilter(filter);
        IpaSecret ipaSecret = new IpaSecret();
        ipaSecret.setAuthId("authId");
        when(pnNationalRegistriesSecretService.getIpaSecret(any())).thenReturn(ipaSecret);
        IPAPecDto ipaPecOKDto = new IPAPecDto();
        when(ipaSecretConfig.getIpaSecret()).thenReturn("ipaSecret");
        when(ipaConverter.convertToIpaPecDtoFromWS23(any())).thenReturn(ipaPecOKDto);
        when(ipaConverter.convertToIPAPecDtoFromWS05(any())).thenReturn(ipaPecOKDto);

        StepVerifier.create(ipaService.getIpaPec(ipaRequestBodyDto, logEvent)).expectNext(new IPAPecDto()).expectComplete().verify();
    }
}

