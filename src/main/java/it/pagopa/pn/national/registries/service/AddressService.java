package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DigitalAddressType;
import it.pagopa.pn.national.registries.converter.AddressAnprConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.model.anpr.ResponseE002OKDto;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.PhysicalAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AddressService {

    private final AnprService anprService;
    private final InadService inadService;
    private final InfoCamereService infoCamereService;
    private final SqsService sqsService;
    private final AddressAnprConverter addressAnprConverter;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public AddressService(AnprService anprService,
                          InadService inadService,
                          InfoCamereService infoCamereService,
                          SqsService sqsService,
                          AddressAnprConverter addressAnprConverter) {
        this.anprService = anprService;
        this.inadService = inadService;
        this.infoCamereService = infoCamereService;
        this.sqsService = sqsService;
        this.addressAnprConverter = addressAnprConverter;
    }

    public Mono<AddressOKDto> retrieveDigitalOrPhysicalAddress(String recipientType, AddressRequestBodyDto addressRequestBodyDto) {
        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();
        String cf = addressRequestBodyDto.getFilter().getTaxId();
        log.info("recipientType {} and domicileType {}", recipientType, addressRequestBodyDto.getFilter().getDomicileType());
        switch (recipientType) {
            case "PF":
                if (AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.equals(addressRequestBodyDto.getFilter().getDomicileType())) {
                    return anprService.getRawAddressANPR(convertToGetAddressAnprRequest(addressRequestBodyDto))
                            .flatMap(anprResponse -> sqsService.push(anprToSqsDto(correlationId, cf, anprResponse))
                                    .map(sqs -> mapToAddressesOKDto(correlationId)));
                } else {
                    return inadService.callEService(convertToGetDigitalAddressInadRequest(addressRequestBodyDto))
                            .flatMap(inadResponse -> sqsService.push(inadToSqsDto(correlationId, cf, inadResponse))
                                    .map(sqs -> mapToAddressesOKDto(correlationId)));
                }
            case "PG":
                if (addressRequestBodyDto.getFilter().getDomicileType().equals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL)) {
                    return infoCamereService.getRegistroImpreseLegalAddress(convertToGetAddressRegistroImpreseRequest(addressRequestBodyDto))
                            .flatMap(registroImpreseResponse -> sqsService.push(regImpToSqsDto(correlationId, cf, registroImpreseResponse))
                                    .map(sqs -> mapToAddressesOKDto(correlationId)));
                } else {
                    return infoCamereService.getIniPecDigitalAddress(convertToGetDigitalAddressIniPecRequest(addressRequestBodyDto))
                            .map(iniPecResponse -> mapToAddressesOKDto(correlationId));
                }
            default:
                log.warn("recipientType {} is not valid", recipientType);
                throw new PnNationalRegistriesException("recipientType not valid", HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class);
        }
    }

    private AddressOKDto mapToAddressesOKDto(String correlationId) {
        AddressOKDto dto = new AddressOKDto();
        dto.setCorrelationId(correlationId);
        return dto;
    }

    private CodeSqsDto anprToSqsDto(String correlationId, String cf, ResponseE002OKDto anprResponse) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(correlationId);
        if (anprResponse != null
                && anprResponse.getListaSoggetti() != null
                && anprResponse.getListaSoggetti().getDatiSoggetto() != null) {
            PhysicalAddress address = anprResponse.getListaSoggetti().getDatiSoggetto().stream()
                    .filter(subject -> subject.getResidenza() != null
                            && subject.getGeneralita() != null
                            && subject.getGeneralita().getCodiceFiscale() != null
                            && subject.getGeneralita().getCodiceFiscale().getCodFiscale() != null
                            && subject.getGeneralita().getCodiceFiscale().getCodFiscale().equalsIgnoreCase(cf))
                    .flatMap(subject -> subject.getResidenza().stream())
                    .max(Comparator.comparing(r -> parseStringToDate(r.getDataDecorrenzaResidenza())))
                    .map(addressAnprConverter::convertResidence)
                    .map(this::convertAnprToPhysicalAddress)
                    .orElse(null);
            codeSqsDto.setPhysicalAddress(address);
        }
        codeSqsDto.setTaxId(cf);
        return codeSqsDto;
    }

    private CodeSqsDto inadToSqsDto(String correlationId, String cf, GetDigitalAddressINADOKDto inadDto) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(correlationId);
        Date now = new Date();
        if (inadDto != null && inadDto.getDigitalAddress() != null) {
            List<DigitalAddress> address = inadDto.getDigitalAddress().stream()
                    .filter(d -> d.getUsageInfo() == null
                            || d.getUsageInfo().getDateEndValidity() == null
                            || d.getUsageInfo().getDateEndValidity().after(now))
                    .map(this::convertInadToDigitalAddress)
                    .collect(Collectors.toList());
            codeSqsDto.setDigitalAddress(address);
        }
        codeSqsDto.setTaxId(cf);
        return codeSqsDto;
    }

    private CodeSqsDto regImpToSqsDto(String correlationId, String cf, GetAddressRegistroImpreseOKDto registroImpreseDto) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(correlationId);
        if (registroImpreseDto.getProfessionalAddress() != null) {
            codeSqsDto.setPhysicalAddress(convertRegImpToPhysicalAddress(registroImpreseDto.getProfessionalAddress()));
        }
        codeSqsDto.setTaxId(cf);
        return codeSqsDto;
    }

    private PhysicalAddress convertAnprToPhysicalAddress(ResidentialAddressDto residenceDto) {
        PhysicalAddress physicalAddress = new PhysicalAddress();
        physicalAddress.setAddress(residenceDto.getAddress());
        physicalAddress.setAt(residenceDto.getAt());
        physicalAddress.setZip(residenceDto.getZip());
        physicalAddress.setMunicipality(residenceDto.getMunicipality());
        physicalAddress.setProvince(residenceDto.getProvince());
        physicalAddress.setForeignState(residenceDto.getForeignState());
        physicalAddress.setMunicipalityDetails(residenceDto.getMunicipalityDetails());
        physicalAddress.setMunicipality(residenceDto.getMunicipality());
        physicalAddress.setProvince(residenceDto.getProvince());
        return physicalAddress;
    }

    private PhysicalAddress convertRegImpToPhysicalAddress(GetAddressRegistroImpreseOKProfessionalAddressDto addressDto) {
        PhysicalAddress physicalAddress = new PhysicalAddress();
        physicalAddress.setProvince(addressDto.getProvince());
        physicalAddress.setAddress(addressDto.getAddress());
        physicalAddress.setMunicipality(addressDto.getMunicipality());
        physicalAddress.setZip(addressDto.getZip());
        return physicalAddress;
    }

    private DigitalAddress convertInadToDigitalAddress(DigitalAddressDto digitalAddressDto) {
        return new DigitalAddress(DigitalAddressType.PEC.getValue(), digitalAddressDto.getDigitalAddress(), DigitalAddressRecipientType.PERSONA_FISICA.getValue());
    }

    private GetDigitalAddressIniPECRequestBodyDto convertToGetDigitalAddressIniPecRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetDigitalAddressIniPECRequestBodyDto dto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto filterDto = new GetDigitalAddressIniPECRequestBodyFilterDto();

        filterDto.setCorrelationId(addressRequestBodyDto.getFilter().getCorrelationId());
        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());

        dto.setFilter(filterDto);
        return dto;
    }

    private GetAddressRegistroImpreseRequestBodyDto convertToGetAddressRegistroImpreseRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetAddressRegistroImpreseRequestBodyDto dto = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto filterDto = new GetAddressRegistroImpreseRequestBodyFilterDto();

        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());

        dto.setFilter(filterDto);
        return dto;
    }

    private GetDigitalAddressINADRequestBodyDto convertToGetDigitalAddressInadRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetDigitalAddressINADRequestBodyDto dto = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();

        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());
        filterDto.setPracticalReference(addressRequestBodyDto.getFilter().getCorrelationId());

        dto.setFilter(filterDto);
        return dto;
    }

    private GetAddressANPRRequestBodyDto convertToGetAddressAnprRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetAddressANPRRequestBodyDto dto = new GetAddressANPRRequestBodyDto();
        GetAddressANPRRequestBodyFilterDto filterDto = new GetAddressANPRRequestBodyFilterDto();

        filterDto.setRequestReason(addressRequestBodyDto.getFilter().getCorrelationId());
        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());
        filterDto.setReferenceRequestDate(addressRequestBodyDto.getFilter().getReferenceRequestDate());

        dto.setFilter(filterDto);
        return dto;
    }

    private LocalDate parseStringToDate(String str) {
        if (str == null) {
            log.warn("can not parse a null date");
            return LocalDate.EPOCH;
        }
        try {
            return LocalDate.parse(str, formatter);
        } catch (DateTimeParseException e) {
            log.warn("can not parse date {}", str, e);
            return LocalDate.EPOCH;
        }
    }
}
