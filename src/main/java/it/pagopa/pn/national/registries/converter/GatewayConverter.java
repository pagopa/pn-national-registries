package it.pagopa.pn.national.registries.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.constant.*;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.gateway.AddressQueryRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.utils.CheckEmailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class GatewayConverter {

    public static final Pattern ANPR_CF_NOT_FOUND = Pattern.compile("(\"codiceErroreAnomalia\")\\s*:\\s*\"(EN122)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INAD_CF_NOT_FOUND = Pattern.compile("(\"detail\")\\s*:\\s*\"(CF non trovato)\"",
            Pattern.CASE_INSENSITIVE);
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String CF_NOT_FOUND = "CF non trovato";

    @Autowired
    private ObjectMapper mapper;

    protected AddressOKDto mapToAddressesOKDto(String correlationId) {
        AddressOKDto dto = new AddressOKDto();
        dto.setCorrelationId(correlationId);
        return dto;
    }

    protected AddressSQSMessageDto anprToSqsDto(String correlationId, GetAddressANPROKDto anprResponse) {
        AddressSQSMessageDto codeSqsDto = newCodeSqsDto(correlationId, GatewayDownstreamService.ANPR);
        if (anprResponse != null && !CollectionUtils.isEmpty(anprResponse.getResidentialAddresses())) {
            codeSqsDto.setPhysicalAddress(convertAnprToPhysicalAddress(anprResponse.getResidentialAddresses().getFirst()));
        } else {
            log.info("correlationId: {} - ANPR - indirizzi non presenti", correlationId);
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
        return codeSqsDto;
    }

    protected AddressSQSMessageDto errorAnprToSqsDto(String correlationId, Throwable throwable) {
        AddressSQSMessageDto codeSqsDto = null;
        // per ANPR CF non trovato corrisponde a HTTP Status 404 e nel body codiceErroreAnomalia = "EN122"
        if (throwable instanceof PnNationalRegistriesException exception
                && exception.getStatusCode() == HttpStatus.NOT_FOUND
                && StringUtils.hasText(exception.getResponseBodyAsString())
                && ANPR_CF_NOT_FOUND.matcher(exception.getResponseBodyAsString()).find()) {
            log.info("correlationId: {} - ANPR - CF non trovato", correlationId);
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
            codeSqsDto = newCodeSqsDto(correlationId, GatewayDownstreamService.ANPR);
            codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
        }
        return codeSqsDto;
    }

    protected AddressSQSMessageDto inadToSqsDto(String correlationId, GetDigitalAddressINADOKDto inadDto, DigitalAddressRecipientType digitalAddressRecipientType) {
        AddressSQSMessageDto codeSqsDto = newCodeSqsDto(correlationId, GatewayDownstreamService.INAD);
        if (inadDto != null && inadDto.getDigitalAddress() != null) {
            codeSqsDto.setDigitalAddress(List.of(convertInadToDigitalAddress(inadDto.getDigitalAddress(), digitalAddressRecipientType)));
        } else {
            log.info("correlationId: {} - INAD - indirizzi non presenti", correlationId);
            codeSqsDto.setDigitalAddress(Collections.emptyList());
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        return codeSqsDto;
    }

    protected AddressSQSMessageDto errorInadToSqsDto(String correlationId, Throwable throwable) {
        AddressSQSMessageDto codeSqsDto = null;
        // per INAD CF non trovato corrisponde a HTTP Status 404 e nel body deve essere contenuta la stringa "CF non trovato"
        if (throwable instanceof PnNationalRegistriesException exception
                && exception.getStatusCode() == HttpStatus.NOT_FOUND
                && ((StringUtils.hasText(exception.getResponseBodyAsString())
                && INAD_CF_NOT_FOUND.matcher(exception.getResponseBodyAsString()).find())
        || CF_NOT_FOUND.equalsIgnoreCase(exception.getMessage()))) {
            log.info("correlationId: {} - INAD - CF non trovato", correlationId);
            codeSqsDto = newCodeSqsDto(correlationId,GatewayDownstreamService.INAD);
            codeSqsDto.setDigitalAddress(Collections.emptyList());
            codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        }
        return codeSqsDto;
    }

    protected AddressSQSMessageDto regImpToSqsDto(String correlationId, GetAddressRegistroImpreseOKDto registroImpreseDto) {
        AddressSQSMessageDto codeSqsDto = newCodeSqsDto(correlationId, GatewayDownstreamService.REGISTRO_IMPRESE);
        if (registroImpreseDto != null && registroImpreseDto.getProfessionalAddress() != null) {
            codeSqsDto.setPhysicalAddress(convertRegImpToPhysicalAddress(registroImpreseDto.getProfessionalAddress()));
        } else {
            log.info("correlationId: {} - InfoCamere sede legale - indirizzo non presente", correlationId);
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
        return codeSqsDto;
    }

    protected AddressSQSMessageDto ipaToSqsDto(String correlationId, IPAPecDto ipaResponse) {
        AddressSQSMessageDto codeSqsDto = newCodeSqsDto(correlationId, GatewayDownstreamService.IPA);
        if (ipaResponse != null && ipaResponse.getDomicilioDigitale() != null) {
            codeSqsDto.setDigitalAddress(List.of(convertIpaPecToDigitalAddress(ipaResponse)));
        } else {
            log.info("correlationId: {} - IPA - WS23 - domicili digitali non presenti", correlationId);
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        return codeSqsDto;
    }

    private AddressSQSMessageDigitalAddressInnerDto convertIpaPecToDigitalAddress(IPAPecDto domicilioDigitale) {
        AddressSQSMessageDigitalAddressInnerDto digitalAddress = new AddressSQSMessageDigitalAddressInnerDto();
        digitalAddress.setAddress(domicilioDigitale.getDomicilioDigitale());
        digitalAddress.setRecipient(AddressSQSMessageDigitalAddressInnerDto.RecipientEnum.fromValue(DigitalAddressRecipientType.IMPRESA.getValue()));
        digitalAddress.setType(DigitalAddressType.PEC.getValue());
        return digitalAddress;
    }

    protected AddressSQSMessageDto newCodeSqsDto(String correlationId, GatewayDownstreamService addressSourceEnum) {
        AddressSQSMessageDto codeSqsDto = new AddressSQSMessageDto();
        codeSqsDto.setCorrelationId(correlationId);
        codeSqsDto.setRegistry(addressSourceEnum.name());
        return codeSqsDto;
    }

    protected PhysicalAddressDto convertAnprToPhysicalAddress(ResidentialAddressDto residenceDto) {
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

    protected AddressSQSMessageDigitalAddressInnerDto convertInadToDigitalAddress(DigitalAddressDto digitalAddressDto, DigitalAddressRecipientType digitalAddressRecipientType) {
        AddressSQSMessageDigitalAddressInnerDto digitalAddress = new AddressSQSMessageDigitalAddressInnerDto();
        digitalAddress.setAddress(digitalAddressDto.getDigitalAddress());
        digitalAddress.setRecipient(AddressSQSMessageDigitalAddressInnerDto.RecipientEnum.fromValue(digitalAddressRecipientType.getValue()));
        digitalAddress.setType(DigitalAddressType.PEC.getValue());
        return digitalAddress;
    }

    protected PhysicalAddressDto convertRegImpToPhysicalAddress(GetAddressRegistroImpreseOKProfessionalAddressDto addressDto) {
        PhysicalAddressDto physicalAddress = new PhysicalAddressDto();
        physicalAddress.setProvince(addressDto.getProvince());
        physicalAddress.setAddress(addressDto.getAddress());
        physicalAddress.setMunicipality(addressDto.getMunicipality());
        physicalAddress.setZip(addressDto.getZip());
        physicalAddress.setForeignState(addressDto.getStato());
        return physicalAddress;
    }

    protected GetAddressANPRRequestBodyDto convertToGetAddressAnprRequest(AddressRequestBodyDto addressRequestBodyDto) {
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

    protected GetDigitalAddressINADRequestBodyDto convertToGetDigitalAddressInadRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetDigitalAddressINADRequestBodyDto dto = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();

        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());
        filterDto.setPracticalReference(addressRequestBodyDto.getFilter().getCorrelationId());

        dto.setFilter(filterDto);
        return dto;
    }

    protected GetDigitalAddressINADRequestBodyDto convertToGetDigitalAddressInadRequest(BatchRequest batchRequest) {
        GetDigitalAddressINADRequestBodyDto dto = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();

        filterDto.setTaxId(batchRequest.getCf());
        filterDto.setPracticalReference(batchRequest.getCorrelationId());

        dto.setFilter(filterDto);
        return dto;
    }

    protected GetAddressRegistroImpreseRequestBodyDto convertToGetAddressRegistroImpreseRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetAddressRegistroImpreseRequestBodyDto dto = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto filterDto = new GetAddressRegistroImpreseRequestBodyFilterDto();

        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());

        dto.setFilter(filterDto);
        return dto;
    }

    protected GetDigitalAddressIniPECRequestBodyDto convertToGetDigitalAddressIniPecRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetDigitalAddressIniPECRequestBodyDto dto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto filterDto = new GetDigitalAddressIniPECRequestBodyFilterDto();

        filterDto.setCorrelationId(addressRequestBodyDto.getFilter().getCorrelationId());
        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());

        dto.setFilter(filterDto);
        return dto;
    }

    protected IPARequestBodyDto convertToGetIpaPecRequest(AddressRequestBodyDto addressRequestBodyDto) {
        IPARequestBodyDto dto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filterDto = new CheckTaxIdRequestBodyFilterDto();
        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());
        dto.setFilter(filterDto);
        return dto;
    }

    protected IPARequestBodyDto convertToGetIpaPecRequest(BatchRequest batchRequest) {
        IPARequestBodyDto dto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filterDto = new CheckTaxIdRequestBodyFilterDto();
        filterDto.setTaxId(batchRequest.getCf());
        dto.setFilter(filterDto);
        return dto;
    }

    public String convertCodeSqsDtoToString(AddressSQSMessageDto codeSqsDto) {
        try {
            return mapper.writeValueAsString(codeSqsDto);
        } catch (JsonProcessingException e) {
            throw new DigitalAddressException("can not convert SQS DTO to String", e);
        }
    }

    protected Mono<GetDigitalAddressINADOKDto> emailValidation(GetDigitalAddressINADOKDto inadResponse) {
        if (!CheckEmailUtils.isValidEmail(inadResponse.getDigitalAddress().getDigitalAddress())) {
            return Mono.error(new PnNationalRegistriesException(CF_NOT_FOUND, HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(), null, null, Charset.defaultCharset(), InadResponseKO.class));
        }
        return Mono.just(inadResponse);
    }

    // START METHODS FOR MULTI ADDRESSES
    protected List<AddressQueryRequest> toAddressQueryRequests(PhysicalAddressesRequestBodyDto requestBodyDto) {
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

    protected GetAddressANPRRequestBodyDto convertToGetAddressAnprRequest(AddressQueryRequest addressQueryRequest) {
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

    protected PhysicalAddressResponseDto convertAnprResponseToInternalRecipientAddress(GetAddressANPROKDto response, AddressQueryRequest addressQueryRequest) {
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

    protected PhysicalAddressResponseDto anprNotFoundErrorToPhysicalAddressSQSMessage(AddressQueryRequest addressQueryRequest) {
        PhysicalAddressResponseDto addressResponseDto = new PhysicalAddressResponseDto();
        addressResponseDto.setRecIndex(addressQueryRequest.getRecIndex());
        addressResponseDto.setRegistry(GatewayDownstreamService.ANPR.name());
        return addressResponseDto;
    }

    protected GetAddressRegistroImpreseRequestBodyDto convertToGetAddressRegistroImpreseRequest(AddressQueryRequest addressQueryRequest) {
        GetAddressRegistroImpreseRequestBodyDto dto = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto filterDto = new GetAddressRegistroImpreseRequestBodyFilterDto();

        filterDto.setTaxId(addressQueryRequest.getTaxId());

        dto.setFilter(filterDto);
        return dto;
    }

    protected PhysicalAddressResponseDto convertRegImprResponseToInternalRecipientAddress(GetAddressRegistroImpreseOKDto response, AddressQueryRequest addressQueryRequest) {
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

    protected PhysicalAddressesResponseDto convertToPhysicalAddressesResponseDto(List<PhysicalAddressResponseDto> addressResponse, String correlationId) {
        PhysicalAddressesResponseDto response = new PhysicalAddressesResponseDto();
        response.setCorrelationId(correlationId);
        response.setAddresses(addressResponse);
        return response;
    }

    // END METHODS FOR MULTI ADDRESSES

}
