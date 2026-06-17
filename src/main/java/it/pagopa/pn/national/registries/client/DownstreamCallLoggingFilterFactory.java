package it.pagopa.pn.national.registries.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class DownstreamCallLoggingFilterFactory {

    private final ObjectMapper objectMapper;

    public DownstreamCallLoggingFilterFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DownstreamCallLoggingFilter create(String clientName) {
        return new DownstreamCallLoggingFilter(clientName, objectMapper);
    }
}
