package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
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
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

@ExtendWith(SpringExtension.class)
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

    final String clientId = "tezt_clientId";

    @BeforeEach
    public void setup() {
        ApiClient apiClient = mock(ApiClient.class);
        legalRepresentationApi = mock(LegalRepresentationApi.class);
        legalRepresentativeApi = mock(LegalRepresentativeApi.class);
        pecApi = mock(PecApi.class);
        sedeApi = mock(SedeApi.class);

        doNothing().when(apiClient).setBearerToken(anyString());
        when(apiClient.addDefaultHeader(anyString(), anyString())).thenReturn(apiClient);

        when(legalRepresentationApi.getApiClient()).thenReturn(apiClient);
        when(legalRepresentativeApi.getApiClient()).thenReturn(apiClient);
        when(pecApi.getApiClient()).thenReturn(apiClient);
        when(sedeApi.getApiClient()).thenReturn(apiClient);
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
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);

        CheckTaxIdRequestBodyFilterDto checkTaxIdRequestBodyFilterDto = new CheckTaxIdRequestBodyFilterDto();
        checkTaxIdRequestBodyFilterDto.setTaxId("taxId");

        InfoCamereLegalInstituionsResponse infoCamereLegalInstituionsResponse = new InfoCamereLegalInstituionsResponse();


        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));

        when(legalRepresentativeApi.getLegalRepresentativeListByTaxId(any(), any(), any()))
                .thenReturn(Mono.just(infoCamereLegalInstituionsResponse));


        StepVerifier.create(infoCamereClient.getLegalInstitutions(checkTaxIdRequestBodyFilterDto))
                .expectNext(infoCamereLegalInstituionsResponse)
                .verifyComplete();
    }


    @Test
    void testCallEServiceRequestId() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(
                clientId,
                accessTokenExpiringMap,
                mapper,
                legalRepresentationApi,
                legalRepresentativeApi,
                pecApi,
                sedeApi);

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

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(pecApi.callRichiestaElencoPec(any(), any(), any())).thenReturn(Mono.just(iniPecBatchResponse));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(request)).expectNext(iniPecBatchResponse).verifyComplete();
    }

    @Test
    void testCallEServiceRequestIdWebException() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);

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

        WebClientResponseException ex = buildException();
        when(pecApi.callRichiestaElencoPec(any(), any(), any()))
                .thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testCallEServiceRequestPec() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        String request = "correlationId";
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setIdentificativoRichiesta("correlationId");

        when(pecApi.callGetElencoPec(any(), any(), any())).thenReturn(Mono.just(response ));
        StepVerifier.create(infoCamereClient.callEServiceRequestPec(request)).expectNext(response).verifyComplete();
    }

    @Test
    void testCallEServiceRequestPecWebException() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        String request = "correlationId";

        WebClientResponseException ex = buildException();
        when(pecApi.callGetElencoPec(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestPec(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testGetLegalAddress() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);

        String request = "taxId";
        AddressRegistroImprese response = new AddressRegistroImprese();
        response.setIndirizzoLocalizzazione(new LegalAddress());
        response.setCf("taxId");

        String jws = "jws";
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials(jws);

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(sedeApi.getAddressByTaxId(anyString(), any(), any())).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereClient.getLegalAddress(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testGetLegalAddressWebException() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);

        String request = "taxId";

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        WebClientResponseException ex = buildException();
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(sedeApi.getAddressByTaxId(anyString(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.getLegalAddress(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }
    @Test
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);

        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.UNAUTHORIZED.value(), "statusText", HttpHeaders.EMPTY, null, null);
        assertTrue(infoCamereClient.shouldRetry(webClientResponseException));
    }

    @Test
    void shouldRetryWhenNotWebClientResponseExceptionThenReturnFalse() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);

        assertFalse(infoCamereClient.shouldRetry(new Exception()));
    }

    @Test
    void testCheckTaxIdAndVatNumberInfoCamere() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);

        InfoCamereLegalRequestBodyFilterDto filterDto = new InfoCamereLegalRequestBodyFilterDto();
        filterDto.setVatNumber("vatNumber");
        filterDto.setTaxId("taxId");

        InfoCamereVerification response = new InfoCamereVerification();
        response.setCfPersona("taxId");

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any()))
                .thenReturn(Mono.just(accessTokenCacheEntry));
        when(legalRepresentationApi.checkTaxIdForLegalRepresentation(anyString(), anyString(), any(), any()))
                .thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(filterDto))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testCheckTaxIdAndVatNumberInfoCamereWebClient() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);

        InfoCamereLegalRequestBodyFilterDto filterDto = new InfoCamereLegalRequestBodyFilterDto();
        filterDto.setTaxId("taxId");
        filterDto.setVatNumber("vatNumber");

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        WebClientResponseException ex = buildException();
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(legalRepresentationApi.checkTaxIdForLegalRepresentation(anyString(), anyString(), any(), any()))
                .thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(filterDto))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testCallEServiceRequestIdExceptionOnRetry() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);
        WebClientResponseException ex = buildExceptionUnauthorized();

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(pecApi.callRichiestaElencoPec(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(any()))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void testCallEServiceRequestPecExceptionOnRetry() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);
        WebClientResponseException ex = buildExceptionUnauthorized();

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(pecApi.callGetElencoPec(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.callEServiceRequestPec(any()))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void testGetLegalAddressExceptionOnRetry() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);
        WebClientResponseException ex = buildExceptionUnauthorized();

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(sedeApi.getAddressByTaxId(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.getLegalAddress(any()))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void testGetLegalInstitutionsExceptionOnRetry() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);
        WebClientResponseException ex = buildExceptionUnauthorized();
        CheckTaxIdRequestBodyFilterDto checkTaxIdRequestBodyFilterDto = new CheckTaxIdRequestBodyFilterDto();
        checkTaxIdRequestBodyFilterDto.setTaxId("taxId");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(legalRepresentativeApi.getLegalRepresentativeListByTaxId(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.getLegalInstitutions(checkTaxIdRequestBodyFilterDto))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void testCheckTaxIdAndVatNumberInfoCamereExceptionOnRetry() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);
        WebClientResponseException ex = buildExceptionUnauthorized();
        InfoCamereLegalRequestBodyFilterDto filterDto = new InfoCamereLegalRequestBodyFilterDto();
        filterDto.setVatNumber("vatNumber");
        filterDto.setTaxId("taxId");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(mock(AccessTokenCacheEntry.class)));
        when(legalRepresentationApi.checkTaxIdForLegalRepresentation(any(), any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(filterDto))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void convertToJsonException() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(clientId, accessTokenExpiringMap, mapper, legalRepresentationApi, legalRepresentativeApi, pecApi, sedeApi);
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