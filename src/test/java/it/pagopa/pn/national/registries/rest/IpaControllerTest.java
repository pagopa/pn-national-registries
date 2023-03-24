package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.service.IpaService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {IpaController.class})
@ExtendWith(SpringExtension.class)
class IpaControllerTest {

    @Autowired
    private IpaController ipaController;

    @MockBean
    private IpaService ipaService;

    @MockBean
    private Scheduler scheduler;

    @MockBean
    private ValidateTaxIdUtils validateTaxIdUtils;



    /**
     * Method under test: {@link IpaController#ipaPec(IPARequestBodyDto, ServerWebExchange)}
     */
    @Test
    void testIpaPec4() {
        when(ipaService.getIpaPec(any())).thenReturn(mock(Mono.class));
        doNothing().when(validateTaxIdUtils).validateTaxId(any());

        CheckTaxIdRequestBodyFilterDto checkTaxIdRequestBodyFilterDto = new CheckTaxIdRequestBodyFilterDto();
        checkTaxIdRequestBodyFilterDto.taxId("42");

        IPARequestBodyDto ipaRequestBodyDto = new IPARequestBodyDto();
        ipaRequestBodyDto.filter(checkTaxIdRequestBodyFilterDto);
        ipaController.ipaPec(ipaRequestBodyDto, null);
        verify(ipaService).getIpaPec(any());
        verify(validateTaxIdUtils).validateTaxId(any());
    }


}

