package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.converter.InfoCamereConverter;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.AddressRegistroImprese;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.InfoCamereLegalInstituionsResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.InfoCamereVerification;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.LegalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.model.inipec.InipecEvaluationResult;
import it.pagopa.pn.national.registries.repository.IniPecBatchRequestRepository;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Slf4j
@TestPropertySource(properties = {
        "pn.national-registries.ttl=0",
        "pn.national-registries.inipec.batch-request-pk-separator=~"
})
@ContextConfiguration(classes = {InfoCamereService.class})
@ExtendWith(SpringExtension.class)
class InfoCamereServiceTest {

    @Autowired
    InfoCamereService infoCamereService;

    @MockitoBean
    InfoCamereConverter infoCamereConverter;
    @MockitoBean
    InfoCamereClient infoCamereClient;
    @MockitoBean
    IniPecBatchRequestRepository batchRequestRepository;
    @MockitoBean
    NationalRegistriesConfig nationalRegistriesConfig;

    @MockitoBean
    ValidateTaxIdUtils validateTaxIdUtils;

    @BeforeEach
    void setup(){
        NationalRegistriesConfig.Inipec inipec = new NationalRegistriesConfig.Inipec();
        inipec.setBatchRequestPkSeparator("~");
        when(nationalRegistriesConfig.getInipec()).thenReturn(inipec);
    }

    @Test
    void testGetDigitalAddress() {
        GetDigitalAddressIniPECRequestBodyDto requestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto dto = new GetDigitalAddressIniPECRequestBodyFilterDto();
        dto.setCorrelationId("correlationId");
        dto.setTaxId("taxId");
        requestBodyDto.setFilter(dto);

        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("taxId");
        batchRequest.setCorrelationId("correlationId");
        when(batchRequestRepository.create(any()))
                .thenReturn(Mono.just(batchRequest));

        GetDigitalAddressIniPECOKDto getDigitalAddressIniPECOKDto = new GetDigitalAddressIniPECOKDto();
        getDigitalAddressIniPECOKDto.setCorrelationId("correlationId");
        when(infoCamereConverter.convertToGetAddressIniPecOKDto(any()))
                .thenReturn(getDigitalAddressIniPECOKDto);

        StepVerifier.create(infoCamereService.getIniPecDigitalAddress("clientId", requestBodyDto, new Date(), RecipientType.PG))
                .expectNext(getDigitalAddressIniPECOKDto)
                .verifyComplete();
    }

