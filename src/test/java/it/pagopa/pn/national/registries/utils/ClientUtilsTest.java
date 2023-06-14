package it.pagopa.pn.national.registries.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.TokenHeader;
import it.pagopa.pn.national.registries.model.TokenPayload;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.kms.model.SignRequest;

import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientUtilsTest {

    @Test
    void testGetPublicKey() {
        assertThrows(InvalidKeySpecException.class, () -> ClientUtils.getPublicKey("test"));
    }

    @Test
    void testGetPrivateKey() {
        assertThrows(InvalidKeySpecException.class, () -> ClientUtils.getPrivateKey("test"));
    }

    @Test
    void testCreateJwtContent() throws JsonProcessingException {
        Map<String, Object> header = new HashMap<>();
        assertNotNull(ClientUtils.createJwtContent(header,header));
    }

    @Test
    void createSignRequest(){
        assertNotNull(ClientUtils.createSignRequest("jwt","key"));
    }

    /**
     * Method under test: {@link ClientUtils#createSignRequest(String, String)}
     */
    @Test
    void testCreateSignRequest() {
        SignRequest actualCreateSignRequestResult = ClientUtils.createSignRequest("Not all who wander are lost", "42");
        assertEquals("42", actualCreateSignRequestResult.keyId());
        assertFalse(actualCreateSignRequestResult.hasGrantTokens());
    }

    /**
     * Method under test: {@link ClientUtils#createJwtContent(TokenHeader, TokenPayload)}
     */
    @Test
    void testCreateJwtContent2() throws JsonProcessingException {
        JwtConfig jwtCfg = mock(JwtConfig.class);
        when(jwtCfg.getKid()).thenReturn("Kid");
        TokenHeader header = new TokenHeader(jwtCfg);
        ClientUtils.createJwtContent(header, new TokenPayload(new JwtConfig()));
        verify(jwtCfg).getKid();
    }

}

