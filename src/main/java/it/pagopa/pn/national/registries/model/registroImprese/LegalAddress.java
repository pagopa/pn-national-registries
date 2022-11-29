package it.pagopa.pn.national.registries.model.registroImprese;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LegalAddress {

    @JsonProperty("denominazione")
    private String address;

     @JsonProperty("via")
     private String street;

     @JsonProperty("comune")
     private String municipality;

     @JsonProperty("provincia")
     private String province;

     @JsonProperty("toponimo")
     private String toponym;

     @JsonProperty("nCivico")
     private String streetNumber;

     @JsonProperty("cap")
     private String postalCode;

}
