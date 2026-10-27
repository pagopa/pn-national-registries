package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.service.IpaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.MDC;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class IpaControllerTest {
    private static final String AWS_XRAY_TRACE_ID = "AWS_XRAY_TRACE_ID";

    @InjectMocks
    private IpaController ipaController;

    @Mock
    private IpaService ipaService;

    @BeforeEach
    void init() {
        ipaController = new IpaController(ipaService, Schedulers.immediate());
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void testIpaPec4() {
        String traceId = "traceId";
        MDC.put(AWS_XRAY_TRACE_ID, traceId);
        IPAPecDto ipaPecDto = new IPAPecDto();
        when(ipaService.getIpaPec(any())).thenReturn(Mono.just(ipaPecDto));

        CheckTaxIdRequestBodyFilterDto checkTaxIdRequestBodyFilterDto = new CheckTaxIdRequestBodyFilterDto();
        checkTaxIdRequestBodyFilterDto.taxId("42");

        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        ipaRequestBodyDto.filter(checkTaxIdRequestBodyFilterDto);
        ipaController.ipaPec(Mono.just(ipaRequestBodyDto), null);
        when(ipaService.getIpaPec(ipaRequestBodyDto))
                .thenReturn(Mono.fromSupplier(() -> {
                    assertEquals(traceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
                    return ipaPecDto;
                }));

        StepVerifier.create(ipaController.ipaPec(Mono.just(ipaRequestBodyDto), null))
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals(ipaPecDto, response.getBody());
                    assertEquals(traceId, response.getHeaders().getFirst("X-Request-ID"));
                })
                .verifyComplete();
    }


}

