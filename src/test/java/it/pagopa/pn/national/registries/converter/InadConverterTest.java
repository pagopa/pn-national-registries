package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.MotivationTermination;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.UsageInfo;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InadConverterTest {

    /**
     * Method under test: {@link InadConverter#mapToResponseOk(ResponseRequestDigitalAddress, String, String)}
     */
    @Test
    void testMapToResponseOk() {
        ResponseRequestDigitalAddress responseRequestDigitalAddressDto = new ResponseRequestDigitalAddress();
        responseRequestDigitalAddressDto.setCodiceFiscale("MNZVMH95B09L084U");
        List<ElementDigitalAddress> list = new ArrayList<>();
        ElementDigitalAddress dto = new ElementDigitalAddress();
        dto.setDigitalAddress("digitalAddress");
        list.add(dto);
        responseRequestDigitalAddressDto.setDigitalAddress(list);
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        responseRequestDigitalAddressDto.setSince(fromResult);
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = InadConverter.mapToResponseOk(responseRequestDigitalAddressDto, "PF", "taxId");
        assertEquals("MNZVMH95B09L084U", actualMapToResponseOkResult.getTaxId());
    }

    @Test
    void testMapToResponseOk2() {
        assertNull(InadConverter.mapToResponseOk(null, "PF", "TaxId").getDigitalAddress());
    }

    @Test
    void testMapToResponseOk3() {
        ResponseRequestDigitalAddress responseRequestDigitalAddress = new ResponseRequestDigitalAddress();
        responseRequestDigitalAddress.setCodiceFiscale("MNZVMH95B09L084U");
        List<ElementDigitalAddress> list = new ArrayList<>();
        ElementDigitalAddress dto = new ElementDigitalAddress();
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(MotivationTermination.VOLONTARIA);
        usageInfo.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        dto.setUsageInfo(usageInfo);
        dto.setDigitalAddress("digitalAddress");
        list.add(dto);
        responseRequestDigitalAddress.setDigitalAddress(list);
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        responseRequestDigitalAddress.setSince(fromResult);
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = InadConverter.mapToResponseOk(responseRequestDigitalAddress, "PF", "TaxId");
        assertEquals("MNZVMH95B09L084U", actualMapToResponseOkResult.getTaxId());
    }

    @Test
    void testMapToResponseOk5() {
        Date now = new Date();

        UsageInfo usageInfo1 = new UsageInfo();
        usageInfo1.setDateEndValidity(Date.from(LocalDate.EPOCH.atStartOfDay(ZoneOffset.UTC).toInstant()));
        usageInfo1.setMotivation(MotivationTermination.UFFICIO);
        ElementDigitalAddress elementDigitalAddress1 = new ElementDigitalAddress();
        elementDigitalAddress1.setDigitalAddress("da1");
        elementDigitalAddress1.setUsageInfo(usageInfo1);

        UsageInfo usageInfo2 = new UsageInfo();
        usageInfo2.setMotivation(MotivationTermination.UFFICIO);
        usageInfo2.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        ElementDigitalAddress elementDigitalAddress2 = new ElementDigitalAddress();
        elementDigitalAddress2.setDigitalAddress("da2");
        elementDigitalAddress2.setUsageInfo(usageInfo2);

        UsageInfo usageInfo3 = new UsageInfo();
        usageInfo3.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        usageInfo3.setMotivation(MotivationTermination.UFFICIO);
        ElementDigitalAddress elementDigitalAddress3 = new ElementDigitalAddress();
        elementDigitalAddress3.setDigitalAddress("da3");
        elementDigitalAddress3.setPracticedProfession("practicedProfession");
        elementDigitalAddress3.setUsageInfo(usageInfo3);

        ResponseRequestDigitalAddress responseRequestDigitalAddress = new ResponseRequestDigitalAddress();
        responseRequestDigitalAddress.setCodiceFiscale("taxId");
        responseRequestDigitalAddress.setSince(now);
        List<ElementDigitalAddress> lista = List.of(elementDigitalAddress1, elementDigitalAddress2, elementDigitalAddress3);
        responseRequestDigitalAddress.setDigitalAddress(lista);

        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(responseRequestDigitalAddress, "PG", "TaxIs");
        assertNotNull(result);
        assertNotNull(result.getDigitalAddress());
        assertFalse(result.getDigitalAddress().getDigitalAddress().equalsIgnoreCase("da1"));
    }

    /**
     * Method under test: {@link InadConverter#mapToResponseOk(ResponseRequestDigitalAddress, String, String)}
     */
    @Test
    void testMapToResponseOk6() {
        ResponseRequestDigitalAddress elementDigitalAddress = new ResponseRequestDigitalAddress();
        Date since = Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        elementDigitalAddress.setSince(since);
        elementDigitalAddress.setCodiceFiscale("MNZVMH95B09L084U");
        ElementDigitalAddress address = new ElementDigitalAddress();
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(MotivationTermination.UFFICIO);
        usageInfo.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        address.setUsageInfo(usageInfo);
        address.setDigitalAddress("digital Address");
        elementDigitalAddress.setDigitalAddress(List.of(address));
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = InadConverter.mapToResponseOk(elementDigitalAddress, "PF", "TaxId");
        assertEquals("MNZVMH95B09L084U", actualMapToResponseOkResult.getTaxId());
        assertEquals(since, actualMapToResponseOkResult.getSince());
    }

    /**
     * Method under test: {@link InadConverter#mapToResponseOk(ResponseRequestDigitalAddress, String, String)}
     */
    @Test
    void testMapToResponseKo7() {
        ResponseRequestDigitalAddress elementDigitalAddress = mock(ResponseRequestDigitalAddress.class);
        when(elementDigitalAddress.getCodiceFiscale()).thenReturn("42");
        Date fromResult = Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        when(elementDigitalAddress.getSince()).thenReturn(fromResult);
        when(elementDigitalAddress.getDigitalAddress()).thenReturn(new ArrayList<>());
        doNothing().when(elementDigitalAddress).setDigitalAddress(Mockito.any());
        doNothing().when(elementDigitalAddress).setSince(Mockito.any());
        doNothing().when(elementDigitalAddress).setCodiceFiscale(Mockito.any());
        elementDigitalAddress.setDigitalAddress(new ArrayList<>());
        elementDigitalAddress
                .setSince(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        elementDigitalAddress.setCodiceFiscale("42");

        Assertions.assertThrows(PnNationalRegistriesException.class, () -> InadConverter.mapToResponseOk(elementDigitalAddress, "PF", "TaxId"),
                "Errore durante la chiamata al servizio EstrazioniPuntualiApi");
    }

    /**
     * Method under test: {@link InadConverter#mapToResponseOk(ResponseRequestDigitalAddress, String, String)}
     */
    @Test
    void testMapToResponseOk8() {
        ResponseRequestDigitalAddress responseRequestDigitalAddress = new ResponseRequestDigitalAddress();
        responseRequestDigitalAddress.setCodiceFiscale("Codice Fiscale");
        List<ElementDigitalAddress> list = new ArrayList<>();
        ElementDigitalAddress dto = new ElementDigitalAddress();
        dto.setDigitalAddress("digitalAddress");
        dto.setPracticedProfession("practicedProfession");

        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(null);
        usageInfo.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        dto.setUsageInfo(usageInfo);

        list.add(dto);

        responseRequestDigitalAddress.setDigitalAddress(list);
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        responseRequestDigitalAddress.setSince(fromResult);
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = InadConverter.mapToResponseOk(responseRequestDigitalAddress, "PG", "TaxId");
        assertEquals("Codice Fiscale", actualMapToResponseOkResult.getTaxId());
    }

    @Test
    void testMapToResponseKo9() {
        ResponseRequestDigitalAddress responseRequestDigitalAddress = new ResponseRequestDigitalAddress();
        responseRequestDigitalAddress.setCodiceFiscale("Codice Fiscale");
        List<ElementDigitalAddress> list = new ArrayList<>();
        ElementDigitalAddress dto = new ElementDigitalAddress();
        dto.setDigitalAddress("digitalAddress");
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(null);
        usageInfo.setDateEndValidity(new Date());
        dto.setUsageInfo(usageInfo);
        list.add(dto);
        responseRequestDigitalAddress.setDigitalAddress(list);
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        responseRequestDigitalAddress.setSince(fromResult);
        assertThrows(PnNationalRegistriesException.class, () -> InadConverter.mapToResponseOk(responseRequestDigitalAddress, "PP", "taxId"));
    }

    @Test
    void testMapToResponseOk10() {
        ResponseRequestDigitalAddress responseRequestDigitalAddress = new ResponseRequestDigitalAddress();
        List<ElementDigitalAddress> list = new ArrayList<>();
        ElementDigitalAddress dto = new ElementDigitalAddress();
        dto.setDigitalAddress("digitalAddress");
        dto.practicedProfession("practicedProfession");
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(null);
        usageInfo.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        dto.setUsageInfo(usageInfo);
        list.add(dto);
        responseRequestDigitalAddress.setDigitalAddress(list);
        responseRequestDigitalAddress.setCodiceFiscale("CodiceFiscale");
        responseRequestDigitalAddress.setSince(new Date());
        assertNotNull(InadConverter.mapToResponseOk(responseRequestDigitalAddress, "PG", "TaxId"));
    }

    @Test
    void testMapToResponseOk11() {
        ResponseRequestDigitalAddress responseRequestDigitalAddress = new ResponseRequestDigitalAddress();
        responseRequestDigitalAddress.setCodiceFiscale("Codice Fiscale");
        List<ElementDigitalAddress> list = new ArrayList<>();
        ElementDigitalAddress dto = new ElementDigitalAddress();
        dto.setDigitalAddress("digitalAddress");
        dto.setPracticedProfession("");

        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(null);
        usageInfo.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        dto.setUsageInfo(usageInfo);

        list.add(dto);

        responseRequestDigitalAddress.setDigitalAddress(list);
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        responseRequestDigitalAddress.setSince(fromResult);
        GetDigitalAddressINADOKDto actualMapToResponseOkResult = InadConverter.mapToResponseOk(responseRequestDigitalAddress, "PG", "00123456789");
        assertEquals("Codice Fiscale", actualMapToResponseOkResult.getTaxId());
    }

    @Test
    void testMapToResponseKo12() {
        ResponseRequestDigitalAddress responseRequestDigitalAddress = new ResponseRequestDigitalAddress();
        responseRequestDigitalAddress.setCodiceFiscale("Codice Fiscale");
        List<ElementDigitalAddress> list = new ArrayList<>();
        ElementDigitalAddress dto = new ElementDigitalAddress();
        dto.setDigitalAddress("digitalAddress");
        ElementDigitalAddress dto2 = new ElementDigitalAddress();
        dto2.setDigitalAddress("digitalAddress");

        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setDateEndValidity(new Date());
        usageInfo.setMotivation(null);
        dto.setUsageInfo(usageInfo);
        dto2.setUsageInfo(usageInfo);

        list.add(dto);
        list.add(dto2);

        responseRequestDigitalAddress.setDigitalAddress(list);
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        responseRequestDigitalAddress.setSince(fromResult);
        assertThrows(PnNationalRegistriesException.class, () -> InadConverter.mapToResponseOk(responseRequestDigitalAddress, "PG", "TaxId"));
    }
}
