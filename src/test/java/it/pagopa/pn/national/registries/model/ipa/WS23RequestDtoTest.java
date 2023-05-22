package it.pagopa.pn.national.registries.model.ipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WS23RequestDtoTest {
    /**
     * Method under test: {@link WS23RequestDto#canEqual(Object)}
     */
    @Test
    void testCanEqual() {
        assertFalse((new WS23RequestDto()).canEqual("Other"));
    }

    /**
     * Method under test: {@link WS23RequestDto#canEqual(Object)}
     */
    @Test
    void testCanEqual2() {
        WS23RequestDto ws23RequestDto = new WS23RequestDto();
        ws23RequestDto.setAuthId("42");
        ws23RequestDto.setCodiceFiscale("Codice Fiscale");

        WS23RequestDto ws23RequestDto2 = new WS23RequestDto();
        ws23RequestDto2.setAuthId("42");
        ws23RequestDto2.setCodiceFiscale("Codice Fiscale");
        assertTrue(ws23RequestDto.canEqual(ws23RequestDto2));
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>default or parameterless constructor of {@link WS23RequestDto}
     *   <li>{@link WS23RequestDto#setAuthId(String)}
     *   <li>{@link WS23RequestDto#setCodiceFiscale(String)}
     *   <li>{@link WS23RequestDto#toString()}
     *   <li>{@link WS23RequestDto#getAuthId()}
     *   <li>{@link WS23RequestDto#getCodiceFiscale()}
     * </ul>
     */
    @Test
    void testConstructor() {
        WS23RequestDto actualWs23RequestDto = new WS23RequestDto();
        actualWs23RequestDto.setAuthId("42");
        actualWs23RequestDto.setCodiceFiscale("Codice Fiscale");
        String actualToStringResult = actualWs23RequestDto.toString();
        assertEquals("42", actualWs23RequestDto.getAuthId());
        assertEquals("Codice Fiscale", actualWs23RequestDto.getCodiceFiscale());
        assertEquals("WS23RequestDto(codiceFiscale=Codice Fiscale, authId=42)", actualToStringResult);
    }

    /**
     * Method under test: {@link WS23RequestDto#equals(Object)}
     */
    @Test
    void testEquals() {
        WS23RequestDto ws23RequestDto = new WS23RequestDto();
        ws23RequestDto.setAuthId("42");
        ws23RequestDto.setCodiceFiscale("Codice Fiscale");
        assertNotEquals(null, ws23RequestDto);
    }

    /**
     * Method under test: {@link WS23RequestDto#equals(Object)}
     */
    @Test
    void testEquals2() {
        WS23RequestDto ws23RequestDto = new WS23RequestDto();
        ws23RequestDto.setAuthId("42");
        ws23RequestDto.setCodiceFiscale("Codice Fiscale");
        assertNotEquals("Different type to WS23RequestDto", ws23RequestDto);
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link WS23RequestDto#equals(Object)}
     *   <li>{@link WS23RequestDto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals3() {
        WS23RequestDto ws23RequestDto = new WS23RequestDto();
        ws23RequestDto.setAuthId("42");
        ws23RequestDto.setCodiceFiscale("Codice Fiscale");
        assertEquals(ws23RequestDto, ws23RequestDto);
        int expectedHashCodeResult = ws23RequestDto.hashCode();
        assertEquals(expectedHashCodeResult, ws23RequestDto.hashCode());
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link WS23RequestDto#equals(Object)}
     *   <li>{@link WS23RequestDto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals4() {
        WS23RequestDto ws23RequestDto = new WS23RequestDto();
        ws23RequestDto.setAuthId("42");
        ws23RequestDto.setCodiceFiscale("Codice Fiscale");

        WS23RequestDto ws23RequestDto2 = new WS23RequestDto();
        ws23RequestDto2.setAuthId("42");
        ws23RequestDto2.setCodiceFiscale("Codice Fiscale");
        assertEquals(ws23RequestDto, ws23RequestDto2);
        int expectedHashCodeResult = ws23RequestDto.hashCode();
        assertEquals(expectedHashCodeResult, ws23RequestDto2.hashCode());
    }
    

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link WS23RequestDto#equals(Object)}
     *   <li>{@link WS23RequestDto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals9() {
        WS23RequestDto ws23RequestDto = new WS23RequestDto();
        ws23RequestDto.setAuthId(null);
        ws23RequestDto.setCodiceFiscale("Codice Fiscale");

        WS23RequestDto ws23RequestDto2 = new WS23RequestDto();
        ws23RequestDto2.setAuthId(null);
        ws23RequestDto2.setCodiceFiscale("Codice Fiscale");
        assertEquals(ws23RequestDto, ws23RequestDto2);
        int expectedHashCodeResult = ws23RequestDto.hashCode();
        assertEquals(expectedHashCodeResult, ws23RequestDto2.hashCode());
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link WS23RequestDto#equals(Object)}
     *   <li>{@link WS23RequestDto#hashCode()}
     * </ul>
     */
    @Test
    void testEquals10() {
        WS23RequestDto ws23RequestDto = new WS23RequestDto();
        ws23RequestDto.setAuthId("42");
        ws23RequestDto.setCodiceFiscale(null);

        WS23RequestDto ws23RequestDto2 = new WS23RequestDto();
        ws23RequestDto2.setAuthId("42");
        ws23RequestDto2.setCodiceFiscale(null);
        assertEquals(ws23RequestDto, ws23RequestDto2);
        int expectedHashCodeResult = ws23RequestDto.hashCode();
        assertEquals(expectedHashCodeResult, ws23RequestDto2.hashCode());
    }
}

