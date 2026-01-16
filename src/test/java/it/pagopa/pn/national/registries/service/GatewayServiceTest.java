package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.constant.GatewayError;
import it.pagopa.pn.national.registries.constant.RecipientType;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.utils.FeatureEnabledUtils;
import org.joda.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    @MockitoBean
    private AnprService anprService;
    @MockitoBean
    private InadService inadService;
    @MockitoBean
    private InfoCamereService infoCamereService;
    @MockitoBean
    private SqsService sqsService;
    @MockitoBean
    private IpaService ipaService;

    @Autowired
    private GatewayService gatewayService;

    @MockitoBean
    private ObjectMapper objectMapper;

    @MockitoBean
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
        when(sqsService.pushToOutputQueue(any(), any())).thenReturn(Mono.just(SendMessageResponse.builder().build()));
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

        when(sqsService.pushToOutputQueue(any(), any()))
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

        when(sqsService.pushToOutputQueue(any(), any()))
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

        when(sqsService.pushToInputDlqQueue(any(), any()))
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

        when(sqsService.pushToOutputQueue(any(), any()))
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

        when(sqsService.pushToInputDlqQueue(any(), any()))
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

        when(sqsService.pushToOutputQueue(any(), any()))
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

        when(sqsService.pushToOutputQueue(any(), any()))
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
        when(sqsService.pushToInputDlqQueue(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectError(RuntimeException.class);
    }

    @Test
    @DisplayName("Test retrieve from IniPEC")
    void testRetrieveDigitalOrPhysicalAddressIniPEC() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(DIGITAL);

        GetDigitalAddressIniPECOKDto inipecDto = new GetDigitalAddressIniPECOKDto();
        when(ipaService.getIpaPec(any())).thenReturn(Mono.just(new IPAPecDto()));
        when(infoCamereService.getIniPecDigitalAddress(any(), any(), any()))
                .thenReturn(Mono.just(inipecDto));

        when(sqsService.pushToOutputQueue(any(), any())).thenReturn(Mono.just(SendMessageResponse.builder().build()));
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
        when(sqsService.pushToInputDlqQueue(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 429})
    void testRetrievePhysicalAddressFromRegImpreseWritesEventOnDlqIfIrrecoverableException(int irrecoverableStatus) {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        PnNationalRegistriesException exception = new PnNationalRegistriesException("", irrecoverableStatus, "", null, null, null, null);
        when(infoCamereService.getRegistroImpreseLegalAddress(any()))
                .thenReturn(Mono.error(exception));
        when(sqsService.pushToInputDlqQueue(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();

        verify(infoCamereService, times(1)).getRegistroImpreseLegalAddress(any());
        verify(sqsService, times(1)).pushToInputDlqQueue(any(), any());
    }

    @Test
    void testRetrievePhysicalAddressFromRegImpreseThrowsException() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        PnNationalRegistriesException exception = new PnNationalRegistriesException("", 500, "", null, null, null, null);
        when(infoCamereService.getRegistroImpreseLegalAddress(any()))
                .thenReturn(Mono.error(exception));

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PG", "clientId", addressRequestBodyDto))
                .expectError(PnNationalRegistriesException.class)
                .verify();

        verify(infoCamereService, times(1)).getRegistroImpreseLegalAddress(any());
        verify(sqsService, times(0)).pushToInputDlqQueue(any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 429})
    void testRetrievePhysicalAddressFromAnprWritesEventOnDlqIfIrrecoverableException(int irrecoverableStatus) {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        PnNationalRegistriesException exception = new PnNationalRegistriesException("", irrecoverableStatus, "", null, null, null, null);
        when(anprService.getAddressANPR(any())).thenReturn(Mono.error(exception));
        when(sqsService.pushToInputDlqQueue(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();

        verify(anprService, times(1)).getAddressANPR(any());
        verify(sqsService, times(1)).pushToInputDlqQueue(any(), any());
    }

    @Test
    void testRetrievePhysicalAddressFromAnprThrowsException() {
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);

        PnNationalRegistriesException exception = new PnNationalRegistriesException("", 500, "", null, null, null, null);
        when(anprService.getAddressANPR(any())).thenReturn(Mono.error(exception));
        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectError(PnNationalRegistriesException.class)
                .verify();

        verify(anprService, times(1)).getAddressANPR(any());
        verify(sqsService, times(0)).pushToInputDlqQueue(any(), any());
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
    @DisplayName("Test retrieve physical address with addresses empty list")
    void testRetrievePhysicalAddressError_addressesEmptyList() {
        PhysicalAddressesRequestBodyDto request = new PhysicalAddressesRequestBodyDto();
        request.setCorrelationId(C_ID);
        request.setReferenceRequestDate(new Date());
        request.setAddresses(List.of());

        StepVerifier.create(gatewayService.retrievePhysicalAddresses(request))
                .expectError(PnNationalRegistriesException.class);
    }

    @Test
    @DisplayName("Test retrievePhysicalAddresses with 2 recipients: PF (200) and PG (200) successfully")
     void testRetrievePhysicalAddressesSuccess1() {
        /*
            In questo scenario gestiamo una richiesta con 2 destinatari:
            1) destinatario PF (200)
            2) destinatario PG (200)
            ci aspettiamo di ricevere una risposta 200 in cui per entrambi i destinatari abbiamo trovato e valorizzato l'indirizzo fisico
         */
        List<RecipientAddressRequestBodyDto> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(buildRecipientAddressRequest(CF_200_ANPR, RecipientType.PF, 0));
        recipientAddresses.add(buildRecipientAddressRequest(CF_200_REG_IMP, RecipientType.PG, 1));
        PhysicalAddressesRequestBodyDto requestBodyDto = buildAddressesRequestBody(recipientAddresses);

        mockPhysicalAddressRegistries(requestBodyDto.getAddresses());


        StepVerifier.create(gatewayService.retrievePhysicalAddresses(requestBodyDto))
                .expectNextMatches(response -> {
                    verifyPhysicalAddressOnResponse(true, response, 0);
                    verifyPhysicalAddressOnResponse(true, response, 1);
                    assertEquals(C_ID, response.getCorrelationId());
                    assertEquals(2, response.getAddresses().size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Test retrievePhysicalAddresses with 2 recipients: PF (404) and PG (200) successfully")
    void testRetrievePhysicalAddressesSuccess2() {
        /*
            In questo scenario gestiamo una richiesta con 2 destinatari:
            1) destinatario PF che non viene trovato in ANPR (404)
            2) destinatario PG (200)
            ci aspettiamo di ricevere una risposta 200 in cui per il primo destinatario non troviamo un indirizzo fisico (ma comunque non restituiamo errore!)
             Mentre per il secondo troviamo e valorizziamo l'indirizzo fisico
         */
        List<RecipientAddressRequestBodyDto> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(buildRecipientAddressRequest(CF_404_ANPR, RecipientType.PF, 0));
        recipientAddresses.add(buildRecipientAddressRequest(CF_200_REG_IMP, RecipientType.PG, 1));
        PhysicalAddressesRequestBodyDto requestBodyDto = buildAddressesRequestBody(recipientAddresses);

        mockPhysicalAddressRegistries(requestBodyDto.getAddresses());


        StepVerifier.create(gatewayService.retrievePhysicalAddresses(requestBodyDto))
                .expectNextMatches(response -> {
                    verifyPhysicalAddressOnResponse(false, response, 0);
                    verifyPhysicalAddressOnResponse(true, response, 1);
                    assertEquals(C_ID, response.getCorrelationId());
                    assertEquals(2, response.getAddresses().size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Test retrievePhysicalAddresses with 2 recipients: PF (429) and PG (200) successfully")
    void testRetrievePhysicalAddressesSuccess3() {
        /*
            In questo scenario gestiamo una richiesta con 2 destinatari:
            1) destinatario PF che riceve un errore 429 da ANPR
            2) destinatario PG (200)
            ci aspettiamo di ricevere una risposta 200 in cui per il primo destinatario non troviamo un indirizzo fisico e restituiamo il relativo errore!
             Mentre per il secondo troviamo e valorizziamo l'indirizzo fisico
         */
        List<RecipientAddressRequestBodyDto> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(buildRecipientAddressRequest(CF_429_ANPR, RecipientType.PF, 0));
        recipientAddresses.add(buildRecipientAddressRequest(CF_200_REG_IMP, RecipientType.PG, 1));
        PhysicalAddressesRequestBodyDto requestBodyDto = buildAddressesRequestBody(recipientAddresses);

        mockPhysicalAddressRegistries(requestBodyDto.getAddresses());


        StepVerifier.create(gatewayService.retrievePhysicalAddresses(requestBodyDto))
                .expectNextMatches(response -> {
                    verifyPhysicalAddressOnResponse(false, response, 0, GatewayError.DOWNSTREAM_TOO_MANY_REQUESTS);
                    verifyPhysicalAddressOnResponse(true, response, 1);
                    assertEquals(C_ID, response.getCorrelationId());
                    assertEquals(2, response.getAddresses().size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Test retrievePhysicalAddresses with 2 recipients: PF (200) and PG (400) successfully")
    void testRetrievePhysicalAddressesSuccess4() {
        /*
            In questo scenario gestiamo una richiesta con 2 destinatari:
            1) destinatario PF (200)
            2) destinatario PG (400)
            ci aspettiamo di ricevere una risposta 200 in cui per il primo destinatario troviamo un indirizzo fisico,
             Mentre per il secondo riceviamo un errore 400 (REQUEST_ERROR)
         */
        List<RecipientAddressRequestBodyDto> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(buildRecipientAddressRequest(CF_200_ANPR, RecipientType.PF, 0));
        recipientAddresses.add(buildRecipientAddressRequest(CF_400_REG_IMP, RecipientType.PG, 1));
        PhysicalAddressesRequestBodyDto requestBodyDto = buildAddressesRequestBody(recipientAddresses);

        mockPhysicalAddressRegistries(requestBodyDto.getAddresses());


        StepVerifier.create(gatewayService.retrievePhysicalAddresses(requestBodyDto))
                .expectNextMatches(response -> {
                    verifyPhysicalAddressOnResponse(true, response, 0);
                    verifyPhysicalAddressOnResponse(false, response, 1, GatewayError.DOWNSTREAM_REQUEST_ERROR);
                    assertEquals(C_ID, response.getCorrelationId());
                    assertEquals(2, response.getAddresses().size());
                    return true;
                })
                .verifyComplete();
    }

    private void mockPhysicalAddressRegistries(List<RecipientAddressRequestBodyDto> recipientAddresses) {
        for(RecipientAddressRequestBodyDto recipientAddress : recipientAddresses) {
            if(recipientAddress.getRecipientType().equals(RecipientAddressRequestBodyDto.RecipientTypeEnum.PF)) {
                mockAnpr(recipientAddress);
            } else if(recipientAddress.getRecipientType().equals(RecipientAddressRequestBodyDto.RecipientTypeEnum.PG)) {
                mockRegImprese(recipientAddress);
            }
        }
    }

    /**
     * Mock ANPR service response based on the taxId of the recipient address.
     * @param recipientAddress the recipient address to mock
     */
    private void mockAnpr(RecipientAddressRequestBodyDto recipientAddress) {
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

    private static GetAddressANPROKDto getDefaultAnpr200Response() {
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

    /**
     * Mock Registro Imprese service response based on the taxId of the recipient address.
     * @param recipientAddress the recipient address to mock
     */
    private void mockRegImprese(RecipientAddressRequestBodyDto recipientAddress) {
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

    private static GetAddressRegistroImpreseOKDto getDefaultRegImprese200Response() {
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

    private void verifyPhysicalAddressOnResponse(
            boolean shouldBeFound,
            PhysicalAddressesResponseDto response,
            int recIndex
    ) {
        verifyPhysicalAddressOnResponse(shouldBeFound, response, recIndex, null);
    }

    private void verifyPhysicalAddressOnResponse(
            boolean shouldBeFound,
            PhysicalAddressesResponseDto response,
            int recIndex,
            GatewayError expectedError
    ) {
        PhysicalAddressResponseDto recipientAddress = response.getAddresses()
                .stream()
                .filter(internalRecipientAddress -> internalRecipientAddress.getRecIndex() == recIndex)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Recipient Address not found"));

        if(shouldBeFound) {
            assertNotNull(recipientAddress.getPhysicalAddress(), "Recipient Address for recIndex " + recIndex + " should be found");
        } else {
            if(expectedError != null) {
                assertEquals(expectedError.name(), recipientAddress.getError(), "Recipient Address for recIndex " + recIndex + " should have error " + expectedError);
            }
            assertNull(recipientAddress.getPhysicalAddress(), "Recipient Address for recIndex" + recIndex + " should not be found");
        }
    }


    private static RecipientAddressRequestBodyDto buildRecipientAddressRequest(String taxId, RecipientType recipientType, Integer recIndex) {
        RecipientAddressRequestBodyDto recipientAddressRequestBodyDto = new RecipientAddressRequestBodyDto();
        recipientAddressRequestBodyDto.setRecipientType(RecipientAddressRequestBodyDto.RecipientTypeEnum.fromValue(recipientType.name()));
        recipientAddressRequestBodyDto.setRecIndex(recIndex);
        recipientAddressRequestBodyDto.setTaxId(taxId);
        return recipientAddressRequestBodyDto;
    }

    private static PhysicalAddressesRequestBodyDto buildAddressesRequestBody(List<RecipientAddressRequestBodyDto> recipientAddresses) {
        PhysicalAddressesRequestBodyDto physicalAddressesRequestBodyDto = new PhysicalAddressesRequestBodyDto();
        physicalAddressesRequestBodyDto.setCorrelationId(C_ID);
        physicalAddressesRequestBodyDto.addresses(recipientAddresses);
        physicalAddressesRequestBodyDto.setReferenceRequestDate(new Date());
        return physicalAddressesRequestBodyDto;
    }
}
