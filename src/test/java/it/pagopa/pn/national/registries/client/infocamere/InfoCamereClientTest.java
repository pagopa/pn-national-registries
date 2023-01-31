package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.InfoCamereLegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerificationResponse;
import it.pagopa.pn.national.registries.model.inipec.RequestCfIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePollingIdIniPec;
import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.model.registroimprese.LegalAddress;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
import java.time.Instant;
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
class InfoCamereClientTest {

    @Mock
    WebClient webClient;

    @Mock
    InfoCamereJwsGenerator infoCamereJwsGenerator;

    @Mock
    InfoCamereWebClient infoCamereWebClient;

    @Mock
    ObjectMapper mapper;
    String clientId = "tezt_clientId";

    private static WebClient.RequestBodyUriSpec requestBodyUriSpec;
    private static WebClient.RequestBodySpec requestBodySpec;
    private static WebClient.RequestHeadersSpec requestHeadersSpec;
    private static WebClient.ResponseSpec responseSpec;
    private static WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @BeforeAll
    static void setup(){
        requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(WebClient.RequestBodySpec.class);
        requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);
        requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
    }

    @Test
    void callGetTokenTest() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        String scope = "test_scope";
        String jws = "jws";

        ClientCredentialsResponseDto response = new ClientCredentialsResponseDto();
        response.setAccessToken("token");

        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jws));

        StepVerifier.create(infoCamereClient.getToken(scope)).expectNext(jws).verifyComplete();
    }

    @Test
    void testCallEServiceRequestId() {
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

        callGetTokenTest();

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

        callGetTokenTest();

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

        callGetTokenTest();

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AddressRegistroImpreseResponse.class)).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereClient.getLegalAddress(request)).expectNext(response).verifyComplete();
    }

    @Test
    void testCheckTaxIdAndVatNumberInfoCamere() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, infoCamereJwsGenerator, mapper);

        InfoCamereLegalRequestBodyFilterDto filter = new InfoCamereLegalRequestBodyFilterDto();
        filter.setTaxId("taxId");
        filter.setVatNumber("vatNumber");

        InfoCamereVerificationResponse infoCamereVerificationResponse = new InfoCamereVerificationResponse();
        infoCamereVerificationResponse.setTaxId("taxId");
        infoCamereVerificationResponse.setVatNumber("vatNumber");
        infoCamereVerificationResponse.setVerificationResult(true);
        infoCamereVerificationResponse.setDateTimeExtraction(Date.from(Instant.now()).toString());

        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("accessToken");
        clientCredentialsResponseDto.setTokenType(TokenTypeDto.BEARER);
        clientCredentialsResponseDto.setExpiresIn(10);

        callGetTokenTest();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(InfoCamereVerificationResponse.class)).thenReturn(Mono.just(infoCamereVerificationResponse));

        StepVerifier.create(infoCamereClient.checkTaxIdAndVatNumberInfoCamere(filter)).expectNext(infoCamereVerificationResponse).verifyComplete();
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