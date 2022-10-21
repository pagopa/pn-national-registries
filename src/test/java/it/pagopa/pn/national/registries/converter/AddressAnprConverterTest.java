package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.pn.national.registries.model.anpr.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressAnprConverterTest {

    @InjectMocks
    private AddressAnprConverter addressAnprConverter;

    /**
     * Method under test: {@link AddressAnprConverter#convertToGetAddressANPROKDto(ResponseE002OKDto, String)}
     */
    @Test
    void testConvertToGetAddressANPROKDto() {
        SubjectsListDto subjectsListDto = new SubjectsListDto();
        subjectsListDto.setDatiSoggetto(new ArrayList<>());

        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setListaAnomalie(new ArrayList<>());
        responseE002OKDto.setListaSoggetti(subjectsListDto);
        responseE002OKDto.setTestataRisposta(new ResponseHeaderE002Dto());
        assertNull(addressAnprConverter.convertToGetAddressANPROKDto(responseE002OKDto, "Cf").getResidentialAddresses());
    }

    @Test
    void testConvertToGetAddressANPROKDto5() {
        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setTestataRisposta(new ResponseHeaderE002Dto());
        assertNull(addressAnprConverter.convertToGetAddressANPROKDto(responseE002OKDto, "Cf").getResidentialAddresses());
    }

    /**
     * Method under test: {@link AddressAnprConverter#convertToGetAddressANPROKDto(ResponseE002OKDto, String)}
     */
    @Test
    void testConvertToGetAddressANPROKDto11() {
        TaxIdDto taxIdDto = new TaxIdDto();
        taxIdDto.setCodFiscale("Cf");
        taxIdDto.setDataAttribuzioneValidita("Data Attribuzione Validita");
        taxIdDto.setValiditaCF("Validita CF");

        GeneralInformationDto generalInformationDto = new GeneralInformationDto();
        generalInformationDto.setCodiceFiscale(taxIdDto);

        SubjectsInstitutionDataDto subjectsInstitutionDataDto = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto.setGeneralita(generalInformationDto);

        List<ResidenceDto> residenceDtoList = new ArrayList<>();
        ResidenceDto residenceDto = new ResidenceDto();

        residenceDto.setPresso("presso");
        residenceDto.setTipoIndirizzo("4");

        residenceDtoList.add(residenceDto);
        subjectsInstitutionDataDto.setResidenza(residenceDtoList);

        ArrayList<SubjectsInstitutionDataDto> subjectsInstitutionDataDtoList = new ArrayList<>();
        subjectsInstitutionDataDtoList.add(subjectsInstitutionDataDto);

        SubjectsListDto subjectsListDto = new SubjectsListDto();
        subjectsListDto.setDatiSoggetto(subjectsInstitutionDataDtoList);

        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setListaAnomalie(new ArrayList<>());
        responseE002OKDto.setListaSoggetti(subjectsListDto);
        responseE002OKDto.setTestataRisposta(new ResponseHeaderE002Dto());
        assertNotNull(addressAnprConverter.convertToGetAddressANPROKDto(responseE002OKDto, "Cf").getResidentialAddresses());
    }


    /**
     * Method under test: {@link AddressAnprConverter#convertToGetAddressANPROKDto(ResponseE002OKDto, String)}
     */
    @Test
    void testConvertToGetAddressANPROKDto2() {
        TaxIdDto taxIdDto = new TaxIdDto();
        taxIdDto.setCodFiscale("Cf");
        taxIdDto.setDataAttribuzioneValidita("Data Attribuzione Validita");
        taxIdDto.setValiditaCF("Validita CF");

        GeneralInformationDto generalInformationDto = new GeneralInformationDto();
        generalInformationDto.setCodiceFiscale(taxIdDto);

        SubjectsInstitutionDataDto subjectsInstitutionDataDto = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto.setGeneralita(generalInformationDto);

        List<ResidenceDto> residenceDtoList = new ArrayList<>();
        ResidenceDto residenceDto = new ResidenceDto();

        AddressDto addressDto = new AddressDto();
        StreetNumberDto streetNumberDto = new StreetNumberDto();
        streetNumberDto.setNumero("70");
        streetNumberDto.setLettera("A");
        addressDto.setNumeroCivico(streetNumberDto);

        ToponymDto toponymDto = new ToponymDto();
        toponymDto.setSpecie("specie");
        toponymDto.setDenominazioneToponimo("denominazione Toponimo");
        addressDto.setToponimo(toponymDto);
        addressDto.setCap("00178");
        addressDto.setFrazione("frazione");

        MunicipalityDto municipalityDto = new MunicipalityDto();
        municipalityDto.setNomeComune("nomeComune");
        municipalityDto.setSiglaProvinciaIstat("RM");
        addressDto.setComune(municipalityDto);

        ForeignLocation1Dto foreignLocation1Dto = new ForeignLocation1Dto();
        ForeignAddressDto foreignAddressDto = new ForeignAddressDto();
        ForeignLocationDataDto foreignLocationDataDto = new ForeignLocationDataDto();
        foreignLocationDataDto.setDescrizioneLocalita("descrizione");
        foreignAddressDto.setLocalita(foreignLocationDataDto);

        foreignLocation1Dto.setIndirizzoEstero(foreignAddressDto);
        residenceDto.setLocalitaEstera(foreignLocation1Dto);

        residenceDto.setIndirizzo(addressDto);
        residenceDto.setPresso("presso");
        residenceDto.setTipoIndirizzo("4");

        residenceDtoList.add(residenceDto);
        subjectsInstitutionDataDto.setResidenza(residenceDtoList);

        ArrayList<SubjectsInstitutionDataDto> subjectsInstitutionDataDtoList = new ArrayList<>();
        subjectsInstitutionDataDtoList.add(subjectsInstitutionDataDto);

        SubjectsListDto subjectsListDto = new SubjectsListDto();
        subjectsListDto.setDatiSoggetto(subjectsInstitutionDataDtoList);

        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setListaAnomalie(new ArrayList<>());
        responseE002OKDto.setListaSoggetti(subjectsListDto);
        responseE002OKDto.setTestataRisposta(new ResponseHeaderE002Dto());
        assertNotNull(addressAnprConverter.convertToGetAddressANPROKDto(responseE002OKDto, "Cf").getResidentialAddresses());
    }

    @Test
    void testConvertToGetAddressANPROKDto6() {
        TaxIdDto taxIdDto = new TaxIdDto();
        taxIdDto.setCodFiscale("Cf");
        taxIdDto.setDataAttribuzioneValidita("Data Attribuzione Validita");
        taxIdDto.setValiditaCF("Validita CF");

        GeneralInformationDto generalInformationDto = new GeneralInformationDto();
        generalInformationDto.setCodiceFiscale(taxIdDto);

        SubjectsInstitutionDataDto subjectsInstitutionDataDto = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto.setGeneralita(generalInformationDto);

        ArrayList<SubjectsInstitutionDataDto> subjectsInstitutionDataDtoList = new ArrayList<>();
        subjectsInstitutionDataDtoList.add(subjectsInstitutionDataDto);

        SubjectsListDto subjectsListDto = new SubjectsListDto();
        subjectsListDto.setDatiSoggetto(subjectsInstitutionDataDtoList);

        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setListaSoggetti(subjectsListDto);
        assertNull(addressAnprConverter.convertToGetAddressANPROKDto(responseE002OKDto, "Cf").getResidentialAddresses());
    }

    @Test
    void testConvertToGetAddressANPROKDto7() {
        GeneralInformationDto generalInformationDto = new GeneralInformationDto();

        SubjectsInstitutionDataDto subjectsInstitutionDataDto = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto.setGeneralita(generalInformationDto);

        ArrayList<SubjectsInstitutionDataDto> subjectsInstitutionDataDtoList = new ArrayList<>();
        subjectsInstitutionDataDtoList.add(subjectsInstitutionDataDto);

        SubjectsListDto subjectsListDto = new SubjectsListDto();
        subjectsListDto.setDatiSoggetto(subjectsInstitutionDataDtoList);

        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setListaSoggetti(subjectsListDto);
        assertNull(addressAnprConverter.convertToGetAddressANPROKDto(responseE002OKDto, "Cf").getResidentialAddresses());
    }

    @Test
    void testConvertToGetAddressANPROKDto8() {
        TaxIdDto taxIdDto = new TaxIdDto();
        taxIdDto.setCodFiscale("Cf");
        taxIdDto.setDataAttribuzioneValidita("Data Attribuzione Validita");
        taxIdDto.setValiditaCF("Validita CF");

        GeneralInformationDto generalInformationDto = new GeneralInformationDto();
        generalInformationDto.setCodiceFiscale(taxIdDto);

        SubjectsInstitutionDataDto subjectsInstitutionDataDto = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto.setGeneralita(generalInformationDto);

        ArrayList<SubjectsInstitutionDataDto> subjectsInstitutionDataDtoList = new ArrayList<>();
        subjectsInstitutionDataDtoList.add(subjectsInstitutionDataDto);

        SubjectsListDto subjectsListDto = new SubjectsListDto();
        subjectsListDto.setDatiSoggetto(subjectsInstitutionDataDtoList);

        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setListaSoggetti(subjectsListDto);
        assertNull(addressAnprConverter.convertToGetAddressANPROKDto(responseE002OKDto, "cf2").getResidentialAddresses());
    }

    /**
     * Method under test: {@link AddressAnprConverter#convertToGetAddressANPROKDto(ResponseE002OKDto, String)}
     */
    @Test
    void testConvertToGetAddressANPROKDto3() {
        TaxIdDto taxIdDto = new TaxIdDto();
        taxIdDto.setCodFiscale("Cf");
        taxIdDto.setDataAttribuzioneValidita("Data Attribuzione Validita");
        taxIdDto.setValiditaCF("Validita CF");

        GeneralInformationDto generalInformationDto = new GeneralInformationDto();
        generalInformationDto.setCodiceFiscale(taxIdDto);

        SubjectsInstitutionDataDto subjectsInstitutionDataDto = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto.setGeneralita(generalInformationDto);

        List<ResidenceDto> residenceDtoList = new ArrayList<>();
        ResidenceDto residenceDto = new ResidenceDto();

        AddressDto addressDto = new AddressDto();
        StreetNumberDto streetNumberDto = new StreetNumberDto();
        streetNumberDto.setNumero("70");
        streetNumberDto.setLettera("A");
        addressDto.setNumeroCivico(streetNumberDto);

        MunicipalityDto municipalityDto = new MunicipalityDto();
        municipalityDto.setNomeComune("nomeComune");
        municipalityDto.setSiglaProvinciaIstat("RM");
        addressDto.setComune(municipalityDto);

        ForeignLocation1Dto foreignLocation1Dto = new ForeignLocation1Dto();
        ForeignAddressDto foreignAddressDto = new ForeignAddressDto();
        ForeignLocationDataDto foreignLocationDataDto = new ForeignLocationDataDto();
        foreignLocationDataDto.setDescrizioneLocalita("descrizione");
        foreignAddressDto.setLocalita(foreignLocationDataDto);

        foreignLocation1Dto.setIndirizzoEstero(foreignAddressDto);
        residenceDto.setLocalitaEstera(foreignLocation1Dto);

        residenceDto.setIndirizzo(addressDto);
        residenceDto.setPresso("presso");
        residenceDto.setTipoIndirizzo("4");

        residenceDtoList.add(residenceDto);
        subjectsInstitutionDataDto.setResidenza(residenceDtoList);

        ArrayList<SubjectsInstitutionDataDto> subjectsInstitutionDataDtoList = new ArrayList<>();
        subjectsInstitutionDataDtoList.add(subjectsInstitutionDataDto);

        SubjectsListDto subjectsListDto = new SubjectsListDto();
        subjectsListDto.setDatiSoggetto(subjectsInstitutionDataDtoList);

        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setListaAnomalie(new ArrayList<>());
        responseE002OKDto.setListaSoggetti(subjectsListDto);
        responseE002OKDto.setTestataRisposta(new ResponseHeaderE002Dto());
        assertNotNull(addressAnprConverter.convertToGetAddressANPROKDto(responseE002OKDto, "Cf").getResidentialAddresses());
    }

    @Test
    void testConvertToGetAddressANPROKDto9() {
        TaxIdDto taxIdDto = new TaxIdDto();
        taxIdDto.setCodFiscale("Cf");
        taxIdDto.setDataAttribuzioneValidita("Data Attribuzione Validita");
        taxIdDto.setValiditaCF("Validita CF");

        GeneralInformationDto generalInformationDto = new GeneralInformationDto();
        generalInformationDto.setCodiceFiscale(taxIdDto);

        SubjectsInstitutionDataDto subjectsInstitutionDataDto = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto.setGeneralita(generalInformationDto);

        List<ResidenceDto> residenceDtoList = new ArrayList<>();
        ResidenceDto residenceDto = new ResidenceDto();

        ForeignLocation1Dto foreignLocation1Dto = new ForeignLocation1Dto();
        ForeignAddressDto foreignAddressDto = new ForeignAddressDto();
        ForeignLocationDataDto foreignLocationDataDto = new ForeignLocationDataDto();
        foreignLocationDataDto.setDescrizioneLocalita("descrizione");
        foreignAddressDto.setLocalita(foreignLocationDataDto);

        foreignLocation1Dto.setIndirizzoEstero(foreignAddressDto);
        residenceDto.setLocalitaEstera(foreignLocation1Dto);

        residenceDto.setPresso("presso");
        residenceDto.setTipoIndirizzo("4");

        residenceDtoList.add(residenceDto);
        subjectsInstitutionDataDto.setResidenza(residenceDtoList);

        ArrayList<SubjectsInstitutionDataDto> subjectsInstitutionDataDtoList = new ArrayList<>();
        subjectsInstitutionDataDtoList.add(subjectsInstitutionDataDto);

        SubjectsListDto subjectsListDto = new SubjectsListDto();
        subjectsListDto.setDatiSoggetto(subjectsInstitutionDataDtoList);

        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setListaAnomalie(new ArrayList<>());
        responseE002OKDto.setListaSoggetti(subjectsListDto);
        responseE002OKDto.setTestataRisposta(new ResponseHeaderE002Dto());
        assertNotNull(addressAnprConverter.convertToGetAddressANPROKDto(responseE002OKDto, "Cf").getResidentialAddresses());
    }

    @Test
    void testConvertToGetAddressANPROKDto10() {
        TaxIdDto taxIdDto = new TaxIdDto();
        taxIdDto.setCodFiscale("Cf");
        taxIdDto.setDataAttribuzioneValidita("Data Attribuzione Validita");
        taxIdDto.setValiditaCF("Validita CF");

        GeneralInformationDto generalInformationDto = new GeneralInformationDto();
        generalInformationDto.setCodiceFiscale(taxIdDto);

        SubjectsInstitutionDataDto subjectsInstitutionDataDto = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto.setGeneralita(generalInformationDto);

        List<ResidenceDto> residenceDtoList = new ArrayList<>();
        ResidenceDto residenceDto = new ResidenceDto();

        ForeignLocation1Dto foreignLocation1Dto = new ForeignLocation1Dto();
        ForeignAddressDto foreignAddressDto = new ForeignAddressDto();
        ForeignLocationDataDto foreignLocationDataDto = new ForeignLocationDataDto();
        foreignLocationDataDto.setDescrizioneLocalita("descrizione");
        foreignAddressDto.setLocalita(foreignLocationDataDto);

        foreignLocation1Dto.setIndirizzoEstero(foreignAddressDto);
        residenceDto.setLocalitaEstera(foreignLocation1Dto);

        ForeignToponymDto foreignToponymDto = new ForeignToponymDto();
        foreignToponymDto.setNumeroCivico("34");
        foreignToponymDto.setDenominazione("via");
        foreignAddressDto.setToponimo(foreignToponymDto);

        foreignLocation1Dto.setIndirizzoEstero(foreignAddressDto);

        residenceDto.setPresso("presso");
        residenceDto.setTipoIndirizzo("4");

        residenceDtoList.add(residenceDto);
        subjectsInstitutionDataDto.setResidenza(residenceDtoList);

        ArrayList<SubjectsInstitutionDataDto> subjectsInstitutionDataDtoList = new ArrayList<>();
        subjectsInstitutionDataDtoList.add(subjectsInstitutionDataDto);

        SubjectsListDto subjectsListDto = new SubjectsListDto();
        subjectsListDto.setDatiSoggetto(subjectsInstitutionDataDtoList);

        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setListaAnomalie(new ArrayList<>());
        responseE002OKDto.setListaSoggetti(subjectsListDto);
        responseE002OKDto.setTestataRisposta(new ResponseHeaderE002Dto());
        assertNotNull(addressAnprConverter.convertToGetAddressANPROKDto(responseE002OKDto, "Cf").getResidentialAddresses());
    }


}

