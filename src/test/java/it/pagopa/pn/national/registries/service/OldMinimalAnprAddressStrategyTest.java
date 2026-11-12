package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.config.AddressModeEnum;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoCivicoInterno;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoNumeroCivico;
import org.junit.jupiter.api.Test;

import static it.pagopa.pn.national.registries.service.AnprAddressTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OldMinimalAnprAddressStrategyTest extends AbstractMinimalAnprAddressStrategyTest {

    private final OldMinimalAnprAddressStrategy strategy = new OldMinimalAnprAddressStrategy();

    @Override
    AnprAddressStrategy strategy() {
        return strategy;
    }

    @Test
    void strategyName() {
        assertEquals(AddressModeEnum.OLD_MINIMAL.name(), strategy.getStrategyName());
    }

    @Test
    void createAddressIgnoresEsponente1() {
        var civico = civico("10", null);
        civico.setEsponente1("bis");
        assertEquals("VIA ROMA 10", strategy.createAddress(indirizzo("VIA", "ROMA", civico)));
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
    void createAddressDetailWithColorAndScala() {
        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setScala("2");
        interno.setScalaEsterna("PAL");
        assertEquals("ROSSO Scala 2 Scala est. PAL", strategy.createAddressDetail(indirizzoConDettaglio("1", interno)));
    }

    @Test
    void createAddressDetailWithBlankScalaIsIgnored() {
        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setScala(" ");
        interno.setScalaEsterna(" ");
        assertEquals("ROSSO", strategy.createAddressDetail(indirizzoConDettaglio("1", interno)));
    }

    @Test
    void createAddressDetailIncludesScalaEsternaAndIgnoresCorteInterniAndIsolato() {
        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setScalaEsterna("PAL");
        interno.setCorte("1");
        interno.setInterno1("2");
        interno.setInterno2("3");
        interno.setIsolato("4");
        assertEquals("Scala est. PAL", strategy.createAddressDetail(indirizzoConDettaglio(null, interno)));
    }
}
