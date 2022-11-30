package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseRequestBodyFilterDto;
import it.pagopa.pn.national.registries.service.RegistroImpreseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroImpreseControllerTest {

    @InjectMocks
    RegistroImpreseController registroImpreseController;

    @Mock
    RegistroImpreseService registroImpreseService;

    @Mock
    Scheduler scheduler;

    @Mock
    ServerWebExchange serverWebExchange;

    @Test
    void addressRegistroImprese() {

        GetAddressRegistroImpreseOKDto response = new GetAddressRegistroImpreseOKDto();
        response.setTaxId("cf");

        GetAddressRegistroImpreseRequestBodyDto body = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto dto = new GetAddressRegistroImpreseRequestBodyFilterDto();
        dto.setTaxId("cf");
        body.setFilter(dto);
        when(registroImpreseService.getAddress(body)).thenReturn(Mono.just(response));

        StepVerifier.create(registroImpreseController.addressRegistroImprese(body,serverWebExchange))
                .expectNext(ResponseEntity.ok().body(response));
    }
}
