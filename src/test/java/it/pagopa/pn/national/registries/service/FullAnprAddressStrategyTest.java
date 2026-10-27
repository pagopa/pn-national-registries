package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.AddressModeEnum;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoCivicoInterno;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoNumeroCivico;
import org.junit.jupiter.api.Test;

import static it.pagopa.pn.national.registries.service.AnprAddressTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FullAnprAddressStrategyTest {

    private final FullAnprAddressStrategy strategy = new FullAnprAddressStrategy();

    @Test
    void strategyName() {
        assertEquals(AddressModeEnum.FULL.name(), strategy.getStrategyName());
    }

    @Test
    void createAddressWithNumberLetterAndExponent() {
        TipoNumeroCivico civico = civico("10", "A");
        civico.setEsponente1("bis");
        assertEquals("VIA ROMA 10/A bis", strategy.createAddress(indirizzo("VIA", "ROMA", civico)));
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
    void createAddressMetricHasPrecedenceOverHouseNumberAndSnc() {
        TipoNumeroCivico civico = civicoMetrico("5", "10");
        civico.setLettera("A");
        civico.setProgSNC("1");
        civico.setEsponente1("bis");
        assertEquals("VIA ROMA KM 5", strategy.createAddress(indirizzo("VIA", "ROMA", civico)));
    }

    @Test
    void createAddressInvalidMetricFallsBackToHouseNumberAndExponent() {
        TipoNumeroCivico civico = civicoMetrico("abc", "10");
        civico.setLettera("A");
        civico.setEsponente1("bis");
        assertEquals("VIA ROMA 10/A bis", strategy.createAddress(indirizzo("VIA", "ROMA", civico)));
    }

    @Test
    void createAddressSncKeepsHouseNumber() {
        TipoNumeroCivico civico = civicoSnc("1", "10");
        civico.setLettera("A");
        assertEquals("VIA ROMA 10/A SNC", strategy.createAddress(indirizzo("VIA", "ROMA", civico)));
    }

    @Test
    void createAddressInvalidSncFallsBackToHouseNumber() {
        TipoNumeroCivico civico = civicoSnc("0", "10");
        civico.setEsponente1("bis");
        assertEquals("VIA ROMA 10 bis", strategy.createAddress(indirizzo("VIA", "ROMA", civico)));
    }

    @Test
    void createAddressDetailWithoutNumeroCivicoReturnsEmpty() {
        assertEquals("", strategy.createAddressDetail(new TipoIndirizzo()));
    }

    @Test
    void createAddressDetailWithoutColorAndCivicoInternoReturnsEmpty() {
        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(new TipoNumeroCivico());
        assertEquals("", strategy.createAddressDetail(indirizzo));
    }

    @Test
    void createAddressDetailWithValidColors() {
        assertEquals("ROSSO", strategy.createAddressDetail(indirizzoConDettaglio("1", null)));
        assertEquals("NERO", strategy.createAddressDetail(indirizzoConDettaglio("2", null)));
        assertEquals("BLU", strategy.createAddressDetail(indirizzoConDettaglio("3", null)));
        assertEquals("Res.", strategy.createAddressDetail(indirizzoConDettaglio("4", null)));
        assertEquals("Non res.", strategy.createAddressDetail(indirizzoConDettaglio("5", null)));
    }

    @Test
    void createAddressDetailWithInvalidColorIsEmpty() {
        assertEquals("", strategy.createAddressDetail(indirizzoConDettaglio("9", null)));
    }

    @Test
    void createAddressDetailWithAllInternalFields() {
        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setCorte("1");
        interno.setScala("2");
        interno.setScalaEsterna("PAL");
        interno.setInterno1("3");
        interno.setEspInterno1("A");
        interno.setInterno2("4");
        interno.setEspInterno2("B");
        interno.setIsolato("5");
        assertEquals("ROSSO Corte 1 Scala 2 Scala est. PAL A B",
                strategy.createAddressDetail(indirizzoConDettaglio("1", interno)));
    }

    @Test
    void createAddressDetailWithOnlyFirstInternal() {
        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setInterno1("3");
        interno.setEspInterno1("A");
        assertEquals("Interno 3 A", strategy.createAddressDetail(indirizzoConDettaglio(null, interno)));
    }

    @Test
    void createAddressDetailWithOnlySecondInternal() {
        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setInterno2("4");
        interno.setEspInterno2("B");
        assertEquals("Interno 4 B", strategy.createAddressDetail(indirizzoConDettaglio(null, interno)));
    }

    @Test
    void createAddressDetailIgnoresBlankInternalFields() {
        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setCorte(" ");
        interno.setScala("");
        interno.setScalaEsterna(" ");
        interno.setInterno1(" ");
        interno.setInterno2("");
        interno.setIsolato(" ");
        assertEquals("", strategy.createAddressDetail(indirizzoConDettaglio(null, interno)));
    }
}