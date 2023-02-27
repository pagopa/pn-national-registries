package it.pagopa.pn.national.registries.model.inipec;

import it.pagopa.pn.national.registries.model.infocamere.InfoCamereCommonError;
import lombok.Data;

import java.util.List;

@Data
public class IniPecPollingResponse extends InfoCamereCommonError {

  private String dataOraDownload;

  private String identificativoRichiesta;

  private List<Pec> elencoPec;

}

