package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.service.IpaService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.scheduler.VirtualTimeScheduler;

import static org.mockito.Mockito.*;

class IpaControllerTest {

    /**
     * Method under test: {@link IpaController#ipaPec(IPARequestBodyDto, ServerWebExchange)}
     */
    @Test
    void testIpaPec7() {

        IpaService ipaService = mock(IpaService.class);
        IPAPecOKDto ipaPecOKDto = new IPAPecOKDto();
        when(ipaService.getIpaPec(any())).thenReturn(Mono.just(ipaPecOKDto));
        ValidateTaxIdUtils validateTaxIdUtils = mock(ValidateTaxIdUtils.class);
        doNothing().when(validateTaxIdUtils).validateTaxId(any());
        IpaController ipaController = new IpaController(ipaService, VirtualTimeScheduler.create(true),
                validateTaxIdUtils);
        ipaController.ipaPec(new IPARequestBodyDto(), null);
        verify(ipaService).getIpaPec(any());
        verify(validateTaxIdUtils).validateTaxId(any());
    }
}

