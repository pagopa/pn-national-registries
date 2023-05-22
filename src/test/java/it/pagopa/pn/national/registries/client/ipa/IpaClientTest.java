package it.pagopa.pn.national.registries.client.ipa;

import it.pagopa.pn.national.registries.config.ipa.IpaSecretConfig;
import it.pagopa.pn.national.registries.model.ipa.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
    void callWS23Service() {

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
        when(requestBodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(WS23ResponseDto.class)).thenReturn(Mono.just(ws23ResponseDto));

        StepVerifier.create(ipaClient.callEServiceWS23("taxId"))
                .expectNext(ws23ResponseDto)
                .verifyComplete();
    }

    @Test
    void callWS05Service() {

        when(ipaWebClient.init()).thenReturn(webClient);

        IpaClient ipaClient = new IpaClient(ipaWebClient, ipaSecretConfig);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("acronimo");
        dataWS05Dto.setCf("codiceFiscale");
        dataWS05Dto.setCap("cap");
        dataWS05Dto.setCategoria("categoria");
        dataWS05Dto.setDataAccreditamento("dataAccreditamento");
        dataWS05Dto.setComune("comune");
        dataWS05Dto.setCodAmm("codiceAmministrazione");
        dataWS05Dto.setIndirizzo("indirizzo");
        dataWS05Dto.setIndirizzo("indirizzo");
        dataWS05Dto.setProvincia("provincia");
        dataWS05Dto.setLivAccessibilita("livelloAccessibilita");
        dataWS05Dto.setMail1("mail1");
        dataWS05Dto.setMail2("mail2");
        dataWS05Dto.setMail3("mail3");
        dataWS05Dto.setMail4("mail4");
        dataWS05Dto.setMail5("mail5");
        dataWS05Dto.setCognResp("cognomeResponsabile");
        dataWS05Dto.setSitoIstituzionale("sitoIstituzionale");
        dataWS05Dto.setTitoloResp("titoloResponsabile");
        dataWS05Dto.setTitoloResp("titoloResponsabile");
        dataWS05Dto.setRegione("regione");
        dataWS05Dto.setDesAmm("descrizioneAmministrazione");

        ResultDto resultDto = new ResultDto();
        resultDto.setCodError(0);
        resultDto.setDescError("no error");
        resultDto.setNumItems(1);
        ws05ResponseDto.setData(dataWS05Dto);
        ws05ResponseDto.setResult(resultDto);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(ipaSecretConfig.getIpaSecret()).thenReturn(new IpaSecret());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("ws/WS05AMMServices/api/WS05_AMM")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(WS05ResponseDto.class)).thenReturn(Mono.just(ws05ResponseDto));

        StepVerifier.create(ipaClient.callEServiceWS05("codiceAmministrazione"))
                .expectNext(ws05ResponseDto)
                .verifyComplete();
    }

    @Test
    void callWS05ServiceException() {

        when(ipaWebClient.init()).thenReturn(webClient);

        IpaClient ipaClient = new IpaClient(ipaWebClient, ipaSecretConfig);

        WS05ResponseDto ws05ResponseDto = new WS05ResponseDto();
        DataWS05Dto dataWS05Dto = new DataWS05Dto();
        dataWS05Dto.setAcronimo("acronimo");
        dataWS05Dto.setCf("codiceFiscale");
        dataWS05Dto.setCap("cap");
        dataWS05Dto.setCategoria("categoria");
        dataWS05Dto.setDataAccreditamento("dataAccreditamento");
        dataWS05Dto.setComune("comune");
        dataWS05Dto.setCodAmm("codiceAmministrazione");
        dataWS05Dto.setIndirizzo("indirizzo");
        dataWS05Dto.setIndirizzo("indirizzo");
        dataWS05Dto.setProvincia("provincia");
        dataWS05Dto.setLivAccessibilita("livelloAccessibilita");
        dataWS05Dto.setMail1("mail1");
        dataWS05Dto.setMail2("mail2");
        dataWS05Dto.setMail3("mail3");
        dataWS05Dto.setMail4("mail4");
        dataWS05Dto.setMail5("mail5");
        dataWS05Dto.setCognResp("cognomeResponsabile");
        dataWS05Dto.setSitoIstituzionale("sitoIstituzionale");
        dataWS05Dto.setTitoloResp("titoloResponsabile");
        dataWS05Dto.setTitoloResp("titoloResponsabile");
        dataWS05Dto.setRegione("regione");
        dataWS05Dto.setDesAmm("descrizioneAmministrazione");

        ResultDto resultDto = new ResultDto();
        resultDto.setCodError(0);
        resultDto.setDescError("no error");
        resultDto.setNumItems(1);
        ws05ResponseDto.setData(dataWS05Dto);
        ws05ResponseDto.setResult(resultDto);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(ipaSecretConfig.getIpaSecret()).thenReturn(new IpaSecret());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("ws/WS05AMMServices/api/WS05_AMM")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(WS05ResponseDto.class)).thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

        StepVerifier.create(ipaClient.callEServiceWS05("codiceAmministrazione"))
                .expectError(WebClientResponseException.class)
                .verify();
    }
}
