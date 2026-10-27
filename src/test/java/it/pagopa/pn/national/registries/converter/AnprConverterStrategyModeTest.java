package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.config.AddressModeEnum;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ResidentialAddressDto;
import it.pagopa.pn.national.registries.service.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test della selezione della strategia di composizione indirizzo in {@link AnprConverter}
 * in funzione della property pn.national-registries.address-composition-mode.
 */
class AnprConverterStrategyModeTest {

    private static final String CF = "CF_TEST";

    private static List<AnprAddressStrategy> allStrategies() {
        return List.of(
                new OldAnprAddressStrategy(),
                new FullAnprAddressStrategy(),
                new MinimalAnprAddressStrategy(),
                new OldMinimalAnprAddressStrategy()
        );
    }

    private static AnprConverter converterWithMode(String mode) {
        NationalRegistriesConfig configs = new NationalRegistriesConfig();
        configs.setAddressCompositionMode(mode);
        return new AnprConverter(allStrategies(), configs);
    }

    private static TipoIndirizzo buildIndirizzo() {
        TipoToponimo toponimo = new TipoToponimo();
        toponimo.setSpecie("VIA");
        toponimo.setDenominazioneToponimo("ROMA");

        TipoCivicoInterno interno = new TipoCivicoInterno();
        interno.setScala("2");
        interno.setCorte("3");

        TipoNumeroCivico civico = new TipoNumeroCivico();
        civico.setNumero("10");
        civico.setLettera("A");
        civico.setEsponente1("bis");
        civico.setColore("1");
        civico.setCivicoInterno(interno);

        TipoComune comune = new TipoComune();
        comune.setNomeComune("Roma");
        comune.setSiglaProvinciaIstat("RM");

        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setToponimo(toponimo);
        indirizzo.setNumeroCivico(civico);
        indirizzo.setCap("00100");
        indirizzo.setFrazione("Frazione");
        indirizzo.setComune(comune);
        return indirizzo;
    }

    private static TipoResidenza residenza(String data, String tipo) {
        TipoResidenza residenza = new TipoResidenza();
        residenza.setDataDecorrenzaResidenza(data);
        residenza.setTipoIndirizzo(tipo);
        residenza.setPresso("presso");
        return residenza;
    }

    private static RispostaE002OK risposta(String cf, TipoResidenza... residenze) {
        TipoCodiceFiscale codiceFiscale = new TipoCodiceFiscale();
        codiceFiscale.setCodFiscale(cf);
        TipoGeneralita generalita = new TipoGeneralita();
        generalita.setCodiceFiscale(codiceFiscale);
        TipoDatiSoggettiEnte soggetto = new TipoDatiSoggettiEnte();
        soggetto.setGeneralita(generalita);
        soggetto.setResidenza(List.of(residenze));
        TipoListaSoggetti lista = new TipoListaSoggetti();
        lista.setDatiSoggetto(List.of(soggetto));
        RispostaE002OK risposta = new RispostaE002OK();
        risposta.setIdOperazioneANPR("OP-1");
        risposta.setListaSoggetti(lista);
        return risposta;
    }

    private static ResidentialAddressDto convertSingle(AnprConverter converter) {
        TipoResidenza residenza = residenza("2022-01-01", "4");
        residenza.setIndirizzo(buildIndirizzo());
        GetAddressANPROKDto dto = converter.convertToGetAddressANPROK(risposta(CF, residenza), CF);
        assertNotNull(dto.getResidentialAddresses());
        assertEquals(1, dto.getResidentialAddresses().size());
        return dto.getResidentialAddresses().get(0);
    }

    @Test
    void modeOld() {
        ResidentialAddressDto result = convertSingle(converterWithMode(AddressModeEnum.OLD.name()));
        assertEquals("VIA ROMA 10/A", result.getAddress());
        assertEquals("2", result.getAddressDetail());
        assertEquals("00100", result.getZip());
        assertEquals("Frazione", result.getMunicipalityDetails());
        assertEquals("Roma", result.getMunicipality());
        assertEquals("RM", result.getProvince());
        assertEquals("presso", result.getAt());
        assertEquals("4", result.getDescription());
    }

    @Test
    void modeFull() {
        ResidentialAddressDto result = convertSingle(converterWithMode(AddressModeEnum.FULL.name()));
        assertEquals("VIA ROMA 10/A bis", result.getAddress());
        assertEquals("ROSSO Corte 3 Scala 2", result.getAddressDetail());
    }

    @Test
    void modeMinimal() {
        ResidentialAddressDto result = convertSingle(converterWithMode(AddressModeEnum.MINIMAL.name()));
        assertEquals("VIA ROMA 10/A", result.getAddress());
        assertEquals("ROSSO Scala 2", result.getAddressDetail());
    }

    @Test
    void modeOldMinimal() {
        ResidentialAddressDto result = convertSingle(converterWithMode(AddressModeEnum.OLD_MINIMAL.name()));
        assertEquals("VIA ROMA 10/A", result.getAddress());
        assertEquals("ROSSO Scala 2", result.getAddressDetail());
    }

