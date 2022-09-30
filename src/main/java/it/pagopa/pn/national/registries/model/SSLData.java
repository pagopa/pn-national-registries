package it.pagopa.pn.national.registries.model;

import lombok.Data;

@Data
public class SSLData {
    private String cert;
    private String key;
    private String pub;
    private String trust;
}
