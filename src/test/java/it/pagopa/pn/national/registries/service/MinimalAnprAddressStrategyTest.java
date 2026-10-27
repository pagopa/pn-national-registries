package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.AddressModeEnum;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoCivicoInterno;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoNumeroCivico;
import org.junit.jupiter.api.Test;

import static it.pagopa.pn.national.registries.service.AnprAddressTestData.indirizzoConDettaglio;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MinimalAnprAddressStrategyTest extends AbstractMinimalAnprAddressStrategyTest {

    private final MinimalAnprAddressStrategy strategy = new MinimalAnprAddressStrategy();

    @Override
    AnprAddressStrategy strategy() {
        return strategy;
    }

    @Test
    void strategyName() {
        assertEquals(AddressModeEnum.MINIMAL.name(), strategy.getStrategyName());
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
    void createAddressDetailWithScalaAndScalaEsterna() {
        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setScala("2");
        interno.setScalaEsterna("PAL 8C");
        assertEquals("ROSSO Scala 2 Scala est. PAL 8C", strategy.createAddressDetail(indirizzoConDettaglio("1", interno)));
    }

    @Test
    void createAddressDetailWithBlankScalaIsIgnored() {
        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setScala(" ");
        interno.setScalaEsterna("");
        assertEquals("ROSSO", strategy.createAddressDetail(indirizzoConDettaglio("1", interno)));
    }

    @Test
    void createAddressDetailIgnoresCorteInterniAndIsolato() {
        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setCorte("1");
        interno.setInterno1("2");
        interno.setEspInterno1("A");
        interno.setInterno2("3");
        interno.setEspInterno2("B");
        interno.setIsolato("4");
        assertEquals("", strategy.createAddressDetail(indirizzoConDettaglio(null, interno)));
    }
}

