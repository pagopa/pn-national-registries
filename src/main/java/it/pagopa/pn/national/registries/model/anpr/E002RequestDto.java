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
public class E002RequestDto {
    private String idOperazioneClient;
    private SearchCriteriaE002Dto criteriRicerca;
    private RequestDateE002Dto datiRichiesta;
}
