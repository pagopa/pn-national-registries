package it.pagopa.pn.national.registries.model.infocamere;

import lombok.Data;

@Data
public class InfoCamereCommonError {
    private String code;
    private String appName;
    private String description;
    private String timestamp;
}
