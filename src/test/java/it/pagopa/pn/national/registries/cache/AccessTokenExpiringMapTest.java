package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.SecretValue;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessTokenExpiringMapTest {

    private ExpiringMap<String, AccessTokenCacheEntry> expiringMap;

    @Mock
    private TokenProvider tokenProvider;

    private AccessTokenExpiringMap accessTokenExpiringMap;


    @BeforeAll
    void init(){
        expiringMap = ExpiringMap.builder()
                .variableExpiration()
                .build();
    }

    @Test
    void testGetTokenExpiringMapMinor() {
        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setExpiresIn(1);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purpose");

        accessTokenExpiringMap = new AccessTokenExpiringMap(tokenProvider,Integer.parseInt("-5000"));

        when(tokenProvider.getToken(new SecretValue())).thenReturn(Mono.just(clientCredentialsResponseDto));

        StepVerifier.create(accessTokenExpiringMap.getToken("purpose",new SecretValue())).expectNext(accessTokenCacheEntry).verifyComplete();

        expiringMap.put("purpose",accessTokenCacheEntry);

        StepVerifier.create(accessTokenExpiringMap.getToken("purpose",new SecretValue())).expectNext(expiringMap.get("purpose")).verifyComplete();

    }

    @Test
    void testGetTokenExpiringMapMajor() {
        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setExpiresIn(1);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purpose");

        accessTokenExpiringMap = new AccessTokenExpiringMap(tokenProvider,Integer.parseInt("5000"));

        when(tokenProvider.getToken(new SecretValue())).thenReturn(Mono.just(clientCredentialsResponseDto));

        StepVerifier.create(accessTokenExpiringMap.getToken("purpose",new SecretValue())).expectNext(accessTokenCacheEntry).verifyComplete();

        expiringMap.put("purpose",accessTokenCacheEntry);

        StepVerifier.create(accessTokenExpiringMap.getToken("purpose",new SecretValue())).expectNext(accessTokenCacheEntry).verifyComplete();

    }


}
