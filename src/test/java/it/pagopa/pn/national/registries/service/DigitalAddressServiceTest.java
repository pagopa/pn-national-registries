package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.AddressInternalResult;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;

import static it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType.IMPRESA;
import static it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType.PERSONALE;
import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static it.pagopa.pn.national.registries.constant.RecipientType.PG;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigitalAddressServiceTest {

    private static final String CORRELATION_ID = "correlation-id";
    private static final String TAX_ID = "RSSMRA80A01H501U";

    @Mock
    private IpaService ipaService;

    @Mock
    private InadService inadService;

    @Mock
    private GatewayUtils gatewayUtils;

    @InjectMocks
    private DigitalAddressService digitalAddressService;

    @Test
    void retrieveDigitalAddressFromIpa_shouldReturnFoundWhenIpaResponseIsValid() {
        AddressRequestBodyDto request = buildRequest();
        IPAPecDto ipaResponse = new IPAPecDto();
        ipaResponse.setDomicilioDigitale("ipa@pec.it");
        ipaResponse.setDenominazione("Comune");
        ipaResponse.setCodEnte("123");
        ipaResponse.setTipo("PA");

        when(ipaService.getIpaPec(any(IPARequestBodyDto.class))).thenReturn(Mono.just(ipaResponse));

        StepVerifier.create(digitalAddressService.retrieveDigitalAddressFromIpa(request, CORRELATION_ID))
                .assertNext(result -> {
                    AddressInternalResult.Found found = assertInstanceOf(AddressInternalResult.Found.class, result);
                    assertEquals(CORRELATION_ID, found.codeSqsDto().getCorrelationId());
                    assertEquals("IPA", found.codeSqsDto().getRegistry());
                    assertNotNull(found.codeSqsDto().getDigitalAddress());
                    assertEquals(1, found.codeSqsDto().getDigitalAddress().size());
                    assertEquals("ipa@pec.it", found.codeSqsDto().getDigitalAddress().getFirst().getAddress());
                })
                .verifyComplete();

        verify(ipaService).getIpaPec(any(IPARequestBodyDto.class));
    }

    @Test
    void retrieveDigitalAddressFromIpa_shouldReturnNotFoundWhenIpaResponseIsEmpty() {
        AddressRequestBodyDto request = buildRequest();

        when(ipaService.getIpaPec(any(IPARequestBodyDto.class))).thenReturn(Mono.just(new IPAPecDto()));

        StepVerifier.create(digitalAddressService.retrieveDigitalAddressFromIpa(request, CORRELATION_ID))
                .assertNext(result -> {
                    AddressInternalResult.NotFound notFound = assertInstanceOf(AddressInternalResult.NotFound.class, result);
                    assertNull(notFound.codeSqsDto());
                })
                .verifyComplete();
    }

    @Test
    void retrieveDigitalAddressFromIpa_shouldReturnNotFoundWhenIpaEmailIsInvalid() {
        AddressRequestBodyDto request = buildRequest();
        IPAPecDto ipaResponse = new IPAPecDto();
        ipaResponse.setDomicilioDigitale("not-an-email");
        ipaResponse.setDenominazione("Comune");
        ipaResponse.setCodEnte("123");
        ipaResponse.setTipo("PA");

        when(ipaService.getIpaPec(any(IPARequestBodyDto.class))).thenReturn(Mono.just(ipaResponse));

        StepVerifier.create(digitalAddressService.retrieveDigitalAddressFromIpa(request, CORRELATION_ID))
                .assertNext(result -> assertInstanceOf(AddressInternalResult.NotFound.class, result))
                .verifyComplete();
    }

    @Test
    void retrieveDigitalAddressFromIpa_shouldPropagateIpaError() {
        AddressRequestBodyDto request = buildRequest();
        RuntimeException exception = new RuntimeException("IPA error");

        when(ipaService.getIpaPec(any(IPARequestBodyDto.class))).thenReturn(Mono.error(exception));

        StepVerifier.create(digitalAddressService.retrieveDigitalAddressFromIpa(request, CORRELATION_ID))
                .expectErrorMatches(error -> error == exception)
                .verify();

        verify(gatewayUtils).logEServiceError(exception, "can not retrieve digital address from IPA: {}");
    }

    @Test
    void retrieveDigitalAddressFromInadSync_shouldReturnFoundWhenDigitalAddressIsValid() {
        AddressRequestBodyDto request = buildRequest();
        DigitalAddressDto digitalAddress = new DigitalAddressDto();
        digitalAddress.setDigitalAddress("inad@pec.it");

        GetDigitalAddressINADOKDto inadResponse = new GetDigitalAddressINADOKDto();
        inadResponse.setTaxId(TAX_ID);
        inadResponse.setDigitalAddress(digitalAddress);

        when(inadService.callEService(any(GetDigitalAddressINADRequestBodyDto.class), eq(PF), eq(CORRELATION_ID)))
                .thenReturn(Mono.just(inadResponse));

        StepVerifier.create(digitalAddressService.retrieveDigitalAddressFromInadSync(request, CORRELATION_ID, PF))
                .assertNext(result -> {
                    AddressInternalResult.Found found = assertInstanceOf(AddressInternalResult.Found.class, result);
                    assertEquals(CORRELATION_ID, found.codeSqsDto().getCorrelationId());
                    assertEquals("INAD", found.codeSqsDto().getRegistry());
                    assertNotNull(found.codeSqsDto().getDigitalAddress());
                    assertEquals(1, found.codeSqsDto().getDigitalAddress().size());
                    assertEquals("inad@pec.it", found.codeSqsDto().getDigitalAddress().getFirst().getAddress());
                    assertEquals(PERSONALE.getValue(), found.codeSqsDto().getDigitalAddress().getFirst().getRecipient());
                })
                .verifyComplete();
    }

    @Test
    void retrieveDigitalAddressFromInadSync_shouldReturnNotFoundWhenDigitalAddressIsMissing() {
        AddressRequestBodyDto request = buildRequest();
        GetDigitalAddressINADOKDto inadResponse = new GetDigitalAddressINADOKDto();
        inadResponse.setTaxId(TAX_ID);
        inadResponse.setDigitalAddress(null);

        when(inadService.callEService(any(GetDigitalAddressINADRequestBodyDto.class), eq(PF), eq(CORRELATION_ID)))
                .thenReturn(Mono.just(inadResponse));

        StepVerifier.create(digitalAddressService.retrieveDigitalAddressFromInadSync(request, CORRELATION_ID, PF))
                .assertNext(result -> {
                    AddressInternalResult.NotFound notFound = assertInstanceOf(AddressInternalResult.NotFound.class, result);
                    assertNotNull(notFound.codeSqsDto());
                    assertNotNull(notFound.codeSqsDto().getDigitalAddress());
                    assertTrue(notFound.codeSqsDto().getDigitalAddress().isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void retrieveDigitalAddressFromInadSync_shouldPropagateInadError() {
        AddressRequestBodyDto request = buildRequest();
        RuntimeException exception = new RuntimeException("INAD error");

        when(inadService.callEService(any(GetDigitalAddressINADRequestBodyDto.class), eq(PF), eq(CORRELATION_ID)))
                .thenReturn(Mono.error(exception));

        StepVerifier.create(digitalAddressService.retrieveDigitalAddressFromInadSync(request, CORRELATION_ID, PF))
                .expectErrorMatches(error -> error == exception)
                .verify();

        verify(gatewayUtils).logEServiceError(exception, "can not retrieve digital address from INAD: {}");
        verifyNoInteractions(ipaService);
    }

    @Test
    void retrieveDigitalAddressFromInadSync_shouldPropagateRecognizedNotFoundWhenSelectedEmailIsInvalid() {
        AddressRequestBodyDto request = buildRequest();
        DigitalAddressDto digitalAddress = new DigitalAddressDto();
        digitalAddress.setDigitalAddress("not-an-email");

        GetDigitalAddressINADOKDto inadResponse = new GetDigitalAddressINADOKDto();
        inadResponse.setTaxId(TAX_ID);
        inadResponse.setDigitalAddress(digitalAddress);

        when(inadService.callEService(any(GetDigitalAddressINADRequestBodyDto.class), eq(PF), eq(CORRELATION_ID)))
                .thenReturn(Mono.just(inadResponse));

        StepVerifier.create(digitalAddressService.retrieveDigitalAddressFromInadSync(request, CORRELATION_ID, PF))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(PnNationalRegistriesException.class, error);
                    PnNationalRegistriesException exception = (PnNationalRegistriesException) error;
                    assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatusCode());
                    assertEquals("CF non trovato", exception.getMessage());
                })
                .verify();
    }

    @Test
    void retrieveDigitalAddressFromInadSync_shouldMapPgSingleCompanyAddressAsFoundForImpresa() {
        AddressRequestBodyDto request = buildRequest();
        request.getFilter().setTaxId("12345678901");

        DigitalAddressDto digitalAddress = new DigitalAddressDto();
        digitalAddress.setDigitalAddress("company@pec.it");

        GetDigitalAddressINADOKDto inadResponse = new GetDigitalAddressINADOKDto();
        inadResponse.setTaxId("12345678901");
        inadResponse.setDigitalAddress(digitalAddress);

        when(inadService.callEService(any(GetDigitalAddressINADRequestBodyDto.class), eq(PG), eq(CORRELATION_ID)))
                .thenReturn(Mono.just(inadResponse));

        StepVerifier.create(digitalAddressService.retrieveDigitalAddressFromInadSync(request, CORRELATION_ID, PG))
                .assertNext(result -> {
                    AddressInternalResult.Found found = assertInstanceOf(AddressInternalResult.Found.class, result);
                    assertEquals("company@pec.it", found.codeSqsDto().getDigitalAddress().getFirst().getAddress());
                    assertEquals(IMPRESA.getValue(), found.codeSqsDto().getDigitalAddress().getFirst().getRecipient());
                })
                .verifyComplete();
    }

    private AddressRequestBodyDto buildRequest() {
        AddressRequestBodyFilterDto filter = new AddressRequestBodyFilterDto();
        filter.setCorrelationId(CORRELATION_ID);
        filter.setTaxId(TAX_ID);
        filter.setReferenceRequestDate(new Date());
        filter.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);

        AddressRequestBodyDto request = new AddressRequestBodyDto();
        request.setFilter(filter);
        return request;
    }
}
