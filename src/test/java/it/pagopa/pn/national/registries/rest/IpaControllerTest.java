package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.service.IpaService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class IpaControllerTest {

    @InjectMocks
    private IpaController ipaController;

    @Mock
    private IpaService ipaService;

    @Mock
    private ValidateTaxIdUtils validateTaxIdUtils;



    /**
     * Method under test: {@link IpaController#ipaPec(IPARequestBodyDto, ServerWebExchange)}
     */
    @Test
    void testIpaPec4() {
        IPAPecDto ipaPecDto = new IPAPecDto();
        when(ipaService.getIpaPec(any())).thenReturn(Mono.just(ipaPecDto));
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

