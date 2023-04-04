package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.pdnd.PdndClient;
import it.pagopa.pn.national.registries.model.ClientCredentialsResponseDto;
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

    public TokenProvider(PdndAssertionGenerator assertionGenerator,
                         PdndClient pdndClient,
                         @Value("${pn.national-registries.pdnd.client-assertion-type}") String clientAssertionType,
                         @Value("${pn.national-registries.pdnd.grant-type}") String grantType) {
        this.assertionGenerator = assertionGenerator;
        this.clientAssertionType = clientAssertionType;
        this.grantType = grantType;
        this.pdndClient = pdndClient;
    }

    public Mono<ClientCredentialsResponseDto> getTokenPdnd(PdndSecretValue pdndSecretValue) {
        String clientAssertion = assertionGenerator.generateClientAssertion(pdndSecretValue);
        return pdndClient.createToken(clientAssertion, clientAssertionType, grantType, pdndSecretValue.getClientId())
                .map(clientCredentialsResponseDto -> clientCredentialsResponseDto);
    }
}
