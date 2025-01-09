package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereTokenClient;
import it.pagopa.pn.national.registries.client.pdnd.PdndClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.ClientCredentialsResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.TokenType;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {TokenProvider.class, String.class})
@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    @MockBean
    private PdndAssertionGenerator pdndAssertionGenerator;

    @Autowired
    private TokenProvider tokenProvider;

    @Mock
    PdndAssertionGenerator assertionGenerator;

    @Mock
    PdndClient pdndClient;

    @Mock
    InfoCamereTokenClient infoCamereTokenClient;

    PnAuditLogEventType type = PnAuditLogEventType.AUD_NR_PF_PHYSICAL;
    Map<String, String> mdc = new HashMap<>();
    String message = "message";
    Object[] arguments = new Object[] {"arg1", "arg2"};
    PnAuditLogEvent logEvent;

    @BeforeEach
    public void setup() {
        mdc.put("key", "value");
        logEvent = new PnAuditLogEvent(type, mdc, message, arguments);
    }

    @Test
    @DisplayName("Should throw an exception when the client id and secret are invalid")
    void getTokenWhenClientIdAndSecretAreInvalidThenThrowException() {
        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        pdndSecretValue.setClientId("clientId");
        pdndSecretValue.setKeyId("keyId");
        when(assertionGenerator.generateClientAssertion(any())).thenReturn("clientAssertion");
        when(pdndClient.createToken(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Mono.empty());

        TokenProvider tokenProvider = new TokenProvider(assertionGenerator, pdndClient, infoCamereTokenClient,
                "clientAssertionType", "grantType");
        Mono<ClientCredentialsResponse> token = tokenProvider.getTokenPdnd(pdndSecretValue, logEvent);

        StepVerifier.create(token).verifyComplete();
    }

    @Test
    @DisplayName("Should return a token when the client id and secret are valid")
    void getTokenWhenClientIdAndSecretAreValidThenReturnAToken() {
        String clientId = "clientId";
        String secret = "secret";
        String token = "token";
        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        pdndSecretValue.setClientId(clientId);
        pdndSecretValue.setKeyId(secret);
        ClientCredentialsResponse clientCredentialsResponse = new ClientCredentialsResponse();
        clientCredentialsResponse.setAccessToken(token);

        when(assertionGenerator.generateClientAssertion(any())).thenReturn("assertion");
        when(pdndClient.createToken(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(clientCredentialsResponse));

        TokenProvider tokenProvider = new TokenProvider(assertionGenerator, pdndClient, infoCamereTokenClient,
                "clientAssertionType", "grantType");

        Mono<ClientCredentialsResponse> tokenMono = tokenProvider.getTokenPdnd(pdndSecretValue, logEvent);

        StepVerifier.create(tokenMono)
                .expectNextMatches(
                        clientCredentialsResponseDto1 ->
                                clientCredentialsResponseDto1.getAccessToken().equals(token))
                .verifyComplete();
    }

    @Test
    void getToken() {
        TokenProvider tokenProvider = new TokenProvider(assertionGenerator, pdndClient, infoCamereTokenClient,
                "client_credentials", "basePath");
        ClientCredentialsResponse clientCredentialsResponse = new ClientCredentialsResponse();
        clientCredentialsResponse.setAccessToken("token");
        when(assertionGenerator.generateClientAssertion(any())).thenReturn("clientAssertion");
        when(pdndClient.createToken("clientAssertion", "client_credentials",
                "basePath", null, logEvent)).thenReturn(Mono.just(clientCredentialsResponse));
        StepVerifier.create(tokenProvider.getTokenPdnd(new PdndSecretValue(), logEvent)).expectNext(clientCredentialsResponse).verifyComplete();
    }

    @Test
    void getTokenSecretEmpty() {
        TokenProvider tokenProvider = new TokenProvider(assertionGenerator, pdndClient, infoCamereTokenClient,
                "test", "client_credentials");
        ClientCredentialsResponse clientCredentialsResponse = new ClientCredentialsResponse();
        clientCredentialsResponse.setTokenType(TokenType.BEARER);
        when(pdndClient.createToken(null, "test", "client_credentials", null, logEvent))
                .thenReturn(Mono.just(clientCredentialsResponse));
        StepVerifier.create(tokenProvider.getTokenPdnd(new PdndSecretValue(), logEvent)).expectNext(clientCredentialsResponse)
                .verifyComplete();
    }

    @Test
    void getTokenInfoCamere(){
        TokenProvider tokenProvider = new TokenProvider(assertionGenerator, pdndClient, infoCamereTokenClient,
                "test", "client_credentials");
        when(infoCamereTokenClient.getToken(anyString(), any())).thenReturn(Mono.just("scope"));
        StepVerifier.create(tokenProvider.getTokenInfoCamere("scope", logEvent)).expectNext("scope").verifyComplete();
    }
}
