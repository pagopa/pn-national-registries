package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;

public interface AnprAddressStrategy {
    String createAddress(TipoIndirizzo indirizzo);
    String createAddressDetail(TipoIndirizzo indirizzo);
    String getStrategyName();
}
