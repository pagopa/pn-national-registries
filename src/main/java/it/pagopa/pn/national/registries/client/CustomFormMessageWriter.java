package it.pagopa.pn.national.registries.client;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.codec.FormHttpMessageWriter;

public class CustomFormMessageWriter extends FormHttpMessageWriter {

    @Override
    protected @NotNull MediaType getMediaType(MediaType mediaType) {
        return Objects.requireNonNullElseGet(mediaType, () -> new MediaType(MediaType.APPLICATION_FORM_URLENCODED, DEFAULT_CHARSET));
    }
}
