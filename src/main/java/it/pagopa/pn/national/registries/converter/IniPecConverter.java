package it.pagopa.pn.national.registries.converter;

import com.amazonaws.util.StringUtils;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DigitalAddressType;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetAddressRegistroImpreseOKProfessionalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECOKDto;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.Pec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;
import it.pagopa.pn.national.registries.model.registroImprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.model.registroImprese.LegalAddress;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

    public CodeSqsDto convertoResponsePecToCodeSqsDto(BatchRequest batchRequest, ResponsePecIniPec responsePecIniPec) {
        List<Pec> pecs = responsePecIniPec.getElencoPec();
        Optional<Pec> opt = pecs.stream().filter(pec1 -> pec1.getCf().equalsIgnoreCase(batchRequest.getCf())).findAny();
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        if (opt.isPresent()) {
            codeSqsDto.setCorrelationId(batchRequest.getCorrelationId());
            codeSqsDto.setTaxId(batchRequest.getCf());
            codeSqsDto.setPrimaryDigitalAddress(toDigitalAddress(opt.get().getPecImpresa(), DigitalAddressRecipientType.IMPRESA));
            ArrayList<DigitalAddress> secondaryDigitalAddresses = opt.get().getPecProfessionistas().stream().map(s -> toDigitalAddress(s, DigitalAddressRecipientType.PROFESSIONISTA)).collect(Collectors.toCollection(ArrayList::new));
            codeSqsDto.setSecondaryDigitalAddresses(secondaryDigitalAddresses);
        }
        return codeSqsDto;
    }

    public GetAddressRegistroImpreseOKDto mapToResponseOk(AddressRegistroImpreseResponse response) {
        GetAddressRegistroImpreseOKDto getAddressRegistroImpreseOKDto = new GetAddressRegistroImpreseOKDto();
        getAddressRegistroImpreseOKDto.setTaxId(response.getTaxId());
        getAddressRegistroImpreseOKDto.setDateTimeExtraction(new Date());
        getAddressRegistroImpreseOKDto.setProfessionalAddress(convertToProfessionalAddressDto(response));
        return getAddressRegistroImpreseOKDto;
    }

    private GetAddressRegistroImpreseOKProfessionalAddressDto convertToProfessionalAddressDto(AddressRegistroImpreseResponse response) {
        GetAddressRegistroImpreseOKProfessionalAddressDto dto = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        if(response.getAddress()!=null) {
            dto.setAddress(createLegalAddress(response.getAddress()));
            dto.setMunicipality(response.getAddress().getMunicipality());
            dto.setProvince(response.getAddress().getProvince());
            dto.setZip(response.getAddress().getPostalCode());
            dto.setDescription(response.getAddress().getAddress());
        }
        return dto;
    }

    private String createLegalAddress(LegalAddress address) {
        return address.getToponym() + " " + address.getStreet() + " " + address.getStreetNumber();
    }

    private DigitalAddress toDigitalAddress(String address, DigitalAddressRecipientType recipient) {
        return new DigitalAddress(DigitalAddressType.PEC.getValue(), address, recipient.getValue());
    }

}
