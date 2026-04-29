package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.LegalRepresentationApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.LegalRepresentativeApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.PecApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.SedeApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.InfoCamereLegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfoCamereClientTest {

    @Mock
    LegalRepresentationApi legalRepresentationApi;

    @Mock
    LegalRepresentativeApi legalRepresentativeApi;

    @Mock
    PecApi pecApi;

    @Mock
    SedeApi sedeApi;

    @Mock
    AccessTokenExpiringMap accessTokenExpiringMap;

    @Mock
    ObjectMapper mapper;

    @InjectMocks
    private InfoCamereClient infoCamereClient;

    @Mock
    NationalRegistriesConfig nationalRegistriesConfig;

    ApiClient apiClient;

    @BeforeEach
    void setUp() {
        apiClient = new ApiClient();
        apiClient.setBasePath("basePath");
    }


    public WebClientResponseException buildException() {
        HttpHeaders headers = mock(HttpHeaders.class);
        byte[] testByteArray = new byte[0];
        String test = "test";
        return new WebClientResponseException(test, 500, test, headers, testByteArray, Charset.defaultCharset());
    }

    public WebClientResponseException buildExceptionUnauthorized() {
        HttpHeaders headers = mock(HttpHeaders.class);
        byte[] testByteArray = new byte[0];
        String test = "test";
        return new WebClientResponseException(test, 401, test, headers, testByteArray, Charset.defaultCharset());
    }

    @Test
    void testGetLegalInstitutions() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        CheckTaxIdRequestBodyFilterDto checkTaxIdRequestBodyFilterDto = new CheckTaxIdRequestBodyFilterDto();
        checkTaxIdRequestBodyFilterDto.setTaxId("taxId");

        InfoCamereLegalInstituionsResponse infoCamereLegalInstituionsResponse = new InfoCamereLegalInstituionsResponse();


        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(legalRepresentativeApi.getApiClient()).thenReturn(apiClient);
        when(legalRepresentativeApi.getLegalRepresentativeListByTaxIdWithHttpInfo(any(), any(), any()))
                .thenReturn(Mono.just(ResponseEntity.ok(infoCamereLegalInstituionsResponse)));


        StepVerifier.create(infoCamereClient.getLegalInstitutions(checkTaxIdRequestBodyFilterDto))
                .expectNext(infoCamereLegalInstituionsResponse)
                .verifyComplete();
    }


    @Test
    void testCallEServiceRequestId() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        IniPecBatchRequest.IniPecCf iniPecCf = new IniPecBatchRequest.IniPecCf();
        iniPecCf.setCf("taxId");
        IniPecBatchRequest request = new IniPecBatchRequest();
        request.setDataOraRichiesta(LocalDateTime.now().toString());
        request.setElencoCf(List.of(iniPecCf));

        IniPecBatchResponse iniPecBatchResponse = new IniPecBatchResponse();
        iniPecBatchResponse.setIdentificativoRichiesta("correlationId");
        iniPecBatchResponse.setDataOraRichiesta(OffsetDateTime.now().toString());

        String requestJson = "requestJson";
        try {
            when(mapper.writeValueAsString(request)).thenReturn(requestJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");
        when(pecApi.getApiClient()).thenReturn(apiClient);
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(pecApi.callRichiestaElencoPecWithHttpInfo(any(), any(), any())).thenReturn(Mono.just(ResponseEntity.ok(iniPecBatchResponse)));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(request)).expectNext(iniPecBatchResponse).verifyComplete();
    }

    @Test
    void testCallEServiceRequestIdWebException() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        IniPecBatchRequest.IniPecCf iniPecCf = new IniPecBatchRequest.IniPecCf();
        iniPecCf.setCf("taxId");
        IniPecBatchRequest request = new IniPecBatchRequest();
        request.setDataOraRichiesta(LocalDateTime.now().toString());
        request.setElencoCf(List.of(iniPecCf));

        String requestJson = "requestJson";
        try {
            when(mapper.writeValueAsString(request)).thenReturn(requestJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        when(pecApi.getApiClient()).thenReturn(apiClient);
        WebClientResponseException ex = buildException();
        when(pecApi.callRichiestaElencoPecWithHttpInfo(any(), any(), any()))
                .thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testCallEServiceRequestPec() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        String request = "correlationId";
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setIdentificativoRichiesta("correlationId");
        when(pecApi.getApiClient()).thenReturn(apiClient);
        when(pecApi.callGetElencoPecWithHttpInfo(any(), any(), any())).thenReturn(Mono.just(ResponseEntity.ok(response)));
        StepVerifier.create(infoCamereClient.callEServiceRequestPec(request)).expectNext(response).verifyComplete();
    }

    @Test
    void testCallEServiceRequestPecWebException() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        String request = "correlationId";
        when(pecApi.getApiClient()).thenReturn(apiClient);
        WebClientResponseException ex = buildException();
        when(pecApi.callGetElencoPecWithHttpInfo(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestPec(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testGetLegalAddress() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        String request = "taxId";
        AddressRegistroImprese response = new AddressRegistroImprese();
        response.setIndirizzoLocalizzazione(new LegalAddress());
        response.setCf("taxId");

        String jws = "jws";
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials(jws);
        when(sedeApi.getApiClient()).thenReturn(apiClient);
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(sedeApi.getAddressByTaxIdWithHttpInfo(anyString(), any(), any())).thenReturn(Mono.just(ResponseEntity.ok(response)));

        StepVerifier.create(infoCamereClient.getLegalAddress(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testGetLegalAddressWebException() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        String request = "taxId";

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");
        when(sedeApi.getApiClient()).thenReturn(apiClient);
        WebClientResponseException ex = buildException();
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(sedeApi.getAddressByTaxIdWithHttpInfo(anyString(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.getLegalAddress(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }
    @Test
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.UNAUTHORIZED.value(), "statusText", HttpHeaders.EMPTY, null, null);
        assertTrue(infoCamereClient.shouldRetry(webClientResponseException));
    }

    @Test
    void shouldRetryWhenNotWebClientResponseExceptionThenReturnFalse() {
        assertFalse(infoCamereClient.shouldRetry(new Exception()));
    }

    @Test
    void testCheckTaxIdAndVatNumberInfoCamere() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);

        InfoCamereLegalRequestBodyFilterDto filterDto = new InfoCamereLegalRequestBodyFilterDto();
        filterDto.setVatNumber("vatNumber");
        filterDto.setTaxId("taxId");

        InfoCamereVerification response = new InfoCamereVerification();
        response.setCfPersona("taxId");

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");
        when(legalRepresentationApi.getApiClient()).thenReturn(apiClient);
        when(accessTokenExpiringMap.getInfoCamereToken(any()))
                .thenReturn(Mono.just(accessTokenCacheEntry));
        when(legalRepresentationApi.checkTaxIdForLegalRepresentationWithHttpInfo(anyString(), anyString(), any(), any()))
                .thenReturn(Mono.just(ResponseEntity.ok(response)));

        StepVerifier.create(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(filterDto))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testCheckTaxIdAndVatNumberInfoCamereWebClient() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);

        InfoCamereLegalRequestBodyFilterDto filterDto = new InfoCamereLegalRequestBodyFilterDto();
        filterDto.setTaxId("taxId");
        filterDto.setVatNumber("vatNumber");

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");
        when(legalRepresentationApi.getApiClient()).thenReturn(apiClient);
        WebClientResponseException ex = buildException();
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(legalRepresentationApi.checkTaxIdForLegalRepresentationWithHttpInfo(anyString(), anyString(), any(), any()))
                .thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(filterDto))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testCallEServiceRequestIdExceptionOnRetry() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        WebClientResponseException ex = buildExceptionUnauthorized();
        when(pecApi.getApiClient()).thenReturn(apiClient);
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(pecApi.callRichiestaElencoPecWithHttpInfo(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(any()))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void testCallEServiceRequestPecExceptionOnRetry() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        WebClientResponseException ex = buildExceptionUnauthorized();
        when(pecApi.getApiClient()).thenReturn(apiClient);
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(pecApi.callGetElencoPecWithHttpInfo(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestPec(any()))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void testGetLegalAddressExceptionOnRetry() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        WebClientResponseException ex = buildExceptionUnauthorized();
        when(sedeApi.getApiClient()).thenReturn(apiClient);
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(sedeApi.getAddressByTaxIdWithHttpInfo(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.getLegalAddress(any()))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void testGetLegalInstitutionsExceptionOnRetry() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        WebClientResponseException ex = buildExceptionUnauthorized();
        CheckTaxIdRequestBodyFilterDto checkTaxIdRequestBodyFilterDto = new CheckTaxIdRequestBodyFilterDto();
        checkTaxIdRequestBodyFilterDto.setTaxId("taxId");
        when(legalRepresentativeApi.getApiClient()).thenReturn(apiClient);
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(legalRepresentativeApi.getLegalRepresentativeListByTaxIdWithHttpInfo(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.getLegalInstitutions(checkTaxIdRequestBodyFilterDto))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void testCheckTaxIdAndVatNumberInfoCamereExceptionOnRetry() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        WebClientResponseException ex = buildExceptionUnauthorized();
        InfoCamereLegalRequestBodyFilterDto filterDto = new InfoCamereLegalRequestBodyFilterDto();
        filterDto.setVatNumber("vatNumber");
        filterDto.setTaxId("taxId");
        when(legalRepresentationApi.getApiClient()).thenReturn(apiClient);
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(legalRepresentationApi.checkTaxIdForLegalRepresentationWithHttpInfo(any(), any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(filterDto))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void convertToJsonException() {
        IniPecBatchRequest.IniPecCf iniPecCf = new IniPecBatchRequest.IniPecCf();
        iniPecCf.setCf("taxId");
        IniPecBatchRequest request = new IniPecBatchRequest();
        request.setDataOraRichiesta(LocalDateTime.now().toString());
        request.setElencoCf(List.of(iniPecCf));

        try {
            when(mapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        assertThrows(PnInternalException.class, () -> infoCamereClient.callEServiceRequestId(request));
    }
}