package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inipec.IniPecClient;
import it.pagopa.pn.national.registries.converter.IniPecConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseOKProfessionalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.registroImprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.model.registroImprese.LegalAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroImpreseServiceTest {

    @InjectMocks
    RegistroImpreseService registroImpreseService;

    @Mock
    IniPecClient iniPecClient;

    @Mock
    IniPecConverter iniPecConverter;

    @Test
    void getAddress() {

        GetAddressRegistroImpreseRequestBodyDto request = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto dto = new GetAddressRegistroImpreseRequestBodyFilterDto();
        dto.setTaxId("cf");
        request.setFilter(dto);

        GetAddressRegistroImpreseOKDto response = new GetAddressRegistroImpreseOKDto();
        response.setTaxId("cf");
        GetAddressRegistroImpreseOKProfessionalAddressDto professional = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        professional.setAddress("address");
        response.setProfessionalAddress(professional);

        AddressRegistroImpreseResponse addressRegistroImpreseResponse = new AddressRegistroImpreseResponse();
        addressRegistroImpreseResponse.setTaxId("cf");
        LegalAddress legalAddress = new LegalAddress();
        legalAddress.setToponym("address");
        addressRegistroImpreseResponse.setAddress(legalAddress);

        when(iniPecClient.getLegalAddress(any())).thenReturn(Mono.just(addressRegistroImpreseResponse));
        when(iniPecConverter.mapToResponseOk(any())).thenReturn(response);

        StepVerifier.create(registroImpreseService.getAddress(request))
                .expectNext(response).verifyComplete();
    }
}