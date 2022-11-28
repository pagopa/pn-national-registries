package it.pagopa.pn.national.registries.converter;

import com.amazonaws.util.StringUtils;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECOKDto;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.Pec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    public CodeSqsDto convertoResponsePecToCodeSqsDto(BatchRequest batchRequest, ResponsePecIniPec responsePecIniPec){
        List<Pec> pecs = responsePecIniPec.getElencoPec();
        Optional<Pec> opt = pecs.stream().filter(pec1 -> pec1.getCf().equalsIgnoreCase(batchRequest.getCf())).findAny();
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        if(opt.isPresent()) {
            codeSqsDto.setCorrelationId(batchRequest.getCorrelationId());
            codeSqsDto.setStatus("OK");
            codeSqsDto.setCf(opt.get().getCf());
            codeSqsDto.setPecImpresa(opt.get().getPecImpresa());
            codeSqsDto.setPecProfessionista(opt.get().getPecProfessionistas());
        }else {
            if (batchRequest.getRetry() > 3) {
                codeSqsDto.setStatus("KO");
                codeSqsDto.setDescription("Superato il numero massimo di tentativi");
            }
        }
        return codeSqsDto;
    }

}
