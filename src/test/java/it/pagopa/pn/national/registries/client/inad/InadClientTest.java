package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.client.anpr.AgidJwtSignature;
import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.dto.TokenTypeDto;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;
import org.apache.http.client.utils.URIBuilder;
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
import java.util.Date;

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
    AgidJwtSignature agidJwtSignature;

    @MockBean
    InadWebClient inadWebClient;

    @Test
    void callEService() {
        when(inadWebClient.init()).thenReturn(webClient);
        InadClient inadClient = new InadClient(
                accessTokenExpiringMap,inadWebClient,"purposeId"
        );

        ResponseRequestDigitalAddressDto response = new ResponseRequestDigitalAddressDto();
        response.setCodiceFiscale("cf");
        response.setSince(new Date());

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setAccessToken("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(accessTokenExpiringMap.getToken("purposeId")).thenReturn(Mono.just(accessTokenCacheEntry));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        //when(requestHeadersUriSpec.uri().thenReturn(requestHeadersSpec));
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ResponseRequestDigitalAddressDto.class)).thenReturn(Mono.just(response));

        StepVerifier.create(inadClient.callEService("cf","test")).expectNext().verifyError();

    }

    @Test
    @DisplayName("Should return false when the exception is not webclientresponseexception")
    void checkExceptionTypeWhenNotWebClientResponseExceptionThenReturnFalse() {
        InadClient inadClient =
                new InadClient(
                        accessTokenExpiringMap,
                        inadWebClient,
                        "purposeId");
        assertFalse(inadClient.checkExceptionType(new Exception()));
    }

    @Test
    @DisplayName(
            "Should return true when the exception is webclientresponseexception and the status code is 401")
    void checkExceptionTypeWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        InadClient inadClient =
                new InadClient(
                        accessTokenExpiringMap,
                        inadWebClient,
                        "purposeId");
        WebClientResponseException webClientResponseException =
                new WebClientResponseException(
                        "message",
                        HttpStatus.UNAUTHORIZED.value(),
                        "statusText",
                        HttpHeaders.EMPTY,
                        null,
                        null);
        assertTrue(inadClient.checkExceptionType(webClientResponseException));
    }
}
