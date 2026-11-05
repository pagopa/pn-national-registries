package it.pagopa.pn.national.registries.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GatewayOutputEvent implements GenericEvent<GatewayEventHeader, CodeSqsDto> {

    private GatewayEventHeader header;
    private CodeSqsDto payload;

    @Override
    public GatewayEventHeader getHeader() {
        return header;
    }

    @Override
    public CodeSqsDto getPayload() {
        return payload;
    }
}
