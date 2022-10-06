package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.pn.national.registries.model.anpr.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.util.ArrayListWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(MockitoExtension.class)
class AddressAnprConverterTest {

    @InjectMocks
    private AddressAnprConverter addressAnprConverter;

    /**
     * Method under test: {@link AddressAnprConverter#convertToGetAddressANPROKDto(RispostaE002OKDto, String)}
     */
    @Test
    void testConvertToGetAddressANPROKDto() {
        TipoListaSoggettiDto tipoListaSoggettiDto = new TipoListaSoggettiDto();
        tipoListaSoggettiDto.setDatiSoggetto(new ArrayList<>());

        RispostaE002OKDto rispostaE002OKDto = new RispostaE002OKDto();
        rispostaE002OKDto.setListaAnomalie(new ArrayList<>());
        rispostaE002OKDto.setListaSoggetti(tipoListaSoggettiDto);
        rispostaE002OKDto.setTestataRisposta(new TipoTestataRispostaE000Dto());
        assertNull(addressAnprConverter.convertToGetAddressANPROKDto(rispostaE002OKDto, "Cf").getResidentialAddresses());
    }

    /**
     * Method under test: {@link AddressAnprConverter#convertToGetAddressANPROKDto(RispostaE002OKDto, String)}
     */
    @Test
    void testConvertToGetAddressANPROKDto2() {
        TipoCodiceFiscaleDto tipoCodiceFiscaleDto = new TipoCodiceFiscaleDto();
        tipoCodiceFiscaleDto.setCodFiscale("Cf");
        tipoCodiceFiscaleDto.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscaleDto.setValiditaCF("Validita CF");

        TipoGeneralitaDto tipoGeneralitaDto = new TipoGeneralitaDto();
        tipoGeneralitaDto.setCodiceFiscale(tipoCodiceFiscaleDto);

        TipoDatiSoggettiEnteDto tipoDatiSoggettiEnteDto = new TipoDatiSoggettiEnteDto();
        tipoDatiSoggettiEnteDto.setGeneralita(tipoGeneralitaDto);

        List<TipoResidenzaDto> tipoResidenzaDtoList = new ArrayList<>();
        TipoResidenzaDto tipoResidenzaDto = new TipoResidenzaDto();

        TipoIndirizzoDto tipoIndirizzoDto = new TipoIndirizzoDto();
        TipoNumeroCivicoDto tipoNumeroCivicoDto = new TipoNumeroCivicoDto();
        tipoNumeroCivicoDto.setNumero("70");
        tipoNumeroCivicoDto.setLettera("A");
        tipoIndirizzoDto.setNumeroCivico(tipoNumeroCivicoDto);

        TipoToponimoDto tipoToponimoDto = new TipoToponimoDto();
        tipoToponimoDto.setSpecie("specie");
        tipoToponimoDto.setDenominazioneToponimo("denominazione Toponimo");
        tipoIndirizzoDto.setToponimo(tipoToponimoDto);
        tipoIndirizzoDto.setCap("00178");
        tipoIndirizzoDto.setFrazione("frazione");

        TipoComuneDto tipoComuneDto = new TipoComuneDto();
        tipoComuneDto.setNomeComune("nomeComune");
        tipoComuneDto.setSiglaProvinciaIstat("RM");
        tipoIndirizzoDto.setComune(tipoComuneDto);

        TipoLocalitaEstera1Dto tipoLocalitaEstera1Dto = new TipoLocalitaEstera1Dto();
        TipoIndirizzoEsteroDto tipoIndirizzoEsteroDto = new TipoIndirizzoEsteroDto();
        TipoDatoLocalitaEsteraDto tipoDatoLocalitaEsteraDto = new TipoDatoLocalitaEsteraDto();
        tipoDatoLocalitaEsteraDto.setDescrizioneLocalita("descrizione");
        tipoIndirizzoEsteroDto.setLocalita(tipoDatoLocalitaEsteraDto);

        tipoLocalitaEstera1Dto.setIndirizzoEstero(tipoIndirizzoEsteroDto);
        tipoResidenzaDto.setLocalitaEstera(tipoLocalitaEstera1Dto);

        tipoResidenzaDto.setIndirizzo(tipoIndirizzoDto);
        tipoResidenzaDto.setPresso("presso");
        tipoResidenzaDto.setTipoIndirizzo("4");

        tipoResidenzaDtoList.add(tipoResidenzaDto);
        tipoDatiSoggettiEnteDto.setResidenza(tipoResidenzaDtoList);

        ArrayList<TipoDatiSoggettiEnteDto> tipoDatiSoggettiEnteDtoList = new ArrayList<>();
        tipoDatiSoggettiEnteDtoList.add(tipoDatiSoggettiEnteDto);

        TipoListaSoggettiDto tipoListaSoggettiDto = new TipoListaSoggettiDto();
        tipoListaSoggettiDto.setDatiSoggetto(tipoDatiSoggettiEnteDtoList);

        RispostaE002OKDto rispostaE002OKDto = new RispostaE002OKDto();
        rispostaE002OKDto.setListaAnomalie(new ArrayList<>());
        rispostaE002OKDto.setListaSoggetti(tipoListaSoggettiDto);
        rispostaE002OKDto.setTestataRisposta(new TipoTestataRispostaE000Dto());
        assertNotNull(addressAnprConverter.convertToGetAddressANPROKDto(rispostaE002OKDto, "Cf").getResidentialAddresses());
    }

