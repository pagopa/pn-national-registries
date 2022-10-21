package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxIdDto {
    private String codFiscale;
    private String validitaCF;
    private String dataAttribuzioneValidita;
}
