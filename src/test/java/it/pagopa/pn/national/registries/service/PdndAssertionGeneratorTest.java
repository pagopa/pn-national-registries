package it.pagopa.pn.national.registries.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.model.JwtConfig;
import it.pagopa.pn.national.registries.model.PdndSecretValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;

@ExtendWith(MockitoExtension.class)
class PdndAssertionGeneratorTest {

    @InjectMocks
    private PdndAssertionGenerator pdndAssertionGenerator;

    /**
     * Method under test: {@link PdndAssertionGenerator#generateClientAssertion(PdndSecretValue)}
     */
    @Test
    void testGenerateClientAssertion() throws AwsServiceException, SdkClientException {

        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("Audience");
        jwtConfig.setIssuer("Issuer");
        jwtConfig.setKid("Kid");
        jwtConfig.setPurposeId("42");
        jwtConfig.setSubject("Hello from the Dreaming Spires");

        JwtConfig jwtConfig1 = new JwtConfig();
        jwtConfig1.setAudience("Audience");
        jwtConfig1.setIssuer("Issuer");
        jwtConfig1.setKid("Kid");
        jwtConfig1.setPurposeId("42");
        jwtConfig1.setSubject("Hello from the Dreaming Spires");
        PdndSecretValue pdndSecretValue = mock(PdndSecretValue.class);
        when(pdndSecretValue.getKeyId()).thenThrow(new PnInternalException("","",new Throwable()));
        when(pdndSecretValue.getJwtConfig()).thenReturn(jwtConfig1);
        doNothing().when(pdndSecretValue).setClientId(any());
        doNothing().when(pdndSecretValue).setJwtConfig(any());
        doNothing().when(pdndSecretValue).setKeyId(any());
        pdndSecretValue.setClientId("42");
        pdndSecretValue.setJwtConfig(jwtConfig);
        pdndSecretValue.setKeyId("42");
        assertThrows(PnInternalException.class, () -> pdndAssertionGenerator.generateClientAssertion(pdndSecretValue));
        verify(pdndSecretValue, atLeast(1)).getJwtConfig();
        verify(pdndSecretValue).getKeyId();
        verify(pdndSecretValue).setClientId(any());
        verify(pdndSecretValue).setJwtConfig(any());
        verify(pdndSecretValue).setKeyId(any());
    }

    /**
     * Method under test: {@link PdndAssertionGenerator#generateClientAssertion(PdndSecretValue)}
     */
    @Test
    void testGenerateClientAssertion2() throws AwsServiceException, SdkClientException {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("Audience");
        jwtConfig.setIssuer("Issuer");
        jwtConfig.setKid("Kid");
        jwtConfig.setPurposeId("42");
        jwtConfig.setSubject("Hello from the Dreaming Spires");
        JwtConfig jwtConfig1 = mock(JwtConfig.class);
        when(jwtConfig1.getAudience()).thenReturn("foo");
        when(jwtConfig1.getIssuer()).thenReturn("Issuer");
        when(jwtConfig1.getKid()).thenReturn("Kid");
        when(jwtConfig1.getPurposeId()).thenReturn("42");
        when(jwtConfig1.getSubject()).thenReturn("Hello from the Dreaming Spires");
        doNothing().when(jwtConfig1).setAudience(any());
        doNothing().when(jwtConfig1).setIssuer(any());
        doNothing().when(jwtConfig1).setKid(any());
        doNothing().when(jwtConfig1).setPurposeId(any());
        doNothing().when(jwtConfig1).setSubject(any());
        jwtConfig1.setAudience("Audience");
        jwtConfig1.setIssuer("Issuer");
        jwtConfig1.setKid("Kid");
        jwtConfig1.setPurposeId("42");
        jwtConfig1.setSubject("Hello from the Dreaming Spires");
        PdndSecretValue pdndSecretValue = mock(PdndSecretValue.class);
        when(pdndSecretValue.getKeyId()).thenThrow(new PnInternalException("","",new Throwable()));
        when(pdndSecretValue.getJwtConfig()).thenReturn(jwtConfig1);
        doNothing().when(pdndSecretValue).setClientId(any());
        doNothing().when(pdndSecretValue).setJwtConfig(any());
        doNothing().when(pdndSecretValue).setKeyId(any());
        pdndSecretValue.setClientId("42");
        pdndSecretValue.setJwtConfig(jwtConfig);
        pdndSecretValue.setKeyId("42");
        assertThrows(PnInternalException.class, () -> pdndAssertionGenerator.generateClientAssertion(pdndSecretValue));
        verify(pdndSecretValue, atLeast(1)).getJwtConfig();
        verify(pdndSecretValue).getKeyId();
        verify(pdndSecretValue).setClientId(any());
        verify(pdndSecretValue).setJwtConfig(any());
        verify(pdndSecretValue).setKeyId(any());
        verify(jwtConfig1).getAudience();
        verify(jwtConfig1).getIssuer();
        verify(jwtConfig1).getKid();
        verify(jwtConfig1).getPurposeId();
        verify(jwtConfig1).getSubject();
        verify(jwtConfig1).setAudience(any());
        verify(jwtConfig1).setIssuer(any());
        verify(jwtConfig1).setKid(any());
        verify(jwtConfig1).setPurposeId(any());
        verify(jwtConfig1).setSubject(any());
    }

