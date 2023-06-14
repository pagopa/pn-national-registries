package it.pagopa.pn.national.registries.client.infocamere;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.JWT;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.utils.ClientUtils;
import it.pagopa.pn.national.registries.config.infocamere.InfoCamereSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_CODE_INFOCAMERE;
import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.ERROR_MESSAGE_INFOCAMERE;

@Slf4j
@Component
public class InfoCamereJwsGenerator {

    private final String aud;
    private final InfoCamereSecretConfig infoCamereSecretConfig;
    private final String clientId;

    public InfoCamereJwsGenerator(@Value("${pn.national.registries.infocamere.base-path}") String aud,
                                  @Value("${pn.national.registries.infocamere.client-id}") String clientId,
                                  InfoCamereSecretConfig infoCamereSecretConfig) {
        this.aud = aud;
        this.infoCamereSecretConfig = infoCamereSecretConfig;
        this.clientId = clientId;
    }

    public String createAuthRest(String scope) {
        try {
            log.info("start to createAuthRest");
            SSLData sslData = infoCamereSecretConfig.getInfoCamereAuthRestSecret();
            return JWT.create()
                    .withHeader(createHeaderMap(sslData))
                    .withPayload(createClaimMap(scope))
                    .sign(Algorithm.RSA256(ClientUtils.getPublicKey(sslData.getPub()), ClientUtils.getPrivateKey(sslData.getKey())));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new PnInternalException(ERROR_MESSAGE_INFOCAMERE, ERROR_CODE_INFOCAMERE, e);
        }
    }

    private Map<String, Object> createHeaderMap(SSLData sslData) {
        Map<String, Object> map = new HashMap<>();
        map.put(HeaderParams.TYPE, "JWT");
        map.put(HeaderParams.ALGORITHM, "ES256");
        map.put("x5c", List.of(sslData.getCert()));
        map.put("use", "sig");
        log.debug("HeaderMap type: {}, alg: {}",map.get(HeaderParams.TYPE), map.get(HeaderParams.ALGORITHM));
        return map;
    }

    private Map<String, Object> createClaimMap(String scope) {
        Map<String, Object> map = new HashMap<>();
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long expireSeconds = nowSeconds + 120;

        map.put(RegisteredClaims.AUDIENCE, aud);
        map.put(RegisteredClaims.EXPIRES_AT, expireSeconds);
        map.put(RegisteredClaims.ISSUER, clientId);
        map.put(RegisteredClaims.SUBJECT, clientId);
        map.put(RegisteredClaims.JWT_ID, UUID.randomUUID().toString());
        map.put("scope", scope);

        log.debug("ClaimMap audience: {}", map.get(RegisteredClaims.AUDIENCE));
        return map;
    }

}
