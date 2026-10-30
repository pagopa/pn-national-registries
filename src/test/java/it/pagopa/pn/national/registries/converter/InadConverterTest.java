package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.MotivationTermination;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.UsageInfo;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.UsageInfoDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.constant.RecipientType.PG;
import static org.junit.jupiter.api.Assertions.*;

class InadConverterTest {

    private static final String VALID_PIVA = "00123456789";
    private static final String VALID_CF = "MNZVMH95B09L084U";

    @Test
    void mapToResponseOkShouldReturnEmptyResponseWhenInputIsNull() {
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(null, PF, VALID_CF, false, "correlationID");
        assertNotNull(result);
        assertNull(result.getDigitalAddress());
        assertNull(result.getTaxId());
        assertNull(result.getSince());
    }

    @Test
    void mapToResponseOkShouldMapSinceAndTaxId() {
        Date since = new Date();
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, since, createPersonalAddress("personal@pec.it"));
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid");
        assertNotNull(result);
        assertEquals(VALID_CF, result.getTaxId());
        assertEquals(since, result.getSince());
    }

    @Test
    void mapToResponseOkShouldReturnPersonalAddressForPf() {
        ElementDigitalAddress personalAddress = createPersonalAddress("personal@pec.it");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), personalAddress);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid");
        assertNotNull(result.getDigitalAddress());
        assertEquals("personal@pec.it", result.getDigitalAddress().getDigitalAddress());
        assertNull(result.getDigitalAddress().getPracticedProfession());
    }

    @Test
    void mapToResponseOkShouldPreferPersonalAddressForPfWhenFallbackIsEnabled() {
        ElementDigitalAddress personalAddress = createPersonalAddress("personal@pec.it");
        ElementDigitalAddress professionalAddress = createProfessionalAddress("professional@pec.it", "INGEGNERE");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), professionalAddress, personalAddress);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, true, "correlationid");
        assertNotNull(result.getDigitalAddress());
        assertEquals("personal@pec.it", result.getDigitalAddress().getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldPreferProfessionalAddressForPfWhenFallbackIsEnabledAndPersonalIsInvalid() {
        ElementDigitalAddress personalAddress = createPersonalAddress("2026-01-01");
        ElementDigitalAddress professionalAddress = createProfessionalAddress("professional@pec.it", "INGEGNERE");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), professionalAddress, personalAddress);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, true, "correlationid");
        assertNotNull(result.getDigitalAddress());
        assertEquals("professional@pec.it", result.getDigitalAddress().getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldUseProfessionalAddressForPfWhenFallbackEnabledAndPersonalMissing() {
        ElementDigitalAddress professionalAddress = createProfessionalAddress("professional@pec.it", "INGEGNERE");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), professionalAddress);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, true, "correlationid");
        assertNotNull(result.getDigitalAddress());
        assertEquals("professional@pec.it", result.getDigitalAddress().getDigitalAddress());
        assertEquals("INGEGNERE", result.getDigitalAddress().getPracticedProfession());
    }

    @Test
    void mapToResponseOkShouldThrowForPfWhenOnlyProfessionalAddressExistsAndFallbackDisabled() {
        ElementDigitalAddress professionalAddress = createProfessionalAddress("professional@pec.it", "INGEGNERE");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), professionalAddress);
        assertThrows(PnNationalRegistriesException.class, () -> InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationId"));
    }

    @Test
    void mapToResponseOkShouldReturnEmptyAddressForPfWhenNoAddressExists() {
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date());
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationId");
        assertNotNull(result);
        assertEquals(VALID_CF, result.getTaxId());
        assertNull(result.getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldReturnSingleNonProfessionalAddressForPgWithPiva() {
        ElementDigitalAddress address = createPersonalAddress("company@pec.it");
        ResponseRequestDigitalAddress input = createResponse(VALID_PIVA, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PG, VALID_PIVA, false, "correlationid");
        assertNotNull(result.getDigitalAddress());
        assertEquals("company@pec.it", result.getDigitalAddress().getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldIgnoreExpiredAddress() {
        ElementDigitalAddress expiredAddress = createAddress("expired@pec.it", null, daysFromNow(-1), MotivationTermination.CESSAZIONE_UFFICIO);
        ElementDigitalAddress validAddress = createPersonalAddress("valid@pec.it");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), expiredAddress, validAddress);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid");
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
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid");
        assertNotNull(result.getDigitalAddress());
        assertEquals("valid@pec.it", result.getDigitalAddress().getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldAcceptAddressWithoutUsageInfo() {
        ElementDigitalAddress address = new ElementDigitalAddress();
        address.setDigitalAddress("valid@pec.it");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid");
        assertNotNull(result.getDigitalAddress());
        assertEquals("valid@pec.it", result.getDigitalAddress().getDigitalAddress());
        assertNotNull(result.getDigitalAddress().getUsageInfo());
        assertNull(result.getDigitalAddress().getUsageInfo().getMotivation());
        assertNull(result.getDigitalAddress().getUsageInfo().getDateEndValidity());
    }

    @Test
    void mapToResponseOkShouldMapDateEndValidity() {
        Date dateEndValidity = daysFromNow(1);
        ElementDigitalAddress address = createAddress("valid@pec.it", null, dateEndValidity, null);
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid");
        assertNotNull(result.getDigitalAddress());
        assertNotNull(result.getDigitalAddress().getUsageInfo());
        assertEquals(dateEndValidity, result.getDigitalAddress().getUsageInfo().getDateEndValidity());
    }

    @Test
    void mapToResponseOkShouldMapCessazioneUfficioMotivation() {
        ElementDigitalAddress address = createAddress("valid@pec.it", null, daysFromNow(1), MotivationTermination.CESSAZIONE_UFFICIO);
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid");
        assertNotNull(result.getDigitalAddress());
        assertNotNull(result.getDigitalAddress().getUsageInfo());
        assertEquals(UsageInfoDto.MotivationEnum.CESSAZIONE_UFFICIO, result.getDigitalAddress().getUsageInfo().getMotivation());
    }

    @Test
    void mapToResponseOkShouldMapCessazioneVolontariaMotivation() {
        ElementDigitalAddress address = createAddress("valid@pec.it", null, daysFromNow(1), MotivationTermination.CESSAZIONE_VOLONTARIA);
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid");
        assertNotNull(result.getDigitalAddress());
        assertNotNull(result.getDigitalAddress().getUsageInfo());
        assertEquals(UsageInfoDto.MotivationEnum.CESSAZIONE_VOLONTARIA, result.getDigitalAddress().getUsageInfo().getMotivation());
    }

    @Test
    void mapToResponseOkShouldMapNullMotivation() {
        ElementDigitalAddress address = createAddress("valid@pec.it", null, daysFromNow(1), null);
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), address);
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid");
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
        GetDigitalAddressINADOKDto result = assertDoesNotThrow(() -> InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationId"));
        assertEquals(VALID_CF, result.getTaxId());
        assertNull(result.getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldIgnoreNullDigitalAddressElement() {
        ElementDigitalAddress validAddress = createPersonalAddress("valid@pec.it");
        ResponseRequestDigitalAddress input = new ResponseRequestDigitalAddress();
        input.setCodiceFiscale(VALID_CF);
        input.setSince(new Date());
        input.setDigitalAddress(java.util.Arrays.asList(null, validAddress));

        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationId");

        assertNotNull(result.getDigitalAddress());
        assertEquals("valid@pec.it", result.getDigitalAddress().getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldNotSetAddressForPgWhenTaxIdIsNotPiva() {
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), createPersonalAddress("company@pec.it"));
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PG, VALID_CF, false, "correlationid");
        assertNull(result.getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldNotSetAddressForPgWhenMoreThanOneAddress() {
        ResponseRequestDigitalAddress input = createResponse(VALID_PIVA, new Date(),
                createPersonalAddress("company1@pec.it"), createPersonalAddress("company2@pec.it"));
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PG, VALID_PIVA, false, "correlationid");
        assertNull(result.getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldNotSetAddressForPgWhenAddressIsProfessional() {
        ResponseRequestDigitalAddress input = createResponse(VALID_PIVA, new Date(),
                createProfessionalAddress("professional@pec.it", "AVVOCATO"));
        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PG, VALID_PIVA, false, "correlationid");
        assertNull(result.getDigitalAddress());
    }

    @Test
    void mapToResponseOkShouldThrowForPgWhenAllAddressesAreExpired() {
        ResponseRequestDigitalAddress input = createResponse(VALID_PIVA, new Date(),
                createAddress("expired@pec.it", null, daysFromNow(-2), MotivationTermination.CESSAZIONE_VOLONTARIA));
        assertThrows(PnNationalRegistriesException.class,
                () -> InadConverter.mapToResponseOk(input, PG, VALID_PIVA, false, "correlationid"));
    }

    @Test
    void mapToResponseOkShouldThrowForPfWhenAllAddressesAreExpired() {
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(),
                createAddress("expired@pec.it", null, daysFromNow(-2), MotivationTermination.CESSAZIONE_UFFICIO));
        assertThrows(PnNationalRegistriesException.class, () -> InadConverter.mapToResponseOk(input, PF, VALID_CF, true, "correlationId"));
    }

    @Test
    void mapToResponseOkShouldThrowForPfWhenResponseTaxIdIsNotACodiceFiscale() {
        ResponseRequestDigitalAddress input = createResponse(VALID_PIVA, new Date(), createPersonalAddress("personal@pec.it"));
        PnNationalRegistriesException exception = assertThrows(PnNationalRegistriesException.class,
                () -> InadConverter.mapToResponseOk(input, PF, VALID_PIVA, false, "correlationId"));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void mapToResponseOkShouldThrowForPfWhenPersonalEmailIsInvalidAndFallbackDisabled() {
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), createPersonalAddress("not-an-email"));
        assertThrows(PnNationalRegistriesException.class,
                () -> InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid"));
    }

    @Test
    void mapToResponseOkShouldThrowForPfWhenFallbackEnabledPersonalEmailInvalidAndNoProfessionalAddress() {
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), createPersonalAddress("not-an-email"));
        assertThrows(PnNationalRegistriesException.class,
                () -> InadConverter.mapToResponseOk(input, PF, VALID_CF, true, "correlationid"));
    }

    @Test
    void mapResponseByTaxIdShouldReturnEmptyMapForNullResponse() {
        assertTrue(inadConverterInstance().mapResponseByTaxId(null).isEmpty());
    }

    @Test
    void mapResponseByTaxIdShouldReturnEmptyMapForNullList() {
        it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress response =
                new it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress();
        response.setList(null);
        assertTrue(inadConverterInstance().mapResponseByTaxId(response).isEmpty());
    }

    @Test
    void mapResponseByTaxIdShouldReturnEmptyMapForEmptyList() {
        it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress response =
                new it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress();
        response.setList(List.of());
        assertTrue(inadConverterInstance().mapResponseByTaxId(response).isEmpty());
    }

    @Test
    void mapResponseByTaxIdShouldIndexByTaxIdKeepingFirstOnDuplicates() {
        ResponseRequestDigitalAddress first = createResponse(VALID_CF, new Date(), createPersonalAddress("first@pec.it"));
        ResponseRequestDigitalAddress duplicate = createResponse(VALID_CF, new Date(), createPersonalAddress("second@pec.it"));
        ResponseRequestDigitalAddress other = createResponse(VALID_PIVA, new Date(), createPersonalAddress("other@pec.it"));
        it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress response =
                new it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress();
        response.setList(List.of(first, duplicate, other));

        java.util.Map<String, ResponseRequestDigitalAddress> result = inadConverterInstance().mapResponseByTaxId(response);

        assertEquals(2, result.size());
        assertSame(first, result.get(VALID_CF));
        assertSame(other, result.get(VALID_PIVA));
    }

    /**
     * N12 - un domicilio appena scaduto non deve oscurare un secondo candidato ancora valido.
     */
    @Test
    void mapToResponseOkShouldSkipJustExpiredPersonalAddressAndUseNextValidCandidate() {
        ElementDigitalAddress expired = createAddress("expired@pec.it", null, Date.from(Instant.now().minusSeconds(1)), null);
        ElementDigitalAddress valid = createAddress("valid@pec.it", null, Date.from(Instant.now().plusSeconds(60)), null);
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), expired, valid);

        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid");

        assertNotNull(result.getDigitalAddress());
        assertEquals("valid@pec.it", result.getDigitalAddress().getDigitalAddress());
    }

    /**
     * N12 - una validità futura deve mantenere utilizzabile il domicilio.
     */
    @Test
    void mapToResponseOkShouldAcceptAddressWhoseEndValidityIsInTheFuture() {
        ElementDigitalAddress futureValid = createAddress("future-valid@pec.it", null, Date.from(Instant.now().plusSeconds(60)), null);
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), futureValid);

        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, false, "correlationid");

        assertNotNull(result.getDigitalAddress());
        assertEquals("future-valid@pec.it", result.getDigitalAddress().getDigitalAddress());
    }

    /**
     * N12 - con fallback PF abilitato un primo professionale non utilizzabile non deve impedire la scelta del secondo valido.
     */
    @Test
    void mapToResponseOkShouldSkipInvalidProfessionalCandidateAndUseSecondValidProfessionalAddress() {
        ElementDigitalAddress invalidPersonal = createPersonalAddress("not-an-email");
        ElementDigitalAddress invalidProfessional = createProfessionalAddress("also-not-an-email", "AVVOCATO");
        ElementDigitalAddress validProfessional = createProfessionalAddress("valid-professional@pec.it", "COMMERCIALISTA");
        ResponseRequestDigitalAddress input = createResponse(VALID_CF, new Date(), invalidPersonal, invalidProfessional, validProfessional);

        GetDigitalAddressINADOKDto result = InadConverter.mapToResponseOk(input, PF, VALID_CF, true, "correlationid");

        assertNotNull(result.getDigitalAddress());
        assertEquals("valid-professional@pec.it", result.getDigitalAddress().getDigitalAddress());
        assertEquals("COMMERCIALISTA", result.getDigitalAddress().getPracticedProfession());
    }

    private InadConverter inadConverterInstance() {
        try {
            java.lang.reflect.Constructor<InadConverter> constructor = InadConverter.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
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