package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.pdnd.AuthApiCustom;
import it.pagopa.pn.national.registries.exceptions.PdndTokenGeneratorException;
import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.SecretValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = TokenProvider.class)
@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    @Mock
    PdndAssertionGenerator assertionGenerator;

    @Mock
    SecretManagerService secretManagerService;

    @Mock
    AuthApiCustom authApiCustom;

    @Test
    void getToken() {
        TokenProvider tokenProvider = new TokenProvider(assertionGenerator,
                secretManagerService,
                authApiCustom,
                "test",
                "client_credentials",
                "basePath");
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("{\n" +
                "\"client_id\":\"123\",\n" +
                "\"keyId\":\"123\"\n" +
                "}").build();
        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("token");
        when(authApiCustom.getApiClient()).thenReturn(new ApiClient());
        when(secretManagerService.getSecretValue(any())).thenReturn(Optional.of(getSecretValueResponse));
        when(assertionGenerator.generateClientAssertion(any())).thenReturn("clientAssertion");
        when(authApiCustom.createToken(eq("clientAssertion"),anyString(),anyString(),anyString())).thenReturn(Mono.just(clientCredentialsResponseDto));
        StepVerifier.create(tokenProvider.getToken("purpose")).expectNext(clientCredentialsResponseDto).verifyComplete();
    }

    @Test
    void getTokenSecretEmpty() {
        TokenProvider tokenProvider = new TokenProvider(assertionGenerator,
                secretManagerService,
                authApiCustom,
                "test",
                "client_credentials",
                "basePath");
        when(secretManagerService.getSecretValue(any())).thenReturn(Optional.empty());
        StepVerifier.create(tokenProvider.getToken("purpose")).expectComplete().verify();
    }
}
