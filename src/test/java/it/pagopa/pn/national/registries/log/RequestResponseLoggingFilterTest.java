package it.pagopa.pn.national.registries.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;
import org.springframework.http.server.reactive.ChannelSendOperator;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.WebSessionManager;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = "pn.national.registries.health-check-path=/health-check")
class RequestResponseLoggingFilterTest {

    @InjectMocks
    private RequestResponseLoggingFilter requestResponseLoggingFilter;

    @BeforeEach
    void setUp() {
        requestResponseLoggingFilter = new RequestResponseLoggingFilter("health-check-path");
    }
    /**
     * Method under test: {@link RequestResponseLoggingFilter#filter(ServerWebExchange, WebFilterChain)}
     */
    @Test
    void testFilter() {
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        when(serverHttpRequestDecorator.getHeaders()).thenReturn(new HttpHeaders());
        when(serverHttpRequestDecorator.getId()).thenReturn("https://example.org/example");
        WebSessionManager webSessionManager = mock(WebSessionManager.class);
        when(webSessionManager.getSession(any())).thenReturn((Mono<WebSession>) mock(Mono.class));
        MockServerHttpResponse response = new MockServerHttpResponse();
        DefaultServerCodecConfigurer codecConfigurer = new DefaultServerCodecConfigurer();
        DefaultServerWebExchange exchange = new DefaultServerWebExchange(serverHttpRequestDecorator, response,
                webSessionManager, codecConfigurer, new AcceptHeaderLocaleContextResolver());

        WebFilterChain webFilterChain = mock(WebFilterChain.class);
        ChannelSendOperator<Object> channelSendOperator = new ChannelSendOperator<>(
                (Publisher<Object>) mock(Publisher.class), (Function<Publisher<Object>, Publisher<Void>>) mock(Function.class));

        when(webFilterChain.filter(any())).thenReturn(channelSendOperator);
        assertSame(channelSendOperator, requestResponseLoggingFilter.filter(exchange, webFilterChain));
    }

    /**
     * Method under test: {@link RequestResponseLoggingFilter#filter(ServerWebExchange, WebFilterChain)}
     */
    @Test
    void testFilter2() {
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        when(serverHttpRequestDecorator.getHeaders()).thenReturn(new HttpHeaders());
        when(serverHttpRequestDecorator.getId()).thenReturn("https://example.org/example");
        WebSessionManager webSessionManager = mock(WebSessionManager.class);
        when(webSessionManager.getSession(any())).thenReturn((Mono<WebSession>) mock(Mono.class));
        MockServerHttpResponse response = new MockServerHttpResponse();
        DefaultServerCodecConfigurer codecConfigurer = new DefaultServerCodecConfigurer();
        DefaultServerWebExchange exchange = new DefaultServerWebExchange(serverHttpRequestDecorator, response,
                webSessionManager, codecConfigurer, new AcceptHeaderLocaleContextResolver());

        WebFilterChain webFilterChain = mock(WebFilterChain.class);
        when(webFilterChain.filter(any()))
                .thenThrow(new IllegalStateException("@NotNull method %s.%s must not return null"));
        assertThrows(IllegalStateException.class, () -> requestResponseLoggingFilter.filter(exchange, webFilterChain));
    }

    /**
     * Method under test: {@link RequestResponseLoggingFilter#filter(ServerWebExchange, WebFilterChain)}
     */
    @Test
    void testFilter3() {
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        ServerHttpRequestDecorator serverHttpRequestDecorator1 = new ServerHttpRequestDecorator(
                new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(
                        new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(serverHttpRequestDecorator)))));
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.mutate()).thenThrow(new IllegalStateException("foo"));
        when(defaultServerWebExchange.getResponse()).thenReturn(new MockServerHttpResponse());
        when(defaultServerWebExchange.getRequest()).thenReturn(serverHttpRequestDecorator1);
        WebFilterChain webFilterChain = mock(WebFilterChain.class);
        assertThrows(IllegalStateException.class,
                () -> requestResponseLoggingFilter.filter(defaultServerWebExchange, webFilterChain));
    }
}

