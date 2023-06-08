package it.pagopa.pn.national.registries.converter;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DigitalAddressType;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.exceptions.IniPecException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereCommonError;
import it.pagopa.pn.national.registries.model.infocamere.InfoCamereVerification;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.Pec;
import it.pagopa.pn.national.registries.model.inipec.IniPecPollingResponse;

import it.pagopa.pn.national.registries.model.registroimprese.AddressRegistroImprese;
import it.pagopa.pn.national.registries.model.registroimprese.LegalAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class InfoCamereConverter {

    private final ObjectMapper mapper;
    private final long iniPecTtl;

    private static final String PEC_REQUEST_IN_PROGRESS_MESSAGE = "List PEC in progress";

    public InfoCamereConverter(ObjectMapper mapper,
                               @Value("${pn.national.registries.inipec.ttl}") long iniPecTtl) {
        this.mapper = mapper;
        this.iniPecTtl = iniPecTtl;
    }

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

    public BatchPolling createBatchPollingByBatchIdAndPollingId(String batchId, String pollingId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setBatchId(batchId);
        batchPolling.setPollingId(pollingId);
        batchPolling.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchPolling.setRetry(0);
        batchPolling.setCreatedAt(now);
        batchPolling.setTtl(now.plusSeconds(iniPecTtl).toEpochSecond(ZoneOffset.UTC));
        return batchPolling;
    }

    public CodeSqsDto convertResponsePecToCodeSqsDto(BatchRequest batchRequest, IniPecPollingResponse iniPecPollingResponse) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(batchRequest.getCorrelationId());
        List<Pec> pecs = iniPecPollingResponse.getElencoPec();
        pecs.stream()
                .filter(p -> p.getCf().equalsIgnoreCase(batchRequest.getCf()))
                .findAny()
                .ifPresentOrElse(
                        pec -> codeSqsDto.setDigitalAddress(convertToDigitalAddress(pec)),
                        () -> codeSqsDto.setDigitalAddress(Collections.emptyList()));
        return codeSqsDto;
    }

    public CodeSqsDto convertIniPecRequestToSqsDto(BatchRequest request, @Nullable String error) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(request.getCorrelationId());
        if (error != null) {
            codeSqsDto.setError(error);
        } else {
            codeSqsDto.setDigitalAddress(Collections.emptyList());
        }
        return codeSqsDto;
    }

    public String convertCodeSqsDtoToString(CodeSqsDto codeSqsDto) {
        try {
            return mapper.writeValueAsString(codeSqsDto);
        } catch (JsonProcessingException e) {
            throw new IniPecException("can not convert SQS DTO to String", e);
        }
    }

    public boolean checkIfResponseIsInfoCamereError(InfoCamereCommonError response) {
        return (response.getCode() != null && !"".equals(response.getCode())
                || response.getDescription() != null && !"".equals(response.getDescription())
                || response.getTimestamp() != null && !"".equals(response.getTimestamp())
                || response.getAppName() != null && !"".equals(response.getAppName())
        );
    }

    public GetAddressRegistroImpreseOKDto mapToResponseOkByResponse(AddressRegistroImprese response) {
        GetAddressRegistroImpreseOKDto getAddressRegistroImpreseOKDto = new GetAddressRegistroImpreseOKDto();
        getAddressRegistroImpreseOKDto.setTaxId(response.getTaxId());
        getAddressRegistroImpreseOKDto.setDateTimeExtraction(new Date());
        getAddressRegistroImpreseOKDto.setProfessionalAddress(convertToProfessionalAddressDto(response));
        return getAddressRegistroImpreseOKDto;
    }

    public GetAddressRegistroImpreseOKDto mapToResponseOkByRequest(GetAddressRegistroImpreseRequestBodyDto request) {
        GetAddressRegistroImpreseOKDto getAddressRegistroImpreseOKDto = new GetAddressRegistroImpreseOKDto();
        getAddressRegistroImpreseOKDto.setTaxId(request.getFilter().getTaxId());
        getAddressRegistroImpreseOKDto.setDateTimeExtraction(new Date());
        getAddressRegistroImpreseOKDto.setProfessionalAddress(new GetAddressRegistroImpreseOKProfessionalAddressDto());
        return getAddressRegistroImpreseOKDto;
    }

    private GetAddressRegistroImpreseOKProfessionalAddressDto convertToProfessionalAddressDto(AddressRegistroImprese response) {
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
        if (pec.getPecProfessionista() != null) {
            pec.getPecProfessionista().stream()
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

    public InfoCamereLegalOKDto infoCamereResponseToDtoByResponse(InfoCamereVerification response) {
        InfoCamereLegalOKDto infoCamereLegalOKDto = new InfoCamereLegalOKDto();
        infoCamereLegalOKDto.setDateTimeExtraction(new Date());
        infoCamereLegalOKDto.setTaxId(response.getTaxId());
        infoCamereLegalOKDto.setVatNumber(response.getVatNumber());
        infoCamereLegalOKDto.setVerificationResult("OK".equalsIgnoreCase(response.getVerificationResult()));

        return infoCamereLegalOKDto;
    }

    public InfoCamereLegalOKDto infoCamereResponseToDtoByRequest(InfoCamereLegalRequestBodyDto request) {
        InfoCamereLegalOKDto infoCamereLegalOKDto = new InfoCamereLegalOKDto();
        infoCamereLegalOKDto.setDateTimeExtraction(new Date());
        infoCamereLegalOKDto.setTaxId(request.getFilter().getTaxId());
        infoCamereLegalOKDto.setVatNumber(request.getFilter().getVatNumber());
        infoCamereLegalOKDto.setVerificationResult(false);

        return infoCamereLegalOKDto;
    }

    public boolean checkListPecInProgress(IniPecPollingResponse response) {
        return response.getCode().equalsIgnoreCase("WSPA_ERR_05") && response.getDescription().startsWith(PEC_REQUEST_IN_PROGRESS_MESSAGE);
    }
}
