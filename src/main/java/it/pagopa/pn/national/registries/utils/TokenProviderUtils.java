package it.pagopa.pn.national.registries.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.SecretValue;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
public class TokenProviderUtils {

    private TokenProviderUtils() {}

    public static SecretValue convertToSecretValueObject(String value){
        ObjectMapper mapper = new ObjectMapper();
        SecretValue secretValue = new SecretValue();
        try {
            secretValue = mapper.readValue(value,SecretValue.class);
        } catch (JsonProcessingException e) {
            //TODO: EXCEPTION
            log.info("cannot parse secret on SecretValue class");
        }
        return secretValue;
    }

    public static String jsonObjectToUrlSafeBase64String(String jsonString) {
        return stringToUrlSafeBase64String(jsonString);
    }

    private static String stringToUrlSafeBase64String(String inString) {
        byte[] jsonBytes = inString.getBytes(StandardCharsets.UTF_8);
        return bytesToUrlSafeBase64String(jsonBytes);
    }

    public static String bytesToUrlSafeBase64String(byte[] bytes) {
        byte[] base64JsonBytes = Base64Utils.encodeUrlSafe(bytes);
        return new String(base64JsonBytes, StandardCharsets.UTF_8)
                .replaceFirst("=+$", "");
    }

    @Data
    public static class TokenHeader {
        String alg;
        String kid;
        String typ;

        public TokenHeader(JwtConfig jwtCfg) {
            alg = "RS256";
            kid = jwtCfg.getKid();
            typ = "JWT";
        }
    }

    @Data
    public static class TokenPayload {
        String iss;
        String sub;
        String aud;
        String jti;
        long iat;
        long exp;
        String purposeId;

        public TokenPayload(JwtConfig jwtCfg) {
            long nowSeconds = System.currentTimeMillis() / 1000L;
            long ttlSeconds = System.currentTimeMillis() * 1000L;
            long expireSeconds = nowSeconds + ttlSeconds;

            iss = jwtCfg.getIssuer();
            sub = jwtCfg.getSubject();
            aud = jwtCfg.getAudience();
            jti = UUID.randomUUID().toString();
            iat = nowSeconds;
            exp = expireSeconds;
            String confpurposeId = jwtCfg.getPurposeId();
            if (StringUtils.hasText(confpurposeId)) {
                purposeId = confpurposeId;
            }
        }
    }

}
