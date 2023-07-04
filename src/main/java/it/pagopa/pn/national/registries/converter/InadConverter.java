package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.DigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.UsageInfoDto;
import it.pagopa.pn.national.registries.model.inad.ElementDigitalAddressDto;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.model.inad.MotivationTerminationDto;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INAD;


@Slf4j
@Component
public class InadConverter {

    private static final String INAD_CF_NOT_FOUND = "\"detail\":\"CF non trovato\"";


    private InadConverter() {
    }

    public static GetDigitalAddressINADOKDto mapToResponseOk(ResponseRequestDigitalAddressDto elementDigitalAddressDto, String recipientType, String taxId) {
        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();
        if (elementDigitalAddressDto != null) {
            response.setSince(elementDigitalAddressDto.getSince());
            response.setTaxId(elementDigitalAddressDto.getTaxId());
            if (elementDigitalAddressDto.getDigitalAddress() != null) {
                List<DigitalAddressDto> digitalAddressDtoList = elementDigitalAddressDto.getDigitalAddress().stream()
                        .filter(InadConverter::isValid)
                        .map(InadConverter::convertToGetDigitalAddressINADOKDigitalAddressInnerDto)
                        .toList();
                switch (recipientType) {
                    case "PF" -> mapToPfAddress(digitalAddressDtoList, response);
                    case "PG" -> mapToPgAddress(digitalAddressDtoList, taxId, response);
                    default -> throw new PnNationalRegistriesException("Invalid recipientType",HttpStatus.BAD_REQUEST.value(),
                            HttpStatus.BAD_REQUEST.getReasonPhrase(),null,null , Charset.defaultCharset(), InadResponseKO.class);
                }
            } else {
                log.info("inad digital addresses is null");
            }
        }
        return response;
    }

    private static void mapToPgAddress (List<DigitalAddressDto> digitalAddressDtoList, String taxId,GetDigitalAddressINADOKDto response){
        if (digitalAddressDtoList.size() == 1 && digitalAddressDtoList.get(0).getPracticedProfession().isEmpty()
                && taxId.length() == 11) {
            response.setDigitalAddress(digitalAddressDtoList.get(0));
        } else {
            digitalAddressDtoList.stream()
                    .filter(item -> item.getPracticedProfession() != null)
                    .findFirst()
                    .ifPresentOrElse(
                            response::setDigitalAddress,
                            () -> {
                                throw new PnNationalRegistriesException(ERROR_MESSAGE_INAD, HttpStatus.NOT_FOUND.value(),
                                        HttpStatus.NOT_FOUND.getReasonPhrase(),null,INAD_CF_NOT_FOUND.getBytes(StandardCharsets.UTF_8), Charset.defaultCharset(), InadResponseKO.class);
                            }
                    );
        }
    }
    private static void mapToPfAddress(List<DigitalAddressDto> digitalAddressDtoList, GetDigitalAddressINADOKDto response)
    {
        digitalAddressDtoList.stream()
                .filter(item -> item.getPracticedProfession() == null)
                .findFirst()
                .ifPresentOrElse(
                        response::setDigitalAddress,
                        () -> {
                            throw new PnNationalRegistriesException(ERROR_MESSAGE_INAD, HttpStatus.NOT_FOUND.value(),
                                    HttpStatus.NOT_FOUND.getReasonPhrase(),null,INAD_CF_NOT_FOUND.getBytes(StandardCharsets.UTF_8) , Charset.defaultCharset(), InadResponseKO.class);
                        }
                );
    }
    private static DigitalAddressDto convertToGetDigitalAddressINADOKDigitalAddressInnerDto(ElementDigitalAddressDto item) {
        DigitalAddressDto digitalAddress = new DigitalAddressDto();

        digitalAddress.setDigitalAddress(item.getDigitalAddress());
        digitalAddress.setPracticedProfession(item.getPracticedProfession());
        digitalAddress.setUsageInfo(convertUsageInfo(item));

        return digitalAddress;
    }

    private static UsageInfoDto convertUsageInfo(ElementDigitalAddressDto item) {
        UsageInfoDto usageInfoDto = new UsageInfoDto();
        if (item != null && item.getUsageInfo() != null) {
            usageInfoDto.setMotivation(convertMotivation(item.getUsageInfo().getMotivation()));
            usageInfoDto.setDateEndValidity(item.getUsageInfo().getDateEndValidity());
        }
        return usageInfoDto;
    }

    private static UsageInfoDto.MotivationEnum convertMotivation(MotivationTerminationDto motivation) {
        if (motivation == null) {
            return null;
        }
        return switch (motivation) {
            case UFFICIO -> UsageInfoDto.MotivationEnum.UFFICIO;
            case VOLONTARIA -> UsageInfoDto.MotivationEnum.VOLONTARIA;
        };
    }

    private static boolean isValid(ElementDigitalAddressDto dto) {
        Date now = new Date();
        if(dto.getUsageInfo() == null
                || dto.getUsageInfo().getDateEndValidity() == null
                || dto.getUsageInfo().getDateEndValidity().equals(now)
                || dto.getUsageInfo().getDateEndValidity().after(now)){
            return true;
        }
        else{
            log.info("inad digital address: {} is not valid", dto.getDigitalAddress());
            return false;
        }
    }
}
