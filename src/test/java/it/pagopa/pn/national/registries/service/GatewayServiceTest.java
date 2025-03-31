package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.constant.DomicileType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.InternalRecipientAddress;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressesGatewayEvent;
import it.pagopa.pn.national.registries.model.*;
import it.pagopa.pn.national.registries.utils.FeatureEnabledUtils;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = {
        "pn.national.registries.val.cx.id.enabled=true"
})
@ContextConfiguration(classes = {GatewayService.class})
@ExtendWith(SpringExtension.class)
class GatewayServiceTest {

    @MockBean
    private AnprService anprService;
    @MockBean
    private InadService inadService;
    @MockBean
    private InfoCamereService infoCamereService;
    @MockBean
    private SqsService sqsService;
    @MockBean
    private IpaService ipaService;

    @Autowired
    private GatewayService gatewayService;

    @MockBean
    private ObjectMapper objectMapper;

    @MockBean
    private FeatureEnabledUtils featureEnabledUtils;

    private static final String CF = "CF";
    private static final String C_ID = "correlationId";

    @Test
    void handleMessage(){
        PnAddressGatewayEvent.Payload payload = PnAddressGatewayEvent.Payload.builder()
                .correlationId("correlationId")
                .taxId("taxid")
                .pnNationalRegistriesCxId("cxId")
                .recipientType("PF")
                .domicileType("DIGITAL")
                .referenceRequestDate(new Date())
                .build();

        GetDigitalAddressINADOKDto getDigitalAddressINADOKDto = new GetDigitalAddressINADOKDto();
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("digitalAddress@inad.com");
        getDigitalAddressINADOKDto.setDigitalAddress(digitalAddressDto);

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId("correlationId");
        when(inadService.callEService(any(), any(), any())).thenReturn(Mono.just(getDigitalAddressINADOKDto));
        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), any())).thenReturn(Mono.just(SendMessageResponse.builder().build()));
        StepVerifier.create(gatewayService.handleMessage(payload)).expectNext(addressOKDto).verifyComplete();
    }

    @Test
    @DisplayName("Test recipientType not valid")
    void testCheckFlag() {
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.filter(new AddressRequestBodyFilterDto());
        assertThrows(PnNationalRegistriesException.class,
                () -> gatewayService.retrieveDigitalOrPhysicalAddressAsync("Recipient Type", null, addressRequestBodyDto));
    }

    @Test
    @DisplayName("Test recipientType not valid")
    void testRetrieveDigitalOrPhysicalAddress() {
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.filter(new AddressRequestBodyFilterDto());
        assertThrows(PnInternalException.class,
                () -> gatewayService.retrieveDigitalOrPhysicalAddress("Recipient Type", "clientId", addressRequestBodyDto));
    }

    @Test
    @DisplayName("Test CxId required")
    void testRetrieveDigitalOrPhysicalAddressThrow() {
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.filter(new AddressRequestBodyFilterDto());
        assertThrows(PnInternalException.class,
                () -> gatewayService.retrieveDigitalOrPhysicalAddress("Recipient Type", null, addressRequestBodyDto));
    }

    @Test
    @DisplayName("Test retrieve from ANPR Async")
    void testRetrieveDigitalOrPhysicalAddressAsync() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        GetAddressANPROKDto getAddressANPROKDto = new GetAddressANPROKDto();

        when(anprService.getAddressANPR(any()))
                .thenReturn(Mono.just(getAddressANPROKDto));

        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        MDC.setContextMap(Map.of(MDCUtils.MDC_TRACE_ID_KEY, "traceId"));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddressAsync("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
    }

    @Test
    @DisplayName("Test retrieve from ANPR")
    void testRetrieveDigitalOrPhysicalAddressAnpr() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        GetAddressANPROKDto getAddressANPROKDto = new GetAddressANPROKDto();

        when(anprService.getAddressANPR(any()))
                .thenReturn(Mono.just(getAddressANPROKDto));

        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
    }

    @Test
    @DisplayName("Test failed retrieve from ANPR")
    void testRetrieveDigitalOrPhysicalAddressAnprError() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        when(anprService.getAddressANPR(any()))
                .thenReturn(Mono.error(new RuntimeException()));

        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectError(RuntimeException.class);
    }

    @Test
    @DisplayName("Test retrieve from INAD")
    void testRetrieveDigitalOrPhysicalAddressInad() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(DIGITAL);

        GetDigitalAddressINADOKDto getDigitalAddressINADOKDto = new GetDigitalAddressINADOKDto();
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("digitalAddress@inad.com");
        getDigitalAddressINADOKDto.setDigitalAddress(digitalAddressDto);

        when(inadService.callEService(any(), any(), any()))
                .thenReturn(Mono.just(getDigitalAddressINADOKDto));

        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();

        verifyNoInteractions(infoCamereService);
        verifyNoInteractions(ipaService);
    }

    @Test
    void testRetrieveDigitalOrPhysicalAddressNewWorkflow() {
        when(featureEnabledUtils.isPfNewWorkflowEnabled(any())).thenReturn(true);
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(DIGITAL);

        GetDigitalAddressIniPECOKDto inipecDto = new GetDigitalAddressIniPECOKDto();

        when(infoCamereService.getIniPecDigitalAddress(any(), any(), any()))
                .thenReturn(Mono.just(inipecDto));


        ArgumentCaptor<CodeSqsDto> codeSqsDtoArgumentCaptor = ArgumentCaptor.forClass(CodeSqsDto.class);
        when(sqsService.pushToOutputQueue(codeSqsDtoArgumentCaptor.capture(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();

   }


    @Test
    @DisplayName("Test failed retrieve from INAD")
    void testRetrieveDigitalOrPhysicalAddressInadError() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(DIGITAL);

        when(inadService.callEService(any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectError(RuntimeException.class);
        verifyNoInteractions(infoCamereService);
        verifyNoInteractions(ipaService);
    }

    @Test
    @DisplayName("Test failed retrieve from INAD")
    void testRetrieveDigitalOrPhysicalAddressInadErrorIvalidEmail() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(DIGITAL);

        GetDigitalAddressINADOKDto getDigitalAddressINADOKDto = new GetDigitalAddressINADOKDto();
        DigitalAddressDto digitalAddressDto = new DigitalAddressDto();
        digitalAddressDto.setDigitalAddress("digitalAddressInadIvalidEmail.com");
        getDigitalAddressINADOKDto.setDigitalAddress(digitalAddressDto);

        when(inadService.callEService(any(), any(), any()))
                .thenReturn(Mono.just(getDigitalAddressINADOKDto));

        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
        verifyNoInteractions(infoCamereService);
        verifyNoInteractions(ipaService);
    }


    @Test
    @DisplayName("Test retrieve from Registro Imprese")
    void testRetrieveDigitalOrPhysicalAddressRegImp() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        GetAddressRegistroImpreseOKDto getAddressRegistroImpreseOKDto = new GetAddressRegistroImpreseOKDto();

        when(infoCamereService.getRegistroImpreseLegalAddress(any()))
                .thenReturn(Mono.just(getAddressRegistroImpreseOKDto));

        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
    }

    @Test
    @DisplayName("Test failed retrieve from Registro Imprese")
    void testRetrieveDigitalOrPhysicalAddressRegImpError() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        when(infoCamereService.getRegistroImpreseLegalAddress(any()))
                .thenReturn(Mono.error(new RuntimeException()));
        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectError(RuntimeException.class);
    }

    @Test
    @DisplayName("Test retrieve from IniPEC")
    void testRetrieveDigitalOrPhysicalAddressIniPEC() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(DIGITAL);

        GetDigitalAddressIniPECOKDto inipecDto = new GetDigitalAddressIniPECOKDto();

        when(infoCamereService.getIniPecDigitalAddress(any(), any(), any()))
                .thenReturn(Mono.just(inipecDto));

        when(sqsService.pushToOutputQueue(any(CodeSqsDto.class), any())).thenReturn(Mono.just(SendMessageResponse.builder().build()));
        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
    }

    @Test
    @DisplayName("Test retrieve from IniPEC")
    void testRetrieveDigitalOrPhysicalAddressIniPEC2() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(DIGITAL);

        IPAPecDto ipaPecOKDto = new IPAPecDto();

        when(ipaService.getIpaPec(any()))
                .thenReturn(Mono.just(ipaPecOKDto));

        when(infoCamereService.getIniPecDigitalAddress(any(),any(), any())).thenReturn(Mono.just(new GetDigitalAddressIniPECOKDto()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
    }

    @Test
    void testLogInWarn() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        PnNationalRegistriesException exception = new PnNationalRegistriesException("", 400, "", null, null, null, null);
        when(infoCamereService.getRegistroImpreseLegalAddress(any()))
                .thenReturn(Mono.error(exception));
        when(sqsService.pushToInputDlqQueue(any(InternalCodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
    }

    private AddressRequestBodyDto newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum domicileType) {
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setTaxId(CF);
        filterDto.setCorrelationId(C_ID);
        filterDto.setDomicileType(domicileType);
        filterDto.setReferenceRequestDate(LocalDateTime.now().toDate());

        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        addressRequestBodyDto.setFilter(filterDto);
        return addressRequestBodyDto;
    }

    @Test
    @DisplayName("Test retrieve physical address successfully")
    void testRetrievePhysicalAddressSuccess() {
        PhysicalAddressesRequestBodyDto request = new PhysicalAddressesRequestBodyDto();
        request.setCorrelationId(C_ID);
        request.setReferenceRequestDate(new Date());
        RecipientAddressRequestBodyDto recipient = new RecipientAddressRequestBodyDto();
        RecipientAddressRequestBodyFilterDto filter = new RecipientAddressRequestBodyFilterDto();
        filter.setTaxId(CF);
        filter.setRecipientType(RecipientAddressRequestBodyFilterDto.RecipientTypeEnum.PF);
        filter.setRecIndex("1");
        recipient.setFilter(filter);
        request.setAddresses(List.of(recipient));

        when(sqsService.pushToMultiInputQueue(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrievePhysicalAddress("clientId", request))
                .expectNext(addressOKDto)
                .verifyComplete();
    }

    @Test
    @DisplayName("Test retrieve physical address with error")
    void testRetrievePhysicalAddressError() {
        PhysicalAddressesRequestBodyDto request = new PhysicalAddressesRequestBodyDto();
        request.setCorrelationId(C_ID);
        request.setReferenceRequestDate(new Date());
        RecipientAddressRequestBodyDto recipient = new RecipientAddressRequestBodyDto();
        RecipientAddressRequestBodyFilterDto filter = new RecipientAddressRequestBodyFilterDto();
        filter.setTaxId(CF);
        filter.setRecipientType(RecipientAddressRequestBodyFilterDto.RecipientTypeEnum.PF);
        filter.setRecIndex("1");
        recipient.setFilter(filter);
        request.setAddresses(List.of(recipient));

        when(sqsService.pushToMultiInputQueue(any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        StepVerifier.create(gatewayService.retrievePhysicalAddress("clientId", request))
                .expectError(RuntimeException.class);
    }

    @Test
    @DisplayName("Test retrieve physical address with addresses not specified")
    void testRetrievePhysicalAddressError_addressesNotSpecified() {
        PhysicalAddressesRequestBodyDto request = new PhysicalAddressesRequestBodyDto();
        request.setCorrelationId(C_ID);
        request.setReferenceRequestDate(new Date());

        when(sqsService.pushToMultiInputQueue(any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        StepVerifier.create(gatewayService.retrievePhysicalAddress("clientId", request))
                .expectError(PnNationalRegistriesException.class);
    }

    @Test
    @DisplayName("Test retrieve physical address with addresses empty list")
    void testRetrievePhysicalAddressError_addressesEmptyList() {
        PhysicalAddressesRequestBodyDto request = new PhysicalAddressesRequestBodyDto();
        request.setCorrelationId(C_ID);
        request.setReferenceRequestDate(new Date());
        request.setAddresses(List.of());

        when(sqsService.pushToMultiInputQueue(any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        StepVerifier.create(gatewayService.retrievePhysicalAddress("clientId", request))
                .expectError(PnNationalRegistriesException.class);
    }

    @Test
    @DisplayName("Test handleMessageMultiRequest")
    void testHandleMessageMultiRequest() {

        when(sqsService.pushMultiToOutputQueue(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        MDC.setContextMap(Map.of(MDCUtils.MDC_TRACE_ID_KEY, "traceId"));
        GetAddressANPROKDto anprResponse = new GetAddressANPROKDto();
        anprResponse.setClientOperationId("clientOperationId");
        when(anprService.getAddressANPR(any())).thenReturn(Mono.just(anprResponse));

        StepVerifier.create(gatewayService.handleMessageMultiRequest(getPnAddressesGatewayEventPayload()))
                .expectNextMatches(response -> response.getCorrelationId().equals("correlationId"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Test retrieveMultiPhysicalAddress")
    void testRetrieveMultiPhysicalAddress() {
        when(sqsService.pushMultiToOutputQueue(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        MDC.setContextMap(Map.of(MDCUtils.MDC_TRACE_ID_KEY, "traceId"));

        GetAddressANPROKDto anprResponse = new GetAddressANPROKDto();
        anprResponse.setClientOperationId("clientOperationId");
        when(anprService.getAddressANPR(any())).thenReturn(Mono.just(anprResponse));

        GetAddressRegistroImpreseOKDto regImpreseResponse = new GetAddressRegistroImpreseOKDto();
        regImpreseResponse.setTaxId("taxId");
        when(infoCamereService.getRegistroImpreseLegalAddress(any())).thenReturn(Mono.just(regImpreseResponse));

        StepVerifier.create(gatewayService.retrieveMultiPhysicalAddress(getAddressQueryRequestList()))
                .expectNextMatches(response -> response.getCorrelationId().equals("correlationId"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Test retrievePhysicalAddresses for PF")
    void testRetrievePhysicalAddressesForPF() {
        GetAddressANPROKDto anprResponse = new GetAddressANPROKDto();
        anprResponse.setClientOperationId("clientOperationId");
        when(anprService.getAddressANPR(any())).thenReturn(Mono.just(anprResponse));
        AddressQueryRequest addressQueryRequestPF = AddressQueryRequest.builder()
                .correlationId("correlationId")
                .taxId("taxId")
                .referenceRequestDate(new Date())
                .recipientType(RecipientType.PF)
                .recIndex(0)
                .pnNationalRegistriesCxId("cxId")
                .build();

        StepVerifier.create(gatewayService.retrievePhysicalAddresses(addressQueryRequestPF))
                .expectNextMatches(response -> response.getRegistry().equals(GatewayDownstreamService.ANPR.name()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Test retrievePhysicalAddresses for PG")
    void testRetrievePhysicalAddressesForPG() {
        GetAddressRegistroImpreseOKDto regImpreseResponse = new GetAddressRegistroImpreseOKDto();
        regImpreseResponse.setTaxId("taxId2");
        when(infoCamereService.getRegistroImpreseLegalAddress(any())).thenReturn(Mono.just(regImpreseResponse));

        AddressQueryRequest addressQueryRequestPG = AddressQueryRequest.builder()
                .correlationId("correlationId")
                .taxId("taxId2")
                .recipientType(RecipientType.PG)
                .recIndex(1)
                .pnNationalRegistriesCxId("cxId")
                .build();

        StepVerifier.create(gatewayService.retrievePhysicalAddresses(addressQueryRequestPG))
                .expectNextMatches(response -> response.getRegistry().equals(GatewayDownstreamService.REGISTRO_IMPRESE.name()))
                .verifyComplete();
    }


    private static PnAddressesGatewayEvent.Payload getPnAddressesGatewayEventPayload() {
        return PnAddressesGatewayEvent.Payload.builder()
                .correlationId("correlationId")
                .referenceRequestDate(new Date())
                .pnNationalRegistriesCxId("cxId")
                .internalRecipientAdresses(List.of(InternalRecipientAddress.builder()
                        .taxId("taxId")
                        .recipientType(String.valueOf(RecipientType.PF))
                        .domicileType(DomicileType.PHYSICAL.name())
                        .recIndex(0)
                        .build()))
                .build();
    }

    private static List<AddressQueryRequest> getAddressQueryRequestList() {
        return List.of(
                AddressQueryRequest.builder()
                        .correlationId("correlationId")
                        .taxId("taxId1")
                        .recipientType(RecipientType.PF)
                        .recIndex(0)
                        .pnNationalRegistriesCxId("cxId")
                        .referenceRequestDate(new Date())
                        .domicileType(DomicileType.PHYSICAL)
                        .build(),
                AddressQueryRequest.builder()
                        .correlationId("correlationId")
                        .taxId("taxId2")
                        .recipientType(RecipientType.PG)
                        .referenceRequestDate(new Date())
                        .domicileType(DomicileType.PHYSICAL)
                        .recIndex(1)
                        .pnNationalRegistriesCxId("cxId")
                        .build()
        );
    }

}
