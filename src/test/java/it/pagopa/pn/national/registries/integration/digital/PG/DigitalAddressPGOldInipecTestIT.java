package it.pagopa.pn.national.registries.integration.digital.PG;

import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS05Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS23Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.ResultDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS05ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS23ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPARequestBodyDto;
import it.pagopa.pn.national.registries.integration.AbstractNationalRegistriesComponentIT;
import it.pagopa.pn.national.registries.integration.PnNationalRegistriesTestApplication;
import it.pagopa.pn.national.registries.model.GatewayResponse;
import it.pagopa.pn.national.registries.model.inipec.IniPecBatchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.national.registries.converter.GatewayConverter.convertToGetIpaPecRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Scenari funzionali PG DIGITAL
 *
 * Configurazione:
 *  - INAD massivo disattivato
 *  - old IniPEC attivo
 */
@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
@TestPropertySource(properties = {
        "pn.national-registries.enable-inad-massive-api=false",
        "pn.national-registries.enable-old-inipec-flow=true",
        "pn.national-registries.enable-pf-pec-fallback-flow=true",
        "pn.national-registries.inipec.polling-first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.polling-first-attempt-fixed-delay-seconds=0",
        "spring.task.scheduling.enabled=false"
})
public class DigitalAddressPGOldInipecTestIT extends AbstractNationalRegistriesComponentIT {

    private static final String PG_PROFESSIONAL = "56815563368";
    private static final String PG_BUSINESS_PROFESSIONAL = "28223913097";
    private static final String PG_INVALID = "39274018561";
    private static final String PG_ND = "83016259471";
    private static final String PG_NF = "12825810299";
    private static final String PG_MISSING = "00534280912";

