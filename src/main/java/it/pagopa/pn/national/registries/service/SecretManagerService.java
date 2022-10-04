package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.exceptions.PdndTokenGeneratorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

@Slf4j
@Service
public class SecretManagerService {

    private final SecretsManagerClient secretsManagerClient;

    public SecretManagerService(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;
    }

    public Optional<GetSecretValueResponse> getSecretValue(String secretName) {
        if (!StringUtils.hasText(secretName)) {
            log.warn("missing secret name");
            return Optional.empty();
        }
        GetSecretValueRequest secretValueRequest = GetSecretValueRequest.builder().secretId(secretName).build();
        try {
            return Optional.of(secretsManagerClient.getSecretValue(secretValueRequest));
        } catch (Exception e) {
            throw new PdndTokenGeneratorException(e);
        }
    }

}

