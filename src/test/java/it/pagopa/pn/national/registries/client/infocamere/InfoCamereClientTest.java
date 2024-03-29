package it.pagopa.pn.national.registries.client.infocamere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.InfoCamereLegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.TokenTypeDto;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereLegalInstituionsResponse;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerification;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchResponse;
import it.pagopa.pn.national.registries.model.inipec.IniPecPollingResponse;
import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImprese;
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

    @Mock
    WebClient webClient;

    @Mock
    InfoCamereWebClient infoCamereWebClient;

    @Mock
    AccessTokenExpiringMap accessTokenExpiringMap;

    @Mock
    ObjectMapper mapper;

    final String clientId = "tezt_clientId";

    private static WebClient.RequestBodyUriSpec requestBodyUriSpec;
    private static WebClient.RequestBodySpec requestBodySpec;
    private static WebClient.RequestHeadersSpec requestHeadersSpec;
    private static WebClient.ResponseSpec responseSpec;
    private static WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @BeforeAll
    static void setup() {
        requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(WebClient.RequestBodySpec.class);
        requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);
        requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
    }

    @Test
    void testGetLegalInstitutions() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, accessTokenExpiringMap, mapper);

        CheckTaxIdRequestBodyFilterDto checkTaxIdRequestBodyFilterDto = new CheckTaxIdRequestBodyFilterDto();
        checkTaxIdRequestBodyFilterDto.setTaxId("taxId");

        InfoCamereLegalInstituionsResponse infoCamereLegalInstituionsResponse = new InfoCamereLegalInstituionsResponse();


        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");
        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(InfoCamereLegalInstituionsResponse.class)).thenReturn(Mono.just(infoCamereLegalInstituionsResponse));
        StepVerifier.create(infoCamereClient.getLegalInstitutions(checkTaxIdRequestBodyFilterDto)).expectNext(infoCamereLegalInstituionsResponse).verifyComplete();
    }


    @Test
    void testCallEServiceRequestId() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, accessTokenExpiringMap, mapper);

        IniPecBatchRequest.IniPecCf iniPecCf = new IniPecBatchRequest.IniPecCf();
        iniPecCf.setCf("taxId");
        IniPecBatchRequest request = new IniPecBatchRequest();
        request.setDataOraRichiesta(LocalDateTime.now().toString());
        request.setElencoCf(List.of(iniPecCf));

        IniPecBatchResponse iniPecBatchResponse = new IniPecBatchResponse();
        iniPecBatchResponse.setIdentificativoRichiesta("correlationId");
        iniPecBatchResponse.setDataOraRichiesta(LocalDateTime.now().toString());

        String requestJson = "requestJson";
        try {
            when(mapper.writeValueAsString(request)).thenReturn(requestJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(IniPecBatchResponse.class)).thenReturn(Mono.just(iniPecBatchResponse));
        StepVerifier.create(infoCamereClient.callEServiceRequestId(request)).expectNext(iniPecBatchResponse).verifyComplete();
    }

    @Test
    void testCallEServiceRequestIdWebException() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, accessTokenExpiringMap, mapper);
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        IniPecBatchRequest.IniPecCf iniPecCf = new IniPecBatchRequest.IniPecCf();
        iniPecCf.setCf("taxId");
        IniPecBatchRequest request = new IniPecBatchRequest();
        request.setDataOraRichiesta(LocalDateTime.now().toString());
        request.setElencoCf(List.of(iniPecCf));

        String requestJson = "requestJson";
        try {
            when(mapper.writeValueAsString(request)).thenReturn(requestJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        //    callGetTokenTest();

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        WebClientResponseException exception = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(responseSpec.bodyToMono(IniPecBatchResponse.class)).thenReturn(Mono.error(exception));

        StepVerifier.create(infoCamereClient.callEServiceRequestId(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testCallEServiceRequestPec() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, accessTokenExpiringMap, mapper);
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        String request = "correlationId";
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setIdentificativoRichiesta("correlationId");

        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("token");
        clientCredentialsResponseDto.setTokenType(TokenTypeDto.BEARER);
        clientCredentialsResponseDto.setExpiresIn(10);

        //     callGetTokenTest();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(IniPecPollingResponse.class)).thenReturn(Mono.just(response));

        StepVerifier.create(infoCamereClient.callEServiceRequestPec(request)).expectNext(response).verifyComplete();
    }

    @Test
    void testCallEServiceRequestPecWebException() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, accessTokenExpiringMap, mapper);
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        String request = "correlationId";

        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setAccessToken("accessToken");
        clientCredentialsResponseDto.setTokenType(TokenTypeDto.BEARER);
        clientCredentialsResponseDto.setExpiresIn(10);
        String jws = "jws";

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jws));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        WebClientResponseException exception = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(responseSpec.bodyToMono(IniPecPollingResponse.class)).thenReturn(Mono.error(exception));

        StepVerifier.create(infoCamereClient.callEServiceRequestPec(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void testGetLegalAddress() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, accessTokenExpiringMap, mapper);

        String request = "taxId";
        AddressRegistroImprese response = new AddressRegistroImprese();
        response.setAddress(new LegalAddress());
        response.setTaxId("taxId");

        String jws = "jws";
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials(jws);

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpecLA = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpecLA = mock(WebClient.ResponseSpec.class);

        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any()))
                .thenReturn(requestBodySpecLA);
        when(requestBodySpec.headers(any()))
                .thenReturn(requestBodySpecLA);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jws));
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(requestBodySpecLA.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecLA);
        when(requestBodySpecLA.headers(any())).thenReturn(requestBodySpecLA);
        when(responseSpecLA.bodyToMono(AddressRegistroImprese.class)).thenReturn(Mono.just(response));
        when(requestBodySpecLA.retrieve()).thenReturn(responseSpecLA);
        when(webClient.post()).thenReturn(requestBodyUriSpec);

        StepVerifier.create(infoCamereClient.getLegalAddress(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testGetLegalAddressWebException() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, accessTokenExpiringMap, mapper);

        String request = "taxId";

        String jws = "jws";
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpecLA = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpecLA = mock(WebClient.ResponseSpec.class);

        when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any()))
                .thenReturn(requestBodySpecLA);
        when(requestBodySpec.headers(any()))
                .thenReturn(requestBodySpecLA);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jws));
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(requestBodySpecLA.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecLA);
        when(requestBodySpecLA.headers(any())).thenReturn(requestBodySpecLA);
        WebClientResponseException exception = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        when(responseSpecLA.bodyToMono(AddressRegistroImprese.class)).thenReturn(Mono.error(exception));
        when(requestBodySpecLA.retrieve()).thenReturn(responseSpecLA);

        when(webClient.post())
                .thenReturn(requestBodyUriSpec);

        StepVerifier.create(infoCamereClient.getLegalAddress(request))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }
    @Test
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, accessTokenExpiringMap, mapper);
        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.UNAUTHORIZED.value(), "statusText", HttpHeaders.EMPTY, null, null);
        assertTrue(infoCamereClient.shouldRetry(webClientResponseException));
    }

    @Test
    void shouldRetryWhenNotWebClientResponseExceptionThenReturnFalse() {
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, accessTokenExpiringMap, mapper);

        assertFalse(infoCamereClient.shouldRetry(new Exception()));
    }

    @Test
    void testCheckTaxIdAndVatNumberInfoCamere() {
        when(infoCamereWebClient.init()).thenReturn(webClient);
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, accessTokenExpiringMap, mapper);

        InfoCamereLegalRequestBodyFilterDto filterDto = new InfoCamereLegalRequestBodyFilterDto();
        InfoCamereVerification response = new InfoCamereVerification();
        response.setTaxId("taxId");

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
        WebClient.RequestBodyUriSpec requestBodyUriSpecToken = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpecToken = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestBodySpec requestBodySpecIC = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpecToken = mock(WebClient.ResponseSpec.class);
        WebClient.ResponseSpec responseSpecIC = mock(WebClient.ResponseSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecToken = mock(WebClient.RequestHeadersSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecIC = mock(WebClient.RequestHeadersSpec.class);
        WebClient.RequestHeadersUriSpec requestHeadersUriSpecIC = mock(WebClient.RequestHeadersUriSpec.class);
        String jws = "jws";

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
        when(responseSpecIC.bodyToMono(InfoCamereVerification.class)).thenReturn(Mono.just(response));
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
        InfoCamereClient infoCamereClient = new InfoCamereClient(infoCamereWebClient, clientId, accessTokenExpiringMap, mapper);

        InfoCamereLegalRequestBodyFilterDto filterDto = new InfoCamereLegalRequestBodyFilterDto();
        String jws = "jws";
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("jws");

        when(accessTokenExpiringMap.getInfoCamereToken(any())).thenReturn(Mono.just(accessTokenCacheEntry));
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
        when(responseSpecIC.bodyToMono(InfoCamereVerification.class)).thenReturn(Mono.error(exception));
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