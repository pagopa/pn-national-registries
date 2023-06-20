package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.inad.InadSecretConfig;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.function.Function;

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
    WebClient webClient;

    @MockBean
    InadWebClient inadWebClient;

    @MockBean
    InadSecretConfig inadSecretConfig;

    @Test
    void callEService() {
        when(inadWebClient.init()).thenReturn(webClient);
        InadClient inadClient = new InadClient(accessTokenExpiringMap,inadWebClient,"purposeId", inadSecretConfig);

        ResponseRequestDigitalAddressDto response = new ResponseRequestDigitalAddressDto();
        response.setTaxId("cf");
        response.setSince(new Date());

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        when(accessTokenExpiringMap.getPDNDToken(eq("purposeId"), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestBodySpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ResponseRequestDigitalAddressDto.class)).thenReturn(Mono.just(response));

        StepVerifier.create(inadClient.callEService("cf","test"))
                .expectNext()
                .verifyError();
    }

    @Test
    void callEServiceDoOnError() {
        when(inadWebClient.init()).thenReturn(webClient);
        InadClient inadClient = new InadClient(accessTokenExpiringMap,inadWebClient,"purposeId", inadSecretConfig);
        HttpHeaders headers = mock(HttpHeaders.class);
        byte[] testByteArray = new byte[0];
        String test = "test";
        WebClientResponseException webClientResponseException = new WebClientResponseException(test, HttpStatus.NOT_FOUND.value(), test, headers, testByteArray, Charset.defaultCharset());

        when(accessTokenExpiringMap.getPDNDToken(eq("purposeId"), any(), anyBoolean())).thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(inadClient.callEService("cf", "test"))
                .verifyError(WebClientResponseException.class);
    }

    @Test
    @DisplayName("Should return false when the exception is not webclientresponseexception")
    void shouldRetryWhenNotWebClientResponseExceptionThenReturnFalse() {
        InadClient inadClient = new InadClient(accessTokenExpiringMap, inadWebClient, "purposeId", inadSecretConfig);
        assertFalse(inadClient.shouldRetry(new Exception()));
    }

    @Test
    @DisplayName("Should return true when the exception is webclientresponseexception and the status code is 401")
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        InadClient inadClient = new InadClient(accessTokenExpiringMap, inadWebClient, "purposeId", inadSecretConfig);
        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.UNAUTHORIZED.value(), "statusText", HttpHeaders.EMPTY, null, null);
        assertTrue(inadClient.shouldRetry(webClientResponseException));
    }
}
