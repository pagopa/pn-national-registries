package it.pagopa.pn.national.registries.rest;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADRequestBodyFilterDto;
import it.pagopa.pn.national.registries.service.InadService;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class InadControllerTest {

    @InjectMocks
    InadController inadController;

    @Mock
    InadService inadService;

    @Mock
    ServerWebExchange serverWebExchange;

    @Mock
    ValidateTaxIdUtils validateTaxIdUtils;

    @Test
    void getDigitalAddressINAD() {
        GetDigitalAddressINADRequestBodyDto extractDigitalAddressINADRequestBodyDto = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto dto = new GetDigitalAddressINADRequestBodyFilterDto();
        dto.setTaxId("PPPPLT80A01H501V");
        extractDigitalAddressINADRequestBodyDto.setFilter(dto);

        GetDigitalAddressINADOKDto getDigitalAddressINADOKDto = new GetDigitalAddressINADOKDto();
        getDigitalAddressINADOKDto.setTaxId("DDDFGF52F52H501S");
        getDigitalAddressINADOKDto.setSince(new Date());
        when(inadService.callEService(extractDigitalAddressINADRequestBodyDto)).thenReturn(Mono.just(getDigitalAddressINADOKDto));
        StepVerifier.create(inadController.digitalAddressINAD(extractDigitalAddressINADRequestBodyDto,serverWebExchange))
                .expectNext(ResponseEntity.ok().body(getDigitalAddressINADOKDto));
    }
}
