package it.pagopa.pn.national.registries.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSLData {
    private String cert;
    private String keyId;
    private String dns;
    private String secretid;
}
