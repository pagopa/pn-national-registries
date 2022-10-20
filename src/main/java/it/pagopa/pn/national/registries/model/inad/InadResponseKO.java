package it.pagopa.pn.national.registries.model.inad;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InadResponseKO {
    private String detail;
}
