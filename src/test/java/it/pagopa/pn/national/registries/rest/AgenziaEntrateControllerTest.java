package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.agenziaentrate.ResultCodeEnum;
import it.pagopa.pn.national.registries.model.agenziaentrate.ResultDetailEnum;
import it.pagopa.pn.national.registries.service.AgenziaEntrateService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
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

@ExtendWith(MockitoExtension.class)
class AgenziaEntrateControllerTest {

    @InjectMocks
    AgenziaEntrateController agenziaEntrateController;

    @Mock
    AgenziaEntrateService agenziaEntrateService;

    @Mock
    ServerWebExchange serverWebExchange;

    @Mock
    ValidateTaxIdUtils validateTaxIdUtils;

    @Mock
    Scheduler scheduler;


    @Test
    void checkTaxIdAndVatNumber() {
        ADELegalRequestBodyDto adeLegalRequestBodyDto = new ADELegalRequestBodyDto();
        ADELegalRequestBodyFilterDto adeLegalRequestBodyFilterDto = new ADELegalRequestBodyFilterDto();
        adeLegalRequestBodyFilterDto.setTaxId("PPPPLT80A01H501V");
        adeLegalRequestBodyFilterDto.setVatNumber("testVatNumber");
        adeLegalRequestBodyDto.setFilter(adeLegalRequestBodyFilterDto);
        ADELegalOKDto adeLegalOKDto = new ADELegalOKDto();
        adeLegalOKDto.setResultCode(ResultCodeEnum.fromValue("00"));
        adeLegalOKDto.setVerificationResult(true);
        adeLegalOKDto.setResultDetail(ResultDetailEnum.getCode("XX00"));
        StepVerifier.create(agenziaEntrateController.adeLegal(Mono.just(adeLegalRequestBodyDto), serverWebExchange))
                .expectNext(ResponseEntity.ok().body(adeLegalOKDto));
    }
}
