package it.pagopa.pn.national.registries.cache;

import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.dto.TokenTypeDto;
import it.pagopa.pn.national.registries.service.TokenProvider;
import lombok.SneakyThrows;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

        when(tokenProvider.getToken("purpose")).thenReturn(Mono.just(clientCredentialsResponseDto));

        StepVerifier.create(accessTokenExpiringMap.getToken("purpose")).expectNext(accessTokenCacheEntry).verifyComplete();

        expiringMap.put("purpose",accessTokenCacheEntry);

        StepVerifier.create(accessTokenExpiringMap.getToken("purpose")).expectNext(expiringMap.get("purpose")).verifyComplete();

    }

    @Test
    void testGetTokenExpiringMapMajor() {
        ClientCredentialsResponseDto clientCredentialsResponseDto = new ClientCredentialsResponseDto();
        clientCredentialsResponseDto.setExpiresIn(1);

        AccessTokenCacheEntry accessTokenCacheEntry = new AccessTokenCacheEntry("purpose");

        accessTokenExpiringMap = new AccessTokenExpiringMap(tokenProvider,Integer.parseInt("5000"));

        when(tokenProvider.getToken("purpose")).thenReturn(Mono.just(clientCredentialsResponseDto));

        StepVerifier.create(accessTokenExpiringMap.getToken("purpose")).expectNext(accessTokenCacheEntry).verifyComplete();

        expiringMap.put("purpose",accessTokenCacheEntry);

        StepVerifier.create(accessTokenExpiringMap.getToken("purpose")).expectNext(accessTokenCacheEntry).verifyComplete();

    }


}
