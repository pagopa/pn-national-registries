package it.pagopa.pn.national.registries.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DigitalAddressType;
import it.pagopa.pn.national.registries.constant.DomicileType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressesGatewayEvent;
import it.pagopa.pn.national.registries.model.*;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.PhysicalAddress;
import it.pagopa.pn.national.registries.utils.CheckEmailUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
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

    protected CodeSqsDto anprToSqsDto(String correlationId, GetAddressANPROKDto anprResponse) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId);
        if (anprResponse != null && !CollectionUtils.isEmpty(anprResponse.getResidentialAddresses())) {
            codeSqsDto.setPhysicalAddress(convertAnprToPhysicalAddress(anprResponse.getResidentialAddresses().get(0)));
        } else {
            log.info("correlationId: {} - ANPR - indirizzi non presenti", correlationId);
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
        return codeSqsDto;
    }

    protected CodeSqsDto errorAnprToSqsDto(String correlationId, Throwable throwable) {
        CodeSqsDto codeSqsDto = null;
        // per ANPR CF non trovato corrisponde a HTTP Status 404 e nel body codiceErroreAnomalia = "EN122"
        if (throwable instanceof PnNationalRegistriesException exception
                && exception.getStatusCode() == HttpStatus.NOT_FOUND
                && StringUtils.hasText(exception.getResponseBodyAsString())
                && ANPR_CF_NOT_FOUND.matcher(exception.getResponseBodyAsString()).find()) {
            log.info("correlationId: {} - ANPR - CF non trovato", correlationId);
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
            codeSqsDto = newCodeSqsDto(correlationId);
            codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
        }
        return codeSqsDto;
    }

    protected CodeSqsDto inadToSqsDto(String correlationId, GetDigitalAddressINADOKDto inadDto, DigitalAddressRecipientType digitalAddressRecipientType) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId);
        if (inadDto != null && inadDto.getDigitalAddress() != null) {
            codeSqsDto.setDigitalAddress(List.of(convertInadToDigitalAddress(inadDto.getDigitalAddress(), digitalAddressRecipientType)));
        } else {
            log.info("correlationId: {} - INAD - indirizzi non presenti", correlationId);
            codeSqsDto.setDigitalAddress(Collections.emptyList());
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        return codeSqsDto;
    }

    protected CodeSqsDto errorInadToSqsDto(String correlationId, Throwable throwable) {
        CodeSqsDto codeSqsDto = null;
        // per INAD CF non trovato corrisponde a HTTP Status 404 e nel body deve essere contenuta la stringa "CF non trovato"
        if (throwable instanceof PnNationalRegistriesException exception
                && exception.getStatusCode() == HttpStatus.NOT_FOUND
                && ((StringUtils.hasText(exception.getResponseBodyAsString())
                && INAD_CF_NOT_FOUND.matcher(exception.getResponseBodyAsString()).find())
        || CF_NOT_FOUND.equalsIgnoreCase(exception.getMessage()))) {
            log.info("correlationId: {} - INAD - CF non trovato", correlationId);
            codeSqsDto = newCodeSqsDto(correlationId);
            codeSqsDto.setDigitalAddress(Collections.emptyList());
            codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        }
        return codeSqsDto;
    }

    protected CodeSqsDto regImpToSqsDto(String correlationId, GetAddressRegistroImpreseOKDto registroImpreseDto) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId);
        if (registroImpreseDto != null && registroImpreseDto.getProfessionalAddress() != null) {
            codeSqsDto.setPhysicalAddress(convertRegImpToPhysicalAddress(registroImpreseDto.getProfessionalAddress()));
        } else {
            log.info("correlationId: {} - InfoCamere sede legale - indirizzo non presente", correlationId);
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
        return codeSqsDto;
    }

    protected CodeSqsDto ipaToSqsDto(String correlationId, IPAPecDto ipaResponse) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId);
        if (ipaResponse != null && ipaResponse.getDomicilioDigitale() != null) {
            codeSqsDto.setDigitalAddress(List.of(convertIpaPecToDigitalAddress(ipaResponse)));
        } else {
            log.info("correlationId: {} - IPA - WS23 - domicili digitali non presenti", correlationId);
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        return codeSqsDto;
    }

    private DigitalAddress convertIpaPecToDigitalAddress(IPAPecDto domicilioDigitale) {
        return new DigitalAddress(DigitalAddressType.PEC.getValue(),
                domicilioDigitale.getDomicilioDigitale(),
                DigitalAddressRecipientType.IMPRESA.getValue());

    }

    protected CodeSqsDto errorRegImpToSqsDto(String correlationId, Throwable error) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId);
        codeSqsDto.setError(error.getMessage());
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
        return codeSqsDto;
    }

    protected CodeSqsDto newCodeSqsDto(String correlationId) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(correlationId);
        return codeSqsDto;
    }

    protected PhysicalAddress convertAnprToPhysicalAddress(ResidentialAddressDto residenceDto) {
        PhysicalAddress physicalAddress = new PhysicalAddress();
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

    protected DigitalAddress convertInadToDigitalAddress(DigitalAddressDto digitalAddressDto, DigitalAddressRecipientType digitalAddressRecipientType) {
        return new DigitalAddress(DigitalAddressType.PEC.getValue(), digitalAddressDto.getDigitalAddress(), digitalAddressRecipientType.getValue());
    }

    protected PhysicalAddress convertRegImpToPhysicalAddress(GetAddressRegistroImpreseOKProfessionalAddressDto addressDto) {
        PhysicalAddress physicalAddress = new PhysicalAddress();
        physicalAddress.setProvince(addressDto.getProvince());
        physicalAddress.setAddress(addressDto.getAddress());
        physicalAddress.setMunicipality(addressDto.getMunicipality());
        physicalAddress.setZip(addressDto.getZip());
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

    protected IPARequestBodyDto convertToGetIpaPecRequest(BatchRequest batchRequest) {
        IPARequestBodyDto dto = new IPARequestBodyDto();
        CheckTaxIdRequestBodyFilterDto filterDto = new CheckTaxIdRequestBodyFilterDto();
        filterDto.setTaxId(batchRequest.getCf());
        dto.setFilter(filterDto);
        return dto;
    }

    public String convertCodeSqsDtoToString(CodeSqsDto codeSqsDto) {
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
    protected List<AddressQueryRequest> toAddressQueryRequests(PnAddressesGatewayEvent.Payload payload) {
        return payload.getInternalRecipientAdresses().stream()
                .map(addressRequestBodyDto -> AddressQueryRequest.builder()
                        .correlationId(payload.getCorrelationId())
                        .pnNationalRegistriesCxId(payload.getPnNationalRegistriesCxId())
                        .referenceRequestDate(payload.getReferenceRequestDate())
                        .taxId(addressRequestBodyDto.getTaxId())
                        .recipientType(RecipientType.fromString(addressRequestBodyDto.getRecipientType()))
                        .recIndex(addressRequestBodyDto.getRecIndex())
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

    protected MultiCodeSqsDto.PhysicalAddressSQSMessage convertAnprResponseToInternalRecipientAddress(GetAddressANPROKDto response, AddressQueryRequest addressQueryRequest) {
        MultiCodeSqsDto.PhysicalAddressSQSMessage physicalAddressSQSMessage = new MultiCodeSqsDto.PhysicalAddressSQSMessage();
        if (response != null && !CollectionUtils.isEmpty(response.getResidentialAddresses())) {
            physicalAddressSQSMessage.setPhysicalAddress(convertAnprToPhysicalAddress(response.getResidentialAddresses().get(0)));
        } else {
            log.info("correlationId: {} recIndex: {} - ANPR - indirizzi non presenti", addressQueryRequest.getCorrelationId(), addressQueryRequest.getRecIndex());
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
        }
        physicalAddressSQSMessage.setRecIndex(addressQueryRequest.getRecIndex());
        physicalAddressSQSMessage.setRegistry(GatewayDownstreamService.ANPR.name());
        return physicalAddressSQSMessage;
    }

    protected MultiCodeSqsDto.PhysicalAddressSQSMessage anprNotFoundErrorToPhysicalAddressSQSMessage(AddressQueryRequest addressQueryRequest) {
        MultiCodeSqsDto.PhysicalAddressSQSMessage physicalAddressSQSMessage = new MultiCodeSqsDto.PhysicalAddressSQSMessage();
        physicalAddressSQSMessage.setRecIndex(addressQueryRequest.getRecIndex());
        physicalAddressSQSMessage.setRegistry(GatewayDownstreamService.ANPR.name());
        return physicalAddressSQSMessage;
    }

    protected GetAddressRegistroImpreseRequestBodyDto convertToGetAddressRegistroImpreseRequest(AddressQueryRequest addressQueryRequest) {
        GetAddressRegistroImpreseRequestBodyDto dto = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto filterDto = new GetAddressRegistroImpreseRequestBodyFilterDto();

        filterDto.setTaxId(addressQueryRequest.getTaxId());

        dto.setFilter(filterDto);
        return dto;
    }

    protected MultiCodeSqsDto.PhysicalAddressSQSMessage convertRegImprResponseToInternalRecipientAddress(GetAddressRegistroImpreseOKDto response, AddressQueryRequest addressQueryRequest) {
        MultiCodeSqsDto.PhysicalAddressSQSMessage physicalAddressSQSMessage = new MultiCodeSqsDto.PhysicalAddressSQSMessage();
        if (response != null && response.getProfessionalAddress() != null) {
            physicalAddressSQSMessage.setPhysicalAddress(convertRegImpToPhysicalAddress(response.getProfessionalAddress()));
        } else {
            log.info("correlationId: {} - InfoCamere sede legale - indirizzo non presente", addressQueryRequest.getCorrelationId());
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
        }
        physicalAddressSQSMessage.setRecIndex(addressQueryRequest.getRecIndex());
        physicalAddressSQSMessage.setRegistry(GatewayDownstreamService.REGISTRO_IMPRESE.name());
        return physicalAddressSQSMessage;
    }

    protected MultiCodeSqsDto convertToMultiCodeSqsDto(List<MultiCodeSqsDto.PhysicalAddressSQSMessage> addesses, String correlationId) {
        MultiCodeSqsDto multiCodeSqsDto = new MultiCodeSqsDto();
        multiCodeSqsDto.setCorrelationId(correlationId);
        multiCodeSqsDto.setAddressType(DomicileType.PHYSICAL.name());
        multiCodeSqsDto.setAddresses(addesses);
        return multiCodeSqsDto;
    }

    protected MultiRecipientCodeSqsDto convertToMultiRecipientCodeSqsDto (List<AddressQueryRequest> addressQueryRequests) {
        String commonCorrelationId = addressQueryRequests.get(0).getCorrelationId();
        String commonPnNationalRegistriesCxId = addressQueryRequests.get(0).getPnNationalRegistriesCxId();
        Date commonReferenceRequestDate = addressQueryRequests.get(0).getReferenceRequestDate();
        return MultiRecipientCodeSqsDto.builder()
                .correlationId(commonCorrelationId)
                .pnNationalRegistriesCxId(commonPnNationalRegistriesCxId)
                .referenceRequestDate(commonReferenceRequestDate)
                .internalRecipientAdresses(convertToInternalRecipientAddresses(addressQueryRequests))
                .build();
    }

    private static @NotNull List<MultiRecipientCodeSqsDto.InternalRecipientAddress> convertToInternalRecipientAddresses(List<AddressQueryRequest> addressQueryRequests) {
        return addressQueryRequests.stream()
                .map(addressQueryRequest -> MultiRecipientCodeSqsDto.InternalRecipientAddress.builder()
                        .recipientType(addressQueryRequest.getRecipientType().name())
                        .recIndex(addressQueryRequest.getRecIndex())
                        .taxId(addressQueryRequest.getTaxId())
                        .domicileType(addressQueryRequest.getDomicileType().name())
                        .build()
                )
                .toList();
    }
    // END METHODS FOR MULTI ADDRESSES

}