    /**
     * Method under test: {@link PdndAssertionGenerator#generateClientAssertion(PdndSecretValue)}
     */
    @Test
    void testGenerateClientAssertion3() throws AwsServiceException, SdkClientException {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("Audience");
        jwtConfig.setIssuer("Issuer");
        jwtConfig.setKid("Kid");
        jwtConfig.setPurposeId("42");
        jwtConfig.setSubject("Hello from the Dreaming Spires");
        JwtConfig jwtConfig1 = mock(JwtConfig.class);
        when(jwtConfig1.getAudience()).thenReturn("it.pagopa.pn.national.registries.model.JwtConfig");
        when(jwtConfig1.getIssuer()).thenReturn("Issuer");
        when(jwtConfig1.getKid()).thenReturn("Kid");
        when(jwtConfig1.getPurposeId()).thenReturn("42");
        when(jwtConfig1.getSubject()).thenReturn("Hello from the Dreaming Spires");
        doNothing().when(jwtConfig1).setAudience(any());
        doNothing().when(jwtConfig1).setIssuer(any());
        doNothing().when(jwtConfig1).setKid(any());
        doNothing().when(jwtConfig1).setPurposeId(any());
        doNothing().when(jwtConfig1).setSubject(any());
        jwtConfig1.setAudience("Audience");
        jwtConfig1.setIssuer("Issuer");
        jwtConfig1.setKid("Kid");
        jwtConfig1.setPurposeId("42");
        jwtConfig1.setSubject("Hello from the Dreaming Spires");
        PdndSecretValue pdndSecretValue = mock(PdndSecretValue.class);
        when(pdndSecretValue.getKeyId()).thenThrow(new PnInternalException("","",new Throwable()));
        when(pdndSecretValue.getJwtConfig()).thenReturn(jwtConfig1);
        doNothing().when(pdndSecretValue).setClientId(any());
        doNothing().when(pdndSecretValue).setJwtConfig(any());
        doNothing().when(pdndSecretValue).setKeyId(any());
        pdndSecretValue.setClientId("42");
        pdndSecretValue.setJwtConfig(jwtConfig);
        pdndSecretValue.setKeyId("42");
        assertThrows(PnInternalException.class, () -> pdndAssertionGenerator.generateClientAssertion(pdndSecretValue));
        verify(pdndSecretValue, atLeast(1)).getJwtConfig();
        verify(pdndSecretValue).getKeyId();
        verify(pdndSecretValue).setClientId(any());
        verify(pdndSecretValue).setJwtConfig(any());
        verify(pdndSecretValue).setKeyId(any());
        verify(jwtConfig1).getAudience();
        verify(jwtConfig1).getIssuer();
        verify(jwtConfig1).getKid();
        verify(jwtConfig1).getPurposeId();
        verify(jwtConfig1).getSubject();
        verify(jwtConfig1).setAudience(any());
        verify(jwtConfig1).setIssuer(any());
        verify(jwtConfig1).setKid(any());
        verify(jwtConfig1).setPurposeId(any());
        verify(jwtConfig1).setSubject(any());
    }

