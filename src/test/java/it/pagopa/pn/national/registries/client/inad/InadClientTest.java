package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.inad.InadSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.api.ApiEstrazioniPuntualiApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class InadClientTest {

    @MockBean
    AccessTokenExpiringMap accessTokenExpiringMap;

    @MockBean
    ApiEstrazioniPuntualiApi apiEstrazioniPuntualiApi;

    @MockBean
    InadSecretConfig inadSecretConfig;

    @MockBean
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @Test
    void callEService() {

        InadClient inadClient = new InadClient(accessTokenExpiringMap, apiEstrazioniPuntualiApi, inadSecretConfig, pnNationalRegistriesSecretService);

        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale("cf");
        response.setSince(OffsetDateTime.now());

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));

        when(apiEstrazioniPuntualiApi.recuperoDomicilioDigitale(anyString(), anyString())).thenReturn(Mono.just(response));

        PdndSecretValue value = new PdndSecretValue();
        value.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(value);
        StepVerifier.create(inadClient.callEService("cf","test"))
                .expectNext()
                .verifyError();
    }

    @Test
    void callEServiceDoOnError() {
        InadClient inadClient = new InadClient(accessTokenExpiringMap, apiEstrazioniPuntualiApi, inadSecretConfig, pnNationalRegistriesSecretService);
        HttpHeaders headers = mock(HttpHeaders.class);
        byte[] testByteArray = new byte[0];
        String test = "test";
        WebClientResponseException webClientResponseException = new WebClientResponseException(test, HttpStatus.NOT_FOUND.value(), test, headers, testByteArray, Charset.defaultCharset());

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.error(webClientResponseException));
        PdndSecretValue value = new PdndSecretValue();
        value.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(value);
        StepVerifier.create(inadClient.callEService("cf", "test"))
                .verifyError(WebClientResponseException.class);
    }

    @Test
    void callExtractOnRetryExhaustedThrow() {
        InadClient inadClient = new InadClient(accessTokenExpiringMap, apiEstrazioniPuntualiApi, inadSecretConfig, pnNationalRegistriesSecretService);

        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale("cf");
        response.setSince(OffsetDateTime.now());

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        PdndSecretValue value = new PdndSecretValue();
        value.setJwtConfig(new JwtConfig());

        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.BAD_REQUEST.value(), "statusText", HttpHeaders.EMPTY, null, null);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(value);
        when(apiEstrazioniPuntualiApi.getApiClient()).thenReturn(mock(ApiClient.class));
        when(apiEstrazioniPuntualiApi.recuperoDomicilioDigitale(any(),any())).thenReturn(Mono.error(webClientResponseException));
        StepVerifier.create(inadClient.callEService("cf","test"))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void callExtractDoOnError() {
        InadClient inadClient = new InadClient(accessTokenExpiringMap, apiEstrazioniPuntualiApi, inadSecretConfig, pnNationalRegistriesSecretService);

        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale("cf");
        response.setSince(OffsetDateTime.now());

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        PdndSecretValue value = new PdndSecretValue();
        value.setJwtConfig(new JwtConfig());

        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.UNAUTHORIZED.value(), "statusText", HttpHeaders.EMPTY, null, null);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(value);
        when(apiEstrazioniPuntualiApi.getApiClient()).thenReturn(mock(ApiClient.class));
        when(apiEstrazioniPuntualiApi.recuperoDomicilioDigitale(any(),any())).thenThrow(webClientResponseException);
        StepVerifier.create(inadClient.callEService("cf","test"))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    @DisplayName("Should return false when the exception is not webclientresponseexception")
    void shouldRetryWhenNotWebClientResponseExceptionThenReturnFalse() {
        InadClient inadClient = new InadClient(accessTokenExpiringMap, apiEstrazioniPuntualiApi, inadSecretConfig, pnNationalRegistriesSecretService);
        assertFalse(inadClient.shouldRetry(new Exception()));
    }

    @Test
    @DisplayName("Should return true when the exception is webclientresponseexception and the status code is 401")
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        InadClient inadClient = new InadClient(accessTokenExpiringMap, apiEstrazioniPuntualiApi, inadSecretConfig, pnNationalRegistriesSecretService);
        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.UNAUTHORIZED.value(), "statusText", HttpHeaders.EMPTY, null, null);
        assertTrue(inadClient.shouldRetry(webClientResponseException));
    }
}
