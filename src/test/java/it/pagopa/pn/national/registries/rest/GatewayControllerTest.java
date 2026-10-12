package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.service.GatewayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayControllerTest {

    @Mock
    GatewayService gatewayService;

    @Mock
    ServerWebExchange serverWebExchange;

    @Test
    void getAddresses_shouldDelegateAsyncRequestAndReturnAcceptedCorrelationId() {
        GatewayController gatewayController = new GatewayController(gatewayService, Schedulers.immediate());

        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        AddressRequestBodyFilterDto addressRequestBodyFilterDto = new AddressRequestBodyFilterDto();
        addressRequestBodyFilterDto.setTaxId("PPPPLT80A01H501V");
        addressRequestBodyFilterDto.setCorrelationId("correlationId");
        addressRequestBodyFilterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL);
        addressRequestBodyDto.setFilter(addressRequestBodyFilterDto);

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");

        when(gatewayService.retrieveDigitalOrPhysicalAddressAsync("PF", "clientId", addressRequestBodyDto))
                .thenReturn(Mono.just(addressOKDto));

        StepVerifier.create(gatewayController.getAddresses("PF", Mono.just(addressRequestBodyDto), "clientId", serverWebExchange))
                .expectNext(ResponseEntity.ok(addressOKDto))
                .verifyComplete();

        verify(gatewayService).retrieveDigitalOrPhysicalAddressAsync("PF", "clientId", addressRequestBodyDto);
    }

    @Test
    void getPhysicalAddresses_shouldDelegateToGatewayService() {
        GatewayController gatewayController = new GatewayController(gatewayService, Schedulers.immediate());

        RecipientAddressRequestBodyDto recipientAddressRequestBodyDto = new RecipientAddressRequestBodyDto();
        recipientAddressRequestBodyDto.setTaxId("PPPPLT80A01H501V");
        recipientAddressRequestBodyDto.setRecIndex(0);
        recipientAddressRequestBodyDto.setRecipientType(RecipientAddressRequestBodyDto.RecipientTypeEnum.PF);

        PhysicalAddressesRequestBodyDto physicalAddressesRequestBodyDto = new PhysicalAddressesRequestBodyDto();
        physicalAddressesRequestBodyDto.setCorrelationId("correlationId");
        physicalAddressesRequestBodyDto.setAddresses(List.of(recipientAddressRequestBodyDto));

        PhysicalAddressesResponseDto physicalAddressesResponseDto = getPhysicalAddressesResponseDto();

        when(gatewayService.retrieveSyncPhysicalAddresses(physicalAddressesRequestBodyDto))
                .thenReturn(Mono.just(physicalAddressesResponseDto));

        StepVerifier.create(gatewayController.getPhysicalAddresses(Mono.just(physicalAddressesRequestBodyDto), serverWebExchange))
                .expectNext(ResponseEntity.ok(physicalAddressesResponseDto))
                .verifyComplete();

        verify(gatewayService).retrieveSyncPhysicalAddresses(physicalAddressesRequestBodyDto);
    }

    private static PhysicalAddressesResponseDto getPhysicalAddressesResponseDto() {
        PhysicalAddressesResponseDto physicalAddressesResponseDto = new PhysicalAddressesResponseDto();
        PhysicalAddressResponseDto physicalAddressResponseDto = new PhysicalAddressResponseDto();
        PhysicalAddressDto physicalAddressDto = new PhysicalAddressDto();
        physicalAddressDto.setAddress("address");
        physicalAddressResponseDto.setRecIndex(0);
        physicalAddressResponseDto.setRegistry("ANPR");
        physicalAddressResponseDto.setPhysicalAddress(physicalAddressDto);

        physicalAddressesResponseDto.setCorrelationId("correlationId");
        physicalAddressesResponseDto.setAddresses(List.of(physicalAddressResponseDto));
        return physicalAddressesResponseDto;
    }

}