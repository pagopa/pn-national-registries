package it.pagopa.pn.national.registries.client.inipec;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.JWT;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.inipec.IniPecSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
public class AgidJwtSign {

    private final String aud;
    private final IniPecSecretConfig iniPecSecretConfig;

    public AgidJwtSign(@Value("${pn.national.registries.pdnd.inipec.base-path}") String aud,
                            IniPecSecretConfig iniPecSecretConfig) {
        this.aud = aud;
        this.iniPecSecretConfig = iniPecSecretConfig;
    }

    public String createAgidJwt(String digest) {
        try {
            log.info("start to createAgidJwt with digest: {}",digest);

            SSLData sslData = iniPecSecretConfig.getIniPecIntegritySecret();

            return JWT.create().withHeader(createHeaderMap(sslData)).withPayload(createClaimMap(digest))
                    .sign(Algorithm.RSA256(getPublicKey(sslData.getPub()), getPrivateKey(sslData.getKey())));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_ANPR, ERROR_CODE_ADDRESS_ANPR,e);
        }
    }

    private Map<String, Object> createHeaderMap(SSLData sslData) {
        Map<String, Object> map = new HashMap<>();
        map.put(HeaderParams.TYPE, "JWT");
        map.put(HeaderParams.ALGORITHM, "ES256");
        map.put("x5c", List.of(sslData.getCert()));
        log.debug("HeaderMap type: {}, alg: {}",map.get(HeaderParams.TYPE), map.get(HeaderParams.ALGORITHM));
        return map;
    }

    private Map<String, Object> createClaimMap(String digest) {
        Map<String, Object> map = new HashMap<>();
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long expireSeconds = nowSeconds + 5000L;

        map.put(RegisteredClaims.AUDIENCE, aud);
        map.put(RegisteredClaims.ISSUED_AT, nowSeconds);
        map.put(RegisteredClaims.NOT_BEFORE, nowSeconds);
        map.put(RegisteredClaims.EXPIRES_AT, expireSeconds);
        map.put(RegisteredClaims.ISSUER, "url");
        map.put(RegisteredClaims.SUBJECT, "url");
        map.put(RegisteredClaims.JWT_ID, UUID.randomUUID().toString());

        map.put("signed_headers", createSignedHeaders(digest));
        log.debug("ClaimMap audience: {}",map.get(RegisteredClaims.AUDIENCE));

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
        log.debug("start getPublicKey");
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(pub));
        X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(is.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(encodedKeySpec);
    }

    protected RSAPrivateKey getPrivateKey(String key) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        log.debug("start getPrivateKey");
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(key));
        PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(is.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(encodedKeySpec);
    }
}