    /**
     * Method under test: {@link PdndAssertionGenerator#generateClientAssertion(PdndSecretValue)}
     */
    @Test
    void testGenerateClientAssertion4() throws AwsServiceException, SdkClientException {

        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("Audience");
        jwtConfig.setIssuer("Issuer");
        jwtConfig.setKid("Kid");
        jwtConfig.setPurposeId("42");
        jwtConfig.setSubject("Hello from the Dreaming Spires");
        JwtConfig jwtConfig1 = mock(JwtConfig.class);
        when(jwtConfig1.getAudience()).thenReturn("");
        when(jwtConfig1.getIssuer()).thenReturn("Issuer");
        when(jwtConfig1.getKid()).thenReturn("Kid");
        when(jwtConfig1.getPurposeId()).thenReturn("42");
        when(jwtConfig1.getSubject()).thenReturn("Hello from the Dreaming Spires");
        doNothing().when(jwtConfig1).setAudience(any());
        doNothing().when(jwtConfig1).setIssuer(any());
        doNothing().when(jwtConfig1).setKid(any());
        doNothing().when(jwtConfig1).setPurposeId(any());
        doNothing().when(jwtConfig1).setSubject(any());
        jwtConfig1.setAudience("Audience");
        jwtConfig1.setIssuer("Issuer");
        jwtConfig1.setKid("Kid");
        jwtConfig1.setPurposeId("42");
        jwtConfig1.setSubject("Hello from the Dreaming Spires");
        PdndSecretValue pdndSecretValue = mock(PdndSecretValue.class);
        when(pdndSecretValue.getKeyId()).thenThrow(new PnInternalException("","",new Throwable()));
        when(pdndSecretValue.getJwtConfig()).thenReturn(jwtConfig1);
        doNothing().when(pdndSecretValue).setClientId(any());
        doNothing().when(pdndSecretValue).setJwtConfig(any());
        doNothing().when(pdndSecretValue).setKeyId(any());
        pdndSecretValue.setClientId("42");
        pdndSecretValue.setJwtConfig(jwtConfig);
        pdndSecretValue.setKeyId("42");
        assertThrows(PnInternalException.class, () -> pdndAssertionGenerator.generateClientAssertion(pdndSecretValue));
        verify(pdndSecretValue, atLeast(1)).getJwtConfig();
        verify(pdndSecretValue).getKeyId();
        verify(pdndSecretValue).setClientId(any());
        verify(pdndSecretValue).setJwtConfig(any());
        verify(pdndSecretValue).setKeyId(any());
        verify(jwtConfig1).getAudience();
        verify(jwtConfig1).getIssuer();
        verify(jwtConfig1).getKid();
        verify(jwtConfig1).getPurposeId();
        verify(jwtConfig1).getSubject();
        verify(jwtConfig1).setAudience(any());
        verify(jwtConfig1).setIssuer(any());
        verify(jwtConfig1).setKid(any());
        verify(jwtConfig1).setPurposeId(any());
        verify(jwtConfig1).setSubject(any());
    }

