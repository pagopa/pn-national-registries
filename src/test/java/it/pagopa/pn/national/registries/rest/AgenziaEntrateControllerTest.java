package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.agenziaentrate.ResultCodeEnum;
import it.pagopa.pn.national.registries.model.agenziaentrate.ResultDetailEnum;
import it.pagopa.pn.national.registries.service.AgenziaEntrateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgenziaEntrateControllerTest {
    private AgenziaEntrateController agenziaEntrateController;
    private static final String AWS_XRAY_TRACE_ID = "AWS_XRAY_TRACE_ID";

    @Mock
    AgenziaEntrateService agenziaEntrateService;

    @Mock
    ServerWebExchange serverWebExchange;

    @BeforeEach
    void setUp() {
        agenziaEntrateController = new AgenziaEntrateController(agenziaEntrateService, Schedulers.immediate());
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void checkTaxId() {
        String traceId = "trace-id-123";
        MDC.put(AWS_XRAY_TRACE_ID, traceId);

        CheckTaxIdRequestBodyDto checkTaxIdRequestBodyDto = new CheckTaxIdRequestBodyDto();
        CheckTaxIdRequestBodyFilterDto dto = new CheckTaxIdRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        checkTaxIdRequestBodyDto.setFilter(dto);

        CheckTaxIdOKDto checkTaxIdOKDto = new CheckTaxIdOKDto();
        checkTaxIdOKDto.setTaxId("PPPPLT80A01H501V");
        checkTaxIdOKDto.setIsValid(true);

        when(agenziaEntrateService.callEService(checkTaxIdRequestBodyDto))
                .thenReturn((Mono.fromSupplier(() -> {
                    assertEquals(traceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
                    return checkTaxIdOKDto;
                })));

        StepVerifier.create(agenziaEntrateController.checkTaxId(Mono.just(checkTaxIdRequestBodyDto), serverWebExchange))
                .expectNext(ResponseEntity.ok(checkTaxIdOKDto))
                .verifyComplete();
    }

    @Test
    void checkTaxIdAndVatNumber() {
        String traceId = "trace-id-456";
        MDC.put(AWS_XRAY_TRACE_ID, traceId);

        ADELegalRequestBodyDto adeLegalRequestBodyDto = new ADELegalRequestBodyDto();
        ADELegalRequestBodyFilterDto adeLegalRequestBodyFilterDto = new ADELegalRequestBodyFilterDto();
        adeLegalRequestBodyFilterDto.setTaxId("PPPPLT80A01H501V");
        adeLegalRequestBodyFilterDto.setVatNumber("testVatNumber");
        adeLegalRequestBodyDto.setFilter(adeLegalRequestBodyFilterDto);
        ADELegalOKDto adeLegalOKDto = new ADELegalOKDto();
        adeLegalOKDto.setResultCode(ResultCodeEnum.fromValue("00"));
        adeLegalOKDto.setVerificationResult(true);
        adeLegalOKDto.setResultDetail(ResultDetailEnum.getCode("XX00"));

        when(agenziaEntrateService.checkTaxIdAndVatNumber(adeLegalRequestBodyDto))
                .thenReturn((Mono.fromSupplier(() -> {
                    assertEquals(traceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
                    return adeLegalOKDto;
                })));

        StepVerifier.create(agenziaEntrateController.adeLegal(Mono.just(adeLegalRequestBodyDto), serverWebExchange))
                .expectNext(ResponseEntity.ok(adeLegalOKDto))
                .verifyComplete();
    }
}
