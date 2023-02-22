package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.PhysicalAddress;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GatewayConverterTest {

    private static final String C_ID = "correlationId";
    private static final String CF = "CF";

    /**
     * Method under test: {@link GatewayConverter#mapToAddressesOKDto(String)}
     */
    @Test
    void testMapToAddressesOKDto() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        AddressOKDto addressOKDto = gatewayConverter.mapToAddressesOKDto(C_ID);
        assertEquals(C_ID, addressOKDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#anprToSqsDto(String, String, GetAddressANPROKDto)}
     */
    @Test
    void testAnprToSqsDto1() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.anprToSqsDto(C_ID, CF, new GetAddressANPROKDto());
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#anprToSqsDto(String, String, GetAddressANPROKDto)}
     */
    @Test
    void testAnprToSqsDto2() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.anprToSqsDto(C_ID, CF, null);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getPhysicalAddress());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#anprToSqsDto(String, String, GetAddressANPROKDto)}
     */
    @Test
    void testAnprToSqsDto3() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        GetAddressANPROKDto getAddressANPROKDto = new GetAddressANPROKDto();
        getAddressANPROKDto.setResidentialAddresses(List.of(new ResidentialAddressDto()));
        CodeSqsDto codeSqsDto = gatewayConverter.anprToSqsDto(C_ID, CF, getAddressANPROKDto);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNotNull(codeSqsDto.getPhysicalAddress());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#errorAnprToSqsDto(String, String, Throwable)}
     */
    @Test
    void testErrorAnprToSqsDto1() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"codiceErroreAnomalia\": \"ENX\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, AnprResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorAnprToSqsDto(C_ID, CF, exception);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertEquals("message", codeSqsDto.getError());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getPhysicalAddress());
    }

    /**
     * Method under test: {@link GatewayConverter#errorAnprToSqsDto(String, String, Throwable)}
     */
    @Test
    @DisplayName("ANPR CF non trovato")
    void testErrorAnprToSqsDto2() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"codiceErroreAnomalia\": \"EN122\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, AnprResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorAnprToSqsDto(C_ID, CF, exception);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertNull(codeSqsDto.getError());
        assertNull(codeSqsDto.getPhysicalAddress());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#inadToSqsDto(String, String, GetDigitalAddressINADOKDto)}
     */
    @Test
    void testInadToSqsDto1() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.inadToSqsDto(C_ID, CF, new GetDigitalAddressINADOKDto());
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertTrue(codeSqsDto.getDigitalAddress().isEmpty());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#inadToSqsDto(String, String, GetDigitalAddressINADOKDto)}
     */
    @Test
    void testInadToSqsDto2() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.inadToSqsDto(C_ID, CF, null);
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertTrue(codeSqsDto.getDigitalAddress().isEmpty());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#inadToSqsDto(String, String, GetDigitalAddressINADOKDto)}
     */
    @Test
    void testInadToSqsDto3() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        GetDigitalAddressINADOKDto getDigitalAddressINADOKDto = new GetDigitalAddressINADOKDto();
        getDigitalAddressINADOKDto.setDigitalAddress(List.of(new DigitalAddressDto()));
        CodeSqsDto codeSqsDto = gatewayConverter.inadToSqsDto(C_ID, CF, getDigitalAddressINADOKDto);
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertFalse(codeSqsDto.getDigitalAddress().isEmpty());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#errorInadToSqsDto(String, String, Throwable)}
     */
    @Test
    void testErrorInadToSqsDto1() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"detail\": \"xxx\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, InadResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorInadToSqsDto(C_ID, CF, exception);
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertEquals("message", codeSqsDto.getError());
        assertNull(codeSqsDto.getDigitalAddress());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#errorInadToSqsDto(String, String, Throwable)}
     */
    @Test
    @DisplayName("INAD CF non trovato")
    void testErrorInadToSqsDto2() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"detail\": \"cf non trovato\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, InadResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorInadToSqsDto(C_ID, CF, exception);
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertNull(codeSqsDto.getError());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertTrue(codeSqsDto.getDigitalAddress().isEmpty());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#regImpToSqsDto(String, String, GetAddressRegistroImpreseOKDto)}
     */
    @Test
    void testRegImpToSqsDto1() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.regImpToSqsDto(C_ID, CF, new GetAddressRegistroImpreseOKDto());
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#regImpToSqsDto(String, String, GetAddressRegistroImpreseOKDto)}
     */
    @Test
    void testRegImpToSqsDto2() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.regImpToSqsDto(C_ID, CF, null);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#regImpToSqsDto(String, String, GetAddressRegistroImpreseOKDto)}
     */
    @Test
    void testRegImpToSqsDto3() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        GetAddressRegistroImpreseOKDto getAddressRegistroImpreseOKDto = new GetAddressRegistroImpreseOKDto();
        getAddressRegistroImpreseOKDto.setProfessionalAddress(new GetAddressRegistroImpreseOKProfessionalAddressDto());
        CodeSqsDto codeSqsDto = gatewayConverter.regImpToSqsDto(C_ID, CF, getAddressRegistroImpreseOKDto);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(CF, codeSqsDto.getTaxId());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#newCodeSqsDto(String, String)}
     */
    @Test
    void testNewCodeSqsDto() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.newCodeSqsDto(C_ID, CF);
        assertEquals(CF, codeSqsDto.getTaxId());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#convertAnprToPhysicalAddress(ResidentialAddressDto)}
     */
    @Test
    void testConvertAnprToPhysicalAddress() {
        GatewayConverter gatewayConverter = new GatewayConverter();
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
     * Method under test: {@link GatewayConverter#convertInadToDigitalAddress(DigitalAddressDto)}
     */
    @Test
    void testConvertInadToDigitalAddress() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("digitalAddress");
        DigitalAddress digitalAddress = gatewayConverter.convertInadToDigitalAddress(digitalAddressDto);
        assertEquals("digitalAddress", digitalAddress.getAddress());
        assertEquals("PEC", digitalAddress.getType());
        assertEquals("PERSONA_FISICA", digitalAddress.getRecipient());
    }

    /**
     * Method under test: {@link GatewayConverter#convertRegImpToPhysicalAddress(GetAddressRegistroImpreseOKProfessionalAddressDto)}
     */
    @Test
    void testConvertRegImpToPhysicalAddress() {
        GatewayConverter gatewayConverter = new GatewayConverter();
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
        GatewayConverter gatewayConverter = new GatewayConverter();
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
        GatewayConverter gatewayConverter = new GatewayConverter();
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
        GatewayConverter gatewayConverter = new GatewayConverter();
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
        GatewayConverter gatewayConverter = new GatewayConverter();
        AddressRequestBodyDto requestBodyDto = newAddressRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyDto iniPecRequestBodyDto = gatewayConverter.convertToGetDigitalAddressIniPecRequest(requestBodyDto);
        assertNotNull(iniPecRequestBodyDto.getFilter());
        assertEquals(CF, iniPecRequestBodyDto.getFilter().getTaxId());
        assertEquals(C_ID, iniPecRequestBodyDto.getFilter().getCorrelationId());
    }

    private AddressRequestBodyDto newAddressRequestBodyDto() {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId(CF);
        filterDto.setCorrelationId(C_ID);
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        filterDto.setReferenceRequestDate("2023-02-16");
        AddressRequestBodyDto requestBodyDto = new AddressRequestBodyDto();
        requestBodyDto.setFilter(filterDto);
        return requestBodyDto;
    }
}