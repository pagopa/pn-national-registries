package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.constant.*;
import it.pagopa.pn.national.registries.entity.NationalRegistriesRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.AddressQueryRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Slf4j
public class GatewayConverter {

    public static final Pattern ANPR_CF_NOT_FOUND = Pattern.compile("(\"codiceErroreAnomalia\")\\s*:\\s*\"(EN122)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INAD_CF_NOT_FOUND = Pattern.compile("(\"detail\")\\s*:\\s*\"(CF non trovato)\"",
            Pattern.CASE_INSENSITIVE);
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String CF_NOT_FOUND = "CF non trovato";

    public static AddressOKDto mapToAddressesOKDto(String correlationId) {
        AddressOKDto dto = new AddressOKDto();
        dto.setCorrelationId(correlationId);
        return dto;
    }

    public static CodeSqsDto anprToSqsDto(String correlationId, GetAddressANPROKDto anprResponse) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId, GatewayDownstreamService.ANPR);
        if (anprResponse != null && !CollectionUtils.isEmpty(anprResponse.getResidentialAddresses())) {
            codeSqsDto.setPhysicalAddress(convertAnprToPhysicalAddress(anprResponse.getResidentialAddresses().getFirst()));
        } else {
            log.info("correlationId: {} - ANPR - indirizzi non presenti", correlationId);
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
        return codeSqsDto;
    }

    public static CodeSqsDto errorAnprToSqsDto(String correlationId, Throwable throwable) {
        CodeSqsDto codeSqsDto = null;
        // per ANPR CF non trovato corrisponde a HTTP Status 404 e nel body codiceErroreAnomalia = "EN122"
        if (isAnprAddressNotFound(throwable)) {
            log.info("correlationId: {} - ANPR - CF non trovato", correlationId);
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
            codeSqsDto = newCodeSqsDto(correlationId, GatewayDownstreamService.ANPR);
            codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
        }
        return codeSqsDto;
    }

