package it.pagopa.pn.national.registries.model.ipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WS05RequestDtoTest {
    /**
     * Method under test: {@link WS05RequestDto#canEqual(Object)}
     */
    @Test
    void testCanEqual() {
        assertFalse((new WS05RequestDto()).canEqual("Other"));
    }

    /**
     * Method under test: {@link WS05RequestDto#canEqual(Object)}
     */
    @Test
    void testCanEqual2() {
        WS05RequestDto ws05RequestDto = new WS05RequestDto();
        ws05RequestDto.setAuthId("42");
        ws05RequestDto.setCodiceAmministrazione("Codice Amministrazione");

        WS05RequestDto ws05RequestDto2 = new WS05RequestDto();
        ws05RequestDto2.setAuthId("42");
        ws05RequestDto2.setCodiceAmministrazione("Codice Amministrazione");
        assertTrue(ws05RequestDto.canEqual(ws05RequestDto2));
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>default or parameterless constructor of {@link WS05RequestDto}
     *   <li>{@link WS05RequestDto#setAuthId(String)}
     *   <li>{@link WS05RequestDto#setCodiceAmministrazione(String)}
     *   <li>{@link WS05RequestDto#toString()}
     *   <li>{@link WS05RequestDto#getAuthId()}
     *   <li>{@link WS05RequestDto#getCodiceAmministrazione()}
     * </ul>
     */
    @Test
    void testConstructor() {
        WS05RequestDto actualWs05RequestDto = new WS05RequestDto();
        actualWs05RequestDto.setAuthId("42");
        actualWs05RequestDto.setCodiceAmministrazione("Codice Amministrazione");
        String actualToStringResult = actualWs05RequestDto.toString();
        assertEquals("42", actualWs05RequestDto.getAuthId());
        assertEquals("Codice Amministrazione", actualWs05RequestDto.getCodiceAmministrazione());
        assertEquals("WS05RequestDto(codiceAmministrazione=Codice Amministrazione, authId=42)", actualToStringResult);
    }

    /**
     * Method under test: {@link WS05RequestDto#equals(Object)}
     */
    @Test
    void testEquals() {
        WS05RequestDto ws05RequestDto = new WS05RequestDto();
        ws05RequestDto.setAuthId("42");
        ws05RequestDto.setCodiceAmministrazione("Codice Amministrazione");
        assertNotEquals(null, ws05RequestDto);
    }

    /**
     * Method under test: {@link WS05RequestDto#equals(Object)}
     */
    @Test
    void testEquals2() {
        WS05RequestDto ws05RequestDto = new WS05RequestDto();
        ws05RequestDto.setAuthId("42");
        ws05RequestDto.setCodiceAmministrazione("Codice Amministrazione");
        assertNotEquals("Different type to WS05RequestDto", ws05RequestDto);
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link WS05RequestDto#equals(Object)}
     *   <li>{@link WS05RequestDto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals3() {
        WS05RequestDto ws05RequestDto = new WS05RequestDto();
        ws05RequestDto.setAuthId("42");
        ws05RequestDto.setCodiceAmministrazione("Codice Amministrazione");
        assertEquals(ws05RequestDto, ws05RequestDto);
        int expectedHashCodeResult = ws05RequestDto.hashCode();
        assertEquals(expectedHashCodeResult, ws05RequestDto.hashCode());
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link WS05RequestDto#equals(Object)}
     *   <li>{@link WS05RequestDto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals4() {
        WS05RequestDto ws05RequestDto = new WS05RequestDto();
        ws05RequestDto.setAuthId("42");
        ws05RequestDto.setCodiceAmministrazione("Codice Amministrazione");

        WS05RequestDto ws05RequestDto2 = new WS05RequestDto();
        ws05RequestDto2.setAuthId("42");
        ws05RequestDto2.setCodiceAmministrazione("Codice Amministrazione");
        assertEquals(ws05RequestDto, ws05RequestDto2);
        int expectedHashCodeResult = ws05RequestDto.hashCode();
        assertEquals(expectedHashCodeResult, ws05RequestDto2.hashCode());
    }




    /**
     * Method under test: {@link WS05RequestDto#equals(Object)}
     */
    @Test
    void testEquals7() {
        WS05RequestDto ws05RequestDto = new WS05RequestDto();
        ws05RequestDto.setAuthId("42");
        ws05RequestDto.setCodiceAmministrazione("42");

        WS05RequestDto ws05RequestDto2 = new WS05RequestDto();
        ws05RequestDto2.setAuthId("42");
        ws05RequestDto2.setCodiceAmministrazione("Codice Amministrazione");
        assertNotEquals(ws05RequestDto, ws05RequestDto2);
    }

    /**
     * Method under test: {@link WS05RequestDto#equals(Object)}
     */
    @Test
    void testEquals8() {
        WS05RequestDto ws05RequestDto = new WS05RequestDto();
        ws05RequestDto.setAuthId("42");
        ws05RequestDto.setCodiceAmministrazione(null);

        WS05RequestDto ws05RequestDto2 = new WS05RequestDto();
        ws05RequestDto2.setAuthId("42");
        ws05RequestDto2.setCodiceAmministrazione("Codice Amministrazione");
        assertNotEquals(ws05RequestDto, ws05RequestDto2);
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link WS05RequestDto#equals(Object)}
     *   <li>{@link WS05RequestDto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals9() {
        WS05RequestDto ws05RequestDto = new WS05RequestDto();
        ws05RequestDto.setAuthId(null);
        ws05RequestDto.setCodiceAmministrazione("Codice Amministrazione");

        WS05RequestDto ws05RequestDto2 = new WS05RequestDto();
        ws05RequestDto2.setAuthId(null);
        ws05RequestDto2.setCodiceAmministrazione("Codice Amministrazione");
        assertEquals(ws05RequestDto, ws05RequestDto2);
        int expectedHashCodeResult = ws05RequestDto.hashCode();
        assertEquals(expectedHashCodeResult, ws05RequestDto2.hashCode());
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link WS05RequestDto#equals(Object)}
     *   <li>{@link WS05RequestDto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals10() {
        WS05RequestDto ws05RequestDto = new WS05RequestDto();
        ws05RequestDto.setAuthId("42");
        ws05RequestDto.setCodiceAmministrazione(null);

        WS05RequestDto ws05RequestDto2 = new WS05RequestDto();
        ws05RequestDto2.setAuthId("42");
        ws05RequestDto2.setCodiceAmministrazione(null);
        assertEquals(ws05RequestDto, ws05RequestDto2);
        int expectedHashCodeResult = ws05RequestDto.hashCode();
        assertEquals(expectedHashCodeResult, ws05RequestDto2.hashCode());
    }
}

