package it.pagopa.pn.national.registries.client.anpr;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.anpr.AnprSecretConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.security.spec.InvalidKeySpecException;

@ExtendWith(MockitoExtension.class)
class AgidJwtSignatureTest {

    @Mock
    AnprSecretConfig anprSecretConfig;

    @Test
    void testCreateAgidJWT() {
        AgidJwtSignature agidJwtSignature = new AgidJwtSignature("", anprSecretConfig);
        String digest = "digest";
        Assertions.assertThrows(PnInternalException.class,()->agidJwtSignature.createAgidJwt(digest));
    }

    @Test
    void testCreateAgidJWT2() {
        AgidJwtSignature agidJwtSignature = new AgidJwtSignature("secret1", anprSecretConfig);
        String digest = "digest";
        Assertions.assertNull(agidJwtSignature.createAgidJwt(digest));
    }

    @Test
    void testgetPrivateKey() {
        AgidJwtSignature agidJwtSignature = new AgidJwtSignature("secret1",anprSecretConfig);
        Assertions.assertThrows(InvalidKeySpecException.class,()->agidJwtSignature.getPrivateKey("dGVzdA=="));
    }

}

