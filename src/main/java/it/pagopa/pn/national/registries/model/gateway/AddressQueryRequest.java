package it.pagopa.pn.national.registries.model.gateway;

import it.pagopa.pn.national.registries.constant.DomicileType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class AddressQueryRequest {
    private String correlationId;
    private String pnNationalRegistriesCxId;
    private Date referenceRequestDate;
    private String taxId;
    private Integer recIndex;
    private RecipientType recipientType;
    private DomicileType domicileType;
}
