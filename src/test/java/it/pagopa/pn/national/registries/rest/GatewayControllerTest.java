package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.service.GatewayService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


@ExtendWith(MockitoExtension.class)
class GatewayControllerTest {

    @InjectMocks
    GatewayController gatewayController;

    @Mock
    GatewayService gatewayService;
    @Mock
    ServerWebExchange serverWebExchange;

    @Mock
    ValidateTaxIdUtils validateTaxIdUtils;

    @Test
    void testGetAddress() {
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        AddressRequestBodyFilterDto addressRequestBodyFilterDto = new AddressRequestBodyFilterDto();
        addressRequestBodyFilterDto.setTaxId("PPPPLT80A01H501V");
        addressRequestBodyDto.setFilter(addressRequestBodyFilterDto);
        AddressOKDto addressOKDto = new AddressOKDto();
        StepVerifier.create(gatewayController.getAddresses("",Mono.just(addressRequestBodyDto), "clientId", serverWebExchange))
                .expectNext(ResponseEntity.ok().body(addressOKDto));
    }

}