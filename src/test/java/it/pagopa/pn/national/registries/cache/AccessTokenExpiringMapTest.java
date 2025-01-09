package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.ClientCredentialsResponse;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.service.TokenProvider;
import net.jodah.expiringmap.ExpiringMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessTokenExpiringMapTest {

    private ExpiringMap<String, AccessTokenCacheEntry> expiringMap;

    @Mock
    private TokenProvider tokenProvider;


    private AccessTokenExpiringMap accessTokenExpiringMap;

    PnAuditLogEventType type = PnAuditLogEventType.AUD_NR_PF_PHYSICAL;
    Map<String, String> mdc = new HashMap<>();
    String message = "message";
    Object[] arguments = new Object[] {"arg1", "arg2"};
    PnAuditLogEvent logEvent;


    @BeforeAll
    void init() {
        mdc.put("key", "value");
        logEvent = new PnAuditLogEvent(type, mdc, message, arguments);
        expiringMap = ExpiringMap.builder()
                .variableExpiration()
                .build();
    }

    @Test
    void testGetTokenExpiringMapMinor() {
        ClientCredentialsResponse clientCredentialsResponse = new ClientCredentialsResponse();
        clientCredentialsResponse.setExpiresIn(1);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purpose");

        accessTokenExpiringMap = new AccessTokenExpiringMap(tokenProvider, -5000, -5000);

        when(tokenProvider.getTokenPdnd(new PdndSecretValue(), logEvent)).thenReturn(Mono.just(clientCredentialsResponse));

        StepVerifier.create(accessTokenExpiringMap.getPDNDToken("purpose", new PdndSecretValue(), false, logEvent))
                .expectNext(accessTokenCacheEntry)
                .verifyComplete();

        expiringMap.put("purpose", accessTokenCacheEntry);

        StepVerifier.create(accessTokenExpiringMap.getPDNDToken("purpose", new PdndSecretValue(), false, logEvent))
                .expectNext(expiringMap.get("purpose"))
                .verifyComplete();
    }

    @Test
    void testGetTokenExpiringMapMajor() {
        ClientCredentialsResponse clientCredentialsResponse = new ClientCredentialsResponse();
        clientCredentialsResponse.setExpiresIn(1);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purpose");

        accessTokenExpiringMap = new AccessTokenExpiringMap(tokenProvider, 5000, 5000);

        when(tokenProvider.getTokenPdnd(new PdndSecretValue(), logEvent)).thenReturn(Mono.just(clientCredentialsResponse));

        StepVerifier.create(accessTokenExpiringMap.getPDNDToken("purpose", new PdndSecretValue(), false, logEvent))
                .expectNext(accessTokenCacheEntry)
                .verifyComplete();

        expiringMap.put("purpose", accessTokenCacheEntry);

        StepVerifier.create(accessTokenExpiringMap.getPDNDToken("purpose", new PdndSecretValue(), false, logEvent))
                .expectNext(accessTokenCacheEntry)
                .verifyComplete();
    }

    @Test
    void testGetTokenExpiringMapInfoCamere1() {
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("eyJhbGciOiJIUzUxMiJ9.eyJhdWQiOiJodHRwczovL2ljYXBpc2NsLmluZm9jYW1lcmUuaXQvaWMvY2Uvd3NwYS93c3BhL3Jlc3QvIiwic3ViIjoiYTdlMTUyY2FjNDYwOTE3ZjMxMjNjYzI0MTBmNWE4ZDIiLCJzY29wZSI6InNlZGUtaW1wcmVzYS1wYSIsImlzcyI6ImE3ZTE1MmNhYzQ2MDkxN2YzMTIzY2MyNDEwZjVhOGQyIiwiZXhwIjo5OTgwNzc3Nzg5LCJpYXQiOjE2ODA3NzcxODksImp0aSI6IjcxZWY5ZmEzLThkMmYtNDAwMi05MTQwLTI2MWFjNmRkNzgyMiJ9.wb9I-1b0YVat0EaRUyY8wHww1Dz6-VuoQsQ-N2S5dArCiawiRsdSypsLPyI5TYh-RTA6-sbp4921vWmUiaNFxg");

        accessTokenExpiringMap = new AccessTokenExpiringMap(tokenProvider, Integer.MAX_VALUE, Integer.MAX_VALUE);
        when(tokenProvider.getTokenInfoCamere("scope", logEvent)).thenReturn(Mono.just("eyJhbGciOiJIUzUxMiJ9.eyJhdWQiOiJodHRwczovL2ljYXBpc2NsLmluZm9jYW1lcmUuaXQvaWMvY2Uvd3NwYS93c3BhL3Jlc3QvIiwic3ViIjoiYTdlMTUyY2FjNDYwOTE3ZjMxMjNjYzI0MTBmNWE4ZDIiLCJzY29wZSI6InNlZGUtaW1wcmVzYS1wYSIsImlzcyI6ImE3ZTE1MmNhYzQ2MDkxN2YzMTIzY2MyNDEwZjVhOGQyIiwiZXhwIjo5OTgwNzc3Nzg5LCJpYXQiOjE2ODA3NzcxODksImp0aSI6IjcxZWY5ZmEzLThkMmYtNDAwMi05MTQwLTI2MWFjNmRkNzgyMiJ9.wb9I-1b0YVat0EaRUyY8wHww1Dz6-VuoQsQ-N2S5dArCiawiRsdSypsLPyI5TYh-RTA6-sbp4921vWmUiaNFxg"));

        StepVerifier.create(accessTokenExpiringMap.getInfoCamereToken("scope", logEvent))
                .expectNext(accessTokenCacheEntry)
                .verifyComplete();

        expiringMap.put("scope", accessTokenCacheEntry);

        StepVerifier.create(accessTokenExpiringMap.getInfoCamereToken("scope", logEvent))
                .expectNext(expiringMap.get("scope"))
                .verifyComplete();
    }

    @Test
    void testGetTokenExpiringMapInfoCamere2() {
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("eyJhbGciOiJIUzUxMiJ9.eyJhdWQiOiJodHRwczovL2ljYXBpc2NsLmluZm9jYW1lcmUuaXQvaWMvY2Uvd3NwYS93c3BhL3Jlc3QvIiwic3ViIjoiYTdlMTUyY2FjNDYwOTE3ZjMxMjNjYzI0MTBmNWE4ZDIiLCJzY29wZSI6InNlZGUtaW1wcmVzYS1wYSIsImlzcyI6ImE3ZTE1MmNhYzQ2MDkxN2YzMTIzY2MyNDEwZjVhOGQyIiwiZXhwIjo5OTgwNzc3Nzg5LCJpYXQiOjE2ODA3NzcxODksImp0aSI6IjcxZWY5ZmEzLThkMmYtNDAwMi05MTQwLTI2MWFjNmRkNzgyMiJ9.wb9I-1b0YVat0EaRUyY8wHww1Dz6-VuoQsQ-N2S5dArCiawiRsdSypsLPyI5TYh-RTA6-sbp4921vWmUiaNFxg");
        accessTokenExpiringMap = new AccessTokenExpiringMap(tokenProvider, 5000, 5000);

        when(tokenProvider.getTokenInfoCamere("scope", logEvent)).thenReturn(Mono.just("eyJhbGciOiJIUzUxMiJ9.eyJhdWQiOiJodHRwczovL2ljYXBpc2NsLmluZm9jYW1lcmUuaXQvaWMvY2Uvd3NwYS93c3BhL3Jlc3QvIiwic3ViIjoiYTdlMTUyY2FjNDYwOTE3ZjMxMjNjYzI0MTBmNWE4ZDIiLCJzY29wZSI6InNlZGUtaW1wcmVzYS1wYSIsImlzcyI6ImE3ZTE1MmNhYzQ2MDkxN2YzMTIzY2MyNDEwZjVhOGQyIiwiZXhwIjo5OTgwNzc3Nzg5LCJpYXQiOjE2ODA3NzcxODksImp0aSI6IjcxZWY5ZmEzLThkMmYtNDAwMi05MTQwLTI2MWFjNmRkNzgyMiJ9.wb9I-1b0YVat0EaRUyY8wHww1Dz6-VuoQsQ-N2S5dArCiawiRsdSypsLPyI5TYh-RTA6-sbp4921vWmUiaNFxg"));

        StepVerifier.create(accessTokenExpiringMap.getInfoCamereToken("scope", logEvent))
                .expectNext(accessTokenCacheEntry)
                .verifyComplete();

        expiringMap.put("scope", accessTokenCacheEntry);

        StepVerifier.create(accessTokenExpiringMap.getInfoCamereToken("scope", logEvent))
                .expectNext(accessTokenCacheEntry)
                .verifyComplete();
    }

    @Test
    void testGetTokenExpiringMapInfoCamere3() {
        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("scope");
        accessTokenCacheEntry.setClientCredentials("eyJhbGciOiJIUzUxMiJ9.eyJhdWQiOiJodHRwczovL2ljYXBpc2NsLmluZm9jYW1lcmUuaXQvaWMvY2Uvd3NwYS93c3BhL3Jlc3QvIiwic3ViIjoiYTdlMTUyY2FjNDYwOTE3ZjMxMjNjYzI0MTBmNWE4ZDIiLCJzY29wZSI6InNlZGUtaW1wcmVzYS1wYSIsImlzcyI6ImE3ZTE1MmNhYzQ2MDkxN2YzMTIzY2MyNDEwZjVhOGQyIiwiZXhwIjoxNjgwNzc3Nzg5LCJpYXQiOjE2ODA3NzcxODksImp0aSI6IjcxZWY5ZmEzLThkMmYtNDAwMi05MTQwLTI2MWFjNmRkNzgyMiJ9.HVnHcmcebOnw5y4ziVIZjSz90ZjCvbyyxlFZ4Uq9V1Hka8Add7GQ6qO8BFpF73hvlFFVY-Av-58-OIq312N5oQ");
        accessTokenExpiringMap = new AccessTokenExpiringMap(tokenProvider, 5000, 5000);

        when(tokenProvider.getTokenInfoCamere("scope", logEvent)).thenReturn(Mono.just("eyJhbGciOiJIUzUxMiJ9.eyJhdWQiOiJodHRwczovL2ljYXBpc2NsLmluZm9jYW1lcmUuaXQvaWMvY2Uvd3NwYS93c3BhL3Jlc3QvIiwic3ViIjoiYTdlMTUyY2FjNDYwOTE3ZjMxMjNjYzI0MTBmNWE4ZDIiLCJzY29wZSI6InNlZGUtaW1wcmVzYS1wYSIsImlzcyI6ImE3ZTE1MmNhYzQ2MDkxN2YzMTIzY2MyNDEwZjVhOGQyIiwiZXhwIjoxNjgwNzc3Nzg5LCJpYXQiOjE2ODA3NzcxODksImp0aSI6IjcxZWY5ZmEzLThkMmYtNDAwMi05MTQwLTI2MWFjNmRkNzgyMiJ9.HVnHcmcebOnw5y4ziVIZjSz90ZjCvbyyxlFZ4Uq9V1Hka8Add7GQ6qO8BFpF73hvlFFVY-Av-58-OIq312N5oQ"));

        StepVerifier.create(accessTokenExpiringMap.getInfoCamereToken("scope", logEvent))
                .expectError(PnInternalException.class)
                .verify();

        expiringMap.put("scope", accessTokenCacheEntry);

        StepVerifier.create(accessTokenExpiringMap.getInfoCamereToken("scope", logEvent))
                .expectNext(accessTokenCacheEntry)
                .verifyComplete();
    }

}
