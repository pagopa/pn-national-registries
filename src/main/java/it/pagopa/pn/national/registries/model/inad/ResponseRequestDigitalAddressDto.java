package it.pagopa.pn.national.registries.model.inad;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class ResponseRequestDigitalAddressDto {

  @JsonProperty("codice_fiscale")
  private String taxId;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private Date since;

  private List<ElementDigitalAddressDto> digitalAddress = new ArrayList<>();
}