    /**
     * IPA diretto e Gateway PG restituiscono la stessa PEC.
     */
    @Test
    void Ipa_DirectEndpointAndGateway_ReturnSameFunctionalAddress() {
        String taxId = VALID_PG_PIVA;
        String correlationId = "ipa-gateway-old-inipec-" + UUID.randomUUID();

        when(ipaClient.callEServiceWS23(eq(taxId), anyString()))
                .thenReturn(Mono.just(ipaFoundResponse(taxId, "same-ipa@pec.it")));

        IPARequestBodyDto directRequest = convertToGetIpaPecRequest(
                buildGatewayRequest(
                        "ipa-direct-old-inipec-" + UUID.randomUUID(),
                        taxId,
                        AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL
                )
        );

        IPAPecDto direct = webTestClient.post()
                .uri("/national-registries-private/ipa/pec")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(directRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(IPAPecDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(direct);

        enqueueGatewayRequest(
                "PG",
                taxId,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );
        processNextGatewayInputMessage();

        Message output = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(output);

        GatewayResponse gateway = readNewOutputMessage(output.body());

        assertEquals( direct.getDomicilioDigitale(), gateway.getPayload().getDigitalAddress().getFirst().getAddress() );
        assertEquals("IPA", gateway.getPayload().getRegistry());
    }

    /**
     * IPA trovato.
     */
    @Test
    void PG_Digital_IPA() {
        String correlationId = "pg-old-inipec-ipa-found-" + UUID.randomUUID();

        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any()))
                .thenReturn(Mono.just(
                        ipaFoundResponse(
                                VALID_PG_PIVA,
                                "pg@pec.it"
                        )
                ));

        AddressOKDto response = enqueueGatewayRequest(
                "PG",
                VALID_PG_PIVA,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        GatewayResponse payload = readNewOutputMessage(outputMessage.body());

        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("IPA", payload.getPayload().getRegistry());
        assertEquals( AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL.getValue(), payload.getPayload().getAddressType() );
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals( "pg@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress() );
        assertEquals( "IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient() );

        assertTrue( findNationalRegistriesRequestsByCorrelationId(correlationId) .isEmpty() );
        assertTrue( findBatchRequestsByCorrelationPrefix(correlationId) .isEmpty() );
        assertNull(receiveSingleMessage(INPUT_DLQ_QUEUE));

        verify(ipaClient)
                .callEServiceWS23(eq(VALID_PG_PIVA), any());

        verifyNoInteractions(
                infoCamereClient,
                inadClient
        );
    }

    /**
     * IPA con più risultati: usa WS05.
     */
    @Test
    void PG_Digital_IPA_MultipleResults_UsesWS05() {
        String correlationId =
                "pg-old-inipec-ipa-multiple-" + UUID.randomUUID();

        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any()))
                .thenReturn(Mono.just(ipaMultipleResponse()));

        when(ipaClient.callEServiceWS05(eq("cod-amm-1"), any()))
                .thenReturn(Mono.just(
                        ipaWs05Response("ws05@pec.it")
                ));

        AddressOKDto response = enqueueGatewayRequest(
                "PG",
                VALID_PG_PIVA,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());

        Message outputMessage = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(outputMessage);

        GatewayResponse payload = readNewOutputMessage(outputMessage.body());

        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("IPA", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals( "ws05@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress() );
        assertEquals( "IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient() );

        assertTrue( findNationalRegistriesRequestsByCorrelationId(correlationId) .isEmpty() );
        assertTrue( findBatchRequestsByCorrelationPrefix(correlationId) .isEmpty() );

        verify(ipaClient)
                .callEServiceWS23(eq(VALID_PG_PIVA), any());

        verify(ipaClient)
                .callEServiceWS05(eq("cod-amm-1"), any());

        verifyNoInteractions(
                infoCamereClient,
                inadClient
        );
    }

    /**
     * IPA NotFound -> old IniPEC trovato.
     */
    @Test
    void PG_Digital_IPA_NotFound_OldInipecFound() {
        String correlationId =
                "pg-old-inipec-found-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                VALID_PG_PIVA
        );

        GatewayResponse payload = completeOldInipecFinal(
                correlationId,
                inipecPollingResponse(
                        pecResponse(
                                VALID_PG_PIVA,
                                "old-inipec@pec.it"
                        )
                )
        );

        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals( "old-inipec@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress() );
        assertEquals( "IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient() );

        verifyNoInteractions(inadClient);
    }

    /**
     * IPA con PEC invalida -> old IniPEC trovato.
     */
    @Test
    void PG_Digital_IPA_InvalidPec_OldInipecFound() {
        String correlationId =
                "pg-old-inipec-after-invalid-ipa-" + UUID.randomUUID();

        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any()))
                .thenReturn(Mono.just(
                        ipaFoundResponse(
                                VALID_PG_PIVA,
                                "invalid-pec"
                        )
                ));

        AddressOKDto response = enqueueGatewayRequest(
                "PG",
                VALID_PG_PIVA,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        processNextGatewayInputMessage();

        assertEquals(correlationId, response.getCorrelationId());
        assertOldInipecPendingRequest(
                correlationId,
                VALID_PG_PIVA
        );

        GatewayResponse payload = completeOldInipecFinal(
                correlationId,
                inipecPollingResponse(
                        pecResponse(
                                VALID_PG_PIVA,
                                "old-inipec-after-invalid-ipa@pec.it"
                        )
                )
        );

        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals( "old-inipec-after-invalid-ipa@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress() );
        assertEquals( "IMPRESA", payload.getPayload().getDigitalAddress().getFirst().getRecipient() );

        verifyNoInteractions(inadClient);
    }

    /**
     * IPA errore tecnico: nessun proseguimento.
     */
    @Test
    void PG_Digital_IPA_TechnicalError_DoesNotContinue() {
        String correlationId =
                "pg-old-inipec-ipa-technical-" + UUID.randomUUID();

        RuntimeException technicalError =
                new RuntimeException("IPA unavailable");

        when(ipaClient.callEServiceWS23(eq(VALID_PG_PIVA), any()))
                .thenReturn(Mono.error(technicalError));

        AddressOKDto response = enqueueGatewayRequest(
                "PG",
                VALID_PG_PIVA,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        assertEquals(correlationId, response.getCorrelationId());

        RuntimeException thrown =
                processNextGatewayInputMessageExpectingFailure();

        assertSame( technicalError, thrown.getCause() != null ? thrown.getCause() : thrown );

        assertTrue( findNationalRegistriesRequestsByCorrelationId(correlationId) .isEmpty() );
        assertTrue( findBatchRequestsByCorrelationPrefix(correlationId) .isEmpty() );

        verify(ipaClient)
                .callEServiceWS23(eq(VALID_PG_PIVA), any());

        verifyNoInteractions(
                infoCamereClient,
                inadClient
        );
    }

    /**
     * IPA NotFound -> old IniPEC con PEC professionale valida.
     */
    @Test
    void PG_Digital_FullFlow_OldInipecProfessionalPecIsReturned() {
        String correlationId =
                "pg-old-inipec-professional-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                PG_PROFESSIONAL
        );

        GatewayResponse payload = completeOldInipecFinal(
                correlationId,
                inipecPollingResponse(
                        pecResponseWithProfessionals(
                                PG_PROFESSIONAL,
                                null,
                                "professional@pec.it"
                        )
                )
        );

        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals( "professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress() );
        assertEquals( "PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient() );

        verifyNoInteractions(inadClient);
    }

    /**
     * IPA NotFound -> old IniPEC con PEC impresa e professionali valide.
     */
    @Test
    void PG_Digital_FullFlow_OldInipecBusinessAndAllProfessionalPecsAreReturned() {
        String correlationId =
                "pg-old-inipec-business-professionals-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                PG_BUSINESS_PROFESSIONAL
        );

        GatewayResponse payload = completeOldInipecFinal(
                correlationId,
                inipecPollingResponse(
                        pecResponseWithProfessionals(
                                PG_BUSINESS_PROFESSIONAL,
                                "business@pec.it",
                                "professional-one@pec.it",
                                "professional-two@pec.it"
                        )
                )
        );

        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(3, payload.getPayload().getDigitalAddress().size());

        assertTrue( payload.getPayload().getDigitalAddress().stream() .anyMatch(address -> "business@pec.it".equals( address.getAddress() ) && "IMPRESA".equals( address.getRecipient() ) ) );

        assertTrue( payload.getPayload().getDigitalAddress().stream() .anyMatch(address -> "professional-one@pec.it".equals( address.getAddress() ) && "PROFESSIONISTA".equals( address.getRecipient() ) ) );

        assertTrue( payload.getPayload().getDigitalAddress().stream() .anyMatch(address -> "professional-two@pec.it".equals( address.getAddress() ) && "PROFESSIONISTA".equals( address.getRecipient() ) ) );

        verifyNoInteractions(inadClient);
    }

    /**
     * IPA NotFound -> old IniPEC con PEC invalida -> INAD.
     */
    @Test
    void PG_Digital_FullFlow_OldInipecInvalidPec_ContinuesToInad() {
        String correlationId =
                "pg-old-inipec-invalid-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                PG_INVALID
        );

        when(inadClient.callEService(
                eq(PG_INVALID),
                startsWith(correlationId)
        )).thenReturn(
                Mono.just(
                        inadResponse(
                                PG_INVALID,
                                "inad-after-old-inipec-invalid@pec.it",
                                null
                        )
                )
        );

        GatewayResponse payload = completeOldInipecFinal(
                correlationId,
                inipecPollingResponse(
                        pecResponse(
                                PG_INVALID,
                                "not-an-email"
                        )
                )
        );

        assertInadPayload(
                correlationId,
                payload,
                "inad-after-old-inipec-invalid@pec.it"
        );
    }

    /**
     * IPA NotFound -> old IniPEC ND -> INAD.
     */
    @Test
    void PG_Digital_FullFlow_OldInipecNd_ContinuesToInad() {
        String correlationId =
                "pg-old-inipec-nd-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                PG_ND
        );

        Pec nd = pecResponse(
                PG_ND,
                "ignored-nd@pec.it"
        );
        nd.setStatoImpresa(Pec.StatoImpresaEnum.ND);

        when(inadClient.callEService(
                eq(PG_ND),
                startsWith(correlationId)
        )).thenReturn(
                Mono.just(
                        inadResponse(
                                PG_ND,
                                "inad-after-old-inipec-nd@pec.it",
                                null
                        )
                )
        );

        GatewayResponse payload = completeOldInipecFinal(
                correlationId,
                inipecPollingResponse(nd)
        );

        assertInadPayload(
                correlationId,
                payload,
                "inad-after-old-inipec-nd@pec.it"
        );
    }

    /**
     * IPA NotFound -> old IniPEC NF -> INAD.
     */
    @Test
    void PG_Digital_FullFlow_OldInipecNf_ContinuesToInad() {
        String correlationId =
                "pg-old-inipec-nf-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                PG_NF
        );

        Pec nf = pecResponse(
                PG_NF,
                "ignored-nf@pec.it"
        );
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);

        when(inadClient.callEService(
                eq(PG_NF),
                startsWith(correlationId)
        )).thenReturn(
                Mono.just(
                        inadResponse(
                                PG_NF,
                                "inad-after-old-inipec-nf@pec.it",
                                null
                        )
                )
        );

        GatewayResponse payload = completeOldInipecFinal(
                correlationId,
                inipecPollingResponse(nf)
        );

        assertInadPayload(
                correlationId,
                payload,
                "inad-after-old-inipec-nf@pec.it"
        );
    }

    /**
     * IPA NotFound -> old IniPEC senza CF richiesto -> INAD.
     */
    @Test
    void PG_Digital_FullFlow_OldInipecMissingTaxId_ContinuesToInad() {
        String correlationId =
                "pg-old-inipec-missing-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                PG_MISSING
        );

        when(inadClient.callEService(
                eq(PG_MISSING),
                startsWith(correlationId)
        )).thenReturn(
                Mono.just(
                        inadResponse(
                                PG_MISSING,
                                "inad-after-old-inipec-missing@pec.it",
                                null
                        )
                )
        );

        GatewayResponse payload = completeOldInipecFinal(
                correlationId,
                inipecPollingResponse()
        );

        assertInadPayload(
                correlationId,
                payload,
                "inad-after-old-inipec-missing@pec.it"
        );
    }

    /**
     * IPA NotFound -> old IniPEC con più PEC professionali valide.
     */
    @Test
    void PG_Digital_FullFlow_OldInipec_AllProfessionalPecsArePreserved() {
        String correlationId =
                "pg-old-inipec-professionals-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                PG_PROFESSIONAL
        );

        GatewayResponse payload = completeOldInipecFinal(
                correlationId,
                inipecPollingResponse(
                        pecResponseWithProfessionals(
                                PG_PROFESSIONAL,
                                null,
                                "professional-one@pec.it",
                                "professional-two@pec.it"
                        )
                )
        );

        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(2, payload.getPayload().getDigitalAddress().size());

        assertTrue( payload.getPayload().getDigitalAddress().stream() .anyMatch(address -> "professional-one@pec.it".equals( address.getAddress() ) && "PROFESSIONISTA".equals( address.getRecipient() ) ) );

        assertTrue( payload.getPayload().getDigitalAddress().stream() .anyMatch(address -> "professional-two@pec.it".equals( address.getAddress() ) && "PROFESSIONISTA".equals( address.getRecipient() ) ) );

        verifyNoInteractions(inadClient);
    }

