package it.pagopa.pn.national.registries.exceptions;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecErrorDto;
import it.pagopa.pn.national.registries.model.anpr.AnprResponseKO;
import it.pagopa.pn.national.registries.model.inad.InadResponseKO;
import it.pagopa.pn.national.registries.model.ipa.IpaResponseKO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHeadResponseDecorator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {PnWebExceptionHandler.class})
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

    @Test
    void testHandle4() {
        PnNationalRegistriesException exception = new PnNationalRegistriesException("ex.getMessage()", 400,
                "ex.getStatusText()", headers, null,
                Charset.defaultCharset(), IPAPecErrorDto.class);

        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.bufferFactory()).thenReturn(dataBufferFactory);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(dataBufferFactory.wrap((byte[]) any())).thenReturn(dataBuffer);
        Problem problem = new Problem();
        problem.setStatus(400);
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

    @Test
    void testHandle3() {
        PnNationalRegistriesException exception = mock(PnNationalRegistriesException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exception.getMessage()).thenReturn("bad request");
        when(exception.getResponseBodyAsString()).thenReturn("{}");
        Class<?> ipaResponseKOClass = IpaResponseKO.class;
        doReturn(ipaResponseKOClass).when(exception).getClassName();
        when(serverWebExchange.getResponse()).thenReturn(serverHttpResponse);
        when(serverHttpResponse.bufferFactory()).thenReturn(dataBufferFactory);
        when(serverWebExchange.getRequest()).thenReturn(serverHttpRequest);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(dataBufferFactory.wrap((byte[]) any())).thenReturn(dataBuffer);
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception)).expectComplete();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle9() {
        HttpHeaders headers = new HttpHeaders();
        byte[] responseBody = "AAAAAAAA".getBytes(StandardCharsets.UTF_8);
        when(exceptionHelper.handleException(org.mockito.Mockito.any()))
                .thenThrow(new PnNationalRegistriesException("An error occurred", 1, "Status Text", headers, responseBody,
                        null, Object.class));
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getResponse()).thenReturn(new MockServerHttpResponse());
        Throwable t = new Throwable();
        assertThrows(PnNationalRegistriesException.class, () -> pnWebExceptionHandler.handle(defaultServerWebExchange, t));
        verify(exceptionHelper).handleException(org.mockito.Mockito.any());
        verify(defaultServerWebExchange).getResponse();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle10() {
        Problem problem = new Problem();
        problem.status(1);
        when(exceptionHelper.handleException(org.mockito.Mockito.any())).thenReturn(problem);
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getRequest())
                .thenReturn(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(
                        new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(serverHttpRequestDecorator))))));
        when(defaultServerWebExchange.getResponse()).thenReturn(new MockServerHttpResponse());
        pnWebExceptionHandler.handle(defaultServerWebExchange, new Throwable());
        verify(exceptionHelper).handleException(org.mockito.Mockito.any());
        verify(defaultServerWebExchange).getRequest();
        verify(defaultServerWebExchange, atLeast(1)).getResponse();
        verify(serverHttpRequestDecorator).getURI();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle11() {
        ArrayList<ProblemError> problemErrorList = new ArrayList<>();
        problemErrorList.add(new ProblemError("Code", "Element", "Detail"));
        Problem problem = mock(Problem.class);
        when(problem.status(org.mockito.Mockito.any())).thenReturn(new Problem());
        when(problem.getStatus()).thenReturn(1);
        when(problem.getDetail()).thenReturn("Detail");
        when(problem.getTitle()).thenReturn("Dr");
        when(problem.getErrors()).thenReturn(problemErrorList);
        problem.status(1);
        when(exceptionHelper.handleException(org.mockito.Mockito.any())).thenReturn(problem);
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getRequest())
                .thenReturn(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(
                        new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(serverHttpRequestDecorator))))));
        when(defaultServerWebExchange.getResponse()).thenReturn(new MockServerHttpResponse());
        pnWebExceptionHandler.handle(defaultServerWebExchange, new Throwable());
        verify(exceptionHelper).handleException(org.mockito.Mockito.any());
        verify(problem).status(org.mockito.Mockito.any());
        verify(problem).getStatus();
        verify(problem).getDetail();
        verify(problem).getTitle();
        verify(problem).getErrors();
        verify(defaultServerWebExchange).getRequest();
        verify(defaultServerWebExchange, atLeast(1)).getResponse();
        verify(serverHttpRequestDecorator).getURI();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle13() {
        Problem problem = mock(Problem.class);
        when(problem.status(org.mockito.Mockito.any())).thenReturn(new Problem());
        when(problem.getStatus()).thenReturn(1);
        when(problem.getDetail()).thenReturn("Detail");
        when(problem.getTitle()).thenReturn("Dr");
        when(problem.getErrors()).thenReturn(new ArrayList<>());
        problem.status(1);
        when(exceptionHelper.handleException(org.mockito.Mockito.any())).thenReturn(problem);
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getRequest())
                .thenReturn(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(
                        new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(serverHttpRequestDecorator))))));
        when(defaultServerWebExchange.getResponse())
                .thenReturn(new HttpHeadResponseDecorator(new MockServerHttpResponse()));
        pnWebExceptionHandler.handle(defaultServerWebExchange, new Throwable());
        verify(exceptionHelper).handleException(org.mockito.Mockito.any());
        verify(problem).status(org.mockito.Mockito.any());
        verify(problem).getStatus();
        verify(problem).getDetail();
        verify(problem).getTitle();
        verify(problem).getErrors();
        verify(defaultServerWebExchange).getRequest();
        verify(defaultServerWebExchange, atLeast(1)).getResponse();
        verify(serverHttpRequestDecorator).getURI();
    }
}
