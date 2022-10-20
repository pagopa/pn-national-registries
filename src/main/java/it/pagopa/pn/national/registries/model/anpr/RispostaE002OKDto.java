package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RispostaE002OKDto {
    private TipoTestataRispostaE000Dto testataRisposta;
    private TipoListaSoggettiDto listaSoggetti;
    private List<TipoErroriAnomaliaDto> listaAnomalie = null;
}
