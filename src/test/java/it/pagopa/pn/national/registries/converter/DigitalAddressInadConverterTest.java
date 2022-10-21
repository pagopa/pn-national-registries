package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.UsageInfoDto;
import it.pagopa.pn.national.registries.model.inad.ElementDigitalAddressDto;
import it.pagopa.pn.national.registries.model.inad.MotivationTerminationDto;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.pagopa.pn.national.registries.model.inad.UsageInfo;
import org.apache.velocity.util.ArrayListWrapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DigitalAddressInadConverterTest {
    /**
     * Method under test: {@link DigitalAddressInadConverter#mapToResponseOk(ResponseRequestDigitalAddressDto)}
     */
    @Test
    void testMapToResponseOk() {
        ResponseRequestDigitalAddressDto responseRequestDigitalAddressDto = new ResponseRequestDigitalAddressDto();
        responseRequestDigitalAddressDto.setTaxId("Codice Fiscale");
        List<ElementDigitalAddressDto> list = new ArrayList<>();
        ElementDigitalAddressDto dto = new ElementDigitalAddressDto();
        dto.setDigitalAddress("digitalAddress");
        dto.setPracticedProfession("practicedProfession");
        list.add(dto);
        responseRequestDigitalAddressDto.setDigitalAddress(list);
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        responseRequestDigitalAddressDto.setSince(fromResult);
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = DigitalAddressInadConverter
                .mapToResponseOk(responseRequestDigitalAddressDto);
        assertEquals("Codice Fiscale", actualMapToResponseOkResult.getTaxId());
    }

    /**
     * Method under test: {@link DigitalAddressInadConverter#mapToResponseOk(ResponseRequestDigitalAddressDto)}
     */
    @Test
    void testMapToResponseOk2() {
        assertNull(DigitalAddressInadConverter.mapToResponseOk(null).getDigitalAddress());
    }

    @Test
    void testMapToResponseOk3() {
        ResponseRequestDigitalAddressDto responseRequestDigitalAddressDto = new ResponseRequestDigitalAddressDto();
        responseRequestDigitalAddressDto.setTaxId("Codice Fiscale");
        List<ElementDigitalAddressDto> list = new ArrayList<>();
        ElementDigitalAddressDto dto = new ElementDigitalAddressDto();
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(MotivationTerminationDto.UFFICIO);
        usageInfo.setDateEndValidity(new Date());
        dto.setUsageInfo(usageInfo);
        dto.setDigitalAddress("digitalAddress");
        dto.setPracticedProfession("practicedProfession");
        list.add(dto);
        responseRequestDigitalAddressDto.setDigitalAddress(list);
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        responseRequestDigitalAddressDto.setSince(fromResult);
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = DigitalAddressInadConverter
                .mapToResponseOk(responseRequestDigitalAddressDto);
        assertEquals("Codice Fiscale", actualMapToResponseOkResult.getTaxId());
    }

    @Test
    void testMapToResponseOk4() {
        ResponseRequestDigitalAddressDto responseRequestDigitalAddressDto = new ResponseRequestDigitalAddressDto();
        responseRequestDigitalAddressDto.setTaxId("Codice Fiscale");
        List<ElementDigitalAddressDto> list = new ArrayList<>();
        ElementDigitalAddressDto dto = new ElementDigitalAddressDto();
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(MotivationTerminationDto.VOLONTARIA);
        usageInfo.setDateEndValidity(new Date());
        dto.setUsageInfo(usageInfo);
        dto.setDigitalAddress("digitalAddress");
        dto.setPracticedProfession("practicedProfession");
        list.add(dto);
        responseRequestDigitalAddressDto.setDigitalAddress(list);
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        responseRequestDigitalAddressDto.setSince(fromResult);
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = DigitalAddressInadConverter
                .mapToResponseOk(responseRequestDigitalAddressDto);
        assertEquals("Codice Fiscale", actualMapToResponseOkResult.getTaxId());
    }
}

