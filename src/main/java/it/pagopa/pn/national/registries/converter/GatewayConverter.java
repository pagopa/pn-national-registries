package it.pagopa.pn.national.registries.converter;

import io.netty.handler.codec.DateFormatter;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DigitalAddressType;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.PhysicalAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class GatewayConverter {

    private static final Pattern ANPR_CF_NOT_FOUND = Pattern.compile("(\"codiceErroreAnomalia\")\\s*:\\s*\"(EN122)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INAD_CF_NOT_FOUND = Pattern.compile("(\"detail\")\\s*:\\s*\"(CF non trovato)\"",
            Pattern.CASE_INSENSITIVE);

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
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId);
        // per ANPR CF non trovato corrisponde a HTTP Status 404 e nel body codiceErroreAnomalia = "EN122"
        if (throwable instanceof PnNationalRegistriesException exception
                && exception.getStatusCode() == HttpStatus.NOT_FOUND
                && StringUtils.hasText(exception.getResponseBodyAsString())
                && ANPR_CF_NOT_FOUND.matcher(exception.getResponseBodyAsString()).find()) {
            log.info("correlationId: {} - ANPR - CF non trovato", correlationId);
            // il physicalAddress rimane null, sarà compito di chi serializzerà il JSON occuparsi d'includere il campo
        } else {
            codeSqsDto.setError(throwable.getMessage());
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
        return codeSqsDto;
    }

    protected CodeSqsDto inadToSqsDto(String correlationId, GetDigitalAddressINADOKDto inadDto) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId);
        if (inadDto != null && inadDto.getDigitalAddress() != null) {
            List<DigitalAddress> address = inadDto.getDigitalAddress().stream()
                    .map(this::convertInadToDigitalAddress)
                    .toList();
            codeSqsDto.setDigitalAddress(address);
        } else {
            log.info("correlationId: {} - INAD - indirizzi non presenti", correlationId);
            codeSqsDto.setDigitalAddress(Collections.emptyList());
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        return codeSqsDto;
    }

    protected CodeSqsDto errorInadToSqsDto(String correlationId, Throwable throwable) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId);
        // per INAD CF non trovato corrisponde a HTTP Status 404 e nel body deve essere contenuta la stringa "CF non trovato"
        if (throwable instanceof PnNationalRegistriesException exception
                && exception.getStatusCode() == HttpStatus.NOT_FOUND
                && StringUtils.hasText(exception.getResponseBodyAsString())
                && INAD_CF_NOT_FOUND.matcher(exception.getResponseBodyAsString()).find()) {
            log.info("correlationId: {} - INAD - CF non trovato", correlationId);
            codeSqsDto.setDigitalAddress(Collections.emptyList());
        } else {
            codeSqsDto.setError(throwable.getMessage());
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        return codeSqsDto;
    }

    protected CodeSqsDto errorIpaToSqsDto(String correlationId, Throwable throwable) {
        CodeSqsDto codeSqsDto = newCodeSqsDto(correlationId);
        if (throwable instanceof PnNationalRegistriesException exception
                && exception.getStatusCode() == HttpStatus.BAD_REQUEST
                && StringUtils.hasText(exception.getResponseBodyAsString())) {
            log.info("correlationId: {} - IPA - " + throwable.getMessage(), correlationId);
            codeSqsDto.setDigitalAddress(Collections.emptyList());
        } else {
            codeSqsDto.setError(throwable.getMessage());
        }
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
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
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.getValue());
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

    protected DigitalAddress convertInadToDigitalAddress(DigitalAddressDto digitalAddressDto) {
        return new DigitalAddress(DigitalAddressType.PEC.getValue(), digitalAddressDto.getDigitalAddress(), DigitalAddressRecipientType.PERSONA_FISICA.getValue());
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
        filterDto.setReferenceRequestDate(addressRequestBodyDto.getFilter().getReferenceRequestDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime().toString().substring(0, 10));

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

}
