package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RispostaE002OKDto {
    private TipoTestataRispostaE000Dto testataRisposta;
    private TipoListaSoggettiDto listaSoggetti;
    private List<TipoErroriAnomaliaDto> listaAnomalie = null;
}
