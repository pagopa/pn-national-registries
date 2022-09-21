package it.pagopa.pn.national.registries.client.anpr;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Component
public class AnprWebClient extends CommonBaseClient {

    public AnprWebClient() {
        initWebClient();
    }

    protected WebClient initWebClient() {
        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                //  .secure(sslContextSpec -> sslContextSpec.sslContext(Objects.requireNonNull(getSSLContext())))
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(10000, TimeUnit.MILLISECONDS)));

        return super.enrichBuilder(WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))).build();
    }
}