    /**
     * Method under test: {@link AddressAnprConverter#convertToGetAddressANPROKDto(RispostaE002OKDto, String)}
     */
    @Test
    void testConvertToGetAddressANPROKDto3() {
        TipoCodiceFiscaleDto tipoCodiceFiscaleDto = new TipoCodiceFiscaleDto();
        tipoCodiceFiscaleDto.setCodFiscale("Cf");
        tipoCodiceFiscaleDto.setDataAttribuzioneValidita("Data Attribuzione Validita");
        tipoCodiceFiscaleDto.setValiditaCF("Validita CF");

        TipoGeneralitaDto tipoGeneralitaDto = new TipoGeneralitaDto();
        tipoGeneralitaDto.setCodiceFiscale(tipoCodiceFiscaleDto);

        TipoDatiSoggettiEnteDto tipoDatiSoggettiEnteDto = new TipoDatiSoggettiEnteDto();
        tipoDatiSoggettiEnteDto.setGeneralita(tipoGeneralitaDto);

        List<TipoResidenzaDto> tipoResidenzaDtoList = new ArrayList<>();
        TipoResidenzaDto tipoResidenzaDto = new TipoResidenzaDto();

        TipoIndirizzoDto tipoIndirizzoDto = new TipoIndirizzoDto();
        TipoNumeroCivicoDto tipoNumeroCivicoDto = new TipoNumeroCivicoDto();
        tipoNumeroCivicoDto.setNumero("70");
        tipoNumeroCivicoDto.setLettera("A");
        tipoIndirizzoDto.setNumeroCivico(tipoNumeroCivicoDto);

        TipoComuneDto tipoComuneDto = new TipoComuneDto();
        tipoComuneDto.setNomeComune("nomeComune");
        tipoComuneDto.setSiglaProvinciaIstat("RM");
        tipoIndirizzoDto.setComune(tipoComuneDto);

        TipoLocalitaEstera1Dto tipoLocalitaEstera1Dto = new TipoLocalitaEstera1Dto();
        TipoIndirizzoEsteroDto tipoIndirizzoEsteroDto = new TipoIndirizzoEsteroDto();
        TipoDatoLocalitaEsteraDto tipoDatoLocalitaEsteraDto = new TipoDatoLocalitaEsteraDto();
        tipoDatoLocalitaEsteraDto.setDescrizioneLocalita("descrizione");
        tipoIndirizzoEsteroDto.setLocalita(tipoDatoLocalitaEsteraDto);

        tipoLocalitaEstera1Dto.setIndirizzoEstero(tipoIndirizzoEsteroDto);
        tipoResidenzaDto.setLocalitaEstera(tipoLocalitaEstera1Dto);

        tipoResidenzaDto.setIndirizzo(tipoIndirizzoDto);
        tipoResidenzaDto.setPresso("presso");
        tipoResidenzaDto.setTipoIndirizzo("4");

        tipoResidenzaDtoList.add(tipoResidenzaDto);
        tipoDatiSoggettiEnteDto.setResidenza(tipoResidenzaDtoList);

        ArrayList<TipoDatiSoggettiEnteDto> tipoDatiSoggettiEnteDtoList = new ArrayList<>();
        tipoDatiSoggettiEnteDtoList.add(tipoDatiSoggettiEnteDto);

        TipoListaSoggettiDto tipoListaSoggettiDto = new TipoListaSoggettiDto();
        tipoListaSoggettiDto.setDatiSoggetto(tipoDatiSoggettiEnteDtoList);

        RispostaE002OKDto rispostaE002OKDto = new RispostaE002OKDto();
        rispostaE002OKDto.setListaAnomalie(new ArrayList<>());
        rispostaE002OKDto.setListaSoggetti(tipoListaSoggettiDto);
        rispostaE002OKDto.setTestataRisposta(new TipoTestataRispostaE000Dto());
        assertNotNull(addressAnprConverter.convertToGetAddressANPROKDto(rispostaE002OKDto, "Cf").getResidentialAddresses());
    }

}

