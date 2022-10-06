package it.pagopa.pn.national.registries.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

@ExtendWith(MockitoExtension.class)
class ResponseExchangeFilterTest {

    @InjectMocks
    ResponseExchangeFilter responseExchangeFilter;

    @Test
    void filter() {
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK).body("response").build();
        ExchangeFunction exchangeFunction = clientRequest -> Mono.just(clientResponse);
        ClientRequest clientRequest = ClientRequest.create(HttpMethod.POST, URI.create("test")).build();
        StepVerifier.create(responseExchangeFilter.filter(clientRequest,exchangeFunction))
                .expectNextMatches(response -> clientResponse.statusCode().is2xxSuccessful()).verifyComplete();
    }
}
