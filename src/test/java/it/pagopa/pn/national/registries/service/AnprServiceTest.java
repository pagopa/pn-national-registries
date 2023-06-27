package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.converter.AnprConverter;
import it.pagopa.pn.national.registries.entity.CounterModel;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.anpr.ResponseE002OKDto;
import it.pagopa.pn.national.registries.model.anpr.SubjectsInstitutionDataDto;
import it.pagopa.pn.national.registries.model.anpr.SubjectsListDto;
import it.pagopa.pn.national.registries.model.anpr.ResidenceDto;
import it.pagopa.pn.national.registries.repository.CounterRepositoryImpl;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

    @Mock
    ValidateTaxIdUtils validateTaxIdUtils;

    @Test
    void testGetAddressANPR2() {

        GetAddressANPRRequestBodyDto request = new GetAddressANPRRequestBodyDto();
        GetAddressANPRRequestBodyFilterDto cf = new GetAddressANPRRequestBodyFilterDto();
        cf.setTaxId("DDDFFF80A01H501F");
        cf.setReferenceRequestDate("2022-10-28");
        request.setFilter(cf);

        ResponseE002OKDto response = new ResponseE002OKDto();
        SubjectsListDto listaSoggettiDto = new SubjectsListDto();
        SubjectsInstitutionDataDto soggettoEnteDto = new SubjectsInstitutionDataDto();

        List<SubjectsInstitutionDataDto> listDatiSoggetto = new ArrayList<>();
        List<ResidenceDto> listRes = new ArrayList<>();
        ResidenceDto res = new ResidenceDto();
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
        when(anprConverter.convertToGetAddressANPROKDto(any(), anyString())).thenReturn(getAddressANPROKDto);

        StepVerifier.create(anprService.getAddressANPR(request)).expectNext(getAddressANPROKDto).expectComplete().verify();
    }
}

