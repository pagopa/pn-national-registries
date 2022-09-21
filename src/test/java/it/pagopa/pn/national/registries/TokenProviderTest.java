package it.pagopa.pn.national.registries;

import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.api.AuthApi;
import it.pagopa.pn.national.registries.service.PdndAssertionGenerator;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import it.pagopa.pn.national.registries.service.TokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = TokenProvider.class)
class TokenProviderTest {

    @InjectMocks
    TokenProvider tokenProvider;

    @Mock
    AuthApi authApi;

    @Mock
    PdndAssertionGenerator pdndAssertionGenerator;

    @Mock
    SecretManagerService secretManagerService;

    @Test
    void testTokenOk(){

    }

    @Test
    void testTokenException(){
        when(authApi.createToken(any(),any(),any(),any())).thenThrow(WebClientResponseException.class);
        when(pdndAssertionGenerator.generateClientAssertion(any())).thenReturn("token");
        Optional<GetSecretValueResponse> opt = Optional.of(GetSecretValueResponse.builder().arn("test").name("test").secretString("test").build());
        when(secretManagerService.getSecretValue(any())).thenReturn(opt);
        tokenProvider.getToken("test");
    }
}
