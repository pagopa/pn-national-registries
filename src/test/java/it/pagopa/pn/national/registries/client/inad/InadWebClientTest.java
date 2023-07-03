package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfWebClient;
import it.pagopa.pn.national.registries.config.inad.InadWebClientConfig;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {CheckCfWebClient.class, CommonWebClient.class, ResponseExchangeFilter.class})
@ExtendWith(MockitoExtension.class)
class InadWebClientTest {

    @Test
    void testInit() {
        InadWebClientConfig inadWebClientConfig = new InadWebClientConfig();
        InadWebClient inadWebClient = new InadWebClient(true, "test.it", inadWebClientConfig);
        Assertions.assertThrows(NullPointerException.class, inadWebClient::init);
    }

}
