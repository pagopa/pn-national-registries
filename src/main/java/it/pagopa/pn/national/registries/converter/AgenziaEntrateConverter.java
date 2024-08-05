package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import it.pagopa.pn.national.registries.model.agenziaentrate.ResultCodeEnum;
import it.pagopa.pn.national.registries.model.agenziaentrate.ResultDetailEnum;
import org.springframework.stereotype.Component;

@Component
public class AgenziaEntrateConverter {

    public ADELegalOKDto adELegalResponseToDto(CheckValidityRappresentanteResp checkValidityRappresentanteResp) {
        ADELegalOKDto adeLegalOKDto = new ADELegalOKDto();
        adeLegalOKDto.setResultCode(ResultCodeEnum.fromValue(checkValidityRappresentanteResp.codiceRitorno));
        adeLegalOKDto.setVerificationResult(checkValidityRappresentanteResp.valido);
        adeLegalOKDto.setResultDetail(ResultDetailEnum.getCode(checkValidityRappresentanteResp.dettaglioEsito));
        adeLegalOKDto.setResultDetailMessage(ResultDetailEnum.getValueFromCode(checkValidityRappresentanteResp.dettaglioEsito));

        return adeLegalOKDto;
    }

}
