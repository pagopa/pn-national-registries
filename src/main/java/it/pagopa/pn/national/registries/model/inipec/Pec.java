package it.pagopa.pn.national.registries.model.inipec;

import lombok.Data;

import java.util.List;

@Data
public class Pec {

  private String cf;

  private String pecImpresa;

  private List<PecProfessionista> pecProfessionistas;

}

