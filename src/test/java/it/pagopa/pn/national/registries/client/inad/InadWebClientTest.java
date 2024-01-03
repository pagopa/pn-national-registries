package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfWebClient;
import it.pagopa.pn.national.registries.config.inad.InadWebClientConfig;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {CheckCfWebClient.class, CommonBaseClient.class, ResponseExchangeFilter.class})
@ExtendWith(MockitoExtension.class)
class InadWebClientTest {

    @Mock
    ResponseExchangeFilter responseExchangeFilter;

    @Test
    void testInit() {
        InadWebClient inadWebClient = new InadWebClient("test.it", responseExchangeFilter);
        Assertions.assertDoesNotThrow(inadWebClient::init);
    }

}
