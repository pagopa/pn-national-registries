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
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import it.pagopa.pn.national.registries.model.gateway.AddressQueryRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayAddressResponse;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.model.infocamere.InfocamereResponseKO;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.PhysicalAddress;
import it.pagopa.pn.national.registries.repository.CounterRepositoryImpl;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepositoryImpl;
import it.pagopa.pn.national.registries.service.*;
import it.pagopa.pn.national.registries.utils.FeatureEnabledUtils;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import static it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL;
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
                new InfoCamereConverter(2L, "~"), iniPecBatchRequestRepository, 2L, "~", validateTaxIdUtils);

        InadService inadService = new InadService(mock(InadClient.class), validateTaxIdUtils, featureEnabledUtils);
        PnNationalRegistriesSecretService pnNationalRegistriesSecretService = new PnNationalRegistriesSecretService(new CachedSecretsManagerConsumer(mock(SecretsManagerClient.class)));
        IpaSecretConfig ipaSecretConfig = new IpaSecretConfig("ipaSecret");
        IpaService ipaService = new IpaService(new IpaConverter(), mock(IpaClient.class), validateTaxIdUtils, pnNationalRegistriesSecretService, ipaSecretConfig);

        SqsClient sqsClient = mock(SqsClient.class);
        GatewayService gatewayService = new GatewayService(anprService, inadService, infoCamereService, ipaService,
                new SqsService("outputQueue", "inputQueue", "inputDlqQueue", sqsClient,
                        new ObjectMapper()), featureEnabledUtils,
                true);
        CodeSqsDto actualIpaToSqsDtoResult = gatewayService.ipaToSqsDto("42", new IPAPecDto());
        assertEquals(DIGITAL.name(), actualIpaToSqsDtoResult.getAddressType());
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

    // START TESTS FOR MULTI ADDRESSES

    @Test
    void testToAddressQueryRequests() {
        Date now = new Date();
        PhysicalAddressesRequestBodyDto requestBodyDto = new PhysicalAddressesRequestBodyDto();
        requestBodyDto.setCorrelationId("correlationId");
        requestBodyDto.setReferenceRequestDate(now);

        RecipientAddressRequestBodyDto address1 = new RecipientAddressRequestBodyDto();
        address1.setTaxId("taxId1");
        address1.setRecipientType(RecipientAddressRequestBodyDto.RecipientTypeEnum.PF);
        address1.setRecIndex(0);

        RecipientAddressRequestBodyDto address2 = new RecipientAddressRequestBodyDto();
        address2.setTaxId("taxId2");
        address2.setRecipientType(RecipientAddressRequestBodyDto.RecipientTypeEnum.PG);
        address2.setRecIndex(1);

        requestBodyDto.setAddresses(List.of(address1, address2));

        List<AddressQueryRequest> result = gatewayConverter.toAddressQueryRequests(requestBodyDto);

        assertEquals(2, result.size());

        AddressQueryRequest request1 = result.get(0);
        assertEquals("correlationId", request1.getCorrelationId());
        assertEquals(now, request1.getReferenceRequestDate());
        assertEquals("taxId1", request1.getTaxId());
        assertEquals(RecipientType.fromString(RecipientAddressRequestBodyDto.RecipientTypeEnum.PF.name()), request1.getRecipientType());
        assertEquals(0, request1.getRecIndex());

        AddressQueryRequest request2 = result.get(1);
        assertEquals("correlationId", request2.getCorrelationId());
        assertEquals(now, request2.getReferenceRequestDate());
        assertEquals("taxId2", request2.getTaxId());
        assertEquals(RecipientType.fromString(RecipientAddressRequestBodyDto.RecipientTypeEnum.PG.name()), request2.getRecipientType());
        assertEquals(1, request2.getRecIndex());
    }

    @Test
    void testConvertToGetAddressAnprMultiRequest() {
        AddressQueryRequest addressQueryRequest = AddressQueryRequest.builder()
                .correlationId("testCorrelationId")
                .taxId("testTaxId")
                .referenceRequestDate(Date.from(LocalDate.of(2023, 10, 1)
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .build();

        GetAddressANPRRequestBodyDto result = gatewayConverter.convertToGetAddressAnprRequest(addressQueryRequest);

        assertEquals("testCorrelationId", result.getFilter().getRequestReason());
        assertEquals("testTaxId", result.getFilter().getTaxId());
        assertEquals("2023-10-01", result.getFilter().getReferenceRequestDate());
    }

    @Test
    void testConvertAnprResponseToInternalRecipientAddress() {
        AddressQueryRequest addressQueryRequest = AddressQueryRequest.builder()
                .correlationId("testCorrelationId")
                .recIndex(1)
                .build();

        ResidentialAddressDto residentialAddress = new ResidentialAddressDto();
        residentialAddress.setAddress("Test Address");
        residentialAddress.setZip("12345");
        residentialAddress.setProvince("Test Province");
        residentialAddress.setMunicipality("Test Municipality");

        GetAddressANPROKDto response = new GetAddressANPROKDto();
        response.setResidentialAddresses(List.of(residentialAddress));

        GatewayAddressResponse.AddressInfo result = gatewayConverter.convertAnprResponseToInternalRecipientAddress(response, addressQueryRequest);

        assertEquals("Test Address", result.getPhysicalAddress().getAddress());
        assertEquals("12345", result.getPhysicalAddress().getZip());
        assertEquals("Test Province", result.getPhysicalAddress().getProvince());
        assertEquals("Test Municipality", result.getPhysicalAddress().getMunicipality());
        assertEquals(1, result.getRecIndex());
        assertEquals("ANPR", result.getRegistry());
    }

    @Test
    void testConvertAnprResponseToInternalRecipientAddressWithNullResponse() {
        AddressQueryRequest addressQueryRequest = AddressQueryRequest.builder()
                .correlationId("testCorrelationId")
                .recIndex(1)
                .build();

        GatewayAddressResponse.AddressInfo result = gatewayConverter.convertAnprResponseToInternalRecipientAddress(null, addressQueryRequest);

        assertNull(result.getPhysicalAddress());
        assertEquals(1, result.getRecIndex());
        assertEquals("ANPR", result.getRegistry());
    }

    @Test
    void testAnprNotFoundErrorToPhysicalAddressSQSMessage() {
        AddressQueryRequest addressQueryRequest = AddressQueryRequest.builder()
                .correlationId("testCorrelationId")
                .recIndex(1)
                .build();

        GatewayAddressResponse.AddressInfo result = gatewayConverter.anprNotFoundErrorToPhysicalAddressSQSMessage(addressQueryRequest);

        assertEquals(1, result.getRecIndex());
        assertEquals("ANPR", result.getRegistry());
    }

    @Test
    void testConvertToGetAddressRegistroImpreseMultiRequest() {
        AddressQueryRequest addressQueryRequest = AddressQueryRequest.builder()
                .taxId("testTaxId")
                .build();

        GetAddressRegistroImpreseRequestBodyDto result = gatewayConverter.convertToGetAddressRegistroImpreseRequest(addressQueryRequest);

        assertEquals("testTaxId", result.getFilter().getTaxId());
    }

    @Test
    void testConvertRegImprResponseToInternalRecipientAddress() {
        AddressQueryRequest addressQueryRequest = AddressQueryRequest.builder()
                .correlationId("testCorrelationId")
                .recIndex(1)
                .build();

        GetAddressRegistroImpreseOKDto response = getProfessionalAddress();

        GatewayAddressResponse.AddressInfo result = gatewayConverter.convertRegImprResponseToInternalRecipientAddress(response, addressQueryRequest);

        assertEquals("Test Address", result.getPhysicalAddress().getAddress());
        assertEquals("12345", result.getPhysicalAddress().getZip());
        assertEquals("Test Province", result.getPhysicalAddress().getProvince());
        assertEquals("Test Municipality", result.getPhysicalAddress().getMunicipality());
        assertEquals(1, result.getRecIndex());
        assertEquals("REGISTRO_IMPRESE", result.getRegistry());
    }

    @Test
    void testConvertRegImprResponseToInternalRecipientAddressWithNullResponse() {
        AddressQueryRequest addressQueryRequest = AddressQueryRequest.builder()
                .correlationId("testCorrelationId")
                .recIndex(1)
                .build();

        GatewayAddressResponse.AddressInfo result = gatewayConverter.convertRegImprResponseToInternalRecipientAddress(null, addressQueryRequest);

        assertNull(result.getPhysicalAddress());
        assertEquals(1, result.getRecIndex());
        assertEquals("REGISTRO_IMPRESE", result.getRegistry());
    }

    @Test
    void testConvertToPhysicalAddressesResponseDto() {
        String correlationId = "correlationId";

        List<GatewayAddressResponse.AddressInfo> physicalAddress = getPhysicalAddress();

        PhysicalAddressesResponseDto result = gatewayConverter.convertToPhysicalAddressesResponseDto(physicalAddress, correlationId);

        assertNotNull(result);
        assertEquals(correlationId, result.getCorrelationId());
        assertNotNull(result.getAddresses());
        assertEquals(1, result.getAddresses().size());

        PhysicalAddressResponseDto responseDto = result.getAddresses().get(0);
        assertNotNull(responseDto.getPhysicalAddress());
        assertEquals("Test Address", responseDto.getPhysicalAddress().getAddress());
        assertEquals("12345", responseDto.getPhysicalAddress().getZip());
        assertEquals("Test Province", responseDto.getPhysicalAddress().getProvince());
        assertEquals("Test Municipality", responseDto.getPhysicalAddress().getMunicipality());
        assertEquals(1, responseDto.getRecIndex());
        assertEquals("ANPR", responseDto.getRegistry());
    }

    private static GetAddressRegistroImpreseOKDto getProfessionalAddress() {
        GetAddressRegistroImpreseOKProfessionalAddressDto professionalAddress = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        professionalAddress.setAddress("Test Address");
        professionalAddress.setZip("12345");
        professionalAddress.setProvince("Test Province");
        professionalAddress.setMunicipality("Test Municipality");

        GetAddressRegistroImpreseOKDto response = new GetAddressRegistroImpreseOKDto();
        response.setProfessionalAddress(professionalAddress);
        return response;
    }

    private static List<GatewayAddressResponse.AddressInfo> getPhysicalAddress() {
        PhysicalAddress physicalAddress = new PhysicalAddress();
        physicalAddress.setAddress("Test Address");
        physicalAddress.setZip("12345");
        physicalAddress.setProvince("Test Province");
        physicalAddress.setMunicipality("Test Municipality");

        GatewayAddressResponse.AddressInfo addressInfo = new GatewayAddressResponse.AddressInfo();
        addressInfo.setPhysicalAddress(physicalAddress);
        addressInfo.setRecIndex(1);
        addressInfo.setRegistry("ANPR");

        return List.of(addressInfo);
    }

    // END TESTS FOR MULTI ADDRESSES
}