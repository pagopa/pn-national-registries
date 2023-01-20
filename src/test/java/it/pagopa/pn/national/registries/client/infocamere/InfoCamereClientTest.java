package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.config.infocamere.InfoCamereSecretConfig;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import it.pagopa.pn.national.registries.model.inipec.RequestCfIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
import it.pagopa.pn.national.registries.model.registroImprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.model.registroImprese.LegalAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reactivestreams.Publisher;
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
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class InfoCamereClientTest {

    @MockBean
    WebClient webClient;

    @MockBean
    InfoCamereJwsGenerator infoCamereJwsGenerator;

    @MockBean
    InfoCamereWebClient infoCamereWebClient;

    @MockBean
    InfoCamereSecretConfig infoCamereSecretConfig;

    @MockBean
    ObjectMapper mapper;
    String clientId = "tezt_clientId";

    @Test
    void callgetTokenTest() {

        String scope = "test_scope";
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        ClientCredentialsResponseDto response = new ClientCredentialsResponseDto();
        response.setAccessToken("token");

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);

        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body((Publisher<Object>) any(), (Class<Object>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClientCredentialsResponseDto.class)).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereClient.getToken(scope)).expectError();

    }

    @Test
    void testCallEServiceRequestId(){
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

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

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body((Publisher<Object>) any(), (Class<Object>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClientCredentialsResponseDto.class)).thenReturn(Mono.just(clientCredentialsResponseDto));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/richiestaElencoPec")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ResponsePollingIdIniPec.class)).thenReturn(Mono.just(responsePollingIdIniPec));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(request)).expectNext(responsePollingIdIniPec).verifyComplete();
    }

    @Test
    void testCallEServiceRequestPec() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        String request = "correlationId";
        ResponsePecIniPec response = new ResponsePecIniPec();
        response.setIdentificativoRichiesta("correlationId");

        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("token");
        clientCredentialsResponseDto.setTokenType(TokenTypeDto.BEARER);
        clientCredentialsResponseDto.setExpiresIn(10);

        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body((Publisher<Object>) any(), (Class<Object>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClientCredentialsResponseDto.class)).thenReturn(Mono.just(clientCredentialsResponseDto));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ResponsePecIniPec.class)).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereClient.callEServiceRequestPec(request)).expectNext(response).verifyComplete();

    }

    @Test
    void testGetLegalAddress() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        String request = "taxId";
        AddressRegistroImpreseResponse response = new AddressRegistroImpreseResponse();
        response.setAddress(new LegalAddress());
        response.setTaxId("taxId");

        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("accessToken");
        clientCredentialsResponseDto.setTokenType(TokenTypeDto.BEARER);
        clientCredentialsResponseDto.setExpiresIn(10);

        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body((Publisher<Object>) any(), (Class<Object>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClientCredentialsResponseDto.class)).thenReturn(Mono.just(clientCredentialsResponseDto));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AddressRegistroImpreseResponse.class)).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereClient.getLegalAddress(request)).expectError();

    }


    @Test
    void checkExceptionTypeWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);
        WebClientResponseException webClientResponseException =
                new WebClientResponseException(
                        "message",
                        HttpStatus.UNAUTHORIZED.value(),
                        "statusText",
                        HttpHeaders.EMPTY,
                        null,
                        null);
        assertTrue(infoCamereClient.checkExceptionType(webClientResponseException));
    }

    @Test
    void checkExceptionTypeWhenNotWebClientResponseExceptionThenReturnFalse() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        assertFalse(infoCamereClient.checkExceptionType(new Exception()));
    }
}