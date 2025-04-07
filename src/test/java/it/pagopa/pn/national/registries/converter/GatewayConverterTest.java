package it.pagopa.pn.national.registries.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DomicileType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.InternalRecipientAddress;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressesGatewayEvent;
import it.pagopa.pn.national.registries.model.AddressQueryRequest;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.MultiCodeSqsDto;
import it.pagopa.pn.national.registries.model.MultiRecipientCodeSqsDto;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.PhysicalAddress;
import it.pagopa.pn.national.registries.utils.FeatureEnabledUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
        "pn.national.registries.inipec.ttl=0"
})
@ContextConfiguration(classes = GatewayConverter.class)
@ExtendWith(SpringExtension.class)
class GatewayConverterTest {

    private static final String C_ID = "correlationId";
    private static final String CF = "CF";

    @Autowired
    private GatewayConverter gatewayConverter;
    @MockBean
    private ObjectMapper objectMapper;

    @MockBean
    private FeatureEnabledUtils featureEnabledUtils;


    /**
     * Method under test: {@link GatewayConverter#mapToAddressesOKDto(String)}
     */
    @Test
    void testMapToAddressesOKDto() {
        AddressOKDto addressOKDto = gatewayConverter.mapToAddressesOKDto(C_ID);
        assertEquals(C_ID, addressOKDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#anprToSqsDto(String, GetAddressANPROKDto)}
     */
    @Test
    void testAnprToSqsDto1() {
        CodeSqsDto codeSqsDto = gatewayConverter.anprToSqsDto(C_ID, new GetAddressANPROKDto());
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#anprToSqsDto(String, GetAddressANPROKDto)}
     */
    @Test
    void testAnprToSqsDto2() {
        CodeSqsDto codeSqsDto = gatewayConverter.anprToSqsDto(C_ID, null);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getPhysicalAddress());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#anprToSqsDto(String, GetAddressANPROKDto)}
     */
    @Test
    void testAnprToSqsDto3() {
        GetAddressANPROKDto getAddressANPROKDto = new GetAddressANPROKDto();
        getAddressANPROKDto.setResidentialAddresses(List.of(new ResidentialAddressDto()));
        CodeSqsDto codeSqsDto = gatewayConverter.anprToSqsDto(C_ID, getAddressANPROKDto);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNotNull(codeSqsDto.getPhysicalAddress());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#errorAnprToSqsDto(String, Throwable)}
     */
    @Test
    void testErrorAnprToSqsDto1() {
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"codiceErroreAnomalia\": \"EN122\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, AnprResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorAnprToSqsDto(C_ID, exception);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getPhysicalAddress());
    }

    @Test
    void testErrorAnprToSqsDto1bis() {
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"codiceErroreAnomalia\": \"ENX\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, AnprResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorAnprToSqsDto(C_ID, exception);
        assertNull(codeSqsDto);
    }

    /**
     * Method under test: {@link GatewayConverter#errorAnprToSqsDto(String, Throwable)}
     */
    @Test
    @DisplayName("ANPR CF non trovato")
    void testErrorAnprToSqsDto2() {
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"codiceErroreAnomalia\": \"EN122\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, AnprResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorAnprToSqsDto(C_ID, exception);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertNull(codeSqsDto.getError());
        assertNull(codeSqsDto.getPhysicalAddress());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#inadToSqsDto(String, GetDigitalAddressINADOKDto, DigitalAddressRecipientType)}
     */
    @Test
    void testInadToSqsDto1() {
        CodeSqsDto codeSqsDto = gatewayConverter.inadToSqsDto(C_ID, new GetDigitalAddressINADOKDto(), DigitalAddressRecipientType.PERSONA_FISICA);
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertTrue(codeSqsDto.getDigitalAddress().isEmpty());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#inadToSqsDto(String, GetDigitalAddressINADOKDto, DigitalAddressRecipientType)}
     */
    @Test
    void testInadToSqsDto2() {
        CodeSqsDto codeSqsDto = gatewayConverter.inadToSqsDto(C_ID, null, DigitalAddressRecipientType.PERSONA_GIURIDICA);
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertTrue(codeSqsDto.getDigitalAddress().isEmpty());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#inadToSqsDto(String, GetDigitalAddressINADOKDto, DigitalAddressRecipientType)}
     */
    @Test
    void testInadToSqsDto3() {
        GetDigitalAddressINADOKDto getDigitalAddressINADOKDto = new GetDigitalAddressINADOKDto();
        getDigitalAddressINADOKDto.setDigitalAddress(new DigitalAddressDto());
        CodeSqsDto codeSqsDto = gatewayConverter.inadToSqsDto(C_ID, getDigitalAddressINADOKDto, DigitalAddressRecipientType.PERSONA_GIURIDICA);
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertFalse(codeSqsDto.getDigitalAddress().isEmpty());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getError());
    }

    @Test
    void testErrorInadToSqsDto1Bis() {
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"detail\": \"xxx\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, InadResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorInadToSqsDto(C_ID, exception);
        assertNull(codeSqsDto);
    }

    /**
     * Method under test: {@link GatewayConverter#errorInadToSqsDto(String, Throwable)}
     */
    @Test
    @DisplayName("INAD CF non trovato")
    void testErrorInadToSqsDto2() {
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"detail\": \"cf non trovato\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, InadResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorInadToSqsDto(C_ID, exception);
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertNull(codeSqsDto.getError());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertTrue(codeSqsDto.getDigitalAddress().isEmpty());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#regImpToSqsDto(String, GetAddressRegistroImpreseOKDto)}
     */
    @Test
    void testRegImpToSqsDto1() {
        CodeSqsDto codeSqsDto = gatewayConverter.regImpToSqsDto(C_ID, new GetAddressRegistroImpreseOKDto());
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#regImpToSqsDto(String, GetAddressRegistroImpreseOKDto)}
     */
    @Test
    void testRegImpToSqsDto2() {
        CodeSqsDto codeSqsDto = gatewayConverter.regImpToSqsDto(C_ID, null);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#regImpToSqsDto(String, GetAddressRegistroImpreseOKDto)}
     */
    @Test
    void testRegImpToSqsDto3() {
        GetAddressRegistroImpreseOKDto getAddressRegistroImpreseOKDto = new GetAddressRegistroImpreseOKDto();
        getAddressRegistroImpreseOKDto.setProfessionalAddress(new GetAddressRegistroImpreseOKProfessionalAddressDto());
        CodeSqsDto codeSqsDto = gatewayConverter.regImpToSqsDto(C_ID, getAddressRegistroImpreseOKDto);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#ipaToSqsDto(String, IPAPecDto)}
     */
    @Test
    void testIpaToSqsDto() {
        CodeSqsDto actualIpaToSqsDtoResult = gatewayConverter.ipaToSqsDto("42", new IPAPecDto());
        assertEquals(DIGITAL.name(), actualIpaToSqsDtoResult.getAddressType());
        assertEquals("42", actualIpaToSqsDtoResult.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#ipaToSqsDto(String, IPAPecDto)}
     */
    @Test
    void testIpaToSqsDto2() {
        CodeSqsDto actualIpaToSqsDtoResult = (new GatewayConverter()).ipaToSqsDto("foo", null);
        assertEquals(DIGITAL.name(), actualIpaToSqsDtoResult.getAddressType());
        assertEquals("foo", actualIpaToSqsDtoResult.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#ipaToSqsDto(String, IPAPecDto)}
     */
    @Test
    void testIpaToSqsDto3() {

        IPAPecDto ipaResponse = new IPAPecDto();
        ipaResponse.domicilioDigitale("foo");
        CodeSqsDto actualIpaToSqsDtoResult = gatewayConverter.ipaToSqsDto("foo", ipaResponse);
        assertEquals(DIGITAL.name(), actualIpaToSqsDtoResult.getAddressType());
        List<DigitalAddress> digitalAddress = actualIpaToSqsDtoResult.getDigitalAddress();
        assertEquals(1, digitalAddress.size());
        assertEquals("foo", actualIpaToSqsDtoResult.getCorrelationId());
        DigitalAddress getResult = digitalAddress.get(0);
        assertEquals("foo", getResult.getAddress());
        assertEquals(IpaConverter.ADDRESS_TYPE, getResult.getType());
        assertEquals("IMPRESA", getResult.getRecipient());
    }

    /**
     * Method under test: {@link GatewayConverter#ipaToSqsDto(String, IPAPecDto)}
     */
    @Test
    void testIpaToSqsDto4() {
        CodeSqsDto actualIpaToSqsDtoResult = gatewayConverter.ipaToSqsDto("42", new IPAPecDto());
        assertEquals(DIGITAL.name(), actualIpaToSqsDtoResult.getAddressType());
        assertEquals("42", actualIpaToSqsDtoResult.getCorrelationId());
    }


    /**
     * Method under test: {@link GatewayConverter#errorRegImpToSqsDto(String, Throwable)}
     */
    @Test
    void testErrorRegImpToSqsDto1() {
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "OPS".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, InfocamereResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorRegImpToSqsDto(C_ID, exception);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals("message", codeSqsDto.getError());
        assertNull(codeSqsDto.getPhysicalAddress());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#newCodeSqsDto(String)}
     */
    @Test
    void testNewCodeSqsDto() {
        CodeSqsDto codeSqsDto = gatewayConverter.newCodeSqsDto(C_ID);
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#convertAnprToPhysicalAddress(ResidentialAddressDto)}
     */
    @Test
    void testConvertAnprToPhysicalAddress() {
        ResidentialAddressDto residentialAddressDto = new ResidentialAddressDto();
        residentialAddressDto.setAddress("address");
        residentialAddressDto.setZip("zip");
        residentialAddressDto.setProvince("province");
        residentialAddressDto.setMunicipalityDetails("municipalityDetails");
        residentialAddressDto.setMunicipality("municipality");
        residentialAddressDto.setForeignState("foreignState");
        residentialAddressDto.setAt("at");
        PhysicalAddress physicalAddress = gatewayConverter.convertAnprToPhysicalAddress(residentialAddressDto);
        assertEquals("address", physicalAddress.getAddress());
        assertEquals("zip", physicalAddress.getZip());
        assertEquals("province", physicalAddress.getProvince());
        assertEquals("municipalityDetails", physicalAddress.getMunicipalityDetails());
        assertEquals("municipality", physicalAddress.getMunicipality());
        assertEquals("foreignState", physicalAddress.getForeignState());
        assertEquals("at", physicalAddress.getAt());
    }

    /**
     * Method under test: {@link GatewayConverter#convertInadToDigitalAddress(DigitalAddressDto, it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType)}
     */
    @Test
    void testConvertInadToDigitalAddress() {
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("digitalAddress");
        DigitalAddress digitalAddress = gatewayConverter.convertInadToDigitalAddress(digitalAddressDto, DigitalAddressRecipientType.PERSONA_GIURIDICA);
        assertEquals("digitalAddress", digitalAddress.getAddress());
        assertEquals("PEC", digitalAddress.getType());
        assertEquals("PERSONA_GIURIDICA", digitalAddress.getRecipient());
    }

    /**
     * Method under test: {@link GatewayConverter#convertInadToDigitalAddress(DigitalAddressDto, it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType)}
     */
    @Test
    void testConvertInadToDigitalAddress2() {
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("digitalAddress");
        DigitalAddress digitalAddress = gatewayConverter.convertInadToDigitalAddress(digitalAddressDto, DigitalAddressRecipientType.PERSONA_FISICA);
        assertEquals("digitalAddress", digitalAddress.getAddress());
        assertEquals("PEC", digitalAddress.getType());
        assertEquals("PERSONA_FISICA", digitalAddress.getRecipient());
    }

    /**
     * Method under test: {@link GatewayConverter#convertRegImpToPhysicalAddress(GetAddressRegistroImpreseOKProfessionalAddressDto)}
     */
    @Test
    void testConvertRegImpToPhysicalAddress() {
        GetAddressRegistroImpreseOKProfessionalAddressDto registroImpreseDto = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        registroImpreseDto.setAddress("address");
        registroImpreseDto.setProvince("province");
        registroImpreseDto.setDescription("description");
        registroImpreseDto.setZip("zip");
        registroImpreseDto.setMunicipality("municipality");
        PhysicalAddress physicalAddress = gatewayConverter.convertRegImpToPhysicalAddress(registroImpreseDto);
        assertEquals("address", physicalAddress.getAddress());
        assertEquals("zip", physicalAddress.getZip());
        assertEquals("province", physicalAddress.getProvince());
        assertEquals("municipality", physicalAddress.getMunicipality());
    }

    /**
     * Method under test: {@link GatewayConverter#convertToGetAddressAnprRequest(AddressRequestBodyDto)}
     */
    @Test
    void testConvertToGetAddressAnprRequest() {
        AddressRequestBodyDto requestBodyDto = newAddressRequestBodyDto();
        GetAddressANPRRequestBodyDto anprRequestBodyDto = gatewayConverter.convertToGetAddressAnprRequest(requestBodyDto);
        assertNotNull(anprRequestBodyDto.getFilter());
        assertEquals(CF, anprRequestBodyDto.getFilter().getTaxId());
        assertEquals(C_ID, anprRequestBodyDto.getFilter().getRequestReason());
        assertEquals("2023-02-16", anprRequestBodyDto.getFilter().getReferenceRequestDate());
    }

    /**
     * Method under test: {@link GatewayConverter#convertToGetDigitalAddressInadRequest(AddressRequestBodyDto)}
     */
    @Test
    void testConvertToGetDigitalAddressInadRequest() {
        AddressRequestBodyDto requestBodyDto = newAddressRequestBodyDto();
        GetDigitalAddressINADRequestBodyDto inadRequestBodyDto = gatewayConverter.convertToGetDigitalAddressInadRequest(requestBodyDto);
        assertNotNull(inadRequestBodyDto.getFilter());
        assertEquals(CF, inadRequestBodyDto.getFilter().getTaxId());
        assertEquals(C_ID, inadRequestBodyDto.getFilter().getPracticalReference());
    }

    /**
     * Method under test: {@link GatewayConverter#convertToGetAddressRegistroImpreseRequest(AddressRequestBodyDto)}
     */
    @Test
    void testConvertToGetAddressRegistroImpreseRequest() {
        AddressRequestBodyDto requestBodyDto = newAddressRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyDto regImpRequestBodyDto = gatewayConverter.convertToGetAddressRegistroImpreseRequest(requestBodyDto);
        assertNotNull(regImpRequestBodyDto.getFilter());
        assertEquals(CF, regImpRequestBodyDto.getFilter().getTaxId());
    }

    /**
     * Method under test: {@link GatewayConverter#convertToGetDigitalAddressIniPecRequest(AddressRequestBodyDto)}
     */
    @Test
    void testConvertToGetDigitalAddressIniPecRequest() {
        AddressRequestBodyDto requestBodyDto = newAddressRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyDto iniPecRequestBodyDto = gatewayConverter.convertToGetDigitalAddressIniPecRequest(requestBodyDto);
        assertNotNull(iniPecRequestBodyDto.getFilter());
        assertEquals(CF, iniPecRequestBodyDto.getFilter().getTaxId());
        assertEquals(C_ID, iniPecRequestBodyDto.getFilter().getCorrelationId());
    }

/*    @Test
    void testConvertToGetIpaPecRequest(){
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        AddressRequestBodyFilterDto addressRequestBodyFilterDto = new AddressRequestBodyFilterDto();
        addressRequestBodyFilterDto.setTaxId("taxId");
        addressRequestBodyDto.setFilter(addressRequestBodyFilterDto);
        assertNotNull(gatewayConverter.convertToGetIpaPecRequest(addressRequestBodyDto));
    }*/

    @Test
    void testConvertToGetDigitalAddressInadRequest2(){
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setCf("cf");
        assertNotNull(gatewayConverter.convertToGetDigitalAddressInadRequest(batchRequest));
    }

    private AddressRequestBodyDto newAddressRequestBodyDto() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId(CF);
        filterDto.setCorrelationId(C_ID);
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        filterDto.setReferenceRequestDate(Date.from(LocalDate.of(2023, 2, 16).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        AddressRequestBodyDto requestBodyDto = new AddressRequestBodyDto();
        requestBodyDto.setFilter(filterDto);
        return requestBodyDto;
    }


    @Test
    void testConvertCodeSqsDtoToString() throws JsonProcessingException {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        when(objectMapper.writeValueAsString(codeSqsDto))
                .thenReturn("string");
        assertEquals("string", gatewayConverter.convertCodeSqsDtoToString(codeSqsDto));
    }

    @Test
    void testConvertCodeSqsDtoToStringError() throws JsonProcessingException {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        when(objectMapper.writeValueAsString(codeSqsDto))
                .thenThrow(JsonProcessingException.class);
        assertThrows(DigitalAddressException.class, () -> gatewayConverter.convertCodeSqsDtoToString(codeSqsDto));
    }



    @Test
    void testEmailValidationWithValidEmail() {
        // Arrange
        GatewayConverter gatewayConverter = new GatewayConverter();
        GetDigitalAddressINADOKDto inadResponse = new GetDigitalAddressINADOKDto();
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("valid@example.com");
        inadResponse.setDigitalAddress(digitalAddressDto);

        // Act
        Mono<GetDigitalAddressINADOKDto> result = gatewayConverter.emailValidation(inadResponse);

        // Assert
        StepVerifier.create(result)
                .expectNext(inadResponse)
                .verifyComplete();
    }

    @Test
    void testEmailValidationWithInvalidEmail() {
        // Arrange
        GetDigitalAddressINADOKDto inadResponse = new GetDigitalAddressINADOKDto();
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("invalid-email");
        inadResponse.setDigitalAddress(digitalAddressDto);

        // Act
        Mono<GetDigitalAddressINADOKDto> result = gatewayConverter.emailValidation(inadResponse);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PnNationalRegistriesException &&
                        throwable.getMessage().equals("CF non trovato") &&
                        ((PnNationalRegistriesException) throwable).getStatusCode().value() == HttpStatus.NOT_FOUND.value())
                .verify();
    }

    // START METHODS TESTS FOR MULTI ADDRESSES
    @Test
    void testToAddressQueryRequests() {
        InternalRecipientAddress internalRecipientAddress1 = InternalRecipientAddress.builder()
                .taxId("taxId1")
                .recipientType(RecipientType.PF.name())
                .domicileType(DomicileType.PHYSICAL.name())
                .recIndex(0)
                .build();

        InternalRecipientAddress internalRecipientAddress2 = InternalRecipientAddress.builder()
                .taxId("taxId2")
                .recipientType(RecipientType.PG.name())
                .domicileType(DomicileType.PHYSICAL.name())
                .recIndex(1)
                .build();

        PnAddressesGatewayEvent.Payload payload = PnAddressesGatewayEvent.Payload.builder()
                .correlationId("test-correlation-id")
                .referenceRequestDate(new java.util.Date())
                .pnNationalRegistriesCxId("test-cx-id")
                .internalRecipientAdresses(List.of(internalRecipientAddress1, internalRecipientAddress2))
                .build();

        List<AddressQueryRequest> result = gatewayConverter.toAddressQueryRequests(payload);

        assertNotNull(result);
        assertEquals(2, result.size());

        AddressQueryRequest request1 = result.get(0);
        assertEquals("test-correlation-id", request1.getCorrelationId());
        assertEquals("test-cx-id", request1.getPnNationalRegistriesCxId());
        assertEquals("taxId1", request1.getTaxId());
        assertEquals(RecipientType.PF, request1.getRecipientType());
        assertEquals(DomicileType.PHYSICAL, request1.getDomicileType());
        assertEquals(0, request1.getRecIndex());

        AddressQueryRequest request2 = result.get(1);
        assertEquals("test-correlation-id", request2.getCorrelationId());
        assertEquals("test-cx-id", request2.getPnNationalRegistriesCxId());
        assertEquals("taxId2", request2.getTaxId());
        assertEquals(RecipientType.PG, request2.getRecipientType());
        assertEquals(DomicileType.PHYSICAL, request2.getDomicileType());
        assertEquals(1, request2.getRecIndex());
    }

    @Test
    void testConvertToGetPhysicalAddressAnprRequest() {
        AddressQueryRequest addressQueryRequest = getAddressQueryRequest1();

        GetAddressANPRRequestBodyDto result = gatewayConverter.convertToGetAddressAnprRequest(addressQueryRequest);

        assertNotNull(result);
        assertNotNull(result.getFilter());
        assertEquals("test-correlation-id", result.getFilter().getRequestReason());
        assertEquals("test-tax-id", result.getFilter().getTaxId());
        assertEquals("2023-02-16", result.getFilter().getReferenceRequestDate());
    }

    @Test
    void testConvertAnprResponseToInternalRecipientAddress() {
        AddressQueryRequest addressQueryRequest = getAddressQueryRequest1();

        ResidentialAddressDto residentialAddressDto = new ResidentialAddressDto();
        residentialAddressDto.setAddress("test-address");
        residentialAddressDto.setZip("test-zip");
        residentialAddressDto.setProvince("test-province");
        residentialAddressDto.setMunicipality("test-municipality");

        GetAddressANPROKDto response = new GetAddressANPROKDto();
        response.setResidentialAddresses(List.of(residentialAddressDto));

        MultiCodeSqsDto.PhysicalAddressSQSMessage result = gatewayConverter.convertAnprResponseToInternalRecipientAddress(response, addressQueryRequest);

        assertNotNull(result);
        assertNotNull(result.getPhysicalAddress());
        assertEquals("test-address", result.getPhysicalAddress().getAddress());
        assertEquals("test-zip", result.getPhysicalAddress().getZip());
        assertEquals("test-province", result.getPhysicalAddress().getProvince());
        assertEquals("test-municipality", result.getPhysicalAddress().getMunicipality());
        assertEquals(0, result.getRecIndex());
        assertEquals("ANPR", result.getRegistry());
    }

    @Test
    void testConvertAnprResponseToInternalRecipientAddressWithNoResidentialAddresses() {
        GetAddressANPROKDto response = new GetAddressANPROKDto();
        response.setResidentialAddresses(Collections.emptyList());

        AddressQueryRequest addressQueryRequest = AddressQueryRequest.builder()
                .correlationId("test-correlation-id")
                .pnNationalRegistriesCxId("test-cx-id")
                .referenceRequestDate(new java.util.Date())
                .taxId("test-tax-id")
                .recipientType(RecipientType.PF)
                .recIndex(0)
                .domicileType(DomicileType.PHYSICAL)
                .build();

        MultiCodeSqsDto.PhysicalAddressSQSMessage result = gatewayConverter.convertAnprResponseToInternalRecipientAddress(response, addressQueryRequest);

        assertNull(result.getPhysicalAddress());
        assertEquals(0, result.getRecIndex());
        assertEquals("ANPR", result.getRegistry());
    }

    @Test
    void testAnprNotFoundErrorToPhysicalAddressSQSMessage() {
        AddressQueryRequest addressQueryRequest = getAddressQueryRequest1();

        MultiCodeSqsDto.PhysicalAddressSQSMessage result = gatewayConverter.anprNotFoundErrorToPhysicalAddressSQSMessage(addressQueryRequest);

        assertNotNull(result);
        assertEquals(0, result.getRecIndex());
        assertEquals("ANPR", result.getRegistry());
        assertNull(result.getPhysicalAddress());
    }

    @Test
    void testConvertToGetPhysicalAddressRegistroImpreseRequest() {
        AddressQueryRequest addressQueryRequest = getAddressQueryRequest1();

        // Act
        GetAddressRegistroImpreseRequestBodyDto result = gatewayConverter.convertToGetAddressRegistroImpreseRequest(addressQueryRequest);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getFilter());
        assertEquals("test-tax-id", result.getFilter().getTaxId());
    }

    @Test
    void testConvertRegImprResponseToInternalRecipientAddress() {
        AddressQueryRequest addressQueryRequest = getAddressQueryRequest1();

        GetAddressRegistroImpreseOKProfessionalAddressDto professionalAddressDto = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        professionalAddressDto.setAddress("test-address");
        professionalAddressDto.setZip("test-zip");
        professionalAddressDto.setProvince("test-province");
        professionalAddressDto.setMunicipality("test-municipality");

        GetAddressRegistroImpreseOKDto response = new GetAddressRegistroImpreseOKDto();
        response.setProfessionalAddress(professionalAddressDto);

        MultiCodeSqsDto.PhysicalAddressSQSMessage result = gatewayConverter.convertRegImprResponseToInternalRecipientAddress(response, addressQueryRequest);

        assertNotNull(result);
        assertNotNull(result.getPhysicalAddress());
        assertEquals("test-address", result.getPhysicalAddress().getAddress());
        assertEquals("test-zip", result.getPhysicalAddress().getZip());
        assertEquals("test-province", result.getPhysicalAddress().getProvince());
        assertEquals("test-municipality", result.getPhysicalAddress().getMunicipality());
        assertEquals(0, result.getRecIndex());
        assertEquals("REGISTRO_IMPRESE", result.getRegistry());
    }

    @Test
    void testConvertRegImprResponseToInternalRecipientAddressWithNoProfessionalAddress() {
        GetAddressRegistroImpreseOKDto response = new GetAddressRegistroImpreseOKDto();
        response.setProfessionalAddress(null);

        AddressQueryRequest addressQueryRequest = AddressQueryRequest.builder()
                .correlationId("test-correlation-id")
                .pnNationalRegistriesCxId("test-cx-id")
                .referenceRequestDate(new java.util.Date())
                .taxId("test-tax-id")
                .recipientType(RecipientType.PG)
                .recIndex(0)
                .domicileType(DomicileType.PHYSICAL)
                .build();

        MultiCodeSqsDto.PhysicalAddressSQSMessage result = gatewayConverter.convertRegImprResponseToInternalRecipientAddress(response, addressQueryRequest);

        assertNull(result.getPhysicalAddress());
        assertEquals(0, result.getRecIndex());
        assertEquals("REGISTRO_IMPRESE", result.getRegistry());
    }

    @Test
    void testConvertToMultiCodeSqsDto() {
        String correlationId = "test-correlation-id";
        MultiCodeSqsDto.PhysicalAddressSQSMessage message1 = new MultiCodeSqsDto.PhysicalAddressSQSMessage();
        message1.setRecIndex(1);
        message1.setRegistry("ANPR");

        MultiCodeSqsDto.PhysicalAddressSQSMessage message2 = new MultiCodeSqsDto.PhysicalAddressSQSMessage();
        message2.setRecIndex(2);
        message2.setRegistry("REGISTRO_IMPRESE");

        List<MultiCodeSqsDto.PhysicalAddressSQSMessage> addresses = List.of(message1, message2);

        MultiCodeSqsDto result = gatewayConverter.convertToMultiCodeSqsDto(addresses, correlationId);

        assertNotNull(result);
        assertEquals(correlationId, result.getCorrelationId());
        assertEquals("PHYSICAL", result.getAddressType());
        assertEquals(2, result.getAddresses().size());
        assertEquals(1, result.getAddresses().get(0).getRecIndex());
        assertEquals("ANPR", result.getAddresses().get(0).getRegistry());
        assertEquals(2, result.getAddresses().get(1).getRecIndex());
        assertEquals("REGISTRO_IMPRESE", result.getAddresses().get(1).getRegistry());
    }

    @Test
    void testConvertToMultiRecipientCodeSqsDto() {
        AddressQueryRequest addressQueryRequest1 = getAddressQueryRequest1();

        List<AddressQueryRequest> addressQueryRequests = List.of(
                addressQueryRequest1,
                AddressQueryRequest.builder()
                .correlationId("test-correlation-id")
                .pnNationalRegistriesCxId("test-cx-id")
                .taxId("test-tax-id-2")
                .recIndex(1)
                .recipientType(RecipientType.PG)
                .domicileType(DomicileType.PHYSICAL)
                .referenceRequestDate(Date.from(LocalDate.of(2023, 2, 16).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .build());

        MultiRecipientCodeSqsDto result = gatewayConverter.convertToMultiRecipientCodeSqsDto(addressQueryRequests);

        assertNotNull(result);
        assertEquals("test-correlation-id", result.getCorrelationId());
        assertEquals("test-cx-id", result.getPnNationalRegistriesCxId());
        assertEquals(Date.from(LocalDate.of(2023, 2, 16).atStartOfDay(ZoneId.systemDefault()).toInstant()), result.getReferenceRequestDate());
        assertEquals(2, result.getInternalRecipientAdresses().size());
        assertEquals(0, result.getInternalRecipientAdresses().get(0).getRecIndex());
        assertEquals("test-tax-id", result.getInternalRecipientAdresses().get(0).getTaxId());
        assertEquals("PHYSICAL", result.getInternalRecipientAdresses().get(0).getDomicileType());
        assertEquals(1, result.getInternalRecipientAdresses().get(1).getRecIndex());
        assertEquals("test-tax-id-2", result.getInternalRecipientAdresses().get(1).getTaxId());
        assertEquals("PHYSICAL", result.getInternalRecipientAdresses().get(1).getDomicileType());
    }

    private static AddressQueryRequest getAddressQueryRequest1() {
        return AddressQueryRequest.builder()
                .correlationId("test-correlation-id")
                .pnNationalRegistriesCxId("test-cx-id")
                .taxId("test-tax-id")
                .recIndex(0)
                .recipientType(RecipientType.PF)
                .domicileType(DomicileType.PHYSICAL)
                .referenceRequestDate(Date.from(LocalDate.of(2023, 2, 16).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .build();
    }

    // END METHODS TESTS FOR MULTI ADDRESSES

}