package it.pagopa.pn.national.registries.model;

import lombok.Data;

@Data
public class TokenHeader {

    String alg;
    String kid;
    String typ;

    public TokenHeader(JwtConfig jwtCfg) {
        alg = "RS256";
        kid = jwtCfg.getKid();
        typ = "JWT";
    }
}