    @Test
    void modeNullFallsBackToOld() {
        ResidentialAddressDto result = convertSingle(converterWithMode(null));
        assertEquals("VIA ROMA 10/A", result.getAddress());
        assertEquals("2", result.getAddressDetail());
    }

    @Test
    void modeUnknownFallsBackToOld() {
        ResidentialAddressDto result = convertSingle(converterWithMode("UNKNOWN"));
        assertEquals("VIA ROMA 10/A", result.getAddress());
        assertEquals("2", result.getAddressDetail());
    }

    @Test
    void modeEmptyFallsBackToOld() {
        ResidentialAddressDto result = convertSingle(converterWithMode(""));
        assertEquals("VIA ROMA 10/A", result.getAddress());
        assertEquals("2", result.getAddressDetail());
    }

    @Test
    void modeIsCaseSensitiveAndFallsBackToOld() {
        ResidentialAddressDto result = convertSingle(converterWithMode("full"));
        assertEquals("VIA ROMA 10/A", result.getAddress());
    }

    @Test
    void exceptionReadingModeFallsBackToOld() {
        NationalRegistriesConfig configs = mock(NationalRegistriesConfig.class);
        when(configs.getAddressCompositionMode()).thenThrow(new IllegalStateException("boom"));
        AnprConverter converter = new AnprConverter(allStrategies(), configs);

        ResidentialAddressDto result = convertSingle(converter);
        assertEquals("VIA ROMA 10/A", result.getAddress());
        assertEquals("2", result.getAddressDetail());
    }

    @Test
    void modeChangeIsReadAtEachConversion() {
        NationalRegistriesConfig configs = new NationalRegistriesConfig();
        configs.setAddressCompositionMode(AddressModeEnum.OLD.name());
        AnprConverter converter = new AnprConverter(allStrategies(), configs);
        assertEquals("2", convertSingle(converter).getAddressDetail());

        configs.setAddressCompositionMode(AddressModeEnum.FULL.name());
        assertEquals("ROSSO Corte 3 Scala 2", convertSingle(converter).getAddressDetail());
    }

    @Test
    void duplicatedStrategyNamesAreRejected() {
        List<AnprAddressStrategy> duplicated = List.of(new OldAnprAddressStrategy(), new OldAnprAddressStrategy());
        NationalRegistriesConfig configs = new NationalRegistriesConfig();
        assertThrows(IllegalStateException.class, () -> new AnprConverter(duplicated, configs));
    }

    @Test
    void nullResponseReturnsEmptyDto() {
        GetAddressANPROKDto dto = converterWithMode("OLD").convertToGetAddressANPROK(null, CF);
        assertNotNull(dto);
        assertNull(dto.getClientOperationId());
        assertTrue(dto.getResidentialAddresses().isEmpty());
    }

    @Test
    void clientOperationIdIsMapped() {
        RispostaE002OK risposta = new RispostaE002OK();
        risposta.setIdOperazioneANPR("OP-9");
        GetAddressANPROKDto dto = converterWithMode("OLD").convertToGetAddressANPROK(risposta, CF);
        assertEquals("OP-9", dto.getClientOperationId());
        assertTrue(dto.getResidentialAddresses().isEmpty());
    }

    @Test
    void taxIdMatchIsCaseInsensitive() {
        TipoResidenza residenza = residenza("2022-01-01", "4");
        residenza.setIndirizzo(buildIndirizzo());
        GetAddressANPROKDto dto = converterWithMode("OLD").convertToGetAddressANPROK(risposta("cf_test", residenza), CF);
        assertEquals(1, dto.getResidentialAddresses().size());
    }

    @Test
    void taxIdNullDoesNotMatch() {
        TipoResidenza residenza = residenza("2022-01-01", "4");
        residenza.setIndirizzo(buildIndirizzo());
        GetAddressANPROKDto dto = converterWithMode("OLD").convertToGetAddressANPROK(risposta(null, residenza), CF);
        assertNull(dto.getResidentialAddresses());
    }

    @Test
    void subjectWithoutGeneralitaIsIgnored() {
        TipoDatiSoggettiEnte soggetto = new TipoDatiSoggettiEnte();
        soggetto.setResidenza(List.of(residenza("2022-01-01", "4")));
        TipoListaSoggetti lista = new TipoListaSoggetti();
        lista.setDatiSoggetto(List.of(soggetto));
        RispostaE002OK risposta = new RispostaE002OK();
        risposta.setListaSoggetti(lista);

        GetAddressANPROKDto dto = converterWithMode("OLD").convertToGetAddressANPROK(risposta, CF);

        assertNull(dto.getResidentialAddresses());
    }

