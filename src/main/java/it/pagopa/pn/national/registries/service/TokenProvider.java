package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.client.pdnd.AuthApiCustom;
import it.pagopa.pn.national.registries.generated.openapi.pdnd.client.v1.dto.ClientCredentialsResponseDto;
import it.pagopa.pn.national.registries.model.SecretValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.*;

@Slf4j
@Component
public class TokenProvider {

    private final String clientAssertionType;
    private final String grantType;
    private final PdndAssertionGenerator assertionGenerator;
    private final SecretManagerService secretManagerService;
    private final AuthApiCustom authApiCustom;
    private final String pdndBasePath;

    public TokenProvider(PdndAssertionGenerator assertionGenerator,
                         SecretManagerService secretManagerService,
                         AuthApiCustom authApiCustom,
                         @Value("${pn.national-registries.pdnd.client-assertion-type}") String clientAssertionType,
                         @Value("${pn.national-registries.pdnd.grant-type}") String grantType,
                         @Value("${pdnd.base-path}") String pdndBasePath
    ) {
        this.assertionGenerator = assertionGenerator;
        this.secretManagerService = secretManagerService;
        this.clientAssertionType = clientAssertionType;
        this.grantType = grantType;
        this.authApiCustom = authApiCustom;
        this.pdndBasePath = pdndBasePath;
    }

    public Mono<ClientCredentialsResponseDto> getToken(String purposeId) {
        Optional<GetSecretValueResponse> getSecretValueResponse = secretManagerService.getSecretValue(purposeId);
        if (getSecretValueResponse.isEmpty()) {
            log.info("secret value not found");
            return Mono.empty();
        }
        SecretValue secretValue = convertToSecretValueObject(getSecretValueResponse.get().secretString());
        String clientAssertion = assertionGenerator.generateClientAssertion(secretValue);
        authApiCustom.getApiClient().setBasePath(pdndBasePath);
        Mono<ClientCredentialsResponseDto> resp = authApiCustom.createToken(clientAssertion, clientAssertionType, grantType, secretValue.getClientId());
        return resp.map(clientCredentialsResponseDto -> clientCredentialsResponseDto);
    }

    private SecretValue convertToSecretValueObject(String value) {
        ObjectMapper mapper = new ObjectMapper();
        SecretValue secretValue;
        try {
            secretValue = mapper.readValue(value, SecretValue.class);
        } catch (JsonProcessingException e) {
                throw new PnInternalException(ERROR_MESSAGE_PDND_TOKEN, ERROR_CODE_PDND_TOKEN,e);
        }
        return secretValue;
    }
}
