package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.MotivationTermination;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.UsageInfo;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.DigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.UsageInfoDto;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Slf4j
@Component
public class InadConverter {

    private static final String INAD_CF_NOT_FOUND = "CF non trovato";
    private static final int PIVA_LENGTH = 11;
    private static final int CF_LENGTH = 16;


    private InadConverter() {
    }

    public static RecipientType retrieveRecipientType(BatchRequest request) {
        return  StringUtils.hasText(request.getCf()) && request.getCf().length() == CF_LENGTH ? RecipientType.PF : RecipientType.PG;
    }

    public static GetDigitalAddressINADOKDto mapToResponseOk(ResponseRequestDigitalAddress elementDigitalAddress, RecipientType recipientType, String taxId, boolean newWorkflowEnabled) {
        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();
        if (elementDigitalAddress != null) {
            response.setSince(elementDigitalAddress.getSince());
            response.setTaxId(elementDigitalAddress.getCodiceFiscale());
            if (elementDigitalAddress.getDigitalAddress() != null) {
                List<DigitalAddressDto> digitalAddressDtoList = elementDigitalAddress.getDigitalAddress().stream()
                        .filter(InadConverter::isValid)
                        .map(InadConverter::convertToGetDigitalAddressINADOKDigitalAddressInnerDto)
                        .toList();
                switch (recipientType) {
                    case PF -> mapToPfAddress(digitalAddressDtoList, response, newWorkflowEnabled);
                    case PG -> mapToPgAddress(digitalAddressDtoList, taxId, response);
                    default -> throw new PnNationalRegistriesException("Invalid recipientType",HttpStatus.BAD_REQUEST.value(),
                            HttpStatus.BAD_REQUEST.getReasonPhrase(),null,null , Charset.defaultCharset(), InadResponseKO.class);
                }
            } else {
                log.info("inad digital addresses is null");
            }
        }
        return response;
    }

    private static void mapToPgAddress (List<DigitalAddressDto> digitalAddressDtoList, String taxId, GetDigitalAddressINADOKDto response){
        if (taxId.length() == PIVA_LENGTH &&
                digitalAddressDtoList.size() == 1 && !StringUtils.hasText(digitalAddressDtoList.get(0).getPracticedProfession())) {
            response.setDigitalAddress(digitalAddressDtoList.get(0));
        }else{
            retrieveProfessionalAddress(digitalAddressDtoList)
                    .ifPresentOrElse(
                            response::setDigitalAddress,
                            () -> {
                                throw new PnNationalRegistriesException(INAD_CF_NOT_FOUND, HttpStatus.NOT_FOUND.value(),
                                        HttpStatus.NOT_FOUND.getReasonPhrase(),null,null, Charset.defaultCharset(), InadResponseKO.class);
                            }
                    );
        }
    }
    private static void mapToPfAddress(List<DigitalAddressDto> digitalAddressDtoList, GetDigitalAddressINADOKDto response, boolean newWorkflowEnabled) {
        if (newWorkflowEnabled) {
            retrieveProfessionalAddress(digitalAddressDtoList)
                    .ifPresentOrElse(
                            response::setDigitalAddress,
                            () -> retrievePersonalAddress(digitalAddressDtoList, response));

        } else {
            retrievePersonalAddress(digitalAddressDtoList, response);
        }
    }

    private static void retrievePersonalAddress(List<DigitalAddressDto> digitalAddressDtoList, GetDigitalAddressINADOKDto response) {
        digitalAddressDtoList.stream()
                .filter(item -> !StringUtils.hasText(item.getPracticedProfession()) && response.getTaxId().length() == CF_LENGTH)
                .findFirst()
                .ifPresentOrElse(
                        response::setDigitalAddress,
                        () -> {
                            throw new PnNationalRegistriesException(INAD_CF_NOT_FOUND, HttpStatus.NOT_FOUND.value(),
                                    HttpStatus.NOT_FOUND.getReasonPhrase(), null, null, Charset.defaultCharset(), InadResponseKO.class);
                        }
                );
    }

    private static Optional<DigitalAddressDto> retrieveProfessionalAddress(List<DigitalAddressDto> digitalAddressDtoList) {
        return digitalAddressDtoList.stream()
                .filter(item -> StringUtils.hasText(item.getPracticedProfession()))
                .findFirst();
    }


    private static DigitalAddressDto convertToGetDigitalAddressINADOKDigitalAddressInnerDto(ElementDigitalAddress item) {
        DigitalAddressDto digitalAddress = new DigitalAddressDto();

        digitalAddress.setDigitalAddress(item.getDigitalAddress());
        digitalAddress.setPracticedProfession(item.getPracticedProfession());
        digitalAddress.setUsageInfo(convertUsageInfo(item));

        return digitalAddress;
    }

    private static UsageInfoDto convertUsageInfo(ElementDigitalAddress item) {
        UsageInfoDto usageInfoDto = new UsageInfoDto();
        if (item != null && item.getUsageInfo() != null) {
            usageInfoDto.setMotivation(convertMotivation(item.getUsageInfo().getMotivation()));
            usageInfoDto.setDateEndValidity(item.getUsageInfo().getDateEndValidity());
        }
        return usageInfoDto;
    }

    private static UsageInfoDto.MotivationEnum convertMotivation(MotivationTermination motivation) {
        if (motivation == null) {
            return null;
        }
        return switch (motivation) {
            case CESSAZIONE_UFFICIO -> UsageInfoDto.MotivationEnum.CESSAZIONE_UFFICIO;
            case CESSAZIONE_VOLONTARIA -> UsageInfoDto.MotivationEnum.CESSAZIONE_VOLONTARIA;
        };
    }

    private static boolean isValid(ElementDigitalAddress dto) {
        Date parsedEndValidity = Optional.ofNullable(dto.getUsageInfo())
                .map(UsageInfo::getDateEndValidity)
                .orElse(null);

        Date now = new Date();

        if (Objects.isNull(parsedEndValidity) || parsedEndValidity.equals(now) || parsedEndValidity.after(now)) {
            return true;
        } else {
            log.info("inad digital address is not valid");
            return false;
        }
    }
}
