package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.exceptions.RuntimeJAXBException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.dto.Richiesta;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.*;

@Component
@lombok.CustomLog
@RequiredArgsConstructor
public class AgenziaEntrateService {

    private final AgenziaEntrateConverter agenziaEntrateConverter;
    private final AdELegalClient adELegalClient;
    private final ValidateTaxIdUtils validateTaxIdUtils;
    private final ValidateUtils validateUtils;
    private final CheckCfClient checkCfClient;

    public Mono<CheckTaxIdOKDto> callEService(CheckTaxIdRequestBodyDto request) {
        log.logChecking(PROCESS_CHECKING_AGENZIA_ENTRATE_CHECK_TAX_ID);
        String cf = request.getFilter().getTaxId();
        if (!validateUtils.taxIdIsInWhiteList(cf)) {
            validateTaxIdUtils.validateTaxId(cf, PROCESS_NAME_AGENZIA_ENTRATE_CHECK_TAX_ID, true);

            return checkCfClient.callEService(createRequest(request))
                    .doOnNext(taxIdVerification -> log.logCheckingOutcome(PROCESS_CHECKING_AGENZIA_ENTRATE_CHECK_TAX_ID, true))
                    .doOnError(throwable -> log.logCheckingOutcome(PROCESS_CHECKING_AGENZIA_ENTRATE_CHECK_TAX_ID, false, throwable.getMessage()))
                    .map(agenziaEntrateConverter::convertToCfStatusDto);
        } else {
            log.logCheckingOutcome(PROCESS_CHECKING_AGENZIA_ENTRATE_CHECK_TAX_ID, true);
            return Mono.just(new CheckTaxIdOKDto().taxId(cf).isValid(true));
        }
    }

    private Richiesta createRequest(CheckTaxIdRequestBodyDto taxCodeRequestDto) {
        Richiesta richiesta = new Richiesta();
        richiesta.setCodiceFiscale(taxCodeRequestDto.getFilter().getTaxId());
        return richiesta;
    }

    public Mono<ADELegalOKDto> checkTaxIdAndVatNumber(ADELegalRequestBodyDto request) {
        log.logChecking(PROCESS_CHECKING_AGENZIAN_ENTRATE_LEGAL);

        validateTaxIdUtils.validateTaxId(request.getFilter().getTaxId(), PROCESS_NAME_AGENZIA_ENTRATE_LEGAL, false);
        validateTaxIdUtils.validateTaxId(request.getFilter().getVatNumber(), PROCESS_NAME_AGENZIA_ENTRATE_LEGAL, false);

        return adELegalClient.checkTaxIdAndVatNumberAdE(request.getFilter())
                .map(response -> {
                    try {
                        CheckValidityRappresentanteResp checkValidityRappresentanteResp = unmarshaller(response);
                        log.logCheckingOutcome(PROCESS_CHECKING_AGENZIAN_ENTRATE_LEGAL, true);
                        return agenziaEntrateConverter.adELegalResponseToDto(checkValidityRappresentanteResp);
                    } catch (JAXBException e) {
                        log.logCheckingOutcome(PROCESS_CHECKING_AGENZIAN_ENTRATE_LEGAL, false, e.getMessage());
                        throw new RuntimeJAXBException(e.getMessage());
                    }
                });
    }


    public CheckValidityRappresentanteResp unmarshaller(String response) throws JAXBException {

        JAXBContext jaxbContext = JAXBContext.newInstance(CheckValidityRappresentanteResp.class);
        String responseBody = extractResponseBody(response);

        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        return (CheckValidityRappresentanteResp) jaxbUnmarshaller.unmarshal(new StringReader(responseBody));
    }

    private String extractResponseBody(String xmlResponse) {
        Pattern responsePattern = Pattern.compile(".*Body\\>(.*)<\\/soapenv:Body>");
        String checkValidityElement = "";
        Matcher matcher = responsePattern.matcher(xmlResponse);
        while (matcher.find()) {
            checkValidityElement = matcher.group(1);
        }
        return checkValidityElement;
    }
}
