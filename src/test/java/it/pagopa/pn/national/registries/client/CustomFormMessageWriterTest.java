package it.pagopa.pn.national.registries.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomFormMessageWriterTest {

    private CustomFormMessageWriter customFormMessageWriter;

    @BeforeEach
    void setUp() {
        customFormMessageWriter = new CustomFormMessageWriter();
    }

    @Test
    @DisplayName("Should return the media type when the media type is not null")
    void getMediaTypeWhenMediaTypeIsNotNull() {
        MediaType mediaType =
                new MediaType(MediaType.APPLICATION_FORM_URLENCODED, StandardCharsets.UTF_8);
        assertEquals(mediaType, customFormMessageWriter.getMediaType(mediaType));
    }

    @Test
    @DisplayName("Should return the default media type when the media type is null")
    void getMediaTypeWhenMediaTypeIsNull() {
        MediaType mediaType = null;
        MediaType expectedMediaType =
                new MediaType(MediaType.APPLICATION_FORM_URLENCODED, StandardCharsets.UTF_8);

        MediaType actualMediaType = customFormMessageWriter.getMediaType(mediaType);

        assertEquals(expectedMediaType, actualMediaType);
    }
}
