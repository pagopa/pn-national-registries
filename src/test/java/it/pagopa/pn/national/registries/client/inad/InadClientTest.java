package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.inad.InadSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.api.ApiEstrazioniMultipleApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.api.ApiEstrazioniPuntualiApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.RequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseStatusListDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.TokenType;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class InadClientTest {

    private static final String TOKEN = "test-token";
    private static final String PURPOSE_ID = "purposeId";
    private static final String SECRET_NAME = "secret";

    @MockitoBean
    AccessTokenExpiringMap accessTokenExpiringMap;

    @MockitoBean
    ApiEstrazioniPuntualiApi apiEstrazioniPuntualiApi;

    @MockitoBean
    ApiEstrazioniMultipleApi apiEstrazioniMultipleApi;

    @MockitoBean
    InadSecretConfig inadSecretConfig;

    @MockitoBean
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @Test
    void callEService() {
        InadClient inadClient = buildClient();

        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale("cf");
        response.setSince(new Date());

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniPuntualiApi.getApiClient()).thenReturn(apiClient);
        when(apiEstrazioniPuntualiApi.recuperoDomicilioDigitale(anyString(), anyString())).thenReturn(Mono.just(response));

        StepVerifier.create(inadClient.callEService("cf", "test"))
                .expectNext(response)
                .verifyComplete();

        verify(apiClient).setBearerToken(TOKEN);
    }

    @Test
    void callEServiceTokenError() {
        InadClient inadClient = buildClient();

        WebClientResponseException exception = webClientException(HttpStatus.NOT_FOUND);

        mockSecret();

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), eq(false))).thenReturn(Mono.error(exception));

        StepVerifier.create(inadClient.callEService("cf", "test"))
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void callEServiceNonRetryableError() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniPuntualiApi.getApiClient()).thenReturn(apiClient);
        when(apiEstrazioniPuntualiApi.recuperoDomicilioDigitale(anyString(), anyString())).thenReturn(Mono.error(webClientException(HttpStatus.BAD_REQUEST)));

        StepVerifier.create(inadClient.callEService("cf", "test"))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void callEServiceUnauthorizedRetryExhausted() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniPuntualiApi.getApiClient()).thenReturn(apiClient);

        when(apiEstrazioniPuntualiApi.recuperoDomicilioDigitale(any(), any())).thenReturn(Mono.error(webClientException(HttpStatus.UNAUTHORIZED)));

        StepVerifier.create(inadClient.callEService("cf", "test"))
                .expectError(PnInternalException.class)
                .verify();

        // prima chiamata + un retry
        verify(apiEstrazioniPuntualiApi, times(2)).recuperoDomicilioDigitale(any(), any());
    }

    @Test
    void callMassiveEServiceSuccess() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        RequestListDigitalAddress request = new RequestListDigitalAddress();
        ResponseRequestListDigitalAddress response = new ResponseRequestListDigitalAddress();
        when(apiEstrazioniMultipleApi.richiestaElencoDomiciliDigitaliWithHttpInfo(same(request))).thenReturn(Mono.just(ResponseEntity.ok(response)));

        StepVerifier.create(inadClient.callMassiveEService(request))
                .expectNext(response)
                .verifyComplete();

        verify(apiClient).setBearerToken(TOKEN);
        verify(apiEstrazioniMultipleApi).richiestaElencoDomiciliDigitaliWithHttpInfo(request);
    }

    @Test
    void callMassiveEServiceBadRequest() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        RequestListDigitalAddress request = new RequestListDigitalAddress();
        when(apiEstrazioniMultipleApi.richiestaElencoDomiciliDigitaliWithHttpInfo(same(request)))
                .thenReturn(
                        Mono.error(
                                webClientException(HttpStatus.BAD_REQUEST)
                        )
                );

        StepVerifier.create(inadClient.callMassiveEService(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void callMassiveEServiceUnauthorizedRetryExhausted() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        RequestListDigitalAddress request = new RequestListDigitalAddress();
        when(apiEstrazioniMultipleApi.richiestaElencoDomiciliDigitaliWithHttpInfo(same(request)))
                .thenReturn(
                        Mono.error(
                                webClientException(HttpStatus.UNAUTHORIZED)
                        )
                );

        StepVerifier.create(inadClient.callMassiveEService(request))
                .expectError(PnInternalException.class)
                .verify();

        verify(apiEstrazioniMultipleApi, times(2)).richiestaElencoDomiciliDigitaliWithHttpInfo(request);
    }

    @Test
    void callMassiveEServiceGenericError() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        RequestListDigitalAddress request = new RequestListDigitalAddress();
        RuntimeException exception = new RuntimeException("generic error");
        when(apiEstrazioniMultipleApi.richiestaElencoDomiciliDigitaliWithHttpInfo(same(request)))
                .thenReturn(Mono.error(exception));

        StepVerifier.create(inadClient.callMassiveEService(request))
                .expectErrorMatches(ex -> ex == exception)
                .verify();
    }

    @Test
    void checkMassiveRequestStatusSuccess() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        ResponseStatusListDigitalAddress response = new ResponseStatusListDigitalAddress();
        when(apiEstrazioniMultipleApi.verificaStatoRichiestaElencoDomiciliDigitali("polling-id")).thenReturn(Mono.just(response));

        StepVerifier.create(inadClient.checkMassiveRequestStatus("polling-id"))
                .expectNext(response)
                .verifyComplete();

        verify(apiClient).setBearerToken(TOKEN);
    }

    @Test
    void checkMassiveRequestStatusBadRequest() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        when(apiEstrazioniMultipleApi.verificaStatoRichiestaElencoDomiciliDigitali("polling-id"))
                .thenReturn(
                        Mono.error(
                                webClientException(HttpStatus.BAD_REQUEST)
                        )
                );

        StepVerifier.create(inadClient.checkMassiveRequestStatus("polling-id"))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void checkMassiveRequestStatusUnauthorizedRetryExhausted() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        when(apiEstrazioniMultipleApi.verificaStatoRichiestaElencoDomiciliDigitali("polling-id"))
                .thenReturn(
                        Mono.error(
                                webClientException(HttpStatus.UNAUTHORIZED)
                        )
                );

        StepVerifier.create(inadClient.checkMassiveRequestStatus("polling-id"))
                .expectError(PnInternalException.class)
                .verify();

        verify(apiEstrazioniMultipleApi, times(2)).verificaStatoRichiestaElencoDomiciliDigitali("polling-id");
    }

    @Test
    void checkMassiveRequestStatusGenericError() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        RuntimeException exception = new RuntimeException("generic error");
        when(apiEstrazioniMultipleApi.verificaStatoRichiestaElencoDomiciliDigitali("polling-id")).thenReturn(Mono.error(exception));

        StepVerifier.create(inadClient.checkMassiveRequestStatus("polling-id"))
                .expectErrorMatches(ex -> ex == exception)
                .verify();
    }

    @Test
    void getMassiveRequestResultSuccess() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        ResponseListDigitalAddress response = new ResponseListDigitalAddress();
        when(apiEstrazioniMultipleApi.recuperoElencoDomiciliDigitali("polling-id")).thenReturn(Mono.just(response));

        StepVerifier.create(inadClient.getMassiveRequestResult("polling-id"))
                .expectNext(response)
                .verifyComplete();

        verify(apiClient).setBearerToken(TOKEN);
    }

    @Test
    void getMassiveRequestResultBadRequest() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        when(apiEstrazioniMultipleApi.recuperoElencoDomiciliDigitali("polling-id"))
                .thenReturn(
                        Mono.error(
                                webClientException(HttpStatus.BAD_REQUEST)
                        )
                );

        StepVerifier.create(inadClient.getMassiveRequestResult("polling-id"))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void getMassiveRequestResultUnauthorizedRetryExhausted() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        when(apiEstrazioniMultipleApi.recuperoElencoDomiciliDigitali("polling-id"))
                .thenReturn(
                        Mono.error(
                                webClientException(HttpStatus.UNAUTHORIZED)
                        )
                );

        StepVerifier.create(inadClient.getMassiveRequestResult("polling-id"))
                .expectError(PnInternalException.class)
                .verify();

        verify(apiEstrazioniMultipleApi, times(2)).recuperoElencoDomiciliDigitali("polling-id");
    }

    @Test
    void getMassiveRequestResultGenericError() {
        InadClient inadClient = buildClient();

        mockToken();

        ApiClient apiClient = mock(ApiClient.class);

        when(apiEstrazioniMultipleApi.getApiClient()).thenReturn(apiClient);
        RuntimeException exception = new RuntimeException("generic error");
        when(apiEstrazioniMultipleApi.recuperoElencoDomiciliDigitali("polling-id")).thenReturn(Mono.error(exception));

        StepVerifier.create(inadClient.getMassiveRequestResult("polling-id"))
                .expectErrorMatches(ex -> ex == exception)
                .verify();
    }

    @Test
    void shouldRetryWhenNotWebClientResponseExceptionThenReturnFalse() {
        InadClient inadClient = buildClient();
        assertFalse(inadClient.shouldRetry(new Exception()));
    }

    @Test
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        InadClient inadClient = buildClient();

        WebClientResponseException exception = webClientException(HttpStatus.UNAUTHORIZED);

        assertTrue(inadClient.shouldRetry(exception));
    }

    @Test
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIsNot401ThenReturnFalse() {
        InadClient inadClient = buildClient();

        WebClientResponseException exception = webClientException(HttpStatus.BAD_REQUEST);

        assertFalse(inadClient.shouldRetry(exception));
    }

    private InadClient buildClient() {
        return new InadClient(
                accessTokenExpiringMap,
                apiEstrazioniPuntualiApi,
                apiEstrazioniMultipleApi,
                inadSecretConfig,
                pnNationalRegistriesSecretService
        );
    }

    private void mockToken() {
        PdndSecretValue secretValue = mockSecret();

        AccessTokenCacheEntry token = new AccessTokenCacheEntry(PURPOSE_ID);

        token.setTokenValue(TOKEN);
        token.setTokenType(TokenType.BEARER);

        when(accessTokenExpiringMap.getPDNDToken(
                any(),
                same(secretValue),
                eq(false)
        )).thenReturn(Mono.just(token));
    }

    private PdndSecretValue mockSecret() {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setPurposeId(PURPOSE_ID);

        PdndSecretValue secretValue =
                new PdndSecretValue();

        secretValue.setJwtConfig(jwtConfig);

        when(inadSecretConfig.getPdndSecret())
                .thenReturn(SECRET_NAME);

        when(pnNationalRegistriesSecretService
                .getPdndSecretValue(SECRET_NAME))
                .thenReturn(secretValue);

        return secretValue;
    }

    private WebClientResponseException webClientException(HttpStatus status) {
        return new WebClientResponseException(
                "message",
                status.value(),
                status.getReasonPhrase(),
                HttpHeaders.EMPTY,
                new byte[0],
                Charset.defaultCharset()
        );
    }
}