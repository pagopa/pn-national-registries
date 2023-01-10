package it.pagopa.pn.national.registries.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.amazonaws.services.sqs.model.SendMessageResult;
import it.pagopa.pn.national.registries.converter.AddressAnprConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import it.pagopa.pn.national.registries.model.anpr.*;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ContextConfiguration(classes = {AddressService.class})
@ExtendWith(SpringExtension.class)
class AddressServiceTest {

    @MockBean
    private AddressAnprConverter addressAnprConverter;
    @MockBean
    private AnprService anprService;
    @MockBean
    private InadService inadService;
    @MockBean
    private InfoCamereService infoCamereService;
    @MockBean
    private SqsService sqsService;

    @Autowired
    private AddressService addressService;

    /**
     * Method under test: {@link AddressService#retrieveDigitalOrPhysicalAddress(String, AddressRequestBodyDto)}
     */
    @Test
    @DisplayName("Test recipientType not valid")
    void testRetrieveDigitalOrPhysicalAddress() {
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.filter(new AddressRequestBodyFilterDto());
        assertThrows(PnNationalRegistriesException.class,
                () -> addressService.retrieveDigitalOrPhysicalAddress("Recipient Type", addressRequestBodyDto));
    }

    @Captor
    ArgumentCaptor<CodeSqsDto> anprSqsCaptor;

    /**
     * Method under test: {@link AddressService#retrieveDigitalOrPhysicalAddress(String, AddressRequestBodyDto)}
     */
    @Test
    @DisplayName("Test retrieve from ANPR")
    void testRetrieveDigitalOrPhysicalAddressAnpr() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId("COD_FISCALE_1");
        filterDto.setCorrelationId("correlationId");
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        filterDto.setReferenceRequestDate("refReqDate");
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.setFilter(filterDto);

        GetAddressANPRRequestBodyFilterDto getAddressANPRRequestBodyFilterDto = new GetAddressANPRRequestBodyFilterDto();
        getAddressANPRRequestBodyFilterDto.setRequestReason("correlationId");
        getAddressANPRRequestBodyFilterDto.setTaxId("COD_FISCALE_1");
        getAddressANPRRequestBodyFilterDto.setReferenceRequestDate("refReqDate");
        GetAddressANPRRequestBodyDto getAddressANPRRequestBodyDto = new GetAddressANPRRequestBodyDto();
        getAddressANPRRequestBodyDto.setFilter(getAddressANPRRequestBodyFilterDto);

        ResidenceDto residenceDto1 = new ResidenceDto();
        residenceDto1.setNoteIndirizzo("r1");
        residenceDto1.setDataDecorrenzaResidenza("2022-11-01");
        ResidenceDto residenceDto2 = new ResidenceDto();
        residenceDto2.setNoteIndirizzo("r2");
        residenceDto2.setDataDecorrenzaResidenza("2022-12-01");
        ResidenceDto residenceDto3 = new ResidenceDto();
        residenceDto3.setDataDecorrenzaResidenza("");
        ResidenceDto residenceDto4 = new ResidenceDto();
        TaxIdDto taxIdDto1 = new TaxIdDto();
        taxIdDto1.setCodFiscale("COD_FISCALE_1");
        GeneralInformationDto generalInformationDto1 = new GeneralInformationDto();
        generalInformationDto1.setCodiceFiscale(taxIdDto1);
        TaxIdDto taxIdDto2 = new TaxIdDto();
        taxIdDto2.setCodFiscale("COD_FISCALE_2");
        GeneralInformationDto generalInformationDto2 = new GeneralInformationDto();
        generalInformationDto2.setCodiceFiscale(taxIdDto2);
        SubjectsInstitutionDataDto subjectsInstitutionDataDto1 = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto1.setResidenza(List.of(residenceDto1, residenceDto2, residenceDto3, residenceDto4));
        subjectsInstitutionDataDto1.setGeneralita(generalInformationDto1);
        SubjectsInstitutionDataDto subjectsInstitutionDataDto2 = new SubjectsInstitutionDataDto();
        subjectsInstitutionDataDto2.setGeneralita(generalInformationDto2);
        SubjectsListDto subjectsListDto = new SubjectsListDto();
        subjectsListDto.setDatiSoggetto(List.of(subjectsInstitutionDataDto1, subjectsInstitutionDataDto2));
        ResponseE002OKDto responseE002OKDto = new ResponseE002OKDto();
        responseE002OKDto.setListaSoggetti(subjectsListDto);

