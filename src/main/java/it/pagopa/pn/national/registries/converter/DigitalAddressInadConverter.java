package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.DigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.UsageInfoDto;
import it.pagopa.pn.national.registries.model.inad.ElementDigitalAddressDto;
import it.pagopa.pn.national.registries.model.inad.MotivationTerminationDto;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_INAD;
import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_MESSAGE_INAD;

@Slf4j
@Component
public class DigitalAddressInadConverter {

    private DigitalAddressInadConverter() {
    }

    public static GetDigitalAddressINADOKDto mapToResponseOk(ResponseRequestDigitalAddressDto elementDigitalAddressDto) {
        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();

        if (elementDigitalAddressDto != null) {

            response.setSince(elementDigitalAddressDto.getSince());
            response.setTaxId(elementDigitalAddressDto.getTaxId());

            if (elementDigitalAddressDto.getDigitalAddress() != null) {
                Date now = new Date();
                response.setDigitalAddress(elementDigitalAddressDto.getDigitalAddress().stream()
                        .filter(a -> isValid(a, now))
                        .map(DigitalAddressInadConverter::convertToGetDigitalAddressINADOKDigitalAddressInnerDto)
                        .toList());
                log.info("inad digital addresses: {} - valid at {}: {}", response.getDigitalAddress().size(), now, elementDigitalAddressDto.getDigitalAddress().size());
            } else {
                log.info("inad digital addresses is null");
            }
        }
        return response;
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
        return switch (motivation) {
            case UFFICIO -> UsageInfoDto.MotivationEnum.UFFICIO;
            case VOLONTARIA -> UsageInfoDto.MotivationEnum.VOLONTARIA;
            default -> throw new PnInternalException(ERROR_MESSAGE_INAD + " Invalid motivation for", ERROR_CODE_INAD);
        };
    }

    private static boolean isValid(ElementDigitalAddressDto dto, Date date) {
        return dto.getUsageInfo() == null
                || dto.getUsageInfo().getDateEndValidity() == null
                || dto.getUsageInfo().getDateEndValidity().equals(date)
                || dto.getUsageInfo().getDateEndValidity().after(date);
    }
}
