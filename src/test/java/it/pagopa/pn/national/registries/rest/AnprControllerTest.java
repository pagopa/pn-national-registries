package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyFilterDto;
import it.pagopa.pn.national.registries.repository.NationalRegistriesRequestsRepository;
import it.pagopa.pn.national.registries.service.*;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.test.StepVerifier;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnprControllerTest {


    AnprController anprController;

    @Mock
    AnprService anprService;

    @Mock
    ServerWebExchange serverWebExchange;

    @Mock
    ValidateTaxIdUtils validateTaxIdUtils;


    @BeforeEach
    void init() {
        GatewayService gatewayService = new GatewayService(mock(SqsService.class), mock(NationalRegistriesConfig.class),
                mock(PhysicalAddressService.class), mock(DigitalAddressService.class), mock(GatewayUtils.class), mock(NationalRegistriesRequestsRepository.class));
        anprController = new AnprController(anprService, gatewayService, mock(Scheduler.class));
    }

    @Test
    void testGetAddressANPR() {
        GetAddressANPRRequestBodyDto getAddressANPRRequestBodyDto = new GetAddressANPRRequestBodyDto();
        GetAddressANPRRequestBodyFilterDto dto = new GetAddressANPRRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        getAddressANPRRequestBodyDto.setFilter(dto);

        GetAddressANPROKDto getAddressANPROKDto = new GetAddressANPROKDto();
        getAddressANPROKDto.setResidentialAddresses(new ArrayList<>());

       StepVerifier.create(anprController.addressANPR(Mono.just(getAddressANPRRequestBodyDto), serverWebExchange))
                .expectNext(ResponseEntity.ok().body(getAddressANPROKDto));
    }
}
