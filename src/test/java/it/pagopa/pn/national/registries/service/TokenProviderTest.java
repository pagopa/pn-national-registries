package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.pdnd.PdndClient;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
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
    SecretManagerService secretManagerService;

    @Mock
    PdndClient pdndClient;

    @Test
    void getToken() {
        TokenProvider tokenProvider = new TokenProvider(assertionGenerator,
                secretManagerService,
                pdndClient,
                "client_credentials",
                "basePath");
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("{\n" +
                "\"client_id\":\"123\",\n" +
                "\"keyId\":\"123\"\n" +
                "}").build();
        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("token");
        when(secretManagerService.getSecretValue(any())).thenReturn(Optional.of(getSecretValueResponse));
        when(assertionGenerator.generateClientAssertion(any())).thenReturn("clientAssertion");
        when(pdndClient.createToken(eq("clientAssertion"),anyString(),anyString(),anyString())).thenReturn(Mono.just(clientCredentialsResponseDto));
        StepVerifier.create(tokenProvider.getToken("purpose")).expectNext(clientCredentialsResponseDto).verifyComplete();
    }

    @Test
    void getTokenSecretEmpty() {
        TokenProvider tokenProvider = new TokenProvider(assertionGenerator,
                secretManagerService,
                pdndClient,
                "test",
                "client_credentials");
        when(secretManagerService.getSecretValue(any())).thenReturn(Optional.empty());
        StepVerifier.create(tokenProvider.getToken("purpose")).expectComplete().verify();
    }
}
