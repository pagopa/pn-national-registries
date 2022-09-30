package it.pagopa.pn.national.registries.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class SchedulerConfig {

    @Bean("nationalRegistriesScheduler")
    public Scheduler scheduler(){
        return Schedulers.boundedElastic();
    }

}
