package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.model.inad.ElementDigitalAddressDto;
import it.pagopa.pn.national.registries.model.inad.MotivationTerminationDto;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.pagopa.pn.national.registries.model.inad.UsageInfo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InadConverterTest {

    /**
     * Method under test: {@link InadConverter#mapToResponseOk(ResponseRequestDigitalAddressDto)}
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
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = InadConverter.mapToResponseOk(responseRequestDigitalAddressDto);
        assertEquals("Codice Fiscale", actualMapToResponseOkResult.getTaxId());
    }

    /**
     * Method under test: {@link InadConverter#mapToResponseOk(ResponseRequestDigitalAddressDto)}
     */
    @Test
    void testMapToResponseOk2() {
        assertNull(InadConverter.mapToResponseOk(null).getDigitalAddress());
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
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = InadConverter.mapToResponseOk(responseRequestDigitalAddressDto);
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
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = InadConverter.mapToResponseOk(responseRequestDigitalAddressDto);
        assertEquals("Codice Fiscale", actualMapToResponseOkResult.getTaxId());
    }

    @Test
    void testMapToResponseOk5() {
        Date now = new Date();

        UsageInfo usageInfo1 = new UsageInfo();
        usageInfo1.setDateEndValidity(Date.from(LocalDate.EPOCH.atStartOfDay(ZoneOffset.UTC).toInstant()));
        usageInfo1.setMotivation(MotivationTerminationDto.UFFICIO);
        ElementDigitalAddressDto elementDigitalAddressDto1 = new ElementDigitalAddressDto();
        elementDigitalAddressDto1.setDigitalAddress("da1");
        elementDigitalAddressDto1.setUsageInfo(usageInfo1);

        UsageInfo usageInfo2 = new UsageInfo();
        usageInfo2.setMotivation(MotivationTerminationDto.UFFICIO);
        ElementDigitalAddressDto elementDigitalAddressDto2 = new ElementDigitalAddressDto();
        elementDigitalAddressDto2.setDigitalAddress("da2");
        elementDigitalAddressDto2.setUsageInfo(usageInfo2);

        UsageInfo usageInfo3 = new UsageInfo();
        usageInfo3.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        usageInfo3.setMotivation(MotivationTerminationDto.UFFICIO);
        ElementDigitalAddressDto elementDigitalAddressDto3 = new ElementDigitalAddressDto();
        elementDigitalAddressDto3.setDigitalAddress("da3");
        elementDigitalAddressDto3.setUsageInfo(usageInfo3);

        ResponseRequestDigitalAddressDto responseRequestDigitalAddressDto = new ResponseRequestDigitalAddressDto();
        responseRequestDigitalAddressDto.setTaxId("taxId");
        responseRequestDigitalAddressDto.setSince(now);
        List<ElementDigitalAddressDto> lista = List.of(elementDigitalAddressDto1, elementDigitalAddressDto2, elementDigitalAddressDto3);
        responseRequestDigitalAddressDto.setDigitalAddress(lista);

        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(responseRequestDigitalAddressDto);
        assertNotNull(result);
        assertNotNull(result.getDigitalAddress());
        assertEquals(2, result.getDigitalAddress().size());
        assertFalse(result.getDigitalAddress().stream().anyMatch(d -> d.getDigitalAddress().equals("da1")));
    }

    /**
     * Method under test: {@link InadConverter#mapToResponseOk(ResponseRequestDigitalAddressDto)}
     */
    @Test
    void testMapToResponseOk6() {
        ResponseRequestDigitalAddressDto elementDigitalAddressDto = new ResponseRequestDigitalAddressDto();
        elementDigitalAddressDto.setDigitalAddress(new ArrayList<>());
        Date since = Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        elementDigitalAddressDto.setSince(since);
        elementDigitalAddressDto.setTaxId("42");
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = InadConverter.mapToResponseOk(elementDigitalAddressDto);
        assertTrue(actualMapToResponseOkResult.getDigitalAddress().isEmpty());
        assertEquals("42", actualMapToResponseOkResult.getTaxId());
        assertSame(since, actualMapToResponseOkResult.getSince());
    }

    /**
     * Method under test: {@link InadConverter#mapToResponseOk(ResponseRequestDigitalAddressDto)}
     */
    @Test
    void testMapToResponseOk7() {
        ResponseRequestDigitalAddressDto elementDigitalAddressDto = mock(ResponseRequestDigitalAddressDto.class);
        when(elementDigitalAddressDto.getTaxId()).thenReturn("42");
        Date fromResult = Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        when(elementDigitalAddressDto.getSince()).thenReturn(fromResult);
        when(elementDigitalAddressDto.getDigitalAddress()).thenReturn(new ArrayList<>());
        doNothing().when(elementDigitalAddressDto).setDigitalAddress(Mockito.<List<ElementDigitalAddressDto>>any());
        doNothing().when(elementDigitalAddressDto).setSince(Mockito.<Date>any());
        doNothing().when(elementDigitalAddressDto).setTaxId(Mockito.<String>any());
        elementDigitalAddressDto.setDigitalAddress(new ArrayList<>());
        elementDigitalAddressDto
                .setSince(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        elementDigitalAddressDto.setTaxId("42");
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = InadConverter.mapToResponseOk(elementDigitalAddressDto);
        assertTrue(actualMapToResponseOkResult.getDigitalAddress().isEmpty());
        assertEquals("42", actualMapToResponseOkResult.getTaxId());
        assertSame(fromResult, actualMapToResponseOkResult.getSince());
        verify(elementDigitalAddressDto).getTaxId();
        verify(elementDigitalAddressDto).getSince();
        verify(elementDigitalAddressDto, atLeast(1)).getDigitalAddress();
        verify(elementDigitalAddressDto).setDigitalAddress(Mockito.<List<ElementDigitalAddressDto>>any());
        verify(elementDigitalAddressDto).setSince(Mockito.<Date>any());
        verify(elementDigitalAddressDto).setTaxId(Mockito.<String>any());
    }

    /**
     * Method under test: {@link InadConverter#mapToResponseOk(ResponseRequestDigitalAddressDto)}
     */
    @Test
    void testMapToResponseOk8() {
        ResponseRequestDigitalAddressDto responseRequestDigitalAddressDto = new ResponseRequestDigitalAddressDto();
        responseRequestDigitalAddressDto.setTaxId("Codice Fiscale");
        List<ElementDigitalAddressDto> list = new ArrayList<>();
        ElementDigitalAddressDto dto = new ElementDigitalAddressDto();
        dto.setDigitalAddress("digitalAddress");
        dto.setPracticedProfession("practicedProfession");

        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(null);
        usageInfo.setDateEndValidity(new Date());
        dto.setUsageInfo(usageInfo);

        list.add(dto);

        responseRequestDigitalAddressDto.setDigitalAddress(list);
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        responseRequestDigitalAddressDto.setSince(fromResult);
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = InadConverter.mapToResponseOk(responseRequestDigitalAddressDto);
        assertEquals("Codice Fiscale", actualMapToResponseOkResult.getTaxId());
    }
}
