package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.pdnd.PdndClient;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.SecretValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    @Mock
    PdndAssertionGenerator assertionGenerator;

    @Mock
    PdndClient pdndClient;

    @Test
    void getToken() {
        TokenProvider tokenProvider = new TokenProvider(assertionGenerator,
                pdndClient,
                "client_credentials",
                "basePath");
        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("token");
        when(assertionGenerator.generateClientAssertion(any())).thenReturn("clientAssertion");
        when(pdndClient.createToken(eq("clientAssertion"),anyString(),anyString(),anyString())).thenReturn(Mono.just(clientCredentialsResponseDto));
        StepVerifier.create(tokenProvider.getToken(new SecretValue())).expectNext(clientCredentialsResponseDto).verifyComplete();
    }

    @Test
    void getTokenSecretEmpty() {
        TokenProvider tokenProvider = new TokenProvider(assertionGenerator,
                pdndClient,
                "test",
                "client_credentials");
        StepVerifier.create(tokenProvider.getToken(new SecretValue())).expectComplete().verify();
    }
}
