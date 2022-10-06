package it.pagopa.pn.national.registries.client.anpr;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.service.SecretManagerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgidJwtSignatureTest {

    @Mock
    SecretManagerService secretManagerService;

    @Test
    void testCreateAgidJWT() {
        GetSecretValueResponse response2 = GetSecretValueResponse.builder().secretString("{\n" +
                "\"iss\":\"iss\",\n" +
                "\"sub\":\"sub\",\n" +
                "\"aud\":\"aud\",\n" +
                "\"kid\":\"kid\",\n" +
                "\"purposeId\":\"purposeId\"\n" +
                "}").build();
        GetSecretValueResponse response1 = GetSecretValueResponse.builder().secretString("{\n" +
                "\"cert\":\"cert\",\n" +
                "\"key\":\"key\",\n" +
                "\"pub\":\"pub\",\n" +
                "\"trust\":\"dGVzdA==\"\n" +
                "}").build();
        AgidJwtSignature agidJwtSignature = new AgidJwtSignature("secret1",
                "secret2",
                secretManagerService,
                new ObjectMapper());
        when(secretManagerService.getSecretValue("secret1")).thenReturn(Optional.of(response1));
        when(secretManagerService.getSecretValue("secret2")).thenReturn(Optional.of(response2));
        String digest = "digest";
        Assertions.assertThrows(PnInternalException.class,()->agidJwtSignature.createAgidJwt(digest));
    }

}

