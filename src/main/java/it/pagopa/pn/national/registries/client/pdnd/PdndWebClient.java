package it.pagopa.pn.national.registries.client.pdnd;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Component
public class PdndWebClient extends CommonBaseClient {

    private static final Integer TIMEOUT = 10000;

    public PdndWebClient() {
        initWebClient();
    }

    protected final WebClient initWebClient() {
        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(TIMEOUT, TimeUnit.MILLISECONDS)));

        return super.enrichBuilder(WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))).build();
    }
}
