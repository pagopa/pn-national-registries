package it.pagopa.pn.national.registries.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.model.SecretValue;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static it.pagopa.pn.national.registries.exceptions.PnNationalregistriesExceptionCodes.*;

@Slf4j
@Component
public class PnNationlRegistriesSecretConfig {

    private final SecretManagerService secretManagerService;

    public PnNationlRegistriesSecretConfig(SecretManagerService secretManagerService) {
        this.secretManagerService = secretManagerService;
    }

    protected SecretValue getSecretValue(String purposeId) {
        Optional<GetSecretValueResponse> opt = secretManagerService.getSecretValue(purposeId);
        if(opt.isPresent()){
            return convertToSecretValueObject(opt.get().secretString());
        }else{
            log.info("secret value not found");
            throw new PnInternalException(ERROR_MESSAGE_SECRET_MANAGER, ERROR_CODE_SECRET_MANAGER, new Throwable());
        }
    }

    protected SSLData getSslDataSecretValue(String secretName) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            Optional<GetSecretValueResponse> opt = secretManagerService.getSecretValue(secretName);
            if(opt.isPresent()){
                return mapper.readValue(opt.get().secretString(), SSLData.class);
            }else{
                log.info("secret value not found");
                throw new PnInternalException(ERROR_MESSAGE_SECRET_MANAGER, ERROR_CODE_SECRET_MANAGER, new Throwable());
            }

        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_SECRET_MANAGER, ERROR_CODE_SECRET_MANAGER, e);
        }
    }

    private SecretValue convertToSecretValueObject(String value) {
        ObjectMapper mapper = new ObjectMapper();
        SecretValue secretValue;
        try {
            secretValue = mapper.readValue(value, SecretValue.class);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_ANPR, ERROR_CODE_ADDRESS_ANPR, e);
        }
        return secretValue;
    }
}
