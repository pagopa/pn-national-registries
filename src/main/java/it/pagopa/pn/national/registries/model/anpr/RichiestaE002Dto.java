package it.pagopa.pn.national.registries.model.anpr;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

/**
 * RichiestaE002Dto
 */
@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RichiestaE002Dto {
    private TipoCriteriRicercaE002Dto criteriRicerca;
    private TipoTestataRichiestaE000Dto testataRichiesta;
    private TipoDatiRichiestaE002Dto datiRichiesta;

}


