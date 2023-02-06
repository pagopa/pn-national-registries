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
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.InfoCamereLegalOKDto;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerificationResponse;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.Pec;
import it.pagopa.pn.national.registries.model.inipec.ResponsePecIniPec;

import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImpreseResponse;
import it.pagopa.pn.national.registries.model.registroimprese.LegalAddress;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class InfoCamereConverter {

    public GetDigitalAddressIniPECOKDto convertToGetAddressIniPecOKDto(BatchRequest requestCorrelation) {
        GetDigitalAddressIniPECOKDto response = new GetDigitalAddressIniPECOKDto();
        checkCorrelationIdAndSetInResponse(requestCorrelation.getCorrelationId(), response);
        return response;
    }

    private void checkCorrelationIdAndSetInResponse(String correlationId, GetDigitalAddressIniPECOKDto response) {
        if (!StringUtils.isNullOrEmpty(correlationId)) {
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
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        List<Pec> pecs = responsePecIniPec.getElencoPec();
        pecs.stream()
                .filter(p -> p.getCf().equalsIgnoreCase(batchRequest.getCf()))
                .findAny()
                .ifPresent(pec -> {
                    codeSqsDto.setCorrelationId(batchRequest.getCorrelationId());
                    codeSqsDto.setTaxId(batchRequest.getCf());
                    codeSqsDto.setDigitalAddress(convertToDigitalAddress(pec));
                });
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
        if (response.getAddress() != null) {
            dto.setAddress(createLegalAddress(response.getAddress()));
            dto.setMunicipality(response.getAddress().getMunicipality());
            dto.setProvince(response.getAddress().getProvince());
            dto.setZip(response.getAddress().getPostalCode());
            dto.setDescription(response.getAddress().getAddress());
        }
        return dto;
    }

    private List<DigitalAddress> convertToDigitalAddress(Pec pec) {
        List<DigitalAddress> digitalAddress = new ArrayList<>();
        if (!StringUtils.isNullOrEmpty(pec.getPecImpresa())) {
            digitalAddress.add(toDigitalAddress(pec.getPecImpresa(), DigitalAddressRecipientType.IMPRESA));
        }
        if (pec.getPecProfessionistas() != null) {
            pec.getPecProfessionistas().stream()
                    .map(pecProf -> toDigitalAddress(pecProf.getPec(), DigitalAddressRecipientType.PROFESSIONISTA))
                    .forEach(digitalAddress::add);
        }
        return digitalAddress;
    }

    private String createLegalAddress(LegalAddress address) {
        return address.getToponym() + " " + address.getStreet() + " " + address.getStreetNumber();
    }

    private DigitalAddress toDigitalAddress(String address, DigitalAddressRecipientType recipient) {
        return new DigitalAddress(DigitalAddressType.PEC.getValue(), address, recipient.getValue());
    }

    public InfoCamereLegalOKDto infoCamereResponseToDto(InfoCamereVerificationResponse response) {
        InfoCamereLegalOKDto infoCamereLegalOKDto = new InfoCamereLegalOKDto();
        infoCamereLegalOKDto.setDateTimeExtraction(new Date());
        infoCamereLegalOKDto.setTaxId(response.getTaxId());
        infoCamereLegalOKDto.setVatNumber(response.getVatNumber());
        infoCamereLegalOKDto.setVerificationResult(response.getVerificationResult());

        return infoCamereLegalOKDto;
    }
}
