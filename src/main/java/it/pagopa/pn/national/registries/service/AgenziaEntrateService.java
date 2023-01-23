package it.pagopa.pn.national.registries.service;

import ente.rappresentante.verifica.anagrafica.CheckValidityRappresentanteRespType;
import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage.ResponseBody;
import it.pagopa.pn.national.registries.client.agenziaentrate.SOAPMessage.ResponseEnvelope;
import it.pagopa.pn.national.registries.converter.AgenziaEntrateConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdRequestBodyDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.io.StringWriter;

@Component
@Slf4j
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
    public String marshaller(Object object) {
        StringWriter sw = new StringWriter();
        JAXB.marshal(object, sw);
        return sw.toString();
    }

    public CheckValidityRappresentanteRespType getResponseCheckValidityReappresentateRespType(ResponseBody responseBody) {
        return responseBody.getCheckValidityRappresentanteRespType();
    }
    public CheckValidityRappresentanteRespType getResponseBody(ResponseEnvelope responseEnvelope) {
        return getResponseCheckValidityReappresentateRespType(responseEnvelope.getBody());
    }
    public CheckValidityRappresentanteRespType unmarshaller(String string) {
        ResponseEnvelope responseEnvelope = JAXB.unmarshal(new StringReader(string), ResponseEnvelope.class);
        return getResponseBody(responseEnvelope);
    }

    public Mono<ADELegalOKDto> checkTaxIdAndVatNumber(ADELegalRequestBodyDto request) {
        return adELegalClient.checkTaxIdAndVatNumberAdE(agenziaEntrateConverter.toEnvelopeBody(request.getFilter()))
                .map(response -> agenziaEntrateConverter.adELegalResponseToDto(unmarshaller(response)));
    }
}
