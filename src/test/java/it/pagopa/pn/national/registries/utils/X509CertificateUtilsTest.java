package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {X509CertificateUtils.class, SsmParameterConsumerActivation.class})
@ExtendWith(SpringExtension.class)
class X509CertificateUtilsTest {
    @MockitoBean
    private AdeLegalSecretConfig adeLegalSecretConfig;

    @MockitoBean
    private PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

    @MockitoBean
    private SsmClient ssmClient;

    @Autowired
    private X509CertificateUtils x509CertificateUtils;

    @MockitoBean
    private SsmParameterConsumerActivation ssmParameterConsumerActivation;

    /**
     * Method under test: {@link X509CertificateUtils#getPrivateKey(String)}
     */
    @Test
    void testGetPrivateKey() {
        when(pnNationalRegistriesSecretService.getSecret(Mockito.<String>any())).thenReturn("Secret");
        assertThrows(PnInternalException.class, () -> x509CertificateUtils.getPrivateKey("42"));
        verify(pnNationalRegistriesSecretService).getSecret(Mockito.<String>any());
    }

    /**
     * Method under test: {@link X509CertificateUtils#getPrivateKey(String)}
     */
    @Test
    void testGetPrivateKey2() {
        when(pnNationalRegistriesSecretService.getSecret(Mockito.<String>any()))
                .thenReturn("Errore durante il caricamento della private key");
        assertThrows(PnInternalException.class, () -> x509CertificateUtils.getPrivateKey("42"));
        verify(pnNationalRegistriesSecretService).getSecret(Mockito.<String>any());
    }
    /**
     * Method under test: {@link X509CertificateUtils#loadCertificate(String)}
     */
    @Test
    void testLoadCertificate3() {
        assertThrows(PnInternalException.class, () -> x509CertificateUtils.loadCertificate(""));
    }

    @Test
    void getKeyAndCertificateTest() {
        SSLData sslData = new SSLData();
        sslData.setCert("cert");
        when(ssmParameterConsumerActivation.getParameterValue(any(), any())).thenReturn(Optional.of(sslData));

        assertEquals("cert", sslData.getCert());
        assertNotNull(sslData);
    }
    @Test
    void getKeyAndCertificateTest2() {
        when(ssmParameterConsumerActivation.getParameterValue(any(), any())).thenReturn(Optional.empty());
        assertThrows(PnInternalException.class, () -> x509CertificateUtils.getKeyAndCertificate("/data"));
    }
}

