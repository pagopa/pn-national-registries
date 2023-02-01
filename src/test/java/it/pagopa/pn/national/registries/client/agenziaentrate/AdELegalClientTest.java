package it.pagopa.pn.national.registries.client.agenziaentrate;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.xml.bind.JAXB;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {AdELegalClient.class})
@ExtendWith(SpringExtension.class)
class AdELegalClientTest {

    @Autowired
    private AdELegalClient adELegalClient;


    @MockBean
    WebClient webClient;

    @MockBean
    AgenziaEntrateWebClientSOAP agenziaEntrateWebClientSOAP;


    /**
     * Method under test: {@link AdELegalClient#getToken()}
     */
    @Test
    void testGetToken2() {
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Diffblue AI was unable to find a test

        AgenziaEntrateWebClientSOAP agenziaEntrateWebClientSOAP = mock(AgenziaEntrateWebClientSOAP.class);
        when(agenziaEntrateWebClientSOAP.init()).thenReturn(null);
        (new AdELegalClient(agenziaEntrateWebClientSOAP)).getToken();
        verify(agenziaEntrateWebClientSOAP).init();
    }

    /**
     * Method under test: {@link AdELegalClient#checkTaxIdAndVatNumberAdE(ADELegalRequestBodyFilterDto)}
     */
    @Test
    void testCheckTaxIdAndVatNumberAdE2() {
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Diffblue AI was unable to find a test

        AgenziaEntrateWebClientSOAP agenziaEntrateWebClientSOAP = mock(AgenziaEntrateWebClientSOAP.class);
        when(agenziaEntrateWebClientSOAP.init()).thenReturn(null);
        AdELegalClient adELegalClient = new AdELegalClient(agenziaEntrateWebClientSOAP);
        adELegalClient.checkTaxIdAndVatNumberAdE(new ADELegalRequestBodyFilterDto());
        verify(agenziaEntrateWebClientSOAP).init();
    }

    @Test
    void checkTaxIdAndVatNumberTest() {
        when(agenziaEntrateWebClientSOAP.init()).thenReturn(webClient);
        AdELegalClient adELegalClient = new AdELegalClient(agenziaEntrateWebClientSOAP);

        ADELegalRequestBodyFilterDto adeLegalRequestBodyFilterDto = new ADELegalRequestBodyFilterDto();
        adeLegalRequestBodyFilterDto.setVatNumber("testVatNumber");
        adeLegalRequestBodyFilterDto.setTaxId("setTaxId");

        StringWriter requestSW = new StringWriter();
        JAXB.marshal(adeLegalRequestBodyFilterDto, requestSW);

        CheckValidityRappresentanteResp checkValidityRappresentanteRespType = new CheckValidityRappresentanteResp();
        checkValidityRappresentanteRespType.setValido(true);
        checkValidityRappresentanteRespType.setDettaglioEsito("XX00");
        checkValidityRappresentanteRespType.setCodiceRitorno("00");

        StringWriter responseSW = new StringWriter();
        JAXB.marshal(checkValidityRappresentanteRespType, responseSW);
        String response = responseSW.toString();

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);


        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/legalerappresentateAdE/check")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(response));

        StepVerifier.create(adELegalClient.checkTaxIdAndVatNumberAdE(adeLegalRequestBodyFilterDto)).expectNext(response).verifyComplete();

    }

    @Test
    @DisplayName("Should return false when the exception is not webclientresponseexception")
    void checkExceptionTypeWhenNotWebClientResponseExceptionThenReturnFalse() {
        AdELegalClient adELegalClient = new AdELegalClient(agenziaEntrateWebClientSOAP);
        assertFalse(adELegalClient.checkExceptionType(new Exception()));
    }

    @Test
    @DisplayName(
            "Should return true when the exception is webclientresponseexception and the status code is 401")
    void checkExceptionTypeWhenWebClientResponseExceptionAndStatusCodeIs401ThenReturnTrue() {
        AdELegalClient adELegalClient = new AdELegalClient(agenziaEntrateWebClientSOAP);

        WebClientResponseException webClientResponseException =
                new WebClientResponseException(
                        "message",
                        HttpStatus.UNAUTHORIZED.value(),
                        "statusText",
                        HttpHeaders.EMPTY,
                        null,
                        null);
        assertTrue(adELegalClient.checkExceptionType(webClientResponseException));
    }

    /**
     * Method under test: {@link AdELegalClient#checkExceptionType(Throwable)}
     */
    @Test
    void testCheckExceptionType() {
        assertFalse(adELegalClient.checkExceptionType(new Throwable()));
    }
}
