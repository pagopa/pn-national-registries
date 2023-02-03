package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.config.infocamere.InfoCamereSecretConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.InfoCamereLegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerificationResponse;
import it.pagopa.pn.national.registries.model.inipec.RequestCfIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.model.registroimprese.LegalAddress;
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
import java.util.List;
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
    void callGetTokenTest() {
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
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jws));

        StepVerifier.create(infoCamereClient.getToken(scope)).expectError();
    }

    @Test
    void testGetTokenWebException() {
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
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        WebClientResponseException exception = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(exception));

        StepVerifier.create(infoCamereClient.getToken(scope)).expectError(PnNationalRegistriesException.class).verify();
    }

    @Test
    void testCallEServiceRequestId() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        RequestCfIniPec request = new RequestCfIniPec();
        request.setDataOraRichiesta(LocalDateTime.now().toString());
        request.setElencoCf(List.of("taxId"));

        ResponsePollingIdIniPec responsePollingIdIniPec = new ResponsePollingIdIniPec();
        responsePollingIdIniPec.setIdentificativoRichiesta("correlationId");
        responsePollingIdIniPec.setDataOraRichiesta(LocalDateTime.now().toString());

        String requestJson = "requestJson";
        try {
            when(mapper.writeValueAsString(request)).thenReturn(requestJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

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
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jws));

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
    void testCallEServiceRequestIdWebException() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        RequestCfIniPec request = new RequestCfIniPec();
        request.setDataOraRichiesta(LocalDateTime.now().toString());
        request.setElencoCf(List.of("taxId"));

        String requestJson = "requestJson";
        try {
            when(mapper.writeValueAsString(request)).thenReturn(requestJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

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
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jws));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/richiestaElencoPec")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        WebClientResponseException exception = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(responseSpec.bodyToMono(ResponsePollingIdIniPec.class)).thenReturn(Mono.error(exception));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testCallEServiceRequestPec() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        String request = "correlationId";
        ResponsePecIniPec response = new ResponsePecIniPec();
        response.setIdentificativoRichiesta("correlationId");

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
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jws));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ResponsePecIniPec.class)).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereClient.callEServiceRequestPec(request)).expectNext(response).verifyComplete();
    }

    @Test
    void testCallEServiceRequestPecWebException() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        String request = "correlationId";

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
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jws));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        WebClientResponseException exception = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(responseSpec.bodyToMono(ResponsePecIniPec.class)).thenReturn(Mono.error(exception));

        StepVerifier.create(infoCamereClient.callEServiceRequestPec(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testGetLegalAddress() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        String request = "taxId";
        AddressRegistroImpreseResponse response = new AddressRegistroImpreseResponse();
        response.setAddress(new LegalAddress());
        response.setTaxId("taxId");

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpecToken = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestBodySpec requestBodySpecLA = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpecToken = mock(WebClient.ResponseSpec.class);
        WebClient.ResponseSpec responseSpecLA = mock(WebClient.ResponseSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecToken = mock(WebClient.RequestHeadersSpec.class);

        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any()))
                .thenReturn(requestBodySpecToken)
                        .thenReturn(requestBodySpecLA);
        when(requestBodySpecToken.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecToken);
        when(requestBodySpecToken.headers(any()))
                .thenReturn(requestBodySpecToken)
                .thenReturn(requestBodySpecLA);
        when(requestBodySpecToken.bodyValue(any())).thenReturn(requestHeadersSpecToken);
        when(responseSpecToken.bodyToMono(String.class)).thenReturn(Mono.just(jws));
        when(requestHeadersSpecToken.retrieve()).thenReturn(responseSpecToken);

        when(requestBodySpecLA.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecLA);
        when(requestBodySpecLA.headers(any())).thenReturn(requestBodySpecLA);
        when(responseSpecLA.bodyToMono(AddressRegistroImpreseResponse.class)).thenReturn(Mono.just(response));
        when(requestBodySpecLA.retrieve()).thenReturn(responseSpecLA);

        when(webClient.post())
                .thenReturn(requestBodyUriSpec);

        StepVerifier.create(infoCamereClient.getLegalAddress(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testGetLegalAddressWebException() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        String request = "taxId";

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpecToken = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestBodySpec requestBodySpecLA = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpecToken = mock(WebClient.ResponseSpec.class);
        WebClient.ResponseSpec responseSpecLA = mock(WebClient.ResponseSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecToken = mock(WebClient.RequestHeadersSpec.class);

        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any()))
                .thenReturn(requestBodySpecToken)
                .thenReturn(requestBodySpecLA);
        when(requestBodySpecToken.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecToken);
        when(requestBodySpecToken.headers(any()))
                .thenReturn(requestBodySpecToken)
                .thenReturn(requestBodySpecLA);
        when(requestBodySpecToken.bodyValue(any())).thenReturn(requestHeadersSpecToken);
        when(responseSpecToken.bodyToMono(String.class)).thenReturn(Mono.just(jws));
        when(requestHeadersSpecToken.retrieve()).thenReturn(responseSpecToken);

        when(requestBodySpecLA.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecLA);
        when(requestBodySpecLA.headers(any())).thenReturn(requestBodySpecLA);
        WebClientResponseException exception = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(responseSpecLA.bodyToMono(AddressRegistroImpreseResponse.class)).thenReturn(Mono.error(exception));
        when(requestBodySpecLA.retrieve()).thenReturn(responseSpecLA);

        when(webClient.post())
                .thenReturn(requestBodyUriSpec);

        StepVerifier.create(infoCamereClient.getLegalAddress(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
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

    @Test
    void testCheckTaxIdAndVatNumberInfoCamere() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        InfoCamereLegalRequestBodyFilterDto filterDto = new InfoCamereLegalRequestBodyFilterDto();
        InfoCamereVerificationResponse response = new InfoCamereVerificationResponse();
        response.setTaxId("taxId");

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        WebClient.RequestBodyUriSpec requestBodyUriSpecToken = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpecToken = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestBodySpec requestBodySpecIC = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpecToken = mock(WebClient.ResponseSpec.class);
        WebClient.ResponseSpec responseSpecIC = mock(WebClient.ResponseSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecToken = mock(WebClient.RequestHeadersSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecIC = mock(WebClient.RequestHeadersSpec.class);
        WebClient.RequestHeadersUriSpec requestHeadersUriSpecIC = mock(WebClient.RequestHeadersUriSpec.class);

        when(requestBodyUriSpecToken.uri((Function<UriBuilder, URI>) any()))
                .thenReturn(requestBodySpecToken);
        when(requestBodySpecToken.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecToken);
        when(requestBodySpecToken.headers(any())).thenReturn(requestBodySpecToken);
        when(requestBodySpecToken.bodyValue(any())).thenReturn(requestHeadersSpecToken);
        when(responseSpecToken.bodyToMono(String.class)).thenReturn(Mono.just(jws));
        when(requestHeadersSpecToken.retrieve()).thenReturn(responseSpecToken);

        when(requestHeadersUriSpecIC.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpecIC);
        when(requestBodySpecIC.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecIC);
        when(requestHeadersSpecIC.headers(any())).thenReturn(requestBodySpecIC);
        when(responseSpecIC.bodyToMono(InfoCamereVerificationResponse.class)).thenReturn(Mono.just(response));
        when(requestBodySpecIC.retrieve()).thenReturn(responseSpecIC);

        when(webClient.post())
                .thenReturn(requestBodyUriSpecToken);
        when(webClient.get())
                .thenReturn(requestHeadersUriSpecIC);

        StepVerifier.create(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(filterDto))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testCheckTaxIdAndVatNumberInfoCamereWebClient() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        InfoCamereLegalRequestBodyFilterDto filterDto = new InfoCamereLegalRequestBodyFilterDto();

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        WebClient.RequestBodyUriSpec requestBodyUriSpecToken = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpecToken = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestBodySpec requestBodySpecIC = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpecToken = mock(WebClient.ResponseSpec.class);
        WebClient.ResponseSpec responseSpecIC = mock(WebClient.ResponseSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecToken = mock(WebClient.RequestHeadersSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecIC = mock(WebClient.RequestHeadersSpec.class);
        WebClient.RequestHeadersUriSpec requestHeadersUriSpecIC = mock(WebClient.RequestHeadersUriSpec.class);

        when(requestBodyUriSpecToken.uri((Function<UriBuilder, URI>) any()))
                .thenReturn(requestBodySpecToken);
        when(requestBodySpecToken.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecToken);
        when(requestBodySpecToken.headers(any())).thenReturn(requestBodySpecToken);
        when(requestBodySpecToken.bodyValue(any())).thenReturn(requestHeadersSpecToken);
        when(responseSpecToken.bodyToMono(String.class)).thenReturn(Mono.just(jws));
        when(requestHeadersSpecToken.retrieve()).thenReturn(responseSpecToken);

        when(requestHeadersUriSpecIC.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpecIC);
        when(requestBodySpecIC.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecIC);
        when(requestHeadersSpecIC.headers(any())).thenReturn(requestBodySpecIC);
        WebClientResponseException exception = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(responseSpecIC.bodyToMono(InfoCamereVerificationResponse.class)).thenReturn(Mono.error(exception));
        when(requestBodySpecIC.retrieve()).thenReturn(responseSpecIC);

        when(webClient.post())
                .thenReturn(requestBodyUriSpecToken);
        when(webClient.get())
                .thenReturn(requestHeadersUriSpecIC);

        StepVerifier.create(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(filterDto))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }
}