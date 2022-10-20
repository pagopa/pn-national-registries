package it.pagopa.pn.national.registries.model.inad;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ResponseRequestDigitalAddressDto {

  @JsonProperty("codice_fiscale")
  private String taxId;
  private String since;
  private List<ElementDigitalAddressDto> digitalAddress = new ArrayList<>();
}

