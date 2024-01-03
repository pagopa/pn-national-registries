package it.pagopa.pn.national.registries.client.ipa;

import it.pagopa.pn.national.registries.config.ipa.IpaWebClientConfig;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpaWebClientTest {

    @Mock
    ResponseExchangeFilter responseExchangeFilter;

    @Test
    void testInit() {
        IpaWebClientConfig webClientConfig = new IpaWebClientConfig();
        webClientConfig.setTcpMaxPoolSize(1);
        webClientConfig.setTcpMaxQueuedConnections(1);
        webClientConfig.setTcpPoolIdleTimeout(1);
        webClientConfig.setTcpPendingAcquiredTimeout(1);
        IpaWebClient ipaWebClient = new IpaWebClient( "test.it", responseExchangeFilter);
        Assertions.assertDoesNotThrow(ipaWebClient::init);
    }

}