    @Test
    void createBatchRequestByCf_shouldPersistRecipientTypeClientIdAndReferenceRequestDate() {
        GetDigitalAddressIniPECRequestBodyDto requestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto dto = new GetDigitalAddressIniPECRequestBodyFilterDto();
        dto.setCorrelationId("correlationId");
        dto.setTaxId("taxId");
        requestBodyDto.setFilter(dto);

        Date referenceRequestDate = Date.from(LocalDateTime.of(2026, 10, 10, 12, 30).toInstant(ZoneOffset.UTC));

        when(batchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(infoCamereService.createBatchRequestByCf("clientId", requestBodyDto, "awsMessageId", referenceRequestDate, RecipientType.PF))
                .assertNext(batchRequest -> {
                    assertNotNull(batchRequest.getCreatedAt());
                    assertEquals("taxId", batchRequest.getCf());
                    assertEquals("clientId", batchRequest.getClientId());
                    assertEquals("awsMessageId", batchRequest.getAwsMessageId());
                    assertEquals(RecipientType.PF.name(), batchRequest.getRecipientType());
                    assertEquals(referenceRequestDate.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime(), batchRequest.getReferenceRequestDate());
                    assertNotNull(batchRequest.getCorrelationId());
                })
                .verifyComplete();

        verify(batchRequestRepository).create(argThat(batchRequest ->
                batchRequest.getCorrelationId().startsWith("correlationId~")
                        && "taxId".equals(batchRequest.getCf())
                        && "clientId".equals(batchRequest.getClientId())
                        && "awsMessageId".equals(batchRequest.getAwsMessageId())
                        && RecipientType.PF.name().equals(batchRequest.getRecipientType())
                        && referenceRequestDate.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime().equals(batchRequest.getReferenceRequestDate())
        ));
    }

    @Test
    void getAddress() {
        GetAddressRegistroImpreseRequestBodyDto request = new GetAddressRegistroImpreseRequestBodyDto();
        TaxIdFilterDto dto = new TaxIdFilterDto();
        dto.setTaxId("cf");
        request.setFilter(dto);

        GetAddressRegistroImpreseOKDto response = new GetAddressRegistroImpreseOKDto();
        response.setTaxId("cf");
        GetAddressRegistroImpreseOKProfessionalAddressDto professional = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        professional.setAddress("address");
        response.setProfessionalAddress(professional);

        AddressRegistroImprese addressRegistroImpreseResponse = new AddressRegistroImprese();
        addressRegistroImpreseResponse.setCf("cf");
        LegalAddress legalAddress = new LegalAddress();
        legalAddress.setToponimo("address");
        addressRegistroImpreseResponse.setIndirizzoLocalizzazione(legalAddress);

        when(infoCamereClient.getLegalAddress(any())).thenReturn(Mono.just(addressRegistroImpreseResponse));
        when(infoCamereConverter.mapToResponseOkByResponse((AddressRegistroImprese) any())).thenReturn(response);

        StepVerifier.create(infoCamereService.getRegistroImpreseLegalAddress(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void getAddressWhenNotFound() {
        GetAddressRegistroImpreseRequestBodyDto request = new GetAddressRegistroImpreseRequestBodyDto();
        TaxIdFilterDto dto = new TaxIdFilterDto();
        dto.setTaxId("cf");
        request.setFilter(dto);

        GetAddressRegistroImpreseOKDto response = new GetAddressRegistroImpreseOKDto();
        response.setTaxId("cf");
        GetAddressRegistroImpreseOKProfessionalAddressDto professional = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        response.setProfessionalAddress(professional);
        response.setDateTimeExtraction(new Date());

        AddressRegistroImprese addressRegistroImpreseResponse = new AddressRegistroImprese();
        addressRegistroImpreseResponse.setCode("err-sede");
        addressRegistroImpreseResponse.setDescription("description");
        addressRegistroImpreseResponse.setAppName("appName");
        addressRegistroImpreseResponse.setTimestamp(OffsetDateTime.now().toString());

        when(infoCamereClient.getLegalAddress(any())).thenReturn(Mono.just(addressRegistroImpreseResponse));
        when(infoCamereConverter.checkIfResponseIsInfoCamereError((AddressRegistroImprese)any())).thenReturn(true);
        when(infoCamereConverter.mapToResponseOkByRequest((GetAddressRegistroImpreseRequestBodyDto) any())).thenReturn(response);

        StepVerifier.create(infoCamereService.getRegistroImpreseLegalAddress(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void checkTaxIdAndVatNumber() {
        InfoCamereVerification response = new InfoCamereVerification();
        response.setCfPersona("taxId");
        response.setCfImpresa("vatNumber");

        InfoCamereLegalRequestBodyDto body = new InfoCamereLegalRequestBodyDto();
        InfoCamereLegalRequestBodyFilterDto dto = new InfoCamereLegalRequestBodyFilterDto();
        dto.setTaxId("taxId");
        dto.setVatNumber("vatNumber");
        body.setFilter(dto);

        InfoCamereLegalOKDto infoCamereLegalOKDto = new InfoCamereLegalOKDto();
        infoCamereLegalOKDto.setTaxId("taxId");
        infoCamereLegalOKDto.setVatNumber("vatNumber");
        infoCamereLegalOKDto.setVerificationResult(true);


        when(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(body.getFilter())).thenReturn(Mono.just(response));
        when(infoCamereConverter.infoCamereResponseToDtoByResponse(response)).thenReturn(infoCamereLegalOKDto);

        StepVerifier.create(infoCamereService.checkTaxIdAndVatNumber(body))
                .expectNext(infoCamereLegalOKDto)
                .verifyComplete();
    }

    @Test
    void checkTaxIdAndVatNumberWhenNotFound() {
        InfoCamereVerification response = new InfoCamereVerification();
        response.setCode("err-lrpunt");
        response.setDescription("description");
        response.setAppName("appName");
        response.setTimestamp(OffsetDateTime.now().toString());

        InfoCamereLegalRequestBodyDto body = new InfoCamereLegalRequestBodyDto();
        InfoCamereLegalRequestBodyFilterDto dto = new InfoCamereLegalRequestBodyFilterDto();
        dto.setTaxId("taxIdTest");
        dto.setVatNumber("vatNumber");
        body.setFilter(dto);

        InfoCamereLegalOKDto infoCamereLegalOKDto = new InfoCamereLegalOKDto();
        infoCamereLegalOKDto.setTaxId("taxIdTest");
        infoCamereLegalOKDto.setVatNumber("vatNumber");
        infoCamereLegalOKDto.setVerificationResult(false);


        when(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(body.getFilter())).thenReturn(Mono.just(response));
        when(infoCamereConverter.checkIfResponseIsInfoCamereError((InfoCamereVerification) any())).thenReturn(true);
        when(infoCamereConverter.infoCamereResponseToDtoByRequest(body)).thenReturn(infoCamereLegalOKDto);

        StepVerifier.create(infoCamereService.checkTaxIdAndVatNumber(body))
                .expectNext(infoCamereLegalOKDto)
                .verifyComplete();
    }

    @Test
    void getInstitutions() {
        InfoCamereLegalInstitutionsRequestBodyDto request = new InfoCamereLegalInstitutionsRequestBodyDto();
        TaxIdFilterDto dto = new TaxIdFilterDto();
        dto.setTaxId("cf");
        request.setFilter(dto);

        InfoCamereLegalInstitutionsOKDto response = new InfoCamereLegalInstitutionsOKDto();
        response.setLegalTaxId("cf");

        InfoCamereLegalInstituionsResponse infoCamereLegalInstituionsResponse = new InfoCamereLegalInstituionsResponse();
        infoCamereLegalInstituionsResponse.setCfPersona("cf");


        when(infoCamereClient.getLegalInstitutions(any())).thenReturn(Mono.just(infoCamereLegalInstituionsResponse));
        when(infoCamereConverter.mapToResponseOkByResponse((InfoCamereLegalInstituionsResponse) any())).thenReturn(response);

        StepVerifier.create(infoCamereService.getLegalInstitutions(request))
                .expectNext(response)
                .verifyComplete();
    }

    // ---- evaluateInipecStatoImpresa ----

    @Test
    void evaluateInipecStatoImpresa_shouldReturnRetryWhenStatoImpresaIsER() {
        Pec pec = new Pec();
        pec.setStatoImpresa(Pec.StatoImpresaEnum.ER);

        InipecEvaluationResult result = infoCamereService.evaluateInipecStatoImpresa("correlationId", "12345678901", "PG", pec);

        assertInstanceOf(InipecEvaluationResult.Retry.class, result);
        verifyNoInteractions(infoCamereConverter);
    }

    @ParameterizedTest
    @EnumSource(value = Pec.StatoImpresaEnum.class, names = {"ND", "NF"})
    void evaluateInipecStatoImpresa_shouldReturnNotFoundWhenStatoImpresaIsNdOrNf(Pec.StatoImpresaEnum stato) {
        Pec pec = new Pec();
        pec.setStatoImpresa(stato);
        pec.setPecImpresa("impresa@pec.it");

        InipecEvaluationResult result = infoCamereService.evaluateInipecStatoImpresa("correlationId", "12345678901", "PG", pec);

        assertInstanceOf(InipecEvaluationResult.NotFound.class, result);
        verifyNoInteractions(infoCamereConverter);
    }

    @Test
    void evaluateInipecStatoImpresa_shouldReturnFoundWhenStatoImpresaIsNullAndPecIsValid() {
        Pec pec = new Pec();
        CodeSqsDto codeSqsDto = codeSqsDtoWithAddresses("impresa@pec.it");
        when(infoCamereConverter.convertResponsePecToCodeSqsDto("correlationId", pec)).thenReturn(codeSqsDto);

        InipecEvaluationResult result = infoCamereService.evaluateInipecStatoImpresa("correlationId", "12345678901", "PG", pec);

        InipecEvaluationResult.Found found = assertInstanceOf(InipecEvaluationResult.Found.class, result);
        assertSame(codeSqsDto, found.codeSqsDto());
        assertEquals(1, found.codeSqsDto().getDigitalAddress().size());
    }

    @Test
    void evaluateInipecStatoImpresa_shouldRemoveInvalidEmailsAndKeepValidOnes() {
        Pec pec = new Pec();
        CodeSqsDto codeSqsDto = codeSqsDtoWithAddresses("not-an-email", "valid@pec.it");
        when(infoCamereConverter.convertResponsePecToCodeSqsDto("correlationId", pec)).thenReturn(codeSqsDto);

        InipecEvaluationResult result = infoCamereService.evaluateInipecStatoImpresa("correlationId", "12345678901", "PG", pec);

        InipecEvaluationResult.Found found = assertInstanceOf(InipecEvaluationResult.Found.class, result);
        assertEquals(1, found.codeSqsDto().getDigitalAddress().size());
        assertEquals("valid@pec.it", found.codeSqsDto().getDigitalAddress().get(0).getAddress());
    }

    @Test
    void evaluateInipecStatoImpresa_shouldReturnNotFoundWhenAllEmailsAreInvalid() {
        Pec pec = new Pec();
        when(infoCamereConverter.convertResponsePecToCodeSqsDto("correlationId", pec))
                .thenReturn(codeSqsDtoWithAddresses("not-an-email", "also invalid"));

        InipecEvaluationResult result = infoCamereService.evaluateInipecStatoImpresa("correlationId", "12345678901", "PG", pec);

        assertInstanceOf(InipecEvaluationResult.NotFound.class, result);
    }

    @Test
    void evaluateInipecStatoImpresa_shouldReturnNotFoundWhenNoPecIsPresent() {
        Pec pec = new Pec();
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setDigitalAddress(null);
        when(infoCamereConverter.convertResponsePecToCodeSqsDto("correlationId", pec)).thenReturn(codeSqsDto);

        InipecEvaluationResult result = infoCamereService.evaluateInipecStatoImpresa("correlationId", "RSSMRA80A01H501U", "PF", pec);

        assertInstanceOf(InipecEvaluationResult.NotFound.class, result);
    }

    // ---- callEService ----

    @Test
    void callEService_shouldReturnClientResponse() {
        IniPecBatchRequest request = new IniPecBatchRequest();
        request.setElencoCf(List.of(new IniPecBatchRequest.IniPecCf()));
        IniPecBatchResponse response = new IniPecBatchResponse();
        when(infoCamereClient.callEServiceRequestId(request)).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereService.callEService(request, "batchId"))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void callEService_shouldPropagateClientError() {
        IniPecBatchRequest request = new IniPecBatchRequest();
        request.setElencoCf(List.of(new IniPecBatchRequest.IniPecCf()));
        RuntimeException error = new RuntimeException("infocamere down");
        when(infoCamereClient.callEServiceRequestId(request)).thenReturn(Mono.error(error));

        StepVerifier.create(infoCamereService.callEService(request, "batchId"))
                .expectErrorMatches(e -> e == error)
                .verify();
    }

    // ---- callIniPecEService ----

    @Test
    void callIniPecEService_shouldReturnResponseWhenNotAnInfoCamereError() {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(new Pec()));
        when(infoCamereClient.callEServiceRequestPec("pollingId")).thenReturn(Mono.just(response));
        when(infoCamereConverter.checkIfResponseIsInfoCamereError((IniPecPollingResponse) any())).thenReturn(false);

        StepVerifier.create(infoCamereService.callIniPecEService("batchId", "pollingId"))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void callIniPecEService_shouldFailWithNotFoundWhenResponseIsInfoCamereError() {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setDescription("List PEC in progress");
        when(infoCamereClient.callEServiceRequestPec("pollingId")).thenReturn(Mono.just(response));
        when(infoCamereConverter.checkIfResponseIsInfoCamereError((IniPecPollingResponse) any())).thenReturn(true);

        StepVerifier.create(infoCamereService.callIniPecEService("batchId", "pollingId"))
                .expectErrorSatisfies(e -> {
                    PnNationalRegistriesException exception = assertInstanceOf(PnNationalRegistriesException.class, e);
                    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
                    assertEquals("List PEC in progress", exception.getMessage());
                })
                .verify();
    }

    @Test
    void callIniPecEService_shouldPropagateClientError() {
        RuntimeException error = new RuntimeException("infocamere down");
        when(infoCamereClient.callEServiceRequestPec("pollingId")).thenReturn(Mono.error(error));

        StepVerifier.create(infoCamereService.callIniPecEService("batchId", "pollingId"))
                .expectErrorMatches(e -> e == error)
                .verify();
    }

    // ---- ttl, separatore e ramo errore dei servizi sincroni ----

    @Test
    void createBatchRequestByCf_shouldInitializeBatchRequestWithDefaultsAndNullReferenceRequestDate() {
        GetDigitalAddressIniPECRequestBodyDto requestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto filter = new GetDigitalAddressIniPECRequestBodyFilterDto();
        filter.setCorrelationId("correlationId");
        filter.setTaxId("12345678901");
        requestBodyDto.setFilter(filter);
        when(batchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(infoCamereService.createBatchRequestByCf("clientId", requestBodyDto, null, null, RecipientType.PG))
                .assertNext(batchRequest -> {
                    assertEquals(BatchStatus.NO_BATCH_ID.getValue(), batchRequest.getBatchId());
                    assertEquals(BatchStatus.NOT_WORKED.getValue(), batchRequest.getStatus());
                    assertEquals(0, batchRequest.getRetry());
                    assertNull(batchRequest.getReferenceRequestDate());
                    assertNull(batchRequest.getAwsMessageId());
                    assertEquals("correlationId~" + batchRequest.getCreatedAt(), batchRequest.getCorrelationId());
                    // ttl configurato a 0: coincide con createdAt in epoch seconds
                    assertEquals(batchRequest.getCreatedAt().toEpochSecond(ZoneOffset.UTC), batchRequest.getTtl());
                })
                .verifyComplete();
    }

    @Test
    void getIniPecDigitalAddress_shouldPropagateRepositoryError() {
        GetDigitalAddressIniPECRequestBodyDto requestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto filter = new GetDigitalAddressIniPECRequestBodyFilterDto();
        filter.setCorrelationId("correlationId");
        filter.setTaxId("12345678901");
        requestBodyDto.setFilter(filter);
        RuntimeException error = new RuntimeException("dynamo down");
        when(batchRequestRepository.create(any())).thenReturn(Mono.error(error));

        StepVerifier.create(infoCamereService.getIniPecDigitalAddress("clientId", requestBodyDto, new Date(), RecipientType.PG))
                .expectErrorMatches(e -> e == error)
                .verify();

        verify(infoCamereConverter, never()).convertToGetAddressIniPecOKDto(any());
        verify(validateTaxIdUtils).validateTaxId(eq("12345678901"), anyString(), eq(false));
    }

    @Test
    void getRegistroImpreseLegalAddress_shouldValidateTaxIdAndPropagateClientError() {
        GetAddressRegistroImpreseRequestBodyDto request = new GetAddressRegistroImpreseRequestBodyDto();
        TaxIdFilterDto filter = new TaxIdFilterDto();
        filter.setTaxId("cf");
        request.setFilter(filter);
        RuntimeException error = new RuntimeException("infocamere down");
        when(infoCamereClient.getLegalAddress("cf")).thenReturn(Mono.error(error));

        StepVerifier.create(infoCamereService.getRegistroImpreseLegalAddress(request))
                .expectErrorMatches(e -> e == error)
                .verify();

        verify(validateTaxIdUtils).validateTaxId(eq("cf"), anyString(), eq(false));
    }

    @Test
    void getLegalInstitutions_shouldPropagateClientError() {
        InfoCamereLegalInstitutionsRequestBodyDto request = new InfoCamereLegalInstitutionsRequestBodyDto();
        TaxIdFilterDto filter = new TaxIdFilterDto();
        filter.setTaxId("cf");
        request.setFilter(filter);
        RuntimeException error = new RuntimeException("infocamere down");
        when(infoCamereClient.getLegalInstitutions(any())).thenReturn(Mono.error(error));

        StepVerifier.create(infoCamereService.getLegalInstitutions(request))
                .expectErrorMatches(e -> e == error)
                .verify();

        verify(validateTaxIdUtils).validateTaxId(eq("cf"), anyString(), eq(false));
    }

    @Test
    void checkTaxIdAndVatNumber_shouldPropagateClientError() {
        InfoCamereLegalRequestBodyDto body = new InfoCamereLegalRequestBodyDto();
        InfoCamereLegalRequestBodyFilterDto filter = new InfoCamereLegalRequestBodyFilterDto();
        filter.setTaxId("taxId");
        filter.setVatNumber("vatNumber");
        body.setFilter(filter);
        RuntimeException error = new RuntimeException("infocamere down");
        when(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(filter)).thenReturn(Mono.error(error));

        StepVerifier.create(infoCamereService.checkTaxIdAndVatNumber(body))
                .expectErrorMatches(e -> e == error)
                .verify();

        verify(validateTaxIdUtils).validateTaxId(eq("taxId"), anyString(), eq(false));
    }

    /**
     * N17 - un timeout del registro deve essere propagato come guasto tecnico, non trasformato in lista vuota.
     */
    @Test
    void getLegalInstitutions_timeout_shouldBePropagatedAndNotConvertedToEmptyResult() {
        InfoCamereLegalInstitutionsRequestBodyDto request = new InfoCamereLegalInstitutionsRequestBodyDto();
        TaxIdFilterDto filter = new TaxIdFilterDto();
        filter.setTaxId("cf");
        request.setFilter(filter);
        TimeoutException timeout = new TimeoutException("infocamere timeout");
        when(infoCamereClient.getLegalInstitutions(any())).thenReturn(Mono.error(timeout));

        StepVerifier.create(infoCamereService.getLegalInstitutions(request))
                .expectErrorMatches(error -> error == timeout)
                .verify();

        verify(infoCamereConverter, never()).mapToResponseOkByResponse((InfoCamereLegalInstituionsResponse) any());
    }

    private static CodeSqsDto codeSqsDtoWithAddresses(String... addresses) {
        List<DigitalAddress> digitalAddresses = new ArrayList<>();
        for (String address : addresses) {
            DigitalAddress digitalAddress = new DigitalAddress();
            digitalAddress.setAddress(address);
            digitalAddress.setType("PEC");
            digitalAddress.setRecipient("IMPRESA");
            digitalAddresses.add(digitalAddress);
        }
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setDigitalAddress(digitalAddresses);
        return codeSqsDto;
    }
}

