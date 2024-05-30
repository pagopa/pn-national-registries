package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.exceptions.RuntimeJAXBException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalRequestBodyDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_CHECKING_AGENZIAN_ENTRATE_LEGAL;
import static it.pagopa.pn.national.registries.constant.ProcessStatus.PROCESS_NAME_AGENZIA_ENTRATE_LEGAL;

@Component
@lombok.CustomLog
public class AgenziaEntrateService {

    private final AgenziaEntrateConverter agenziaEntrateConverter;
    private final AdELegalClient adELegalClient;
    private final ValidateTaxIdUtils validateTaxIdUtils;

    public AgenziaEntrateService(AgenziaEntrateConverter agenziaEntrateConverter,
                                 AdELegalClient adELegalClient,
                                 ValidateTaxIdUtils validateTaxIdUtils) {
        this.agenziaEntrateConverter = agenziaEntrateConverter;
        this.adELegalClient = adELegalClient;
        this.validateTaxIdUtils = validateTaxIdUtils;
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
