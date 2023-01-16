package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

import java.util.List;

@Data
public class ResponsePecIniPec {

  private String dataOraDownload;

  private String identificativoRichiesta;

  private List<Pec> elencoPec;

}

