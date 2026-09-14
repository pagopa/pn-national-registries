package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;

import static it.pagopa.pn.national.registries.constant.RecipientType.PF;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigitalAddressServiceTest {

    private static final String CX_ID = "cx-id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String TAX_ID = "RSSMRA80A01H501U";

    @Mock
    private IpaService ipaService;

    @Mock
    private InfoCamereService infoCamereService;

    @Mock
    private InadService inadService;

    @Mock
    private SqsService sqsService;

    @Mock
    private GatewayUtils gatewayUtils;

    @InjectMocks
    private DigitalAddressService digitalAddressService;


    @Test
    void retrieveDigitalAddressForPG_shouldPushIpaAddressToSqsWhenValid() {
        AddressRequestBodyDto request = buildRequest();

        IPAPecDto ipaResponse = new IPAPecDto();
        ipaResponse.setDomicilioDigitale("ipa@pec.it");
        ipaResponse.setDenominazione("Comune");
        ipaResponse.setCodEnte("123");
        ipaResponse.setTipo("PA");

        when(ipaService.getIpaPec(any(IPARequestBodyDto.class)))
                .thenReturn(Mono.just(ipaResponse));

        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), eq(CX_ID)))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        digitalAddressService.retrieveDigitalAddressForPG(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .verifyComplete();

        verify(ipaService).getIpaPec(any(IPARequestBodyDto.class));

        verify(sqsService).pushToOutputQueue(
                argThat(dto ->
                        CORRELATION_ID.equals(dto.getCorrelationId())
                                && dto.getDigitalAddress() != null
                                && dto.getDigitalAddress().size() == 1
                                && "ipa@pec.it".equals(
                                dto.getDigitalAddress().getFirst().getAddress()
                        )
                ),
                eq(CX_ID)
        );

        verifyNoInteractions(infoCamereService);
    }


    @Test
    void retrieveDigitalAddressForPG_shouldFallbackToIniPecWhenIpaResponseIsEmpty() {
        AddressRequestBodyDto request = buildRequest();

        IPAPecDto ipaResponse = new IPAPecDto();

        when(ipaService.getIpaPec(any(IPARequestBodyDto.class)))
                .thenReturn(Mono.just(ipaResponse));

        when(infoCamereService.getIniPecDigitalAddress(
                eq(CX_ID),
                any(GetDigitalAddressIniPECRequestBodyDto.class),
                eq(request.getFilter().getReferenceRequestDate())
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        digitalAddressService.retrieveDigitalAddressForPG(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .verifyComplete();

        verify(infoCamereService).getIniPecDigitalAddress(
                eq(CX_ID),
                argThat(req ->
                        req.getFilter() != null
                                && CORRELATION_ID.equals(req.getFilter().getCorrelationId())
                                && TAX_ID.equals(req.getFilter().getTaxId())
                ),
                eq(request.getFilter().getReferenceRequestDate())
        );

        verifyNoInteractions(sqsService);
    }


    @Test
    void retrieveDigitalAddressForPG_shouldFallbackToIniPecWhenIpaEmailIsInvalid() {
        AddressRequestBodyDto request = buildRequest();

        IPAPecDto ipaResponse = new IPAPecDto();
        ipaResponse.setDomicilioDigitale("not-an-email");
        ipaResponse.setDenominazione("Comune");
        ipaResponse.setCodEnte("123");
        ipaResponse.setTipo("PA");

        when(ipaService.getIpaPec(any(IPARequestBodyDto.class)))
                .thenReturn(Mono.just(ipaResponse));

        when(infoCamereService.getIniPecDigitalAddress(
                eq(CX_ID),
                any(GetDigitalAddressIniPECRequestBodyDto.class),
                any(Date.class)
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        digitalAddressService.retrieveDigitalAddressForPG(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .verifyComplete();

        verify(infoCamereService).getIniPecDigitalAddress(
                eq(CX_ID),
                any(GetDigitalAddressIniPECRequestBodyDto.class),
                eq(request.getFilter().getReferenceRequestDate())
        );

        verifyNoInteractions(sqsService);
    }


    @Test
    void retrieveDigitalAddressForPG_shouldPropagateIpaError() {
        AddressRequestBodyDto request = buildRequest();

        RuntimeException exception = new RuntimeException("IPA error");

        when(ipaService.getIpaPec(any(IPARequestBodyDto.class)))
                .thenReturn(Mono.error(exception));

        StepVerifier.create(
                        digitalAddressService.retrieveDigitalAddressForPG(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .expectErrorMatches(error -> error == exception)
                .verify();

        verify(gatewayUtils).logEServiceError(
                eq(exception),
                eq("can not retrieve digital address from IPA: {}")
        );

        verifyNoInteractions(infoCamereService);
        verifyNoInteractions(sqsService);
    }


    @Test
    void retrieveDigitalAddressFromInad_shouldPushAddressToSqsForPf() {
        AddressRequestBodyDto request = buildRequest();

        DigitalAddressDto digitalAddress = new DigitalAddressDto();
        digitalAddress.setDigitalAddress("inad@pec.it");

        GetDigitalAddressINADOKDto inadResponse =
                new GetDigitalAddressINADOKDto();
        inadResponse.setDigitalAddress(digitalAddress);

        when(inadService.callEService(
                any(GetDigitalAddressINADRequestBodyDto.class),
                eq(PF)
        )).thenReturn(Mono.just(inadResponse));

        when(sqsService.pushToOutputQueue(
                any(CodeSqsDto.class),
                eq(CX_ID)
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        digitalAddressService.retrieveDigitalAddressFromInadForPF(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .verifyComplete();

        verify(inadService).callEService(
                argThat(req ->
                        req.getFilter() != null
                                && TAX_ID.equals(req.getFilter().getTaxId())
                                && CORRELATION_ID.equals(
                                req.getFilter().getPracticalReference()
                        )
                ),
                eq(PF)
        );

        verify(sqsService).pushToOutputQueue(
                argThat(dto ->
                        CORRELATION_ID.equals(dto.getCorrelationId())
                                && dto.getDigitalAddress() != null
                                && dto.getDigitalAddress().size() == 1
                                && "inad@pec.it".equals(
                                dto.getDigitalAddress().getFirst().getAddress()
                        )
                ),
                eq(CX_ID)
        );
    }


    @Test
    void retrieveDigitalAddressFromInad_ForPf_shouldPropagateError() {
        AddressRequestBodyDto request = buildRequest();

        RuntimeException exception = new RuntimeException("INAD error");

        when(inadService.callEService(
                any(GetDigitalAddressINADRequestBodyDto.class),
                eq(PF)
        )).thenReturn(Mono.error(exception));

        StepVerifier.create(
                        digitalAddressService.retrieveDigitalAddressFromInadForPF(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .expectErrorMatches(error -> error == exception)
                .verify();

        verify(gatewayUtils).logEServiceError(
                eq(exception),
                eq("can not retrieve digital address from INAD: {}")
        );

        verifyNoInteractions(sqsService);
    }


    @Test
    void retrieveDigitalAddressWithIniPecFallback_shouldPushInadAddressWhenPresent() {
        AddressRequestBodyDto request = buildRequest();

        DigitalAddressDto digitalAddress = new DigitalAddressDto();
        digitalAddress.setDigitalAddress("inad@pec.it");

        GetDigitalAddressINADOKDto inadResponse =
                new GetDigitalAddressINADOKDto();
        inadResponse.setDigitalAddress(digitalAddress);

        when(inadService.callEService(
                any(GetDigitalAddressINADRequestBodyDto.class),
                eq(PF)
        )).thenReturn(Mono.just(inadResponse));

        when(sqsService.pushToOutputQueue(
                any(CodeSqsDto.class),
                eq(CX_ID)
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        digitalAddressService.retrieveDigitalAddressForPFFromInadWithIniPecFallback(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .verifyComplete();

        verify(sqsService).pushToOutputQueue(
                argThat(dto ->
                        dto.getDigitalAddress() != null
                                && dto.getDigitalAddress().size() == 1
                                && "inad@pec.it".equals(
                                dto.getDigitalAddress().getFirst().getAddress()
                        )
                ),
                eq(CX_ID)
        );

        verifyNoInteractions(infoCamereService);
    }


    @Test
    void retrieveDigitalAddressWithIniPecFallback_shouldUseIniPecWhenInadReturnsEmptyAddress() {
        AddressRequestBodyDto request = buildRequest();

        GetDigitalAddressINADOKDto inadResponse =
                new GetDigitalAddressINADOKDto();

        inadResponse.setDigitalAddress(new DigitalAddressDto());

        when(inadService.callEService(
                any(GetDigitalAddressINADRequestBodyDto.class),
                eq(PF)
        )).thenReturn(Mono.just(inadResponse));

        when(infoCamereService.getIniPecDigitalAddress(
                eq(CX_ID),
                any(GetDigitalAddressIniPECRequestBodyDto.class),
                eq(request.getFilter().getReferenceRequestDate())
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        digitalAddressService.retrieveDigitalAddressForPFFromInadWithIniPecFallback(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .verifyComplete();

        verify(infoCamereService).getIniPecDigitalAddress(
                eq(CX_ID),
                any(GetDigitalAddressIniPECRequestBodyDto.class),
                eq(request.getFilter().getReferenceRequestDate())
        );

        verifyNoInteractions(sqsService);
    }


    @Test
    void retrieveDigitalAddressWithIniPecFallback_shouldUseIniPecWhenInadReturns404() {
        AddressRequestBodyDto request = buildRequest();

        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode())
                .thenReturn(HttpStatus.NOT_FOUND);

        when(inadService.callEService(
                any(GetDigitalAddressINADRequestBodyDto.class),
                eq(PF)
        )).thenReturn(Mono.error(exception));

        when(infoCamereService.getIniPecDigitalAddress(
                eq(CX_ID),
                any(GetDigitalAddressIniPECRequestBodyDto.class),
                eq(request.getFilter().getReferenceRequestDate())
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        digitalAddressService.retrieveDigitalAddressForPFFromInadWithIniPecFallback(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .verifyComplete();

        verify(infoCamereService).getIniPecDigitalAddress(
                eq(CX_ID),
                any(GetDigitalAddressIniPECRequestBodyDto.class),
                eq(request.getFilter().getReferenceRequestDate())
        );

        verifyNoInteractions(sqsService);
    }


    @Test
    void retrieveDigitalAddressWithIniPecFallback_shouldPropagateNon404InadError() {
        AddressRequestBodyDto request = buildRequest();

        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode())
                .thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        when(inadService.callEService(
                any(GetDigitalAddressINADRequestBodyDto.class),
                eq(PF)
        )).thenReturn(Mono.error(exception));

        StepVerifier.create(
                        digitalAddressService.retrieveDigitalAddressForPFFromInadWithIniPecFallback(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .expectError(PnNationalRegistriesException.class)
                .verify();

        verifyNoInteractions(infoCamereService);
        verifyNoInteractions(sqsService);
    }

    private AddressRequestBodyDto buildRequest() {
        AddressRequestBodyFilterDto filter =
                new AddressRequestBodyFilterDto();

        filter.setCorrelationId(CORRELATION_ID);
        filter.setTaxId(TAX_ID);
        filter.setReferenceRequestDate(new Date());
        filter.setDomicileType(
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL
        );

        AddressRequestBodyDto request =
                new AddressRequestBodyDto();

        request.setFilter(filter);

        return request;
    }
}

