package it.pagopa.pn.national.registries.client.checkcf;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.client.anpr.AgidJwtSignature;
import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.dto.TokenTypeDto;
import it.pagopa.pn.national.registries.model.checkcf.Richiesta;
import it.pagopa.pn.national.registries.model.checkcf.VerificaCodiceFiscale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    AgidJwtSignature agidJwtSignature;

    @MockBean
    CheckCfWebClient checkCfWebClient;

    @Test
    void callEService() {
        when(checkCfWebClient.init()).thenReturn(webClient);
        CheckCfClient checkCfClient = new CheckCfClient(
                accessTokenExpiringMap,checkCfWebClient,"purposeId"
        );
        Richiesta richiesta = new Richiesta();
        richiesta.setCodiceFiscale("cf");

        VerificaCodiceFiscale verificaCodiceFiscale = new VerificaCodiceFiscale();
        verificaCodiceFiscale.setCodiceFiscale("cf");
        verificaCodiceFiscale.setValido(true);
        verificaCodiceFiscale.setMessaggio("valid");

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setAccessToken("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(accessTokenExpiringMap.getToken("purposeId")).thenReturn(Mono.just(accessTokenCacheEntry));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/verifica")).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(VerificaCodiceFiscale.class)).thenReturn(Mono.just(verificaCodiceFiscale));

        StepVerifier.create(checkCfClient.callEService(richiesta)).expectNext(verificaCodiceFiscale).verifyComplete();

    }


    @Test
    @DisplayName("Should return false when the exception is not webclientresponseexception")
    void checkExceptionTypeWhenNotWebClientResponseExceptionThenReturnFalse() {
        CheckCfClient checkCfClient =
                new CheckCfClient(
                        accessTokenExpiringMap,
                        checkCfWebClient,
                        "purposeId");
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
                        "purposeId");
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
