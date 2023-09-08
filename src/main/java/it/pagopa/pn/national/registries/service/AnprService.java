package it.pagopa.pn.national.registries.service;

import com.amazonaws.util.StringUtils;
import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.converter.AnprConverter;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.national.registries.model.anpr.*;
import it.pagopa.pn.national.registries.repository.CounterRepositoryImpl;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import it.pagopa.pn.national.registries.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_NAME_ANPR_ADDRESS;

@Service
@Slf4j
public class AnprService {

    private final AnprConverter anprConverter;
    private final AnprClient anprClient;
    private final CounterRepositoryImpl counterRepository;
    private final ValidateTaxIdUtils validateTaxIdUtils;

    public AnprService(AnprConverter anprConverter,
                       AnprClient anprClient,
                       CounterRepositoryImpl counterRepository,
                       ValidateTaxIdUtils validateTaxIdUtils) {
        this.anprConverter = anprConverter;
        this.anprClient = anprClient;
        this.counterRepository = counterRepository;
        this.validateTaxIdUtils = validateTaxIdUtils;
    }

    public Mono<GetAddressANPROKDto> getAddressANPR(GetAddressANPRRequestBodyDto request) {
        String cf = request.getFilter().getTaxId();
        validateTaxIdUtils.validateTaxId(cf, PROCESS_NAME_ANPR_ADDRESS, false);

        if (StringUtils.isNullOrEmpty(request.getFilter().getReferenceRequestDate())) {
            throw new PnNationalRegistriesException("ReferenceRequestDate cannot be empty", HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null,
                    Charset.defaultCharset(), AnprResponseKO.class);
        }

        return createRequest(request)
                .doOnNext(batchRequest -> log.info("Created ANPR request for taxId: {}", MaskDataUtils.maskString(cf)))
                .flatMap(anprClient::callEService)
                .doOnNext(batchRequest -> log.info("Got ResponseE002OKDto fox taxId: {}", MaskDataUtils.maskString(cf)))
                .map(rispostaE002OKDto -> anprConverter.convertToGetAddressANPROKDto(rispostaE002OKDto, cf));
    }

    private Mono<E002RequestDto> createRequest(GetAddressANPRRequestBodyDto request) {
        return counterRepository.getCounter("anpr")
                .map(s -> constructE002RequestDto(request, s.getCounter()));
    }

    private E002RequestDto constructE002RequestDto(GetAddressANPRRequestBodyDto request, Long s) {
        E002RequestDto richiesta = new E002RequestDto();
        richiesta.setIdOperazioneClient(s.toString());
        SearchCriteriaE002Dto criteriRicercaE002Dto = new SearchCriteriaE002Dto();
        criteriRicercaE002Dto.setCodiceFiscale(request.getFilter().getTaxId());
        richiesta.setCriteriRicerca(criteriRicercaE002Dto);

        RequestDateE002Dto dto = new RequestDateE002Dto();
        dto.setDataRiferimentoRichiesta(request.getFilter().getReferenceRequestDate());
        dto.setMotivoRichiesta(request.getFilter().getRequestReason());
        dto.setCasoUso("C001");

        richiesta.setDatiRichiesta(dto);
        return richiesta;
    }
}
