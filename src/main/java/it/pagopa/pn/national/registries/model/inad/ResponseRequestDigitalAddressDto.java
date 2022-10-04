package it.pagopa.pn.national.registries.model.inad;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Data
public class ResponseRequestDigitalAddressDto {
  private String codiceFiscale;
  private Date since;
  private List<ElementDigitalAddressDto> digitalAddress = new ArrayList<>();
}

