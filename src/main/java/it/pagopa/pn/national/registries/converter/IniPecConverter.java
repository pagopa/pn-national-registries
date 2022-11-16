package it.pagopa.pn.national.registries.converter;

import com.amazonaws.util.StringUtils;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import org.springframework.stereotype.Component;

@Component
public class IniPecConverter {
    public GetDigitalAddressIniPECOKDto convertToGetAddressIniPecOKDto(BatchRequest requestCorrelation) {

        GetDigitalAddressIniPECOKDto response = new GetDigitalAddressIniPECOKDto();

        checkCorrelationIdAndSetInResponse(requestCorrelation.getCorrelationId(), response);

        return response;
    }


    private void checkCorrelationIdAndSetInResponse(String correlationId, GetDigitalAddressIniPECOKDto response){
        if(!StringUtils.isNullOrEmpty(correlationId)){
            response.setCorrelationId(correlationId);
        }
    }


}
