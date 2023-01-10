package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.pdnd.PdndClient;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.SecretValue;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    @Mock
    PdndAssertionGenerator assertionGenerator;

    @Mock
    PdndClient pdndClient;

    @Test
    @DisplayName("Should throw an exception when the client id and secret are invalid")
    void getTokenWhenClientIdAndSecretAreInvalidThenThrowException() {
        SecretValue secretValue = new SecretValue();
        secretValue.setClientId("clientId");
        secretValue.setKeyId("keyId");
        when(assertionGenerator.generateClientAssertion(any())).thenReturn("clientAssertion");
        when(pdndClient.createToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        TokenProvider tokenProvider =
                new TokenProvider(
                        assertionGenerator,pdndClient,"clientAssertionType", "grantType");
        Mono<ClientCredentialsResponseDto> token = tokenProvider.getTokenPdnd(secretValue);

        StepVerifier.create(token).verifyComplete();
    }

    @Test
    @DisplayName("Should return a token when the client id and secret are valid")
    void getTokenWhenClientIdAndSecretAreValidThenReturnAToken() {
        String clientId = "clientId";
        String secret = "secret";
        String token = "token";
        SecretValue secretValue = new SecretValue();
        secretValue.setClientId(clientId);
        secretValue.setKeyId(secret);
        ClientCredentialsResponseDto clientCredentialsResponseDto =
                new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken(token);

        when(assertionGenerator.generateClientAssertion(any())).thenReturn("assertion");
        when(pdndClient.createToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(clientCredentialsResponseDto));

        TokenProvider tokenProvider =
                new TokenProvider(
                        assertionGenerator, pdndClient, "clientAssertionType", "grantType");

        Mono<ClientCredentialsResponseDto> tokenMono = tokenProvider.getTokenPdnd(secretValue);

        StepVerifier.create(tokenMono)
                .expectNextMatches(
                        clientCredentialsResponseDto1 ->
                                clientCredentialsResponseDto1.getAccessToken().equals(token))
                .verifyComplete();
    }

    @Test
    void getToken() {
        TokenProvider tokenProvider = new TokenProvider(assertionGenerator,
                pdndClient,
                "client_credentials",
                "basePath");
        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("token");
        when(assertionGenerator.generateClientAssertion(any())).thenReturn("clientAssertion");
        when(pdndClient.createToken("clientAssertion", "client_credentials",
                "basePath", null)).thenReturn(Mono.just(clientCredentialsResponseDto));
        StepVerifier.create(tokenProvider.getTokenPdnd(new SecretValue())).expectNext(clientCredentialsResponseDto).verifyComplete();
    }

    @Test
    void getTokenSecretEmpty() {
        TokenProvider tokenProvider = new TokenProvider(assertionGenerator,
                pdndClient,
                "test",
                "client_credentials");
        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setTokenType(TokenTypeDto.BEARER);
        when(pdndClient.createToken(null,"test","client_credentials",null))
                .thenReturn(Mono.just(clientCredentialsResponseDto));
        StepVerifier.create(tokenProvider.getTokenPdnd(new SecretValue())).expectNext(clientCredentialsResponseDto)
                .verifyComplete();
    }
}
