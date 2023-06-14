package it.pagopa.pn.national.registries.model.infocamere;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InfoCamereLegalInstituionsResponse extends InfoCamereCommonError {

    @JsonProperty("dataOraEstrazione")
    private String dateTimeExtraction;
    @JsonProperty("cfPersona")
    private String legalTaxId;
    @JsonProperty("elencoImpreseRappresentate")
    private List<InfoCamereInstitution> businessList;

}
