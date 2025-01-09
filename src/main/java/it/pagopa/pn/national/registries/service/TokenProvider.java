package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereTokenClient;
import it.pagopa.pn.national.registries.client.pdnd.PdndClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.ClientCredentialsResponse;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class TokenProvider {

    private final String clientAssertionType;
    private final String grantType;
    private final PdndAssertionGenerator assertionGenerator;
    private final PdndClient pdndClient;
    private final InfoCamereTokenClient infoCamereTokenClient;

    public TokenProvider(PdndAssertionGenerator assertionGenerator,
                         PdndClient pdndClient,
                         InfoCamereTokenClient infoCamereTokenClient,
                         @Value("${pn.national-registries.pdnd.client-assertion-type}") String clientAssertionType,
                         @Value("${pn.national-registries.pdnd.grant-type}") String grantType) {
        this.assertionGenerator = assertionGenerator;
        this.clientAssertionType = clientAssertionType;
        this.grantType = grantType;
        this.pdndClient = pdndClient;
        this.infoCamereTokenClient = infoCamereTokenClient;
    }

    public Mono<ClientCredentialsResponse> getTokenPdnd(PdndSecretValue pdndSecretValue, PnAuditLogEvent logEvent) {
        String clientAssertion = assertionGenerator.generateClientAssertion(pdndSecretValue);
        return pdndClient.createToken(clientAssertion, clientAssertionType, grantType, pdndSecretValue.getClientId(), logEvent);
    }

    public Mono<String> getTokenInfoCamere(String scope, PnAuditLogEvent logEvent) {
        return infoCamereTokenClient.getToken(scope, logEvent);
    }
}
