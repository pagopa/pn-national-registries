package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADRequestBodyFilterDto;
import it.pagopa.pn.national.registries.service.InadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class InadControllerTest {
    private static final String AWS_XRAY_TRACE_ID = "AWS_XRAY_TRACE_ID";

    @InjectMocks
    InadController inadController;

    @Mock
    InadService inadService;

    @Mock
    ServerWebExchange serverWebExchange;

    @BeforeEach
    void init() {
        inadController = new InadController(inadService, Schedulers.immediate());
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void getDigitalAddressINAD() {
        String traceId = "traceId";
        MDC.put(AWS_XRAY_TRACE_ID, traceId);
        GetDigitalAddressINADRequestBodyDto extractDigitalAddressINADRequestBodyDto = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto dto = new GetDigitalAddressINADRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        extractDigitalAddressINADRequestBodyDto.setFilter(dto);

        GetDigitalAddressINADOKDto getDigitalAddressINADOKDto = new GetDigitalAddressINADOKDto();
        getDigitalAddressINADOKDto.setTaxId("DDDFGF52F52H501S");
        getDigitalAddressINADOKDto.setSince(new Date());

        when(inadService.callEService(extractDigitalAddressINADRequestBodyDto, RecipientType.PF, null))
                .thenReturn(Mono.fromSupplier(() -> {
                    assertEquals(traceId, MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
                    return getDigitalAddressINADOKDto;
                }));

        StepVerifier.create(inadController.digitalAddressINAD("PF", Mono.just(extractDigitalAddressINADRequestBodyDto), serverWebExchange))
                .expectNext(ResponseEntity.ok().body(getDigitalAddressINADOKDto))
                .verifyComplete();
    }
}
