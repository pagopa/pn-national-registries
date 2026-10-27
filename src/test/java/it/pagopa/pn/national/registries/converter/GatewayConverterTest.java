package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DomicileType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.constant.RequestStatusEnum;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.AddressQueryRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static it.pagopa.pn.national.registries.utils.GatewayUtils.formatInstant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = GatewayConverter.class)
@ExtendWith(SpringExtension.class)
class GatewayConverterTest {

    private static final String C_ID = "correlationId";
    private static final String CF = "RSSMRA80A01H501U";

    @org.springframework.beans.factory.annotation.Autowired
    private GatewayConverter gatewayConverter;

    @Test
    void testMapToAddressesOKDto() {
        AddressOKDto result = gatewayConverter.mapToAddressesOKDto(C_ID);

        assertNotNull(result);
        assertEquals(C_ID, result.getCorrelationId());
    }


    @Test
    void testAnprToSqsDto() {
        ResidentialAddressDto residentialAddress = new ResidentialAddressDto();
        residentialAddress.setAddress("Via Roma 1");
        residentialAddress.setAddressDetail("Scala A");
        residentialAddress.setAt("Mario Rossi");
        residentialAddress.setZip("00100");
        residentialAddress.setMunicipality("Roma");
        residentialAddress.setProvince("RM");
        residentialAddress.setForeignState("IT");
        residentialAddress.setMunicipalityDetails("Roma Capitale");

        GetAddressANPROKDto response = new GetAddressANPROKDto();
        response.setResidentialAddresses(List.of(residentialAddress));

        CodeSqsDto result =
                gatewayConverter.anprToSqsDto(C_ID, response);

        assertNotNull(result);
        assertEquals(C_ID, result.getCorrelationId());
        assertEquals(GatewayDownstreamService.ANPR.name(), result.getRegistry());
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(),
                result.getAddressType()
        );

