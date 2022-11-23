package it.pagopa.pn.national.registries.converter;

import com.amazonaws.util.StringUtils;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.Pec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public BatchPolling createBatchPollingByBatchIdAndPollingId(String batchId, String pollingId){
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setBatchId(batchId);
        batchPolling.setPollingId(pollingId);
        batchPolling.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchPolling.setTimeStamp(LocalDateTime.now());
        return batchPolling;
    }

    public CodeSqsDto convertoResponsePecToCodeSqsDto(ResponsePecIniPec responsePecIniPec, String status, String description){
        CodeSqsDto codeSqsDto = new CodeSqsDto();

        List<Pec> pecs = new ArrayList<>();
        List<String> cfs = new ArrayList<>();
        List<String> pecImpresa = new ArrayList<>();
        List<String> pecProfessionista = new ArrayList<>();

        if(responsePecIniPec.getElencoPec()!=null){
            pecs = responsePecIniPec.getElencoPec();
            cfs = pecs.stream().map(Pec::getCf).collect(Collectors.toList());
            pecImpresa = pecs.stream().map(Pec::getPecImpresa).collect(Collectors.toList());
        }

        for(Pec pec : pecs){
            if(pec.getPecProfessionistas()!=null)
                pecProfessionista.addAll(new ArrayList<>(pec.getPecProfessionistas()));
        }

        codeSqsDto.setCfs(cfs);
        codeSqsDto.setPecImpresa(pecImpresa);
        codeSqsDto.setPecProfessionista(pecProfessionista);
        codeSqsDto.setStatus(status);
        codeSqsDto.setDescrption(description);
        codeSqsDto.setCorrelationId(responsePecIniPec.getIdentificativoRichiesta());
        return codeSqsDto;
    }

}
