package it.pagopa.pn.national.registries.client.ipa;

import it.pagopa.pn.national.registries.config.ipa.IpaWebClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpaWebClientTest {

    @Test
    void testInit() {
        IpaWebClientConfig webClientConfig = new IpaWebClientConfig();
        webClientConfig.setTcpMaxPoolSize(1);
        webClientConfig.setTcpMaxQueuedConnections(1);
        webClientConfig.setTcpPoolIdleTimeout(1);
        webClientConfig.setTcpPendingAcquiredTimeout(1);
        IpaWebClient ipaWebClient = new IpaWebClient(true, "test.it", webClientConfig);
        Assertions.assertThrows(NullPointerException.class, ipaWebClient::init);
    }

}