        assertNotNull(result.getPhysicalAddress());
        assertEquals("Via Roma 1", result.getPhysicalAddress().getAddress());
        assertEquals("Scala A", result.getPhysicalAddress().getAddressDetails());
        assertEquals("Mario Rossi", result.getPhysicalAddress().getAt());
        assertEquals("00100", result.getPhysicalAddress().getZip());
        assertEquals("Roma", result.getPhysicalAddress().getMunicipality());
        assertEquals("RM", result.getPhysicalAddress().getProvince());
        assertEquals("IT", result.getPhysicalAddress().getForeignState());
        assertEquals(
                "Roma Capitale",
                result.getPhysicalAddress().getMunicipalityDetails()
        );
    }

    @Test
    void testAnprToSqsDtoWithNullResponse() {
        CodeSqsDto result =
                gatewayConverter.anprToSqsDto(C_ID, null);

        assertNotNull(result);
        assertEquals(C_ID, result.getCorrelationId());
        assertEquals(GatewayDownstreamService.ANPR.name(), result.getRegistry());
        assertNull(result.getPhysicalAddress());
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(),
                result.getAddressType()
        );
    }

    @Test
    void testAnprToSqsDtoWithEmptyAddresses() {
        GetAddressANPROKDto response = new GetAddressANPROKDto();
        response.setResidentialAddresses(List.of());

        CodeSqsDto result =
                gatewayConverter.anprToSqsDto(C_ID, response);

        assertNotNull(result);
        assertNull(result.getPhysicalAddress());
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(),
                result.getAddressType()
        );
    }

    @Test
    void testInadToSqsDto() {
        DigitalAddressDto digitalAddress = new DigitalAddressDto();
        digitalAddress.setDigitalAddress("test@pec.it");

        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();
        response.setDigitalAddress(digitalAddress);
        response.setTaxId(CF);

        CodeSqsDto result = gatewayConverter.inadToSqsDto(
                C_ID,
                response
        );

        assertNotNull(result);
        assertEquals(C_ID, result.getCorrelationId());
        assertEquals(GatewayDownstreamService.INAD.name(), result.getRegistry());
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(),
                result.getAddressType()
        );

        assertNotNull(result.getDigitalAddress());
        assertEquals(1, result.getDigitalAddress().size());
        assertEquals("test@pec.it", result.getDigitalAddress().getFirst().getAddress());
        assertEquals(
                DigitalAddressRecipientType.PERSONALE.getValue(),
                result.getDigitalAddress().getFirst().getRecipient()
        );
        assertEquals("PEC", result.getDigitalAddress().getFirst().getType());
    }

    @Test
    void testInadToSqsDtoWithNullResponse() {
        CodeSqsDto result = gatewayConverter.inadToSqsDto(
                C_ID,
                null
        );

        assertNotNull(result);
        assertNotNull(result.getDigitalAddress());
        assertTrue(result.getDigitalAddress().isEmpty());
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(),
                result.getAddressType()
        );
    }

    @Test
    void testIpaToSqsDto() {
        IPAPecDto response = new IPAPecDto();
        response.setDomicilioDigitale("ipa@pec.it");

        CodeSqsDto result =
                gatewayConverter.ipaToSqsDto(C_ID, response);

        assertNotNull(result);
        assertEquals(C_ID, result.getCorrelationId());
        assertEquals(GatewayDownstreamService.IPA.name(), result.getRegistry());
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(),
                result.getAddressType()
        );

        assertNotNull(result.getDigitalAddress());
        assertEquals(1, result.getDigitalAddress().size());
        assertEquals("ipa@pec.it", result.getDigitalAddress().getFirst().getAddress());
        assertEquals(
                DigitalAddressRecipientType.IMPRESA.getValue(),
                result.getDigitalAddress().getFirst().getRecipient()
        );
        assertEquals("PEC", result.getDigitalAddress().getFirst().getType());
    }

    @Test
    void testIpaToSqsDtoWithNullResponse() {
        CodeSqsDto result =
                gatewayConverter.ipaToSqsDto(C_ID, null);

        assertNotNull(result);
        assertNull(result.getDigitalAddress());
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(),
                result.getAddressType()
        );
    }

    @Test
    void testRegImpToSqsDto() {
        GetAddressRegistroImpreseOKProfessionalAddressDto address =
                new GetAddressRegistroImpreseOKProfessionalAddressDto();

        address.setAddress("Via Milano 10");
        address.setMunicipality("Milano");
        address.setProvince("MI");
        address.setZip("20100");
        address.setStato("IT");

        GetAddressRegistroImpreseOKDto response =
                new GetAddressRegistroImpreseOKDto();
        response.setProfessionalAddress(address);

        CodeSqsDto result =
                gatewayConverter.regImpToSqsDto(C_ID, response);

        assertNotNull(result);
        assertEquals(C_ID, result.getCorrelationId());
        assertEquals(
                GatewayDownstreamService.REGISTRO_IMPRESE.name(),
                result.getRegistry()
        );
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(),
                result.getAddressType()
        );

        assertNotNull(result.getPhysicalAddress());
        assertEquals("Via Milano 10", result.getPhysicalAddress().getAddress());
        assertEquals("Milano", result.getPhysicalAddress().getMunicipality());
        assertEquals("MI", result.getPhysicalAddress().getProvince());
        assertEquals("20100", result.getPhysicalAddress().getZip());
        assertEquals("IT", result.getPhysicalAddress().getForeignState());
    }

    @Test
    void testRegImpToSqsDtoWithNullResponse() {
        CodeSqsDto result =
                gatewayConverter.regImpToSqsDto(C_ID, null);

        assertNotNull(result);
        assertNull(result.getPhysicalAddress());
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(),
                result.getAddressType()
        );
    }


    @Test
    void testConvertToGetAddressAnprRequest() {
        Date referenceDate = Date.from(
                LocalDate.of(2026, 9, 10)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );

        AddressRequestBodyFilterDto filter = new AddressRequestBodyFilterDto();
        filter.setCorrelationId(C_ID);
        filter.setTaxId(CF);
        filter.setReferenceRequestDate(referenceDate);

        AddressRequestBodyDto request = new AddressRequestBodyDto();
        request.setFilter(filter);

        GetAddressANPRRequestBodyDto result =
                gatewayConverter.convertToGetAddressAnprRequest(request);

        assertNotNull(result);
        assertNotNull(result.getFilter());
        assertEquals(C_ID, result.getFilter().getRequestReason());
        assertEquals(CF, result.getFilter().getTaxId());
        assertEquals("2026-09-10", result.getFilter().getReferenceRequestDate());
    }


    @Test
    void testConvertToGetAddressRegistroImpreseRequest() {
        AddressRequestBodyFilterDto filter = new AddressRequestBodyFilterDto();
        filter.setTaxId(CF);

        AddressRequestBodyDto request = new AddressRequestBodyDto();
        request.setFilter(filter);

        GetAddressRegistroImpreseRequestBodyDto result =
                gatewayConverter.convertToGetAddressRegistroImpreseRequest(request);

        assertNotNull(result);
        assertNotNull(result.getFilter());
        assertEquals(CF, result.getFilter().getTaxId());
    }


    @Test
    void testConvertToGetIpaPecRequest() {
        AddressRequestBodyFilterDto filter = new AddressRequestBodyFilterDto();
        filter.setTaxId(CF);

        AddressRequestBodyDto request = new AddressRequestBodyDto();
        request.setFilter(filter);

        IPARequestBodyDto result =
                gatewayConverter.convertToGetIpaPecRequest(request);

        assertNotNull(result);
        assertNotNull(result.getFilter());
        assertEquals(CF, result.getFilter().getTaxId());
    }

    @Test
    void testConvertToGetDigitalAddressIniPecRequest() {
        GetDigitalAddressIniPECRequestBodyDto result =
                gatewayConverter.convertToGetDigitalAddressIniPecRequest(C_ID, CF);

        assertNotNull(result);
        assertNotNull(result.getFilter());
        assertEquals(C_ID, result.getFilter().getCorrelationId());
        assertEquals(CF, result.getFilter().getTaxId());
    }

    @Test
    void testToAddressQueryRequests() {
        RecipientAddressRequestBodyDto address =
                new RecipientAddressRequestBodyDto();

        address.setTaxId(CF);
        address.setRecIndex(1);
        address.setRecipientType(
                RecipientAddressRequestBodyDto.RecipientTypeEnum.PF
        );

        Date referenceDate = new Date();

        PhysicalAddressesRequestBodyDto request = new PhysicalAddressesRequestBodyDto();

        request.setCorrelationId(C_ID);
        request.setReferenceRequestDate(referenceDate);
        request.setAddresses(List.of(address));

        List<AddressQueryRequest> result =
                gatewayConverter.toAddressQueryRequests(request);

        assertNotNull(result);
        assertEquals(1, result.size());

        AddressQueryRequest mapped = result.getFirst();

        assertEquals(C_ID, mapped.getCorrelationId());
        assertEquals(CF, mapped.getTaxId());
        assertEquals(1, mapped.getRecIndex());
        assertEquals(referenceDate, mapped.getReferenceRequestDate());
        assertEquals(DomicileType.PHYSICAL, mapped.getDomicileType());
        assertEquals(
                RecipientType.fromString(address.getRecipientType().name()),
                mapped.getRecipientType()
        );
    }

    @Test
    void testConvertAnprResponseToInternalRecipientAddress() {
        ResidentialAddressDto residentialAddress = new ResidentialAddressDto();
        residentialAddress.setAddress("Via Roma");

        GetAddressANPROKDto response = new GetAddressANPROKDto();
        response.setResidentialAddresses(List.of(residentialAddress));

        AddressQueryRequest queryRequest = AddressQueryRequest.builder()
                .correlationId(C_ID)
                .taxId(CF)
                .recIndex(10)
                .build();

        PhysicalAddressResponseDto result =
                gatewayConverter.convertAnprResponseToInternalRecipientAddress(
                        response,
                        queryRequest
                );

        assertNotNull(result);
        assertEquals(10, result.getRecIndex());
        assertEquals(GatewayDownstreamService.ANPR.name(), result.getRegistry());

        assertNotNull(result.getPhysicalAddress());
        assertEquals(
                "Via Roma",
                result.getPhysicalAddress().getAddress()
        );
    }

    @Test
    void testConvertAnprResponseToInternalRecipientAddressWithoutAddress() {
        AddressQueryRequest queryRequest = AddressQueryRequest.builder()
                .correlationId(C_ID)
                .recIndex(10)
                .build();

        PhysicalAddressResponseDto result =
                gatewayConverter.convertAnprResponseToInternalRecipientAddress(
                        null,
                        queryRequest
                );

        assertNotNull(result);
        assertEquals(10, result.getRecIndex());
        assertEquals(GatewayDownstreamService.ANPR.name(), result.getRegistry());
        assertNull(result.getPhysicalAddress());
    }

    @Test
    void testAnprNotFoundErrorToPhysicalAddressSQSMessage() {
        AddressQueryRequest queryRequest = AddressQueryRequest.builder()
                .recIndex(15)
                .build();

        PhysicalAddressResponseDto result =
                gatewayConverter.anprNotFoundErrorToPhysicalAddressSQSMessage(
                        queryRequest
                );

        assertNotNull(result);
        assertEquals(15, result.getRecIndex());
        assertEquals(GatewayDownstreamService.ANPR.name(), result.getRegistry());
        assertNull(result.getPhysicalAddress());
    }

    @Test
    void testConvertRegImprResponseToInternalRecipientAddress() {
        GetAddressRegistroImpreseOKProfessionalAddressDto address =
                new GetAddressRegistroImpreseOKProfessionalAddressDto();

        address.setAddress("Via Impresa");
        address.setMunicipality("Milano");

        GetAddressRegistroImpreseOKDto response =
                new GetAddressRegistroImpreseOKDto();
        response.setProfessionalAddress(address);

        AddressQueryRequest queryRequest = AddressQueryRequest.builder()
                .correlationId(C_ID)
                .recIndex(20)
                .build();

        PhysicalAddressResponseDto result =
                gatewayConverter.convertRegImprResponseToInternalRecipientAddress(
                        response,
                        queryRequest
                );

        assertNotNull(result);
        assertEquals(20, result.getRecIndex());
        assertEquals(
                GatewayDownstreamService.REGISTRO_IMPRESE.name(),
                result.getRegistry()
        );

        assertNotNull(result.getPhysicalAddress());
        assertEquals(
                "Via Impresa",
                result.getPhysicalAddress().getAddress()
        );
    }

    @Test
    void testConvertRegImprResponseToInternalRecipientAddressNullResponse() {
        AddressQueryRequest queryRequest = AddressQueryRequest.builder()
                .correlationId(C_ID)
                .recIndex(20)
                .build();

        PhysicalAddressResponseDto result =
                gatewayConverter.convertRegImprResponseToInternalRecipientAddress(
                        null,
                        queryRequest
                );

        assertNotNull(result);
        assertEquals(20, result.getRecIndex());
        assertEquals(
                GatewayDownstreamService.REGISTRO_IMPRESE.name(),
                result.getRegistry()
        );
        assertNull(result.getPhysicalAddress());
    }

    @Test
    void testConvertToPhysicalAddressesResponseDto() {
        PhysicalAddressResponseDto address = new PhysicalAddressResponseDto();
        address.setRecIndex(1);

        List<PhysicalAddressResponseDto> addresses = List.of(address);

        PhysicalAddressesResponseDto result =
                gatewayConverter.convertToPhysicalAddressesResponseDto(
                        addresses,
                        C_ID
                );

        assertNotNull(result);
        assertEquals(C_ID, result.getCorrelationId());
        assertEquals(addresses, result.getAddresses());
    }


    @Test
    void testToInternalCodeSqsDto() {
        Date referenceDate = new Date();

        AddressRequestBodyFilterDto filter =
                new AddressRequestBodyFilterDto();

        filter.setTaxId(CF);
        filter.setCorrelationId(C_ID);
        filter.setDomicileType(
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL
        );
        filter.setReferenceRequestDate(referenceDate);

        InternalCodeSqsDto result =
                gatewayConverter.toInternalCodeSqsDto(
                        filter,
                        "PF",
                        "cx-id"
                );

        assertNotNull(result);
        assertEquals(CF, result.getTaxId());
        assertEquals(C_ID, result.getCorrelationId());
        assertEquals("PF", result.getRecipientType());
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(),
                result.getDomicileType()
        );
        assertEquals(referenceDate, result.getReferenceRequestDate());
        assertEquals("cx-id", result.getPnNationalRegistriesCxId());
    }

    @Test
    void testConstructNationalRegistriesRequest() {
        Date referenceDate = new Date();

        AddressRequestBodyFilterDto filter = new AddressRequestBodyFilterDto();
        filter.setCorrelationId(C_ID);
        filter.setTaxId(CF);
        filter.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        filter.setReferenceRequestDate(referenceDate);

        AddressRequestBodyDto request = new AddressRequestBodyDto();
        request.setFilter(filter);

        NationalRegistriesRequest result = gatewayConverter.constructNationalRegistriesRequest(
                request,
                RecipientType.PF,
                "cx-id",
                GatewayDownstreamService.INAD
        );

        assertNotNull(result.getCreatedAt());
        assertEquals(formatInstant(result.getCreatedAt()), result.getAttemptAfter());
        assertEquals(C_ID, result.getCorrelationId());
        assertEquals(referenceDate.toInstant().atZone(ZoneOffset.UTC).toString(), result.getReferenceRequestDate());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(), result.getDomicileType());
        assertEquals(CF, result.getTaxId());
        assertEquals(RecipientType.PF.name(), result.getRecipientType());
        assertEquals("cx-id", result.getClientId());
        assertEquals(GatewayDownstreamService.INAD, result.getRegistry());
        assertEquals(RequestStatusEnum.NOT_WORKED, result.getStatus());
        assertEquals("INAD#NOT_WORKED", result.getRegistryStatus());
    }

    @Test
    void testErrorAnprToSqsDtoCfNotFound() {
        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(exception.getResponseBodyAsString())
                .thenReturn("{\"codiceErroreAnomalia\":\"EN122\"}");

        CodeSqsDto result =
                gatewayConverter.errorAnprToSqsDto(C_ID, exception);

        assertNotNull(result);
        assertEquals(C_ID, result.getCorrelationId());
        assertEquals(GatewayDownstreamService.ANPR.name(), result.getRegistry());
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue(),
                result.getAddressType()
        );
        assertNull(result.getPhysicalAddress());
    }

    @Test
    void testErrorAnprToSqsDtoGenericError() {
        RuntimeException exception = new RuntimeException("Generic error");

        CodeSqsDto result =
                gatewayConverter.errorAnprToSqsDto(C_ID, exception);

        assertNull(result);
    }

    @Test
    void testErrorInadToSqsDtoCfNotFound() {
        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(exception.getResponseBodyAsString())
                .thenReturn("{\"detail\":\"CF non trovato\"}");

        CodeSqsDto result =
                gatewayConverter.errorInadToSqsDto(C_ID, exception);

        assertNotNull(result);
        assertEquals(C_ID, result.getCorrelationId());
        assertEquals(GatewayDownstreamService.INAD.name(), result.getRegistry());
        assertNotNull(result.getDigitalAddress());
        assertTrue(result.getDigitalAddress().isEmpty());
        assertEquals(
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(),
                result.getAddressType()
        );
    }

    @Test
    void testErrorInadToSqsDtoCfNotFoundFromMessage() {
        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(exception.getResponseBodyAsString()).thenReturn(null);
        when(exception.getMessage()).thenReturn("CF non trovato");

        CodeSqsDto result =
                gatewayConverter.errorInadToSqsDto(C_ID, exception);

        assertNotNull(result);
        assertTrue(result.getDigitalAddress().isEmpty());
        assertEquals(GatewayDownstreamService.INAD.name(), result.getRegistry());
    }

    @Test
    void testErrorInadToSqsDtoGenericError() {
        RuntimeException exception =
                new RuntimeException("Generic error");

        CodeSqsDto result =
                gatewayConverter.errorInadToSqsDto(C_ID, exception);

        assertNull(result);
    }

    // ---- errori reali (non mock): solo 404 + pattern specifico deve diventare risposta "non trovato" ----

    private static PnNationalRegistriesException pnException(String message, int status, String body) {
        return new PnNationalRegistriesException(message, status, "status", null,
                body == null ? null : body.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.charset.StandardCharsets.UTF_8, Object.class);
    }

    @Test
    void errorAnprToSqsDtoReturnsNullWhenStatusIsNotNotFoundEvenWithEN122() {
        assertNull(GatewayConverter.errorAnprToSqsDto(C_ID, pnException("err", 500, "{\"codiceErroreAnomalia\":\"EN122\"}")));
        assertNull(GatewayConverter.errorAnprToSqsDto(C_ID, pnException("err", 400, "{\"codiceErroreAnomalia\":\"EN122\"}")));
    }

    @Test
    void errorAnprToSqsDtoReturnsNullWhenNotFoundWithOtherErrorCodeOrEmptyBody() {
        assertNull(GatewayConverter.errorAnprToSqsDto(C_ID, pnException("err", 404, "{\"codiceErroreAnomalia\":\"EN999\"}")));
        assertNull(GatewayConverter.errorAnprToSqsDto(C_ID, pnException("err", 404, null)));
        assertNull(GatewayConverter.errorAnprToSqsDto(C_ID, pnException("err", 404, "")));
    }

    @Test
    void errorAnprToSqsDtoMatchesEN122CaseInsensitiveWithSpaces() {
        CodeSqsDto result = GatewayConverter.errorAnprToSqsDto(C_ID, pnException("err", 404, "{\"codiceErroreAnomalia\" : \"en122\"}"));

        assertNotNull(result);
        assertEquals(GatewayDownstreamService.ANPR.name(), result.getRegistry());
    }

    @Test
    void isAnprAddressNotFoundPredicateMatchesOnlyNotFoundWithEN122() {
        assertTrue(GatewayConverter.isAnprAddressNotFound.test(pnException("err", 404, "{\"codiceErroreAnomalia\":\"EN122\"}")));
        assertFalse(GatewayConverter.isAnprAddressNotFound.test(pnException("err", 500, "{\"codiceErroreAnomalia\":\"EN122\"}")));
        assertFalse(GatewayConverter.isAnprAddressNotFound.test(pnException("err", 404, "{\"codiceErroreAnomalia\":\"EN001\"}")));
        assertFalse(GatewayConverter.isAnprAddressNotFound.test(new RuntimeException("x")));
    }

    @Test
    void errorInadToSqsDtoMatchesBodyDetailWhenStatusIsNotFound() {
        CodeSqsDto result = GatewayConverter.errorInadToSqsDto(C_ID, pnException("other", 404, "{\"detail\":\"CF non trovato\"}"));

        assertNotNull(result);
        assertTrue(result.getDigitalAddress().isEmpty());
    }

    @Test
    void errorInadToSqsDtoMatchesMessageCaseInsensitiveWhenBodyIsEmpty() {
        assertNotNull(GatewayConverter.errorInadToSqsDto(C_ID, pnException("cf NON trovato", 404, null)));
    }

    @Test
    void errorInadToSqsDtoReturnsNullForNotFoundWithDifferentDetail() {
        assertNull(GatewayConverter.errorInadToSqsDto(C_ID, pnException("other", 404, "{\"detail\":\"altro errore\"}")));
    }

    @Test
    void errorInadToSqsDtoReturnsNullWhenStatusIsNotNotFound() {
        assertNull(GatewayConverter.errorInadToSqsDto(C_ID, pnException("CF non trovato", 400, "{\"detail\":\"CF non trovato\"}")));
        assertNull(GatewayConverter.errorInadToSqsDto(C_ID, pnException("CF non trovato", 500, null)));
    }

    // ---- INAD: tipo destinatario dell'indirizzo ----

    private static GetDigitalAddressINADOKDto inadResponse(String taxId, String pec, String profession) {
        DigitalAddressDto digitalAddress = new DigitalAddressDto();
        digitalAddress.setDigitalAddress(pec);
        digitalAddress.setPracticedProfession(profession);
        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();
        response.setTaxId(taxId);
        response.setDigitalAddress(digitalAddress);
        return response;
    }

    @Test
    void inadToSqsDtoMapsElevenDigitTaxIdWithoutProfessionToImpresa() {
        CodeSqsDto result = GatewayConverter.inadToSqsDto(C_ID, inadResponse("12345678901", "impresa@pec.it", null));

        assertEquals(DigitalAddressRecipientType.IMPRESA.getValue(), result.getDigitalAddress().getFirst().getRecipient());
    }

    @Test
    void inadToSqsDtoMapsProfessionToProfessionista() {
        CodeSqsDto forCf = GatewayConverter.inadToSqsDto(C_ID, inadResponse(CF, "pro@pec.it", "AVVOCATO"));
        CodeSqsDto forPiva = GatewayConverter.inadToSqsDto(C_ID, inadResponse("12345678901", "pro@pec.it", "AVVOCATO"));

        assertEquals(DigitalAddressRecipientType.PROFESSIONISTA.getValue(), forCf.getDigitalAddress().getFirst().getRecipient());
        assertEquals(DigitalAddressRecipientType.PROFESSIONISTA.getValue(), forPiva.getDigitalAddress().getFirst().getRecipient());
    }

    @Test
    void inadToSqsDtoWithoutDigitalAddressReturnsEmptyList() {
        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();
        response.setTaxId(CF);

        CodeSqsDto result = GatewayConverter.inadToSqsDto(C_ID, response);

        assertTrue(result.getDigitalAddress().isEmpty());
        assertEquals(GatewayDownstreamService.INAD.name(), result.getRegistry());
    }

    @Test
    void ipaToSqsDtoWithoutDomicilioDigitaleHasNoAddresses() {
        CodeSqsDto result = GatewayConverter.ipaToSqsDto(C_ID, new IPAPecDto());

        assertNull(result.getDigitalAddress());
        assertEquals(GatewayDownstreamService.IPA.name(), result.getRegistry());
    }

    @Test
    void anprToSqsDtoUsesOnlyFirstResidentialAddress() {
        ResidentialAddressDto first = new ResidentialAddressDto();
        first.setAddress("Via Prima");
        ResidentialAddressDto second = new ResidentialAddressDto();
        second.setAddress("Via Seconda");
        GetAddressANPROKDto response = new GetAddressANPROKDto();
        response.setResidentialAddresses(List.of(first, second));

        CodeSqsDto result = GatewayConverter.anprToSqsDto(C_ID, response);

        assertEquals("Via Prima", result.getPhysicalAddress().getAddress());
    }

    @Test
    void regImpToSqsDtoWithoutProfessionalAddressHasNoPhysicalAddress() {
        CodeSqsDto result = GatewayConverter.regImpToSqsDto(C_ID, new GetAddressRegistroImpreseOKDto());

        assertNull(result.getPhysicalAddress());
        assertEquals(GatewayDownstreamService.REGISTRO_IMPRESE.name(), result.getRegistry());
    }

    // ---- richieste multiple e costruttori ----

    @Test
    void toAddressQueryRequestsMapsEveryAddressIncludingPg() {
        RecipientAddressRequestBodyDto pf = new RecipientAddressRequestBodyDto();
        pf.setTaxId(CF);
        pf.setRecIndex(0);
        pf.setRecipientType(RecipientAddressRequestBodyDto.RecipientTypeEnum.PF);
        RecipientAddressRequestBodyDto pg = new RecipientAddressRequestBodyDto();
        pg.setTaxId("12345678901");
        pg.setRecIndex(1);
        pg.setRecipientType(RecipientAddressRequestBodyDto.RecipientTypeEnum.PG);
        PhysicalAddressesRequestBodyDto request = new PhysicalAddressesRequestBodyDto();
        request.setCorrelationId(C_ID);
        request.setReferenceRequestDate(new Date());
        request.setAddresses(List.of(pf, pg));

        List<AddressQueryRequest> result = GatewayConverter.toAddressQueryRequests(request);

        assertEquals(2, result.size());
        assertEquals(RecipientType.PF, result.get(0).getRecipientType());
        assertEquals(0, result.get(0).getRecIndex());
        assertEquals(RecipientType.PG, result.get(1).getRecipientType());
        assertEquals("12345678901", result.get(1).getTaxId());
        assertEquals(1, result.get(1).getRecIndex());
    }

    @Test
    void toAddressQueryRequestsWithNoAddressesReturnsEmptyList() {
        PhysicalAddressesRequestBodyDto request = new PhysicalAddressesRequestBodyDto();
        request.setCorrelationId(C_ID);
        request.setAddresses(List.of());

        assertTrue(GatewayConverter.toAddressQueryRequests(request).isEmpty());
    }

    @Test
    void convertToGetAddressAnprRequestFromQueryRequestFormatsDate() {
        Date referenceDate = Date.from(LocalDate.of(2026, 9, 10).atStartOfDay(ZoneId.systemDefault()).toInstant());
        AddressQueryRequest queryRequest = AddressQueryRequest.builder()
                .correlationId(C_ID).taxId(CF).referenceRequestDate(referenceDate).build();

        GetAddressANPRRequestBodyDto result = GatewayConverter.convertToGetAddressAnprRequest(queryRequest);

        assertEquals(C_ID, result.getFilter().getRequestReason());
        assertEquals(CF, result.getFilter().getTaxId());
        assertEquals("2026-09-10", result.getFilter().getReferenceRequestDate());
    }

    @Test
    void convertToGetAddressRegistroImpreseRequestFromQueryRequestUsesTaxId() {
        AddressQueryRequest queryRequest = AddressQueryRequest.builder().taxId("12345678901").build();

        assertEquals("12345678901", GatewayConverter.convertToGetAddressRegistroImpreseRequest(queryRequest).getFilter().getTaxId());
    }

    @Test
    void convertToGetDigitalAddressInadRequestSetsPracticalReferenceAndTaxId() {
        var result = GatewayConverter.convertToGetDigitalAddressInadRequest(C_ID, CF);

        assertEquals(CF, result.getFilter().getTaxId());
        assertEquals(C_ID, result.getFilter().getPracticalReference());
    }

    @Test
    void toAddressRequestBodyDtoMapsPayloadFields() {
        Date referenceDate = new Date();
        it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent.Payload payload =
                it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent.Payload.builder()
                        .correlationId(C_ID).taxId(CF).domicileType("PHYSICAL").referenceRequestDate(referenceDate).build();

        AddressRequestBodyDto result = GatewayConverter.toAddressRequestBodyDto(payload);

        assertEquals(C_ID, result.getFilter().getCorrelationId());
        assertEquals(CF, result.getFilter().getTaxId());
        assertEquals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, result.getFilter().getDomicileType());
        assertEquals(referenceDate, result.getFilter().getReferenceRequestDate());
    }

    @Test
    void constructBatchEntityInitializesBatchInRequestSentStatus() {
        java.time.Instant now = java.time.Instant.parse("2026-01-01T10:00:00Z");
        List<NationalRegistriesRequest> requests = List.of(new NationalRegistriesRequest(), new NationalRegistriesRequest());

        var batch = GatewayConverter.constructBatchEntity("batch-id", requests, now, GatewayDownstreamService.INAD, "polling-id");

        assertEquals("batch-id", batch.getBatchId());
        assertEquals("batch-id", batch.getPracticalReferenceValue());
        assertEquals("polling-id", batch.getPollingId());
        assertEquals(GatewayDownstreamService.INAD.name(), batch.getRegistry());
        assertEquals(2, batch.getBatchSize());
        assertEquals(now, batch.getCreatedAt());
        assertEquals(now, batch.getUpdatedAt());
        assertNull(batch.getAttemptAfter());
        assertEquals(it.pagopa.pn.national.registries.constant.BatchesStatusEnum.REQUEST_SENT, batch.getStatus());
        assertEquals(0, batch.getErrorRetryCount());
        assertEquals(0, batch.getInProgressRetryCount());
    }

    @Test
    void constructNationalRegistriesRequestBuildsRegistryStatusForEveryRegistryAndRecipient() {
        AddressRequestBodyFilterDto filter = new AddressRequestBodyFilterDto();
        filter.setCorrelationId(C_ID);
        filter.setTaxId("12345678901");
        filter.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        filter.setReferenceRequestDate(new Date());
        AddressRequestBodyDto request = new AddressRequestBodyDto();
        request.setFilter(filter);

        NationalRegistriesRequest result = GatewayConverter.constructNationalRegistriesRequest(request, RecipientType.PG, null, GatewayDownstreamService.INIPEC);

        assertEquals("INIPEC#NOT_WORKED", result.getRegistryStatus());
        assertEquals("PG", result.getRecipientType());
        assertNull(result.getClientId());
        assertNotNull(result.getAttemptAfter());
    }
}