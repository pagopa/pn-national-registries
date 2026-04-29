package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereTokenClient;
import it.pagopa.pn.national.registries.client.pdnd.PdndClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.ClientCredentialsResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.TokenType;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    @InjectMocks
    private TokenProvider tokenProvider;

    @Mock
    PdndAssertionGenerator assertionGenerator;

    @Mock
    PdndClient pdndClient;

    @Mock
    InfoCamereTokenClient infoCamereTokenClient;

    @Mock
    NationalRegistriesConfig nationalRegistriesConfig;

    NationalRegistriesConfig.Pdnd pdndConfig;

    @BeforeEach
    void setup() {
        pdndConfig = new NationalRegistriesConfig.Pdnd();
        pdndConfig.setGrantType("client_credentials");
        pdndConfig.setClientAssertionType("urn:ietf:params:oauth:client-assert");
    }

    @Test
    void getTokenWhenClientIdAndSecretAreInvalidThenThrowException() {
        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        pdndSecretValue.setClientId("clientId");
        pdndSecretValue.setKeyId("keyId");
        when(assertionGenerator.generateClientAssertion(any())).thenReturn("clientAssertion");
        when(pdndClient.createToken(anyString(),eq("urn:ietf:params:oauth:client-assert"), eq("client_credentials"), any()))
                .thenReturn(Mono.empty());
        when(nationalRegistriesConfig.getPdnd()).thenReturn(pdndConfig);

        Mono<ClientCredentialsResponse> token = tokenProvider.getTokenPdnd(pdndSecretValue);

        StepVerifier.create(token).verifyComplete();
    }

    @Test
    void getTokenWhenClientIdAndSecretAreValidThenReturnAToken() {
        String clientId = "clientId";
        String secret = "secret";
        String token = "token";
        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        pdndSecretValue.setClientId(clientId);
        pdndSecretValue.setKeyId(secret);
        ClientCredentialsResponse clientCredentialsResponse = new ClientCredentialsResponse();
        clientCredentialsResponse.setAccessToken(token);
        when(nationalRegistriesConfig.getPdnd()).thenReturn(pdndConfig);

        when(assertionGenerator.generateClientAssertion(any())).thenReturn("assertion");
        when(pdndClient.createToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(clientCredentialsResponse));


        Mono<ClientCredentialsResponse> tokenMono = tokenProvider.getTokenPdnd(pdndSecretValue);

        StepVerifier.create(tokenMono)
                .expectNextMatches(
                        clientCredentialsResponseDto1 ->
                                clientCredentialsResponseDto1.getAccessToken().equals(token))
                .verifyComplete();
    }

    @Test
    void getToken() {
        ClientCredentialsResponse clientCredentialsResponse = new ClientCredentialsResponse();
        clientCredentialsResponse.setAccessToken("token");
        when(nationalRegistriesConfig.getPdnd()).thenReturn(pdndConfig);

        when(assertionGenerator.generateClientAssertion(any())).thenReturn("clientAssertion");
        when(pdndClient.createToken(anyString(),eq("urn:ietf:params:oauth:client-assert"), eq("client_credentials"), any()))
                .thenReturn(Mono.just(clientCredentialsResponse));
        StepVerifier.create(tokenProvider.getTokenPdnd(new PdndSecretValue())).expectNext(clientCredentialsResponse).verifyComplete();
    }

    @Test
    void getTokenInfoCamere(){
        when(infoCamereTokenClient.getToken(anyString())).thenReturn(Mono.just("scope"));
        StepVerifier.create(tokenProvider.getTokenInfoCamere("scope")).expectNext("scope").verifyComplete();
    }
}
