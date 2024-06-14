package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class IniPecBatchRequest {

    private Date dataOraRichiesta;

    private List<IniPecCf> elencoCf;

    @Data
    public static class IniPecCf {
        private String cf;
    }

}
