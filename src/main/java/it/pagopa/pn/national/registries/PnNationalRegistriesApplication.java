package it.pagopa.pn.national.registries;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PnNationalRegistriesApplication {

    public static void main(String[] args) {
        SpringApplication.run(PnNationalRegistriesApplication.class, args);
    }

}
