package it.pagopa.pn.national.registries.model.ipa;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;


@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataWS05Dto {

    @JsonProperty("cod_amm")
    private String codAmm;

    @JsonProperty("acronimo")
    private String acronimo;

    @JsonProperty("des_amm")
    private String desAmm;

    @JsonProperty("regione")
    private String regione;

    @JsonProperty("provincia")
    private String provincia;

    @JsonProperty("comune")
    private String comune;

    @JsonProperty("cap")
    private String cap;

    @JsonProperty("indirizzo")
    private String indirizzo;

    @JsonProperty("titolo_resp")
    private String titoloResp;

    @JsonProperty("nome_resp")
    private String nomeResp;

    @JsonProperty("cogn_resp")
    private String cognResp;

    @JsonProperty("sito_istituzionale")
    private String sitoIstituzionale;

    @JsonProperty("liv_accessibilita")
    private String livAccessibilita;

    @JsonProperty("mail1")
    private String mail1;

    @JsonProperty("mail2")
    private String mail2;

    @JsonProperty("mail3")
    private String mail3;

    @JsonProperty("mail4")
    private String mail4;

    @JsonProperty("mail5")
    private String mail5;

    @JsonProperty("tipologia")
    private String tipologia;

    @JsonProperty("categoria")
    private String categoria;

    @JsonProperty("data_accreditamento")
    private String dataAccreditamento;

    @JsonProperty("cf")
    private String cf;
}
