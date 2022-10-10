package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.service.AnprService;
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

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnprControllerTest {

    @InjectMocks
    AnprController anprController;

    @Mock
    AnprService anprService;

    @Mock
    Scheduler scheduler;

    @Mock
    ServerWebExchange serverWebExchange;

    @Test
    void testGetAddressANPR() {
        GetAddressANPRRequestBodyDto getAddressANPRRequestBodyDto = new GetAddressANPRRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto dto = new CheckTaxIdRequestBodyFilterDto();
        dto.setTaxId("DDDFFF92G52H501H");
        getAddressANPRRequestBodyDto.setFilter(dto);

        GetAddressANPROKDto getAddressANPROKDto = new GetAddressANPROKDto();
        getAddressANPROKDto.setResidentialAddresses(new ArrayList<>());

        when(anprService.getAddressANPR(any())).thenReturn(Mono.just(getAddressANPROKDto));

       StepVerifier.create(anprController.addressANPR(getAddressANPRRequestBodyDto, serverWebExchange))
                .expectNext(ResponseEntity.ok().body(getAddressANPROKDto));
    }
}
