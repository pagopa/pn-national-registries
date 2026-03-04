package it.pagopa.pn.national.registries.service;

import java.util.Optional;

public interface AnprAddressStrategy {
    int MAX_LEN = 44;

    String createAddressDetail(it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo indirizzo);
    String getStrategyName();

    default void appendIfFits(StringBuilder sb, String value) {
        String token = Optional.ofNullable(value).map(String::strip).orElse("");
        if (token.isEmpty()) return;

        //Serve per calcolare lo spazio necessario ad aggiungere il token, considerando anche lo spazio se sb non è vuoto
        int extra = token.length() + (sb.isEmpty() ? 0 : 1);
        if (sb.length() + extra > MAX_LEN) return;

        if (!sb.isEmpty()) sb.append(' ');
        sb.append(token);
    }
}
