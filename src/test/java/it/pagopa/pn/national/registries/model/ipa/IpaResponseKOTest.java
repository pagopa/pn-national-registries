package it.pagopa.pn.national.registries.model.ipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IpaResponseKOTest {
    /**
     * Method under test: {@link IpaResponseKO#canEqual(Object)}
     */
    @Test
    void testCanEqual() {
        assertFalse((new IpaResponseKO()).canEqual("Other"));
    }

    /**
     * Method under test: {@link IpaResponseKO#canEqual(Object)}
     */
    @Test
    void testCanEqual2() {
        IpaResponseKO ipaResponseKO = new IpaResponseKO();

        IpaResponseKO ipaResponseKO1 = new IpaResponseKO();
        ipaResponseKO1.setCode("Code");
        ipaResponseKO1.setDetail("Detail");
        assertTrue(ipaResponseKO.canEqual(ipaResponseKO1));
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>default or parameterless constructor of {@link IpaResponseKO}
     *   <li>{@link IpaResponseKO#setCode(String)}
     *   <li>{@link IpaResponseKO#setDetail(String)}
     *   <li>{@link IpaResponseKO#toString()}
     *   <li>{@link IpaResponseKO#getCode()}
     *   <li>{@link IpaResponseKO#getDetail()}
     * </ul>
     */
    @Test
    void testConstructor() {
        IpaResponseKO actualIpaResponseKO = new IpaResponseKO();
        actualIpaResponseKO.setCode("Code");
        actualIpaResponseKO.setDetail("Detail");
        String actualToStringResult = actualIpaResponseKO.toString();
        assertEquals("Code", actualIpaResponseKO.getCode());
        assertEquals("Detail", actualIpaResponseKO.getDetail());
        assertEquals("IpaResponseKO(code=Code, detail=Detail)", actualToStringResult);
    }

    /**
     * Method under test: {@link IpaResponseKO#equals(Object)}
     */
    @Test
    void testEquals() {
        IpaResponseKO ipaResponseKO = new IpaResponseKO();
        ipaResponseKO.setCode("Code");
        ipaResponseKO.setDetail("Detail");
        assertNotEquals(null,
                ipaResponseKO);
    }

    /**
     * Method under test: {@link IpaResponseKO#equals(Object)}
     */
    @Test
    void testEquals2() {
        IpaResponseKO ipaResponseKO = new IpaResponseKO();
        ipaResponseKO.setCode("Code");
        ipaResponseKO.setDetail("Detail");
        assertNotEquals("Different type to IpaResponseKO", ipaResponseKO);
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link IpaResponseKO#equals(Object)}
     *   <li>{@link IpaResponseKO#hashCode()}
     * </ul>
     */
    @Test
    void testEquals3() {
        IpaResponseKO ipaResponseKO = new IpaResponseKO();
        ipaResponseKO.setCode("Code");
        ipaResponseKO.setDetail("Detail");
        assertEquals(ipaResponseKO, ipaResponseKO);
        int expectedHashCodeResult = ipaResponseKO.hashCode();
        assertEquals(expectedHashCodeResult, ipaResponseKO.hashCode());
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link IpaResponseKO#equals(Object)}
     *   <li>{@link IpaResponseKO#hashCode()}
     * </ul>
     */
    @Test
    void testEquals4() {
        IpaResponseKO ipaResponseKO = new IpaResponseKO();
        ipaResponseKO.setCode("Code");
        ipaResponseKO.setDetail("Detail");

        IpaResponseKO ipaResponseKO1 = new IpaResponseKO();
        ipaResponseKO1.setCode("Code");
        ipaResponseKO1.setDetail("Detail");
        assertEquals(ipaResponseKO, ipaResponseKO1);
        int expectedHashCodeResult = ipaResponseKO.hashCode();
        assertEquals(expectedHashCodeResult, ipaResponseKO1.hashCode());
    }



    /**
     * Method under test: {@link IpaResponseKO#equals(Object)}
     */
    @Test
    void testEquals7() {
        IpaResponseKO ipaResponseKO = new IpaResponseKO();
        ipaResponseKO.setCode("Code");
        ipaResponseKO.setDetail("Code");

        IpaResponseKO ipaResponseKO1 = new IpaResponseKO();
        ipaResponseKO1.setCode("Code");
        ipaResponseKO1.setDetail("Detail");
        assertNotEquals(ipaResponseKO, ipaResponseKO1);
    }

    /**
     * Method under test: {@link IpaResponseKO#equals(Object)}
     */
    @Test
    void testEquals8() {
        IpaResponseKO ipaResponseKO = new IpaResponseKO();
        ipaResponseKO.setCode("Code");
        ipaResponseKO.setDetail(null);

        IpaResponseKO ipaResponseKO1 = new IpaResponseKO();
        ipaResponseKO1.setCode("Code");
        ipaResponseKO1.setDetail("Detail");
        assertNotEquals(ipaResponseKO, ipaResponseKO1);
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link IpaResponseKO#equals(Object)}
     *   <li>{@link IpaResponseKO#hashCode()}
     * </ul>
     */
    @Test
    void testEquals9() {
        IpaResponseKO ipaResponseKO = new IpaResponseKO();
        ipaResponseKO.setCode(null);
        ipaResponseKO.setDetail("Detail");

        IpaResponseKO ipaResponseKO1 = new IpaResponseKO();
        ipaResponseKO1.setCode(null);
        ipaResponseKO1.setDetail("Detail");
        assertEquals(ipaResponseKO, ipaResponseKO1);
        int expectedHashCodeResult = ipaResponseKO.hashCode();
        assertEquals(expectedHashCodeResult, ipaResponseKO1.hashCode());
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link IpaResponseKO#equals(Object)}
     *   <li>{@link IpaResponseKO#hashCode()}
     * </ul>
     */
    @Test
    void testEquals10() {
        IpaResponseKO ipaResponseKO = new IpaResponseKO();
        ipaResponseKO.setCode("Code");
        ipaResponseKO.setDetail(null);

        IpaResponseKO ipaResponseKO1 = new IpaResponseKO();
        ipaResponseKO1.setCode("Code");
        ipaResponseKO1.setDetail(null);
        assertEquals(ipaResponseKO, ipaResponseKO1);
        int expectedHashCodeResult = ipaResponseKO.hashCode();
        assertEquals(expectedHashCodeResult, ipaResponseKO1.hashCode());
    }
}

