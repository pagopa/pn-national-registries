package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.CachedSecretsManagerConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {PnNationalRegistriesSecretService.class, CachedSecretsManagerConsumer.class})
@ExtendWith(SpringExtension.class)
class PnNationalRegistriesSecretServiceTest {
    @Autowired
    private PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @MockBean
    private SecretsManagerClient secretsManagerClient;

    @MockBean
    private CachedSecretsManagerConsumer cachedSecretsManagerConsumer;

    @Test
    void getPdndSecretValue(){
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("secret").build();
        when(cachedSecretsManagerConsumer.getSecretValue(anyString())).thenReturn(Optional.of(getSecretValueResponse));
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getPdndSecretValue("secretId"));
    }

    @Test
    void getTrustedCertFromSecret(){
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("secret").build();
        when(cachedSecretsManagerConsumer.getSecretValue(anyString())).thenReturn(Optional.of(getSecretValueResponse));
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getTrustedCertFromSecret("42"));
    }

    @Test
    void getIpaSecret(){
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("secret").build();
        when(cachedSecretsManagerConsumer.getSecretValue(anyString())).thenReturn(Optional.of(getSecretValueResponse));
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getIpaSecret("42"));
    }

    @Test
    void getSecret(){
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("secret").build();
        when(cachedSecretsManagerConsumer.getSecretValue(anyString())).thenReturn(Optional.of(getSecretValueResponse));
        assertNotNull(pnNationalRegistriesSecretService.getSecret("42"));
    }

    /**
     * Method under test: {@link PnNationalRegistriesSecretService#getPdndSecretValue(String)}
     */
    @Test
    void testGetPdndSecretValue5() throws AwsServiceException, SdkClientException {
        when(secretsManagerClient.getSecretValue(Mockito.<GetSecretValueRequest>any()))
                .thenThrow(new PnInternalException("An error occurred",""));
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getPdndSecretValue(""));
    }



    /**
     * Method under test: {@link PnNationalRegistriesSecretService#getTrustedCertFromSecret(String)}
     */
    @Test
    void testGetTrustedCertFromSecret5() throws AwsServiceException, SdkClientException {
        when(secretsManagerClient.getSecretValue(Mockito.<GetSecretValueRequest>any()))
                .thenThrow(new PnInternalException("An error occurred",""));
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getTrustedCertFromSecret(""));
    }

    /**
     * Method under test: {@link PnNationalRegistriesSecretService#getIpaSecret(String)}
     */
    @Test
    void testGetIpaSecret4() throws AwsServiceException, SdkClientException {
        when(secretsManagerClient.getSecretValue(Mockito.<GetSecretValueRequest>any()))
                .thenThrow(new PnInternalException("An error occurred",""));
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getIpaSecret(""));
    }

    /**
     * Method under test: {@link PnNationalRegistriesSecretService#getSecret(String)}
     */
    @Test
    void testGetSecret5() throws AwsServiceException, SdkClientException {
        when(secretsManagerClient.getSecretValue(Mockito.<GetSecretValueRequest>any()))
                .thenThrow(new PnInternalException("An error occurred",""));
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getSecret(""));
    }
}

