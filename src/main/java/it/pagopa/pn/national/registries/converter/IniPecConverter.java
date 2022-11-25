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

    public List<CodeSqsDto> convertoResponsePecToCodeSqsDto(List<BatchRequest> batchRequests, ResponsePecIniPec responsePecIniPec){
        List<CodeSqsDto> codeSqsDtos = new ArrayList<>();
        List<Pec> pecs = responsePecIniPec.getElencoPec();

        for(Pec pec : pecs){
            CodeSqsDto codeSqsDto = new CodeSqsDto();
            String cf = pec.getCf();
            codeSqsDto.setCf(cf);
            if(pec.getPecImpresa()!=null)
                codeSqsDto.setPecImpresa(pec.getPecImpresa());
            if(pec.getPecProfessionistas()!=null){
                codeSqsDto.setPecProfessionista(pec.getPecProfessionistas());
            }
            BatchRequest batchRequest = batchRequests.stream()
                            .filter(batchRequestFounded -> batchRequestFounded.getCf().equals(cf))
                                    .findAny().orElse(null);
            if(batchRequest!=null){
                codeSqsDto.setCorrelationId(batchRequest.getCorrelationId());
                if(batchRequest.getRetry()>3){
                    codeSqsDto.setStatus("KO");
                    codeSqsDto.setDescrption("Retry > 3");
                }else{
                    codeSqsDto.setStatus("OK");
                }
            }
            else{
                codeSqsDto.setStatus("KO");
                codeSqsDto.setDescrption("Correlation id not founded");
            }
            codeSqsDtos.add(codeSqsDto);
        }

        return codeSqsDtos;
    }

}
