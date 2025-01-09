package it.pagopa.pn.national.registries.client.infocamere;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.AuthenticationApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {InfoCamereTokenClient.class, String.class})
@ExtendWith(SpringExtension.class)
class InfoCamereTokenClientTest {
    @Autowired
    private InfoCamereTokenClient infoCamereTokenClient;

    @MockBean
    private InfoCamereJwsGenerator infoCamereJwsGenerator;

    @MockBean
    AuthenticationApi authenticationApi;

    final String clientId = "tezt_clientId";
    PnAuditLogEventType type = PnAuditLogEventType.AUD_NR_PF_PHYSICAL;
    Map<String, String> mdc = new HashMap<>();
    String message = "message";
    Object[] arguments = new Object[] {"arg1", "arg2"};
    PnAuditLogEvent logEvent;

    @BeforeEach
    public void setup() {

        mdc.put("key", "value");
        logEvent = new PnAuditLogEvent(type, mdc, message, arguments);
        ApiClient apiClient = mock(ApiClient.class);
        authenticationApi = mock(AuthenticationApi.class);

        doNothing().when(apiClient).setBearerToken(anyString());
        when(apiClient.addDefaultHeader(anyString(), anyString())).thenReturn(apiClient);

        when(authenticationApi.getApiClient()).thenReturn(apiClient);
    }
    /**
     * Method under test: {@link InfoCamereTokenClient#getToken(String)}
     */
    @Test
    void testGetToken2(){
        HttpHeaders headers = new HttpHeaders();
        when(infoCamereJwsGenerator.createAuthRest(any()))
                .thenThrow(new WebClientResponseException(1, "Status Text", headers, "AAAAAAAA".getBytes(StandardCharsets.UTF_8), null));
        assertThrows(WebClientResponseException.class, () -> infoCamereTokenClient.getToken("Scope", logEvent));
        verify(infoCamereJwsGenerator).createAuthRest(any());
    }

    @Test
    void callGetTokenTest() {
        InfoCamereTokenClient infoCamereTokenClient1 = new InfoCamereTokenClient(clientId, infoCamereJwsGenerator, authenticationApi);

        String scope = "test_scope";
        String jws = "jws";

        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        when(authenticationApi.getToken(any(), any())).thenReturn(Mono.just(jws));

        StepVerifier.create(infoCamereTokenClient1.getToken(scope, logEvent))
                .expectNext(jws)
                .verifyComplete();
    }

    public WebClientResponseException buildException() {
        HttpHeaders headers = mock(HttpHeaders.class);
        byte[] testByteArray = new byte[0];
        String test = "test";
        return new WebClientResponseException(test, 500, test, headers, testByteArray, Charset.defaultCharset());
    }

    @Test
    void testGetTokenWebException() {
        String scope = "test_scope";
        InfoCamereTokenClient infoCamereTokenClient1 = new InfoCamereTokenClient(clientId, infoCamereJwsGenerator, authenticationApi);

        String jws = "jws";
        WebClientResponseException ex = buildException();
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);
        when(authenticationApi.getToken(any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereTokenClient1.getToken(scope, logEvent)).expectError(PnNationalRegistriesException.class).verify();
    }

    @Test
    void testGetTokenWebUnauthorizedException() {
        String scope = "test_scope";
        InfoCamereTokenClient infoCamereTokenClient1 = new InfoCamereTokenClient(clientId, infoCamereJwsGenerator, authenticationApi);

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        WebClientResponseException exception = new WebClientResponseException(HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), null, null, null);
        when(authenticationApi.getToken(any(), any())).thenReturn(Mono.error(exception));

        StepVerifier.create(infoCamereTokenClient1.getToken(scope, logEvent)).expectError(PnInternalException.class).verify();
    }
}

