package it.pagopa.pn.national.registries.client.infocamere;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.api.AuthenticationApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfoCamereTokenClientTest {

    @InjectMocks
    private InfoCamereTokenClient infoCamereTokenClient;

    @Mock
    private InfoCamereJwsGenerator infoCamereJwsGenerator;

    @Mock
    AuthenticationApi authenticationApi;

    @Mock
    NationalRegistriesConfig nationalRegistriesConfig;


    /**
     * Method under test: {@link InfoCamereTokenClient#getToken(String)}
     */
    @Test
    void testGetToken2(){
        HttpHeaders headers = new HttpHeaders();
        when(infoCamereJwsGenerator.createAuthRest(any()))
                .thenThrow(new WebClientResponseException(400, "Status Text", headers, "AAAAAAAA".getBytes(StandardCharsets.UTF_8), null));
        assertThrows(WebClientResponseException.class, () -> infoCamereTokenClient.getToken("Scope"));
        verify(infoCamereJwsGenerator).createAuthRest(any());
    }

    @Test
    void callGetTokenTest() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        when(authenticationApi.getToken(any(), any())).thenReturn(Mono.just("token"));

        String scope = "test_scope";
        String jws = "jws";

        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        when(authenticationApi.getToken(any(), any())).thenReturn(Mono.just(jws));

        StepVerifier.create(infoCamereTokenClient.getToken(scope))
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
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        String scope = "test_scope";
        when(authenticationApi.getToken(any(), any())).thenReturn(Mono.just("token"));

        String jws = "jws";
        WebClientResponseException ex = buildException();
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);
        when(authenticationApi.getToken(any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(infoCamereTokenClient.getToken(scope)).expectError(PnNationalRegistriesException.class).verify();
    }

    @Test
    void testGetTokenWebUnauthorizedException() {
        NationalRegistriesConfig.InfoCamere infoCamereConfig = new NationalRegistriesConfig.InfoCamere();
        when(nationalRegistriesConfig.getInfoCamere()).thenReturn(infoCamereConfig);
        String scope = "test_scope";
        when(authenticationApi.getToken(any(), any())).thenReturn(Mono.just("token"));

        String jws = "jws";
        when(infoCamereJwsGenerator.createAuthRest(any())).thenReturn(jws);

        WebClientResponseException exception = new WebClientResponseException(HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), null, null, null);
        when(authenticationApi.getToken(any(), any())).thenReturn(Mono.error(exception));

        StepVerifier.create(infoCamereTokenClient.getToken(scope)).expectError(PnInternalException.class).verify();
    }
}

