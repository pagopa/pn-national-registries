package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.exceptions.RuntimeJAXBException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import it.pagopa.pn.national.registries.model.agenziaentrate.Request;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

import static it.pagopa.pn.national.registries.constant.ProcessStatus.*;

@Component
@lombok.CustomLog
public class AgenziaEntrateService {

    private final AgenziaEntrateConverter agenziaEntrateConverter;
    private final CheckCfClient checkCfClient;
    private final AdELegalClient adELegalClient;
    private final ValidateTaxIdUtils validateTaxIdUtils;

    public AgenziaEntrateService(AgenziaEntrateConverter agenziaEntrateConverter,
                                 CheckCfClient checkCfClient,
                                 AdELegalClient adELegalClient,
                                 ValidateTaxIdUtils validateTaxIdUtils) {
        this.checkCfClient = checkCfClient;
        this.agenziaEntrateConverter = agenziaEntrateConverter;
        this.adELegalClient = adELegalClient;
        this.validateTaxIdUtils = validateTaxIdUtils;
    }

    public Mono<CheckTaxIdOKDto> callEService(CheckTaxIdRequestBodyDto request) {
        log.logChecking(PROCESS_CHECKING_AGENZIA_ENTRATE_CHECK_TAX_ID);
        validateTaxIdUtils.validateTaxId(request.getFilter().getTaxId(), PROCESS_NAME_AGENZIA_ENTRATE_CHECK_TAX_ID);

        return checkCfClient.callEService(createRequest(request))
                .doOnNext(taxIdVerification -> log.logCheckingOutcome(PROCESS_CHECKING_AGENZIA_ENTRATE_CHECK_TAX_ID,true))
                .doOnError(throwable -> log.logCheckingOutcome(PROCESS_CHECKING_AGENZIA_ENTRATE_CHECK_TAX_ID,false,throwable.getMessage()))
                .map(agenziaEntrateConverter::convertToCfStatusDto);
    }

    private Request createRequest(CheckTaxIdRequestBodyDto taxCodeRequestDto) {
        Request richiesta = new Request();
        richiesta.setCodiceFiscale(taxCodeRequestDto.getFilter().getTaxId());
        return richiesta;
    }

    public CheckValidityRappresentanteResp unmarshaller(String response) throws JAXBException {

        JAXBContext jaxbContext = JAXBContext.newInstance(CheckValidityRappresentanteResp.class);
        String responseBody = response.substring(response.indexOf("<checkValidityRappresentanteResp>"), response.indexOf("</checkValidityRappresentanteResp>")
                + "</checkValidityRappresentanteResp>".length());

        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        return (CheckValidityRappresentanteResp) jaxbUnmarshaller.unmarshal(new StringReader(responseBody));
    }

    public Mono<ADELegalOKDto> checkTaxIdAndVatNumber(ADELegalRequestBodyDto request) {
        log.logChecking(PROCESS_CHECKING_AGENZIAN_ENTRATE_LEGAL);

        validateTaxIdUtils.validateTaxId(request.getFilter().getTaxId(), PROCESS_NAME_AGENZIA_ENTRATE_LEGAL);
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
