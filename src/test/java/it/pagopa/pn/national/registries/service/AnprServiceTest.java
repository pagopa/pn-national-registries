package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.converter.AnprConverter;
import it.pagopa.pn.national.registries.entity.CounterModel;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyFilterDto;
import it.pagopa.pn.national.registries.repository.CounterRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class AnprServiceTest {

    @InjectMocks
    AnprService anprService;

    @Mock
    AnprConverter anprConverter;

    @Mock
    AnprClient anprClient;

    @Mock
    CounterRepositoryImpl counterRepository;

    @Test
    void testGetAddressANPR2() {

        GetAddressANPRRequestBodyDto request = new GetAddressANPRRequestBodyDto();
        GetAddressANPRRequestBodyFilterDto cf = new GetAddressANPRRequestBodyFilterDto();
        cf.setTaxId("DDDFFF80A01H501F");
        cf.setReferenceRequestDate(Date.from(Instant.now()));
        request.setFilter(cf);

        RispostaE002OK response = new RispostaE002OK();
        TipoListaSoggetti listaSoggettiDto = new TipoListaSoggetti();
        TipoDatiSoggettiEnte soggettoEnteDto = new TipoDatiSoggettiEnte();

        List<TipoDatiSoggettiEnte> listDatiSoggetto = new ArrayList<>();
        List<TipoResidenza> listRes = new ArrayList<>();
        TipoResidenza res = new TipoResidenza();
        res.setNoteIndirizzo("indirizzo di test");
        listRes.add(res);
        soggettoEnteDto.setResidenza(listRes);
        listDatiSoggetto.add(soggettoEnteDto);

        listaSoggettiDto.setDatiSoggetto(listDatiSoggetto);
        response.setListaSoggetti(listaSoggettiDto);

        GetAddressANPROKDto getAddressANPROKDto = new GetAddressANPROKDto();
        getAddressANPROKDto.setResidentialAddresses(new ArrayList<>());

        CounterModel counterModel = new CounterModel();
        counterModel.setCounter(1L);
        when(counterRepository.getCounter("anpr")).thenReturn(Mono.just(counterModel));
        when(anprClient.callEService(any())).thenReturn(Mono.just(response));
        when(anprConverter.convertToGetAddressANPROK(any(), anyString())).thenReturn(getAddressANPROKDto);

        StepVerifier.create(anprService.getAddressANPR(request)).expectNext(getAddressANPROKDto).expectComplete().verify();
    }


    @Test
    void testGetAddressANPR3() {
        GetAddressANPRRequestBodyDto request = new GetAddressANPRRequestBodyDto();
        GetAddressANPRRequestBodyFilterDto cf = new GetAddressANPRRequestBodyFilterDto();
        cf.setTaxId("DDDFFF80A01H501F");
        request.setFilter(cf);
        assertThrows(PnNationalRegistriesException.class, () -> anprService.getAddressANPR(request));
    }

    @Test
    void getAddressANPR_shouldBuildE002RequestFromCounterAndFilter() {
        GetAddressANPRRequestBodyDto request = request("DDDFFF80A01H501F", "2022-10-28", "correlation-id");
        RispostaE002OK response = new RispostaE002OK();
        GetAddressANPROKDto expected = new GetAddressANPROKDto();
        when(counterRepository.getCounter("anpr")).thenReturn(Mono.just(counter(42L)));
        when(anprClient.callEService(any())).thenReturn(Mono.just(response));
        when(anprConverter.convertToGetAddressANPROK(response, "DDDFFF80A01H501F")).thenReturn(expected);

        StepVerifier.create(anprService.getAddressANPR(request)).expectNext(expected).verifyComplete();

        ArgumentCaptor<RichiestaE002> captor = ArgumentCaptor.forClass(RichiestaE002.class);
        verify(anprClient).callEService(captor.capture());
        RichiestaE002 sent = captor.getValue();
        assertEquals("42", sent.getIdOperazioneClient());
        assertEquals("DDDFFF80A01H501F", sent.getCriteriRicerca().getCodiceFiscale());
        assertEquals("2022-10-28", sent.getDatiRichiesta().getDataRiferimentoRichiesta());
        assertEquals("correlation-id", sent.getDatiRichiesta().getMotivoRichiesta());
        assertEquals("C001", sent.getDatiRichiesta().getCasoUso());
    }

    @Test
    void getAddressANPR_shouldFailWithBadRequestWhenReferenceRequestDateIsEmpty() {
        GetAddressANPRRequestBodyDto request = request("DDDFFF80A01H501F", "", "correlation-id");

        PnNationalRegistriesException exception = assertThrows(PnNationalRegistriesException.class, () -> anprService.getAddressANPR(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(counterRepository, anprClient, anprConverter);
    }


    @Test
    void getAddressANPR_shouldPropagateCounterError() {
        GetAddressANPRRequestBodyDto request = request("DDDFFF80A01H501F", "2022-10-28", "correlation-id");
        RuntimeException error = new RuntimeException("counter unavailable");
        when(counterRepository.getCounter("anpr")).thenReturn(Mono.error(error));

        StepVerifier.create(anprService.getAddressANPR(request)).expectErrorMatches(e -> e == error).verify();

        verifyNoInteractions(anprClient, anprConverter);
    }

    @Test
    void getAddressANPR_shouldPropagateClientError() {
        GetAddressANPRRequestBodyDto request = request("DDDFFF80A01H501F", "2022-10-28", "correlation-id");
        RuntimeException error = new RuntimeException("anpr down");
        when(counterRepository.getCounter("anpr")).thenReturn(Mono.just(counter(1L)));
        when(anprClient.callEService(any())).thenReturn(Mono.error(error));

        StepVerifier.create(anprService.getAddressANPR(request)).expectErrorMatches(e -> e == error).verify();

        verifyNoInteractions(anprConverter);
    }

    private static GetAddressANPRRequestBodyDto request(String taxId, String referenceRequestDate, String requestReason) {
        GetAddressANPRRequestBodyFilterDto filter = new GetAddressANPRRequestBodyFilterDto();
        filter.setTaxId(taxId);
        filter.setReferenceRequestDate(referenceRequestDate);
        filter.setRequestReason(requestReason);
        GetAddressANPRRequestBodyDto request = new GetAddressANPRRequestBodyDto();
        request.setFilter(filter);
        return request;
    }

    private static CounterModel counter(long value) {
        CounterModel counterModel = new CounterModel();
        counterModel.setCounter(value);
        return counterModel;
    }

}

