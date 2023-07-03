package it.pagopa.pn.national.registries.client.infocamere;

import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.model.SSLData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {InfoCamereJwsGenerator.class, String.class})
@ExtendWith(SpringExtension.class)
class InfoCamereJwsGeneratorTest {

    @Autowired
    private InfoCamereJwsGenerator authRest;

    @MockBean
    private KmsClient kmsClient;

    @MockBean
    private SsmParameterConsumerActivation ssmParameterConsumerActivation;

    @Test
    void testCreateAuthRest() {
        InfoCamereJwsGenerator authRest = new InfoCamereJwsGenerator(kmsClient, ssmParameterConsumerActivation, "aud", "clientID", "infoCamereAuthRestSecret");
        String scope = "test_scope";
        SSLData sslData = new SSLData();
        sslData.setCert("TestCert");
        sslData.setKeyId("KeyID");
        when(ssmParameterConsumerActivation.getParameterValue(any(), any())).thenReturn(Optional.of(sslData));
        SignResponse signResponse = SignResponse.builder()
                .signature(SdkBytes.fromByteArray("".getBytes(StandardCharsets.UTF_8)))
                .build();
        when(kmsClient.sign((SignRequest) any())).thenReturn(signResponse);
        Assertions.assertDoesNotThrow(() -> authRest.createAuthRest(scope));
    }
}
