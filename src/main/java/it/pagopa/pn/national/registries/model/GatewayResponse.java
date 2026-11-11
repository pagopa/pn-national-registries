package it.pagopa.pn.national.registries.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GatewayResponse {
    private CodeSqsDto payload;
    private String clientId;
}
