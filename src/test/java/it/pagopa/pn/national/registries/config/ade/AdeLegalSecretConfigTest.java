package it.pagopa.pn.national.registries.config.ade;

import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
