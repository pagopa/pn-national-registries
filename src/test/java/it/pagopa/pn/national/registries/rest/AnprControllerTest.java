package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyFilterDto;
import it.pagopa.pn.national.registries.service.AnprService;
import it.pagopa.pn.national.registries.service.GatewayService;
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

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnprControllerTest {
    private static final String AWS_XRAY_TRACE_ID = "AWS_XRAY_TRACE_ID";

    AnprController anprController;

    @Mock
    AnprService anprService;

    @Mock
    ServerWebExchange serverWebExchange;


    @BeforeEach
    void init() {
        anprController = new AnprController(anprService, new GatewayService(null, null, null, null, null), Schedulers.immediate());
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void testGetAddressANPR() {
        String traceId = "traceId";
        MDC.put(AWS_XRAY_TRACE_ID, traceId);
        GetAddressANPRRequestBodyDto getAddressANPRRequestBodyDto = new GetAddressANPRRequestBodyDto();
        GetAddressANPRRequestBodyFilterDto dto = new GetAddressANPRRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        getAddressANPRRequestBodyDto.setFilter(dto);

        GetAddressANPROKDto getAddressANPROKDto = new GetAddressANPROKDto();
        getAddressANPROKDto.setResidentialAddresses(new ArrayList<>());

        when(anprService.getAddressANPR(getAddressANPRRequestBodyDto))
                .thenReturn(Mono.fromSupplier(() -> {
                    assertEquals(traceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
                    return getAddressANPROKDto;
                }));

        StepVerifier.create(anprController.addressANPR(Mono.just(getAddressANPRRequestBodyDto), serverWebExchange))
                .expectNext(ResponseEntity.ok().body(getAddressANPROKDto))
                .verifyComplete();
    }
}