    @Test
    void mostRecentResidenceWinsAndInvalidOrMissingDatesAreOldest() {
        TipoResidenza invalida = residenza("not-a-date", "invalid");
        TipoResidenza nulla = residenza(null, "null");
        TipoResidenza vecchia = residenza("2020-01-01", "old");
        TipoResidenza recente = residenza("2024-06-30", "recent");

        GetAddressANPROKDto dto = converterWithMode("OLD").convertToGetAddressANPROK(risposta(CF, invalida, nulla, vecchia, recente), CF);
        assertEquals(1, dto.getResidentialAddresses().size());
        assertEquals("recent", dto.getResidentialAddresses().get(0).getDescription());
    }

    @Test
    void indirizzoWithoutComuneKeepsMunicipalityNull() {
        TipoIndirizzo indirizzo = buildIndirizzo();
        indirizzo.setComune(null);
        TipoResidenza residenza = residenza("2022-01-01", "4");
        residenza.setIndirizzo(indirizzo);
        ResidentialAddressDto result = converterWithMode("OLD").convertToGetAddressANPROK(risposta(CF, residenza), CF).getResidentialAddresses().get(0);
        assertNull(result.getMunicipality());
        assertNull(result.getProvince());
    }

    @Test
    void indirizzoWithForeignLocalityMapsForeignState() {
        TipoDatoLocalitaEstera localita = new TipoDatoLocalitaEstera();
        localita.setDescrizioneStato("FRANCIA");
        TipoIndirizzoEstero indirizzoEstero = new TipoIndirizzoEstero();
        indirizzoEstero.setLocalita(localita);
        TipoLocalitaEstera1 localitaEstera = new TipoLocalitaEstera1();
        localitaEstera.setIndirizzoEstero(indirizzoEstero);

        TipoResidenza residenza = residenza("2022-01-01", "4");
        residenza.setIndirizzo(buildIndirizzo());
        residenza.setLocalitaEstera(localitaEstera);

        ResidentialAddressDto result = converterWithMode("OLD").convertToGetAddressANPROK(risposta(CF, residenza), CF).getResidentialAddresses().get(0);
        assertEquals("FRANCIA", result.getForeignState());
        assertEquals("VIA ROMA 10/A", result.getAddress());
    }

    @Test
    void foreignResidenceIsMapped() {
        TipoDatoLocalitaEstera localita = new TipoDatoLocalitaEstera();
        localita.setDescrizioneStato("FRANCIA");
        localita.setDescrizioneLocalita("Parigi");
        localita.setProvinciaContea("IDF");
        TipoToponimoEstero toponimo = new TipoToponimoEstero();
        toponimo.setDenominazione("Rue de Rivoli");
        toponimo.setNumeroCivico("5");
        TipoIndirizzoEstero indirizzoEstero = new TipoIndirizzoEstero();
        indirizzoEstero.setCap("75001");
        indirizzoEstero.setToponimo(toponimo);
        indirizzoEstero.setLocalita(localita);
        TipoLocalitaEstera1 localitaEstera = new TipoLocalitaEstera1();
        localitaEstera.setIndirizzoEstero(indirizzoEstero);

        TipoResidenza residenza = residenza("2022-01-01", "4");
        residenza.setLocalitaEstera(localitaEstera);

        ResidentialAddressDto result = converterWithMode("OLD").convertToGetAddressANPROK(risposta(CF, residenza), CF).getResidentialAddresses().get(0);
        assertEquals("75001", result.getZip());
        assertEquals("Rue de Rivoli 5", result.getAddress());
        assertEquals("FRANCIA", result.getForeignState());
        assertEquals("Parigi", result.getMunicipality());
        assertEquals("IDF", result.getProvince());
    }

    @Test
    void foreignResidenceWithBlankCivicoNumberHasOnlyDenomination() {
        TipoToponimoEstero toponimo = new TipoToponimoEstero();
        toponimo.setDenominazione("Rue de Rivoli");
        toponimo.setNumeroCivico(" ");
        TipoIndirizzoEstero indirizzoEstero = new TipoIndirizzoEstero();
        indirizzoEstero.setToponimo(toponimo);
        TipoLocalitaEstera1 localitaEstera = new TipoLocalitaEstera1();
        localitaEstera.setIndirizzoEstero(indirizzoEstero);
        TipoResidenza residenza = residenza("2022-01-01", "4");
        residenza.setLocalitaEstera(localitaEstera);

        ResidentialAddressDto result = converterWithMode("OLD").convertToGetAddressANPROK(risposta(CF, residenza), CF).getResidentialAddresses().get(0);
        assertEquals("Rue de Rivoli", result.getAddress());
        assertNull(result.getForeignState());
    }

    @Test
    void foreignResidenceWithoutIndirizzoEsteroLeavesFieldsNull() {
        TipoResidenza residenza = residenza("2022-01-01", "4");
        residenza.setLocalitaEstera(new TipoLocalitaEstera1());

        ResidentialAddressDto result = converterWithMode("OLD").convertToGetAddressANPROK(risposta(CF, residenza), CF).getResidentialAddresses().get(0);
        assertNull(result.getAddress());
        assertNull(result.getZip());
        assertEquals("presso", result.getAt());
    }
}