    /**
     * IPA NotFound -> old IniPEC con PEC impresa invalida e professionale valida.
     */
    @Test
    void PG_Digital_FullFlow_OldInipec_InvalidBusinessPecKeepsValidProfessionalPec() {
        String correlationId =
                "pg-old-inipec-invalid-business-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                PG_BUSINESS_PROFESSIONAL
        );

        GatewayResponse payload = completeOldInipecFinal(
                correlationId,
                inipecPollingResponse(
                        pecResponseWithProfessionals(
                                PG_BUSINESS_PROFESSIONAL,
                                "not-an-email",
                                "professional@pec.it"
                        )
                )
        );

        assertEquals(correlationId, payload.getPayload().getCorrelationId());
        assertEquals("INIPEC", payload.getPayload().getRegistry());
        assertEquals(1, payload.getPayload().getDigitalAddress().size());
        assertEquals( "professional@pec.it", payload.getPayload().getDigitalAddress().getFirst().getAddress() );
        assertEquals( "PROFESSIONISTA", payload.getPayload().getDigitalAddress().getFirst().getRecipient() );

        verifyNoInteractions(inadClient);
    }

    /**
     * Fallback PF attivo: il flusso PG legacy non cambia.
     */
    @Test
    void PG_Digital_FullFlow_OldInipec_IsUnchangedWhenPfFallbackIsEnabled() {
        String correlationId =
                "pg-old-inipec-pf-fallback-enabled-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                VALID_PG_PIVA
        );

        Pec nf = pecResponse(
                VALID_PG_PIVA,
                "ignored@pec.it"
        );
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);

        when(inadClient.callEService(
                eq(VALID_PG_PIVA),
                startsWith(correlationId)
        )).thenReturn(
                Mono.just(
                        inadResponse(
                                VALID_PG_PIVA,
                                "pg-old-inipec-pf-fallback-enabled@pec.it",
                                null
                        )
                )
        );

        GatewayResponse payload = completeOldInipecFinal(
                correlationId,
                inipecPollingResponse(nf)
        );

        assertInadPayload(
                correlationId,
                payload,
                "pg-old-inipec-pf-fallback-enabled@pec.it"
        );
    }

    /**
     * IPA NotFound -> old IniPEC NF -> INAD NotFound -> risultato vuoto.
     */
    @Test
    void PG_Digital_FullFlow_OldInipecNf_InadNotFoundReturnsEmptyResult() {
        String correlationId =
                "pg-old-inipec-inad-not-found-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                VALID_PG_PIVA
        );

        String pollingId =
                "pg-old-inipec-polling-" + UUID.randomUUID();

        when(infoCamereClient.callEServiceRequestId(
                any(IniPecBatchRequest.class)
        )).thenReturn(
                Mono.just(
                        iniPecBatchResponse(pollingId)
                )
        );

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> legacyRequest =
                oldInipecRequest(correlationId);

        String batchId =
                legacyRequest.get("batchId").s();

        Pec nf = pecResponse(
                VALID_PG_PIVA,
                "ignored@pec.it"
        );
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);

        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(
                        Mono.just(
                                inipecPollingResponse(nf)
                        )
                );

        when(inadClient.callEService(
                eq(VALID_PG_PIVA),
                startsWith(correlationId)
        )).thenReturn(
                Mono.error(
                        inadNotFoundException()
                )
        );

        digitalAddressBatchPollingService.batchPecPolling();

        legacyRequest =
                oldInipecRequest(correlationId);

        assertEquals( "WORKED", legacyRequest.get("status").s() );
        assertEquals( "INAD", legacyRequest.get("eservice").s() );
        assertNotNull( legacyRequest.get("message") );

        GatewayResponse payload =
                readNewOutputMessage(
                        legacyRequest.get("message").s()
                );

        assertEquals( correlationId, payload.getPayload().getCorrelationId() );
        assertEquals( "INAD", payload.getPayload().getRegistry() );
        assertNotNull( payload.getPayload().getDigitalAddress() );
        assertTrue( payload.getPayload().getDigitalAddress().isEmpty() );

        assertLegacyPolling(
                batchId,
                pollingId,
                "WORKED",
                0,
                0
        );
    }

    /**
     * IPA NotFound -> old IniPEC NF -> errore tecnico INAD.
     */
    @Test
    void PG_Digital_FullFlow_OldInipecNf_InadTechnicalErrorMarksLegacyRequestError() {
        String correlationId =
                "pg-old-inipec-inad-error-" + UUID.randomUUID();

        startOldInipecFromIpaNotFound(
                correlationId,
                VALID_PG_PIVA
        );

        String pollingId =
                "pg-old-inipec-polling-" + UUID.randomUUID();

        when(infoCamereClient.callEServiceRequestId(
                any(IniPecBatchRequest.class)
        )).thenReturn(
                Mono.just(
                        iniPecBatchResponse(pollingId)
                )
        );

        iniPecBatchRequestService.batchPecRequest();

        Pec nf = pecResponse(
                VALID_PG_PIVA,
                "ignored@pec.it"
        );
        nf.setStatoImpresa(Pec.StatoImpresaEnum.NF);

        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(
                        Mono.just(
                                inipecPollingResponse(nf)
                        )
                );

        when(inadClient.callEService(
                eq(VALID_PG_PIVA),
                startsWith(correlationId)
        )).thenReturn(
                Mono.error(
                        new IllegalStateException(
                                "INAD unavailable"
                        )
                )
        );

        digitalAddressBatchPollingService.batchPecPolling();

        Map<String, AttributeValue> legacyRequest =
                oldInipecRequest(correlationId);

        assertEquals( "ERROR", legacyRequest.get("status").s() );
        assertEquals( "INAD", legacyRequest.get("eservice").s() );
        assertNull( legacyRequest.get("message") );
    }

    private Map<String, AttributeValue> findLegacyPolling(
            String batchId
    ) {
        return dynamoDbClient.scan(
                        ScanRequest.builder()
                                .tableName(BATCH_POLLING_TABLE)
                                .filterExpression(
                                        "batchId = :batchId"
                                )
                                .expressionAttributeValues(
                                        Map.of(
                                                ":batchId",
                                                AttributeValue.builder()
                                                        .s(batchId)
                                                        .build()
                                        )
                                )
                                .build()
                )
                .items()
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private void assertLegacyPolling(
            String batchId,
            String pollingId,
            String status,
            int retry,
            int inProgressRetry
    ) {
        Map<String, AttributeValue> polling =
                awaitLegacyPolling(
                        batchId,
                        status,
                        retry,
                        inProgressRetry
                );

        assertEquals( pollingId, polling.get("pollingId").s() );
    }

    private Map<String, AttributeValue> awaitLegacyPolling(
            String batchId,
            String status,
            int retry,
            int inProgressRetry
    ) {
        Map<String, AttributeValue> polling = null;

        for (int i = 0; i < 50; i++) {
            polling =
                    findLegacyPolling(batchId);

            if (
                    status.equals(
                            polling.get("status").s()
                    )
                            && String.valueOf(retry).equals(
                            polling.get("retry").n()
                    )
                            && String.valueOf(
                            inProgressRetry
                    ).equals(
                            polling.get(
                                    "inProgressRetry"
                            ).n()
                    )
            ) {
                return polling;
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }

        fail( "Unexpected legacy polling state for batchId " + batchId + ": status=" + polling.get("status").s() + ", retry=" + polling.get("retry").n() + ", inProgressRetry=" + polling.get("inProgressRetry").n() );

        return polling;
    }

    private void startOldInipecFromIpaNotFound(
            String correlationId,
            String taxId
    ) {
        when(ipaClient.callEServiceWS23(eq(taxId), any()))
                .thenReturn(
                        Mono.just(
                                ipaNotFoundResponse()
                        )
                );

        AddressOKDto response = enqueueGatewayRequest(
                "PG",
                taxId,
                AddressRequestBodyFilterDto.DomicileTypeEnum.DIGITAL,
                correlationId
        );

        processNextGatewayInputMessage();

        assertEquals( correlationId, response.getCorrelationId() );

        assertTrue( findNationalRegistriesRequestsByCorrelationId(correlationId) .isEmpty() );

        assertOldInipecPendingRequest(
                correlationId,
                taxId
        );

        assertNull( receiveSingleMessage(OUTPUT_QUEUE) );
    }

    private GatewayResponse completeOldInipecFinal(
            String correlationId,
            IniPecPollingResponse pollingResponse
    ) {
        String pollingId =
                "pg-old-inipec-polling-" + UUID.randomUUID();

        when(infoCamereClient.callEServiceRequestId(
                any(IniPecBatchRequest.class)
        )).thenReturn(
                Mono.just(
                        iniPecBatchResponse(pollingId)
                )
        );

        iniPecBatchRequestService.batchPecRequest();

        Map<String, AttributeValue> legacyRequest =
                oldInipecRequest(correlationId);

        assertEquals( "WORKING", legacyRequest.get("status").s() );

        when(infoCamereClient.callEServiceRequestPec(pollingId))
                .thenReturn(
                        Mono.just(pollingResponse)
                );

        digitalAddressBatchPollingService.batchPecPolling();

        legacyRequest =
                oldInipecRequest(correlationId);

        assertEquals( "WORKED", legacyRequest.get("status").s() );

        assertNotNull( legacyRequest.get("message") );

        return readNewOutputMessage(
                legacyRequest.get("message").s()
        );
    }

    private void assertOldInipecPendingRequest(
            String correlationId,
            String taxId
    ) {
        List<Map<String, AttributeValue>> legacyRequests =
                findBatchRequestsByCorrelationPrefix(correlationId);

        assertEquals( 1, legacyRequests.size() );

        Map<String, AttributeValue> legacyRequest =
                legacyRequests.getFirst();

        assertTrue( legacyRequest.get("correlationId") .s() .startsWith(correlationId + "~") );

        assertEquals( taxId, legacyRequest.get("cf").s() );

        assertEquals( "PG", legacyRequest.get("recipientType").s() );

        assertEquals( "NOT_WORKED", legacyRequest.get("status").s() );
    }

    private Map<String, AttributeValue> oldInipecRequest(
            String correlationId
    ) {
        return findBatchRequestsByCorrelationPrefix(correlationId)
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private void assertInadPayload(
            String correlationId,
            GatewayResponse payload,
            String expectedAddress
    ) {
        assertEquals( correlationId, payload.getPayload().getCorrelationId() );

        assertEquals( "INAD", payload.getPayload().getRegistry() );

        assertEquals( 1, payload.getPayload().getDigitalAddress().size() );

        assertEquals( expectedAddress, payload.getPayload().getDigitalAddress() .getFirst() .getAddress() );

        assertEquals( "IMPRESA", payload.getPayload().getDigitalAddress() .getFirst() .getRecipient() );
    }

    private IniPecPollingResponse inipecPollingResponse(
            Pec... pecs
    ) {
        IniPecPollingResponse response =
                new IniPecPollingResponse();

        response.setElencoPec(
                List.of(pecs)
        );

        return response;
    }

    private Pec pecResponseWithProfessionals(
            String taxId,
            String pecImpresa,
            String... professionalPecs
    ) {
        Map<String, Object> value =
                new HashMap<>();

        value.put(
                "cf",
                taxId
        );

        if (pecImpresa != null) {
            value.put(
                    "pecImpresa",
                    pecImpresa
            );
        }

        value.put(
                "pecProfessionista",
                Arrays.stream(professionalPecs)
                        .map(pec ->
                                Map.of(
                                        "pec",
                                        pec
                                )
                        )
                        .toList()
        );

        return objectMapper.convertValue(
                value,
                Pec.class
        );
    }

    private WS23ResponseDto ipaMultipleResponse() {
        ResultDto result =
                new ResultDto();

        result.setNumItems(2);
        result.setCodErr(0);
        result.setDescErr("");

        DataWS23Dto first =
                new DataWS23Dto();

        first.setCodAmm("cod-amm-1");
        first.setDesAmm("Ente 1");
        first.setTipo("PA");
        first.setDomicilioDigitale(
                "first@pec.it"
        );

        DataWS23Dto second =
                new DataWS23Dto();

        second.setCodAmm("cod-amm-2");
        second.setDesAmm("Ente 2");
        second.setTipo("PA");
        second.setDomicilioDigitale(
                "second@pec.it"
        );

        WS23ResponseDto response =
                new WS23ResponseDto();

        response.setResult(result);
        response.setData(
                List.of(
                        first,
                        second
                )
        );

        return response;
    }

    private WS05ResponseDto ipaWs05Response(
            String email
    ) {
        ResultDto result =
                new ResultDto();

        result.setNumItems(1);
        result.setCodErr(0);
        result.setDescErr("");

        DataWS05Dto data =
                new DataWS05Dto();

        data.setCodAmm(
                "cod-amm-1"
        );
        data.setDesAmm(
                "Ente 1"
        );
        data.setMail1(email);

        WS05ResponseDto response =
                new WS05ResponseDto();

        response.setResult(result);
        response.setData(data);

        return response;
    }
}
