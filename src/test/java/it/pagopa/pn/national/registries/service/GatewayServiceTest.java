package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.constant.DomicileType;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.entity.GatewayRequestTrackerEntity;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.InternalRecipientAddress;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressesGatewayEvent;
import it.pagopa.pn.national.registries.model.*;
import it.pagopa.pn.national.registries.repository.GatewayRequestTrackerRepository;
import it.pagopa.pn.national.registries.utils.FeatureEnabledUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.*;

import static it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL;
import static org.junit.jupiter.api.Assertions.*;
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
    @MockBean
    private GatewayRequestTrackerRepository gatewayRequestTrackerRepository;

    @Autowired
    private GatewayService gatewayService;

    @MockBean
    private ObjectMapper objectMapper;

    @MockBean
    private FeatureEnabledUtils featureEnabledUtils;

    private static final String CF = "CF";
    private static final String C_ID = "correlationId";
    private static final String CX_ID = "cxId";
    private static final String CF_200_ANPR = "CF200ANPR";
    private static final String CF_400_ANPR = "CF400ANPR";
    private static final String CF_404_ANPR = "CF404ANPR";
    private static final String CF_429_ANPR = "CF429ANPR";
    private static final String CF_200_REG_IMP = "CF200REGIMP";
    private static final String CF_400_REG_IMP = "CF400REGIMP";
    private static final String CF_429_REG_IMP = "CF429REGIMP";
    private static final String CF_500_REG_IMP = "CF500REGIMP";

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

        when(sqsService.pushToValidationInputQueue(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrievePhysicalAddresses("clientId", request))
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

        when(sqsService.pushToValidationInputQueue(any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        StepVerifier.create(gatewayService.retrievePhysicalAddresses("clientId", request))
                .expectError(RuntimeException.class);
    }

    @Test
    @DisplayName("Test retrieve physical address with addresses not specified")
    void testRetrievePhysicalAddressError_addressesNotSpecified() {
        PhysicalAddressesRequestBodyDto request = new PhysicalAddressesRequestBodyDto();
        request.setCorrelationId(C_ID);
        request.setReferenceRequestDate(new Date());

        when(sqsService.pushToValidationInputQueue(any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        StepVerifier.create(gatewayService.retrievePhysicalAddresses("clientId", request))
                .expectError(PnNationalRegistriesException.class);
    }

    @Test
    @DisplayName("Test retrieve physical address with addresses empty list")
    void testRetrievePhysicalAddressError_addressesEmptyList() {
        PhysicalAddressesRequestBodyDto request = new PhysicalAddressesRequestBodyDto();
        request.setCorrelationId(C_ID);
        request.setReferenceRequestDate(new Date());
        request.setAddresses(List.of());

        when(sqsService.pushToValidationInputQueue(any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        StepVerifier.create(gatewayService.retrievePhysicalAddresses("clientId", request))
                .expectError(PnNationalRegistriesException.class);
    }

    @Test
    void testHandleMessageMultiRequestFailsWhenThereAreNoRecipients() {
        PnAddressesGatewayEvent.Payload payload = getPnAddressesGatewayEventPayload(Collections.emptyList());

        StepVerifier.create(gatewayService.handleMessageMultiRequest(payload))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    @DisplayName("Test handleMessageMultiRequest with 2 recipients: PF (200) and PG (200) successfully")
    void testHandleMessageMultiRequestSuccess1() {
        /*
            In questo scenario gestiamo un evento con 2 destinatari:
            1) destinatario PF (200)
            2) destinatario PG (200)
            ci aspettiamo che venga inviata una risposta sulla coda di output di SQS con i dati dell'indirizzo fisico di entrambi i destinatari.
         */
        List<InternalRecipientAddress> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(buildInternalRecipientAddress(CF_200_ANPR, RecipientType.PF, 0));
        recipientAddresses.add(buildInternalRecipientAddress(CF_200_REG_IMP, RecipientType.PG, 1));
        PnAddressesGatewayEvent.Payload payload = getPnAddressesGatewayEventPayload(recipientAddresses);

        mockGatewayRequestTrackerRepository(payload.getCorrelationId());

        mockPhysicalAddressRegistries(payload.getInternalRecipientAdresses());

        when(sqsService.pushMultiToOutputQueue(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.handleMessageMultiRequest(payload))
                .expectNextMatches(response -> response.getCorrelationId().equals(C_ID))
                .verifyComplete();

        Mockito.verify(anprService, times(1)).getAddressANPR(any());
        Mockito.verify(infoCamereService, times(1)).getRegistroImpreseLegalAddress(any());
        Mockito.verify(sqsService, times(1)).pushMultiToOutputQueue(any(), any());

        ArgumentCaptor<MultiCodeSqsDto> captor = ArgumentCaptor.forClass(MultiCodeSqsDto.class);
        verify(sqsService).pushMultiToOutputQueue(captor.capture(), eq(CX_ID));
        MultiCodeSqsDto capturedDto = captor.getValue();

        assertEquals(C_ID, capturedDto.getCorrelationId());
        assertEquals(2, capturedDto.getAddresses().size());
        verifyPhysicalAddressOnOutputQueue(true, capturedDto, 0);
        verifyPhysicalAddressOnOutputQueue(true, capturedDto, 1);
    }

    @Test
    @DisplayName("Test handleMessageMultiRequest with 2 recipients: PF (404) and PG (200) successfully")
    void testHandleMessageMultiRequestSuccess2() {
        /*
            In questo scenario gestiamo un evento con 2 destinatari:
            1) destinatario PF che non viene trovato in ANPR (404)
            2) destinatario PG (200)
            ci aspettiamo che venga inviata una risposta sulla coda di output di SQS con i dati dell'indirizzo fisico del scondo destinatario
             mentre il primo dovrebbe avere un oggetto vuoto.
         */
        List<InternalRecipientAddress> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(buildInternalRecipientAddress(CF_404_ANPR, RecipientType.PF, 0));
        recipientAddresses.add(buildInternalRecipientAddress(CF_200_REG_IMP, RecipientType.PG, 1));
        PnAddressesGatewayEvent.Payload payload = getPnAddressesGatewayEventPayload(recipientAddresses);

        mockGatewayRequestTrackerRepository(payload.getCorrelationId());

        mockPhysicalAddressRegistries(payload.getInternalRecipientAdresses());

        when(sqsService.pushMultiToOutputQueue(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.handleMessageMultiRequest(payload))
                .expectNextMatches(response -> response.getCorrelationId().equals(C_ID))
                .verifyComplete();

        Mockito.verify(anprService, times(1)).getAddressANPR(any());
        Mockito.verify(infoCamereService, times(1)).getRegistroImpreseLegalAddress(any());
        Mockito.verify(sqsService, times(1)).pushMultiToOutputQueue(any(), any());

        ArgumentCaptor<MultiCodeSqsDto> captor = ArgumentCaptor.forClass(MultiCodeSqsDto.class);
        verify(sqsService).pushMultiToOutputQueue(captor.capture(), eq(CX_ID));
        MultiCodeSqsDto capturedDto = captor.getValue();

        assertEquals(C_ID, capturedDto.getCorrelationId());
        assertEquals(2, capturedDto.getAddresses().size());
        verifyPhysicalAddressOnOutputQueue(false, capturedDto, 0);
        verifyPhysicalAddressOnOutputQueue(true, capturedDto, 1);
    }

    @Test
    @DisplayName("Test handleMessageMultiRequest with 2 recipients: PF (429) and PG (200) successfully")
    void testHandleMessageMultiRequestSuccess3() {
        /*
            In questo scenario gestiamo un evento con 2 destinatari:
            1) destinatario PF che riceve un errore 429 da ANPR
            2) destinatario PG (200)
            ci aspettiamo che venga inviato un evento in DLQ e non sulla coda di output di SQS
         */
        List<InternalRecipientAddress> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(buildInternalRecipientAddress(CF_429_ANPR, RecipientType.PF, 0));
        recipientAddresses.add(buildInternalRecipientAddress(CF_200_REG_IMP, RecipientType.PG, 1));
        PnAddressesGatewayEvent.Payload payload = getPnAddressesGatewayEventPayload(recipientAddresses);

        mockGatewayRequestTrackerRepository(payload.getCorrelationId());

        mockPhysicalAddressRegistries(payload.getInternalRecipientAdresses());

        when(sqsService.pushToInputDlqQueue(any(MultiRecipientCodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.handleMessageMultiRequest(payload))
                .expectNextMatches(response -> response.getCorrelationId().equals(C_ID))
                .verifyComplete();

        Mockito.verify(anprService, times(1)).getAddressANPR(any());

        ArgumentCaptor<MultiRecipientCodeSqsDto> captor = ArgumentCaptor.forClass(MultiRecipientCodeSqsDto.class);
        verify(sqsService).pushToInputDlqQueue(captor.capture(), eq(CX_ID));
        MultiRecipientCodeSqsDto capturedDto = captor.getValue();

        assertEquals(C_ID, capturedDto.getCorrelationId());
        assertEquals(2, capturedDto.getInternalRecipientAdresses().size());
        // controllo che non siano stati scritti messaggi sulla coda di output
        Mockito.verify(sqsService, times(0)).pushMultiToOutputQueue(any(), any());
    }

    @Test
    @DisplayName("Test handleMessageMultiRequest with 2 recipients: PF (200) and PG (400) successfully")
    void testHandleMessageMultiRequestSuccess4() {
        /*
            In questo scenario gestiamo un evento con 2 destinatari:
            1) destinatario PF (200)
            2) destinatario PG (400)
            ci aspettiamo che venga inviato un evento in DLQ e non sulla coda di output di SQS
         */
        List<InternalRecipientAddress> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(buildInternalRecipientAddress(CF_200_ANPR, RecipientType.PF, 0));
        recipientAddresses.add(buildInternalRecipientAddress(CF_400_REG_IMP, RecipientType.PG, 1));
        PnAddressesGatewayEvent.Payload payload = getPnAddressesGatewayEventPayload(recipientAddresses);

        mockGatewayRequestTrackerRepository(payload.getCorrelationId());

        mockPhysicalAddressRegistries(payload.getInternalRecipientAdresses());

        when(sqsService.pushToInputDlqQueue(any(MultiRecipientCodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.handleMessageMultiRequest(payload))
                .expectNextMatches(response -> response.getCorrelationId().equals(C_ID))
                .verifyComplete();

        Mockito.verify(infoCamereService, times(1)).getRegistroImpreseLegalAddress(any());

        ArgumentCaptor<MultiRecipientCodeSqsDto> captor = ArgumentCaptor.forClass(MultiRecipientCodeSqsDto.class);
        verify(sqsService).pushToInputDlqQueue(captor.capture(), eq(CX_ID));
        MultiRecipientCodeSqsDto capturedDto = captor.getValue();

        assertEquals(C_ID, capturedDto.getCorrelationId());
        assertEquals(2, capturedDto.getInternalRecipientAdresses().size());
        // controllo che non siano stati scritti messaggi sulla coda di output
        Mockito.verify(sqsService, times(0)).pushMultiToOutputQueue(any(), any());
    }

    @Test
    @DisplayName("Test handleMessageMultiRequest with 2 recipients: PF (200) and PG (500) successfully")
    void testHandleMessageMultiRequestSuccess5() {
        /*
            In questo scenario gestiamo un evento con 2 destinatari:
            1) destinatario PF (200)
            2) destinatario PG (500)
            ci aspettiamo che venga rilanciata un'eccezione e il flusso sia ritentato tramite retry
         */
        List<InternalRecipientAddress> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(buildInternalRecipientAddress(CF_200_ANPR, RecipientType.PF, 0));
        recipientAddresses.add(buildInternalRecipientAddress(CF_500_REG_IMP, RecipientType.PG, 1));
        PnAddressesGatewayEvent.Payload payload = getPnAddressesGatewayEventPayload(recipientAddresses);

        mockGatewayRequestTrackerRepository(payload.getCorrelationId());

        mockPhysicalAddressRegistries(payload.getInternalRecipientAdresses());

        when(sqsService.pushToInputDlqQueue(any(MultiRecipientCodeSqsDto.class), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.handleMessageMultiRequest(payload))
                .expectError(PnNationalRegistriesException.class)
                .verify();

        Mockito.verify(infoCamereService, times(1)).getRegistroImpreseLegalAddress(any());
        // controllo che non siano stati scritti messaggi sulle code
        Mockito.verify(sqsService, times(0)).pushMultiToOutputQueue(any(), any());
        Mockito.verify(sqsService, times(0)).pushToInputDlqQueue(any(MultiRecipientCodeSqsDto.class), any());
    }

    private void mockGatewayRequestTrackerRepository(String correlationId) {
        GatewayRequestTrackerEntity entity = new GatewayRequestTrackerEntity();
        entity.setCorrelationId(correlationId);
        entity.setRequestTimestamp(Instant.now());
        when(gatewayRequestTrackerRepository.putIfAbsentOrRetrieve(correlationId)).thenReturn(Mono.just(entity));
    }

    private void verifyPhysicalAddressOnOutputQueue(boolean shouldBeFound, MultiCodeSqsDto capturedDto, int recIndex) {
        MultiCodeSqsDto.PhysicalAddressSQSMessage recipientAddress = capturedDto.getAddresses()
                .stream()
                .filter(internalRecipientAddress -> internalRecipientAddress.getRecIndex() == recIndex)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Recipient Address not found"));

        if(shouldBeFound) {
            assertNotNull(recipientAddress.getPhysicalAddress());
        } else {
            assertNull(recipientAddress.getPhysicalAddress());
        }
    }

    private void mockPhysicalAddressRegistries(List<InternalRecipientAddress> recipientAddresses) {
        for(InternalRecipientAddress recipientAddress : recipientAddresses) {
            if(recipientAddress.getRecipientType().equals("PF")) {
                mockAnpr(recipientAddress);
            } else if(recipientAddress.getRecipientType().equals("PG")) {
                mockRegImprese(recipientAddress);
            }
        }
    }

    /**
     * Mock Registro Imprese service response based on the taxId of the recipient address.
     * @param recipientAddress the recipient address to mock
     */
    private void mockRegImprese(InternalRecipientAddress recipientAddress) {
        switch (recipientAddress.getTaxId()) {

            case CF_400_REG_IMP:
                PnNationalRegistriesException exception400 = new PnNationalRegistriesException("Bad Request", HttpStatus.BAD_REQUEST.value(), "Bad Request", null, null, Charset.defaultCharset(), null);
                when(infoCamereService.getRegistroImpreseLegalAddress(any())).thenReturn(Mono.error(exception400));
            break;
            case CF_429_REG_IMP:
                PnNationalRegistriesException exception429 = new PnNationalRegistriesException("Too Many Requests", HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests", null, null, Charset.defaultCharset(), null);
                when(infoCamereService.getRegistroImpreseLegalAddress(any())).thenReturn(Mono.error(exception429));
            break;
            case CF_500_REG_IMP:
                PnNationalRegistriesException exception500 = new PnNationalRegistriesException("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", null, null, Charset.defaultCharset(), null);
                when(infoCamereService.getRegistroImpreseLegalAddress(any())).thenReturn(Mono.error(exception500));
            break;
            case CF_200_REG_IMP:
            default:
                GetAddressRegistroImpreseOKDto regImpreseResponse = getDefaultRegImprese200Response();
                when(infoCamereService.getRegistroImpreseLegalAddress(any())).thenReturn(Mono.just(regImpreseResponse));
            break;
        }
    }

    private static @NotNull GetAddressRegistroImpreseOKDto getDefaultRegImprese200Response() {
        GetAddressRegistroImpreseOKDto regImpreseResponse = new GetAddressRegistroImpreseOKDto();
        regImpreseResponse.setTaxId("taxId");
        GetAddressRegistroImpreseOKProfessionalAddressDto professionalAddress = new GetAddressRegistroImpreseOKProfessionalAddressDto();
        professionalAddress.setAddress("Via Roma 1");
        professionalAddress.setMunicipality("Roma");
        professionalAddress.setZip("00100");
        professionalAddress.setProvince("RM");
        regImpreseResponse.setProfessionalAddress(professionalAddress);
        return regImpreseResponse;
    }

    /**
     * Mock ANPR service response based on the taxId of the recipient address.
     * @param recipientAddress the recipient address to mock
     */
    private void mockAnpr(InternalRecipientAddress recipientAddress) {
        switch (recipientAddress.getTaxId()) {
            case CF_400_ANPR:
                PnNationalRegistriesException exception400 = new PnNationalRegistriesException("Bad Request", HttpStatus.BAD_REQUEST.value(), "Bad Request", null, null, Charset.defaultCharset(), null);
                when(anprService.getAddressANPR(any())).thenReturn(Mono.error(exception400));
            break;
            case CF_404_ANPR:
                byte[] responseBody = "{\"codiceErroreAnomalia\":\"EN122\",\"messaggioErrore\":\"Indirizzo non trovato\"}".getBytes(Charset.defaultCharset());
                PnNationalRegistriesException exception404 = new PnNationalRegistriesException("Not Found", HttpStatus.NOT_FOUND.value(), "Not Found", null, responseBody, Charset.defaultCharset(), null);
                when(anprService.getAddressANPR(any())).thenReturn(Mono.error(exception404));
            break;
            case CF_429_ANPR:
                PnNationalRegistriesException exception429 = new PnNationalRegistriesException("Too Many Requests", HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests", null, null, Charset.defaultCharset(), null);
                when(anprService.getAddressANPR(any())).thenReturn(Mono.error(exception429));
            break;
            case CF_200_ANPR:
            default:
                GetAddressANPROKDto anprResponse = getDefaultAnpr200Response();
                when(anprService.getAddressANPR(any())).thenReturn(Mono.just(anprResponse));
            break;
        }
    }

    private static @NotNull GetAddressANPROKDto getDefaultAnpr200Response() {
        GetAddressANPROKDto anprResponse = new GetAddressANPROKDto();
        anprResponse.setClientOperationId("clientOperationId");
        List<ResidentialAddressDto> residentialAddresses = new ArrayList<>();
        ResidentialAddressDto residentialAddress = new ResidentialAddressDto();
        residentialAddress.setAddress("Via Roma 1");
        residentialAddress.setMunicipality("Roma");
        residentialAddress.setZip("00100");
        residentialAddress.setProvince("RM");
        residentialAddresses.add(residentialAddress);
        anprResponse.setResidentialAddresses(residentialAddresses);
        return anprResponse;
    }

    private static InternalRecipientAddress buildInternalRecipientAddress(String taxId, RecipientType recipientType, Integer recIndex) {
        return InternalRecipientAddress.builder()
                .taxId(taxId)
                .recipientType(String.valueOf(recipientType))
                .domicileType(DomicileType.PHYSICAL.name())
                .recIndex(recIndex)
                .build();
    }

    private static PnAddressesGatewayEvent.Payload getPnAddressesGatewayEventPayload(List<InternalRecipientAddress> recipientAddresses) {
        return PnAddressesGatewayEvent.Payload.builder()
                .correlationId(C_ID)
                .referenceRequestDate(new Date())
                .pnNationalRegistriesCxId(CX_ID)
                .internalRecipientAdresses(recipientAddresses)
                .build();
    }
}
