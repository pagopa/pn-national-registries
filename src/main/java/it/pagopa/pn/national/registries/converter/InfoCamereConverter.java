package it.pagopa.pn.national.registries.converter;

import com.amazonaws.util.CollectionUtils;
import com.amazonaws.util.StringUtils;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DigitalAddressType;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Component
public class InfoCamereConverter {
    private final long iniPecTtl;
    private final String batchRequestPkSeparator;
    
    public InfoCamereConverter(@Value("${pn.national.registries.inipec.ttl}") long iniPecTtl,
                               @Value("${pn.national.registries.inipec.batchrequest.pk.separator}") String batchRequestPkSeparator) {
        this.iniPecTtl = iniPecTtl;
        this.batchRequestPkSeparator = batchRequestPkSeparator;
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
        batchPolling.setInProgressRetry(0);
        batchPolling.setCreatedAt(now);
        batchPolling.setTtl(now.plusSeconds(iniPecTtl).toEpochSecond(ZoneOffset.UTC));
        return batchPolling;
    }

    public CodeSqsDto convertResponsePecToCodeSqsDto(BatchRequest batchRequest, IniPecPollingResponse iniPecPollingResponse) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(batchRequest.getCorrelationId().split(batchRequestPkSeparator)[0]);
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
        codeSqsDto.setCorrelationId(request.getCorrelationId().split(batchRequestPkSeparator)[0]);
        if (error != null) {
            codeSqsDto.setError(error);
        } else {
            codeSqsDto.setDigitalAddress(Collections.emptyList());
        }
        return codeSqsDto;
    }

    public boolean checkIfResponseIsInfoCamereError(IniPecPollingResponse response) {
        return org.springframework.util.StringUtils.hasText(response.getCode())
                || org.springframework.util.StringUtils.hasText(response.getDescription())
                || response.getTimestamp() != null
                || org.springframework.util.StringUtils.hasText(response.getAppName());
    }

    public boolean checkIfResponseIsInfoCamereError(AddressRegistroImprese response) {
        return org.springframework.util.StringUtils.hasText(response.getCode())
                || org.springframework.util.StringUtils.hasText(response.getDescription())
                || response.getTimestamp() != null
                || org.springframework.util.StringUtils.hasText(response.getAppName());
    }

    public boolean checkIfResponseIsInfoCamereError(InfoCamereVerification response) {
        return org.springframework.util.StringUtils.hasText(response.getCode())
                || org.springframework.util.StringUtils.hasText(response.getDescription())
                || response.getTimestamp() != null
                || org.springframework.util.StringUtils.hasText(response.getAppName());
    }

    public GetAddressRegistroImpreseOKDto mapToResponseOkByResponse(AddressRegistroImprese response) {
        GetAddressRegistroImpreseOKDto getAddressRegistroImpreseOKDto = new GetAddressRegistroImpreseOKDto();
        getAddressRegistroImpreseOKDto.setTaxId(response.getCf());
        getAddressRegistroImpreseOKDto.setDateTimeExtraction(new Date());
        getAddressRegistroImpreseOKDto.setProfessionalAddress(convertToProfessionalAddressDto(response));
        return getAddressRegistroImpreseOKDto;
    }

    public GetAddressRegistroImpreseOKDto mapToResponseOkByRequest(GetAddressRegistroImpreseRequestBodyDto request) {
        GetAddressRegistroImpreseOKDto getAddressRegistroImpreseOKDto = new GetAddressRegistroImpreseOKDto();
        getAddressRegistroImpreseOKDto.setTaxId(request.getFilter().getTaxId());
        getAddressRegistroImpreseOKDto.setDateTimeExtraction(new Date());
        return getAddressRegistroImpreseOKDto;
    }

    public InfoCamereLegalInstitutionsOKDto mapToResponseOkByResponse(InfoCamereLegalInstituionsResponse response) {
        InfoCamereLegalInstitutionsOKDto infoCamereLegalInstitutions = new InfoCamereLegalInstitutionsOKDto();
        infoCamereLegalInstitutions.setLegalTaxId(response.getCfPersona());
        infoCamereLegalInstitutions.setDateTimeExtraction(response.getDataOraEstrazione());
        infoCamereLegalInstitutions.setBusinessList(convertToBusiness(response));
        infoCamereLegalInstitutions.setDescription(response.getDescription());
        infoCamereLegalInstitutions.setCode(response.getCode());
        infoCamereLegalInstitutions.setAppName(response.getAppName());
        infoCamereLegalInstitutions.setTimestamp(Objects.toString(response.getTimestamp()));
        return infoCamereLegalInstitutions;
    }

    private List<BusinessDto> convertToBusiness(InfoCamereLegalInstituionsResponse response) {
        if(!CollectionUtils.isNullOrEmpty(response.getElencoImpreseRappresentate())) {
            return response.getElencoImpreseRappresentate().stream()
                    .map(infoCamereInstitution -> {
                        BusinessDto businessDto = new BusinessDto();
                        businessDto.setBusinessName(infoCamereInstitution.getDenominazione());
                        businessDto.setBusinessTaxId(infoCamereInstitution.getCfImpresa());
                        return businessDto;
                    })
                    .toList();
        }
        return Collections.emptyList();
    }


    private GetAddressRegistroImpreseOKProfessionalAddressDto convertToProfessionalAddressDto(AddressRegistroImprese response) {
        GetAddressRegistroImpreseOKProfessionalAddressDto dto = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        LegalAddress address = response.getIndirizzoLocalizzazione();
        if (Objects.nonNull(address)) {
            dto.setAddress(createLegalAddress(address));
            dto.setMunicipality(address.getComune());
            dto.setProvince(address.getProvincia());
            dto.setZip(address.getCap());
            dto.setDescription(address.getVia());
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
        return address.getToponimo() + " " + address.getVia() + " " + address.getnCivico();
    }

    private DigitalAddress toDigitalAddress(String address, DigitalAddressRecipientType recipient) {
        return new DigitalAddress(DigitalAddressType.PEC.getValue(), address, recipient.getValue());
    }

    public InfoCamereLegalOKDto infoCamereResponseToDtoByResponse(InfoCamereVerification response) {
        InfoCamereLegalOKDto infoCamereLegalOKDto = new InfoCamereLegalOKDto();
        infoCamereLegalOKDto.setDateTimeExtraction(new Date());
        infoCamereLegalOKDto.setTaxId(response.getCfPersona());
        infoCamereLegalOKDto.setVatNumber(response.getCfImpresa());
        infoCamereLegalOKDto.setVerificationResult("OK".equalsIgnoreCase(response.getEsitoVerifica()));

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
}
