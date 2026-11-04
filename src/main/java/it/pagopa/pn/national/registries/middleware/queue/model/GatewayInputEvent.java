package it.pagopa.pn.national.registries.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GatewayInputEvent implements GenericEvent<GatewayEventHeader, InternalCodeSqsDto> {

    private GatewayEventHeader header;
    private InternalCodeSqsDto payload;

    @Override
    public GatewayEventHeader getHeader() {
        return header;
    }

    @Override
    public InternalCodeSqsDto getPayload() {
        return payload;
    }
}