    /**
     * Method under test: {@link PdndAssertionGenerator#generateClientAssertion(PdndSecretValue)}
     */
    @Test
    void testGenerateClientAssertion5() throws AwsServiceException, SdkClientException {

        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("Audience");
        jwtConfig.setIssuer("Issuer");
        jwtConfig.setKid("Kid");
        jwtConfig.setPurposeId("42");
        jwtConfig.setSubject("Hello from the Dreaming Spires");
        JwtConfig jwtConfig1 = mock(JwtConfig.class);
        when(jwtConfig1.getAudience()).thenReturn("Audience");
        when(jwtConfig1.getIssuer()).thenReturn("42");
        when(jwtConfig1.getKid()).thenReturn("Kid");
        when(jwtConfig1.getPurposeId()).thenReturn("42");
        when(jwtConfig1.getSubject()).thenReturn("Hello from the Dreaming Spires");
        doNothing().when(jwtConfig1).setAudience(any());
        doNothing().when(jwtConfig1).setIssuer(any());
        doNothing().when(jwtConfig1).setKid(any());
        doNothing().when(jwtConfig1).setPurposeId(any());
        doNothing().when(jwtConfig1).setSubject(any());
        jwtConfig1.setAudience("Audience");
        jwtConfig1.setIssuer("Issuer");
        jwtConfig1.setKid("Kid");
        jwtConfig1.setPurposeId("42");
        jwtConfig1.setSubject("Hello from the Dreaming Spires");
        PdndSecretValue pdndSecretValue = mock(PdndSecretValue.class);
        when(pdndSecretValue.getKeyId()).thenThrow(new PnInternalException("","",new Throwable()));
        when(pdndSecretValue.getJwtConfig()).thenReturn(jwtConfig1);
        doNothing().when(pdndSecretValue).setClientId(any());
        doNothing().when(pdndSecretValue).setJwtConfig(any());
        doNothing().when(pdndSecretValue).setKeyId(any());
        pdndSecretValue.setClientId("42");
        pdndSecretValue.setJwtConfig(jwtConfig);
        pdndSecretValue.setKeyId("42");
        assertThrows(PnInternalException.class, () -> pdndAssertionGenerator.generateClientAssertion(pdndSecretValue));
        verify(pdndSecretValue, atLeast(1)).getJwtConfig();
        verify(pdndSecretValue).getKeyId();
        verify(pdndSecretValue).setClientId(any());
        verify(pdndSecretValue).setJwtConfig(any());
        verify(pdndSecretValue).setKeyId(any());
        verify(jwtConfig1).getAudience();
        verify(jwtConfig1).getIssuer();
        verify(jwtConfig1).getKid();
        verify(jwtConfig1).getPurposeId();
        verify(jwtConfig1).getSubject();
        verify(jwtConfig1).setAudience(any());
        verify(jwtConfig1).setIssuer(any());
        verify(jwtConfig1).setKid(any());
        verify(jwtConfig1).setPurposeId(any());
        verify(jwtConfig1).setSubject(any());
    }

    /**
     * Method under test: {@link PdndAssertionGenerator#generateClientAssertion(PdndSecretValue)}
     */
    @Test
    void testGenerateClientAssertion6() throws AwsServiceException, SdkClientException {

        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("Audience");
        jwtConfig.setIssuer("Issuer");
        jwtConfig.setKid("Kid");
        jwtConfig.setPurposeId("42");
        jwtConfig.setSubject("Hello from the Dreaming Spires");
        JwtConfig jwtConfig1 = mock(JwtConfig.class);
        when(jwtConfig1.getAudience()).thenReturn("Audience");
        when(jwtConfig1.getIssuer()).thenReturn("Issuer");
        when(jwtConfig1.getKid()).thenReturn("42");
        when(jwtConfig1.getPurposeId()).thenReturn("42");
        when(jwtConfig1.getSubject()).thenReturn("Hello from the Dreaming Spires");
        doNothing().when(jwtConfig1).setAudience(any());
        doNothing().when(jwtConfig1).setIssuer(any());
        doNothing().when(jwtConfig1).setKid(any());
        doNothing().when(jwtConfig1).setPurposeId(any());
        doNothing().when(jwtConfig1).setSubject(any());
        jwtConfig1.setAudience("Audience");
        jwtConfig1.setIssuer("Issuer");
        jwtConfig1.setKid("Kid");
        jwtConfig1.setPurposeId("42");
        jwtConfig1.setSubject("Hello from the Dreaming Spires");
        PdndSecretValue pdndSecretValue = mock(PdndSecretValue.class);
        when(pdndSecretValue.getKeyId()).thenThrow(new PnInternalException("","",new Throwable()));
        when(pdndSecretValue.getJwtConfig()).thenReturn(jwtConfig1);
        doNothing().when(pdndSecretValue).setClientId(any());
        doNothing().when(pdndSecretValue).setJwtConfig(any());
        doNothing().when(pdndSecretValue).setKeyId(any());
        pdndSecretValue.setClientId("42");
        pdndSecretValue.setJwtConfig(jwtConfig);
        pdndSecretValue.setKeyId("42");
        assertThrows(PnInternalException.class, () -> pdndAssertionGenerator.generateClientAssertion(pdndSecretValue));
        verify(pdndSecretValue, atLeast(1)).getJwtConfig();
        verify(pdndSecretValue).getKeyId();
        verify(pdndSecretValue).setClientId(any());
        verify(pdndSecretValue).setJwtConfig(any());
        verify(pdndSecretValue).setKeyId(any());
        verify(jwtConfig1).getAudience();
        verify(jwtConfig1).getIssuer();
        verify(jwtConfig1).getKid();
        verify(jwtConfig1).getPurposeId();
        verify(jwtConfig1).getSubject();
        verify(jwtConfig1).setAudience(any());
        verify(jwtConfig1).setIssuer(any());
        verify(jwtConfig1).setKid(any());
        verify(jwtConfig1).setPurposeId(any());
        verify(jwtConfig1).setSubject(any());
    }

