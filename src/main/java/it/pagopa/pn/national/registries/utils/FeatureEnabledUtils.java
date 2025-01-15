package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@AllArgsConstructor
@Component
public class FeatureEnabledUtils {

    private final NationalRegistriesConfig configs;

    public boolean isPfNewWorkflowEnabled(Instant referenceRequestDate) {
        boolean isEnabled = false;
        Instant startDate = Instant.parse(configs.getPfNewWorkflowStart());
        Instant endDate = Instant.parse(configs.getPfNewWorkflowStop());
        if (referenceRequestDate.compareTo(startDate) >= 0 && referenceRequestDate.compareTo(endDate) <= 0) {
            isEnabled = true;
        }
        return isEnabled;
    }
}
