package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

@Data
public class DigitalAddress {

  private String type;
  private String address;

  public DigitalAddress(String type, String address){
    this.type = type;
    this.address = address;
  }
}

