package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.exceptions.RuntimeJAXBException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.ADELegalRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import it.pagopa.pn.national.registries.model.agenziaentrate.Request;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

@Component
@lombok.CustomLog
public class AgenziaEntrateService {

    private final AgenziaEntrateConverter agenziaEntrateConverter;
    private final CheckCfClient checkCfClient;
    private final AdELegalClient adELegalClient;

    public AgenziaEntrateService(AgenziaEntrateConverter agenziaEntrateConverter,
                                 CheckCfClient checkCfClient,
                                 AdELegalClient adELegalClient) {
        this.checkCfClient = checkCfClient;
        this.agenziaEntrateConverter = agenziaEntrateConverter;
        this.adELegalClient = adELegalClient;
    }

    public Mono<CheckTaxIdOKDto> callEService(CheckTaxIdRequestBodyDto request) {
        return checkCfClient.callEService(createRequest(request))
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
        String process = "validating taxId and vatNumber";
        log.logChecking(process);
        return adELegalClient.checkTaxIdAndVatNumberAdE(request.getFilter())
                .map(response -> {
                    try {
                        CheckValidityRappresentanteResp checkValidityRappresentanteResp = unmarshaller(response);
                        log.logCheckingOutcome(process, true);
                        return agenziaEntrateConverter.adELegalResponseToDto(checkValidityRappresentanteResp);
                    } catch (JAXBException e) {
                        log.logCheckingOutcome(process, false, "invalid format in responseBody");
                        throw new RuntimeJAXBException(e.getMessage());
                    }
                });
    }
}
