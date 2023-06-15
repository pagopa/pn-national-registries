package it.pagopa.pn.national.registries.model;

import lombok.Data;

@Data
public class JwtDigest {
    private String alg = "SHA256";
    private String value;
}
