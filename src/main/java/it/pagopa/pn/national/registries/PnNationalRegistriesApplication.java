package it.pagopa.pn.national.registries;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PnNationalRegistriesApplication {

    public static void main(String[] args) {
        SpringApplication.run(PnNationalRegistriesApplication.class, args);
        SpringApplication app = new SpringApplication(PnNationalRegistriesApplication.class);
        app.addListeners(new TaskIdApplicationListener());
        app.run(args);
    }

}