    /**
     * Method under test: {@link PdndAssertionGenerator#generateClientAssertion(PdndSecretValue)}
     */
    @Test
    void testGenerateClientAssertion7() throws AwsServiceException, SdkClientException {

        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("Audience");
        jwtConfig.setIssuer("Issuer");
        jwtConfig.setKid("Kid");
        jwtConfig.setPurposeId("42");
        jwtConfig.setSubject("Hello from the Dreaming Spires");
        JwtConfig jwtConfig1 = mock(JwtConfig.class);
        when(jwtConfig1.getAudience()).thenReturn("Audience");
        when(jwtConfig1.getIssuer()).thenReturn("Issuer");
        when(jwtConfig1.getKid()).thenReturn("Kid");
        when(jwtConfig1.getPurposeId()).thenReturn("foo");
        when(jwtConfig1.getSubject()).thenReturn("Hello from the Dreaming Spires");
        doNothing().when(jwtConfig1).setAudience(any());
        doNothing().when(jwtConfig1).setIssuer(any());
        doNothing().when(jwtConfig1).setKid(any());
        doNothing().when(jwtConfig1).setPurposeId(any());
        doNothing().when(jwtConfig1).setSubject(any());
        jwtConfig1.setAudience("Audience");
        jwtConfig1.setIssuer("Issuer");
        jwtConfig1.setKid("Kid");
        jwtConfig1.setPurposeId("42");
        jwtConfig1.setSubject("Hello from the Dreaming Spires");
        PdndSecretValue pdndSecretValue = mock(PdndSecretValue.class);
        when(pdndSecretValue.getKeyId()).thenThrow(new PnInternalException("","",new Throwable()));
        when(pdndSecretValue.getJwtConfig()).thenReturn(jwtConfig1);
        doNothing().when(pdndSecretValue).setClientId(any());
        doNothing().when(pdndSecretValue).setJwtConfig(any());
        doNothing().when(pdndSecretValue).setKeyId(any());
        pdndSecretValue.setClientId("42");
        pdndSecretValue.setJwtConfig(jwtConfig);
        pdndSecretValue.setKeyId("42");
        assertThrows(PnInternalException.class, () -> pdndAssertionGenerator.generateClientAssertion(pdndSecretValue));
        verify(pdndSecretValue, atLeast(1)).getJwtConfig();
        verify(pdndSecretValue).getKeyId();
        verify(pdndSecretValue).setClientId(any());
        verify(pdndSecretValue).setJwtConfig(any());
        verify(pdndSecretValue).setKeyId(any());
        verify(jwtConfig1).getAudience();
        verify(jwtConfig1).getIssuer();
        verify(jwtConfig1).getKid();
        verify(jwtConfig1).getPurposeId();
        verify(jwtConfig1).getSubject();
        verify(jwtConfig1).setAudience(any());
        verify(jwtConfig1).setIssuer(any());
        verify(jwtConfig1).setKid(any());
        verify(jwtConfig1).setPurposeId(any());
        verify(jwtConfig1).setSubject(any());
    }

