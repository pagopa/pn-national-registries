package it.pagopa.pn.national.registries.client.inad;

import it.pagopa.pn.national.registries.client.CommonWebClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfWebClient;
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
    void testInit(){
        InadWebClient inadWebClient = new InadWebClient(100,100,
                100,100,"test.it");
        Assertions.assertThrows(NullPointerException.class, inadWebClient::init);
    }


}
