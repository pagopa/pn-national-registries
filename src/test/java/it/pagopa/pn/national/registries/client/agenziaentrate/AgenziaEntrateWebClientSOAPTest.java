package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.config.adelegal.AdeLegalWebClientConfig;
import it.pagopa.pn.national.registries.log.ResponseExchangeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {AgenziaEntrateWebClientSOAP.class, CommonWebClient.class, ResponseExchangeFilter.class})
@ExtendWith(MockitoExtension.class)
class AgenziaEntrateWebClientSOAPTest {

    @Test
    void testInit() {
        AdeLegalWebClientConfig adeLegalWebClientConfig = new AdeLegalWebClientConfig();
        AgenziaEntrateWebClientSOAP agenziaEntrateWebClientSOAP = new AgenziaEntrateWebClientSOAP(true, "test.it", adeLegalWebClientConfig);
        Assertions.assertThrows(NullPointerException.class, agenziaEntrateWebClientSOAP::init);
    }

}
