package it.pagopa.pn.national.registries.model;

import lombok.Data;

@Data
public class SSLData {
    private String cert;
    private String keyId;
    private String dns;
    private String key;
    private String pub;
    private String trust;
}
