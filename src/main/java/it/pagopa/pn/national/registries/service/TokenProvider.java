package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.infocamere.InfoCamereTokenClient;
import it.pagopa.pn.national.registries.client.pdnd.PdndClient;
import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.generated.openapi.msclient.pdnd.v1.dto.ClientCredentialsResponse;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider {

    private final PdndAssertionGenerator assertionGenerator;
    private final PdndClient pdndClient;
    private final InfoCamereTokenClient infoCamereTokenClient;
    private final NationalRegistriesConfig nationalRegistriesConfig;

    public Mono<ClientCredentialsResponse> getTokenPdnd(PdndSecretValue pdndSecretValue) {
        String clientAssertion = assertionGenerator.generateClientAssertion(pdndSecretValue);
        NationalRegistriesConfig.Pdnd pdnd = nationalRegistriesConfig.getPdnd();
        return pdndClient.createToken(clientAssertion, pdnd.getClientAssertionType(), pdnd.getGrantType(), pdndSecretValue.getClientId());
    }

    public Mono<String> getTokenInfoCamere(String scope) {
        return infoCamereTokenClient.getToken(scope);
    }
}
