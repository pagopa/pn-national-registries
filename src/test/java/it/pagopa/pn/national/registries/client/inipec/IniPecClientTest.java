package it.pagopa.pn.national.registries.client.inipec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.config.inipec.IniPecSecretConfig;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import it.pagopa.pn.national.registries.model.inipec.RequestCfIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
import it.pagopa.pn.national.registries.model.registroImprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.model.registroImprese.LegalAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

        String jws = "jws";
        when(iniPecJwsGenerator.createAuthRest()).thenReturn(jws);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClientCredentialsResponseDto.class)).thenReturn(Mono.just(response));

        StepVerifier.create(iniPecClient.getToken()).expectError();

    }

    @Test
    void testCallEServiceRequestId(){
        when(iniPecWebClient.init()).thenReturn(webClient);
        IniPecClient iniPecClient = new IniPecClient(iniPecWebClient,iniPecJwsGenerator, mapper);

        RequestCfIniPec request = new RequestCfIniPec();
        request.setDataOraRichiesta(LocalDateTime.now().toString());
        ArrayList<String> cfs = new ArrayList<>();
        cfs.add("taxId");
        request.setElencoCf(cfs);

        ResponsePollingIdIniPec responsePollingIdIniPec = new ResponsePollingIdIniPec();
        responsePollingIdIniPec.setIdentificativoRichiesta("correlationId");
        responsePollingIdIniPec.setDataOraRichiesta(LocalDateTime.now().toString());

        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("accessToken");
        clientCredentialsResponseDto.setTokenType(TokenTypeDto.BEARER);
        clientCredentialsResponseDto.setExpiresIn(10);

        String requestJson = "requestJson";
        try {
            when(mapper.writeValueAsString(request)).thenReturn(requestJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClientCredentialsResponseDto.class)).thenReturn(Mono.just(clientCredentialsResponseDto));
        when(iniPecClient.getToken()).thenReturn(Mono.just(clientCredentialsResponseDto));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/richiestaElencoPec")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ResponsePollingIdIniPec.class)).thenReturn(Mono.just(responsePollingIdIniPec));

        StepVerifier.create(iniPecClient.callEServiceRequestId(request)).expectNext(responsePollingIdIniPec).verifyComplete();
    }

    @Test
    void testCallEServiceRequestPec() {
        when(iniPecWebClient.init()).thenReturn(webClient);
        IniPecClient iniPecClient = new IniPecClient(iniPecWebClient,iniPecJwsGenerator, mapper);

        String request = "correlationId";
        ResponsePecIniPec response = new ResponsePecIniPec();
        response.setIdentificativoRichiesta("correlationId");

        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("accessToken");
        clientCredentialsResponseDto.setTokenType(TokenTypeDto.BEARER);
        clientCredentialsResponseDto.setExpiresIn(10);

        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClientCredentialsResponseDto.class)).thenReturn(Mono.just(clientCredentialsResponseDto));
        when(iniPecClient.getToken()).thenReturn(Mono.just(clientCredentialsResponseDto));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ResponsePecIniPec.class)).thenReturn(Mono.just(response));

        StepVerifier.create(iniPecClient.callEServiceRequestPec(request)).expectNext(response).verifyComplete();

    }

    @Test
    void testGetLegalAddress() {
        when(iniPecWebClient.init()).thenReturn(webClient);
        IniPecClient iniPecClient = new IniPecClient(iniPecWebClient,iniPecJwsGenerator, mapper);

        String request = "taxId";
        AddressRegistroImpreseResponse response = new AddressRegistroImpreseResponse();
        response.setAddress(new LegalAddress());
        response.setTaxId("taxId");
        response.setDate(LocalDateTime.now().toString());

        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("accessToken");
        clientCredentialsResponseDto.setTokenType(TokenTypeDto.BEARER);
        clientCredentialsResponseDto.setExpiresIn(10);

        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClientCredentialsResponseDto.class)).thenReturn(Mono.just(clientCredentialsResponseDto));
        when(iniPecClient.getToken()).thenReturn(Mono.just(clientCredentialsResponseDto));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AddressRegistroImpreseResponse.class)).thenReturn(Mono.just(response));

        StepVerifier.create(iniPecClient.getLegalAddress(request)).expectNext(response).verifyComplete();

    }


    @Test
    void checkExceptionTypeWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        IniPecClient iniPecClient = new IniPecClient(iniPecWebClient,iniPecJwsGenerator, mapper);
        WebClientResponseException webClientResponseException =
                new WebClientResponseException(
                        "message",
                        HttpStatus.UNAUTHORIZED.value(),
                        "statusText",
                        HttpHeaders.EMPTY,
                        null,
                        null);
        assertTrue(iniPecClient.checkExceptionType(webClientResponseException));
    }

    @Test
    void checkExceptionTypeWhenNotWebClientResponseExceptionThenReturnFalse() {
        IniPecClient iniPecClient = new IniPecClient(iniPecWebClient,iniPecJwsGenerator, mapper);

        assertFalse(iniPecClient.checkExceptionType(new Exception()));
    }
}

