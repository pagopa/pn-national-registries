package it.pagopa.pn.national.registries.client.agenziaentrate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;

import it.pagopa.pn.national.registries.config.adecheckcf.CheckCfSecretConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.api.VerificheApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.dto.Richiesta;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.dto.VerificaCodiceFiscale;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.TokenType;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class CheckCfClientTest {

    @MockBean
    AccessTokenExpiringMap accessTokenExpiringMap;

    @MockBean
    WebClient webClient;

    @MockBean
    VerificheApi verificheApi;

    @MockBean
    ObjectMapper objectMapper;

    @MockBean
    CheckCfSecretConfig checkCfSecretConfig;

    @MockBean
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @Test
    void callEService() throws JsonProcessingException {
        CheckCfClient checkCfClient = new CheckCfClient(accessTokenExpiringMap, verificheApi, checkCfSecretConfig, pnNationalRegistriesSecretService);
        Richiesta richiesta = new Richiesta();
        richiesta.setCodiceFiscale("cf");

        String richiestaJson = "{\"codiceFiscale\": \"cf\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(richiestaJson);

        VerificaCodiceFiscale taxIdVerification = new VerificaCodiceFiscale();
        taxIdVerification.setCodiceFiscale("cf");
        taxIdVerification.setValido(true);
        taxIdVerification.setMessaggio("valid");

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenType.BEARER);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(verificheApi.getApiClient()).thenReturn(mock(ApiClient.class));
        when(verificheApi.postVerificaCodiceFiscale(any())).thenReturn(Mono.just(taxIdVerification));

        PdndSecretValue value = new PdndSecretValue();
        value.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(value);
        StepVerifier.create(checkCfClient.callEService(richiesta))
                .expectNext(taxIdVerification)
                .verifyComplete();
    }

    @Test
    void checkTaxIdAndVatNumberErrorTest() throws JsonProcessingException {
        CheckCfClient checkCfClient = new CheckCfClient(accessTokenExpiringMap, verificheApi, checkCfSecretConfig, pnNationalRegistriesSecretService);
        HttpHeaders headers = mock(HttpHeaders.class);
        byte[] testByteArray = new byte[0];
        String test = "test";
        WebClientResponseException webClientResponseException = new WebClientResponseException(test, 500, test, headers, testByteArray, Charset.defaultCharset());

        Richiesta richiesta = new Richiesta();
        richiesta.setCodiceFiscale("cf");

        String richiestaJson = "{\"codiceFiscale\": \"cf\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(richiestaJson);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenType.BEARER);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(verificheApi.getApiClient()).thenReturn(mock(ApiClient.class));
        when(verificheApi.postVerificaCodiceFiscale(any())).thenReturn(Mono.error(webClientResponseException));

        PdndSecretValue value = new PdndSecretValue();
        value.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(value);
        StepVerifier.create(checkCfClient.callEService(richiesta))
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    @DisplayName("Should return false when the exception is not webclientresponseexception")
    void shouldRetryWhenNotWebClientResponseExceptionThenReturnFalse() {
        CheckCfClient checkCfClient = new CheckCfClient(accessTokenExpiringMap, verificheApi, checkCfSecretConfig, pnNationalRegistriesSecretService);
        assertFalse(checkCfClient.shouldRetry(new Exception()));
    }

    @Test
    @DisplayName("Should return true when the exception is webclientresponseexception and the status code is 401")
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        CheckCfClient checkCfClient = new CheckCfClient(accessTokenExpiringMap, verificheApi, checkCfSecretConfig, pnNationalRegistriesSecretService);
        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.UNAUTHORIZED.value(), "statusText", HttpHeaders.EMPTY, null, null);
        assertTrue(checkCfClient.shouldRetry(webClientResponseException));
    }
}
