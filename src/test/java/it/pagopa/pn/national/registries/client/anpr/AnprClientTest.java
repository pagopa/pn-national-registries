package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.cache.AccessTokenCacheEntry;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.api.E002ServiceApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RichiestaE002;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RispostaE002OK;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoCriteriRicercaE002;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoListaSoggetti;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.TokenType;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Incubating;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnprClientTest {

    @InjectMocks
    AnprClient anprClient;

    @Mock
    AccessTokenExpiringMap accessTokenExpiringMap;

    @Mock
    WebClient webClient;

    @Mock
    AgidJwtSignature agidJwtSignature;
    
    @Mock
    AgidJwtTrackingEvidence agidJwtTrackingEvidence;

    @Mock
    E002ServiceApi e002ServiceApi;

    @Mock
    ApiClient apiClient;

    @Mock
    NationalRegistriesConfig nationalRegistriesConfig;

    @Mock
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    NationalRegistriesConfig.Anpr anpr;

    @BeforeEach
    void setUp() {
        anpr = new NationalRegistriesConfig.Anpr();
        anpr.setBaseUrl("http://example.com");
        anpr.setTable("pn-Counter");
        anpr.setTrustSecret("testTrustSecret");
        anpr.setPdndClientSecret("testPdndClientSecret");
    }

    @Test
    void shouldRetryWhenNotWebClientResponseExceptionThenReturnFalse() {
        AnprClient anprClient = new AnprClient(accessTokenExpiringMap,agidJwtSignature, e002ServiceApi, agidJwtTrackingEvidence,
                nationalRegistriesConfig, pnNationalRegistriesSecretService);
        assertFalse(anprClient.shouldRetry(new Exception()));
    }

    @Test
    void shouldRetryWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.UNAUTHORIZED.value(), "statusText", HttpHeaders.EMPTY, null, null);
        assertTrue(anprClient.shouldRetry(webClientResponseException));
    }

    @Test
    void callEService() {
        when(nationalRegistriesConfig.getAnpr()).thenReturn(anpr);
        RichiestaE002 richiestaE002 = new RichiestaE002();
        TipoCriteriRicercaE002 dto = new TipoCriteriRicercaE002();
        dto.setCodiceFiscale("DDDFFF52G52H501H");
        richiestaE002.setCriteriRicerca(dto);

        RispostaE002OK rispostaE002OKDto = new RispostaE002OK();
        rispostaE002OKDto.setListaSoggetti(new TipoListaSoggetti());

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenType.BEARER);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(agidJwtTrackingEvidence.createAgidJwt()).thenReturn("testJws");

        when(e002ServiceApi.e002(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(rispostaE002OKDto));

        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        pdndSecretValue.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(pdndSecretValue);
        StepVerifier.create(anprClient.callEService(richiestaE002))
                .expectNext(rispostaE002OKDto)
                .verifyComplete();
    }

    @Test
    void callEService2() {
        when(nationalRegistriesConfig.getAnpr()).thenReturn(anpr);

        RichiestaE002 richiestaE002 = new RichiestaE002();
        TipoCriteriRicercaE002 dto = new TipoCriteriRicercaE002();
        dto.setCodiceFiscale("DDDFFF52G52H501H");
        richiestaE002.setCriteriRicerca(dto);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenType.BEARER);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(agidJwtTrackingEvidence.createAgidJwt()).thenReturn("testJws");
        when(e002ServiceApi.e002(any(), any(), any(), any(), any(), any())).thenThrow(mock(PnNationalRegistriesException.class));
        PdndSecretValue pdndSecretValue = new PdndSecretValue();
        pdndSecretValue.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(pdndSecretValue);

        StepVerifier.create(anprClient.callEService(richiestaE002))
                .verifyError(PnNationalRegistriesException.class);
    }

    @Test
    void callEService3() {
        when(nationalRegistriesConfig.getAnpr()).thenReturn(anpr);

        RichiestaE002 richiestaE002 = new RichiestaE002();
        TipoCriteriRicercaE002 dto = new TipoCriteriRicercaE002();
        dto.setCodiceFiscale("DDDFFF52G52H501H");
        richiestaE002.setCriteriRicerca(dto);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenType.BEARER);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(agidJwtTrackingEvidence.createAgidJwt()).thenReturn("testJws");
        when(e002ServiceApi.e002(any(), any(), any(), any(), any(), any())).thenThrow(mock(PnInternalException.class));

        PdndSecretValue secret = new PdndSecretValue();
        secret.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(secret);
        StepVerifier.create(anprClient.callEService(richiestaE002))
                .verifyError(PnInternalException.class);
    }

    @Test
    void callAnprDoOnError() {
        when(nationalRegistriesConfig.getAnpr()).thenReturn(anpr);

        RichiestaE002 richiestaE002 = new RichiestaE002();
        TipoCriteriRicercaE002 dto = new TipoCriteriRicercaE002();
        dto.setCodiceFiscale("DDDFFF52G52H501H");
        richiestaE002.setCriteriRicerca(dto);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purposeId");
        accessTokenCacheEntry.setTokenValue("fafsff");
        accessTokenCacheEntry.setTokenType(TokenType.BEARER);

        WebClientResponseException webClientResponseException = new WebClientResponseException("message",
                HttpStatus.BAD_REQUEST.value(), "statusText", HttpHeaders.EMPTY, null, null);

        when(accessTokenExpiringMap.getPDNDToken(any(), any(), anyBoolean())).thenReturn(Mono.just(accessTokenCacheEntry));
        when(agidJwtTrackingEvidence.createAgidJwt()).thenReturn("testJws");
        when(e002ServiceApi.e002(any(), any(), any(), any(), any(), any())).thenReturn(Mono.error(webClientResponseException));
        PdndSecretValue secret = new PdndSecretValue();
        secret.setJwtConfig(new JwtConfig());
        when(pnNationalRegistriesSecretService.getPdndSecretValue(any())).thenReturn(secret);
        StepVerifier.create(anprClient.callEService(richiestaE002))
                .verifyError(PnNationalRegistriesException.class);
    }
}
