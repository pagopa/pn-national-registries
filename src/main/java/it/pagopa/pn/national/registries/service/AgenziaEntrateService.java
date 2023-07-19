package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.exceptions.RuntimeJAXBException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import it.pagopa.pn.national.registries.model.agenziaentrate.Request;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.*;

@Component
@lombok.CustomLog
public class AgenziaEntrateService {

    private final AgenziaEntrateConverter agenziaEntrateConverter;
    private final CheckCfClient checkCfClient;
    private final AdELegalClient adELegalClient;
    private final ValidateTaxIdUtils validateTaxIdUtils;
    private final ValidateUtils validateUtils;

    public AgenziaEntrateService(AgenziaEntrateConverter agenziaEntrateConverter,
                                 CheckCfClient checkCfClient,
                                 AdELegalClient adELegalClient,
                                 ValidateTaxIdUtils validateTaxIdUtils, ValidateUtils validateUtils) {
        this.checkCfClient = checkCfClient;
        this.agenziaEntrateConverter = agenziaEntrateConverter;
        this.adELegalClient = adELegalClient;
        this.validateTaxIdUtils = validateTaxIdUtils;
        this.validateUtils = validateUtils;
    }

    public Mono<CheckTaxIdOKDto> callEService(CheckTaxIdRequestBodyDto request) {
        log.logChecking(PROCESS_CHECKING_AGENZIA_ENTRATE_CHECK_TAX_ID);
        String cf = request.getFilter().getTaxId();
        if(!validateUtils.taxIdIsInWhiteList(cf)) {
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

    private Request createRequest(CheckTaxIdRequestBodyDto taxCodeRequestDto) {
        Request richiesta = new Request();
        richiesta.setCodiceFiscale(taxCodeRequestDto.getFilter().getTaxId());
        return richiesta;
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

    public Mono<ADELegalOKDto> checkTaxIdAndVatNumber(ADELegalRequestBodyDto request) {
        log.logChecking(PROCESS_CHECKING_AGENZIAN_ENTRATE_LEGAL);

        validateTaxIdUtils.validateTaxId(request.getFilter().getTaxId(), PROCESS_NAME_AGENZIA_ENTRATE_LEGAL, false);
        validateTaxIdUtils.validateTaxId(request.getFilter().getVatNumber(), PROCESS_NAME_AGENZIA_ENTRATE_LEGAL, false);

        return adELegalClient.checkTaxIdAndVatNumberAdE(request.getFilter())
                .map(response -> {
                    try {
                        CheckValidityRappresentanteResp checkValidityRappresentanteResp = unmarshaller(response);
                        log.logCheckingOutcome(PROCESS_CHECKING_AGENZIAN_ENTRATE_LEGAL,true);
                        return agenziaEntrateConverter.adELegalResponseToDto(checkValidityRappresentanteResp);
                    } catch (JAXBException e) {
                        log.logCheckingOutcome(PROCESS_CHECKING_AGENZIAN_ENTRATE_LEGAL,false,e.getMessage());
                        throw new RuntimeJAXBException(e.getMessage());
                    }
                });
    }
}
