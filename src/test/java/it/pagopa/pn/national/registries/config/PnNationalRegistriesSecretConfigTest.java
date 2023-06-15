package it.pagopa.pn.national.registries.config;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import it.pagopa.pn.national.registries.model.TrustData;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {PnNationalRegistriesSecretConfig.class})
@ExtendWith(MockitoExtension.class)
class PnNationalRegistriesSecretConfigTest {

    @Mock
    private SecretManagerService secretManagerService;

    @InjectMocks
    private PnNationalRegistriesSecretConfig pnNationalRegistriesSecretConfig;

    @Test
    @DisplayName("Should throw an exception when the secretname is not found")
    void getSslDataSecretValueWhenSecretNameIsNotFoundThenThrowException() {
        when(secretManagerService.getSecretValue(anyString())).thenReturn(Optional.empty());
        assertThrows(
                PnInternalException.class,
                () -> pnNationalRegistriesSecretConfig.getTrustedCertFromSecret("secretName"));
    }

    @Test
    @DisplayName("Should return the ssldata when the secretname is found")
    void getSslDataSecretValueWhenSecretNameIsFound() {
        String secretName = "secretName";
        String secretString =
                "{\"trust\":\"trust\"}";
        GetSecretValueResponse getSecretValueResponse =
                GetSecretValueResponse.builder().secretString(secretString).build();
        when(secretManagerService.getSecretValue(anyString()))
                .thenReturn(Optional.of(getSecretValueResponse));
        TrustData trust = pnNationalRegistriesSecretConfig.getTrustedCertFromSecret(secretName);
        assertNotNull(trust);
        assertEquals("trust", trust.getTrust());
    }

    /**
     * Method under test: {@link PnNationalRegistriesSecretConfig#getIpaSecret(String)}
     */
    @Test
    void testGetIpaSecret() {
        when(secretManagerService.getSecretValue(org.mockito.Mockito.any())).thenReturn(Optional.empty());
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretConfig.getIpaSecret("Auth Id Secret"));
        verify(secretManagerService).getSecretValue(org.mockito.Mockito.any());
    }


    /**
     * Method under test: {@link PnNationalRegistriesSecretConfig#getIpaSecret(String)}
     */
    @Test
    void testGetIpaSecret2() {
        when(secretManagerService.getSecretValue(org.mockito.Mockito.any()))
                .thenThrow(new PnInternalException("An error occurred", ""));
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretConfig.getIpaSecret("Auth Id Secret"));
        verify(secretManagerService).getSecretValue(org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("Should throw an exception when the secret is not found")
    void getSecretValueWhenSecretIsNotFoundThenThrowException() {
        when(secretManagerService.getSecretValue(any())).thenReturn(Optional.empty());
        assertThrows(PnInternalException.class, () -> pnNationalRegistriesSecretConfig.getPdndSecretValue("", ""));
    }

    @Test
    @DisplayName("Should return secret value when the secret is found")
    void getSecretValueWhenSecretIsFound() {
        String secretId = "secretName";
        String purposeId = "purposeId";
        String secretValue =
                "{\"client_id\":\"clientId\",\"key_id\":\"keyId\",\"jwt_config\":{\"issuer\":\"issuer\",\"audience\":\"audience\",\"subject\":\"subject\",\"expires_in\":3600,\"algorithm\":\"RS256\",\"key_type\":\"RSA\",\"key_size\":2048}}";
        GetSecretValueResponse getSecretValueResponse =
                GetSecretValueResponse.builder().secretString(secretValue).build();
        when(secretManagerService.getSecretValue(any()))
                .thenReturn(Optional.of(getSecretValueResponse));

        PdndSecretValue secret = pnNationalRegistriesSecretConfig.getPdndSecretValue(purposeId, secretId);

        assertNotNull(secret);
        assertEquals("clientId", secret.getClientId());
        assertNull(secret.getKeyId());
    }
}
