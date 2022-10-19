package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.converter.AddressAnprConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.model.anpr.RichiestaE002Dto;
import it.pagopa.pn.national.registries.model.anpr.TipoCriteriRicercaE002Dto;
import it.pagopa.pn.national.registries.model.anpr.TipoDatiRichiestaE002Dto;
import it.pagopa.pn.national.registries.model.anpr.TipoTestataRichiestaE000Dto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AnprService {

    private final AddressAnprConverter addressAnprConverter;
    private final AnprClient anprClient;

    public AnprService(AddressAnprConverter addressAnprConverter,
                       AnprClient anprClient) {
        this.addressAnprConverter = addressAnprConverter;
        this.anprClient = anprClient;
    }

    public Mono<GetAddressANPROKDto> getAddressANPR(GetAddressANPRRequestBodyDto request) {
        RichiestaE002Dto richiestaE002Dto = createRequest(request);
        return anprClient.callEService(richiestaE002Dto)
                .map(rispostaE002OKDto -> addressAnprConverter.convertToGetAddressANPROKDto(rispostaE002OKDto, richiestaE002Dto.getCriteriRicerca().getCodiceFiscale()));
    }

    private RichiestaE002Dto createRequest(GetAddressANPRRequestBodyDto request) {
        RichiestaE002Dto richiesta = new RichiestaE002Dto();
        TipoCriteriRicercaE002Dto criteriRicercaE002Dto = new TipoCriteriRicercaE002Dto();
        criteriRicercaE002Dto.setCodiceFiscale(request.getFilter().getTaxId());
        richiesta.setCriteriRicerca(criteriRicercaE002Dto);

        TipoTestataRichiestaE000Dto tipoTestata = new TipoTestataRichiestaE000Dto();
        tipoTestata.setIdOperazioneClient(String.valueOf(System.nanoTime()));
        tipoTestata.setCodMittente("600010");
        tipoTestata.setCodDestinatario("ANPR02");
        tipoTestata.setOperazioneRichiesta("E002");
        tipoTestata.setDataOraRichiesta(LocalDateTime.now().toString());
        tipoTestata.setTipoOperazione("C");
        tipoTestata.setTipoInvio("TEST");
        tipoTestata.setDataDecorrenza(request.getFilter().getReferenceRequestDate());
        richiesta.setTestataRichiesta(tipoTestata);

        TipoDatiRichiestaE002Dto dto = new TipoDatiRichiestaE002Dto();
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
