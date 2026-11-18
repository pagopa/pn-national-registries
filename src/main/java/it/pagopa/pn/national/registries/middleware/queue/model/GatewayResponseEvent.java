package it.pagopa.pn.national.registries.middleware.queue.model;


import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.national.registries.model.GatewayResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GatewayResponseEvent implements GenericEvent<GenericEventHeader, GatewayResponse> {

    private GenericEventHeader header;
    private GatewayResponse payload;

    @Override
    public GenericEventHeader getHeader() {
        return header;
    }

    @Override
    public GatewayResponse getPayload() {
        return payload;
    }
}
