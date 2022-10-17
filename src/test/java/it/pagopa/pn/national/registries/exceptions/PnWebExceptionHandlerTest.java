package it.pagopa.pn.national.registries.exceptions;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PnWebExceptionHandlerTest {
    @InjectMocks
    private PnWebExceptionHandler pnWebExceptionHandler;

    @Mock
    ServerWebExchange serverWebExchange;

    @Mock
    ServerHttpResponse serverHttpResponse;

    @Mock
    ServerHttpRequest serverHttpRequest;

    @Mock
    DataBufferFactory dataBufferFactory;

    @Mock
    DataBuffer dataBuffer;

    @Mock
    ExceptionHelper exceptionHelper;

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle(){
        Problem problem = new Problem();
        problem.setStatus(400);
        Throwable throwable = new Throwable();
        when(exceptionHelper.handleException(any())).thenReturn(problem);
        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(serverHttpResponse.bufferFactory()).thenReturn(dataBufferFactory);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(dataBufferFactory.wrap((byte[]) any())).thenReturn(dataBuffer);
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange,throwable)).expectComplete();

    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle2() {

        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(exception.getMessage()).thenReturn("bad request");
        when(exception.getResponseBodyAsString()).thenReturn("{\n" +
                "\"title\":\"bas request\",\n" +
                "\"status\":\"400\",\n" +
                "\"detail\":\"invalid request\"\n" +
                "}");
        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.bufferFactory()).thenReturn(dataBufferFactory);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(dataBufferFactory.wrap((byte[]) any())).thenReturn(dataBuffer);
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange,exception)).expectComplete();

    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle3() {
        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(exception.getMessage()).thenReturn("bad request");
        when(exception.getResponseBodyAsString()).thenReturn("");
        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.bufferFactory()).thenReturn(dataBufferFactory);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(dataBufferFactory.wrap((byte[]) any())).thenReturn(dataBuffer);
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange,exception)).expectComplete();

    }


    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle4(){

        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(exception.getMessage()).thenReturn("bad request");
        when(exception.getResponseBodyAsString()).thenReturn("");
        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.bufferFactory()).thenReturn(dataBufferFactory);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(dataBufferFactory.wrap((byte[]) any())).thenReturn(dataBuffer);
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange,exception)).expectComplete();

    }
}

