package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

import java.util.List;

@Data
public class IniPecPollingResponse {

  private String dataOraDownload;

  private String identificativoRichiesta;

  private List<Pec> elencoPec;

}

