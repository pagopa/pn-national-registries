package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.AddressModeEnum;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoCivicoInterno;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoNumeroCivico;
import org.junit.jupiter.api.Test;

import static it.pagopa.pn.national.registries.service.AnprAddressTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OldAnprAddressStrategyTest {

    private final OldAnprAddressStrategy strategy = new OldAnprAddressStrategy();

    @Test
    void strategyName() {
        assertEquals(AddressModeEnum.OLD.name(), strategy.getStrategyName());
    }

    @Test
    void createAddressWithNumberAndLetter() {
        assertEquals("VIA ROMA 10/A", strategy.createAddress(indirizzo("VIA", "ROMA", civico("10", "A"))));
    }

    @Test
    void createAddressWithNumberOnly() {
        assertEquals("VIA ROMA 10", strategy.createAddress(indirizzo("VIA", "ROMA", civico("10", null))));
    }

    @Test
    void createAddressWithLetterOnly() {
        assertEquals("VIA ROMA A", strategy.createAddress(indirizzo("VIA", "ROMA", civico(null, "A"))));
    }

    @Test
    void createAddressWithoutSpecie() {
        assertEquals(" ROMA 10", strategy.createAddress(indirizzo(null, "ROMA", civico("10", null))));
    }

    @Test
    void createAddressWithoutNumberAndLetter() {
        assertEquals("VIA ROMA ", strategy.createAddress(indirizzo("VIA", "ROMA", civico(null, null))));
    }

    @Test
    void createAddressIgnoresMetricAndSnc() {
        assertEquals("VIA ROMA 10", strategy.createAddress(indirizzo("VIA", "ROMA", civicoMetrico("5", "10"))));
        assertEquals("VIA ROMA 10", strategy.createAddress(indirizzo("VIA", "ROMA", civicoSnc("1", "10"))));
    }

    @Test
    void createAddressWithoutToponimoReturnsEmpty() {
        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(civico("10", "A"));
        assertEquals("", strategy.createAddress(indirizzo));
    }

    @Test
    void createAddressWithoutNumeroCivicoReturnsEmpty() {
        assertEquals("", strategy.createAddress(indirizzo("VIA", "ROMA", null)));
    }

    @Test
    void createAddressDetailWithoutNumeroCivicoReturnsEmpty() {
        assertEquals("", strategy.createAddressDetail(new TipoIndirizzo()));
    }

    @Test
    void createAddressDetailWithoutCivicoInternoReturnsEmpty() {
        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(new TipoNumeroCivico());
        assertEquals("", strategy.createAddressDetail(indirizzo));
    }

    @Test
    void createAddressDetailWithoutScalaReturnsEmpty() {
        assertEquals("", strategy.createAddressDetail(indirizzoConDettaglio("1", new TipoCivicoInterno())));
    }

    @Test
    void createAddressDetailReturnsOnlyScala() {
        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setScala("B");
        interno.setCorte("2");
        interno.setInterno1("3");
        assertEquals("B", strategy.createAddressDetail(indirizzoConDettaglio("1", interno)));
    }
}

