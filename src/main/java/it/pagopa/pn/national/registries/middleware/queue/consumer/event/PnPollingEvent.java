package it.pagopa.pn.national.registries.middleware.queue.consumer.event;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PnPollingEvent implements GenericEvent<StandardEventHeader, PnPollingEvent.Payload> {

    private StandardEventHeader header;

    private Payload payload;

    @Getter
    @Builder
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        @NotEmpty
        private GatewayDownstreamService registry;

        @NotEmpty
        private String batchId;

        @NotEmpty
        private String pollingId;

        private Integer batchSize;
        private Integer retryCount;
        private Integer inProgressRetryCount;

    }

}
