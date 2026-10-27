package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.MotivationTermination;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.UsageInfo;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_NAME_INAD_ADDRESS;
import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InadServiceTest {

    @InjectMocks
    InadService inadService;

    @Mock
    InadClient inadClient;

    @Mock
    ValidateTaxIdUtils validateTaxIdUtils;

    @Mock
    NationalRegistriesConfig nationalRegistriesConfig;

    private static final String TAX_ID = "CMUJFD29M22L916P";
    private static final String DIGITAL_ADDRESS_1 = "da_1";
    private static final String DIGITAL_ADDRESS_2 = "da_2";
    private static final String DIGITAL_ADDRESS_3 = "da_3";

    @Test
    void callEServiceOldWorkflow() {

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(false);
        String practicalReference = "00001";
        Date now = new Date();

        UsageInfo usageInfo1 = new UsageInfo();
        usageInfo1.setDateEndValidity(Date.from(LocalDate.EPOCH.atStartOfDay(ZoneOffset.UTC).toInstant()));
        usageInfo1.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        ElementDigitalAddress elementDigitalAddressDto1 = new ElementDigitalAddress();
        elementDigitalAddressDto1.setDigitalAddress(DIGITAL_ADDRESS_1);
        elementDigitalAddressDto1.setUsageInfo(usageInfo1);

        UsageInfo usageInfo2 = new UsageInfo();
        usageInfo2.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        usageInfo2.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        ElementDigitalAddress elementDigitalAddressDto2 = new ElementDigitalAddress();
        elementDigitalAddressDto2.setDigitalAddress(DIGITAL_ADDRESS_2);
        elementDigitalAddressDto2.setUsageInfo(usageInfo2);

        UsageInfo usageInfo3 = new UsageInfo();
        usageInfo3.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        usageInfo3.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        ElementDigitalAddress elementDigitalAddressDto3 = new ElementDigitalAddress();
        elementDigitalAddressDto1.setPracticedProfession("practicedProfession");
        elementDigitalAddressDto3.setDigitalAddress(DIGITAL_ADDRESS_3);
        elementDigitalAddressDto3.setUsageInfo(usageInfo3);

        ResponseRequestDigitalAddress responseRequestDigitalAddressDto = new ResponseRequestDigitalAddress();
        responseRequestDigitalAddressDto.setCodiceFiscale(TAX_ID);
        responseRequestDigitalAddressDto.setSince(now);
        List<ElementDigitalAddress> lista = List.of(elementDigitalAddressDto1, elementDigitalAddressDto2, elementDigitalAddressDto3);
        responseRequestDigitalAddressDto.setDigitalAddress(lista);

        when(inadClient.callEService(TAX_ID, practicalReference))
                .thenReturn(Mono.just(responseRequestDigitalAddressDto));

        GetDigitalAddressINADRequestBodyDto req = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();
        filterDto.setTaxId(TAX_ID);
        filterDto.setPracticalReference(practicalReference);
        req.setFilter(filterDto);


        StepVerifier.create(inadService.callEService(req, PF, "correlationId"))
                .expectNextMatches(getDigitalAddressINADOKDto -> Objects.isNull(getDigitalAddressINADOKDto.getDigitalAddress().getPracticedProfession()))
                .verifyComplete();
    }

    @Test
    void callEServiceREferenceDateNullOldWorkflowPersonalPecRetrieved() {
        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(false);

        String practicalReference = "00001";
        Date now = new Date();

        UsageInfo usageInfo1 = new UsageInfo();
        usageInfo1.setDateEndValidity(Date.from(LocalDate.EPOCH.atStartOfDay(ZoneOffset.UTC).toInstant()));
        usageInfo1.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        ElementDigitalAddress elementDigitalAddressDto1 = new ElementDigitalAddress();
        elementDigitalAddressDto1.setDigitalAddress(DIGITAL_ADDRESS_1);
        elementDigitalAddressDto1.setUsageInfo(usageInfo1);

        UsageInfo usageInfo3 = new UsageInfo();
        usageInfo3.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        usageInfo3.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        ElementDigitalAddress elementDigitalAddressDto3 = new ElementDigitalAddress();
        elementDigitalAddressDto3.setPracticedProfession("practicedProfession");
        elementDigitalAddressDto3.setDigitalAddress(DIGITAL_ADDRESS_3);
        elementDigitalAddressDto3.setUsageInfo(usageInfo3);

        UsageInfo usageInfo2 = new UsageInfo();
        usageInfo2.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        usageInfo2.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        ElementDigitalAddress elementDigitalAddressDto2 = new ElementDigitalAddress();
        elementDigitalAddressDto2.setDigitalAddress(DIGITAL_ADDRESS_2);
        elementDigitalAddressDto2.setUsageInfo(usageInfo2);

        ResponseRequestDigitalAddress responseRequestDigitalAddressDto = new ResponseRequestDigitalAddress();
        responseRequestDigitalAddressDto.setCodiceFiscale(TAX_ID);
        responseRequestDigitalAddressDto.setSince(now);
        List<ElementDigitalAddress> lista = List.of(elementDigitalAddressDto1, elementDigitalAddressDto2, elementDigitalAddressDto3);
        responseRequestDigitalAddressDto.setDigitalAddress(lista);

        when(inadClient.callEService(TAX_ID, practicalReference))
                .thenReturn(Mono.just(responseRequestDigitalAddressDto));

        GetDigitalAddressINADRequestBodyDto req = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();
        filterDto.setTaxId(TAX_ID);
        filterDto.setPracticalReference(practicalReference);
        req.setFilter(filterDto);

        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();
        response.setTaxId(TAX_ID);
        response.setSince(now);
        response.setDigitalAddress(
                new DigitalAddressDto()
                        .digitalAddress(DIGITAL_ADDRESS_2)
                        .usageInfo(new UsageInfoDto()
                                .dateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()))
                                .motivation(UsageInfoDto.MotivationEnum.CESSAZIONE_UFFICIO)));


        StepVerifier.create(inadService.callEService(req, PF,"correlationId"))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void callEServiceREferenceDateNullPfProfessionEnable() {

        when(nationalRegistriesConfig.isEnablePfPecFallbackFlow()).thenReturn(true);
        String practicalReference = "00001";
        Date now = new Date();

        UsageInfo usageInfo3 = new UsageInfo();
        usageInfo3.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        usageInfo3.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        ElementDigitalAddress elementDigitalAddressDto3 = new ElementDigitalAddress();
        elementDigitalAddressDto3.setPracticedProfession("practicedProfession");
        elementDigitalAddressDto3.setDigitalAddress(DIGITAL_ADDRESS_3);
        elementDigitalAddressDto3.setUsageInfo(usageInfo3);

        UsageInfo usageInfo2 = new UsageInfo();
        usageInfo2.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        usageInfo2.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        ElementDigitalAddress elementDigitalAddressDto2 = new ElementDigitalAddress();
        elementDigitalAddressDto2.setDigitalAddress(DIGITAL_ADDRESS_2);
        elementDigitalAddressDto2.setPracticedProfession("practicedProfession2");
        elementDigitalAddressDto2.setUsageInfo(usageInfo2);

        ResponseRequestDigitalAddress responseRequestDigitalAddressDto = new ResponseRequestDigitalAddress();
        responseRequestDigitalAddressDto.setCodiceFiscale(TAX_ID);
        responseRequestDigitalAddressDto.setSince(now);
        List<ElementDigitalAddress> lista = List.of(elementDigitalAddressDto2, elementDigitalAddressDto3);
        responseRequestDigitalAddressDto.setDigitalAddress(lista);

        when(inadClient.callEService(TAX_ID, practicalReference))
                .thenReturn(Mono.just(responseRequestDigitalAddressDto));

        GetDigitalAddressINADRequestBodyDto req = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();
        filterDto.setTaxId(TAX_ID);
        filterDto.setPracticalReference(practicalReference);
        req.setFilter(filterDto);

        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();
        response.setTaxId(TAX_ID);
        response.setSince(now);
        response.setDigitalAddress(
                new DigitalAddressDto()
                        .digitalAddress(DIGITAL_ADDRESS_2)
                        .practicedProfession("practicedProfession2")
                        .usageInfo(new UsageInfoDto()
                                .dateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()))
                                .motivation(UsageInfoDto.MotivationEnum.CESSAZIONE_UFFICIO)));


        StepVerifier.create(inadService.callEService(req, PF, "correlationId"))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void callEServiceShouldPropagateClientError() {
        String practicalReference = "00001";
        RuntimeException error = new RuntimeException("INAD unavailable");
        GetDigitalAddressINADRequestBodyDto req = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();
        filterDto.setTaxId(TAX_ID);
        filterDto.setPracticalReference(practicalReference);
        req.setFilter(filterDto);

        when(inadClient.callEService(TAX_ID, practicalReference)).thenReturn(Mono.error(error));

        StepVerifier.create(inadService.callEService(req, PF, "correlationId"))
                .expectErrorMatches(throwable -> throwable == error)
                .verify();

        verify(validateTaxIdUtils).validateTaxId(TAX_ID, PROCESS_NAME_INAD_ADDRESS, false);
        verify(inadClient).callEService(TAX_ID, practicalReference);
        verifyNoInteractions(nationalRegistriesConfig);
    }

    @Test
    void callEServiceShouldStopWhenTaxIdValidationFails() {
        String practicalReference = "00001";
        PnNationalRegistriesException validationError = new PnNationalRegistriesException(
                "invalid tax id",
                org.springframework.http.HttpStatus.BAD_REQUEST.value(),
                org.springframework.http.HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null,
                null,
                Charset.defaultCharset(),
                Object.class
        );
        GetDigitalAddressINADRequestBodyDto req = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();
        filterDto.setTaxId(TAX_ID);
        filterDto.setPracticalReference(practicalReference);
        req.setFilter(filterDto);

        doThrow(validationError).when(validateTaxIdUtils).validateTaxId(TAX_ID, PROCESS_NAME_INAD_ADDRESS, false);

        PnNationalRegistriesException thrown = assertThrows(PnNationalRegistriesException.class,
                () -> inadService.callEService(req, PF, "correlationId"));

        assertSame(validationError, thrown);
        verifyNoInteractions(inadClient, nationalRegistriesConfig);
    }

}