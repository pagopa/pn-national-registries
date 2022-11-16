package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

import java.util.List;

@Data
public class RequestCfIniPec {

  private String dataOraRichiesta;

  private List<String> elencoCf;

}

