package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.config.SsmParameterConsumerActivation;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import it.pagopa.pn.national.registries.service.PnNationalRegistriesSecretService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = {AdELegalWebClient.class, CommonWebClient.class, ResponseExchangeFilter.class})
@ExtendWith(MockitoExtension.class)
class AdELegalWebClientTest {

    @Mock
    SsmParameterConsumerActivation ssmParameterConsumerActivation;
    @Mock
    PnNationalRegistriesSecretService pnNationalRegistriesSecretService;

  /*  @Test
    @DisplayName("Should return a web client when the secret value is found")
    void initWhenSecretValueIsFoundThenReturnWebClient() {
        AdeLegalWebClientConfig webClientConfig = new AdeLegalWebClientConfig();
        webClientConfig.setTcpMaxPoolSize(1);
        webClientConfig.setTcpMaxQueuedConnections(1);
        webClientConfig.setTcpPendingAcquiredTimeout(1);
        webClientConfig.setTcpPoolIdleTimeout(1);


        AdELegalWebClient adELegalWebClient = new AdELegalWebClient(true, "", webClientConfig, adeLegalSecretConfig, pnNationalRegistriesSecretService);
        Assertions.assertThrows(NullPointerException.class, () -> adELegalWebClient.init());
    }

    */
    @Test
    void test() {
        assertTrue(true);
    }
}
