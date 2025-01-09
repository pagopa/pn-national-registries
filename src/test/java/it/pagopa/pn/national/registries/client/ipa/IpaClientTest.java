package it.pagopa.pn.national.registries.client.ipa;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.api.IpaApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class IpaClientTest {

    @InjectMocks
    private IpaClient ipaClient;
    @Mock
    private IpaApi ipaApi;

    PnAuditLogEventType type = PnAuditLogEventType.AUD_NR_PF_PHYSICAL;
    Map<String, String> mdc = new HashMap<>();
    String message = "message";
    Object[] arguments = new Object[] {"arg1", "arg2"};
    PnAuditLogEvent logEvent;

    @BeforeEach
    public void setup() {
        mdc.put("key", "value");
        logEvent = new PnAuditLogEvent(type, mdc, message, arguments);
    }

    @Test
    void callWS23Service() {

        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        List<DataWS23Dto> dataWS23DtoList = new ArrayList<>();
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        dataWS23Dto.setCodAmm("codeEnte");
        dataWS23Dto.setTipo("type");
        dataWS23Dto.setDomicilioDigitale("domicilioDigitale");
        dataWS23Dto.setDesAmm("denominazione");
        dataWS23DtoList.add(dataWS23Dto);
        ResultDto resultDto = new ResultDto();
        resultDto.setCodErr(0);
        resultDto.setDescErr("no error");
        resultDto.setNumItems(1);
        ws23ResponseDto.setData(dataWS23DtoList);
        ws23ResponseDto.setResult(resultDto);

        when(ipaApi.callEServiceWS23(any(), any())).thenReturn(Mono.just(ws23ResponseDto));

        StepVerifier.create(ipaClient.callEServiceWS23("taxId","secret", logEvent))
                .expectNext(ws23ResponseDto)
                .verifyComplete();
    }

    @Test
    void callWS05Service() {
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
        resultDto.setCodErr(0);
        resultDto.setDescErr("no error");
        resultDto.setNumItems(1);
        ws05ResponseDto.setData(dataWS05Dto);
        ws05ResponseDto.setResult(resultDto);

        when(ipaApi.callEServiceWS05(any(), any())).thenReturn(Mono.just(ws05ResponseDto));

        StepVerifier.create(ipaClient.callEServiceWS05("codiceAmministrazione", "secretName", logEvent))
                .expectNext(ws05ResponseDto)
                .verifyComplete();
    }

    @Test
    void callWS05ServiceException() {
        when(ipaApi.callEServiceWS05(any(), any())).thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

        StepVerifier.create(ipaClient.callEServiceWS05("codiceAmministrazione","secreName", logEvent))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }

    @Test
    void callWS23ServiceException() {
        when(ipaApi.callEServiceWS23(any(), any())).thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

        StepVerifier.create(ipaClient.callEServiceWS23("codiceAmministrazione","secreName", logEvent))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }
}