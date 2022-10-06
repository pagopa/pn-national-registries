package it.pagopa.pn.national.registries.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.scheduler.Scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SchedulerConfig.class)
class SchedulerConfigTest {

    @Autowired
    private SchedulerConfig schedulerConfig;

    @Test
    @DisplayName("Should return a boundedelastic scheduler")
    void schedulerShouldReturnBoundedElasticScheduler() {
        Scheduler scheduler = schedulerConfig.scheduler();
        assertNotNull(scheduler);
        assertEquals("Schedulers.boundedElastic()", scheduler.toString());
    }
}
