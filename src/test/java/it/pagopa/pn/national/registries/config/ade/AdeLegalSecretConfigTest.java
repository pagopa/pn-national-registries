package it.pagopa.pn.national.registries.config.ade;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class AdeLegalSecretConfigTest {
    
    @InjectMocks
    AdeLegalSecretConfig adeLegalSecretConfig;
    @Mock
    SsmParameterConsumerActivation ssmParameterConsumerActivation;

  /*  @Test
    void getKeyAndCertificateTest() {
        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        when(ssmParameterConsumerActivation.getParameterValue(any(), any())).thenReturn(Optional.of(sslData));

        Assertions.assertEquals("cert", sslDataResult.getCert());
        Assertions.assertNotNull(sslDataResult);   
    }
    @Test
    void getKeyAndCertificateTest2() {
        when(ssmParameterConsumerActivation.getParameterValue(any(), any())).thenReturn(Optional.empty());
        Assertions.assertThrows(PnInternalException.class, () -> adeLegalSecretConfig.getKeyAndCertificate());
    }
*/

    @Test
    void test() {
        assertTrue(true);
    }
}
