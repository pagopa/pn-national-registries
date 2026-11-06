package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecretManagerServiceTest {
    @InjectMocks
    SecretManagerService secretManagerService;

    @Mock
    SecretsManagerClient secretsManagerClient;

    @Test
    void getSecretValue() {
        GetSecretValueResponse response = GetSecretValueResponse.builder().secretString("test").build();
        when(secretsManagerClient.getSecretValue((GetSecretValueRequest) any())).thenReturn(response);
        Assertions.assertEquals(Optional.of(response), secretManagerService.getSecretValue("test"));

        ArgumentCaptor<GetSecretValueRequest> requestCaptor = ArgumentCaptor.forClass(GetSecretValueRequest.class);
        verify(secretsManagerClient).getSecretValue(requestCaptor.capture());
        Assertions.assertEquals("test", requestCaptor.getValue().secretId());
    }

    @Test
    void getSecretValueThrow() {
        when(secretsManagerClient.getSecretValue((GetSecretValueRequest) any())).thenThrow(ResourceNotFoundException.class);
        Assertions.assertThrows(PnInternalException.class,() -> secretManagerService.getSecretValue("test"));
    }

    @Test
    void getSecretValueEmpty() {
        Assertions.assertEquals(Optional.empty(),secretManagerService.getSecretValue(""));
        Assertions.assertEquals(Optional.empty(),secretManagerService.getSecretValue(" "));
        Assertions.assertEquals(Optional.empty(),secretManagerService.getSecretValue(null));
        verifyNoInteractions(secretsManagerClient);
    }

    @Test
    void getSecretValueShouldReturnEmptyWhenClientReturnsNull() {
        when(secretsManagerClient.getSecretValue((GetSecretValueRequest) any())).thenReturn(null);

        Assertions.assertEquals(Optional.empty(), secretManagerService.getSecretValue("test"));

        verify(secretsManagerClient).getSecretValue(any(GetSecretValueRequest.class));
    }
}