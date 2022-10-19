package it.pagopa.pn.national.registries.client.anpr;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.SecretValue;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import it.pagopa.pn.national.registries.model.anpr.RichiestaE002Dto;
import it.pagopa.pn.national.registries.model.anpr.RispostaE002OKDto;
import it.pagopa.pn.national.registries.model.anpr.TipoCriteriRicercaE002Dto;
import it.pagopa.pn.national.registries.model.anpr.TipoListaSoggettiDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
class AnprClientTest {

    @MockBean
    AccessTokenExpiringMap accessTokenExpiringMap;

    @MockBean
    WebClient webClient;

    @MockBean
    AgidJwtSignature agidJwtSignature;

    @MockBean
    AnprWebClient anprWebClient;

    @MockBean
    AnprSecretConfig anprSecretConfig;

    @Test
    @DisplayName("Should return false when the exception is not webclientresponseexception")
    void checkExceptionTypeWhenNotWebClientResponseExceptionThenReturnFalse() {
        AnprClient anprClient =
                new AnprClient(
                        accessTokenExpiringMap,
                        new ObjectMapper(),
                        agidJwtSignature,
                        anprWebClient,
                        "purposeId",
                        anprSecretConfig);
        assertFalse(anprClient.checkExceptionType(new Exception()));
    }

    @Test
    @DisplayName(
            "Should return true when the exception is webclientresponseexception and the status code is 401")
    void checkExceptionTypeWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        AnprClient anprClient =
                new AnprClient(
                        accessTokenExpiringMap,
                        new ObjectMapper(),
                        agidJwtSignature,
                        anprWebClient,
                        "purposeId",
                        anprSecretConfig);
        WebClientResponseException webClientResponseException =
                new WebClientResponseException(
                        "message",
                        HttpStatus.UNAUTHORIZED.value(),
                        "statusText",
                        HttpHeaders.EMPTY,
                        null,
                        null);
        assertTrue(anprClient.checkExceptionType(webClientResponseException));
    }

    @Test
    void callEService() {
        when(anprWebClient.init()).thenReturn(webClient);
        AnprClient anprClient = new AnprClient(
                accessTokenExpiringMap,new ObjectMapper(),agidJwtSignature, anprWebClient, "purposeId", anprSecretConfig);

        SecretValue secretValue = new SecretValue();
        secretValue.setClientId("testClient");
        secretValue.setKeyId("testKeyId");
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("aud");
        jwtConfig.setKid("kid");
        jwtConfig.setIssuer("testiss");
        jwtConfig.setSubject("subject");
        jwtConfig.setPurposeId("purposeId");
        secretValue.setJwtConfig(jwtConfig);
        //when(anprSecretConfig.getAnprSecretValue()).thenReturn(secretValue);

        RichiestaE002Dto richiestaE002Dto = new RichiestaE002Dto();
        TipoCriteriRicercaE002Dto dto = new TipoCriteriRicercaE002Dto();
        dto.setCodiceFiscale("DDDFFF52G52H501H");
        richiestaE002Dto.setCriteriRicerca(dto);

        RispostaE002OKDto rispostaE002OKDto = new RispostaE002OKDto();
        rispostaE002OKDto.setListaSoggetti(new TipoListaSoggettiDto());

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setAccessToken("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(accessTokenExpiringMap.getToken(any(),any())).thenReturn(Mono.just(accessTokenCacheEntry));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/anpr-service-e002")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RispostaE002OKDto.class)).thenReturn(Mono.just(rispostaE002OKDto));

        StepVerifier.create(anprClient.callEService(richiestaE002Dto)).expectNext(rispostaE002OKDto).verifyComplete();

    }
}
