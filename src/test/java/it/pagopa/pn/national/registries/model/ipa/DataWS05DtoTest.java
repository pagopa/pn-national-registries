package it.pagopa.pn.national.registries.model.ipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DataWS05DtoTest {
    /**
     * Method under test: {@link DataWS05Dto#canEqual(Object)}
     */
    @Test
    void testCanEqual() {
        assertFalse((new DataWS05Dto()).canEqual("Other"));
    }

    /**
     * Method under test: {@link DataWS05Dto#canEqual(Object)}
     */
    @Test
    void testCanEqual2() {
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertTrue(dataWS05Dto.canEqual(dataWS05Dto2));
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>default or parameterless constructor of {@link DataWS05Dto}
     *   <li>{@link DataWS05Dto#setAcronimo(String)}
     *   <li>{@link DataWS05Dto#setCap(String)}
     *   <li>{@link DataWS05Dto#setCategoria(String)}
     *   <li>{@link DataWS05Dto#setCf(String)}
     *   <li>{@link DataWS05Dto#setCodAmm(String)}
     *   <li>{@link DataWS05Dto#setCognResp(String)}
     *   <li>{@link DataWS05Dto#setComune(String)}
     *   <li>{@link DataWS05Dto#setDataAccreditamento(String)}
     *   <li>{@link DataWS05Dto#setDesAmm(String)}
     *   <li>{@link DataWS05Dto#setIndirizzo(String)}
     *   <li>{@link DataWS05Dto#setLivAccessibilita(String)}
     *   <li>{@link DataWS05Dto#setMail1(String)}
     *   <li>{@link DataWS05Dto#setMail2(String)}
     *   <li>{@link DataWS05Dto#setMail3(String)}
     *   <li>{@link DataWS05Dto#setMail4(String)}
     *   <li>{@link DataWS05Dto#setMail5(String)}
     *   <li>{@link DataWS05Dto#setNomeResp(String)}
     *   <li>{@link DataWS05Dto#setProvincia(String)}
     *   <li>{@link DataWS05Dto#setRegione(String)}
     *   <li>{@link DataWS05Dto#setTipologia(String)}
     *   <li>{@link DataWS05Dto#setTitoloResp(String)}
     *   <li>{@link DataWS05Dto#setSitoIstituzionale(String)}
     *   <li>{@link DataWS05Dto#toString()}
     *   <li>{@link DataWS05Dto#getAcronimo()}
     *   <li>{@link DataWS05Dto#getCap()}
     *   <li>{@link DataWS05Dto#getCategoria()}
     *   <li>{@link DataWS05Dto#getCf()}
     *   <li>{@link DataWS05Dto#getCodAmm()}
     *   <li>{@link DataWS05Dto#getCognResp()}
     *   <li>{@link DataWS05Dto#getComune()}
     *   <li>{@link DataWS05Dto#getDataAccreditamento()}
     *   <li>{@link DataWS05Dto#getDesAmm()}
     *   <li>{@link DataWS05Dto#getIndirizzo()}
     *   <li>{@link DataWS05Dto#getLivAccessibilita()}
     *   <li>{@link DataWS05Dto#getMail1()}
     *   <li>{@link DataWS05Dto#getMail2()}
     *   <li>{@link DataWS05Dto#getMail3()}
     *   <li>{@link DataWS05Dto#getMail4()}
     *   <li>{@link DataWS05Dto#getMail5()}
     *   <li>{@link DataWS05Dto#getNomeResp()}
     *   <li>{@link DataWS05Dto#getProvincia()}
     *   <li>{@link DataWS05Dto#getRegione()}
     *   <li>{@link DataWS05Dto#getSitoIstituzionale()}
     *   <li>{@link DataWS05Dto#getTipologia()}
     *   <li>{@link DataWS05Dto#getTitoloResp()}
     * </ul>
     */
    @Test
    void testConstructor() {
        DataWS05Dto actualDataWS05Dto = new DataWS05Dto();
        actualDataWS05Dto.setAcronimo("Acronimo");
        actualDataWS05Dto.setCap("Cap");
        actualDataWS05Dto.setCategoria("Categoria");
        actualDataWS05Dto.setCf("Cf");
        actualDataWS05Dto.setCodAmm("Cod Amm");
        actualDataWS05Dto.setCognResp("Cogn Resp");
        actualDataWS05Dto.setComune("Comune");
        actualDataWS05Dto.setDataAccreditamento("alice.liddell@example.org");
        actualDataWS05Dto.setDesAmm("Des Amm");
        actualDataWS05Dto.setIndirizzo("Indirizzo");
        actualDataWS05Dto.setLivAccessibilita("Liv Accessibilita");
        actualDataWS05Dto.setMail1("Mail1");
        actualDataWS05Dto.setMail2("Mail2");
        actualDataWS05Dto.setMail3("Mail3");
        actualDataWS05Dto.setMail4("Mail4");
        actualDataWS05Dto.setMail5("Mail5");
        actualDataWS05Dto.setNomeResp("Nome Resp");
        actualDataWS05Dto.setProvincia("Provincia");
        actualDataWS05Dto.setRegione("us-east-2");
        actualDataWS05Dto.setTipologia("Tipologia");
        actualDataWS05Dto.setTitoloResp("Titolo Resp");
        actualDataWS05Dto.setSitoIstituzionale("Sito Istituzionale");
        String actualToStringResult = actualDataWS05Dto.toString();
        assertEquals("Acronimo", actualDataWS05Dto.getAcronimo());
        assertEquals("Cap", actualDataWS05Dto.getCap());
        assertEquals("Categoria", actualDataWS05Dto.getCategoria());
        assertEquals("Cf", actualDataWS05Dto.getCf());
        assertEquals("Cod Amm", actualDataWS05Dto.getCodAmm());
        assertEquals("Cogn Resp", actualDataWS05Dto.getCognResp());
        assertEquals("Comune", actualDataWS05Dto.getComune());
        assertEquals("alice.liddell@example.org", actualDataWS05Dto.getDataAccreditamento());
        assertEquals("Des Amm", actualDataWS05Dto.getDesAmm());
        assertEquals("Indirizzo", actualDataWS05Dto.getIndirizzo());
        assertEquals("Liv Accessibilita", actualDataWS05Dto.getLivAccessibilita());
        assertEquals("Mail1", actualDataWS05Dto.getMail1());
        assertEquals("Mail2", actualDataWS05Dto.getMail2());
        assertEquals("Mail3", actualDataWS05Dto.getMail3());
        assertEquals("Mail4", actualDataWS05Dto.getMail4());
        assertEquals("Mail5", actualDataWS05Dto.getMail5());
        assertEquals("Nome Resp", actualDataWS05Dto.getNomeResp());
        assertEquals("Provincia", actualDataWS05Dto.getProvincia());
        assertEquals("us-east-2", actualDataWS05Dto.getRegione());
        assertEquals("Sito Istituzionale", actualDataWS05Dto.getSitoIstituzionale());
        assertEquals("Tipologia", actualDataWS05Dto.getTipologia());
        assertEquals("Titolo Resp", actualDataWS05Dto.getTitoloResp());
        assertEquals(
                "DataWS05Dto(codAmm=Cod Amm, acronimo=Acronimo, desAmm=Des Amm, regione=us-east-2, provincia=Provincia,"
                        + " comune=Comune, cap=Cap, indirizzo=Indirizzo, titoloResp=Titolo Resp, nomeResp=Nome Resp, cognResp=Cogn"
                        + " Resp, sitoIstituzionale=Sito Istituzionale, livAccessibilita=Liv Accessibilita, mail1=Mail1, mail2=Mail2,"
                        + " mail3=Mail3, mail4=Mail4, mail5=Mail5, tipologia=Tipologia, categoria=Categoria, dataAccreditamento"
                        + "=alice.liddell@example.org, cf=Cf)",
                actualToStringResult);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals() {
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
        assertNotEquals(null, dataWS05Dto);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals2() {
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
        assertNotEquals( "Different type to DataWS05Dto", dataWS05Dto);
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link DataWS05Dto#equals(Object)}
     *   <li>{@link DataWS05Dto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals3() {
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
        assertEquals(dataWS05Dto, dataWS05Dto);
        int expectedHashCodeResult = dataWS05Dto.hashCode();
        assertEquals(expectedHashCodeResult, dataWS05Dto.hashCode());
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link DataWS05Dto#equals(Object)}
     *   <li>{@link DataWS05Dto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals4() {
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertEquals(dataWS05Dto, dataWS05Dto2);
        int expectedHashCodeResult = dataWS05Dto.hashCode();
        assertEquals(expectedHashCodeResult, dataWS05Dto2.hashCode());
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals5() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Cod Amm");
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals6() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo(null);
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals7() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cod Amm");
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals8() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap(null);
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals9() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Cod Amm");
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals10() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria(null);
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals11() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cod Amm");
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals12() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf(null);
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals13() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cf");
        dataWS05Dto.setCodAmm("Acronimo");
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals14() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cf");
        dataWS05Dto.setCodAmm(null);
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals15() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cf");
        dataWS05Dto.setCodAmm("Cod Amm");
        dataWS05Dto.setCognResp("Cod Amm");
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals16() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cf");
        dataWS05Dto.setCodAmm("Cod Amm");
        dataWS05Dto.setCognResp(null);
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals17() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cf");
        dataWS05Dto.setCodAmm("Cod Amm");
        dataWS05Dto.setCognResp("Cogn Resp");
        dataWS05Dto.setComune("Cod Amm");
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals18() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cf");
        dataWS05Dto.setCodAmm("Cod Amm");
        dataWS05Dto.setCognResp("Cogn Resp");
        dataWS05Dto.setComune(null);
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals19() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cf");
        dataWS05Dto.setCodAmm("Cod Amm");
        dataWS05Dto.setCognResp("Cogn Resp");
        dataWS05Dto.setComune("Comune");
        dataWS05Dto.setDataAccreditamento("Cod Amm");
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals20() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cf");
        dataWS05Dto.setCodAmm("Cod Amm");
        dataWS05Dto.setCognResp("Cogn Resp");
        dataWS05Dto.setComune("Comune");
        dataWS05Dto.setDataAccreditamento(null);
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals21() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cf");
        dataWS05Dto.setCodAmm("Cod Amm");
        dataWS05Dto.setCognResp("Cogn Resp");
        dataWS05Dto.setComune("Comune");
        dataWS05Dto.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto.setDesAmm("Cod Amm");
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals22() {
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("Acronimo");
        dataWS05Dto.setCap("Cap");
        dataWS05Dto.setCategoria("Categoria");
        dataWS05Dto.setCf("Cf");
        dataWS05Dto.setCodAmm("Cod Amm");
        dataWS05Dto.setCognResp("Cogn Resp");
        dataWS05Dto.setComune("Comune");
        dataWS05Dto.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto.setDesAmm(null);
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals23() {
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
        dataWS05Dto.setIndirizzo("Cod Amm");
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals24() {
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
        dataWS05Dto.setIndirizzo(null);
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals25() {
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
        dataWS05Dto.setLivAccessibilita("Cod Amm");
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals26() {
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
        dataWS05Dto.setLivAccessibilita(null);
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

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals27() {
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
        dataWS05Dto.setMail1("Cod Amm");
        dataWS05Dto.setMail2("Mail2");
        dataWS05Dto.setMail3("Mail3");
        dataWS05Dto.setMail4("Mail4");
        dataWS05Dto.setMail5("Mail5");
        dataWS05Dto.setNomeResp("Nome Resp");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals28() {
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
        dataWS05Dto.setMail1(null);
        dataWS05Dto.setMail2("Mail2");
        dataWS05Dto.setMail3("Mail3");
        dataWS05Dto.setMail4("Mail4");
        dataWS05Dto.setMail5("Mail5");
        dataWS05Dto.setNomeResp("Nome Resp");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals29() {
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
        dataWS05Dto.setMail2("Cod Amm");
        dataWS05Dto.setMail3("Mail3");
        dataWS05Dto.setMail4("Mail4");
        dataWS05Dto.setMail5("Mail5");
        dataWS05Dto.setNomeResp("Nome Resp");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals30() {
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
        dataWS05Dto.setMail2(null);
        dataWS05Dto.setMail3("Mail3");
        dataWS05Dto.setMail4("Mail4");
        dataWS05Dto.setMail5("Mail5");
        dataWS05Dto.setNomeResp("Nome Resp");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals31() {
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
        dataWS05Dto.setMail3("Cod Amm");
        dataWS05Dto.setMail4("Mail4");
        dataWS05Dto.setMail5("Mail5");
        dataWS05Dto.setNomeResp("Nome Resp");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals32() {
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
        dataWS05Dto.setMail3(null);
        dataWS05Dto.setMail4("Mail4");
        dataWS05Dto.setMail5("Mail5");
        dataWS05Dto.setNomeResp("Nome Resp");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals33() {
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
        dataWS05Dto.setMail4("Cod Amm");
        dataWS05Dto.setMail5("Mail5");
        dataWS05Dto.setNomeResp("Nome Resp");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals34() {
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
        dataWS05Dto.setMail4(null);
        dataWS05Dto.setMail5("Mail5");
        dataWS05Dto.setNomeResp("Nome Resp");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals35() {
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
        dataWS05Dto.setMail5("Cod Amm");
        dataWS05Dto.setNomeResp("Nome Resp");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals36() {
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
        dataWS05Dto.setMail5(null);
        dataWS05Dto.setNomeResp("Nome Resp");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals37() {
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
        dataWS05Dto.setNomeResp("Cod Amm");
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals38() {
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
        dataWS05Dto.setNomeResp(null);
        dataWS05Dto.setProvincia("Provincia");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals39() {
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
        dataWS05Dto.setProvincia("Cod Amm");
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals40() {
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
        dataWS05Dto.setProvincia(null);
        dataWS05Dto.setRegione("us-east-2");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals41() {
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
        dataWS05Dto.setRegione("Cod Amm");
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals42() {
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
        dataWS05Dto.setRegione(null);
        dataWS05Dto.setTipologia("Tipologia");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals43() {
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
        dataWS05Dto.setTipologia("Cod Amm");
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals44() {
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
        dataWS05Dto.setTipologia(null);
        dataWS05Dto.setTitoloResp("Titolo Resp");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals45() {
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
        dataWS05Dto.setTitoloResp("Cod Amm");

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }

    /**
     * Method under test: {@link DataWS05Dto#equals(Object)}
     */
    @Test
    void testEquals46() {
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
        dataWS05Dto.setTitoloResp(null);

        DataWS05Dto dataWS05Dto2 = new DataWS05Dto();
        dataWS05Dto2.setAcronimo("Acronimo");
        dataWS05Dto2.setCap("Cap");
        dataWS05Dto2.setCategoria("Categoria");
        dataWS05Dto2.setCf("Cf");
        dataWS05Dto2.setCodAmm("Cod Amm");
        dataWS05Dto2.setCognResp("Cogn Resp");
        dataWS05Dto2.setComune("Comune");
        dataWS05Dto2.setDataAccreditamento("alice.liddell@example.org");
        dataWS05Dto2.setDesAmm("Des Amm");
        dataWS05Dto2.setIndirizzo("Indirizzo");
        dataWS05Dto2.setLivAccessibilita("Liv Accessibilita");
        dataWS05Dto2.setMail1("Mail1");
        dataWS05Dto2.setMail2("Mail2");
        dataWS05Dto2.setMail3("Mail3");
        dataWS05Dto2.setMail4("Mail4");
        dataWS05Dto2.setMail5("Mail5");
        dataWS05Dto2.setNomeResp("Nome Resp");
        dataWS05Dto2.setProvincia("Provincia");
        dataWS05Dto2.setRegione("us-east-2");
        dataWS05Dto2.setTipologia("Tipologia");
        dataWS05Dto2.setTitoloResp("Titolo Resp");
        assertNotEquals(dataWS05Dto, dataWS05Dto2);
    }
}

