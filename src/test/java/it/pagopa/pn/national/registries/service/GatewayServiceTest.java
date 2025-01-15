package it.pagopa.pn.national.registries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import it.pagopa.pn.national.registries.utils.FeatureEnabledUtils;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
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

        IPAPecDto ipaPecDto = new IPAPecDto();
       ipaPecDto.setDomicilioDigitale("digitalAddress@ipa.com");

        when(ipaService.getIpaPec(any()))
                .thenReturn(Mono.just(ipaPecDto));

        ArgumentCaptor<CodeSqsDto> codeSqsDtoArgumentCaptor = ArgumentCaptor.forClass(CodeSqsDto.class);
        when(sqsService.pushToOutputQueue(codeSqsDtoArgumentCaptor.capture(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();

        verify(ipaService, times(1)).getIpaPec(any());
        verifyNoInteractions(inadService);
        verifyNoInteractions(infoCamereService);
        Assertions.assertEquals(ipaPecDto.getDomicilioDigitale(), codeSqsDtoArgumentCaptor.getValue().getDigitalAddress().get(0).getAddress());
        Assertions.assertEquals(DIGITAL.name(), codeSqsDtoArgumentCaptor.getValue().getAddressType());
    }

    @Test
    void testRetrieveDigitalOrPhysicalAddressNewWorkflowIpaNotFound() {
        when(featureEnabledUtils.isPfNewWorkflowEnabled(any())).thenReturn(true);
        AddressRequestBodyDto addressRequestBodyDto = newAddressRequestBodyDto(DIGITAL);

        IPAPecDto ipaPecDto = new IPAPecDto();

        when(ipaService.getIpaPec(any()))
                .thenReturn(Mono.just(ipaPecDto));
        when(infoCamereService.getIniPecDigitalAddress(any(), any(), any())).thenReturn(Mono.just(new GetDigitalAddressIniPECOKDto()));

        AddressOKDto addressOKDto = new AddressOKDto();
        addressOKDto.setCorrelationId(C_ID);

        StepVerifier.create(gatewayService.retrieveDigitalOrPhysicalAddress("PF", "clientId", addressRequestBodyDto))
                .expectNext(addressOKDto)
                .verifyComplete();

        verify(ipaService, times(1)).getIpaPec(any());
        verifyNoInteractions(inadService);
        verify(infoCamereService, times(1)).getIniPecDigitalAddress(any(), any(), any());
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

        IPAPecDto ipaPecOKDto = new IPAPecDto();
        ipaPecOKDto.setDomicilioDigitale("domicilioDigitale@ipa.com");
        ipaPecOKDto.setTipo("tipo");
        ipaPecOKDto.setCodEnte("codEnte");
        ipaPecOKDto.setDenominazione("denominazione");

        when(ipaService.getIpaPec(any()))
                .thenReturn(Mono.just(ipaPecOKDto));

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
}
