package it.pagopa.pn.national.registries.exceptions;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
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
import static org.mockito.Mockito.*;

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
    @Mock
    HttpHeaders headers;

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle() {
        Problem problem = new Problem();
        problem.setStatus(400);
        Throwable throwable = new Throwable();
        when(exceptionHelper.handleException(any())).thenReturn(problem);
        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(serverHttpResponse.bufferFactory()).thenReturn(dataBufferFactory);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(dataBufferFactory.wrap((byte[]) any())).thenReturn(dataBuffer);
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, throwable)).expectComplete();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle2() {
        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(exception.getMessage()).thenReturn("bad request");
        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.bufferFactory()).thenReturn(dataBufferFactory);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(dataBufferFactory.wrap((byte[]) any())).thenReturn(dataBuffer);
        Problem problem = new Problem();
        problem.setStatus(400);
        when(exceptionHelper.handleException(any())).thenReturn(problem);
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception)).expectComplete();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle5() {
        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
        when(exception.getMessage()).thenReturn("bad request");
        when(exceptionHelper.handleException(exception)).thenReturn(Problem.builder().status(400).build());
        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.bufferFactory()).thenReturn(dataBufferFactory);
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(dataBufferFactory.wrap((byte[]) any())).thenReturn(dataBuffer);
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception)).expectComplete();
    }

    @Test
    void testHandle6() {
        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(exception.getMessage()).thenReturn("bad request");
        when(exception.getResponseBodyAsString()).thenReturn("{}");
        Class<?> inadResponseKOClass = InadResponseKO.class;
        doReturn(inadResponseKOClass).when(exception).getClassName();
        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.bufferFactory()).thenReturn(dataBufferFactory);
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(dataBufferFactory.wrap((byte[]) any())).thenReturn(dataBuffer);
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception)).expectComplete();
    }

    @Test
    void testHandle7() {
        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exception.getMessage()).thenReturn("bad request");
        when(exception.getResponseBodyAsString()).thenReturn("{}");
        Class<?> inadResponseKOClass = AnprResponseKO.class;
        doReturn(inadResponseKOClass).when(exception).getClassName();
        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.bufferFactory()).thenReturn(dataBufferFactory);
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(dataBufferFactory.wrap((byte[]) any())).thenReturn(dataBuffer);
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception)).expectComplete();
    }
}
