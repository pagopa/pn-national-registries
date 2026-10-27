package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.CachedSecretsManagerConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {PnNationalRegistriesSecretService.class, CachedSecretsManagerConsumer.class})
@ExtendWith(SpringExtension.class)
class PnNationalRegistriesSecretServiceTest {
    @Autowired
    private PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @MockitoBean
    private SecretsManagerClient secretsManagerClient;

    @MockitoBean
    private CachedSecretsManagerConsumer cachedSecretsManagerConsumer;

    @Test
    void getPdndSecretValue(){
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("secret").build();
        when(cachedSecretsManagerConsumer.getSecretValue(anyString())).thenReturn(Optional.of(getSecretValueResponse));
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getPdndSecretValue("secretId"));
    }

    @Test
    void getPdndSecretValueSuccess(){
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("{}").build();
        when(cachedSecretsManagerConsumer.getSecretValue("secretId")).thenReturn(Optional.of(getSecretValueResponse));
        assertNotNull(pnNationalRegistriesSecretService.getPdndSecretValue("secretId"));
    }

    @Test
    void getTrustedCertFromSecret(){
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("secret").build();
        when(cachedSecretsManagerConsumer.getSecretValue(anyString())).thenReturn(Optional.of(getSecretValueResponse));
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getTrustedCertFromSecret("42"));
    }

    @Test
    void getTrustedCertFromSecretSuccess(){
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("{}").build();
        when(cachedSecretsManagerConsumer.getSecretValue("42")).thenReturn(Optional.of(getSecretValueResponse));
        assertNotNull(pnNationalRegistriesSecretService.getTrustedCertFromSecret("42"));
    }

    @Test
    void getIpaSecret(){
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("secret").build();
        when(cachedSecretsManagerConsumer.getSecretValue(anyString())).thenReturn(Optional.of(getSecretValueResponse));
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getIpaSecret("42"));
    }

    @Test
    void getIpaSecretSuccess(){
        GetSecretValueResponse getSecretValueResponse = GetSecretValueResponse.builder().secretString("{\"AUTH_ID\":\"auth-id\"}").build();
        when(cachedSecretsManagerConsumer.getSecretValue("42")).thenReturn(Optional.of(getSecretValueResponse));
        assertEquals("auth-id", pnNationalRegistriesSecretService.getIpaSecret("42").getAuthId());
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
    void testGetPdndSecretValue5() {
        when(cachedSecretsManagerConsumer.getSecretValue(anyString())).thenReturn(Optional.empty());
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getPdndSecretValue(""));
    }



    /**
     * Method under test: {@link PnNationalRegistriesSecretService#getTrustedCertFromSecret(String)}
     */
    @Test
    void testGetTrustedCertFromSecret5() {
        when(cachedSecretsManagerConsumer.getSecretValue(anyString())).thenReturn(Optional.empty());
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getTrustedCertFromSecret(""));
    }

    /**
     * Method under test: {@link PnNationalRegistriesSecretService#getIpaSecret(String)}
     */
    @Test
    void testGetIpaSecret4() {
        when(cachedSecretsManagerConsumer.getSecretValue(anyString())).thenReturn(Optional.empty());
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getIpaSecret(""));
    }

    /**
     * Method under test: {@link PnNationalRegistriesSecretService#getSecret(String)}
     */
    @Test
    void testGetSecret5() {
        when(cachedSecretsManagerConsumer.getSecretValue(anyString())).thenReturn(Optional.empty());
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretService.getSecret(""));
    }
}