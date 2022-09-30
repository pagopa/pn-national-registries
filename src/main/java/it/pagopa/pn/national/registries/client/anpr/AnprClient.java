package it.pagopa.pn.national.registries.client.anpr;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.JWT;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.cache.AccessTokenExpiringMap;
import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.ApiClient;
import it.pagopa.pn.national.registries.model.*;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class AnprClient{

    private final AccessTokenExpiringMap accessTokenExpiringMap;
    private final String purposeId;
    private final AnprApiClient anprApiClient;
    private final String secret;
    private final String secretIntegrity;
    private final SecretManagerService secretManagerService;
    private final ObjectMapper mapper;

    protected AnprClient(AccessTokenExpiringMap accessTokenExpiringMap,
                         SecretManagerService secretManagerService,
                         AnprApiClient anprApiClient,
                         ObjectMapper mapper,
                         @Value("${pdnd.c001.purpose-id}") String purposeId,
                         @Value("${pdnd.anpr.secret.integrity}") String secretIntegrity,
                         @Value("${pdnd.anpr.secret.agid-jwt-signature}") String secret) {
        this.accessTokenExpiringMap = accessTokenExpiringMap;
        this.anprApiClient = anprApiClient;
        this.purposeId=purposeId;
        this.secret = secret;
        this.secretIntegrity = secretIntegrity;
        this.secretManagerService = secretManagerService;
        this.mapper = mapper;
    }

    public Mono<ApiClient> getApiClient(String digest){
        return accessTokenExpiringMap.getToken(purposeId).flatMap(accessTokenCacheEntry -> {
            log.info(accessTokenCacheEntry.getAccessToken());
            anprApiClient.setApiKey(createAgidJwt(digest));
            anprApiClient.setBearerToken(accessTokenCacheEntry.getAccessToken());
            return Mono.just(anprApiClient);
        });
    }

    private String createAgidJwt(String digest) {
        Optional<GetSecretValueResponse> optJwtConfig = secretManagerService.getSecretValue(secret);
        Optional<GetSecretValueResponse> optSslData = secretManagerService.getSecretValue(secretIntegrity);
        if (optJwtConfig.isEmpty() || optSslData.isEmpty()) {
            log.info("secret value not found");
            return null;
        }
        try {
            JwtConfig jwtConfig = mapper.readValue(optJwtConfig.get().secretString(), JwtConfig.class);
            SSLData sslData = mapper.readValue(optSslData.get().secretString(), SSLData.class);
            TokenHeader th = new TokenHeader(jwtConfig);
            TokenPayload tp = new TokenPayload(jwtConfig);
            return JWT.create().withHeader(createHeaderMap(th,sslData)).withPayload(createClaimMap(digest,tp))
                    .sign(Algorithm.RSA256(getPublicKey(sslData.getPub()),getPrivateKey(sslData.getKey())));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String,Object> createHeaderMap(TokenHeader th,SSLData sslData) {
        Map<String,Object> map =  new HashMap<>();
        map.put(HeaderParams.TYPE,th.getTyp());
        map.put(HeaderParams.ALGORITHM,th.getAlg());
        map.put("x5c","["+sslData.getCert()+"]");
        return map;
    }

    private Map<String,Object> createClaimMap(String digest, TokenPayload tp) {
        Map<String,Object> map =  new HashMap<>();
        map.put(RegisteredClaims.ISSUER,tp.getIss());
        map.put(RegisteredClaims.SUBJECT,tp.getSub());
        map.put(RegisteredClaims.EXPIRES_AT,tp.getExp());
        map.put(RegisteredClaims.AUDIENCE,tp.getAud());
        map.put(RegisteredClaims.ISSUED_AT,tp.getIat());
        map.put(RegisteredClaims.JWT_ID,tp.getJti());
        map.put("signed_headers",createSignedHeaders(digest));
        return map;
    }

    private Map<String,Object> createSignedHeaders(String digest) {
        Map<String,Object> map = new HashMap<>();
        map.put("digest",digest);
        map.put("content-encoding","UTF-8");
        map.put("content-type","application/json");
        return map;
    }


    private RSAPublicKey getPublicKey(String pub) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(pub));
        X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(is.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return  (RSAPublicKey) kf.generatePublic(encodedKeySpec);
    }

    private RSAPrivateKey getPrivateKey(String key) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(key));
        PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(is.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return  (RSAPrivateKey) kf.generatePrivate(encodedKeySpec);
    }
}
