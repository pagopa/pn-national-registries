package it.pagopa.pn.national.registries.service;

public interface AnprAddressStrategy {
    String createAddressDetail(it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo indirizzo);
    String getStrategyName();
}
