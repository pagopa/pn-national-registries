package it.pagopa.pn.national.registries.middleware.queue.consumer.event;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotEmpty;
import java.util.Date;
import java.util.List;


@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PnAddressesGatewayEvent implements GenericEvent<StandardEventHeader, PnAddressesGatewayEvent.Payload> {

    private StandardEventHeader header;

    private Payload payload;

    @Getter
    @Setter
    @Builder
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        @NotEmpty
        private String correlationId;

        @NotEmpty
        private List<InternalRecipientAddress> internalRecipientAdresses;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private Date referenceRequestDate;

        @NotEmpty
        private String pnNationalRegistriesCxId;
    }

}