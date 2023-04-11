package it.pagopa.pn.national.registries.client.ipa;

import it.pagopa.pn.national.registries.config.ipa.IpaSecretConfig;
import it.pagopa.pn.national.registries.model.ipa.DataWS23Dto;
import it.pagopa.pn.national.registries.model.ipa.IpaSecret;
import it.pagopa.pn.national.registries.model.ipa.ResultDto;
import it.pagopa.pn.national.registries.model.ipa.WS23ResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class IpaClientTest {

    @InjectMocks
    private IpaClient ipaClient;

    @Mock
    private WebClient webClient;

    @Mock
    private IpaWebClient ipaWebClient;

    @Mock
    private IpaSecretConfig ipaSecretConfig;

    @Test
    void callEService() {

        when(ipaWebClient.init()).thenReturn(webClient);

        IpaClient ipaClient = new IpaClient(ipaWebClient, ipaSecretConfig);

        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        List<DataWS23Dto> dataWS23DtoList = new ArrayList<>();
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        dataWS23Dto.setCodEnte("codeEnte");
        dataWS23Dto.setType("type");
        dataWS23Dto.setDomicilioDigitale("domicilioDigitale");
        dataWS23Dto.setDenominazione("denominazione");
        dataWS23DtoList.add(dataWS23Dto);
        ResultDto resultDto = new ResultDto();
        resultDto.setCodError(0);
        resultDto.setDescError("no error");
        resultDto.setNumItems(1);
        ws23ResponseDto.setData(dataWS23DtoList);
        ws23ResponseDto.setResult(resultDto);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(ipaSecretConfig.getIpaSecret()).thenReturn(new IpaSecret());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/ws/WS23DOMDIGCFServices/api/WS23_DOM_DIG_CF")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(WS23ResponseDto.class)).thenReturn(Mono.just(ws23ResponseDto));

        StepVerifier.create(ipaClient.callEServiceWS23("taxId"))
                .expectNext(ws23ResponseDto)
                .verifyComplete();
    }
}
