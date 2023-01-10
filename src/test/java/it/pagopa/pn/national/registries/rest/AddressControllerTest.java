package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.national.registries.service.AddressService;
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
class AddressControllerTest {

    @InjectMocks
    AddressController addressController;

    @Mock
    AddressService addressService;
    @Mock
    Scheduler scheduler;
    @Mock
    ServerWebExchange serverWebExchange;

    @Test
    void testGetAddress() {
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        AddressOKDto addressOKDto = new AddressOKDto();
        when(addressService.retrieveDigitalOrPhysicalAddress("", addressRequestBodyDto))
                .thenReturn(Mono.just(addressOKDto));
        StepVerifier.create(addressController.getAddresses("", addressRequestBodyDto, serverWebExchange))
                .expectNext(ResponseEntity.ok().body(addressOKDto));
    }

}