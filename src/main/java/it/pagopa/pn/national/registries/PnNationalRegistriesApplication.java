package it.pagopa.pn.national.registries;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import static it.pagopa.pn.national.registries.utils.SpringApplicationUtils.buildSpringApplicationWithListener;

@SpringBootApplication
public class PnNationalRegistriesApplication {

    public static void main(String[] args) {
        buildSpringApplicationWithListener().run(args);
    }

}
