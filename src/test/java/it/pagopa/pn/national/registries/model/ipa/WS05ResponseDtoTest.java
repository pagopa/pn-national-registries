package it.pagopa.pn.national.registries.model.ipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WS05ResponseDtoTest {
    /**
     * Method under test: {@link WS05ResponseDto#canEqual(Object)}
     */
    @Test
    void testCanEqual() {
        assertFalse((new WS05ResponseDto()).canEqual("Other"));
    }

    /**
     * Method under test: {@link WS05ResponseDto#canEqual(Object)}
     */
    @Test
    void testCanEqual2() {
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
        result.setCodError(-1);
        result.setDescError("An error occurred");
        result.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        ws05ResponseDto.setData(data);
        ws05ResponseDto.setResult(result);

        DataWS05Dto data2 = new DataWS05Dto();
        data2.setAcronimo("Acronimo");
        data2.setCap("Cap");
        data2.setCategoria("Categoria");
        data2.setCf("Cf");
        data2.setCodAmm("Cod Amm");
        data2.setCognResp("Cogn Resp");
        data2.setComune("Comune");
        data2.setDataAccreditamento("alice.liddell@example.org");
        data2.setDesAmm("Des Amm");
        data2.setIndirizzo("Indirizzo");
        data2.setLivAccessibilita("Liv Accessibilita");
        data2.setMail1("Mail1");
        data2.setMail2("Mail2");
        data2.setMail3("Mail3");
        data2.setMail4("Mail4");
        data2.setMail5("Mail5");
        data2.setNomeResp("Nome Resp");
        data2.setProvincia("Provincia");
        data2.setRegione("us-east-2");
        data2.setTipologia("Tipologia");
        data2.setTitoloResp("Titolo Resp");

        ResultDto result2 = new ResultDto();
        result2.setCodError(-1);
        result2.setDescError("An error occurred");
        result2.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto2 = new WS05ResponseDto();
        ws05ResponseDto2.setData(data2);
        ws05ResponseDto2.setResult(result2);
        assertTrue(ws05ResponseDto.canEqual(ws05ResponseDto2));
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>default or parameterless constructor of {@link WS05ResponseDto}
     *   <li>{@link WS05ResponseDto#setData(DataWS05Dto)}
     *   <li>{@link WS05ResponseDto#setResult(ResultDto)}
     *   <li>{@link WS05ResponseDto#toString()}
     *   <li>{@link WS05ResponseDto#getData()}
     *   <li>{@link WS05ResponseDto#getResult()}
     * </ul>
     */
    @Test
    void testConstructor() {
        WS05ResponseDto actualWs05ResponseDto = new WS05ResponseDto();
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
        actualWs05ResponseDto.setData(data);
        ResultDto result = new ResultDto();
        result.setCodError(-1);
        result.setDescError("An error occurred");
        result.setNumItems(1000);
        actualWs05ResponseDto.setResult(result);
        String actualToStringResult = actualWs05ResponseDto.toString();
        assertSame(data, actualWs05ResponseDto.getData());
        assertSame(result, actualWs05ResponseDto.getResult());
        assertEquals("WS05ResponseDto(result=ResultDto(codError=-1, descError=An error occurred, numItems=1000), data"
                + "=DataWS05Dto(codAmm=Cod Amm, acronimo=Acronimo, desAmm=Des Amm, regione=us-east-2, provincia=Provincia,"
                + " comune=Comune, cap=Cap, indirizzo=Indirizzo, titoloResp=Titolo Resp, nomeResp=Nome Resp, cognResp=Cogn"
                + " Resp, sitoIstituzionale=null, livAccessibilita=Liv Accessibilita, mail1=Mail1, mail2=Mail2, mail3=Mail3,"
                + " mail4=Mail4, mail5=Mail5, tipologia=Tipologia, categoria=Categoria, dataAccreditamento=alice.liddell"
                + "@example.org, cf=Cf))", actualToStringResult);
    }

    /**
     * Method under test: {@link WS05ResponseDto#equals(Object)}
     */
    @Test
    void testEquals() {
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
        result.setCodError(-1);
        result.setDescError("An error occurred");
        result.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        ws05ResponseDto.setData(data);
        ws05ResponseDto.setResult(result);
        assertNotEquals(null, ws05ResponseDto);
    }

    /**
     * Method under test: {@link WS05ResponseDto#equals(Object)}
     */
    @Test
    void testEquals2() {
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
        result.setCodError(-1);
        result.setDescError("An error occurred");
        result.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        ws05ResponseDto.setData(data);
        ws05ResponseDto.setResult(result);
        assertNotEquals("Different type to WS05ResponseDto", ws05ResponseDto);
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link WS05ResponseDto#equals(Object)}
     *   <li>{@link WS05ResponseDto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals3() {
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
        result.setCodError(-1);
        result.setDescError("An error occurred");
        result.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        ws05ResponseDto.setData(data);
        ws05ResponseDto.setResult(result);
        assertEquals(ws05ResponseDto, ws05ResponseDto);
        int expectedHashCodeResult = ws05ResponseDto.hashCode();
        assertEquals(expectedHashCodeResult, ws05ResponseDto.hashCode());
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link WS05ResponseDto#equals(Object)}
     *   <li>{@link WS05ResponseDto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals4() {
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
        result.setCodError(-1);
        result.setDescError("An error occurred");
        result.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        ws05ResponseDto.setData(data);
        ws05ResponseDto.setResult(result);

        DataWS05Dto data2 = new DataWS05Dto();
        data2.setAcronimo("Acronimo");
        data2.setCap("Cap");
        data2.setCategoria("Categoria");
        data2.setCf("Cf");
        data2.setCodAmm("Cod Amm");
        data2.setCognResp("Cogn Resp");
        data2.setComune("Comune");
        data2.setDataAccreditamento("alice.liddell@example.org");
        data2.setDesAmm("Des Amm");
        data2.setIndirizzo("Indirizzo");
        data2.setLivAccessibilita("Liv Accessibilita");
        data2.setMail1("Mail1");
        data2.setMail2("Mail2");
        data2.setMail3("Mail3");
        data2.setMail4("Mail4");
        data2.setMail5("Mail5");
        data2.setNomeResp("Nome Resp");
        data2.setProvincia("Provincia");
        data2.setRegione("us-east-2");
        data2.setTipologia("Tipologia");
        data2.setTitoloResp("Titolo Resp");

        ResultDto result2 = new ResultDto();
        result2.setCodError(-1);
        result2.setDescError("An error occurred");
        result2.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto2 = new WS05ResponseDto();
        ws05ResponseDto2.setData(data2);
        ws05ResponseDto2.setResult(result2);
        assertEquals(ws05ResponseDto, ws05ResponseDto2);
        int expectedHashCodeResult = ws05ResponseDto.hashCode();
        assertEquals(expectedHashCodeResult, ws05ResponseDto2.hashCode());
    }

    /**
     * Method under test: {@link WS05ResponseDto#equals(Object)}
     */
    @Test
    void testEquals5() {
        DataWS05Dto data = mock(DataWS05Dto.class);
        doNothing().when(data).setAcronimo(Mockito.<String>any());
        doNothing().when(data).setCap(Mockito.<String>any());
        doNothing().when(data).setCategoria(Mockito.<String>any());
        doNothing().when(data).setCf(Mockito.<String>any());
        doNothing().when(data).setCodAmm(Mockito.<String>any());
        doNothing().when(data).setCognResp(Mockito.<String>any());
        doNothing().when(data).setComune(Mockito.<String>any());
        doNothing().when(data).setDataAccreditamento(Mockito.<String>any());
        doNothing().when(data).setDesAmm(Mockito.<String>any());
        doNothing().when(data).setIndirizzo(Mockito.<String>any());
        doNothing().when(data).setLivAccessibilita(Mockito.<String>any());
        doNothing().when(data).setMail1(Mockito.<String>any());
        doNothing().when(data).setMail2(Mockito.<String>any());
        doNothing().when(data).setMail3(Mockito.<String>any());
        doNothing().when(data).setMail4(Mockito.<String>any());
        doNothing().when(data).setMail5(Mockito.<String>any());
        doNothing().when(data).setNomeResp(Mockito.<String>any());
        doNothing().when(data).setProvincia(Mockito.<String>any());
        doNothing().when(data).setRegione(Mockito.<String>any());
        doNothing().when(data).setTipologia(Mockito.<String>any());
        doNothing().when(data).setTitoloResp(Mockito.<String>any());
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
        result.setCodError(-1);
        result.setDescError("An error occurred");
        result.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        ws05ResponseDto.setData(data);
        ws05ResponseDto.setResult(result);

        DataWS05Dto data2 = new DataWS05Dto();
        data2.setAcronimo("Acronimo");
        data2.setCap("Cap");
        data2.setCategoria("Categoria");
        data2.setCf("Cf");
        data2.setCodAmm("Cod Amm");
        data2.setCognResp("Cogn Resp");
        data2.setComune("Comune");
        data2.setDataAccreditamento("alice.liddell@example.org");
        data2.setDesAmm("Des Amm");
        data2.setIndirizzo("Indirizzo");
        data2.setLivAccessibilita("Liv Accessibilita");
        data2.setMail1("Mail1");
        data2.setMail2("Mail2");
        data2.setMail3("Mail3");
        data2.setMail4("Mail4");
        data2.setMail5("Mail5");
        data2.setNomeResp("Nome Resp");
        data2.setProvincia("Provincia");
        data2.setRegione("us-east-2");
        data2.setTipologia("Tipologia");
        data2.setTitoloResp("Titolo Resp");

        ResultDto result2 = new ResultDto();
        result2.setCodError(-1);
        result2.setDescError("An error occurred");
        result2.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto2 = new WS05ResponseDto();
        ws05ResponseDto2.setData(data2);
        ws05ResponseDto2.setResult(result2);
        assertNotEquals(ws05ResponseDto, ws05ResponseDto2);
    }

    /**
     * Method under test: {@link WS05ResponseDto#equals(Object)}
     */
    @Test
    void testEquals6() {
        DataWS05Dto data = mock(DataWS05Dto.class);
        doNothing().when(data).setAcronimo(Mockito.<String>any());
        doNothing().when(data).setCap(Mockito.<String>any());
        doNothing().when(data).setCategoria(Mockito.<String>any());
        doNothing().when(data).setCf(Mockito.<String>any());
        doNothing().when(data).setCodAmm(Mockito.<String>any());
        doNothing().when(data).setCognResp(Mockito.<String>any());
        doNothing().when(data).setComune(Mockito.<String>any());
        doNothing().when(data).setDataAccreditamento(Mockito.<String>any());
        doNothing().when(data).setDesAmm(Mockito.<String>any());
        doNothing().when(data).setIndirizzo(Mockito.<String>any());
        doNothing().when(data).setLivAccessibilita(Mockito.<String>any());
        doNothing().when(data).setMail1(Mockito.<String>any());
        doNothing().when(data).setMail2(Mockito.<String>any());
        doNothing().when(data).setMail3(Mockito.<String>any());
        doNothing().when(data).setMail4(Mockito.<String>any());
        doNothing().when(data).setMail5(Mockito.<String>any());
        doNothing().when(data).setNomeResp(Mockito.<String>any());
        doNothing().when(data).setProvincia(Mockito.<String>any());
        doNothing().when(data).setRegione(Mockito.<String>any());
        doNothing().when(data).setTipologia(Mockito.<String>any());
        doNothing().when(data).setTitoloResp(Mockito.<String>any());
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
        ResultDto result = mock(ResultDto.class);
        doNothing().when(result).setCodError(anyInt());
        doNothing().when(result).setDescError(Mockito.<String>any());
        doNothing().when(result).setNumItems(anyInt());
        result.setCodError(-1);
        result.setDescError("An error occurred");
        result.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        ws05ResponseDto.setData(data);
        ws05ResponseDto.setResult(result);

        DataWS05Dto data2 = new DataWS05Dto();
        data2.setAcronimo("Acronimo");
        data2.setCap("Cap");
        data2.setCategoria("Categoria");
        data2.setCf("Cf");
        data2.setCodAmm("Cod Amm");
        data2.setCognResp("Cogn Resp");
        data2.setComune("Comune");
        data2.setDataAccreditamento("alice.liddell@example.org");
        data2.setDesAmm("Des Amm");
        data2.setIndirizzo("Indirizzo");
        data2.setLivAccessibilita("Liv Accessibilita");
        data2.setMail1("Mail1");
        data2.setMail2("Mail2");
        data2.setMail3("Mail3");
        data2.setMail4("Mail4");
        data2.setMail5("Mail5");
        data2.setNomeResp("Nome Resp");
        data2.setProvincia("Provincia");
        data2.setRegione("us-east-2");
        data2.setTipologia("Tipologia");
        data2.setTitoloResp("Titolo Resp");

        ResultDto result2 = new ResultDto();
        result2.setCodError(-1);
        result2.setDescError("An error occurred");
        result2.setNumItems(1000);

        WS05ResponseDto ws05ResponseDto2 = new WS05ResponseDto();
        ws05ResponseDto2.setData(data2);
        ws05ResponseDto2.setResult(result2);
        assertNotEquals(ws05ResponseDto, ws05ResponseDto2);
    }
}

