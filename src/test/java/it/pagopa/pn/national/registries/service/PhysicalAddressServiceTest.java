package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.gateway.AddressQueryRequest;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhysicalAddressServiceTest {

    private static final String CORRELATION_ID = "correlation-id";
    private static final String CX_ID = "cx-id";
    private static final String TAX_ID = "RSSMRA80A01H501U";

    @Mock
    private InfoCamereService infoCamereService;

    @Mock
    private AnprService anprService;

    @Mock
    private GatewayUtils gatewayUtils;

    @Mock
    private SqsService sqsService;

    @Mock
    private PnAuditLogEvent auditLogEvent;

    @Mock
    private PnAuditLogEvent successAuditEvent;

    @Mock
    private PnAuditLogEvent failureAuditEvent;

    @InjectMocks
    private PhysicalAddressService physicalAddressService;


    @Test
    void retrieveSyncPhysicalAddresses_shouldReturnBadRequestWhenAddressesAreEmpty() {
        PhysicalAddressesRequestBodyDto request =
                new PhysicalAddressesRequestBodyDto();

        request.setCorrelationId(CORRELATION_ID);
        request.setAddresses(List.of());

        StepVerifier.create(
                        physicalAddressService.retrieveSyncPhysicalAddresses(request)
                )
                .expectErrorMatches(error ->
                        error instanceof PnNationalRegistriesException exception
                                && exception.getStatusCode() == HttpStatus.BAD_REQUEST
                )
                .verify();

        verifyNoInteractions(anprService);
        verifyNoInteractions(infoCamereService);
    }


    @Test
    void retrieveSyncPhysicalAddresses_shouldRetrievePfFromAnpr() {
        PhysicalAddressesRequestBodyDto request =
                buildPhysicalAddressesRequest(RecipientType.PF);

        GetAddressANPROKDto anprResponse = new GetAddressANPROKDto();

        ResidentialAddressDto address = new ResidentialAddressDto();
        address.setAddress("Via Roma 1");
        address.setMunicipality("Roma");
        address.setProvince("RM");
        address.setZip("00100");

        anprResponse.setResidentialAddresses(List.of(address));

        mockAuditSuccess();

        when(anprService.getAddressANPR(any(GetAddressANPRRequestBodyDto.class)))
                .thenReturn(Mono.just(anprResponse));

        StepVerifier.create(
                        physicalAddressService.retrieveSyncPhysicalAddresses(request)
                )
                .assertNext(result -> {
                    assertEquals(CORRELATION_ID, result.getCorrelationId());
                    assertNotNull(result.getAddresses());
                    assertEquals(1, result.getAddresses().size());

                    PhysicalAddressResponseDto response =
                            result.getAddresses().getFirst();

                    assertEquals(0, response.getRecIndex());
                    assertEquals(
                            GatewayDownstreamService.ANPR.name(),
                            response.getRegistry()
                    );

                    assertNotNull(response.getPhysicalAddress());
                    assertEquals(
                            "Via Roma 1",
                            response.getPhysicalAddress().getAddress()
                    );
                })
                .verifyComplete();

        verify(anprService)
                .getAddressANPR(any(GetAddressANPRRequestBodyDto.class));

        verifyNoInteractions(infoCamereService);
    }


    @Test
    void retrieveSyncPhysicalAddresses_shouldReturnEmptyAddressWhenAnprReturnsNotFound() {
        PhysicalAddressesRequestBodyDto request =
                buildPhysicalAddressesRequest(RecipientType.PF);

        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode())
                .thenReturn(HttpStatus.NOT_FOUND);

        when(exception.getResponseBodyAsString())
                .thenReturn("{\"codiceErroreAnomalia\":\"EN122\"}");

        mockAuditSuccess();

        when(anprService.getAddressANPR(any(GetAddressANPRRequestBodyDto.class)))
                .thenReturn(Mono.error(exception));

        StepVerifier.create(
                        physicalAddressService.retrieveSyncPhysicalAddresses(request)
                )
                .assertNext(result -> {
                    assertEquals(1, result.getAddresses().size());

                    PhysicalAddressResponseDto response =
                            result.getAddresses().getFirst();

                    assertEquals(
                            GatewayDownstreamService.ANPR.name(),
                            response.getRegistry()
                    );
                    assertEquals(0, response.getRecIndex());
                    assertNull(response.getPhysicalAddress());
                })
                .verifyComplete();

        verify(gatewayUtils, never()).handleException(
                any(),
                any(),
                any()
        );
    }


    @Test
    void retrieveSyncPhysicalAddresses_shouldHandleGenericAnprError() {
        PhysicalAddressesRequestBodyDto request =
                buildPhysicalAddressesRequest(RecipientType.PF);

        RuntimeException exception =
                new RuntimeException("ANPR error");

        PhysicalAddressResponseDto handledResponse =
                new PhysicalAddressResponseDto();

        handledResponse.setRecIndex(0);
        handledResponse.setRegistry(GatewayDownstreamService.ANPR.name());

        mockAuditFailure();

        when(anprService.getAddressANPR(any(GetAddressANPRRequestBodyDto.class)))
                .thenReturn(Mono.error(exception));

        when(gatewayUtils.handleException(
                eq(exception),
                any(AddressQueryRequest.class),
                eq(GatewayDownstreamService.ANPR)
        )).thenReturn(handledResponse);

        StepVerifier.create(
                        physicalAddressService.retrieveSyncPhysicalAddresses(request)
                )
                .assertNext(result -> {
                    assertEquals(CORRELATION_ID, result.getCorrelationId());
                    assertEquals(1, result.getAddresses().size());
                    assertSame(
                            handledResponse,
                            result.getAddresses().getFirst()
                    );
                })
                .verifyComplete();

        verify(gatewayUtils).handleException(
                eq(exception),
                any(AddressQueryRequest.class),
                eq(GatewayDownstreamService.ANPR)
        );
    }


    @Test
    void retrieveSyncPhysicalAddresses_shouldRetrievePgFromRegistroImprese() {
        PhysicalAddressesRequestBodyDto request =
                buildPhysicalAddressesRequest(RecipientType.PG);

        GetAddressRegistroImpreseOKProfessionalAddressDto address =
                new GetAddressRegistroImpreseOKProfessionalAddressDto();

        address.setAddress("Via Impresa 10");
        address.setMunicipality("Milano");
        address.setProvince("MI");
        address.setZip("20100");

        GetAddressRegistroImpreseOKDto infoCamereResponse =
                new GetAddressRegistroImpreseOKDto();

        infoCamereResponse.setProfessionalAddress(address);

        mockAuditSuccess();

        when(infoCamereService.getRegistroImpreseLegalAddress(
                any(GetAddressRegistroImpreseRequestBodyDto.class)
        )).thenReturn(Mono.just(infoCamereResponse));

        StepVerifier.create(
                        physicalAddressService.retrieveSyncPhysicalAddresses(request)
                )
                .assertNext(result -> {
                    assertEquals(CORRELATION_ID, result.getCorrelationId());
                    assertEquals(1, result.getAddresses().size());

                    PhysicalAddressResponseDto response =
                            result.getAddresses().getFirst();

                    assertEquals(
                            GatewayDownstreamService.REGISTRO_IMPRESE.name(),
                            response.getRegistry()
                    );

                    assertNotNull(response.getPhysicalAddress());
                    assertEquals(
                            "Via Impresa 10",
                            response.getPhysicalAddress().getAddress()
                    );
                })
                .verifyComplete();

        verify(infoCamereService)
                .getRegistroImpreseLegalAddress(
                        any(GetAddressRegistroImpreseRequestBodyDto.class)
                );

        verifyNoInteractions(anprService);
    }


    @Test
    void retrieveSyncPhysicalAddresses_shouldHandleRegistroImpreseError() {
        PhysicalAddressesRequestBodyDto request =
                buildPhysicalAddressesRequest(RecipientType.PG);

        RuntimeException exception =
                new RuntimeException("InfoCamere error");

        PhysicalAddressResponseDto handledResponse =
                new PhysicalAddressResponseDto();

        handledResponse.setRecIndex(0);
        handledResponse.setRegistry(
                GatewayDownstreamService.REGISTRO_IMPRESE.name()
        );

        mockAuditFailure();

        when(infoCamereService.getRegistroImpreseLegalAddress(
                any(GetAddressRegistroImpreseRequestBodyDto.class)
        )).thenReturn(Mono.error(exception));

        when(gatewayUtils.handleException(
                eq(exception),
                any(AddressQueryRequest.class),
                eq(GatewayDownstreamService.REGISTRO_IMPRESE)
        )).thenReturn(handledResponse);

        StepVerifier.create(
                        physicalAddressService.retrieveSyncPhysicalAddresses(request)
                )
                .assertNext(result -> {
                    assertEquals(1, result.getAddresses().size());
                    assertSame(
                            handledResponse,
                            result.getAddresses().getFirst()
                    );
                })
                .verifyComplete();

        verify(gatewayUtils).handleException(
                eq(exception),
                any(AddressQueryRequest.class),
                eq(GatewayDownstreamService.REGISTRO_IMPRESE)
        );
    }


    @Test
    void retrieveSyncPhysicalAddresses_shouldManageMultipleRecipients() {
        PhysicalAddressesRequestBodyDto request =
                new PhysicalAddressesRequestBodyDto();

        request.setCorrelationId(CORRELATION_ID);
        request.setReferenceRequestDate(new Date());

        RecipientAddressRequestBodyDto pf =
                new RecipientAddressRequestBodyDto();

        pf.setTaxId(TAX_ID);
        pf.setRecIndex(0);
        pf.setRecipientType(
                RecipientAddressRequestBodyDto.RecipientTypeEnum.PF
        );

        RecipientAddressRequestBodyDto pg =
                new RecipientAddressRequestBodyDto();

        pg.setTaxId("12345678901");
        pg.setRecIndex(1);
        pg.setRecipientType(
                RecipientAddressRequestBodyDto.RecipientTypeEnum.PG
        );

        request.setAddresses(List.of(pf, pg));

        ResidentialAddressDto residentialAddress =
                new ResidentialAddressDto();
        residentialAddress.setAddress("Via PF");

        GetAddressANPROKDto anprResponse =
                new GetAddressANPROKDto();
        anprResponse.setResidentialAddresses(
                List.of(residentialAddress)
        );

        GetAddressRegistroImpreseOKProfessionalAddressDto professionalAddress =
                new GetAddressRegistroImpreseOKProfessionalAddressDto();
        professionalAddress.setAddress("Via PG");

        GetAddressRegistroImpreseOKDto infoCamereResponse =
                new GetAddressRegistroImpreseOKDto();
        infoCamereResponse.setProfessionalAddress(professionalAddress);

        when(gatewayUtils.buildAndPrintRequestAuditLog(
                any(AddressQueryRequest.class),
                any(GatewayDownstreamService.class)
        )).thenReturn(auditLogEvent);

        when(auditLogEvent.generateSuccess(
                anyString(),
                any(Object[].class)
        )).thenReturn(successAuditEvent);

        when(anprService.getAddressANPR(
                any(GetAddressANPRRequestBodyDto.class)
        )).thenReturn(Mono.just(anprResponse));

        when(infoCamereService.getRegistroImpreseLegalAddress(
                any(GetAddressRegistroImpreseRequestBodyDto.class)
        )).thenReturn(Mono.just(infoCamereResponse));

        StepVerifier.create(
                        physicalAddressService.retrieveSyncPhysicalAddresses(request)
                )
                .assertNext(result -> {
                    assertEquals(2, result.getAddresses().size());

                    assertTrue(
                            result.getAddresses().stream()
                                    .anyMatch(a ->
                                            GatewayDownstreamService.ANPR.name()
                                                    .equals(a.getRegistry())
                                    )
                    );

                    assertTrue(
                            result.getAddresses().stream()
                                    .anyMatch(a ->
                                            GatewayDownstreamService.REGISTRO_IMPRESE.name()
                                                    .equals(a.getRegistry())
                                    )
                    );
                })
                .verifyComplete();
    }


    @Test
    void retrieveAsyncPhysicalAddressFromAnpr_shouldPushToSqs() {
        AddressRequestBodyDto request =
                buildAsyncRequest();

        ResidentialAddressDto residentialAddress =
                new ResidentialAddressDto();
        residentialAddress.setAddress("Via Roma");

        GetAddressANPROKDto response =
                new GetAddressANPROKDto();
        response.setResidentialAddresses(
                List.of(residentialAddress)
        );

        when(anprService.getAddressANPR(
                any(GetAddressANPRRequestBodyDto.class)
        )).thenReturn(Mono.just(response));

        when(sqsService.pushToOutputQueue(
                any(CodeSqsDto.class),
                eq(CX_ID)
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        physicalAddressService.retrieveAsyncPhysicalAddressFromAnpr(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .verifyComplete();

        verify(sqsService).pushToOutputQueue(
                argThat(dto ->
                        CORRELATION_ID.equals(dto.getCorrelationId())
                                && GatewayDownstreamService.ANPR.name()
                                .equals(dto.getRegistry())
                                && dto.getPhysicalAddress() != null
                                && "Via Roma".equals(
                                dto.getPhysicalAddress().getAddress()
                        )
                ),
                eq(CX_ID)
        );
    }


    @Test
    void retrieveAsyncPhysicalAddressFromAnpr_shouldPropagateError() {
        AddressRequestBodyDto request =
                buildAsyncRequest();

        RuntimeException exception =
                new RuntimeException("ANPR error");

        when(anprService.getAddressANPR(
                any(GetAddressANPRRequestBodyDto.class)
        )).thenReturn(Mono.error(exception));

        StepVerifier.create(
                        physicalAddressService.retrieveAsyncPhysicalAddressFromAnpr(
                                CX_ID,
                                request,
                                CORRELATION_ID
                        )
                )
                .expectErrorMatches(error -> error == exception)
                .verify();

        verify(gatewayUtils).logEServiceError(
                eq(exception),
                eq("can not retrieve physical address from ANPR: {}")
        );

        verifyNoInteractions(sqsService);
    }


    @Test
    void retrieveAsyncPhysicalAddressFromRegistroImprese_shouldPushToSqs() {
        AddressRequestBodyDto request =
                buildAsyncRequest();

        GetAddressRegistroImpreseOKProfessionalAddressDto address =
                new GetAddressRegistroImpreseOKProfessionalAddressDto();

        address.setAddress("Via Impresa");

        GetAddressRegistroImpreseOKDto response =
                new GetAddressRegistroImpreseOKDto();

        response.setProfessionalAddress(address);

        when(infoCamereService.getRegistroImpreseLegalAddress(
                any(GetAddressRegistroImpreseRequestBodyDto.class)
        )).thenReturn(Mono.just(response));

        when(sqsService.pushToOutputQueue(
                any(CodeSqsDto.class),
                eq(CX_ID)
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        physicalAddressService
                                .retrieveAsyncPhysicalAddressFromRegistroImprese(
                                        CX_ID,
                                        request,
                                        CORRELATION_ID
                                )
                )
                .verifyComplete();

        verify(sqsService).pushToOutputQueue(
                argThat(dto ->
                        CORRELATION_ID.equals(dto.getCorrelationId())
                                && GatewayDownstreamService.REGISTRO_IMPRESE.name()
                                .equals(dto.getRegistry())
                                && dto.getPhysicalAddress() != null
                                && "Via Impresa".equals(
                                dto.getPhysicalAddress().getAddress()
                        )
                ),
                eq(CX_ID)
        );
    }


    @Test
    void retrieveAsyncPhysicalAddressFromRegistroImprese_shouldPropagateError() {
        AddressRequestBodyDto request =
                buildAsyncRequest();

        RuntimeException exception =
                new RuntimeException("Registro Imprese error");

        when(infoCamereService.getRegistroImpreseLegalAddress(
                any(GetAddressRegistroImpreseRequestBodyDto.class)
        )).thenReturn(Mono.error(exception));

        StepVerifier.create(
                        physicalAddressService
                                .retrieveAsyncPhysicalAddressFromRegistroImprese(
                                        CX_ID,
                                        request,
                                        CORRELATION_ID
                                )
                )
                .expectErrorMatches(error -> error == exception)
                .verify();

        verify(gatewayUtils).logEServiceError(
                eq(exception),
                eq("can not retrieve physical address from Registro Imprese: {}")
        );

        verifyNoInteractions(sqsService);
    }


    @Test
    void isAnprAddressNotFound_shouldReturnTrueFor404WithEN122() {
        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode())
                .thenReturn(HttpStatus.NOT_FOUND);

        when(exception.getResponseBodyAsString())
                .thenReturn("{\"codiceErroreAnomalia\":\"EN122\"}");

        assertTrue(
                physicalAddressService.isAnprAddressNotFound.test(exception)
        );
    }


    @Test
    void isAnprAddressNotFound_shouldReturnTrueIgnoringCase() {
        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode())
                .thenReturn(HttpStatus.NOT_FOUND);

        when(exception.getResponseBodyAsString())
                .thenReturn("{\"CODICEERROREANOMALIA\":\"en122\"}");

        assertTrue(
                physicalAddressService.isAnprAddressNotFound.test(exception)
        );
    }


    @Test
    void isAnprAddressNotFound_shouldReturnFalseForDifferentStatus() {
        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode())
                .thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        assertFalse(physicalAddressService.isAnprAddressNotFound.test(exception));
    }


    @Test
    void isAnprAddressNotFound_shouldReturnFalseForDifferentErrorCode() {
        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode())
                .thenReturn(HttpStatus.NOT_FOUND);

        when(exception.getResponseBodyAsString())
                .thenReturn("{\"codiceErroreAnomalia\":\"OTHER\"}");

        assertFalse(
                physicalAddressService.isAnprAddressNotFound.test(exception)
        );
    }


    @Test
    void isAnprAddressNotFound_shouldReturnFalseWhenBodyIsNull() {
        PnNationalRegistriesException exception =
                mock(PnNationalRegistriesException.class);

        when(exception.getStatusCode())
                .thenReturn(HttpStatus.NOT_FOUND);

        when(exception.getResponseBodyAsString())
                .thenReturn(null);

        assertFalse(
                physicalAddressService.isAnprAddressNotFound.test(exception)
        );
    }


    @Test
    void isAnprAddressNotFound_shouldReturnFalseForGenericException() {
        assertFalse(
                physicalAddressService.isAnprAddressNotFound.test(
                        new RuntimeException("error")
                )
        );
    }


    private PhysicalAddressesRequestBodyDto buildPhysicalAddressesRequest(
            RecipientType recipientType
    ) {
        RecipientAddressRequestBodyDto address =
                new RecipientAddressRequestBodyDto();

        address.setTaxId(TAX_ID);
        address.setRecIndex(0);

        address.setRecipientType(
                RecipientAddressRequestBodyDto.RecipientTypeEnum
                        .valueOf(recipientType.name())
        );

        PhysicalAddressesRequestBodyDto request =
                new PhysicalAddressesRequestBodyDto();

        request.setCorrelationId(CORRELATION_ID);
        request.setReferenceRequestDate(new Date());
        request.setAddresses(List.of(address));

        return request;
    }


    private AddressRequestBodyDto buildAsyncRequest() {
        AddressRequestBodyFilterDto filter =
                new AddressRequestBodyFilterDto();

        filter.setCorrelationId(CORRELATION_ID);
        filter.setTaxId(TAX_ID);
        filter.setReferenceRequestDate(new Date());
        filter.setDomicileType(
                AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL
        );

        AddressRequestBodyDto request =
                new AddressRequestBodyDto();

        request.setFilter(filter);

        return request;
    }


    private void mockAuditSuccess() {
        when(gatewayUtils.buildAndPrintRequestAuditLog(
                any(AddressQueryRequest.class),
                any(GatewayDownstreamService.class)
        )).thenReturn(auditLogEvent);

        when(auditLogEvent.generateSuccess(
                anyString(),
                any(Object[].class)
        )).thenReturn(successAuditEvent);
    }

    private void mockAuditFailure() {
        when(gatewayUtils.buildAndPrintRequestAuditLog(
                any(AddressQueryRequest.class),
                any(GatewayDownstreamService.class)
        )).thenReturn(auditLogEvent);

        when(auditLogEvent.generateFailure(
                anyString(),
                any(Object[].class)
        )).thenReturn(failureAuditEvent);
    }
}
