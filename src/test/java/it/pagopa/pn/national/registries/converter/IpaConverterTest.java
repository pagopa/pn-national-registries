package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {IpaConverter.class})
@ExtendWith(SpringExtension.class)
class IpaConverterTest {
    @InjectMocks
    private IpaConverter ipaConverter;


    /**
     * Method under test: {@link IpaConverter#convertToIPAPecDtoFromWS05(WS05ResponseDto)}
     */
    @Test
    void testConvertToIPAPecDtoFromWS05() {
        DataWS05Dto data = new DataWS05Dto();
        data.setAcronimo("Acronimo");
        data.setCap("Cap");
        data.setCategoria("Categoria");
        data.setCf("Cf");
        data.setCodAmm("Cod Amm");
        data.setCognResp("Cogn Resp");
        data.setComune("Comune");
        data.setDataAccreditamento("alice.liddell@example.org");
        data.setDesAmm("Des Amm");
        data.setIndirizzo("Indirizzo");
        data.setLivAccessibilita("Liv Accessibilita");
        data.setMail1("Mail1");
        data.setMail2("Mail2");
        data.setMail3("Mail3");
        data.setMail4("Mail4");
        data.setMail5("Mail5");
        data.setNomeResp("Nome Resp");
        data.setProvincia("Provincia");
        data.setRegione("us-east-2");
        data.setTipologia("Tipologia");
        data.setTitoloResp("Titolo Resp");

        ResultDto result = new ResultDto();
        result.setCodErr(-1);
        result.setDescErr("An error occurred");
        result.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        ws05ResponseDto.setData(data);
        ws05ResponseDto.setResult(result);
        IPAPecDto actualConvertToIPAPecDtoFromWS05Result = ipaConverter.convertToIPAPecDtoFromWS05(ws05ResponseDto);
        assertEquals("Cod Amm", actualConvertToIPAPecDtoFromWS05Result.getCodEnte());
        assertEquals(IpaConverter.ADDRESS_TYPE, actualConvertToIPAPecDtoFromWS05Result.getTipo());
        assertEquals("Mail1", actualConvertToIPAPecDtoFromWS05Result.getDomicilioDigitale());
        assertEquals("Des Amm", actualConvertToIPAPecDtoFromWS05Result.getDenominazione());
    }

    /**
     * Method under test: {@link IpaConverter#convertToIPAPecDtoFromWS05(WS05ResponseDto)}
     */
    @Test
    void testConvertToIPAPecDtoFromWS052() {
        DataWS05Dto data = new DataWS05Dto();
        data.setAcronimo("Acronimo");
        data.setCap("Cap");
        data.setCategoria("Categoria");
        data.setCf("Cf");
        data.setCodAmm("Cod Amm");
        data.setCognResp("Cogn Resp");
        data.setComune("Comune");
        data.setDataAccreditamento("alice.liddell@example.org");
        data.setDesAmm("Des Amm");
        data.setIndirizzo("Indirizzo");
        data.setLivAccessibilita("Liv Accessibilita");
        data.setMail1("Mail1");
        data.setMail2("Mail2");
        data.setMail3("Mail3");
        data.setMail4("Mail4");
        data.setMail5("Mail5");
        data.setNomeResp("Nome Resp");
        data.setProvincia("Provincia");
        data.setRegione("us-east-2");
        data.setTipologia("Tipologia");
        data.setTitoloResp("Titolo Resp");

        ResultDto result = new ResultDto();
        result.setCodErr(-1);
        result.setDescErr("An error occurred");
        result.setNumItems(1000);

        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cf");
        dataWS05Dto.setCodAmm("Cod Amm");
        dataWS05Dto.setCognResp("Cogn Resp");
        dataWS05Dto.setComune("Comune");
        dataWS05Dto.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto.setDesAmm("Des Amm");
        dataWS05Dto.setIndirizzo("Indirizzo");
        dataWS05Dto.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto.setMail1("Mail1");
        dataWS05Dto.setMail2("Mail2");
        dataWS05Dto.setMail3("Mail3");
        dataWS05Dto.setMail4("Mail4");
        dataWS05Dto.setMail5("Mail5");
        dataWS05Dto.setNomeResp("Nome Resp");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");
        WS05ResponseDto ws05ResponseDto = mock(WS05ResponseDto.class);
        when(ws05ResponseDto.getData()).thenReturn(dataWS05Dto);
        doNothing().when(ws05ResponseDto).setData(Mockito.<DataWS05Dto>any());
        doNothing().when(ws05ResponseDto).setResult(Mockito.<ResultDto>any());
        ws05ResponseDto.setData(data);
        ws05ResponseDto.setResult(result);
        IPAPecDto actualConvertToIPAPecDtoFromWS05Result = ipaConverter.convertToIPAPecDtoFromWS05(ws05ResponseDto);
        assertEquals("Cod Amm", actualConvertToIPAPecDtoFromWS05Result.getCodEnte());
        assertEquals(IpaConverter.ADDRESS_TYPE, actualConvertToIPAPecDtoFromWS05Result.getTipo());
        assertEquals("Mail1", actualConvertToIPAPecDtoFromWS05Result.getDomicilioDigitale());
        assertEquals("Des Amm", actualConvertToIPAPecDtoFromWS05Result.getDenominazione());
        verify(ws05ResponseDto).getData();
        verify(ws05ResponseDto).setData(Mockito.<DataWS05Dto>any());
        verify(ws05ResponseDto).setResult(Mockito.<ResultDto>any());
    }

    @Test
    void testConvertToIPAPecOKDto() {
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        dataWS23Dto.setCodAmm("Cod Ente");
        dataWS23Dto.setDesAmm("Denominazione");
        dataWS23Dto.setDomicilioDigitale("Domicilio Digitale");
        dataWS23Dto.setTipo("Type");
        List<DataWS23Dto> dataWS23DtoList = new ArrayList<>();
        dataWS23DtoList.add(dataWS23Dto);

        ResultDto resultDto = new ResultDto();
        resultDto.setCodErr(-1);
        resultDto.setDescErr("An error occurred");
        resultDto.setNumItems(1000);

        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ws23ResponseDto.setData(dataWS23DtoList);
        ws23ResponseDto.setResult(resultDto);
        IPAPecDto actualConvertToIPAPecOKDtoResult = ipaConverter.convertToIpaPecDtoFromWS23(ws23ResponseDto);
        assertEquals("Cod Ente", actualConvertToIPAPecOKDtoResult.getCodEnte());
        assertEquals("Type", actualConvertToIPAPecOKDtoResult.getTipo());
        assertEquals("Domicilio Digitale", actualConvertToIPAPecOKDtoResult.getDomicilioDigitale());
        assertEquals("Denominazione", actualConvertToIPAPecOKDtoResult.getDenominazione());
    }

}

