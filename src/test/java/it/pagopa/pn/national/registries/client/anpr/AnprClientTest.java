package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.CachedSecretsManagerConsumer;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.api.E002ServiceApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RichiestaE002;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RispostaE002OK;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoCriteriRicercaE002;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoListaSoggetti;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class AnprClientTest {

    @MockBean
    AccessTokenExpiringMap accessTokenExpiringMap;

    @MockBean
    WebClient webClient;

    @MockBean
    AgidJwtSignature agidJwtSignature;
    
    @MockBean
    AgidJwtTrackingEvidence agidJwtTrackingEvidence;

    @MockBean
    E002ServiceApi e002ServiceApi;

    @Mock
    ApiClient apiClient;

    @MockBean
    AnprSecretConfig anprSecretConfig;

    @MockBean
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @Test
    @DisplayName("Should return false when the exception is not webclientresponseexception")
    void shouldRetryWhenNotWebClientResponseExceptionThenReturnFalse() {
        AnprClient anprClient = new AnprClient(accessTokenExpiringMap,agidJwtSignature, e002ServiceApi, agidJwtTrackingEvidence,
                anprSecretConfig, pnNationalRegistriesSecretService);
        assertFalse(anprClient.shouldRetry(new Exception()));
    }

    @Test
    @DisplayName("Should return true when the exception is webclientresponseexception and the status code is 401")
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        AnprClient anprClient = new AnprClient(accessTokenExpiringMap, agidJwtSignature, e002ServiceApi, agidJwtTrackingEvidence,
                 anprSecretConfig, pnNationalRegistriesSecretService);
        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.UNAUTHORIZED.value(), "statusText", HttpHeaders.EMPTY, null, null);
        assertTrue(anprClient.shouldRetry(webClientResponseException));
    }

    @Test
    void callEService() {
        AnprClient anprClient = new AnprClient(accessTokenExpiringMap, agidJwtSignature, e002ServiceApi, agidJwtTrackingEvidence,
                 anprSecretConfig, pnNationalRegistriesSecretService);

        RichiestaE002 richiestaE002 = new RichiestaE002();
        TipoCriteriRicercaE002 dto = new TipoCriteriRicercaE002();
        dto.setCodiceFiscale("DDDFFF52G52H501H");
        richiestaE002.setCriteriRicerca(dto);

        RispostaE002OK rispostaE002OKDto = new RispostaE002OK();
        rispostaE002OKDto.setListaSoggetti(new TipoListaSoggetti());

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(agidJwtTrackingEvidence.createAgidJwt()).thenReturn("testJws");

        when(e002ServiceApi.getApiClient()).thenReturn(apiClient);
        when(e002ServiceApi.e002(any())).thenReturn(Mono.just(rispostaE002OKDto));
        doNothing().when(apiClient).setBearerToken(anyString());
        when(apiClient.addDefaultHeader(any(),any())).thenReturn(apiClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/anpr-service-e002")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RispostaE002OK.class)).thenReturn(Mono.just(rispostaE002OKDto));
        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        pdndSecretValue.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(pdndSecretValue);
        StepVerifier.create(anprClient.callEService(richiestaE002))
                .expectNext(rispostaE002OKDto)
                .verifyComplete();
    }

    @Test
    void callEService2() {
        AnprClient anprClient = new AnprClient(accessTokenExpiringMap, agidJwtSignature, e002ServiceApi, agidJwtTrackingEvidence,
                 anprSecretConfig, pnNationalRegistriesSecretService);

        RichiestaE002 richiestaE002 = new RichiestaE002();
        TipoCriteriRicercaE002 dto = new TipoCriteriRicercaE002();
        dto.setCodiceFiscale("DDDFFF52G52H501H");
        richiestaE002.setCriteriRicerca(dto);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(agidJwtTrackingEvidence.createAgidJwt()).thenReturn("testJws");
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(e002ServiceApi.getApiClient()).thenReturn(apiClient);
        doNothing().when(apiClient).setBearerToken(anyString());
        when(apiClient.addDefaultHeader(any(),any())).thenReturn(apiClient);
        when(requestBodyUriSpec.uri("/anpr-service-e002")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(e002ServiceApi.e002(any())).thenThrow(mock(PnNationalRegistriesException.class));
        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(exception.getHeaders()).thenReturn(new HttpHeaders());
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RispostaE002OK.class)).thenReturn(Mono.error(exception));
        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        pdndSecretValue.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(pdndSecretValue);

        StepVerifier.create(anprClient.callEService(richiestaE002))
                .verifyError(PnNationalRegistriesException.class);
    }

    @Test
    void callEService3() {
        AnprClient anprClient = new AnprClient(accessTokenExpiringMap, agidJwtSignature, e002ServiceApi, agidJwtTrackingEvidence,
                 anprSecretConfig, pnNationalRegistriesSecretService);

        RichiestaE002 richiestaE002 = new RichiestaE002();
        TipoCriteriRicercaE002 dto = new TipoCriteriRicercaE002();
        dto.setCodiceFiscale("DDDFFF52G52H501H");
        richiestaE002.setCriteriRicerca(dto);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(agidJwtTrackingEvidence.createAgidJwt()).thenReturn("testJws");
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(e002ServiceApi.getApiClient()).thenReturn(apiClient);
        doNothing().when(apiClient).setBearerToken(anyString());
        when(apiClient.addDefaultHeader(any(),any())).thenReturn(apiClient);
        when(requestBodyUriSpec.uri("/anpr-service-e002")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(e002ServiceApi.e002(any())).thenThrow(mock(PnInternalException.class));
        when(exception.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);

        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RispostaE002OK.class)).thenReturn(Mono.error(exception));
        PdndSecretValue secret = new PdndSecretValue();
        secret.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(secret);
        StepVerifier.create(anprClient.callEService(richiestaE002))
                .verifyError(PnInternalException.class);
    }

    @Test
    void callAnprDoOnError() {
        AnprClient anprClient = new AnprClient(accessTokenExpiringMap, agidJwtSignature, e002ServiceApi, agidJwtTrackingEvidence,
                anprSecretConfig, pnNationalRegistriesSecretService);

        RichiestaE002 richiestaE002 = new RichiestaE002();
        TipoCriteriRicercaE002 dto = new TipoCriteriRicercaE002();
        dto.setCodiceFiscale("DDDFFF52G52H501H");
        richiestaE002.setCriteriRicerca(dto);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.BAD_REQUEST.value(), "statusText", HttpHeaders.EMPTY, null, null);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(agidJwtTrackingEvidence.createAgidJwt()).thenReturn("testJws");
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(e002ServiceApi.getApiClient()).thenReturn(apiClient);
        doNothing().when(apiClient).setBearerToken(anyString());
        when(apiClient.addDefaultHeader(any(),any())).thenReturn(apiClient);
        when(requestBodyUriSpec.uri("/anpr-service-e002")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(e002ServiceApi.e002(any())).thenReturn(Mono.error(webClientResponseException));
        when(exception.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RispostaE002OK.class)).thenReturn(Mono.error(exception));
        PdndSecretValue secret = new PdndSecretValue();
        secret.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(secret);
        StepVerifier.create(anprClient.callEService(richiestaE002))
                .verifyError(PnNationalRegistriesException.class);
    }
}
