package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.service.IniPecService;
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
class IniPecControllerTest {

    @InjectMocks
    IniPecController iniPecController;

    @Mock
    IniPecService iniPecService;

    @Mock
    Scheduler scheduler;

    @Mock
    ServerWebExchange serverWebExchange;

    @Test
    void getDigitalAddressINAD() {
        GetDigitalAddressIniPECRequestBodyDto requestBodyDto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto dto = new GetDigitalAddressIniPECRequestBodyFilterDto();
        dto.setTaxId("DDDFGF52F52H501S");
        requestBodyDto.setFilter(dto);

        GetDigitalAddressIniPECOKDto getDigitalAddressINADOKDto = new GetDigitalAddressIniPECOKDto();
        getDigitalAddressINADOKDto.setCorrelationId("correlationId");

        when(iniPecService.getDigitalAddress(requestBodyDto)).thenReturn(Mono.just(getDigitalAddressINADOKDto));

        StepVerifier.create(iniPecController.digitalAddressIniPEC(requestBodyDto,serverWebExchange))
                .expectNext(ResponseEntity.ok().body(getDigitalAddressINADOKDto));
    }
}
