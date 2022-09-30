package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.client.anpr.E002ServiceApiCustom;
import it.pagopa.pn.national.registries.converter.AddressAnprConverter;
import it.pagopa.pn.national.registries.exceptions.AnprException;
import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.dto.RichiestaE002Dto;
import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.dto.TipoCriteriRicercaE002Dto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressANPRRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@Slf4j
public class AnprService{

    private final AddressAnprConverter addressAnprConverter;
    private final AnprClient anprClient;
    private final E002ServiceApiCustom e002ServiceApiCustom;
    private final ObjectMapper mapper;

    public AnprService(AddressAnprConverter addressAnprConverter,
                       AnprClient anprClient,
                       E002ServiceApiCustom e002ServiceApiCustom,
                       ObjectMapper mapper) {
        this.anprClient = anprClient;
        this.addressAnprConverter = addressAnprConverter;
        this.e002ServiceApiCustom = e002ServiceApiCustom;
        this.mapper = mapper;
    }

    public Mono<GetAddressANPROKDto> getAddressANPR(GetAddressANPRRequestBodyDto request) {
        RichiestaE002Dto richiestaE002Dto = createRequest(request);
        return anprClient.getApiClient(createDigestFromPayload(richiestaE002Dto)).flatMap(apiClient -> {
            e002ServiceApiCustom.setApiClient(apiClient);
            return callEService(e002ServiceApiCustom,richiestaE002Dto);
        }).retryWhen(Retry.max(1).filter(this::checkExceptionType)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> new AnprException(retrySignal.failure())));
    }

    private String createDigestFromPayload(RichiestaE002Dto request)  {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String requestBody = mapper.writeValueAsString(request);
            byte[] digestByte = md.digest(requestBody.getBytes(StandardCharsets.UTF_8));
            return "SHA-256="+Base64.getEncoder().encodeToString(digestByte);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            log.error("");
            throw new AnprException(e);
        }
    }

    private Mono<GetAddressANPROKDto> callEService(E002ServiceApiCustom e002ServiceApi, RichiestaE002Dto request) {
        return (e002ServiceApi.e002(request)
                .map(rispostaE002OKDto -> addressAnprConverter.convertToGetAddressANPROKDto(rispostaE002OKDto, request.getCriteriRicerca().getCodiceFiscale())));
    }

    public RichiestaE002Dto createRequest(GetAddressANPRRequestBodyDto request) {
        RichiestaE002Dto richiesta = new RichiestaE002Dto();
        TipoCriteriRicercaE002Dto criteriRicercaE002Dto = new TipoCriteriRicercaE002Dto();
        criteriRicercaE002Dto.setCodiceFiscale(request.getFilter().getTaxId());
        richiesta.setCriteriRicerca(criteriRicercaE002Dto);
        return richiesta;
    }

    private boolean checkExceptionType(Throwable throwable){
        if(throwable instanceof WebClientResponseException){
            WebClientResponseException exception = (WebClientResponseException) throwable;
            return exception.getStatusCode()==HttpStatus.UNAUTHORIZED;
        }
        return false;
    }
}
