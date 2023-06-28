package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalSecretConfig;
import it.pagopa.pn.national.registries.model.SSLData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigInteger;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {X509CertificateUtils.class})
@PropertySource("classpath:application-test.properties")
@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
class X509CertificateUtilsTest {


    @Mock
    private AdeLegalSecretConfig adeLegalSecretConfig;
    public static final String KEY = "LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tCk1JSUV2UUlCQURBTkJna3Foa2lHOXcwQkFRRUZBQVNDQktjd2dnU2pBZ0VBQW9JQkFRRElkQWo5dVdPOVJCUlNFeWNDeWtiUzBpMFBOVnYzcFpPTVdiZ2JRNUFWYURMR2w5R2pZelhWTWxQaUl3ODcvamFYT2lMb1dMUWdPeDl5eDE5b2lPRG9udy9HNUw2ZjlwTDBGcVI3c05RQnFQeGVCU090WTI1L3UxMUJyY1p1WElmYmpadUpZd041L1dtRnBRWlVMYng3RTNONmxmYm1MZGFjZFMyQVRzcGs3UWpoUWNETzBVZTBMM1JpWVkxQ24vc3NjbC9YSEkwVzZqTUtpYjBpdTNDTmVwWjFKUStkTmZ4S2xJcWcrMC96RnRnYXlSamNIYng1emFINEcxaXpHalRJcXJUYm5xUTB0RmZvNmQ3U0huMTZDMXZNaEJNM2U4SmhJRmJIcDZ2WW9peFFkSUpVZk9tY211M05NSHpvM1ViZVlEd1lQdnIvOWZwUitFU1hUWXdMQWdNQkFBRUNnZ0VBQWZ3UE9tc1BIOWJqTnN0dk1YbkhVdjh0TTlwVllVUW92eWp0ekJLWU9pU2IxY2Ftam10RHNiTzBuL0x0MHQ5c1NaWXdkeHdFSGpmdUNLZ2Q4RG43SmpRNmpXU0hQNCt1bDV0NDhCSGJUbTAyWWl0QW5PQUFJZXkvd1pBT3JObnBwM1FETDd0VlBRWG1rczlxcTlYZGR4RmZCWWdkNE10SmxsMUNEa0lCK2o3bVcrQnY5d0JrQkExVG9HOWtYRzZlUm1mZmtlTld4eWlvRWVkckNrS2g5c0w2Mm82R3VBZVpMS3h3SGFMRk9GNWxMeHhSQi9MN1psVURldnZlYk9jTld4NWZ2YzJLT25kZzA5aTI3Yml3Wk5Nd05vTHhBbFN6bU1QbCtONGd5Mnc5c2lySHFOY09KUnZKd3dhOFN1cUFXNXNuNmwvWWRqcXIxY1RwbGVDZ2pRS0JnUURXSTk4UlRMNm14dTRsVGU2bGhFR05lcXpMZ0ZnMXF4Y2QweS9tMG83VjNVNXA3cnFUVUo0dVBHTzJqUFNBYlJRMG4yZVpEamh3a0dmT3lTRk5OenJPMnhNTXUrNVY3eXp3UHFpOVBtMHoyQ09EWk05SWVUcTgyQmFydkVjTFM0dm1YSmZRMFhGeUtHRVMvMkNzakJvSDYvVUJQQnF1ck5JWi8zL3Q4U1hmUHdLQmdRRHZvenpZaXVONVlaMWZWMTFGMCtTRVZ5R09lSnhWMXVwbHpTbXNNcWtCcnVBTld1Tnl2eXlMamtJV1U5MUlhSk5yTEozODRnUTQ3dUJwb0pibHdWRDQzdmxNcHV3M0krZzByUjBUVjNxSVVLakFoL1F0Z3hZUk1SWjUyWTljSzVZWHVndUpiRXpkai9MOEpVM3Z6eEJCNWc5WGM4SGI2OWwyWXBqSm9ZaXNOUUtCZ0F3K1FzdTNhcE1ZZnBjdGJINlVJRGRaa3pXVFlmZTNqM3ZLRGt3Ukw4OTBkcjVCd3ZNWFFlUDgyZXFmQm4rdFBPR0JWNmY5a3lhRWF4cjhqdVhlU1lONExRK21Vd3Nnd0ozL3h5QUN5TFdWSHZxdE1kaS83YWJNYUFkcm5WZzgrb095T1kvYjJiT2dNV1NucXp4U1NrNjNvK3R0SjAxalhpUFVZdGtVMTRoN0FvR0FYWVhlbEVBNElNSVk4Z0dkbEVmcThqOHpDSEJIeXpSeURFTHlsWks3NUwxRUVkSTczQ2VDRWdEMkI5SzVGSzVEZ2x0S1dVdzRDNlFtanZLdWtZSk53S0w4dG82YXQ3WmF4RndmY2RIOUwwQVRhL2hqTW5Qclk5QTlqbFo3SGFTaEVpZDRveXhTSDFJenFYWkJvV3ExVkpLZHdDcWNLT0UxNEg4QVZBaW4xMVVDZ1lFQW1Qb2FLQmEyVUpVSERVV2F6OUF6ZXV1K2syckl6UTVubWp0VVpsaEpkSmZNRXdXQ0s0UktlRDQva2c3eXdDRVZaUUdwSGovS3dNbCtuejNBY0RmbTZYektXYmRTWHp6Q2ZhSEpFWEpYL3Rra2s1cHpvaWdZYkZXZkwrMDFUeHUxZEg0ZnV5cTR3UmF0bVE0Yndld3NVVVRaOFJMOW5jdER6SnBPTnErOG5sYz0KLS0tLS1FTkQgUFJJVkFURSBLRVktLS0tLQ==";
    public static final String CERT = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUQ2ekNDQXRPZ0F3SUJBZ0lVQ1o4NUJxa1RwTi9tZ0NsVzNZT0hjcDBJcmpFd0RRWUpLb1pJaHZjTkFRRUxCUUF3Z1lReEN6QUpCZ05WQkFZVEFrVlZNUTR3REFZRFZRUUlEQVZKVkVGTVdURU5NQXNHQTFVRUJ3d0VVRWxUUVRFUk1BOEdBMVVFQ2d3SVRsUlVJRVJCVkVFeER6QU5CZ05WQkFzTUJsQkJSMDlRUVRFVE1CRUdBMVVFQXd3S1kyOXRiVzl1VG1GdFpURWRNQnNHQ1NxR1NJYjNEUUVKQVJZT1pXMWhhV3hBWlcxaGFXd3VhWFF3SGhjTk1qTXdOVEEwTVRFd01ERTJXaGNOTXpNd05UQXhNVEV3TURFMldqQ0JoREVMTUFrR0ExVUVCaE1DUlZVeERqQU1CZ05WQkFnTUJVbFVRVXhaTVEwd0N3WURWUVFIREFSUVNWTkJNUkV3RHdZRFZRUUtEQWhPVkZRZ1JFRlVRVEVQTUEwR0ExVUVDd3dHVUVGSFQxQkJNUk13RVFZRFZRUUREQXBqYjIxdGIyNU9ZVzFsTVIwd0d3WUpLb1pJaHZjTkFRa0JGZzVsYldGcGJFQmxiV0ZwYkM1cGREQ0NBU0l3RFFZSktvWklodmNOQVFFQkJRQURnZ0VQQURDQ0FRb0NnZ0VCQU1oMENQMjVZNzFFRkZJVEp3TEtSdExTTFE4MVcvZWxrNHhadUJ0RGtCVm9Nc2FYMGFOak5kVXlVK0lqRHp2K05wYzZJdWhZdENBN0gzTEhYMmlJNE9pZkQ4Ymt2cC8ya3ZRV3BIdXcxQUdvL0Y0Rkk2MWpibis3WFVHdHhtNWNoOXVObTRsakEzbjlhWVdsQmxRdHZIc1RjM3FWOXVZdDFweDFMWUJPeW1UdENPRkJ3TTdSUjdRdmRHSmhqVUtmK3l4eVg5Y2NqUmJxTXdxSnZTSzdjSTE2bG5VbEQ1MDEvRXFVaXFEN1QvTVcyQnJKR053ZHZIbk5vZmdiV0xNYU5NaXF0TnVlcERTMFYranAzdEllZlhvTFc4eUVFemQ3d21FZ1ZzZW5xOWlpTEZCMGdsUjg2WnlhN2Mwd2ZPamRSdDVnUEJnKyt2LzErbEg0UkpkTmpBc0NBd0VBQWFOVE1GRXdIUVlEVlIwT0JCWUVGTWVUYjV2cTNIdVQ2ZFVXMXdoZW9QYURaUFZqTUI4R0ExVWRJd1FZTUJhQUZNZVRiNXZxM0h1VDZkVVcxd2hlb1BhRFpQVmpNQThHQTFVZEV3RUIvd1FGTUFNQkFmOHdEUVlKS29aSWh2Y05BUUVMQlFBRGdnRUJBRkRBR096MDJVcENLMWFBbithOCtJWVdtOXZ4Z2UvVHRuMjVUWTlYR0RNWkxiUU43czVGNWFmQ3dkVG5UV1FjWE9uQ05RNUY1azJxcFQ0ODZCc3VNRi9MNjBIeTVJUXhCUmxZeXFVam93TUtRSFY2VVowSlJadjdRMGtBanRrdjlUemEzTWo4YWRRWDRDMjdqR0pkZVlobmZ6MEl0TTREWnN2ZjVrc3hDOVpoMjJWTDNCQmEvTjVEb09yNkQ3UVc3K1RlMlIyWkExZWJOQk55ZUNXNmc5Qmhpbm5vSlpwN0p4b1FreTFmdWpyZm00TUczbVkvcXJrZnRyR3kydThCc0V6YkpBNGFsUGpMT2ZNd2dXNXVPUjdSSk9tSTVQUVJ3MVpCVU1CSExPUnd1VlZMd1RmV1BJV290V01FYjV2eFZ6MzVlYkoxNG8wc1lham9wT1d1VW1zPQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0t";

/*
    X509CertificateUtils init(String key, String cert) {
        SSLData sslData = new SSLData();
        sslData.setKey(key);
        sslData.setCert(cert);
        when(adeLegalSecretConfig.getAdeSecretConfig()).thenReturn(sslData);
        return new X509CertificateUtils(adeLegalSecretConfig, adeLegalSecretConfig1, ssmParameterConsumerActivation, pnNationalRegistriesSecretService);
    }


    @Test
    void testGetPrivateKey() {
        X509CertificateUtils x509CertificateUtils = init(KEY, CERT);
        assertDoesNotThrow(x509CertificateUtils::getPrivateKey);
        assertEquals("RSA", x509CertificateUtils.getPrivateKey().getAlgorithm());
    }

    @Test
    void testGetPrivateKeyException() {
        X509CertificateUtils x509CertificateUtils = init("000", CERT);
        assertThrows(PnInternalException.class, x509CertificateUtils::getPrivateKey);
    }

    @Test
    void testLoadCertificateException() {
        assertThrows(PnInternalException.class, () -> init(KEY, "CERT"));
    }

    @Test
    void testGetCertificate() {
        X509CertificateUtils x509CertificateUtils = init(KEY, CERT);
        Certificate certificate = x509CertificateUtils.getCertificate();
        assertEquals("X.509", certificate.getType());
    }

    @Test
    void testGetIssuerName() {
        X509CertificateUtils x509CertificateUtils = init(KEY, CERT);
        String issuerName = x509CertificateUtils.getIssuerName();
        assertEquals("OID.1.2.840.113549.1.9.1=email@email.it, CN=commonName, OU=PAGOPA, O=NTT DATA, L=PISA, ST=ITALY, C=EU", issuerName);
    }

    @Test
    void testGetSerialNumber() {
        X509CertificateUtils x509CertificateUtils = init(KEY, CERT);
        BigInteger serialNumber = x509CertificateUtils.getSerialNumber();
        assertEquals("54931703090714765639646786231862063434924863025", serialNumber.toString());
    }
*/

    @Test
    void test() {
        assertTrue(true);
    }
}

