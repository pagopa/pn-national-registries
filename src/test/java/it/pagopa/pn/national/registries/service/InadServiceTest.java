package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.inad.ElementDigitalAddressDto;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InadServiceTest {
    @InjectMocks
    InadService inadService;

    @Mock
    InadClient inadClient;

    @Test
    void callEService() {
        String taxId = "test";
        String practicalReference = "00001";
        Date now = new Date();

        ResponseRequestDigitalAddressDto responseRequestDigitalAddressDto = new ResponseRequestDigitalAddressDto();
        responseRequestDigitalAddressDto.setTaxId("cf");
        responseRequestDigitalAddressDto.setSince("2017-07-21T17:32:28Z");
        List<ElementDigitalAddressDto> lista = new ArrayList<>();
        responseRequestDigitalAddressDto.setDigitalAddress(lista);
        when(inadClient.callEService(anyString(), anyString())).thenReturn(Mono.just(responseRequestDigitalAddressDto));

        GetDigitalAddressINADRequestBodyDto req = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();
        filterDto.setTaxId(taxId);
        filterDto.setPracticalReference(practicalReference);
        req.setFilter(filterDto);

        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();
        response.setTaxId(taxId);
        response.setSince(now);

        StepVerifier.create(inadService.callEService(req)).expectNext(response).verifyComplete();
    }
}
