package it.pagopa.pn.national.registries.utils;

import java.time.Instant;
import java.util.List;

public class Utils {
    private Utils() { }

    public static Instant getMostRecentInstant(List<Instant> instants) {
        if (instants == null || instants.isEmpty()) {
            return null;
        }

        return instants.stream()
                .max(Instant::compareTo)
                .orElse(null);
    }
}
