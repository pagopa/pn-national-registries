package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.config.NationalRegistriesConfig;
import it.pagopa.pn.national.registries.constant.BatchSendStatus;
import it.pagopa.pn.national.registries.constant.BatchStatus;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DigitalAddressType;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.gateway.GatewayDownstreamService;
import it.pagopa.pn.national.registries.utils.GatewayUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static it.pagopa.pn.national.registries.utils.DigitalAddressUtils.removeInvalidEmails;

@Component
@RequiredArgsConstructor
public class InfoCamereConverter{

    private final NationalRegistriesConfig nationalRegistriesConfig;
    private final GatewayUtils gatewayUtils;

    public Mono<BatchRequest> buildErrorBatchRequest(BatchStatus status, String error, BatchRequest batchRequest, LocalDateTime now) {
        AddressSQSMessageDto sqsDto = convertIniPecRequestToSqsDto(batchRequest, error);
        populateBatchRequestSendFields(batchRequest, status, now, sqsDto);
        return Mono.just(batchRequest);
    }

    public Mono<BatchRequest> updateBatchRequestFields(BatchRequest batchRequest, BatchStatus status, LocalDateTime now, Pec pec) {
        AddressSQSMessageDto codeSqsDto = convertResponsePecToCodeSqsDto(batchRequest, pec);
        populateBatchRequestSendFields(batchRequest, status, now, codeSqsDto);
        return Mono.just(batchRequest);
    }

    private void populateBatchRequestSendFields(BatchRequest batchRequest, BatchStatus status, LocalDateTime now, AddressSQSMessageDto codeSqsDto) {
        removeInvalidEmails(codeSqsDto);
        batchRequest.setMessage(gatewayUtils.convertCodeSqsDtoToString(codeSqsDto));
        batchRequest.setEservice(GatewayDownstreamService.INIPEC.name());
        batchRequest.setStatus(status.getValue());
        batchRequest.setSendStatus(BatchSendStatus.NOT_SENT.getValue());
        batchRequest.setLastReserved(now);
    }

    public GetDigitalAddressIniPECOKDto convertToGetAddressIniPecOKDto(BatchRequest requestCorrelation) {
        GetDigitalAddressIniPECOKDto response = new GetDigitalAddressIniPECOKDto();
        checkCorrelationIdAndSetInResponse(requestCorrelation.getCorrelationId(), response);
        return response;
    }

    private void checkCorrelationIdAndSetInResponse(String correlationId, GetDigitalAddressIniPECOKDto response) {
        if (!StringUtils.isEmpty(correlationId)) {
            response.setCorrelationId(correlationId);
        }
    }

    public BatchPolling createBatchPollingByBatchIdAndPollingId(String batchId, String pollingId, Integer iniPecBatchRequestSize) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        BatchPolling batchPolling = new BatchPolling();
        batchPolling.setBatchId(batchId);
        batchPolling.setPollingId(pollingId);
        batchPolling.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchPolling.setRetry(0);
        batchPolling.setInProgressRetry(0);
        batchPolling.setCreatedAt(now);
        batchPolling.setTtl(now.plusSeconds(nationalRegistriesConfig.getInipec().getTtl()).toEpochSecond(ZoneOffset.UTC));
        batchPolling.setBatchSize(iniPecBatchRequestSize);
        batchPolling.setFirstAttemptAfter(calculateFirstAttemptAfter(iniPecBatchRequestSize));
        return batchPolling;
    }

    private Instant calculateFirstAttemptAfter(Integer iniPecBatchRequestSize) {
        double delaySecondsPerCf = nationalRegistriesConfig.getInipec().getFirstAttemptDelaySecondsPerCf();
        int fixedDelaySeconds = nationalRegistriesConfig.getInipec().getFirstAttemptFixedDelaySeconds();
        int batchSize = Optional.ofNullable(iniPecBatchRequestSize).orElse(0);

        long delaySeconds = Math.round((delaySecondsPerCf * batchSize) + fixedDelaySeconds);

        return Instant.now().plusSeconds(delaySeconds).truncatedTo(ChronoUnit.SECONDS);
    }

  public AddressSQSMessageDto convertResponsePecToCodeSqsDto(BatchRequest batchRequest, Pec pec) {
        AddressSQSMessageDto codeSqsDto = new AddressSQSMessageDto();
        codeSqsDto.setRegistry(GatewayDownstreamService.INIPEC.name());
        codeSqsDto.setCorrelationId(batchRequest.getCorrelationId().split(nationalRegistriesConfig.getInipec().getBatchRequestPkSeparator())[0]);
        codeSqsDto.setDigitalAddress(convertToDigitalAddress(pec));
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
        return codeSqsDto;
    }

    public AddressSQSMessageDto convertIniPecRequestToSqsDto(BatchRequest request, @Nullable String error) {
        AddressSQSMessageDto codeSqsDto = new AddressSQSMessageDto();
        codeSqsDto.setCorrelationId(request.getCorrelationId().split(nationalRegistriesConfig.getInipec().getBatchRequestPkSeparator())[0]);
        if (error != null) {
            codeSqsDto.setError(error);
        } else {
            codeSqsDto.setDigitalAddress(Collections.emptyList());
        }
        codeSqsDto.setRegistry(GatewayDownstreamService.INIPEC.name());
        codeSqsDto.setAddressType(AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue());
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
            dto.setStato(address.getStato());
        }
        return dto;
    }

    private List<AddressSQSMessageDigitalAddressInnerDto> convertToDigitalAddress(Pec pec) {
        List<AddressSQSMessageDigitalAddressInnerDto> digitalAddress = new ArrayList<>();
        if (!StringUtils.isEmpty(pec.getPecImpresa())) {
            digitalAddress.add(toDigitalAddress(pec.getPecImpresa(), DigitalAddressRecipientType.IMPRESA, DigitalAddressType.PEC.getValue()));
        }
        if (pec.getPecProfessionista() != null) {
            pec.getPecProfessionista().stream()
                    .map(pecProf -> toDigitalAddress(pecProf.getPec(), DigitalAddressRecipientType.PROFESSIONISTA, DigitalAddressType.PEC.getValue()))
                    .forEach(digitalAddress::add);
        }
        return digitalAddress;
    }

    private AddressSQSMessageDigitalAddressInnerDto toDigitalAddress(String address, DigitalAddressRecipientType recipientType, String type) {
        AddressSQSMessageDigitalAddressInnerDto digitalAddress = new AddressSQSMessageDigitalAddressInnerDto();
        digitalAddress.setAddress(address);
        digitalAddress.setRecipient(AddressSQSMessageDigitalAddressInnerDto.RecipientEnum.fromValue(recipientType.getValue()));
        digitalAddress.setType(type);
        return digitalAddress;
    }


    private String createLegalAddress(LegalAddress address) {
        List<String> addressFields = new ArrayList<>();
        if (address.getToponimo() != null && !address.getToponimo().isEmpty()) {
            addressFields.add(address.getToponimo());
        }
        if (address.getVia() != null && !address.getVia().isEmpty()) {
            addressFields.add(address.getVia());
        }
        if (address.getnCivico() != null && !address.getnCivico().isEmpty()) {
            addressFields.add(address.getnCivico());
        }
        return String.join(" ", addressFields);
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
