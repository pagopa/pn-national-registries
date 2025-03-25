package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
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

import java.util.Date;
import java.util.List;


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

    @Test
    void testGetPhysicalAddresses() {
        PhysicalAddressesRequestBodyDto physicalAddressesRequestBodyDto = new PhysicalAddressesRequestBodyDto();
        RecipientAddressRequestBodyFilterDto filterDto = new RecipientAddressRequestBodyFilterDto();
        AddressOKDto addressOKDto = new AddressOKDto();
        Date date = new Date();

        physicalAddressesRequestBodyDto.setCorrelationId("correlationId");
        physicalAddressesRequestBodyDto.setReferenceRequestDate(date);
        RecipientAddressRequestBodyDto recipientAddressRequestBodyDto = new RecipientAddressRequestBodyDto();

        filterDto.setTaxId("PPPPLT80A01H501V");
        filterDto.recIndex("recIndex");
        filterDto.recipientType(RecipientAddressRequestBodyFilterDto.RecipientTypeEnum.PF);

        recipientAddressRequestBodyDto.setFilter(filterDto);
        physicalAddressesRequestBodyDto.setAddresses(List.of(recipientAddressRequestBodyDto));

        StepVerifier.create(gatewayController.getPhysicalAddresses(Mono.just(physicalAddressesRequestBodyDto), "clientId", serverWebExchange))
                .expectNext(ResponseEntity.ok().body(addressOKDto));
    }

}