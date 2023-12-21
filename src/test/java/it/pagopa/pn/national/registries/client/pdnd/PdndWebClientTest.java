package it.pagopa.pn.national.registries.client.pdnd;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfWebClient;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {CheckCfWebClient.class, CommonBaseClient.class, ResponseExchangeFilter.class})
@ExtendWith(MockitoExtension.class)
class PdndWebClientTest {

    @Mock
    ResponseExchangeFilter responseExchangeFilter;

    @Test
    void testInit(){
        PdndWebClient pdndWebClient = new PdndWebClient( "test.it", responseExchangeFilter);
        Assertions.assertDoesNotThrow(pdndWebClient::init);
    }

}
