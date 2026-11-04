package it.pagopa.pn.national.registries.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder
public class GatewayEventHeader extends GenericEventHeader {

    public static final String PN_EVENT_HEADER_CLIENT_ID = "clientId";

    private String clientId;

}