    public static CodeSqsDto inadToSqsDto(String correlationId, GetDigitalAddressINADOKDto inadDto) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId, GatewayDownstreamService.INAD);
        if (inadDto != null && inadDto.getDigitalAddress() != null) {
            codeSqsDto.setDigitalAddress(List.of(convertInadToDigitalAddress(inadDto.getDigitalAddress(), evaluteDigitalAddressRecipientType(inadDto))));
        } else {
            log.info("correlationId: {} - INAD - indirizzi non presenti", correlationId);
            codeSqsDto.setDigitalAddress(Collections.emptyList());
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        return codeSqsDto;
    }

    private static DigitalAddressRecipientType evaluteDigitalAddressRecipientType(GetDigitalAddressINADOKDto response) {
        if (StringUtils.hasText(response.getTaxId()) && response.getTaxId().length() == 11 && !StringUtils.hasText(response.getDigitalAddress().getPracticedProfession())) {
            return DigitalAddressRecipientType.IMPRESA;
        } else if(StringUtils.hasText(response.getTaxId())){
            return StringUtils.hasText(response.getDigitalAddress().getPracticedProfession()) ? DigitalAddressRecipientType.PROFESSIONISTA : DigitalAddressRecipientType.PERSONALE;
        }
        return null;
    }

    public static CodeSqsDto errorInadToSqsDto(String correlationId, Throwable throwable) {
        CodeSqsDto codeSqsDto = null;
        // per INAD CF non trovato corrisponde a HTTP Status 404 e nel body deve essere contenuta la stringa "CF non trovato"
        if (isInadAddressNotFound(throwable)) {
            log.info("correlationId: {} - INAD - CF non trovato", correlationId);
            codeSqsDto = newCodeSqsDto(correlationId,GatewayDownstreamService.INAD);
            codeSqsDto.setDigitalAddress(Collections.emptyList());
            codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        }
        return codeSqsDto;
    }

    private static boolean isAnprAddressNotFound(Throwable throwable) {
        return throwable instanceof PnNationalRegistriesException exception && exception.getStatusCode() == HttpStatus.NOT_FOUND
                && StringUtils.hasText(exception.getResponseBodyAsString())
                && ANPR_CF_NOT_FOUND.matcher(exception.getResponseBodyAsString()).find();
    }

    public static final Predicate<Throwable> isAnprAddressNotFound = t -> t instanceof PnNationalRegistriesException exception
            && exception.getStatusCode() == HttpStatus.NOT_FOUND
            && StringUtils.hasText(exception.getResponseBodyAsString())
            && ANPR_CF_NOT_FOUND.matcher(exception.getResponseBodyAsString()).find();


    private static boolean isInadAddressNotFound(Throwable throwable) {
        return throwable instanceof PnNationalRegistriesException exception && exception.getStatusCode() == HttpStatus.NOT_FOUND
                && (
                (StringUtils.hasText(exception.getResponseBodyAsString())
                        && INAD_CF_NOT_FOUND.matcher(exception.getResponseBodyAsString()).find())
                        || CF_NOT_FOUND.equalsIgnoreCase(exception.getMessage())
        );
    }

    public static CodeSqsDto regImpToSqsDto(String correlationId, GetAddressRegistroImpreseOKDto registroImpreseDto) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId, GatewayDownstreamService.REGISTRO_IMPRESE);
        if (registroImpreseDto != null && registroImpreseDto.getProfessionalAddress() != null) {
            codeSqsDto.setPhysicalAddress(convertRegImpToPhysicalAddress(registroImpreseDto.getProfessionalAddress()));
        } else {
            log.info("correlationId: {} - InfoCamere sede legale - indirizzo non presente", correlationId);
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
        return codeSqsDto;
    }

    public static CodeSqsDto ipaToSqsDto(String correlationId, IPAPecDto ipaResponse) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId, GatewayDownstreamService.IPA);
        if (ipaResponse != null && ipaResponse.getDomicilioDigitale() != null) {
            codeSqsDto.setDigitalAddress(List.of(convertIpaPecToDigitalAddress(ipaResponse)));
        } else {
            log.info("correlationId: {} - IPA - WS23 - domicili digitali non presenti", correlationId);
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        return codeSqsDto;
    }

    private static DigitalAddress convertIpaPecToDigitalAddress(IPAPecDto domicilioDigitale) {
        DigitalAddress digitalAddress = new DigitalAddress();
        digitalAddress.setAddress(domicilioDigitale.getDomicilioDigitale());
        digitalAddress.setRecipient(DigitalAddressRecipientType.IMPRESA.getValue());
        digitalAddress.setType(DigitalAddressType.PEC.getValue());
        return digitalAddress;
    }

    private static CodeSqsDto newCodeSqsDto(String correlationId, GatewayDownstreamService addressSourceEnum) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(correlationId);
        codeSqsDto.setRegistry(addressSourceEnum.name());
        return codeSqsDto;
    }

    private static PhysicalAddressDto convertAnprToPhysicalAddress(ResidentialAddressDto residenceDto) {
        PhysicalAddressDto physicalAddress = new PhysicalAddressDto();
        physicalAddress.setAddress(residenceDto.getAddress());
        physicalAddress.setAddressDetails(residenceDto.getAddressDetail());
        physicalAddress.setAt(residenceDto.getAt());
        physicalAddress.setZip(residenceDto.getZip());
        physicalAddress.setMunicipality(residenceDto.getMunicipality());
        physicalAddress.setProvince(residenceDto.getProvince());
        physicalAddress.setForeignState(residenceDto.getForeignState());
        physicalAddress.setMunicipalityDetails(residenceDto.getMunicipalityDetails());
        physicalAddress.setMunicipality(residenceDto.getMunicipality());
        return physicalAddress;
    }

    private static DigitalAddress convertInadToDigitalAddress(DigitalAddressDto digitalAddressDto, DigitalAddressRecipientType digitalAddressRecipientType) {
        DigitalAddress digitalAddress = new DigitalAddress();
        digitalAddress.setAddress(digitalAddressDto.getDigitalAddress());
        digitalAddress.setRecipient(digitalAddressRecipientType.getValue());
        digitalAddress.setType(DigitalAddressType.PEC.getValue());
        return digitalAddress;
    }

    private static PhysicalAddressDto convertRegImpToPhysicalAddress(GetAddressRegistroImpreseOKProfessionalAddressDto addressDto) {
        PhysicalAddressDto physicalAddress = new PhysicalAddressDto();
        physicalAddress.setProvince(addressDto.getProvince());
        physicalAddress.setAddress(addressDto.getAddress());
        physicalAddress.setMunicipality(addressDto.getMunicipality());
        physicalAddress.setZip(addressDto.getZip());
        physicalAddress.setForeignState(addressDto.getStato());
        return physicalAddress;
    }

    public static GetAddressANPRRequestBodyDto convertToGetAddressAnprRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetAddressANPRRequestBodyDto dto = new GetAddressANPRRequestBodyDto();
        GetAddressANPRRequestBodyFilterDto filterDto = new GetAddressANPRRequestBodyFilterDto();
        filterDto.setRequestReason(addressRequestBodyDto.getFilter().getCorrelationId());
        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());
        filterDto.setReferenceRequestDate(DateTimeFormatter.ofPattern(DATE_PATTERN)
                .withZone(ZoneId.systemDefault())
                .format(addressRequestBodyDto.getFilter().getReferenceRequestDate().toInstant()));


        dto.setFilter(filterDto);
        return dto;
    }

    public static GetAddressRegistroImpreseRequestBodyDto convertToGetAddressRegistroImpreseRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetAddressRegistroImpreseRequestBodyDto dto = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto filterDto = new GetAddressRegistroImpreseRequestBodyFilterDto();

        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());

        dto.setFilter(filterDto);
        return dto;
    }

    public static IPARequestBodyDto convertToGetIpaPecRequest(AddressRequestBodyDto addressRequestBodyDto) {
        IPARequestBodyDto dto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filterDto = new CheckTaxIdRequestBodyFilterDto();
        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());
        dto.setFilter(filterDto);
        return dto;
    }

    // START METHODS FOR MULTI ADDRESSES
    public static List<AddressQueryRequest> toAddressQueryRequests(PhysicalAddressesRequestBodyDto requestBodyDto) {
        return requestBodyDto.getAddresses().stream()
                .map(addressRequest -> AddressQueryRequest.builder()
                        .correlationId(requestBodyDto.getCorrelationId())
                        .referenceRequestDate(requestBodyDto.getReferenceRequestDate())
                        .taxId(addressRequest.getTaxId())
                        .recipientType(RecipientType.fromString(addressRequest.getRecipientType().name()))
                        .recIndex(addressRequest.getRecIndex())
                        .domicileType(DomicileType.PHYSICAL)
                        .build())
                .toList();
    }

    public static GetAddressANPRRequestBodyDto convertToGetAddressAnprRequest(AddressQueryRequest addressQueryRequest) {
        GetAddressANPRRequestBodyDto dto = new GetAddressANPRRequestBodyDto();
        GetAddressANPRRequestBodyFilterDto filterDto = new GetAddressANPRRequestBodyFilterDto();
        filterDto.setRequestReason(addressQueryRequest.getCorrelationId());
        filterDto.setTaxId(addressQueryRequest.getTaxId());
        filterDto.setReferenceRequestDate(DateTimeFormatter.ofPattern(DATE_PATTERN)
                .withZone(ZoneId.systemDefault())
                .format(addressQueryRequest.getReferenceRequestDate().toInstant()));

        dto.setFilter(filterDto);
        return dto;
    }

    public static PhysicalAddressResponseDto convertAnprResponseToInternalRecipientAddress(GetAddressANPROKDto response, AddressQueryRequest addressQueryRequest) {
        PhysicalAddressResponseDto addressResponseDto = new PhysicalAddressResponseDto();
        if (response != null && !CollectionUtils.isEmpty(response.getResidentialAddresses())) {
            addressResponseDto.setPhysicalAddress(convertAnprToPhysicalAddress(response.getResidentialAddresses().getFirst()));
        } else {
            log.info("correlationId: {} recIndex: {} - ANPR - indirizzi non presenti", addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex());
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
        }
        addressResponseDto.setRecIndex(addressQueryRequest.getRecIndex());
        addressResponseDto.setRegistry(GatewayDownstreamService.ANPR.name());
        return addressResponseDto;
    }

    public static PhysicalAddressResponseDto anprNotFoundErrorToPhysicalAddressSQSMessage(AddressQueryRequest addressQueryRequest) {
        PhysicalAddressResponseDto addressResponseDto = new PhysicalAddressResponseDto();
        addressResponseDto.setRecIndex(addressQueryRequest.getRecIndex());
        addressResponseDto.setRegistry(GatewayDownstreamService.ANPR.name());
        return addressResponseDto;
    }

    public static GetAddressRegistroImpreseRequestBodyDto convertToGetAddressRegistroImpreseRequest(AddressQueryRequest addressQueryRequest) {
        GetAddressRegistroImpreseRequestBodyDto dto = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto filterDto = new GetAddressRegistroImpreseRequestBodyFilterDto();

        filterDto.setTaxId(addressQueryRequest.getTaxId());

        dto.setFilter(filterDto);
        return dto;
    }

    public static PhysicalAddressResponseDto convertRegImprResponseToInternalRecipientAddress(GetAddressRegistroImpreseOKDto response, AddressQueryRequest addressQueryRequest) {
        PhysicalAddressResponseDto addressInfo = new PhysicalAddressResponseDto();
        if (response != null && response.getProfessionalAddress() != null) {
            addressInfo.setPhysicalAddress(convertRegImpToPhysicalAddress(response.getProfessionalAddress()));
        } else {
            log.info("correlationId: {} - InfoCamere sede legale - indirizzo non presente", addressQueryRequest.getCorrelationId());
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
        }
        addressInfo.setRecIndex(addressQueryRequest.getRecIndex());
        addressInfo.setRegistry(GatewayDownstreamService.REGISTRO_IMPRESE.name());
        return addressInfo;
    }

    public static PhysicalAddressesResponseDto convertToPhysicalAddressesResponseDto(List<PhysicalAddressResponseDto> addressResponse, String correlationId) {
        PhysicalAddressesResponseDto response = new PhysicalAddressesResponseDto();
        response.setCorrelationId(correlationId);
        response.setAddresses(addressResponse);
        return response;
    }

    // END METHODS FOR MULTI ADDRESSES

    public static AddressRequestBodyDto toAddressRequestBodyDto(PnAddressGatewayEvent.Payload payload) {
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        AddressRequestBodyFilterDto addressRequestBodyFilterDto = new AddressRequestBodyFilterDto();
        addressRequestBodyFilterDto.setCorrelationId(payload.getCorrelationId());
        addressRequestBodyFilterDto.setReferenceRequestDate(payload.getReferenceRequestDate());
        addressRequestBodyFilterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.fromValue(payload.getDomicileType()));
        addressRequestBodyFilterDto.setTaxId(payload.getTaxId());
        addressRequestBodyDto.setFilter(addressRequestBodyFilterDto);
        return addressRequestBodyDto;
    }

    public static InternalCodeSqsDto toInternalCodeSqsDto(AddressRequestBodyFilterDto filter, String recipientType, String pnNationalRegistriesCxId) {
        return InternalCodeSqsDto.builder()
                .taxId(filter.getTaxId())
                .correlationId(filter.getCorrelationId())
                .recipientType(recipientType)
                .domicileType(filter.getDomicileType().getValue())
                .referenceRequestDate(filter.getReferenceRequestDate())
                .pnNationalRegistriesCxId(pnNationalRegistriesCxId)
                .build();
    }


    public static NationalRegistriesRequest constructNationalRegistriesRequest(AddressRequestBodyDto addressRequestBodyDto, RecipientType recipientType, String pnNationalRegistriesCxId, GatewayDownstreamService registry) {
        NationalRegistriesRequest nationalRegistriesRequest = new NationalRegistriesRequest();

        nationalRegistriesRequest.setCreatedAt(Instant.now());
        nationalRegistriesRequest.setCorrelationId(addressRequestBodyDto.getFilter().getCorrelationId());
        nationalRegistriesRequest.setReferenceRequestDate(addressRequestBodyDto.getFilter().getReferenceRequestDate().toString());
        nationalRegistriesRequest.setDomicileType(addressRequestBodyDto.getFilter().getDomicileType().getValue());
        nationalRegistriesRequest.setTaxId(addressRequestBodyDto.getFilter().getTaxId());
        nationalRegistriesRequest.setRecipientType(recipientType.name());
        nationalRegistriesRequest.setClientId(pnNationalRegistriesCxId);
        nationalRegistriesRequest.setRegistry(registry);
        nationalRegistriesRequest.setStatus(RequestStatusEnum.NOT_WORKED);
        nationalRegistriesRequest.setRegistryStatus(String.join("#", registry.name(), RequestStatusEnum.NOT_WORKED.name()));

        return nationalRegistriesRequest;
    }

    public static GetDigitalAddressINADRequestBodyDto convertToGetDigitalAddressInadRequest(String correlationId, String taxId) {
        GetDigitalAddressINADRequestBodyDto dto = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();

        filterDto.setTaxId(taxId);
        filterDto.setPracticalReference(correlationId);

        dto.setFilter(filterDto);
        return dto;
    }
}
