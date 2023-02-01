package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.model.anpr.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testConvertConFiltroPerDataDecorrenza() {
        ResidenceDto residenceDto1 = new ResidenceDto();
        residenceDto1.setTipoIndirizzo("t1");
        residenceDto1.setDataDecorrenzaResidenza("2022-11-01");
        ResidenceDto residenceDto2 = new ResidenceDto();
        residenceDto2.setTipoIndirizzo("t2");
        residenceDto2.setDataDecorrenzaResidenza("2022-12-01");
        ResidenceDto residenceDto3 = new ResidenceDto();
        residenceDto3.setDataDecorrenzaResidenza("");
        residenceDto3.setTipoIndirizzo("t3");
        ResidenceDto residenceDto4 = new ResidenceDto();
        residenceDto4.setTipoIndirizzo("t4");
        // mi aspetto che venga selezionata la residence_2 che contiene la data di decorrenza più recente

        TaxIdDto taxIdDto1 = new TaxIdDto();
        taxIdDto1.setCodFiscale("COD_FISCALE_1");

        GeneralInformationDto generalInformationDto1 = new GeneralInformationDto();
        generalInformationDto1.setCodiceFiscale(taxIdDto1);

        SubjectsInstitutionDataDto subjectsInstitutionDataDto1 = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto1.setResidenza(List.of(residenceDto1, residenceDto2, residenceDto3, residenceDto4));
        subjectsInstitutionDataDto1.setGeneralita(generalInformationDto1);

        TaxIdDto taxIdDto2 = new TaxIdDto();
        taxIdDto2.setCodFiscale("COD_FISCALE_2");

        GeneralInformationDto generalInformationDto2 = new GeneralInformationDto();
        generalInformationDto2.setCodiceFiscale(taxIdDto2);

        SubjectsInstitutionDataDto subjectsInstitutionDataDto2 = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto2.setGeneralita(generalInformationDto2);
        // mi aspetto che questo subject venga scartato perché il CF non combacia

        SubjectsListDto subjectsListDto = new SubjectsListDto();
        subjectsListDto.setDatiSoggetto(List.of(subjectsInstitutionDataDto1, subjectsInstitutionDataDto2));

        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setListaSoggetti(subjectsListDto);

        GetAddressANPROKDto response = addressAnprConverter.convertToGetAddressANPROKDto(responseE002OKDto, "COD_FISCALE_1");
        assertNotNull(response);
        assertNotNull(response.getResidentialAddresses());
        assertEquals(1, response.getResidentialAddresses().size());
        assertEquals("t2", response.getResidentialAddresses().get(0).getDescription());
    }
}
