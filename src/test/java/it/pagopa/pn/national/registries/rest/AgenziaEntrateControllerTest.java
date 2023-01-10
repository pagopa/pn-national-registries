package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.service.AgenziaEntrateService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgenziaEntrateControllerTest {

    @InjectMocks
    AgenziaEntrateController agenziaEntrateController;

    @Mock
    AgenziaEntrateService agenziaEntrateService;

    @Mock
    ServerWebExchange serverWebExchange;

    @Mock
    Scheduler scheduler;

    @Test
    void checkTaxId() {
        CheckTaxIdRequestBodyDto checkTaxIdRequestBodyDto = new CheckTaxIdRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto dto = new CheckTaxIdRequestBodyFilterDto();
        dto.setTaxId("DDDFFF852G25H501G");
        checkTaxIdRequestBodyDto.setFilter(dto);

        CheckTaxIdOKDto checkTaxIdOKDto = new CheckTaxIdOKDto();
        checkTaxIdOKDto.setTaxId("DDDFFF852G25H501G");
        checkTaxIdOKDto.setIsValid(true);
        when(agenziaEntrateService.callEService(any())).thenReturn(Mono.just(checkTaxIdOKDto));
        StepVerifier.create(agenziaEntrateController.checkTaxId(checkTaxIdRequestBodyDto,serverWebExchange))
                .expectNext(ResponseEntity.ok().body(checkTaxIdOKDto));
    }
}
