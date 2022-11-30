package it.pagopa.pn.national.registries.client.inipec;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.config.inipec.IniPecSecretConfig;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.Date;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class IniPecClientTest {

    @MockBean
    WebClient webClient;

    @MockBean
    IniPecJwsGenerator iniPecJwsGenerator;

    @MockBean
    IniPecWebClient iniPecWebClient;

    @MockBean
    IniPecSecretConfig iniPecSecretConfig;

    @MockBean
    ObjectMapper mapper;

    @Test
    void callgetTokenTest() {
        when(iniPecWebClient.init()).thenReturn(webClient);
       IniPecClient iniPecClient = new IniPecClient(iniPecWebClient,iniPecJwsGenerator, mapper);

        ClientCredentialsResponseDto response = new ClientCredentialsResponseDto();
        response.setAccessToken("token");

        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClientCredentialsResponseDto.class)).thenThrow(WebClientResponseException.class);

        StepVerifier.create(iniPecClient.getToken()).expectError();

    }
}
