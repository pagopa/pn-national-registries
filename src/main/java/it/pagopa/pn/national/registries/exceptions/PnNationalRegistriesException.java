package it.pagopa.pn.national.registries.exceptions;

import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.Charset;

public class PnNationalRegistriesException extends WebClientResponseException {

    @Getter
    private final Class<?> className;

    public PnNationalRegistriesException(String message, int statusCode, String statusText, HttpHeaders headers, byte[] responseBody, Charset charset, Class<?> className) {
        super(message, statusCode, statusText, headers, responseBody, charset);
        this.className = className;
    }
}
