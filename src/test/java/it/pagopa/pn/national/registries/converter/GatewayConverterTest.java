package it.pagopa.pn.national.registries.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.config.CachedSecretsManagerConsumer;
import it.pagopa.pn.national.registries.config.ipa.IpaSecretConfig;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
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
import it.pagopa.pn.national.registries.service.*;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    /**
     * Method under test: {@link GatewayConverter#errorInadToSqsDto(String, Throwable)}
     */
    @Test
    void testErrorInadToSqsDto1() {
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
        AnprService anprService = new AnprService(new AnprConverter(), mock(AnprClient.class), counterRepository, validateTaxIdUtils);

        DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient2 = mock(DynamoDbEnhancedAsyncClient.class);
        when(dynamoDbEnhancedAsyncClient2.table(Mockito.<String>any(), Mockito.<TableSchema<Object>>any())).thenReturn(
                new DynamoDbAsyncTableDecorator<>(new DynamoDbAsyncTableDecorator<>(new DynamoDbAsyncTableDecorator<>(
                        new DynamoDbAsyncTableDecorator<>(new DynamoDbAsyncTableDecorator<>(mock(DynamoDbAsyncTable.class)))))));
        IniPecBatchRequestRepositoryImpl iniPecBatchRequestRepository = new IniPecBatchRequestRepositoryImpl(
                dynamoDbEnhancedAsyncClient2, 3, 2);

        InfoCamereClient infoCamereClient = mock(InfoCamereClient.class);
        InfoCamereService infoCamereService = new InfoCamereService(infoCamereClient,
                new InfoCamereConverter(2L), iniPecBatchRequestRepository, 2L, validateTaxIdUtils);

        InadService inadService = new InadService(mock(InadClient.class), validateTaxIdUtils);
        PnNationalRegistriesSecretService pnNationalRegistriesSecretService = new PnNationalRegistriesSecretService(new CachedSecretsManagerConsumer(mock(SecretsManagerClient.class)));
        IpaSecretConfig ipaSecretConfig = new IpaSecretConfig("ipaSecret");
        IpaService ipaService = new IpaService(new IpaConverter(), mock(IpaClient.class), validateTaxIdUtils, pnNationalRegistriesSecretService, ipaSecretConfig);

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

    @Test
    void testConvertToGetIpaPecRequest(){
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        AddressRequestBodyFilterDto addressRequestBodyFilterDto = new AddressRequestBodyFilterDto();
        addressRequestBodyFilterDto.setTaxId("taxId");
        addressRequestBodyDto.setFilter(addressRequestBodyFilterDto);
        assertNotNull(gatewayConverter.convertToGetIpaPecRequest(addressRequestBodyDto));
    }

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
}