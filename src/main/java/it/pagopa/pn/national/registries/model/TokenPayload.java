package it.pagopa.pn.national.registries.model;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Data
public class TokenPayload {
    String iss;
    String sub;
    String aud;
    String jti;
    long iat;
    long exp;
    String purposeId;
    private JwtDigest digest;
    private static final long SECONDS_TO_ADD_TO_EXPIRE = 5000L;


    public TokenPayload(JwtConfig jwtCfg, String auditDigest) {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long expireSeconds = nowSeconds + SECONDS_TO_ADD_TO_EXPIRE;

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
        if(StringUtils.hasText(auditDigest)) {
            this.digest = new JwtDigest();
            this.digest.setValue(auditDigest);
        }

    }
}
