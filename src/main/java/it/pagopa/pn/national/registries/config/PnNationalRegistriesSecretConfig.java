package it.pagopa.pn.national.registries.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.ipa.IpaSecret;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesExceptionCodes.*;

@Slf4j
@Component
public class PnNationalRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;

    public PnNationalRegistriesSecretConfig(SecretManagerService secretManagerService) {
        this.secretManagerService = secretManagerService;
    }

    protected PdndSecretValue getPdndSecretValue(String purposeId, String secretId) {
        Optional<GetSecretValueResponse> opt = secretManagerService.getSecretValue(secretId);
        if (opt.isPresent()) {
            log.info("founded secret for purposeId: {} and secretId: {}", purposeId, secretId);
            return convertToSecretValueObject(opt.get().secretString(), PdndSecretValue.class);
        } else {
            log.warn("secret value for purposeId: {} and secretId: {} not found", purposeId, secretId);
            throw new PnInternalException(ERROR_MESSAGE_SECRET_MANAGER, ERROR_CODE_SECRET_MANAGER, new Throwable());
        }
    }

    protected SSLData getSslDataSecretValue(String secretId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Optional<GetSecretValueResponse> opt = secretManagerService.getSecretValue(secretId);
            if (opt.isPresent()) {
                log.info("founded secret value for secret: {}", secretId);
                return mapper.readValue(opt.get().secretString(), SSLData.class);
            } else {
                log.warn("secret value for secret: {} not found", secretId);
                throw new PnInternalException(ERROR_MESSAGE_SECRET_MANAGER, ERROR_CODE_SECRET_MANAGER, new Throwable());
            }
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_SECRET_MANAGER, ERROR_CODE_SECRET_MANAGER, e);
        }
    }

    protected IpaSecret getIpaSecret(String secretId) {
        Optional<GetSecretValueResponse> opt = secretManagerService.getSecretValue(secretId);
        if (opt.isPresent()) {
            log.info("founded AUTH_ID for Ipa client");
            return convertToSecretValueObject(opt.get().secretString(), IpaSecret.class);
        } else {
            log.warn("AUTH_ID for Ipa client not found");
            throw new PnInternalException(ERROR_MESSAGE_SECRET_MANAGER, ERROR_CODE_SECRET_MANAGER, new Throwable());
        }
    }

    private <T> T convertToSecretValueObject(String value, Class<T> type) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_SECRET_MANAGER_CONVERTER, ERROR_CODE_SECRET_MANAGER_CONVERTER, e);
        }
    }
}
