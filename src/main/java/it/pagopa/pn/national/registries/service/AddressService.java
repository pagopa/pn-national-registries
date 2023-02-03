package it.pagopa.pn.national.registries.service;

import com.amazonaws.util.StringUtils;
import it.pagopa.pn.commons.log.MDCWebFilter;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.constant.DigitalAddressRecipientType;
import it.pagopa.pn.national.registries.constant.DigitalAddressType;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import it.pagopa.pn.national.registries.model.inipec.CodeSqsDto;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.PhysicalAddress;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AddressService {

    private final AnprService anprService;
    private final InadService inadService;
    private final InfoCamereService infoCamereService;
    private final SqsService sqsService;
    private final boolean pnNationalRegistriesCxIdFlag;

    public AddressService(AnprService anprService,
                          InadService inadService,
                          InfoCamereService infoCamereService,
                          SqsService sqsService,
                          @Value("${pn.national.registries.val.cx.id.enabled}") boolean pnNationalRegistriesCxIdFlag) {
        this.anprService = anprService;
        this.inadService = inadService;
        this.infoCamereService = infoCamereService;
        this.sqsService = sqsService;
        this.pnNationalRegistriesCxIdFlag = pnNationalRegistriesCxIdFlag;
    }

    public Mono<AddressOKDto> retrieveDigitalOrPhysicalAddressAsync(String recipientType, String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        checkFlagPnNationalRegistriesCxId(pnNationalRegistriesCxId);

        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();
        String cf = addressRequestBodyDto.getFilter().getTaxId();

        Map<String, String> copyOfContext = MDCUtils.retrieveMDCContextMap();

        Sinks.One<Tuple3<String, String, AddressRequestBodyDto>> sink = Sinks.one();

        sink.asMono()
                .flatMap(t -> retrieveDigitalOrPhysicalAddress(t.getT1(), t.getT2(), t.getT3()))
                .contextWrite(ctx -> enrichFluxContext(ctx, copyOfContext))
                .subscribe();

        var emitResult = sink.tryEmitValue(Tuples.of(recipientType, pnNationalRegistriesCxId != null ? pnNationalRegistriesCxId : "", addressRequestBodyDto));
        if (emitResult != Sinks.EmitResult.OK) {
            log.error("can not submit task: {}", emitResult);
            sqsService.push(errorToSqsDto(correlationId, cf, "can not submit task"), pnNationalRegistriesCxId)
                    .subscribe(ok -> {
                    }, e -> log.error("can not send message to SQS queue", e));
        }

        return Mono.just(mapToAddressesOKDto(correlationId));
    }

    public Mono<AddressOKDto> retrieveDigitalOrPhysicalAddress(String recipientType, String pnNationalRegistriesCxId, AddressRequestBodyDto addressRequestBodyDto) {
        String correlationId = addressRequestBodyDto.getFilter().getCorrelationId();
        String cf = addressRequestBodyDto.getFilter().getTaxId();
        log.info("recipientType {} and domicileType {}", recipientType, addressRequestBodyDto.getFilter().getDomicileType());
        switch (recipientType) {
            case "PF" -> {
                if (AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL.equals(addressRequestBodyDto.getFilter().getDomicileType())) {
                    return anprService.getAddressANPR(convertToGetAddressAnprRequest(addressRequestBodyDto))
                            .flatMap(anprResponse -> sqsService.push(anprToSqsDto(correlationId, cf, anprResponse), pnNationalRegistriesCxId)
                                    .map(sqs -> mapToAddressesOKDto(correlationId)))
                            .onErrorResume(e -> sqsService.push(errorToSqsDto(correlationId, cf, e.getMessage()), pnNationalRegistriesCxId)
                                    .map(sendMessageResponse -> mapToAddressesOKDto(correlationId)));
                } else {
                    return inadService.callEService(convertToGetDigitalAddressInadRequest(addressRequestBodyDto))
                            .flatMap(inadResponse -> sqsService.push(inadToSqsDto(correlationId, cf, inadResponse), pnNationalRegistriesCxId)
                                    .map(sqs -> mapToAddressesOKDto(correlationId)))
                            .onErrorResume(e -> sqsService.push(errorInadToSqsDto(correlationId, cf, e), pnNationalRegistriesCxId)
                                    .map(sendMessageResponse -> mapToAddressesOKDto(correlationId)));
                }
            }
            case "PG" -> {
                if (addressRequestBodyDto.getFilter().getDomicileType().equals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL)) {
                    return infoCamereService.getRegistroImpreseLegalAddress(convertToGetAddressRegistroImpreseRequest(addressRequestBodyDto))
                            .flatMap(registroImpreseResponse -> sqsService.push(regImpToSqsDto(correlationId, cf, registroImpreseResponse), pnNationalRegistriesCxId)
                                    .map(sqs -> mapToAddressesOKDto(correlationId)))
                            .onErrorResume(e -> sqsService.push(errorInfoCamereToSqsDto(correlationId, cf, e), pnNationalRegistriesCxId)
                                    .map(sendMessageResponse -> mapToAddressesOKDto(correlationId)));
                } else {
                    return infoCamereService.getIniPecDigitalAddress(pnNationalRegistriesCxId, convertToGetDigitalAddressIniPecRequest(addressRequestBodyDto))
                            .map(iniPecResponse -> mapToAddressesOKDto(correlationId));
                }
            }
            default -> {
                log.warn("recipientType {} is not valid", recipientType);
                throw new PnNationalRegistriesException("recipientType not valid", HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class);
            }
        }
    }

    private void checkFlagPnNationalRegistriesCxId(String pnNationalRegistriesCxId) {
        if (pnNationalRegistriesCxIdFlag && pnNationalRegistriesCxId == null) {
            throw new PnNationalRegistriesException("pnNationalRegistriesCxId required", HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, Charset.defaultCharset(), AddressErrorDto.class);
        }
    }

    private CodeSqsDto errorToSqsDto(String correlationId, String cf, String error) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(correlationId);
        codeSqsDto.setTaxId(cf);
        codeSqsDto.setError(error);
        return codeSqsDto;
    }

    private CodeSqsDto errorInadToSqsDto(String correlationId, String cf, Throwable error) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(correlationId);
        codeSqsDto.setTaxId(cf);
        codeSqsDto.setError(error.getMessage());
        if (error instanceof PnNationalRegistriesException exception && exception.getStatusCode().value() == 404 && !StringUtils.isNullOrEmpty(exception.getResponseBodyAsString()) && exception.getResponseBodyAsString().toUpperCase().contains("CF NON TROVATO")) {
            codeSqsDto.setError(null);
        }
        return codeSqsDto;
    }

    private CodeSqsDto errorInfoCamereToSqsDto(String correlationId, String cf, Throwable error) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(correlationId);
        codeSqsDto.setTaxId(cf);
        codeSqsDto.setError(error.getMessage());
        if (error instanceof PnNationalRegistriesException exception && exception.getStatusCode().value() == 404 && StringUtils.isNullOrEmpty(exception.getResponseBodyAsString())) {
            codeSqsDto.setError(null);
        }
        return codeSqsDto;
    }

    private AddressOKDto mapToAddressesOKDto(String correlationId) {
        AddressOKDto dto = new AddressOKDto();
        dto.setCorrelationId(correlationId);
        return dto;
    }

    private CodeSqsDto anprToSqsDto(String correlationId, String cf, GetAddressANPROKDto anprResponse) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(correlationId);
        if (anprResponse != null && !CollectionUtils.isEmpty(anprResponse.getResidentialAddresses())) {
            codeSqsDto.setPhysicalAddress(convertAnprToPhysicalAddress(anprResponse.getResidentialAddresses().get(0)));
        }
        codeSqsDto.setTaxId(cf);
        return codeSqsDto;
    }

    private CodeSqsDto inadToSqsDto(String correlationId, String cf, GetDigitalAddressINADOKDto inadDto) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(correlationId);
        if (inadDto != null && inadDto.getDigitalAddress() != null) {
            List<DigitalAddress> address = inadDto.getDigitalAddress().stream()
                    .map(this::convertInadToDigitalAddress)
                    .toList();
            codeSqsDto.setDigitalAddress(address);
        }
        codeSqsDto.setTaxId(cf);
        return codeSqsDto;
    }

    private CodeSqsDto regImpToSqsDto(String correlationId, String cf, GetAddressRegistroImpreseOKDto registroImpreseDto) {
        CodeSqsDto codeSqsDto = new CodeSqsDto();
        codeSqsDto.setCorrelationId(correlationId);
        if (registroImpreseDto.getProfessionalAddress() != null) {
            codeSqsDto.setPhysicalAddress(convertRegImpToPhysicalAddress(registroImpreseDto.getProfessionalAddress()));
        }
        codeSqsDto.setTaxId(cf);
        return codeSqsDto;
    }

    private PhysicalAddress convertAnprToPhysicalAddress(ResidentialAddressDto residenceDto) {
        PhysicalAddress physicalAddress = new PhysicalAddress();
        physicalAddress.setAddress(residenceDto.getAddress());
        physicalAddress.setAt(residenceDto.getAt());
        physicalAddress.setZip(residenceDto.getZip());
        physicalAddress.setMunicipality(residenceDto.getMunicipality());
        physicalAddress.setProvince(residenceDto.getProvince());
        physicalAddress.setForeignState(residenceDto.getForeignState());
        physicalAddress.setMunicipalityDetails(residenceDto.getMunicipalityDetails());
        physicalAddress.setMunicipality(residenceDto.getMunicipality());
        physicalAddress.setProvince(residenceDto.getProvince());
        return physicalAddress;
    }

    private PhysicalAddress convertRegImpToPhysicalAddress(GetAddressRegistroImpreseOKProfessionalAddressDto addressDto) {
        PhysicalAddress physicalAddress = new PhysicalAddress();
        physicalAddress.setProvince(addressDto.getProvince());
        physicalAddress.setAddress(addressDto.getAddress());
        physicalAddress.setMunicipality(addressDto.getMunicipality());
        physicalAddress.setZip(addressDto.getZip());
        return physicalAddress;
    }

    private DigitalAddress convertInadToDigitalAddress(DigitalAddressDto digitalAddressDto) {
        return new DigitalAddress(DigitalAddressType.PEC.getValue(), digitalAddressDto.getDigitalAddress(), DigitalAddressRecipientType.PERSONA_FISICA.getValue());
    }

    private GetDigitalAddressIniPECRequestBodyDto convertToGetDigitalAddressIniPecRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetDigitalAddressIniPECRequestBodyDto dto = new GetDigitalAddressIniPECRequestBodyDto();
        GetDigitalAddressIniPECRequestBodyFilterDto filterDto = new GetDigitalAddressIniPECRequestBodyFilterDto();

        filterDto.setCorrelationId(addressRequestBodyDto.getFilter().getCorrelationId());
        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());

        dto.setFilter(filterDto);
        return dto;
    }

    private GetAddressRegistroImpreseRequestBodyDto convertToGetAddressRegistroImpreseRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetAddressRegistroImpreseRequestBodyDto dto = new GetAddressRegistroImpreseRequestBodyDto();
        GetAddressRegistroImpreseRequestBodyFilterDto filterDto = new GetAddressRegistroImpreseRequestBodyFilterDto();

        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());

        dto.setFilter(filterDto);
        return dto;
    }

    private GetDigitalAddressINADRequestBodyDto convertToGetDigitalAddressInadRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetDigitalAddressINADRequestBodyDto dto = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();

        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());
        filterDto.setPracticalReference(addressRequestBodyDto.getFilter().getCorrelationId());

        dto.setFilter(filterDto);
        return dto;
    }

    private GetAddressANPRRequestBodyDto convertToGetAddressAnprRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetAddressANPRRequestBodyDto dto = new GetAddressANPRRequestBodyDto();
        GetAddressANPRRequestBodyFilterDto filterDto = new GetAddressANPRRequestBodyFilterDto();

        filterDto.setRequestReason(addressRequestBodyDto.getFilter().getCorrelationId());
        filterDto.setTaxId(addressRequestBodyDto.getFilter().getTaxId());
        filterDto.setReferenceRequestDate(addressRequestBodyDto.getFilter().getReferenceRequestDate());

        dto.setFilter(filterDto);
        return dto;
    }

    private Context enrichFluxContext(Context ctx, Map<String, String> mdcCtx) {
        if (mdcCtx != null) {
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_TRACE_ID_KEY, mdcCtx.get(MDCWebFilter.MDC_TRACE_ID_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_JTI_KEY, mdcCtx.get(MDCWebFilter.MDC_JTI_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_PN_UID_KEY, mdcCtx.get(MDCWebFilter.MDC_PN_UID_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_CX_ID_KEY, mdcCtx.get(MDCWebFilter.MDC_CX_ID_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_PN_CX_TYPE_KEY, mdcCtx.get(MDCWebFilter.MDC_PN_CX_TYPE_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_PN_CX_GROUPS_KEY, mdcCtx.get(MDCWebFilter.MDC_PN_CX_GROUPS_KEY));
            ctx = addToFluxContext(ctx, MDCWebFilter.MDC_PN_CX_ROLE_KEY, mdcCtx.get(MDCWebFilter.MDC_PN_CX_ROLE_KEY));
        }
        return ctx;
    }

    private Context addToFluxContext(Context ctx, String key, String value) {
        if (value != null) {
            ctx = ctx.put(key, value);
        }
        return ctx;
    }
}
