package it.pagopa.pn.national.registries;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.ApiClient;
import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.api.AuthApi;
import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.SecretValue;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import static it.pagopa.pn.national.registries.utils.TokenProviderUtils.convertToSecretValueObject;

@Slf4j
@Component
public class TokenProvider {

    private final String pdndUrl;
    private final String clientAssertionType;
    private final String grantType;

    private final PdndAssertionGenerator assertionGenerator;
    private final SecretManagerService secretManagerService;
    private final AuthApi authApi;

    public TokenProvider(PdndAssertionGenerator assertionGenerator,
                         SecretManagerService secretManagerService,
                         @Value("${pdnd.base-path}") String pdndUrl,
                         @Value("${pdnd.client-assertion-type}") String clientAssertionType,
                         @Value("${pdnd.grant-type}") String grantType
    ) {
        this.assertionGenerator = assertionGenerator;
        this.secretManagerService = secretManagerService;
        this.pdndUrl = pdndUrl;
        this.clientAssertionType = clientAssertionType;
        this.grantType = grantType;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected( connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(10000, TimeUnit.MILLISECONDS)));

        WebClient webClient = ApiClient.buildWebClientBuilder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        ApiClient newApiClient = new ApiClient(webClient);
        newApiClient.setBasePath(pdndUrl);
        authApi = new AuthApi(newApiClient);
    }

    public Mono<ClientCredentialsResponseDto> getToken(String purposeId) throws Exception {
        Optional<GetSecretValueResponse> getSecretValueResponse = secretManagerService.getSecretValue(purposeId);
        if(getSecretValueResponse.isEmpty()){
            //TODO: GESTIONE ECCEZIONI
            log.info("secret value not found");
            return Mono.empty();
        }
        SecretValue secretValue = convertToSecretValueObject(getSecretValueResponse.get().secretString());
        String clientAssertion = assertionGenerator.generateClientAssertion(secretValue);

        return authApi.createToken(clientAssertion,clientAssertionType,grantType,secretValue.getClientId());
    }


}
