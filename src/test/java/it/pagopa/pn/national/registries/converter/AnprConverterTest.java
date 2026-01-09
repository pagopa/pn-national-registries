package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ResidentialAddressDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AnprConverterTest {

    @InjectMocks
    private AnprConverter anprConverter;

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
}
