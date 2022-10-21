package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.converter.AddressAnprConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.model.anpr.E002RequestDto;
import it.pagopa.pn.national.registries.model.anpr.SearchCriteriaE002Dto;
import it.pagopa.pn.national.registries.model.anpr.RequestDateE002Dto;
import it.pagopa.pn.national.registries.model.anpr.RequestHeaderE002Dto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AnprService {

    private final AddressAnprConverter addressAnprConverter;
    private final AnprClient anprClient;
    private final String anprSendType;

    public AnprService(AddressAnprConverter addressAnprConverter,
                       AnprClient anprClient,
                       @Value("${pn.national.registries.pdnd.anpr.tipo-invio}") String anprSendType){
        this.addressAnprConverter = addressAnprConverter;
        this.anprClient = anprClient;
        this.anprSendType = anprSendType;
    }

    public Mono<GetAddressANPROKDto> getAddressANPR(GetAddressANPRRequestBodyDto request) {
        E002RequestDto e002RequestDto = createRequest(request);
        return anprClient.callEService(e002RequestDto)
                .map(rispostaE002OKDto -> addressAnprConverter.convertToGetAddressANPROKDto(rispostaE002OKDto, e002RequestDto.getCriteriRicerca().getCodiceFiscale()));
    }

    private E002RequestDto createRequest(GetAddressANPRRequestBodyDto request) {
        E002RequestDto richiesta = new E002RequestDto();
        SearchCriteriaE002Dto criteriRicercaE002Dto = new SearchCriteriaE002Dto();
        criteriRicercaE002Dto.setCodiceFiscale(request.getFilter().getTaxId());
        richiesta.setCriteriRicerca(criteriRicercaE002Dto);

        RequestHeaderE002Dto tipoTestata = new RequestHeaderE002Dto();
        tipoTestata.setIdOperazioneClient(String.valueOf(System.nanoTime()));
        tipoTestata.setCodMittente("600010");
        tipoTestata.setCodDestinatario("ANPR02");
        tipoTestata.setOperazioneRichiesta("E002");
        tipoTestata.setDataOraRichiesta(LocalDateTime.now().toString());
        tipoTestata.setTipoOperazione("C");
        tipoTestata.setTipoInvio(anprSendType);
        tipoTestata.setDataDecorrenza(request.getFilter().getReferenceRequestDate());
        richiesta.setTestataRichiesta(tipoTestata);

        RequestDateE002Dto dto = new RequestDateE002Dto();
        dto.setSchedaAnagraficaRichiesta("1");
        dto.setDataRiferimentoRichiesta(request.getFilter().getReferenceRequestDate());
        dto.setDatiAnagraficiRichiesti(List.of("1"));
        dto.setMotivoRichiesta(request.getFilter().getRequestReason());
        dto.setCasoUso("C001");

        richiesta.setDatiRichiesta(dto);
        log.debug("RichiestaE002Dto: {}",request);
        return richiesta;
    }
}
