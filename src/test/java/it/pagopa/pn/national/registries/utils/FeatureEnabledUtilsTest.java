package it.pagopa.pn.national.registries.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

@ContextConfiguration(classes = {FeatureEnabledUtils.class})
@ExtendWith(SpringExtension.class)
class FeatureEnabledUtilsTest {

    @MockitoBean
    private NationalRegistriesConfig nationalRegistriesConfig;

    @Autowired
    private FeatureEnabledUtils featureEnabledUtils;

    @BeforeEach
    void setUp() {
        when(nationalRegistriesConfig.getPfNewWorkflowStart()).thenReturn("2023-01-01T00:00:00Z");
        when(nationalRegistriesConfig.getPfNewWorkflowStop()).thenReturn("2023-12-31T23:59:59Z");
    }

    @Test
    @DisplayName("Test isPfNewWorkflowEnabled with date within range")
    void testIsPfNewWorkflowEnabled_WithinRange() {
        Instant notificationSentAt = Instant.parse("2023-06-15T00:00:00Z");
        assertTrue(featureEnabledUtils.isPfNewWorkflowEnabled(notificationSentAt));
    }

    @Test
    @DisplayName("Test isPfNewWorkflowEnabled with date before range")
    void testIsPfNewWorkflowEnabled_BeforeRange() {
        Instant notificationSentAt = Instant.parse("2022-12-31T23:59:59Z");
        assertFalse(featureEnabledUtils.isPfNewWorkflowEnabled(notificationSentAt));
    }

    @Test
    @DisplayName("Test isPfNewWorkflowEnabled with date after range")
    void testIsPfNewWorkflowEnabled_AfterRange() {
        Instant notificationSentAt = Instant.parse("2024-01-01T00:00:00Z");
        assertFalse(featureEnabledUtils.isPfNewWorkflowEnabled(notificationSentAt));
    }
}