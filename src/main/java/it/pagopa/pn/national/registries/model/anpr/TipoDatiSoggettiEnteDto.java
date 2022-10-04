package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipoDatiSoggettiEnteDto {
    private TipoGeneralitaDto generalita;
    private List<TipoResidenzaDto> residenza = null;
}
