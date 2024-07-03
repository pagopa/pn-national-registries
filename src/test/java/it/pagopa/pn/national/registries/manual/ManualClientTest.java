package it.pagopa.pn.national.registries.manual;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.anpr.E002RequestDto;
import it.pagopa.pn.national.registries.model.anpr.RequestDateE002Dto;
import it.pagopa.pn.national.registries.model.anpr.ResponseE002OKDto;
import it.pagopa.pn.national.registries.model.anpr.SearchCriteriaE002Dto;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;
import it.pagopa.pn.national.registries.model.ipa.WS05ResponseDto;
import it.pagopa.pn.national.registries.model.ipa.WS23ResponseDto;
import it.pagopa.pn.national.registries.repository.CounterRepositoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Objects;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:/application-test.properties")
@ContextConfiguration(locations = {"classpath:app-context.xml"})
@Disabled("This test is disabled because it is a manual test")
public class ManualClientTest {

    @Autowired
    AnprClient anprClient;

    @Autowired
    InadClient inadClient;

    @Autowired
    IpaClient ipaClient;

    @Autowired
    InfoCamereClient infoCamereClient;

    @Autowired
    AdELegalClient adELegalClient;

    @Autowired
    CounterRepositoryImpl counterRepository;

    @Test
    void callAnpr() {
        Long idOperazione = Objects.requireNonNull(counterRepository.getCounter("anpr").block()).getCounter();
        E002RequestDto request = new E002RequestDto();
        request.setIdOperazioneClient(idOperazione.toString());
        SearchCriteriaE002Dto criteriRicercaE002Dto = new SearchCriteriaE002Dto();
        criteriRicercaE002Dto.setCodiceFiscale("STTSGT90A01H501J");
        request.setCriteriRicerca(criteriRicercaE002Dto);

        RequestDateE002Dto dto = new RequestDateE002Dto();
        dto.setDataRiferimentoRichiesta("2024-05-27");
        dto.setMotivoRichiesta("example reason for the request");
        dto.setCasoUso("C001");

        request.setDatiRichiesta(dto);
        ResponseE002OKDto response = anprClient.callEService(request).block();
        Assertions.assertNotNull(response);
    }

    @Test
    void callInad() {
        ResponseRequestDigitalAddressDto response = inadClient.callEService("STTSGT90A01H501J", "test").block();
        Assertions.assertNotNull(response);
    }

    @Test
    void callIPA() {
        WS23ResponseDto ws23ResponseDto = ipaClient.callEServiceWS23("STTSGT90A01H501J", "").block();
        WS05ResponseDto ws05ResponseDto = ipaClient.callEServiceWS05("", "").block();
        Assertions.assertNotNull(ws23ResponseDto);
        Assertions.assertNotNull(ws05ResponseDto);

    }

    @Test
    void callSedeLegale() {
        it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImprese response = infoCamereClient.getLegalAddress("STTSGT90A01H501J").block();
        Assertions.assertNotNull(response);
    }

    @Test
    void callLegaleRappresentanteElenco() {
        CheckTaxIdRequestBodyFilterDto filter = new CheckTaxIdRequestBodyFilterDto();
        filter.setTaxId("STTSGT90A01H501J");
        it.pagopa.pn.national.registries.model.infocamere.InfoCamereLegalInstituionsResponse response = infoCamereClient.getLegalInstitutions(filter).block();
        Assertions.assertNotNull(response);
    }

    @Test
    void callAde() {
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, "test");
        ADELegalRequestBodyFilterDto filter = new ADELegalRequestBodyFilterDto();
        filter.setTaxId("STTSGT90A01H501J");
        filter.setVatNumber("12345678955");
        String response = adELegalClient.checkTaxIdAndVatNumberAdE(filter).block();
        Assertions.assertNotNull(response);
    }
}
