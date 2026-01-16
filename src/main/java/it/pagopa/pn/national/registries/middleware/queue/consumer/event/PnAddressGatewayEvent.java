package it.pagopa.pn.national.registries.middleware.queue.consumer.event;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PnAddressGatewayEvent implements GenericEvent<StandardEventHeader, PnAddressGatewayEvent.Payload> {

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
        private String correlationId;

        @NotEmpty
        @lombok.ToString.Exclude
        private String taxId;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private Date referenceRequestDate;

        @NotEmpty
        private String domicileType;

        @NotEmpty
        private String recipientType;

        @NotEmpty
        private String pnNationalRegistriesCxId;
    }

}
