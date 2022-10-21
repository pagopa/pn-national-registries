package it.pagopa.pn.national.registries.client.checkcf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.checkcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import it.pagopa.pn.national.registries.model.checkcf.Request;
import it.pagopa.pn.national.registries.model.checkcf.TaxIdVerification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class CheckCfClientTest {

    @MockBean
    AccessTokenExpiringMap accessTokenExpiringMap;

    @MockBean
    WebClient webClient;

    @MockBean
    CheckCfWebClient checkCfWebClient;

    @MockBean
    ObjectMapper objectMapper;

    @MockBean
    CheckCfSecretConfig checkCfSecretConfig;

    @Test
    void callEService() throws JsonProcessingException {
        when(checkCfWebClient.init()).thenReturn(webClient);
        CheckCfClient checkCfClient = new CheckCfClient(
                accessTokenExpiringMap,checkCfWebClient,"purposeId",objectMapper, checkCfSecretConfig
        );
        Request richiesta = new Request();
        richiesta.setCodiceFiscale("cf");

        String richiestaJson = "{\"codiceFiscale\": \"cf\"}";
        when( objectMapper.writeValueAsString(any())).thenReturn(richiestaJson);

        TaxIdVerification taxIdVerification = new TaxIdVerification();
        taxIdVerification.setCodiceFiscale("cf");
        taxIdVerification.setValido(true);
        taxIdVerification.setMessaggio("valid");

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setAccessToken("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);


        when(accessTokenExpiringMap.getToken(eq("purposeId"), any())).thenReturn(Mono.just(accessTokenCacheEntry));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/verifica")).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TaxIdVerification.class)).thenReturn(Mono.just(taxIdVerification));

        StepVerifier.create(checkCfClient.callEService(richiesta)).expectNext(taxIdVerification).verifyComplete();

    }

    @Test
    void callEServiceThrowsJsonProcessingException() throws JsonProcessingException {
        when(checkCfWebClient.init()).thenReturn(webClient);
        CheckCfClient checkCfClient = new CheckCfClient(
                accessTokenExpiringMap,checkCfWebClient,"purposeId",objectMapper, checkCfSecretConfig
        );
        Request richiesta = new Request();
        Mockito.when( objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("") {});


        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setAccessToken("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        when(accessTokenExpiringMap.getToken(eq("purposeId"),any())).thenReturn(Mono.just(accessTokenCacheEntry));

        StepVerifier.create(checkCfClient.callEService(richiesta)).expectError(PnInternalException.class).verify();

    }


    @Test
    @DisplayName("Should return false when the exception is not webclientresponseexception")
    void checkExceptionTypeWhenNotWebClientResponseExceptionThenReturnFalse() {
        CheckCfClient checkCfClient =
                new CheckCfClient(
                        accessTokenExpiringMap,
                        checkCfWebClient,
                        "purposeId",
                        objectMapper,
                        checkCfSecretConfig);
        assertFalse(checkCfClient.checkExceptionType(new Exception()));
    }

    @Test
    @DisplayName(
            "Should return true when the exception is webclientresponseexception and the status code is 401")
    void checkExceptionTypeWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        CheckCfClient checkCfClient =
                new CheckCfClient(
                        accessTokenExpiringMap,
                        checkCfWebClient,
                        "purposeId",
                        objectMapper,
                        checkCfSecretConfig);
        WebClientResponseException webClientResponseException =
                new WebClientResponseException(
                        "message",
                        HttpStatus.UNAUTHORIZED.value(),
                        "statusText",
                        HttpHeaders.EMPTY,
                        null,
                        null);
        assertTrue(checkCfClient.checkExceptionType(webClientResponseException));
    }

}