    /**
     * Method under test: {@link PdndAssertionGenerator#generateClientAssertion(PdndSecretValue)}
     */
    @Test
    void testGenerateClientAssertion8() throws AwsServiceException, SdkClientException {

        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("Audience");
        jwtConfig.setIssuer("Issuer");
        jwtConfig.setKid("Kid");
        jwtConfig.setPurposeId("42");
        jwtConfig.setSubject("Hello from the Dreaming Spires");
        JwtConfig jwtConfig1 = mock(JwtConfig.class);
        when(jwtConfig1.getAudience()).thenReturn("Audience");
        when(jwtConfig1.getIssuer()).thenReturn("Issuer");
        when(jwtConfig1.getKid()).thenReturn("Kid");
        when(jwtConfig1.getPurposeId()).thenReturn("it.pagopa.pn.national.registries.model.JwtConfig");
        when(jwtConfig1.getSubject()).thenReturn("Hello from the Dreaming Spires");
        doNothing().when(jwtConfig1).setAudience(any());
        doNothing().when(jwtConfig1).setIssuer(any());
        doNothing().when(jwtConfig1).setKid(any());
        doNothing().when(jwtConfig1).setPurposeId(any());
        doNothing().when(jwtConfig1).setSubject(any());
        jwtConfig1.setAudience("Audience");
        jwtConfig1.setIssuer("Issuer");
        jwtConfig1.setKid("Kid");
        jwtConfig1.setPurposeId("42");
        jwtConfig1.setSubject("Hello from the Dreaming Spires");
        PdndSecretValue pdndSecretValue = mock(PdndSecretValue.class);
        when(pdndSecretValue.getKeyId()).thenThrow(new PnInternalException("","",new Throwable()));
        when(pdndSecretValue.getJwtConfig()).thenReturn(jwtConfig1);
        doNothing().when(pdndSecretValue).setClientId(any());
        doNothing().when(pdndSecretValue).setJwtConfig(any());
        doNothing().when(pdndSecretValue).setKeyId(any());
        pdndSecretValue.setClientId("42");
        pdndSecretValue.setJwtConfig(jwtConfig);
        pdndSecretValue.setKeyId("42");
        assertThrows(PnInternalException.class, () -> pdndAssertionGenerator.generateClientAssertion(pdndSecretValue));
        verify(pdndSecretValue, atLeast(1)).getJwtConfig();
        verify(pdndSecretValue).getKeyId();
        verify(pdndSecretValue).setClientId(any());
        verify(pdndSecretValue).setJwtConfig(any());
        verify(pdndSecretValue).setKeyId(any());
        verify(jwtConfig1).getAudience();
        verify(jwtConfig1).getIssuer();
        verify(jwtConfig1).getKid();
        verify(jwtConfig1).getPurposeId();
        verify(jwtConfig1).getSubject();
        verify(jwtConfig1).setAudience(any());
        verify(jwtConfig1).setIssuer(any());
        verify(jwtConfig1).setKid(any());
        verify(jwtConfig1).setPurposeId(any());
        verify(jwtConfig1).setSubject(any());
    }

    /**
     * Method under test: {@link PdndAssertionGenerator#generateClientAssertion(PdndSecretValue)}
     */
    @Test
    void testGenerateClientAssertion9() throws AwsServiceException, SdkClientException {

        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("Audience");
        jwtConfig.setIssuer("Issuer");
        jwtConfig.setKid("Kid");
        jwtConfig.setPurposeId("42");
        jwtConfig.setSubject("Hello from the Dreaming Spires");
        JwtConfig jwtConfig1 = mock(JwtConfig.class);
        when(jwtConfig1.getAudience()).thenReturn("Audience");
        when(jwtConfig1.getIssuer()).thenReturn("Issuer");
        when(jwtConfig1.getKid()).thenReturn("Kid");
        when(jwtConfig1.getPurposeId()).thenReturn("");
        when(jwtConfig1.getSubject()).thenReturn("Hello from the Dreaming Spires");
        doNothing().when(jwtConfig1).setAudience(any());
        doNothing().when(jwtConfig1).setIssuer(any());
        doNothing().when(jwtConfig1).setKid(any());
        doNothing().when(jwtConfig1).setPurposeId(any());
        doNothing().when(jwtConfig1).setSubject(any());
        jwtConfig1.setAudience("Audience");
        jwtConfig1.setIssuer("Issuer");
        jwtConfig1.setKid("Kid");
        jwtConfig1.setPurposeId("42");
        jwtConfig1.setSubject("Hello from the Dreaming Spires");
        PdndSecretValue pdndSecretValue = mock(PdndSecretValue.class);
        when(pdndSecretValue.getKeyId()).thenThrow(new PnInternalException("","",new Throwable()));
        when(pdndSecretValue.getJwtConfig()).thenReturn(jwtConfig1);
        doNothing().when(pdndSecretValue).setClientId(any());
        doNothing().when(pdndSecretValue).setJwtConfig(any());
        doNothing().when(pdndSecretValue).setKeyId(any());
        pdndSecretValue.setClientId("42");
        pdndSecretValue.setJwtConfig(jwtConfig);
        pdndSecretValue.setKeyId("42");
        assertThrows(PnInternalException.class, () -> pdndAssertionGenerator.generateClientAssertion(pdndSecretValue));
        verify(pdndSecretValue, atLeast(1)).getJwtConfig();
        verify(pdndSecretValue).getKeyId();
        verify(pdndSecretValue).setClientId(any());
        verify(pdndSecretValue).setJwtConfig(any());
        verify(pdndSecretValue).setKeyId(any());
        verify(jwtConfig1).getAudience();
        verify(jwtConfig1).getIssuer();
        verify(jwtConfig1).getKid();
        verify(jwtConfig1).getPurposeId();
        verify(jwtConfig1).getSubject();
        verify(jwtConfig1).setAudience(any());
        verify(jwtConfig1).setIssuer(any());
        verify(jwtConfig1).setKid(any());
        verify(jwtConfig1).setPurposeId(any());
        verify(jwtConfig1).setSubject(any());
    }

