package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DomicileType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.BatchRequest;
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
import java.util.Date;
import java.util.List;

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
    void testNewCodeSqsDto() {
        CodeSqsDto result =
                gatewayConverter.newCodeSqsDto(C_ID, GatewayDownstreamService.ANPR);

        assertNotNull(result);
        assertEquals(C_ID, result.getCorrelationId());
        assertEquals(GatewayDownstreamService.ANPR.name(), result.getRegistry());
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

        CodeSqsDto result = gatewayConverter.inadToSqsDto(
                C_ID,
                response,
                DigitalAddressRecipientType.PERSONA_FISICA
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
                DigitalAddressRecipientType.PERSONA_FISICA.getValue(),
                result.getDigitalAddress().getFirst().getRecipient()
        );
        assertEquals("PEC", result.getDigitalAddress().getFirst().getType());
    }

    @Test
    void testInadToSqsDtoWithNullResponse() {
        CodeSqsDto result = gatewayConverter.inadToSqsDto(
                C_ID,
                null,
                DigitalAddressRecipientType.PERSONA_FISICA
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
    void testConvertAnprToPhysicalAddress() {
        ResidentialAddressDto source = new ResidentialAddressDto();
        source.setAddress("Via Roma");
        source.setAddressDetail("10");
        source.setAt("Mario");
        source.setZip("00100");
        source.setMunicipality("Roma");
        source.setProvince("RM");
        source.setForeignState("Italia");
        source.setMunicipalityDetails("Dettaglio");

        PhysicalAddressDto result =
                gatewayConverter.convertAnprToPhysicalAddress(source);

        assertNotNull(result);
        assertEquals(source.getAddress(), result.getAddress());
        assertEquals(source.getAddressDetail(), result.getAddressDetails());
        assertEquals(source.getAt(), result.getAt());
        assertEquals(source.getZip(), result.getZip());
        assertEquals(source.getMunicipality(), result.getMunicipality());
        assertEquals(source.getProvince(), result.getProvince());
        assertEquals(source.getForeignState(), result.getForeignState());
        assertEquals(
                source.getMunicipalityDetails(),
                result.getMunicipalityDetails()
        );
    }

    @Test
    void testConvertInadToDigitalAddress() {
        DigitalAddressDto source = new DigitalAddressDto();
        source.setDigitalAddress("test@pec.it");

        DigitalAddress result =
                gatewayConverter.convertInadToDigitalAddress(
                        source,
                        DigitalAddressRecipientType.PERSONA_FISICA
                );

        assertNotNull(result);
        assertEquals("test@pec.it", result.getAddress());
        assertEquals(
                DigitalAddressRecipientType.PERSONA_FISICA.getValue(),
                result.getRecipient()
        );
        assertEquals("PEC", result.getType());
    }

    @Test
    void testConvertRegImpToPhysicalAddress() {
        GetAddressRegistroImpreseOKProfessionalAddressDto source =
                new GetAddressRegistroImpreseOKProfessionalAddressDto();

        source.setAddress("Via Roma");
        source.setMunicipality("Roma");
        source.setProvince("RM");
        source.setZip("00100");
        source.setStato("IT");

        PhysicalAddressDto result =
                gatewayConverter.convertRegImpToPhysicalAddress(source);

        assertNotNull(result);
        assertEquals("Via Roma", result.getAddress());
        assertEquals("Roma", result.getMunicipality());
        assertEquals("RM", result.getProvince());
        assertEquals("00100", result.getZip());
        assertEquals("IT", result.getForeignState());
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
    void testConvertToGetDigitalAddressInadRequest() {
        AddressRequestBodyFilterDto filter = new AddressRequestBodyFilterDto();
        filter.setCorrelationId(C_ID);
        filter.setTaxId(CF);

        AddressRequestBodyDto request = new AddressRequestBodyDto();
        request.setFilter(filter);

        GetDigitalAddressINADRequestBodyDto result =
                gatewayConverter.convertToGetDigitalAddressInadRequest(request);

        assertNotNull(result);
        assertNotNull(result.getFilter());
        assertEquals(CF, result.getFilter().getTaxId());
        assertEquals(C_ID, result.getFilter().getPracticalReference());
    }

    @Test
    void testConvertToGetDigitalAddressInadRequestFromBatchRequest() {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf(CF);
        batchRequest.setCorrelationId(C_ID);

        GetDigitalAddressINADRequestBodyDto result =
                gatewayConverter.convertToGetDigitalAddressInadRequest(batchRequest);

        assertNotNull(result);
        assertNotNull(result.getFilter());
        assertEquals(CF, result.getFilter().getTaxId());
        assertEquals(C_ID, result.getFilter().getPracticalReference());
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
    void testConvertToGetDigitalAddressIniPecRequest() {
        AddressRequestBodyFilterDto filter = new AddressRequestBodyFilterDto();
        filter.setCorrelationId(C_ID);
        filter.setTaxId(CF);

        AddressRequestBodyDto request = new AddressRequestBodyDto();
        request.setFilter(filter);

        GetDigitalAddressIniPECRequestBodyDto result =
                gatewayConverter.convertToGetDigitalAddressIniPecRequest(request);

        assertNotNull(result);
        assertNotNull(result.getFilter());
        assertEquals(C_ID, result.getFilter().getCorrelationId());
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
    void testEmptyDigitalCodeSqsDto() {
        CodeSqsDto result =
                gatewayConverter.emptyDigitalCodeSqsDto(C_ID);

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
}