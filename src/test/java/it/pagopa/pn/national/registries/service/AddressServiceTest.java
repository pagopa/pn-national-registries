package it.pagopa.pn.national.registries.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;

import java.util.List;

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
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@ContextConfiguration(classes = {AddressService.class})
@ExtendWith(SpringExtension.class)
class AddressServiceTest {

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

        ResidentialAddressDto residentialAddressDto = new ResidentialAddressDto();
        residentialAddressDto.setAddress("address");

        GetAddressANPROKDto getAddressANPROKDto = new GetAddressANPROKDto();
        getAddressANPROKDto.setResidentialAddresses(List.of(residentialAddressDto));

        when(anprService.getAddressANPR(getAddressANPRRequestBodyDto))
                .thenReturn(Mono.just(getAddressANPROKDto));
        when(sqsService.push(anprSqsCaptor.capture()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

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
    void testRetrieveDigitalOrPhysicalAddressINAD() {
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

        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("DA");
        GetDigitalAddressINADOKDto getDigitalAddressINADOKDto = new GetDigitalAddressINADOKDto();
        getDigitalAddressINADOKDto.setTaxId("COD_FISCALE_1");
        getDigitalAddressINADOKDto.setDigitalAddress(List.of(digitalAddressDto));

        when(inadService.callEService(getDigitalAddressINADRequestBodyDto))
                .thenReturn(Mono.just(getDigitalAddressINADOKDto));
        when(sqsService.push(inadSqsCaptor.capture()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PF", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertNotNull(inadSqsCaptor.getValue().getDigitalAddress());
        assertEquals(1, inadSqsCaptor.getValue().getDigitalAddress().size());
        assertTrue(inadSqsCaptor.getValue().getDigitalAddress().stream()
                .anyMatch(a -> a.getAddress().equals("DA")));
    }

    @Captor
    ArgumentCaptor<CodeSqsDto> regImpSqsCaptor;

    /**
     * Method under test: {@link AddressService#retrieveDigitalOrPhysicalAddress(String, AddressRequestBodyDto)}
     */
    @Test
    @DisplayName("Test retrieve from Registro Imprese")
    void testRetrieveDigitalOrPhysicalAddressRegImp() {
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

        when(infoCamereService.getRegistroImpreseLegalAddress(getAddressRegistroImpreseRequestBodyDto))
                .thenReturn(Mono.just(getAddressRegistroImpreseOKDto));
        when(sqsService.push(regImpSqsCaptor.capture()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PG", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertNotNull(regImpSqsCaptor.getValue().getPhysicalAddress());
        assertEquals("a", regImpSqsCaptor.getValue().getPhysicalAddress().getAddress());
    }

    @Test
    @DisplayName("Test retrieve from INIPEC")
    void testRetrieveDigitalOrPhysicalAddressINIPEC() {
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
