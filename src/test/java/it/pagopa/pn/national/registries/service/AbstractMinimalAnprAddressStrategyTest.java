package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import org.junit.jupiter.api.Test;

import static it.pagopa.pn.national.registries.service.AnprAddressTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Scenari comuni a MINIMAL e OLD_MINIMAL per la composizione dell'indirizzo (createAddress).
 */
abstract class AbstractMinimalAnprAddressStrategyTest {

    abstract AnprAddressStrategy strategy();

    @Test
    void createAddressWithNumberAndLetter() {
        assertEquals("VIA ROMA 10/A", strategy().createAddress(indirizzo("VIA", "ROMA", civico("10", "A"))));
    }

    @Test
    void createAddressWithNumberOnly() {
        assertEquals("VIA ROMA 10", strategy().createAddress(indirizzo("VIA", "ROMA", civico("10", null))));
    }

    @Test
    void createAddressWithoutSpecieAndDenominazione() {
        assertEquals("10", strategy().createAddress(indirizzo(null, null, civico("10", null))));
    }

    @Test
    void createAddressWithoutNumberAndLetter() {
        assertEquals("VIA ROMA", strategy().createAddress(indirizzo("VIA", "ROMA", civico(null, null))));
    }

    @Test
    void createAddressWithoutToponimoReturnsEmpty() {
        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(civico("10", "A"));
        assertEquals("", strategy().createAddress(indirizzo));
    }

    @Test
    void createAddressWithoutNumeroCivicoReturnsEmpty() {
        assertEquals("", strategy().createAddress(indirizzo("VIA", "ROMA", null)));
    }

    @Test
    void createAddressMetricWithoutNumber() {
        assertEquals("VIA ROMA KM 5", strategy().createAddress(indirizzo("VIA", "ROMA", civicoMetrico("5", null))));
    }

    @Test
    void createAddressMetricWithBlankNumberIgnoresNumber() {
        assertEquals("VIA ROMA KM 5", strategy().createAddress(indirizzo("VIA", "ROMA", civicoMetrico("5", " "))));
    }

    @Test
    void createAddressMetricWithNumberKeepsBothNumberAndKm() {
        assertEquals("VIA ROMA 10 KM 5", strategy().createAddress(indirizzo("VIA", "ROMA", civicoMetrico("5", "10"))));
    }

    @Test
    void createAddressMetricWithNumberAndLetter() {
        var civico = civicoMetrico("5", "10");
        civico.setLettera("B");
        assertEquals("VIA ROMA 10/B KM 5", strategy().createAddress(indirizzo("VIA", "ROMA", civico)));
    }

    @Test
    void createAddressMetricZeroFallsBackToHouseNumber() {
        assertEquals("VIA ROMA 10", strategy().createAddress(indirizzo("VIA", "ROMA", civicoMetrico("0", "10"))));
    }

    @Test
    void createAddressMetricNotNumericFallsBackToHouseNumber() {
        assertEquals("VIA ROMA 10", strategy().createAddress(indirizzo("VIA", "ROMA", civicoMetrico("abc", "10"))));
    }

    @Test
    void createAddressMetricBlankFallsBackToHouseNumber() {
        assertEquals("VIA ROMA 10", strategy().createAddress(indirizzo("VIA", "ROMA", civicoMetrico("  ", "10"))));
    }

    @Test
    void createAddressMetricHasPrecedenceOverSnc() {
        var civico = civicoMetrico("5", null);
        civico.setProgSNC("1");
        assertEquals("VIA ROMA KM 5", strategy().createAddress(indirizzo("VIA", "ROMA", civico)));
    }

    @Test
    void createAddressSncWithNumber() {
        assertEquals("VIA ROMA 10 SNC", strategy().createAddress(indirizzo("VIA", "ROMA", civicoSnc("1", "10"))));
    }

    @Test
    void createAddressSncWithoutNumber() {
        assertEquals("VIA ROMA SNC", strategy().createAddress(indirizzo("VIA", "ROMA", civicoSnc("1", null))));
    }

    @Test
    void createAddressSncZeroFallsBackToHouseNumber() {
        assertEquals("VIA ROMA 10", strategy().createAddress(indirizzo("VIA", "ROMA", civicoSnc("0", "10"))));
    }

    @Test
    void createAddressSncNotNumericFallsBackToHouseNumber() {
        assertEquals("VIA ROMA 10", strategy().createAddress(indirizzo("VIA", "ROMA", civicoSnc("x", "10"))));
    }

    @Test
    void createAddressIgnoresEsponente1() {
        var civico = civico("10", null);
        civico.setEsponente1("bis");
        assertEquals("VIA ROMA 10", strategy().createAddress(indirizzo("VIA", "ROMA", civico)));
    }

    @Test
    void createAddressSkipsTokenThatDoesNotFitMaxLength() {
        assertEquals("VIA 10", strategy().createAddress(indirizzo("VIA", "X".repeat(45), civico("10", null))));
    }
}

