package it.pagopa.pn.national.registries.exceptions;

public class DigitalAddressException extends RuntimeException {

    public DigitalAddressException(String message) {
        super(message);
    }

    public DigitalAddressException(String message, Throwable cause) {
        super(message, cause);
    }

}
