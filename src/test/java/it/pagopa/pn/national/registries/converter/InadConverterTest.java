package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.MotivationTermination;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.UsageInfo;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.UsageInfoDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.constant.RecipientType.PG;
import static org.junit.jupiter.api.Assertions.*;

class InadConverterTest {

    private static final String VALID_CF = "MNZVMH95B09L084U";
    private static final String VALID_PIVA = "00123456789";

    @Test
    void retrieveRecipientTypeShouldReturnPfWhenCfHas16Characters() {
        BatchRequest request = new BatchRequest();
        request.setCf(VALID_CF);
        RecipientType result = InadConverter.retrieveRecipientType(request);
        assertEquals(PF, result);
    }

    @Test
    void retrieveRecipientTypeShouldReturnPgWhenCfIsNotPresent() {
        BatchRequest request = new BatchRequest();
        request.setCf(null);
        RecipientType result = InadConverter.retrieveRecipientType(request);
        assertEquals(PG, result);
    }

    @Test
    void retrieveRecipientTypeShouldReturnPgWhenCfHasWrongLength() {
        BatchRequest request = new BatchRequest();
        request.setCf("12345678901");
        RecipientType result = InadConverter.retrieveRecipientType(request);
        assertEquals(PG, result);
    }

    @Test
    void mapToResponseOkShouldReturnEmptyResponseWhenInputIsNull() {
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(null, PF, VALID_CF, false);
        assertNotNull(result);
        assertNull(result.getDigitalAddress());
        assertNull(result.getTaxId());
        assertNull(result.getSince());
    }

