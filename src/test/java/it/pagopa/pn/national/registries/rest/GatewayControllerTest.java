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
        // Set Request
        RecipientAddressRequestBodyDto recipientAddressRequestBodyDto = new RecipientAddressRequestBodyDto();
        recipientAddressRequestBodyDto.setTaxId("PPPPLT80A01H501V");
        recipientAddressRequestBodyDto.setRecIndex(0);
        recipientAddressRequestBodyDto.setRecipientType(RecipientAddressRequestBodyDto.RecipientTypeEnum.PF);

        PhysicalAddressesRequestBodyDto physicalAddressesRequestBodyDto = new PhysicalAddressesRequestBodyDto();
        physicalAddressesRequestBodyDto.setCorrelationId("correlationId");
        physicalAddressesRequestBodyDto.setAddresses(List.of(recipientAddressRequestBodyDto));

        // Set Response

        PhysicalAddressesResponseDto physicalAddressesResponseDto = getPhysicalAddressesResponseDto();

        StepVerifier.create(gatewayController.getPhysicalAddresses(Mono.just(physicalAddressesRequestBodyDto), serverWebExchange))
                .expectNext(ResponseEntity.ok().body(physicalAddressesResponseDto));
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