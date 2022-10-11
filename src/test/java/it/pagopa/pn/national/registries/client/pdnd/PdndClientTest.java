package it.pagopa.pn.national.registries.client.pdnd;

import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {PdndClient.class})
@ExtendWith(SpringExtension.class)
class PdndClientTest {
    @MockBean
    WebClient webClient;

    @MockBean
    PdndWebClient pdndWebClient;

    @Test
    void callCreateTokenTest() {
        when(pdndWebClient.initWebClient()).thenReturn(webClient);
        PdndClient pdndClient = new PdndClient(pdndWebClient);

        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("accessToken");
        clientCredentialsResponseDto.setTokenType(TokenTypeDto.BEARER);
        clientCredentialsResponseDto.setExpiresIn(600);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/authorization-server/token.oauth2")).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestHeadersSpec.header(any(), any())).thenReturn(requestHeadersSpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClientCredentialsResponseDto.class)).thenReturn(Mono.just(clientCredentialsResponseDto));

        StepVerifier.create(pdndClient.createToken("", "", "", "")).expectNext(clientCredentialsResponseDto).verifyComplete();

    }

/*    @Test
    void callEServiceThrowsJsonProcessingException() throws JsonProcessingException {
        when(pdndClient.init()).thenReturn(webClient);
        CheckCfClient checkCfClient = new CheckCfClient(
                accessTokenExpiringMap, pdndClient,"purposeId",objectMapper
        );
        Richiesta richiesta = new Richiesta();
        Mockito.when( objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("") {});


        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setAccessToken("fafsff");
        accessTokenCacheEntry.setTokenType(TokenTypeDto.BEARER);

        when(accessTokenExpiringMap.getToken("purposeId")).thenReturn(Mono.just(accessTokenCacheEntry));

        StepVerifier.create(checkCfClient.callEService(richiesta)).expectError(PnInternalException.class).verify();

    }*/


}
