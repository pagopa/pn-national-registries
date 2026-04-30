package it.pagopa.pn.national.registries.model.inipec;

import it.pagopa.pn.national.registries.exceptions.DigitalAddressException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Getter
@Slf4j
public enum StatoImpresa {
    ER,
    ND,
    NF;

    public static StatoImpresa fromString(String value, String correlationId) {
        return Arrays.stream(StatoImpresa.values())
                .filter(stato -> stato.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Invalid attribute statoImpresa: {} for correlatioId: {}", value, correlationId);
                    return new DigitalAddressException("Invalid attribute statoImpresa");
                });
    }
}