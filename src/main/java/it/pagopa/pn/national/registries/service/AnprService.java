package it.pagopa.pn.national.registries.service;

import com.amazonaws.util.StringUtils;
import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.converter.AnprConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.model.anpr.*;
import it.pagopa.pn.national.registries.repository.CounterRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AnprService {

    private final AnprConverter anprConverter;
    private final AnprClient anprClient;
    private final String anprSendType;
    private final CounterRepositoryImpl counterRepository;

    public AnprService(AnprConverter anprConverter,
                       AnprClient anprClient,
                       @Value("${pn.national.registries.anpr.tipo-invio}") String anprSendType,
                       CounterRepositoryImpl counterRepository) {
        this.anprConverter = anprConverter;
        this.anprClient = anprClient;
        this.anprSendType = anprSendType;
        this.counterRepository = counterRepository;
    }

    public Mono<GetAddressANPROKDto> getAddressANPR(GetAddressANPRRequestBodyDto request) {
        if (StringUtils.isNullOrEmpty(request.getFilter().getReferenceRequestDate())) {
            throw new PnNationalRegistriesException("ReferenceRequestDate cannot be empty", HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null,
                    Charset.defaultCharset(), AnprResponseKO.class);
        }
        String cf = request.getFilter().getTaxId();
        return createRequest(request)
                .flatMap(anprClient::callEService)
                .map(rispostaE002OKDto -> anprConverter.convertToGetAddressANPROKDto(rispostaE002OKDto, cf));
    }

    private Mono<E002RequestDto> createRequest(GetAddressANPRRequestBodyDto request) {
        return counterRepository.getCounter("anpr")
                .flatMap(s -> Mono.just(constructE002RequestDto(request, s.getCounter())));
    }

    private E002RequestDto constructE002RequestDto(GetAddressANPRRequestBodyDto request, Long s) {
        E002RequestDto richiesta = new E002RequestDto();
        SearchCriteriaE002Dto criteriRicercaE002Dto = new SearchCriteriaE002Dto();
        criteriRicercaE002Dto.setCodiceFiscale(request.getFilter().getTaxId());
        richiesta.setCriteriRicerca(criteriRicercaE002Dto);

        RequestHeaderE002Dto tipoTestata = new RequestHeaderE002Dto();
        tipoTestata.setIdOperazioneClient(String.valueOf(s));
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
        return richiesta;
    }
}
