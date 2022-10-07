package it.pagopa.pn.national.registries.client.anpr;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.JWT;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.model.TokenHeader;
import it.pagopa.pn.national.registries.model.TokenPayload;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
import java.util.*;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_CODE_ADDRESS_ANPR;
import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.ERROR_MESSAGE_ADDRESS_ANPR;

@Component
@Slf4j
public class AgidJwtSignature {

    private final String secret;
    private final String secretIntegrity;
    private final SecretManagerService secretManagerService;
    private final ObjectMapper mapper;

    public AgidJwtSignature(@Value("${pn.national.registries.pdnd.anpr.secret.integrity}") String secretIntegrity,
                            @Value("${pn.national.registries.pdnd.anpr.secret.agid-jwt-signature}") String secret,
                            SecretManagerService secretManagerService,
                            ObjectMapper mapper) {
        this.secret = secret;
        this.secretIntegrity = secretIntegrity;
        this.secretManagerService = secretManagerService;
        this.mapper = mapper;
    }

    public String createAgidJwt(String digest) {
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
            return JWT.create().withHeader(createHeaderMap(th, sslData)).withPayload(createClaimMap(digest, tp))
                    .sign(Algorithm.RSA256(getPublicKey(sslData.getPub()), getPrivateKey(sslData.getKey())));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_ANPR, ERROR_CODE_ADDRESS_ANPR,e);
        }
    }

    private Map<String, Object> createHeaderMap(TokenHeader th, SSLData sslData) {
        Map<String, Object> map = new HashMap<>();
        map.put(HeaderParams.TYPE, th.getTyp());
        map.put(HeaderParams.ALGORITHM, th.getAlg());
        map.put("x5c", List.of(sslData.getCert()));
        return map;
    }

    private Map<String, Object> createClaimMap(String digest, TokenPayload tp) {
        Map<String, Object> map = new HashMap<>();
        map.put(RegisteredClaims.ISSUER, tp.getIss());
        map.put(RegisteredClaims.SUBJECT, tp.getSub());
        map.put(RegisteredClaims.EXPIRES_AT, tp.getExp());
        map.put(RegisteredClaims.AUDIENCE, tp.getAud());
        map.put(RegisteredClaims.ISSUED_AT, tp.getIat());
        map.put(RegisteredClaims.JWT_ID, tp.getJti());
        map.put("signed_headers", createSignedHeaders(digest));
        return map;
    }

    private List<Object> createSignedHeaders(String digest) {
        List<Object> list = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("digest", digest);
        list.add(map);
        Map<String, String> map1 = new HashMap<>();
        map1.put("content-encoding", "UTF-8");
        list.add(map1);
        Map<String, String> map2 = new HashMap<>();
        map2.put("content-type", "application/json");
        list.add(map2);

        return list;
    }


    protected RSAPublicKey getPublicKey(String pub) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(pub));
        X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(is.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(encodedKeySpec);
    }

    protected RSAPrivateKey getPrivateKey(String key) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(key));
        PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(is.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(encodedKeySpec);
    }
}
