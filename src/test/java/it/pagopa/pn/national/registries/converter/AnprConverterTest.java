package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.config.AddressModeEnum;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ResidentialAddressDto;
import it.pagopa.pn.national.registries.service.FullAnprAddressStrategy;
import it.pagopa.pn.national.registries.service.OldAnprAddressStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AnprConverterTest {

    private AnprConverter anprConverter;

    private NationalRegistriesConfig configs;

    @BeforeEach
    void setUp() {
        Map<String, it.pagopa.pn.national.registries.service.AnprAddressStrategy> strategies = Map.of(
                AddressModeEnum.OLD.name(), new OldAnprAddressStrategy(),
                AddressModeEnum.FULL.name(), new FullAnprAddressStrategy(),
                AddressModeEnum.MINIMAL.name(), new OldAnprAddressStrategy()
        );

        configs = new NationalRegistriesConfig();
        configs.setAddressCompositionMode(AddressModeEnum.FULL.name());

        anprConverter = new AnprConverter(strategies, configs);
    }

    /**
     * Method under test: {@link AnprConverter#convertToGetAddressANPROK(RispostaE002OK, String)}
     */
    @Test
    void testconvertToGetAddressANPROK() {
        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(new ArrayList<>());

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaAnomalie(new ArrayList<>());
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);
        assertNull(anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses());
    }

    @Test
    void testconvertToGetAddressANPROK5() {
        RispostaE002OK responseE002OK = new RispostaE002OK();
        assertTrue(anprConverter.convertToGetAddressANPROK(responseE002OK, "Cf").getResidentialAddresses().isEmpty());
    }

    /**
     * Method under test: {@link AnprConverter#convertToGetAddressANPROK(RispostaE002OK, String)}
     */
    @Test
    void testconvertToGetAddressANPROK11() {
        TipoCodiceFiscale tipoCodiceFiscale = new TipoCodiceFiscale();
        tipoCodiceFiscale.setCodFiscale("Cf");
        tipoCodiceFiscale.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscale.setValiditaCF("Validita CF");

        TipoGeneralita tipoGeneralita = new TipoGeneralita();
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscale);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        List<TipoResidenza> tipoResidenzaList = new ArrayList<>();
        TipoResidenza tipoResidenza = new TipoResidenza();

        tipoResidenza.setPresso("presso");
        tipoResidenza.setTipoIndirizzo("4");

        tipoResidenzaList.add(tipoResidenza);
        tipoDatiSoggettiEnte.setResidenza(tipoResidenzaList);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaAnomalie(new ArrayList<>());
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);
        assertNotNull(anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses());
    }


    /**
     * Method under test: {@link AnprConverter#convertToGetAddressANPROK(RispostaE002OK, String)}
     */
    @Test
    void testconvertToGetAddressANPROK2() {
        TipoCodiceFiscale tipoCodiceFiscale = new TipoCodiceFiscale();
        tipoCodiceFiscale.setCodFiscale("Cf");
        tipoCodiceFiscale.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscale.setValiditaCF("Validita CF");

        TipoGeneralita tipoGeneralita = new TipoGeneralita();
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscale);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        List<TipoResidenza> tipoResidenzaList = new ArrayList<>();
        TipoResidenza tipoResidenza = new TipoResidenza();

        TipoIndirizzo tipoIndirizzo = new TipoIndirizzo();
        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setNumero("70");
        tipoNumeroCivico.setLettera("A");
        TipoCivicoInterno tipoCivicoInterno = new TipoCivicoInterno();
        tipoCivicoInterno.setScala("42");
        tipoNumeroCivico.setCivicoInterno(tipoCivicoInterno);
        tipoIndirizzo.setNumeroCivico(tipoNumeroCivico);

        TipoToponimo tipoToponimo = new TipoToponimo();
        tipoToponimo.setSpecie("specie");
        tipoToponimo.setDenominazioneToponimo("denominazione Toponimo");
        tipoIndirizzo.setToponimo(tipoToponimo);
        tipoIndirizzo.setCap("00178");
        tipoIndirizzo.setFrazione("frazione");

        TipoComune tipoComune = new TipoComune();
        tipoComune.setNomeComune("nomeComune");
        tipoComune.setSiglaProvinciaIstat("RM");
        tipoIndirizzo.setComune(tipoComune);

        TipoLocalitaEstera1 tipoLocalitaEstera1 = new TipoLocalitaEstera1();
        TipoIndirizzoEstero tipoIndirizzoEstero = new TipoIndirizzoEstero();
        TipoDatoLocalitaEstera tipoDatoLocalitaEstera = new TipoDatoLocalitaEstera();
        tipoDatoLocalitaEstera.setDescrizioneLocalita("descrizione");
        tipoIndirizzoEstero.setLocalita(tipoDatoLocalitaEstera);

        tipoLocalitaEstera1.setIndirizzoEstero(tipoIndirizzoEstero);
        tipoResidenza.setLocalitaEstera(tipoLocalitaEstera1);

        tipoResidenza.setIndirizzo(tipoIndirizzo);
        tipoResidenza.setPresso("presso");
        tipoResidenza.setTipoIndirizzo("4");

        tipoResidenzaList.add(tipoResidenza);
        tipoDatiSoggettiEnte.setResidenza(tipoResidenzaList);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaAnomalie(new ArrayList<>());
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);
        assertNotNull(anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses());
    }

    @Test
    void testconvertToGetAddressANPROK6() {
        TipoCodiceFiscale tipoCodiceFiscale = new TipoCodiceFiscale();
        tipoCodiceFiscale.setCodFiscale("Cf");
        tipoCodiceFiscale.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscale.setValiditaCF("Validita CF");

        TipoGeneralita tipoGeneralita = new TipoGeneralita();
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscale);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);
        assertNull(anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses());
    }

    @Test
    void testconvertToGetAddressANPROK7() {
        TipoGeneralita tipoGeneralita = new TipoGeneralita();

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);
        assertNull(anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses());
    }

    @Test
    void testconvertToGetAddressANPROK8() {
        TipoCodiceFiscale tipoCodiceFiscale = new TipoCodiceFiscale();
        tipoCodiceFiscale.setCodFiscale("Cf");
        tipoCodiceFiscale.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscale.setValiditaCF("Validita CF");

        TipoGeneralita tipoGeneralita = new TipoGeneralita();
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscale);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);
        assertNull(anprConverter.convertToGetAddressANPROK(rispostaE002OK, "cf2").getResidentialAddresses());
    }

    /**
     * Method under test: {@link AnprConverter#convertToGetAddressANPROK(RispostaE002OK, String)}
     */
    @Test
    void testconvertToGetAddressANPROK3() {
        TipoCodiceFiscale tipoCodiceFiscale = new TipoCodiceFiscale();
        tipoCodiceFiscale.setCodFiscale("Cf");
        tipoCodiceFiscale.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscale.setValiditaCF("Validita CF");

        TipoGeneralita tipoGeneralita = new TipoGeneralita();
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscale);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        List<TipoResidenza> tipoResidenzaList = new ArrayList<>();
        TipoResidenza tipoResidenza = new TipoResidenza();

        TipoIndirizzo tipoIndirizzo = new TipoIndirizzo();
        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setNumero("70");
        tipoNumeroCivico.setLettera("A");
        tipoIndirizzo.setNumeroCivico(tipoNumeroCivico);

        TipoComune tipoComune = new TipoComune();
        tipoComune.setNomeComune("nomeComune");
        tipoComune.setSiglaProvinciaIstat("RM");
        tipoIndirizzo.setComune(tipoComune);

        TipoLocalitaEstera1 tipoLocalitaEstera1 = new TipoLocalitaEstera1();
        TipoIndirizzoEstero tipoIndirizzoEstero = new TipoIndirizzoEstero();
        TipoDatoLocalitaEstera tipoDatoLocalitaEstera = new TipoDatoLocalitaEstera();
        tipoDatoLocalitaEstera.setDescrizioneLocalita("descrizione");
        tipoIndirizzoEstero.setLocalita(tipoDatoLocalitaEstera);

        tipoLocalitaEstera1.setIndirizzoEstero(tipoIndirizzoEstero);
        tipoResidenza.setLocalitaEstera(tipoLocalitaEstera1);

        tipoResidenza.setIndirizzo(tipoIndirizzo);
        tipoResidenza.setPresso("presso");
        tipoResidenza.setTipoIndirizzo("4");

        tipoResidenzaList.add(tipoResidenza);
        tipoDatiSoggettiEnte.setResidenza(tipoResidenzaList);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaAnomalie(new ArrayList<>());
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);
        assertNotNull(anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses());
    }

    @Test
    void testconvertToGetAddressANPROK4() {
        TipoCodiceFiscale tipoCodiceFiscale = new TipoCodiceFiscale();
        tipoCodiceFiscale.setCodFiscale("Cf");
        tipoCodiceFiscale.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscale.setValiditaCF("Validita CF");

        TipoGeneralita tipoGeneralita = new TipoGeneralita();
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscale);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        List<TipoResidenza> tipoResidenzaList = new ArrayList<>();
        TipoResidenza tipoResidenza = new TipoResidenza();

        TipoToponimo tipoToponimo= new TipoToponimo();
        tipoToponimo.setDenominazioneToponimo("denToponimo");
        tipoToponimo.setSpecie("specie");
        tipoToponimo.setCodToponimo("codToponimo");
        tipoToponimo.setSpecieFonte("specieFonte");
        tipoToponimo.setToponimoFonte("toponimoFonte");

        TipoIndirizzo tipoIndirizzo = new TipoIndirizzo();
        tipoIndirizzo.setToponimo(tipoToponimo);
        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setNumero("70");
        tipoNumeroCivico.setLettera("1");
        tipoIndirizzo.setNumeroCivico(tipoNumeroCivico);

        TipoComune tipoComune = new TipoComune();
        tipoComune.setNomeComune("nomeComune");
        tipoComune.setSiglaProvinciaIstat("RM");
        tipoIndirizzo.setComune(tipoComune);

        TipoLocalitaEstera1 tipoLocalitaEstera1 = new TipoLocalitaEstera1();
        TipoIndirizzoEstero tipoIndirizzoEstero = new TipoIndirizzoEstero();
        TipoDatoLocalitaEstera tipoDatoLocalitaEstera = new TipoDatoLocalitaEstera();
        tipoDatoLocalitaEstera.setDescrizioneLocalita("descrizione");
        tipoIndirizzoEstero.setLocalita(tipoDatoLocalitaEstera);

        tipoLocalitaEstera1.setIndirizzoEstero(tipoIndirizzoEstero);
        tipoResidenza.setLocalitaEstera(tipoLocalitaEstera1);

        tipoResidenza.setIndirizzo(tipoIndirizzo);
        tipoResidenza.setPresso("presso");
        tipoResidenza.setTipoIndirizzo("4");

        tipoResidenzaList.add(tipoResidenza);
        tipoDatiSoggettiEnte.setResidenza(tipoResidenzaList);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaAnomalie(new ArrayList<>());
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);

        List<ResidentialAddressDto> residentialAddresses = anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses();
        assertEquals(1, residentialAddresses.size());
        assertTrue(residentialAddresses.get(0).getAddress().contains("70/1"));
    }

    @Test
    void testconvertToGetAddressANPROK12() {
        TipoCodiceFiscale tipoCodiceFiscale = new TipoCodiceFiscale();
        tipoCodiceFiscale.setCodFiscale("Cf");
        tipoCodiceFiscale.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscale.setValiditaCF("Validita CF");

        TipoGeneralita tipoGeneralita = new TipoGeneralita();
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscale);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        List<TipoResidenza> tipoResidenzaList = new ArrayList<>();
        TipoResidenza tipoResidenza = new TipoResidenza();

        TipoToponimo tipoToponimo= new TipoToponimo();
        tipoToponimo.setDenominazioneToponimo("denToponimo");
        tipoToponimo.setSpecie("specie");
        tipoToponimo.setCodToponimo("codToponimo");
        tipoToponimo.setSpecieFonte("specieFonte");
        tipoToponimo.setToponimoFonte("toponimoFonte");

        TipoIndirizzo tipoIndirizzo = new TipoIndirizzo();
        tipoIndirizzo.setToponimo(tipoToponimo);
        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setNumero("70");
        tipoNumeroCivico.setLettera("1L");
        tipoIndirizzo.setNumeroCivico(tipoNumeroCivico);

        TipoComune tipoComune = new TipoComune();
        tipoComune.setNomeComune("nomeComune");
        tipoComune.setSiglaProvinciaIstat("RM");
        tipoIndirizzo.setComune(tipoComune);

        TipoLocalitaEstera1 tipoLocalitaEstera1 = new TipoLocalitaEstera1();
        TipoIndirizzoEstero tipoIndirizzoEstero = new TipoIndirizzoEstero();
        TipoDatoLocalitaEstera tipoDatoLocalitaEstera = new TipoDatoLocalitaEstera();
        tipoDatoLocalitaEstera.setDescrizioneLocalita("descrizione");
        tipoIndirizzoEstero.setLocalita(tipoDatoLocalitaEstera);

        tipoLocalitaEstera1.setIndirizzoEstero(tipoIndirizzoEstero);
        tipoResidenza.setLocalitaEstera(tipoLocalitaEstera1);

        tipoResidenza.setIndirizzo(tipoIndirizzo);
        tipoResidenza.setPresso("presso");
        tipoResidenza.setTipoIndirizzo("4");

        tipoResidenzaList.add(tipoResidenza);
        tipoDatiSoggettiEnte.setResidenza(tipoResidenzaList);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaAnomalie(new ArrayList<>());
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);

        List<ResidentialAddressDto> residentialAddresses = anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses();
        assertEquals(1, residentialAddresses.size());
        assertTrue(residentialAddresses.get(0).getAddress().contains("70/1L"));
    }

    @Test
    void testconvertToGetAddressANPROK16() {
        TipoCodiceFiscale tipoCodiceFiscale = new TipoCodiceFiscale();
        tipoCodiceFiscale.setCodFiscale("Cf");
        tipoCodiceFiscale.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscale.setValiditaCF("Validita CF");

        TipoGeneralita tipoGeneralita = new TipoGeneralita();
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscale);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        List<TipoResidenza> tipoResidenzaList = new ArrayList<>();
        TipoResidenza tipoResidenza = new TipoResidenza();

        TipoToponimo tipoToponimo= new TipoToponimo();
        tipoToponimo.setDenominazioneToponimo("denToponimo");
        tipoToponimo.setSpecie("specie");
        tipoToponimo.setCodToponimo("codToponimo");
        tipoToponimo.setSpecieFonte("specieFonte");
        tipoToponimo.setToponimoFonte("toponimoFonte");

        TipoIndirizzo tipoIndirizzo = new TipoIndirizzo();
        tipoIndirizzo.setToponimo(tipoToponimo);
        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setLettera("A");
        tipoIndirizzo.setNumeroCivico(tipoNumeroCivico);

        TipoComune tipoComune = new TipoComune();
        tipoComune.setNomeComune("nomeComune");
        tipoComune.setSiglaProvinciaIstat("RM");
        tipoIndirizzo.setComune(tipoComune);

        TipoLocalitaEstera1 tipoLocalitaEstera1 = new TipoLocalitaEstera1();
        TipoIndirizzoEstero tipoIndirizzoEstero = new TipoIndirizzoEstero();
        TipoDatoLocalitaEstera tipoDatoLocalitaEstera = new TipoDatoLocalitaEstera();
        tipoDatoLocalitaEstera.setDescrizioneLocalita("descrizione");
        tipoIndirizzoEstero.setLocalita(tipoDatoLocalitaEstera);

        tipoLocalitaEstera1.setIndirizzoEstero(tipoIndirizzoEstero);
        tipoResidenza.setLocalitaEstera(tipoLocalitaEstera1);

        tipoResidenza.setIndirizzo(tipoIndirizzo);
        tipoResidenza.setPresso("presso");
        tipoResidenza.setTipoIndirizzo("4");

        tipoResidenzaList.add(tipoResidenza);
        tipoDatiSoggettiEnte.setResidenza(tipoResidenzaList);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaAnomalie(new ArrayList<>());
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);

        List<ResidentialAddressDto> residentialAddresses = anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses();
        assertEquals(1, residentialAddresses.size());
        assertTrue(residentialAddresses.get(0).getAddress().contains("A"));
    }

    @Test
    void testconvertToGetAddressANPROK15() {
        TipoCodiceFiscale tipoCodiceFiscale = new TipoCodiceFiscale();
        tipoCodiceFiscale.setCodFiscale("Cf");
        tipoCodiceFiscale.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscale.setValiditaCF("Validita CF");

        TipoGeneralita tipoGeneralita = new TipoGeneralita();
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscale);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        List<TipoResidenza> tipoResidenzaList = new ArrayList<>();
        TipoResidenza tipoResidenza = new TipoResidenza();

        TipoToponimo tipoToponimo= new TipoToponimo();
        tipoToponimo.setDenominazioneToponimo("denToponimo");
        tipoToponimo.setSpecie("specie");
        tipoToponimo.setCodToponimo("codToponimo");
        tipoToponimo.setSpecieFonte("specieFonte");
        tipoToponimo.setToponimoFonte("toponimoFonte");

        TipoIndirizzo tipoIndirizzo = new TipoIndirizzo();
        tipoIndirizzo.setToponimo(tipoToponimo);
        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setNumero("70");
        tipoNumeroCivico.setLettera("A");
        tipoIndirizzo.setNumeroCivico(tipoNumeroCivico);

        TipoComune tipoComune = new TipoComune();
        tipoComune.setNomeComune("nomeComune");
        tipoComune.setSiglaProvinciaIstat("RM");
        tipoIndirizzo.setComune(tipoComune);

        TipoLocalitaEstera1 tipoLocalitaEstera1 = new TipoLocalitaEstera1();
        TipoIndirizzoEstero tipoIndirizzoEstero = new TipoIndirizzoEstero();
        TipoDatoLocalitaEstera tipoDatoLocalitaEstera = new TipoDatoLocalitaEstera();
        tipoDatoLocalitaEstera.setDescrizioneLocalita("descrizione");
        tipoIndirizzoEstero.setLocalita(tipoDatoLocalitaEstera);

        tipoLocalitaEstera1.setIndirizzoEstero(tipoIndirizzoEstero);
        tipoResidenza.setLocalitaEstera(tipoLocalitaEstera1);

        tipoResidenza.setIndirizzo(tipoIndirizzo);
        tipoResidenza.setPresso("presso");
        tipoResidenza.setTipoIndirizzo("4");

        tipoResidenzaList.add(tipoResidenza);
        tipoDatiSoggettiEnte.setResidenza(tipoResidenzaList);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaAnomalie(new ArrayList<>());
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);

        List<ResidentialAddressDto> residentialAddresses = anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses();
        assertEquals(1, residentialAddresses.size());
        assertTrue(residentialAddresses.get(0).getAddress().contains("70/A"));
    }

    @Test
    void testconvertToGetAddressANPROK9() {
        TipoCodiceFiscale tipoCodiceFiscale = new TipoCodiceFiscale();
        tipoCodiceFiscale.setCodFiscale("Cf");
        tipoCodiceFiscale.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscale.setValiditaCF("Validita CF");

        TipoGeneralita tipoGeneralita = new TipoGeneralita();
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscale);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        List<TipoResidenza> tipoResidenzaList = new ArrayList<>();
        TipoResidenza tipoResidenza = new TipoResidenza();

        TipoLocalitaEstera1 tipoLocalitaEstera1 = new TipoLocalitaEstera1();
        TipoIndirizzoEstero tipoIndirizzoEstero = new TipoIndirizzoEstero();
        TipoDatoLocalitaEstera tipoDatoLocalitaEstera = new TipoDatoLocalitaEstera();
        tipoDatoLocalitaEstera.setDescrizioneLocalita("descrizione");
        tipoIndirizzoEstero.setLocalita(tipoDatoLocalitaEstera);

        tipoLocalitaEstera1.setIndirizzoEstero(tipoIndirizzoEstero);
        tipoResidenza.setLocalitaEstera(tipoLocalitaEstera1);

        tipoResidenza.setPresso("presso");
        tipoResidenza.setTipoIndirizzo("4");

        tipoResidenzaList.add(tipoResidenza);
        tipoDatiSoggettiEnte.setResidenza(tipoResidenzaList);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaAnomalie(new ArrayList<>());
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);
        assertNotNull(anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses());
    }

    @Test
    void testconvertToGetAddressANPROK10() {
        TipoCodiceFiscale tipoCodiceFiscale = new TipoCodiceFiscale();
        tipoCodiceFiscale.setCodFiscale("Cf");
        tipoCodiceFiscale.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscale.setValiditaCF("Validita CF");

        TipoGeneralita tipoGeneralita = new TipoGeneralita();
        tipoGeneralita.setCodiceFiscale(tipoCodiceFiscale);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte.setGeneralita(tipoGeneralita);

        List<TipoResidenza> tipoResidenzaList = new ArrayList<>();
        TipoResidenza tipoResidenza = new TipoResidenza();

        TipoLocalitaEstera1 tipoLocalitaEstera1 = new TipoLocalitaEstera1();
        TipoIndirizzoEstero tipoIndirizzoEstero = new TipoIndirizzoEstero();
        TipoDatoLocalitaEstera tipoDatoLocalitaEstera = new TipoDatoLocalitaEstera();
        tipoDatoLocalitaEstera.setDescrizioneLocalita("descrizione");
        tipoIndirizzoEstero.setLocalita(tipoDatoLocalitaEstera);

        tipoLocalitaEstera1.setIndirizzoEstero(tipoIndirizzoEstero);
        tipoResidenza.setLocalitaEstera(tipoLocalitaEstera1);

        TipoToponimoEstero tipoToponimoEstero = new TipoToponimoEstero();
        tipoToponimoEstero.setNumeroCivico("34");
        tipoToponimoEstero.setDenominazione("via");
        tipoIndirizzoEstero.setToponimo(tipoToponimoEstero);

        tipoLocalitaEstera1.setIndirizzoEstero(tipoIndirizzoEstero);

        tipoResidenza.setPresso("presso");
        tipoResidenza.setTipoIndirizzo("4");

        tipoResidenzaList.add(tipoResidenza);
        tipoDatiSoggettiEnte.setResidenza(tipoResidenzaList);

        ArrayList<TipoDatiSoggettiEnte> tipoDatiSoggettiEnteList = new ArrayList<>();
        tipoDatiSoggettiEnteList.add(tipoDatiSoggettiEnte);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(tipoDatiSoggettiEnteList);

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaAnomalie(new ArrayList<>());
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);
        assertNotNull(anprConverter.convertToGetAddressANPROK(rispostaE002OK, "Cf").getResidentialAddresses());
    }

    @Test
    void testConvertConFiltroPerDataDecorrenza() {
        TipoResidenza tipoResidenza1 = new TipoResidenza();
        tipoResidenza1.setTipoIndirizzo("t1");
        tipoResidenza1.setDataDecorrenzaResidenza("2022-11-01");
        TipoResidenza tipoResidenza2 = new TipoResidenza();
        tipoResidenza2.setTipoIndirizzo("t2");
        tipoResidenza2.setDataDecorrenzaResidenza("2022-12-01");
        TipoResidenza tipoResidenza3 = new TipoResidenza();
        tipoResidenza3.setDataDecorrenzaResidenza("");
        tipoResidenza3.setTipoIndirizzo("t3");
        TipoResidenza tipoResidenza4 = new TipoResidenza();
        tipoResidenza4.setTipoIndirizzo("t4");
        // mi aspetto che venga selezionata la residence_2 che contiene la data di decorrenza più recente

        TipoCodiceFiscale tipoCodiceFiscale1 = new TipoCodiceFiscale();
        tipoCodiceFiscale1.setCodFiscale("COD_FISCALE_1");

        TipoGeneralita tipoGeneralita1 = new TipoGeneralita();
        tipoGeneralita1.setCodiceFiscale(tipoCodiceFiscale1);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte1 = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte1.setResidenza(List.of(tipoResidenza1, tipoResidenza2, tipoResidenza3, tipoResidenza4));
        tipoDatiSoggettiEnte1.setGeneralita(tipoGeneralita1);

        TipoCodiceFiscale tipoCodiceFiscale2 = new TipoCodiceFiscale();
        tipoCodiceFiscale2.setCodFiscale("COD_FISCALE_2");

        TipoGeneralita tipoGeneralita2 = new TipoGeneralita();
        tipoGeneralita2.setCodiceFiscale(tipoCodiceFiscale2);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte2 = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte2.setGeneralita(tipoGeneralita2);
        // mi aspetto che questo subject venga scartato perché il CF non combacia

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(List.of(tipoDatiSoggettiEnte1, tipoDatiSoggettiEnte2));

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);

        GetAddressANPROKDto response = anprConverter.convertToGetAddressANPROK(rispostaE002OK, "COD_FISCALE_1");
        assertNotNull(response);
        assertNotNull(response.getResidentialAddresses());
        assertEquals(1, response.getResidentialAddresses().size());
        assertEquals("t2", response.getResidentialAddresses().get(0).getDescription());
    }

    @Test
    void testConvertConDatiAddressDetail() {
        TipoCivicoInterno tipoCivicoInterno = new TipoCivicoInterno();
        tipoCivicoInterno.setCorte("2");
        tipoCivicoInterno.setIsolato("8");
        tipoCivicoInterno.setScala("9");
        tipoCivicoInterno.setScalaEsterna("PAL 8C");

        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setColore("1");
        tipoNumeroCivico.setCivicoInterno(tipoCivicoInterno);

        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(tipoNumeroCivico);

        TipoResidenza tipoResidenza1 = new TipoResidenza();
        tipoResidenza1.setTipoIndirizzo("t1");
        tipoResidenza1.setDataDecorrenzaResidenza("2022-11-01");
        tipoResidenza1.setIndirizzo(indirizzo);
        // mi aspetto che venga selezionata la residence_2 che contiene la data di decorrenza più recente

        TipoCodiceFiscale tipoCodiceFiscale1 = new TipoCodiceFiscale();
        tipoCodiceFiscale1.setCodFiscale("COD_FISCALE_1");

        TipoGeneralita tipoGeneralita1 = new TipoGeneralita();
        tipoGeneralita1.setCodiceFiscale(tipoCodiceFiscale1);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte1 = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte1.setResidenza(List.of(tipoResidenza1));
        tipoDatiSoggettiEnte1.setGeneralita(tipoGeneralita1);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(List.of(tipoDatiSoggettiEnte1));

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);

        GetAddressANPROKDto response = anprConverter.convertToGetAddressANPROK(rispostaE002OK, "COD_FISCALE_1");
        assertNotNull(response);
        assertNotNull(response.getResidentialAddresses());
        assertNotNull(response.getResidentialAddresses().getFirst().getAddressDetail());
        assertTrue(response.getResidentialAddresses().getFirst().getAddressDetail().contains("R"));
        assertTrue(response.getResidentialAddresses().getFirst().getAddressDetail().contains("2"));
        assertTrue(response.getResidentialAddresses().getFirst().getAddressDetail().contains("9"));
        assertTrue(response.getResidentialAddresses().getFirst().getAddressDetail().contains("PAL 8C"));
        assertTrue(response.getResidentialAddresses().getFirst().getAddressDetail().contains("8"));
    }

    @Test
    void testConvertConDatiAddressDetailInterni() {
        TipoCivicoInterno tipoCivicoInterno = new TipoCivicoInterno();
        tipoCivicoInterno.setInterno1("1");
        tipoCivicoInterno.setEspInterno1("A");
        tipoCivicoInterno.setInterno2("5");
        tipoCivicoInterno.setEspInterno2("B");

        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setColore("1");
        tipoNumeroCivico.setCivicoInterno(tipoCivicoInterno);

        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(tipoNumeroCivico);

        TipoResidenza tipoResidenza1 = new TipoResidenza();
        tipoResidenza1.setTipoIndirizzo("t1");
        tipoResidenza1.setDataDecorrenzaResidenza("2022-11-01");
        tipoResidenza1.setIndirizzo(indirizzo);
        // mi aspetto che venga selezionata la residence_2 che contiene la data di decorrenza più recente

        TipoCodiceFiscale tipoCodiceFiscale1 = new TipoCodiceFiscale();
        tipoCodiceFiscale1.setCodFiscale("COD_FISCALE_1");

        TipoGeneralita tipoGeneralita1 = new TipoGeneralita();
        tipoGeneralita1.setCodiceFiscale(tipoCodiceFiscale1);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte1 = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte1.setResidenza(List.of(tipoResidenza1));
        tipoDatiSoggettiEnte1.setGeneralita(tipoGeneralita1);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(List.of(tipoDatiSoggettiEnte1));

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);

        GetAddressANPROKDto response = anprConverter.convertToGetAddressANPROK(rispostaE002OK, "COD_FISCALE_1");
        assertNotNull(response);
        assertNotNull(response.getResidentialAddresses());
        assertNotNull(response.getResidentialAddresses().getFirst().getAddressDetail());
        assertTrue(response.getResidentialAddresses().getFirst().getAddressDetail().contains("1 A"));
        assertTrue(response.getResidentialAddresses().getFirst().getAddressDetail().contains("5 B"));
    }

    @Test
    void testCreateAddressDetailDoesNotExceed44Chars() {
        TipoCivicoInterno civicoInterno = new TipoCivicoInterno();
        civicoInterno.setScala("  SCALA-1234567890   ");
        civicoInterno.setCorte("CORTE-ABCDEFGHIJ");
        civicoInterno.setInterno1("INTERNO1-XYZ");
        civicoInterno.setEspInterno1("ESP1");
        civicoInterno.setInterno2("INTERNO2");
        civicoInterno.setEspInterno2("ESP2");
        civicoInterno.setScalaEsterna("SCALAESTERNA");
        civicoInterno.setSecondario("SECONDARIO");
        civicoInterno.setPiano("PIANO");
        civicoInterno.setNui("NUI");
        civicoInterno.setIsolato("ISOLATO");

        TipoNumeroCivico numeroCivico = new TipoNumeroCivico();
        numeroCivico.setColore("1");
        numeroCivico.setCivicoInterno(civicoInterno);

        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(numeroCivico);

        TipoResidenza residenza = new TipoResidenza();
        residenza.setTipoIndirizzo("t1");
        residenza.setDataDecorrenzaResidenza("2022-11-01");
        residenza.setIndirizzo(indirizzo);

        TipoCodiceFiscale cf = new TipoCodiceFiscale();
        cf.setCodFiscale("COD_FISCALE_1");

        TipoGeneralita generalita = new TipoGeneralita();
        generalita.setCodiceFiscale(cf);

        TipoDatiSoggettiEnte soggetto = new TipoDatiSoggettiEnte();
        soggetto.setGeneralita(generalita);
        soggetto.setResidenza(List.of(residenza));

        TipoListaSoggetti lista = new TipoListaSoggetti();
        lista.setDatiSoggetto(List.of(soggetto));

        RispostaE002OK risposta = new RispostaE002OK();
        risposta.setListaSoggetti(lista);

        // Act
        GetAddressANPROKDto out = anprConverter.convertToGetAddressANPROK(risposta, "COD_FISCALE_1");
        String detail = out.getResidentialAddresses().getFirst().getAddressDetail();

        // Assert
        assertNotNull(detail);
        assertTrue(detail.length() <= 44);
    }

    @Test
    void testConvertConDatiAddressDetailModeNotRecognised() {
        configs.setAddressCompositionMode("TEST");
        TipoCivicoInterno tipoCivicoInterno = new TipoCivicoInterno();
        tipoCivicoInterno.setCorte("2");
        tipoCivicoInterno.setIsolato("8");
        tipoCivicoInterno.setScala("9");
        tipoCivicoInterno.setScalaEsterna("PAL 8C");

        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setColore("1");
        tipoNumeroCivico.setCivicoInterno(tipoCivicoInterno);

        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(tipoNumeroCivico);

        TipoResidenza tipoResidenza1 = new TipoResidenza();
        tipoResidenza1.setTipoIndirizzo("t1");
        tipoResidenza1.setDataDecorrenzaResidenza("2022-11-01");
        tipoResidenza1.setIndirizzo(indirizzo);
        // mi aspetto che venga selezionata la residence_2 che contiene la data di decorrenza più recente

        TipoCodiceFiscale tipoCodiceFiscale1 = new TipoCodiceFiscale();
        tipoCodiceFiscale1.setCodFiscale("COD_FISCALE_1");

        TipoGeneralita tipoGeneralita1 = new TipoGeneralita();
        tipoGeneralita1.setCodiceFiscale(tipoCodiceFiscale1);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte1 = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte1.setResidenza(List.of(tipoResidenza1));
        tipoDatiSoggettiEnte1.setGeneralita(tipoGeneralita1);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(List.of(tipoDatiSoggettiEnte1));

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);

        GetAddressANPROKDto response = anprConverter.convertToGetAddressANPROK(rispostaE002OK, "COD_FISCALE_1");
        assertNotNull(response);
        assertNotNull(response.getResidentialAddresses());
        assertNotNull(response.getResidentialAddresses().getFirst().getAddressDetail());
        assertFalse(response.getResidentialAddresses().getFirst().getAddressDetail().contains("R"));
        assertFalse(response.getResidentialAddresses().getFirst().getAddressDetail().contains("2"));
        assertTrue(response.getResidentialAddresses().getFirst().getAddressDetail().contains("9"));
        assertFalse(response.getResidentialAddresses().getFirst().getAddressDetail().contains("PAL 8C"));
        assertFalse(response.getResidentialAddresses().getFirst().getAddressDetail().contains("8"));
    }

    @Test
    void testConvertConCampiAggiuntivi() {
        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setEsponente1("600");
        tipoNumeroCivico.setNumero("22");
        tipoNumeroCivico.setLettera("B");

        TipoToponimo tipoToponimo = new TipoToponimo();
        tipoToponimo.setSpecie("specie");
        tipoToponimo.setDenominazioneToponimo("denominazione Toponimo");

        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(tipoNumeroCivico);
        indirizzo.setToponimo(tipoToponimo);

        TipoResidenza tipoResidenza1 = new TipoResidenza();
        tipoResidenza1.setTipoIndirizzo("t1");
        tipoResidenza1.setDataDecorrenzaResidenza("2022-11-01");
        tipoResidenza1.setIndirizzo(indirizzo);
        // mi aspetto che venga selezionata la residence_2 che contiene la data di decorrenza più recente

        TipoCodiceFiscale tipoCodiceFiscale1 = new TipoCodiceFiscale();
        tipoCodiceFiscale1.setCodFiscale("COD_FISCALE_1");

        TipoGeneralita tipoGeneralita1 = new TipoGeneralita();
        tipoGeneralita1.setCodiceFiscale(tipoCodiceFiscale1);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte1 = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte1.setResidenza(List.of(tipoResidenza1));
        tipoDatiSoggettiEnte1.setGeneralita(tipoGeneralita1);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(List.of(tipoDatiSoggettiEnte1));

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);

        GetAddressANPROKDto response = anprConverter.convertToGetAddressANPROK(rispostaE002OK, "COD_FISCALE_1");
        assertNotNull(response);
        assertTrue(response.getResidentialAddresses().getFirst().getAddress().contains("600"));
        assertTrue(response.getResidentialAddresses().getFirst().getAddress().contains("22/B"));
    }

    @Test
    void testConvertConCampiAggiuntiviMetrico() {
        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setMetrico("100");
        tipoNumeroCivico.setNumero("22");
        tipoNumeroCivico.setLettera("B");

        TipoToponimo tipoToponimo = new TipoToponimo();
        tipoToponimo.setSpecie("specie");
        tipoToponimo.setDenominazioneToponimo("denominazione Toponimo");

        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(tipoNumeroCivico);
        indirizzo.setToponimo(tipoToponimo);

        TipoResidenza tipoResidenza1 = new TipoResidenza();
        tipoResidenza1.setTipoIndirizzo("t1");
        tipoResidenza1.setDataDecorrenzaResidenza("2022-11-01");
        tipoResidenza1.setIndirizzo(indirizzo);
        // mi aspetto che venga selezionata la residence_2 che contiene la data di decorrenza più recente

        TipoCodiceFiscale tipoCodiceFiscale1 = new TipoCodiceFiscale();
        tipoCodiceFiscale1.setCodFiscale("COD_FISCALE_1");

        TipoGeneralita tipoGeneralita1 = new TipoGeneralita();
        tipoGeneralita1.setCodiceFiscale(tipoCodiceFiscale1);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte1 = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte1.setResidenza(List.of(tipoResidenza1));
        tipoDatiSoggettiEnte1.setGeneralita(tipoGeneralita1);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(List.of(tipoDatiSoggettiEnte1));

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);

        GetAddressANPROKDto response = anprConverter.convertToGetAddressANPROK(rispostaE002OK, "COD_FISCALE_1");
        assertNotNull(response);
        assertTrue(response.getResidentialAddresses().getFirst().getAddress().contains("KM 100"));
        assertFalse(response.getResidentialAddresses().getFirst().getAddress().contains("22/B"));
    }

    @Test
    void testConvertConCampiAggiuntiviSNC() {
        TipoNumeroCivico tipoNumeroCivico = new TipoNumeroCivico();
        tipoNumeroCivico.setProgSNC("1");
        tipoNumeroCivico.setNumero("22");
        tipoNumeroCivico.setLettera("B");

        TipoToponimo tipoToponimo = new TipoToponimo();
        tipoToponimo.setSpecie("specie");
        tipoToponimo.setDenominazioneToponimo("denominazione Toponimo");

        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setNumeroCivico(tipoNumeroCivico);
        indirizzo.setToponimo(tipoToponimo);

        TipoResidenza tipoResidenza1 = new TipoResidenza();
        tipoResidenza1.setTipoIndirizzo("t1");
        tipoResidenza1.setDataDecorrenzaResidenza("2022-11-01");
        tipoResidenza1.setIndirizzo(indirizzo);
        // mi aspetto che venga selezionata la residence_2 che contiene la data di decorrenza più recente

        TipoCodiceFiscale tipoCodiceFiscale1 = new TipoCodiceFiscale();
        tipoCodiceFiscale1.setCodFiscale("COD_FISCALE_1");

        TipoGeneralita tipoGeneralita1 = new TipoGeneralita();
        tipoGeneralita1.setCodiceFiscale(tipoCodiceFiscale1);

        TipoDatiSoggettiEnte tipoDatiSoggettiEnte1 = new TipoDatiSoggettiEnte();
        tipoDatiSoggettiEnte1.setResidenza(List.of(tipoResidenza1));
        tipoDatiSoggettiEnte1.setGeneralita(tipoGeneralita1);

        TipoListaSoggetti tipoListaSoggetti = new TipoListaSoggetti();
        tipoListaSoggetti.setDatiSoggetto(List.of(tipoDatiSoggettiEnte1));

        RispostaE002OK rispostaE002OK = new RispostaE002OK();
        rispostaE002OK.setListaSoggetti(tipoListaSoggetti);

        GetAddressANPROKDto response = anprConverter.convertToGetAddressANPROK(rispostaE002OK, "COD_FISCALE_1");
        assertNotNull(response);
        assertTrue(response.getResidentialAddresses().getFirst().getAddress().contains("SNC"));
        assertTrue(response.getResidentialAddresses().getFirst().getAddress().contains("22/B"));
    }
}
