package it.pagopa.pn.national.registries.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.pn.commons.log.MDCWebFilter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.AddressRequestBodyFilterDto;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@TestPropertySource(properties = {
        "pn.national.registries.val.cx.id.enabled=true"
})
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

    @Test
    @DisplayName("Test recipientType not valid")
    void testCheckFlag() {
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.filter(new AddressRequestBodyFilterDto());
        assertThrows(PnNationalRegistriesException.class,
                () -> addressService.retrieveDigitalOrPhysicalAddressAsync("Recipient Type", null, addressRequestBodyDto));
    }

    @Test
    @DisplayName("Test recipientType not valid")
    void testRetrieveDigitalOrPhysicalAddress() {
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.filter(new AddressRequestBodyFilterDto());
        assertThrows(PnNationalRegistriesException.class,
                () -> addressService.retrieveDigitalOrPhysicalAddress("Recipient Type", "clientId", addressRequestBodyDto));
    }

    @Test
    @DisplayName("Test CxId required")
    void testRetrieveDigitalOrPhysicalAddressThrow() {
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.filter(new AddressRequestBodyFilterDto());
        assertThrows(PnNationalRegistriesException.class,
                () -> addressService.retrieveDigitalOrPhysicalAddress("Recipient Type", null, addressRequestBodyDto));
    }

    @Captor
    ArgumentCaptor<CodeSqsDto> anprSqsCaptor;

    @Test
    @DisplayName("Test retrieve from ANPR Async")
    void testRetrieveDigitalOrPhysicalAddressAsync() {
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

        when(sqsService.push((CodeSqsDto) any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        MDC.setContextMap(Map.of(MDCWebFilter.MDC_TRACE_ID_KEY, "traceId"));

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddressAsync("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
    }

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

        when(sqsService.push(anprSqsCaptor.capture(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertNotNull(anprSqsCaptor.getValue().getPhysicalAddress());
        assertNull(anprSqsCaptor.getValue().getError());
        assertEquals("PHYSICAL", anprSqsCaptor.getValue().getAddressType());
    }

    @Test
    @DisplayName("Test failed retrieve from ANPR")
    void testRetrieveDigitalOrPhysicalAddressAnprError() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId("COD_FISCALE_1");
        filterDto.setCorrelationId("correlationId");
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        filterDto.setReferenceRequestDate("refReqDate");
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.setFilter(filterDto);

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(exception.getResponseBodyAsString()).thenReturn("{ ... \"codiceErroreAnomalia\": \"ENX\", ...");
        when(exception.getMessage()).thenReturn("message");

        when(anprService.getAddressANPR(any()))
                .thenReturn(Mono.error(exception));

        when(sqsService.push(anprSqsCaptor.capture(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertEquals("message", anprSqsCaptor.getValue().getError());
        assertEquals("PHYSICAL", anprSqsCaptor.getValue().getAddressType());
        assertNull(anprSqsCaptor.getValue().getPhysicalAddress());
    }

    @Test
    @DisplayName("Test retrieve from ANPR - CF non trovato")
    void testRetrieveDigitalOrPhysicalAddressAnprCfNotFound() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId("COD_FISCALE_1");
        filterDto.setCorrelationId("correlationId");
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        filterDto.setReferenceRequestDate("refReqDate");
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.setFilter(filterDto);

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(exception.getResponseBodyAsString()).thenReturn("{ ... \"codiceErroreAnomalia\": \"EN122\", ...");

        when(anprService.getAddressANPR(any()))
                .thenReturn(Mono.error(exception));

        when(sqsService.push(anprSqsCaptor.capture(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertNull(anprSqsCaptor.getValue().getError());
        assertNull(anprSqsCaptor.getValue().getPhysicalAddress());
        assertEquals("PHYSICAL", anprSqsCaptor.getValue().getAddressType());
    }

    @Captor
    ArgumentCaptor<CodeSqsDto> inadSqsCaptor;

    @Test
    @DisplayName("Test retrieve from INAD")
    void testRetrieveDigitalOrPhysicalAddressInad() {
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

        when(sqsService.push(inadSqsCaptor.capture(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertNotNull(inadSqsCaptor.getValue().getDigitalAddress());
        assertEquals(3, inadSqsCaptor.getValue().getDigitalAddress().size());
        assertTrue(inadSqsCaptor.getValue().getDigitalAddress().stream()
                .anyMatch(a -> a.getAddress().equals("a1")));
        assertEquals("DIGITAL", inadSqsCaptor.getValue().getAddressType());
    }

    @Test
    @DisplayName("Test failed retrieve from INAD")
    void testRetrieveDigitalOrPhysicalAddressInadError() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId("COD_FISCALE_1");
        filterDto.setCorrelationId("correlationId");
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        filterDto.setReferenceRequestDate("refReqDate");
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.setFilter(filterDto);

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(exception.getResponseBodyAsString()).thenReturn("{ ... \"detail\": \"xxx\", ...");
        when(exception.getMessage()).thenReturn("message");

        when(inadService.callEService(any()))
                .thenReturn(Mono.error(exception));

        when(sqsService.push(inadSqsCaptor.capture(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertNull(inadSqsCaptor.getValue().getDigitalAddress());
        assertEquals("DIGITAL", inadSqsCaptor.getValue().getAddressType());
        assertEquals("message", inadSqsCaptor.getValue().getError());
    }

    @Test
    @DisplayName("Test retrieve from ANPR - CF non trovato")
    void testRetrieveDigitalOrPhysicalAddressInadCfNotFound() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId("COD_FISCALE_1");
        filterDto.setCorrelationId("correlationId");
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        filterDto.setReferenceRequestDate("refReqDate");
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.setFilter(filterDto);

        PnNationalRegistriesException exception = new PnNationalRegistriesException("", HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"detail\": \"Cf non trovato\" ... }".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8,
                InadResponseKO.class);

        when(inadService.callEService(any()))
                .thenReturn(Mono.error(exception));
        when(sqsService.push(inadSqsCaptor.capture(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertEquals("DIGITAL", inadSqsCaptor.getValue().getAddressType());
        assertNotNull(inadSqsCaptor.getValue().getDigitalAddress());
        assertNull(inadSqsCaptor.getValue().getError());
    }

    @Captor
    ArgumentCaptor<CodeSqsDto> regImpSqsCaptor;

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
        when(sqsService.push(regImpSqsCaptor.capture(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertNotNull(regImpSqsCaptor.getValue().getPhysicalAddress());
        assertEquals("a", regImpSqsCaptor.getValue().getPhysicalAddress().getAddress());
        assertEquals("PHYSICAL", regImpSqsCaptor.getValue().getAddressType());
    }

    @Test
    @DisplayName("Test failed retrieve from Registro Imprese")
    void testRetrieveDigitalOrPhysicalAddressRegImpError() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId("COD_FISCALE_1");
        filterDto.setCorrelationId("correlationId");
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        filterDto.setReferenceRequestDate("refReqDate");
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.setFilter(filterDto);

        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(exception.getResponseBodyAsString()).thenReturn("{ ... }");
        when(exception.getMessage()).thenReturn("message");

        when(infoCamereService.getRegistroImpreseLegalAddress(any()))
                .thenReturn(Mono.error(exception));
        when(sqsService.push(regImpSqsCaptor.capture(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertEquals("PHYSICAL", regImpSqsCaptor.getValue().getAddressType());
        assertNotNull(regImpSqsCaptor.getValue().getError());
        assertNull(regImpSqsCaptor.getValue().getPhysicalAddress());
    }

    @Test
    @DisplayName("Test retrieve from Registro Imprese - CF non trovato")
    void testRetrieveDigitalOrPhysicalAddressRegImpCfNotFound() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId("COD_FISCALE_1");
        filterDto.setCorrelationId("correlationId");
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        filterDto.setReferenceRequestDate("refReqDate");
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.setFilter(filterDto);

        PnNationalRegistriesException exception = new PnNationalRegistriesException("", HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(), null, null, StandardCharsets.UTF_8, InfocamereResponseKO.class);
        when(infoCamereService.getRegistroImpreseLegalAddress(any()))
                .thenReturn(Mono.error(exception));
        when(sqsService.push(regImpSqsCaptor.capture(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        assertNull(regImpSqsCaptor.getValue().getError());
        assertNull(regImpSqsCaptor.getValue().getPhysicalAddress());
        assertEquals("PHYSICAL", regImpSqsCaptor.getValue().getAddressType());
    }

    @Test
    @DisplayName("Test retrieve from IniPEC")
    void testRetrieveDigitalOrPhysicalAddressIniPEC() {
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

        when(infoCamereService.getIniPecDigitalAddress("clientId", getDigitalAddressIniPECRequestBodyDto))
                .thenReturn(Mono.just(new GetDigitalAddressIniPECOKDto()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        StepVerifier.create(addressService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
    }
}