    /**
     * Method under test: {@link PdndAssertionGenerator#generateClientAssertion(PdndSecretValue)}
     */
    @Test
    void testGenerateClientAssertion10() throws AwsServiceException, SdkClientException {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setAudience("Audience");
        jwtConfig.setIssuer("Issuer");
        jwtConfig.setKid("Kid");
        jwtConfig.setPurposeId("42");
        jwtConfig.setSubject("Hello from the Dreaming Spires");
        JwtConfig jwtConfig1 = mock(JwtConfig.class);
        when(jwtConfig1.getAudience()).thenReturn("Audience");
        when(jwtConfig1.getIssuer()).thenReturn("Issuer");
        when(jwtConfig1.getKid()).thenReturn("Kid");
        when(jwtConfig1.getPurposeId()).thenReturn("42");
        when(jwtConfig1.getSubject()).thenReturn("42");
        doNothing().when(jwtConfig1).setAudience(any());
        doNothing().when(jwtConfig1).setIssuer(any());
        doNothing().when(jwtConfig1).setKid(any());
        doNothing().when(jwtConfig1).setPurposeId(any());
        doNothing().when(jwtConfig1).setSubject(any());
        jwtConfig1.setAudience("Audience");
        jwtConfig1.setIssuer("Issuer");
        jwtConfig1.setKid("Kid");
        jwtConfig1.setPurposeId("42");
        jwtConfig1.setSubject("Hello from the Dreaming Spires");
        PdndSecretValue pdndSecretValue = mock(PdndSecretValue.class);
        when(pdndSecretValue.getKeyId()).thenThrow(new PnInternalException("","",new Throwable()));
        when(pdndSecretValue.getJwtConfig()).thenReturn(jwtConfig1);
        doNothing().when(pdndSecretValue).setClientId(any());
        doNothing().when(pdndSecretValue).setJwtConfig(any());
        doNothing().when(pdndSecretValue).setKeyId(any());
        pdndSecretValue.setClientId("42");
        pdndSecretValue.setJwtConfig(jwtConfig);
        pdndSecretValue.setKeyId("42");
        assertThrows(PnInternalException.class, () -> pdndAssertionGenerator.generateClientAssertion(pdndSecretValue));
        verify(pdndSecretValue, atLeast(1)).getJwtConfig();
        verify(pdndSecretValue).getKeyId();
        verify(pdndSecretValue).setClientId(any());
        verify(pdndSecretValue).setJwtConfig(any());
        verify(pdndSecretValue).setKeyId(any());
        verify(jwtConfig1).getAudience();
        verify(jwtConfig1).getIssuer();
        verify(jwtConfig1).getKid();
        verify(jwtConfig1).getPurposeId();
        verify(jwtConfig1).getSubject();
        verify(jwtConfig1).setAudience(any());
        verify(jwtConfig1).setIssuer(any());
        verify(jwtConfig1).setKid(any());
        verify(jwtConfig1).setPurposeId(any());
        verify(jwtConfig1).setSubject(any());
    }
}

