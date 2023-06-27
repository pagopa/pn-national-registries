package it.pagopa.pn.national.registries.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.PhysicalAddress;
import it.pagopa.pn.national.registries.repository.CounterRepositoryImpl;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepositoryImpl;
import it.pagopa.pn.national.registries.service.AnprService;
import it.pagopa.pn.national.registries.service.GatewayService;
import it.pagopa.pn.national.registries.service.InadService;
import it.pagopa.pn.national.registries.service.InfoCamereService;
import it.pagopa.pn.national.registries.service.IpaService;
import it.pagopa.pn.national.registries.service.SqsService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
     * Method under test: {@link GatewayConverter#anprToSqsDto(String, GetAddressANPROKDto)}
     */
    @Test
    void testAnprToSqsDto1() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.anprToSqsDto(C_ID, new GetAddressANPROKDto());
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#anprToSqsDto(String, GetAddressANPROKDto)}
     */
    @Test
    void testAnprToSqsDto2() {
        GatewayConverter gatewayConverter = new GatewayConverter();
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
        GatewayConverter gatewayConverter = new GatewayConverter();
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
        GatewayConverter gatewayConverter = new GatewayConverter();
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"codiceErroreAnomalia\": \"ENX\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, AnprResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorAnprToSqsDto(C_ID, exception);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals("message", codeSqsDto.getError());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getPhysicalAddress());
    }

    /**
     * Method under test: {@link GatewayConverter#errorAnprToSqsDto(String, Throwable)}
     */
    @Test
    @DisplayName("ANPR CF non trovato")
    void testErrorAnprToSqsDto2() {
        GatewayConverter gatewayConverter = new GatewayConverter();
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
     * Method under test: {@link GatewayConverter#inadToSqsDto(String, GetDigitalAddressINADOKDto)}
     */
    @Test
    void testInadToSqsDto1() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.inadToSqsDto(C_ID, new GetDigitalAddressINADOKDto());
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertTrue(codeSqsDto.getDigitalAddress().isEmpty());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#inadToSqsDto(String, GetDigitalAddressINADOKDto)}
     */
    @Test
    void testInadToSqsDto2() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.inadToSqsDto(C_ID, null);
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertTrue(codeSqsDto.getDigitalAddress().isEmpty());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#inadToSqsDto(String, GetDigitalAddressINADOKDto)}
     */
    @Test
    void testInadToSqsDto3() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        GetDigitalAddressINADOKDto getDigitalAddressINADOKDto = new GetDigitalAddressINADOKDto();
        getDigitalAddressINADOKDto.setDigitalAddress(List.of(new DigitalAddressDto()));
        CodeSqsDto codeSqsDto = gatewayConverter.inadToSqsDto(C_ID, getDigitalAddressINADOKDto);
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertNotNull(codeSqsDto.getDigitalAddress());
        assertFalse(codeSqsDto.getDigitalAddress().isEmpty());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
        assertNull(codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#errorInadToSqsDto(String, Throwable)}
     */
    @Test
    void testErrorInadToSqsDto1() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), null,
                "{ ... \"detail\": \"xxx\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, InadResponseKO.class);
        CodeSqsDto codeSqsDto = gatewayConverter.errorInadToSqsDto(C_ID, exception);
        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertEquals("message", codeSqsDto.getError());
        assertNull(codeSqsDto.getDigitalAddress());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#errorInadToSqsDto(String, Throwable)}
     */
    @Test
    @DisplayName("INAD CF non trovato")
    void testErrorInadToSqsDto2() {
        GatewayConverter gatewayConverter = new GatewayConverter();
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
     * Method under test: {@link GatewayConverter#errorIpaToSqsDto(String, Throwable)}
     */
    @Test
    void testErrorIpaToSqsDto() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto actualErrorIpaToSqsDtoResult = gatewayConverter.errorIpaToSqsDto("42", new Throwable());
        assertEquals("DIGITAL", actualErrorIpaToSqsDtoResult.getAddressType());
        assertNull(actualErrorIpaToSqsDtoResult.getError());
        assertEquals("42", actualErrorIpaToSqsDtoResult.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#errorIpaToSqsDto(String, Throwable)}
     */
    @Test
    void testErrorIpaToSqsDto2() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        PnNationalRegistriesException exception = new PnNationalRegistriesException("message",
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null,
                "{ ... \"detail\": \"xxx\", ...".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, IPAPecErrorDto.class);

        CodeSqsDto codeSqsDto = gatewayConverter.errorIpaToSqsDto(C_ID, exception);

        assertEquals("DIGITAL", codeSqsDto.getAddressType());
    }

    /**
     * Method under test: {@link GatewayConverter#errorIpaToSqsDto(String, Throwable)}
     */
    @Test
    void testErrorIpaToSqsDto3() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        Throwable exception = new Throwable("message");

        CodeSqsDto codeSqsDto = gatewayConverter.errorIpaToSqsDto(C_ID, exception);

        assertEquals("DIGITAL", codeSqsDto.getAddressType());
        assertEquals("message", codeSqsDto.getError());
    }

    /**
     * Method under test: {@link GatewayConverter#regImpToSqsDto(String, GetAddressRegistroImpreseOKDto)}
     */
    @Test
    void testRegImpToSqsDto1() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.regImpToSqsDto(C_ID, new GetAddressRegistroImpreseOKDto());
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#regImpToSqsDto(String, GetAddressRegistroImpreseOKDto)}
     */
    @Test
    void testRegImpToSqsDto2() {
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.regImpToSqsDto(C_ID, null);
        assertEquals("PHYSICAL", codeSqsDto.getAddressType());
        assertEquals(C_ID, codeSqsDto.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#regImpToSqsDto(String, GetAddressRegistroImpreseOKDto)}
     */
    @Test
    void testRegImpToSqsDto3() {
        GatewayConverter gatewayConverter = new GatewayConverter();
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
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto actualIpaToSqsDtoResult = gatewayConverter.ipaToSqsDto("42", new IPAPecDto());
        assertEquals("PHYSICAL", actualIpaToSqsDtoResult.getAddressType());
        assertEquals("42", actualIpaToSqsDtoResult.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#ipaToSqsDto(String, IPAPecDto)}
     */
    @Test
    void testIpaToSqsDto2() {
        CodeSqsDto actualIpaToSqsDtoResult = (new GatewayConverter()).ipaToSqsDto("foo", null);
        assertEquals("PHYSICAL", actualIpaToSqsDtoResult.getAddressType());
        assertEquals("foo", actualIpaToSqsDtoResult.getCorrelationId());
    }

    /**
     * Method under test: {@link GatewayConverter#ipaToSqsDto(String, IPAPecDto)}
     */
    @Test
    void testIpaToSqsDto3() {
        GatewayConverter gatewayConverter = new GatewayConverter();

        IPAPecDto ipaResponse = new IPAPecDto();
        ipaResponse.domicilioDigitale("foo");
        CodeSqsDto actualIpaToSqsDtoResult = gatewayConverter.ipaToSqsDto("foo", ipaResponse);
        assertEquals("PHYSICAL", actualIpaToSqsDtoResult.getAddressType());
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
        DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient = mock(DynamoDbEnhancedAsyncClient.class);
        when(dynamoDbEnhancedAsyncClient.table(Mockito.<String>any(), Mockito.<TableSchema<Object>>any())).thenReturn(
                new DynamoDbAsyncTableDecorator<>(new DynamoDbAsyncTableDecorator<>(new DynamoDbAsyncTableDecorator<>(
                        new DynamoDbAsyncTableDecorator<>(new DynamoDbAsyncTableDecorator<>(mock(DynamoDbAsyncTable.class)))))));
        CounterRepositoryImpl counterRepository = new CounterRepositoryImpl(dynamoDbEnhancedAsyncClient,
                "correlationId: {} - IPA - WS23 - domicili digitali non presenti");

        ValidateTaxIdUtils validateTaxIdUtils = mock(ValidateTaxIdUtils.class);
        AnprService anprService = new AnprService(new AnprConverter(), mock(AnprClient.class),
                "correlationId: {} - IPA - WS23 - domicili digitali non presenti", counterRepository, validateTaxIdUtils);

        DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient2 = mock(DynamoDbEnhancedAsyncClient.class);
        when(dynamoDbEnhancedAsyncClient2.table(Mockito.<String>any(), Mockito.<TableSchema<Object>>any())).thenReturn(
                new DynamoDbAsyncTableDecorator<>(new DynamoDbAsyncTableDecorator<>(new DynamoDbAsyncTableDecorator<>(
                        new DynamoDbAsyncTableDecorator<>(new DynamoDbAsyncTableDecorator<>(mock(DynamoDbAsyncTable.class)))))));
        IniPecBatchRequestRepositoryImpl iniPecBatchRequestRepository = new IniPecBatchRequestRepositoryImpl(
                dynamoDbEnhancedAsyncClient2, 3, 2);

        InfoCamereClient infoCamereClient = mock(InfoCamereClient.class);
        InfoCamereService infoCamereService = new InfoCamereService(infoCamereClient,
                new InfoCamereConverter(new ObjectMapper(), 2L), iniPecBatchRequestRepository, 2L, validateTaxIdUtils);

        InadService inadService = new InadService(mock(InadClient.class), validateTaxIdUtils);
        IpaService ipaService = new IpaService(new IpaConverter(), mock(IpaClient.class), validateTaxIdUtils);

        SqsClient sqsClient = mock(SqsClient.class);
        GatewayService gatewayService = new GatewayService(anprService, inadService, infoCamereService, ipaService,
                new SqsService("correlationId: {} - IPA - WS23 - domicili digitali non presenti", sqsClient,
                        new ObjectMapper()),
                true);
        CodeSqsDto actualIpaToSqsDtoResult = gatewayService.ipaToSqsDto("42", new IPAPecDto());
        assertEquals("PHYSICAL", actualIpaToSqsDtoResult.getAddressType());
        assertEquals("42", actualIpaToSqsDtoResult.getCorrelationId());
        verify(dynamoDbEnhancedAsyncClient).table(Mockito.<String>any(), Mockito.<TableSchema<Object>>any());
        verify(dynamoDbEnhancedAsyncClient2).table(Mockito.<String>any(), Mockito.<TableSchema<Object>>any());
    }


    /**
     * Method under test: {@link GatewayConverter#errorRegImpToSqsDto(String, Throwable)}
     */
    @Test
    void testErrorRegImpToSqsDto1() {
        GatewayConverter gatewayConverter = new GatewayConverter();
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
        GatewayConverter gatewayConverter = new GatewayConverter();
        CodeSqsDto codeSqsDto = gatewayConverter.newCodeSqsDto(C_ID);
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
        filterDto.setReferenceRequestDate(Date.from(LocalDate.of(2023, 2, 16).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        AddressRequestBodyDto requestBodyDto = new AddressRequestBodyDto();
        requestBodyDto.setFilter(filterDto);
        return requestBodyDto;
    }
}