    @Test
    void mapToResponseOkShouldMapSinceAndTaxId() {
        Date since = new Date();
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, since, createPersonalAddress("personal@pec.it"));
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false);
        assertNotNull(result);
        assertEquals(VALID_CF, result.getTaxId());
        assertEquals(since, result.getSince());
    }

    @Test
    void mapToResponseOkShouldReturnPersonalAddressForPf() {
        ElementDigitalAddress personalAddress = createPersonalAddress("personal@pec.it");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), personalAddress);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false);
        assertNotNull(result.getDigitalAddress());
        assertEquals("personal@pec.it", result.getDigitalAddress().getDigitalAddress());
        assertNull(result.getDigitalAddress().getPracticedProfession());
    }

    @Test
    void mapToResponseOkShouldPreferPersonalAddressForPfWhenFallbackIsEnabled() {
        ElementDigitalAddress personalAddress = createPersonalAddress("personal@pec.it");
        ElementDigitalAddress professionalAddress = createProfessionalAddress("professional@pec.it", "INGEGNERE");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), professionalAddress, personalAddress);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, true);
        assertNotNull(result.getDigitalAddress());
        assertEquals("personal@pec.it", result.getDigitalAddress().getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldUseProfessionalAddressForPfWhenFallbackEnabledAndPersonalMissing() {
        ElementDigitalAddress professionalAddress = createProfessionalAddress("professional@pec.it", "INGEGNERE");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), professionalAddress);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, true);
        assertNotNull(result.getDigitalAddress());
        assertEquals("professional@pec.it", result.getDigitalAddress().getDigitalAddress());
        assertEquals("INGEGNERE", result.getDigitalAddress().getPracticedProfession());
    }

    @Test
    void mapToResponseOkShouldThrowForPfWhenOnlyProfessionalAddressExistsAndFallbackDisabled() {
        ElementDigitalAddress professionalAddress = createProfessionalAddress("professional@pec.it", "INGEGNERE");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), professionalAddress);
        assertThrows(PnNationalRegistriesException.class, () -> InadConverter.mapToResponseOk(input, PF, VALID_CF, false));
    }

    @Test
    void mapToResponseOkShouldThrowForPfWhenNoAddressExists() {
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date());
        assertThrows(PnNationalRegistriesException.class, () -> InadConverter.mapToResponseOk(input, PF, VALID_CF, false));
    }

    @Test
    void mapToResponseOkShouldReturnSingleNonProfessionalAddressForPgWithPiva() {
        ElementDigitalAddress address = createPersonalAddress("company@pec.it");
        ResponseRequestDigitalAddress input = createResponse(VALID_PIVA, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PG, VALID_PIVA, false);
        assertNotNull(result.getDigitalAddress());
        assertEquals("company@pec.it", result.getDigitalAddress().getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldReturnProfessionalAddressForPg() {
        ElementDigitalAddress personalAddress = createPersonalAddress("personal@pec.it");
        ElementDigitalAddress professionalAddress = createProfessionalAddress("professional@pec.it", "AVVOCATO");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), personalAddress, professionalAddress);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PG, VALID_CF, false);
        assertNotNull(result.getDigitalAddress());
        assertEquals("professional@pec.it", result.getDigitalAddress().getDigitalAddress());
        assertEquals("AVVOCATO", result.getDigitalAddress().getPracticedProfession());
    }

    @Test
    void mapToResponseOkShouldThrowForPgWhenProfessionalAddressIsMissing() {
        ElementDigitalAddress firstAddress = createPersonalAddress("first@pec.it");
        ElementDigitalAddress secondAddress = createPersonalAddress("second@pec.it");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), firstAddress, secondAddress);
        assertThrows(PnNationalRegistriesException.class, () -> InadConverter.mapToResponseOk(input, PG, VALID_CF, false));
    }

    @Test
    void mapToResponseOkShouldIgnoreExpiredAddress() {
        ElementDigitalAddress expiredAddress = createAddress("expired@pec.it", null, daysFromNow(-1), MotivationTermination.CESSAZIONE_UFFICIO);
        ElementDigitalAddress validAddress = createPersonalAddress("valid@pec.it");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), expiredAddress, validAddress);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false);
        assertNotNull(result.getDigitalAddress());
        assertEquals("valid@pec.it", result.getDigitalAddress().getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldAcceptAddressWithoutDateEndValidity() {
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setDateEndValidity(null);
        ElementDigitalAddress address = new ElementDigitalAddress();
        address.setDigitalAddress("valid@pec.it");
        address.setUsageInfo(usageInfo);
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false);
        assertNotNull(result.getDigitalAddress());
        assertEquals("valid@pec.it", result.getDigitalAddress().getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldAcceptAddressWithoutUsageInfo() {
        ElementDigitalAddress address = new ElementDigitalAddress();
        address.setDigitalAddress("valid@pec.it");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false);
        assertNotNull(result.getDigitalAddress());
        assertEquals("valid@pec.it", result.getDigitalAddress().getDigitalAddress());
        assertNotNull(result.getDigitalAddress().getUsageInfo());
        assertNull(result.getDigitalAddress().getUsageInfo().getMotivation());
        assertNull(result.getDigitalAddress().getUsageInfo().getDateEndValidity());
    }

    @Test
    void mapToResponseOkShouldMapCessazioneUfficioMotivation() {
        ElementDigitalAddress address = createAddress("valid@pec.it", null, daysFromNow(1), MotivationTermination.CESSAZIONE_UFFICIO);
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false);
        assertNotNull(result.getDigitalAddress());
        assertNotNull(result.getDigitalAddress().getUsageInfo());
        assertEquals(UsageInfoDto.MotivationEnum.CESSAZIONE_UFFICIO, result.getDigitalAddress().getUsageInfo().getMotivation());
    }

    @Test
    void mapToResponseOkShouldMapCessazioneVolontariaMotivation() {
        ElementDigitalAddress address = createAddress("valid@pec.it", null, daysFromNow(1), MotivationTermination.CESSAZIONE_VOLONTARIA);
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false);
        assertNotNull(result.getDigitalAddress());
        assertNotNull(result.getDigitalAddress().getUsageInfo());
        assertEquals(UsageInfoDto.MotivationEnum.CESSAZIONE_VOLONTARIA, result.getDigitalAddress().getUsageInfo().getMotivation());
    }

    @Test
    void mapToResponseOkShouldMapNullMotivation() {
        ElementDigitalAddress address = createAddress("valid@pec.it", null, daysFromNow(1), null);
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false);
        assertNotNull(result.getDigitalAddress());
        assertNotNull(result.getDigitalAddress().getUsageInfo());
        assertNull(result.getDigitalAddress().getUsageInfo().getMotivation());
    }

    @Test
    void mapToResponseOkShouldNotThrowWhenDigitalAddressListIsNull() {
        ResponseRequestDigitalAddress input = new ResponseRequestDigitalAddress();
        input.setCodiceFiscale(VALID_CF);
        input.setSince(new Date());
        input.setDigitalAddress(null);
        GetDigitalAddressINADOKDto result = assertDoesNotThrow(() -> InadConverter.mapToResponseOk(input, PF, VALID_CF, false));
        assertEquals(VALID_CF, result.getTaxId());
        assertNull(result.getDigitalAddress());
    }

    private ResponseRequestDigitalAddress createResponse(String taxId, Date since, ElementDigitalAddress... addresses) {
        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale(taxId);
        response.setSince(since);
        response.setDigitalAddress(List.of(addresses));
        return response;
    }

    private ElementDigitalAddress createPersonalAddress(String address) {
        return createAddress(address, null, daysFromNow(1), null);
    }

    private ElementDigitalAddress createProfessionalAddress(String address, String profession) {
        return createAddress(address, profession, daysFromNow(1), null);
    }

    private ElementDigitalAddress createAddress(String address, String profession, Date dateEndValidity, MotivationTermination motivation) {
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setDateEndValidity(dateEndValidity);
        usageInfo.setMotivation(motivation);
        ElementDigitalAddress element = new ElementDigitalAddress();
        element.setDigitalAddress(address);
        element.setPracticedProfession(profession);
        element.setUsageInfo(usageInfo);
        return element;
    }

    private Date daysFromNow(long days) {
        return Date.from(LocalDate.now().plusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}