        when(anprService.getRawAddressANPR(getAddressANPRRequestBodyDto))
                .thenReturn(Mono.just(responseE002OKDto));
        when(addressAnprConverter.convertResidence(residenceDto2))
                .thenReturn(new ResidentialAddressDto());
        when(sqsService.push(anprSqsCaptor.capture()))
                .thenReturn(Mono.just(new SendMessageResult()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PF", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertNotNull(anprSqsCaptor.getValue().getPhysicalAddress());
    }

    @Captor
    ArgumentCaptor<CodeSqsDto> inadSqsCaptor;

    /**
     * Method under test: {@link AddressService#retrieveDigitalOrPhysicalAddress(String, AddressRequestBodyDto)}
     */
    @Test
    @DisplayName("Test retrieve from INAD")
    void testRetrieveDigitalOrPhysicalAddress3() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId("COD_FISCALE_1");
        filterDto.setCorrelationId("correlationId");
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        filterDto.setReferenceRequestDate("refReqDate");
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.setFilter(filterDto);

        GetDigitalAddressINADRequestBodyFilterDto getDigitalAddressINADRequestBodyFilterDto = new GetDigitalAddressINADRequestBodyFilterDto();
        getDigitalAddressINADRequestBodyFilterDto.setTaxId("COD_FISCALE_1");
        getDigitalAddressINADRequestBodyFilterDto.setPracticalReference("correlationId");
        GetDigitalAddressINADRequestBodyDto getDigitalAddressINADRequestBodyDto = new GetDigitalAddressINADRequestBodyDto();
        getDigitalAddressINADRequestBodyDto.setFilter(getDigitalAddressINADRequestBodyFilterDto);

        UsageInfoDto usageInfoDto1 = new UsageInfoDto();
        usageInfoDto1.setDateEndValidity(Date.from(LocalDate.EPOCH.atStartOfDay(ZoneOffset.UTC).toInstant()));
        DigitalAddressDto digitalAddressDto1 = new DigitalAddressDto();
        digitalAddressDto1.setDigitalAddress("a1");
        digitalAddressDto1.setUsageInfo(usageInfoDto1);
        UsageInfoDto usageInfoDto2 = new UsageInfoDto();
        usageInfoDto2.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        DigitalAddressDto digitalAddressDto2 = new DigitalAddressDto();
        digitalAddressDto2.setDigitalAddress("a2");
        digitalAddressDto2.setUsageInfo(usageInfoDto2);
        DigitalAddressDto digitalAddressDto3 = new DigitalAddressDto();
        digitalAddressDto3.setDigitalAddress("a3");
        GetDigitalAddressINADOKDto getDigitalAddressINADOKDto = new GetDigitalAddressINADOKDto();
        getDigitalAddressINADOKDto.setTaxId("COD_FISCALE_1");
        getDigitalAddressINADOKDto.setDigitalAddress(List.of(digitalAddressDto1, digitalAddressDto2, digitalAddressDto3));

        when(inadService.callEService(getDigitalAddressINADRequestBodyDto))
                .thenReturn(Mono.just(getDigitalAddressINADOKDto));
        when(sqsService.push(inadSqsCaptor.capture()))
                .thenReturn(Mono.just(new SendMessageResult()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PF", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertNotNull(inadSqsCaptor.getValue().getDigitalAddress());
        assertEquals(2, inadSqsCaptor.getValue().getDigitalAddress().size());
        assertFalse(inadSqsCaptor.getValue().getDigitalAddress().stream()
                .anyMatch(a -> a.getAddress().equals("a1")));
    }

    @Captor
    ArgumentCaptor<CodeSqsDto> regImpSqsCaptor;

    /**
     * Method under test: {@link AddressService#retrieveDigitalOrPhysicalAddress(String, AddressRequestBodyDto)}
     */
    @Test
    @DisplayName("Test retrieve from Registro Imprese")
    void testRetrieveDigitalOrPhysicalAddress4() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId("COD_FISCALE_1");
        filterDto.setCorrelationId("correlationId");
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        filterDto.setReferenceRequestDate("refReqDate");
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.setFilter(filterDto);

        GetAddressRegistroImpreseRequestBodyFilterDto getAddressRegistroImpreseRequestBodyFilterDto = new GetAddressRegistroImpreseRequestBodyFilterDto();
        getAddressRegistroImpreseRequestBodyFilterDto.setTaxId("COD_FISCALE_1");
        GetAddressRegistroImpreseRequestBodyDto getAddressRegistroImpreseRequestBodyDto = new GetAddressRegistroImpreseRequestBodyDto();
        getAddressRegistroImpreseRequestBodyDto.setFilter(getAddressRegistroImpreseRequestBodyFilterDto);

        GetAddressRegistroImpreseOKProfessionalAddressDto getAddressRegistroImpreseOKProfessionalAddressDto = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        getAddressRegistroImpreseOKProfessionalAddressDto.setAddress("a");
        GetAddressRegistroImpreseOKDto getAddressRegistroImpreseOKDto = new GetAddressRegistroImpreseOKDto();
        getAddressRegistroImpreseOKDto.setTaxId("COD_FISCALE_1");
        getAddressRegistroImpreseOKDto.setProfessionalAddress(getAddressRegistroImpreseOKProfessionalAddressDto);

        when(infoCamereService.getRegistroImpreseAddress(getAddressRegistroImpreseRequestBodyDto))
                .thenReturn(Mono.just(getAddressRegistroImpreseOKDto));
        when(sqsService.push(regImpSqsCaptor.capture()))
                .thenReturn(Mono.just(new SendMessageResult()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PG", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertNotNull(regImpSqsCaptor.getValue().getPhysicalAddress());
        assertEquals("a", regImpSqsCaptor.getValue().getPhysicalAddress().getAddress());
    }

    @Test
    @DisplayName("Test retrieve from ")
    void testRetrieveDigitalOrPhysicalAddress5() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId("COD_FISCALE_1");
        filterDto.setCorrelationId("correlationId");
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        filterDto.setReferenceRequestDate("refReqDate");
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.setFilter(filterDto);

        GetDigitalAddressIniPECRequestBodyFilterDto getDigitalAddressIniPECRequestBodyFilterDto = new GetDigitalAddressIniPECRequestBodyFilterDto();
        getDigitalAddressIniPECRequestBodyFilterDto.setTaxId("COD_FISCALE_1");
        getDigitalAddressIniPECRequestBodyFilterDto.setCorrelationId("correlationId");
        GetDigitalAddressIniPECRequestBodyDto getDigitalAddressIniPECRequestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        getDigitalAddressIniPECRequestBodyDto.setFilter(getDigitalAddressIniPECRequestBodyFilterDto);

        when(infoCamereService.getIniPecDigitalAddress(getDigitalAddressIniPECRequestBodyDto))
                .thenReturn(Mono.just(new GetDigitalAddressIniPECOKDto()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PG", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
    }
}
