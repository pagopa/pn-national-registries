package it.pagopa.pn.national.registries.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.national.registries.BaseTest;
import it.pagopa.pn.national.registries.client.agenziaentrate.AdELegalClient;
import it.pagopa.pn.national.registries.client.agenziaentrate.CheckCfClient;
import it.pagopa.pn.national.registries.client.anpr.AnprClient;
import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.client.infocamere.InfoCamereClient;
import it.pagopa.pn.national.registries.client.ipa.IpaClient;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.api.VerificheApi;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RispostaE002OK;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoCodiceFiscale;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoComune;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoDatiSoggettiEnte;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoGeneralita;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoIndirizzo;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoListaSoggetti;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoNumeroCivico;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoResidenza;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoToponimo;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.MotivationTermination;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.UsageInfo;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.AddressRegistroImprese;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecBatchResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.IniPecPollingResponse;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.LegalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.infocamere.v1.dto.Pec;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS23Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.ResultDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS23ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressIniPECOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressIniPECRequestBodyFilterDto;
import it.pagopa.pn.national.registries.middleware.queue.consumer.GatewayInputsHandler;
import it.pagopa.pn.national.registries.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.national.registries.model.InternalCodeSqsDto;
import it.pagopa.pn.national.registries.service.DigitalAddressBatchPollingService;
import it.pagopa.pn.national.registries.service.IniPecBatchRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@TestPropertySource(properties = {
        "spring.cloud.aws.sqs.enabled=false",
        "pn.national-registries.val-cx-id-enabled=true",
        "pn.national-registries.query-limit=20",
        "pn.national.registries.anpr.table=pn-counter",
        "pn.national-registries.dao.requestsTableName=pn-NationalRegistries-Requests",
        "pn.national-registries.dao.shedlockTableName=pn-nationalRegistries-ShedLock",
        "pn.national-registries.inipec.ttl=1209600",
        "pn.national-registries.inipec.batchrequest-pk-separator=~",
        "pn.national-registries.inipec.batch-request-max-retry=3",
        "pn.national-registries.inipec.max-batch-request-size=6250",
        "pn.national-registries.inipec.batch-request-recovery-after=3600",
        "pn.national.registries.inipec.batch.request.delay=30000",
        "pn.national-registries.inipec.batch.request.recovery.delay=30000",
        "pn.national-registries.inipec.batch.polling.max-retry=3",
        "pn.national-registries.inipec.batch.polling.inprogress.max-retry=24",
        "pn.national-registries.inipec.first-attempt-delay-seconds-per-cf=0",
        "pn.national-registries.inipec.first-attempt-fixed-delay-seconds=0",
        "pn.national.registries.pdnd.base-path=http://localhost:1080/nationalregistriesmock/",
        "pn.national.registries.anpr.base-path=http://localhost:1080/nationalregistriesmock/",
        "pn.national.registries.ade-legal.base-path=http://localhost:1080/nationalregistriesmock/",
        "pn.national.registries.ade-check-cf.base-path=http://localhost:1080/nationalregistriesmock/",
        "pn.national.registries.inad.base-path=http://localhost:1080/nationalregistriesmock/",
        "pn.national.registries.infocamere.base-path=http://localhost:1080/nationalregistriesmock/ic/ce/wspa/wspa/rest/",
        "pn.national.registries.ipa.base-path=http://localhost:1080/nationalregistriesmock/",
        "pn.national.registries.pdnd.anpr.secret=pn-national-registries/pdnd/ANPR",
        "pn.national.registries.trust.anpr.secret=pn-national-registries/anpr/auth",
        "pn.national.registries.ade.legal.trust.secret=pn-national-registries/ade/auth",
        "pn.national.registries.pdnd.ade-check-cf.secret=pn-national-registries/pdnd/CheckCF",
        "pn.national.registries.trust.ade-check-cf.secret=pn-national-registries/CheckCF/auth-rest",
        "pn.national.registries.pdnd.inad.secret=pn-national-registries/pdnd/INAD",
        "pn.national.registries.ssm.infocamere.auth-rest=/pn-national-registries/infocamere-cert",
        "pn.national.registries.infocamere.client-id=a7e152cac460917f3123cc2410f5a8d2",
        "pn.national.registries.ipa.secret=pn-national-registries/ipa",
        "pn.national.registries.sqs.output.queue.name=pn-national_registry_gateway_outputs",
        "pn.national.registries.sqs.input.queue.name=pn-national_registry_gateway_inputs",
        "pn.national.registries.sqs.input.dlq.queue.name=pn-national_registry_gateway_inputs_DLQ",
        "pn.national.registries.health-check-path=http://localhost"
})
abstract class AbstractNationalRegistriesComponentIT extends BaseTest.WithLocalStack {

    protected static final String INPUT_QUEUE = "pn-national_registry_gateway_inputs";
    protected static final String OUTPUT_QUEUE = "pn-national_registry_gateway_outputs";
    protected static final String INPUT_DLQ_QUEUE = "pn-national_registry_gateway_inputs_DLQ";
    protected static final String BATCH_REQUESTS_TABLE = "pn-batchRequests";
    protected static final String BATCH_POLLING_TABLE = "pn-batchPolling";
    protected static final String NATIONAL_REGISTRIES_REQUESTS_TABLE = "pn-NationalRegistries-Requests";
    protected static final String COUNTER_TABLE = "pn-counter";
    protected static final String CX_ID = "pn-delivery";
    protected static final String VALID_PF_CF = "PPPPLT80A01H501V";
    protected static final String VALID_PG_PIVA = "75120855137";

    @MockitoBean
    protected CheckCfClient checkCfClient;

    @MockitoBean
    protected AdELegalClient adELegalClient;

    @MockitoBean
    protected AnprClient anprClient;

    @MockitoBean
    protected InadClient inadClient;

    @MockitoBean
    protected InfoCamereClient infoCamereClient;

    @MockitoBean
    protected IpaClient ipaClient;

    @MockitoBean
    protected VerificheApi verificheApi;

    @Autowired
    protected org.springframework.context.ApplicationContext applicationContext;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected SqsAsyncClient sqsAsyncClient;

    @Autowired
    protected DynamoDbClient dynamoDbClient;

    @Autowired
    protected GatewayInputsHandler gatewayInputsHandler;

    @Autowired
    protected IniPecBatchRequestService iniPecBatchRequestService;

    @Autowired
    protected DigitalAddressBatchPollingService digitalAddressBatchPollingService;

    protected WebTestClient webTestClient;

    @BeforeEach
    void initComponentBase() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
                .configureClient()
                .responseTimeout(Duration.ofSeconds(10))
                .build();

        drainQueue(INPUT_QUEUE);
        drainQueue(OUTPUT_QUEUE);
        drainQueue(INPUT_DLQ_QUEUE);
        clearTable(BATCH_REQUESTS_TABLE, "correlationId");
        clearTable(BATCH_POLLING_TABLE, "batchId");
        clearTable(NATIONAL_REGISTRIES_REQUESTS_TABLE, "correlationId", "createdAt");
        clearTable(COUNTER_TABLE, "eservice");
    }

    protected AddressOKDto enqueueGatewayRequest(String recipientType, String taxId, AddressRequestBodyFilterDto.DomicileTypeEnum domicileType, String correlationId) {
        return webTestClient.post()
                .uri("/national-registries-private/" + recipientType + "/addresses")
                .header("pn-national-registries-cx-id", CX_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildGatewayRequest(correlationId, taxId, domicileType))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AddressOKDto.class)
                .returnResult()
                .getResponseBody();
    }

    protected GetDigitalAddressIniPECOKDto enqueueIniPecRequest(String correlationId, String taxId) {
        return webTestClient.post()
                .uri("/national-registries-private/inipec/digital-address")
                .header("pn-national-registries-cx-id", CX_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildIniPecRequest(correlationId, taxId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(GetDigitalAddressIniPECOKDto.class)
                .returnResult()
                .getResponseBody();
    }

    protected void processNextGatewayInputMessage() {
        Message message = receiveSingleMessage(INPUT_QUEUE);
        org.junit.jupiter.api.Assertions.assertNotNull(message, "Expected one input queue message");
        PnAddressGatewayEvent.Payload payload = readInputMessage(message.body());
        gatewayInputsHandler.pnNationalRegistriesGatewayRequestConsumer(MessageBuilder.withPayload(toGatewayPayload(payload)).build());
    }

    protected RuntimeException processNextGatewayInputMessageExpectingFailure() {
        Message message = receiveSingleMessage(INPUT_QUEUE);
        org.junit.jupiter.api.Assertions.assertNotNull(message, "Expected one input queue message");
        PnAddressGatewayEvent.Payload payload = readInputMessage(message.body());
        return org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> gatewayInputsHandler.pnNationalRegistriesGatewayRequestConsumer(MessageBuilder.withPayload(toGatewayPayload(payload)).build()));
    }

    protected PnAddressGatewayEvent.Payload toGatewayPayload(PnAddressGatewayEvent.Payload payload) {
        return PnAddressGatewayEvent.Payload.builder()
                .correlationId(payload.getCorrelationId())
                .taxId(payload.getTaxId())
                .referenceRequestDate(payload.getReferenceRequestDate())
                .domicileType(payload.getDomicileType())
                .recipientType(payload.getRecipientType())
                .pnNationalRegistriesCxId(payload.getPnNationalRegistriesCxId())
                .build();
    }

    protected AddressRequestBodyDto buildGatewayRequest(String correlationId, String taxId, AddressRequestBodyFilterDto.DomicileTypeEnum domicileType) {
        AddressRequestBodyFilterDto filter = new AddressRequestBodyFilterDto();
        filter.setCorrelationId(correlationId);
        filter.setTaxId(taxId);
        filter.setDomicileType(domicileType);
        filter.setReferenceRequestDate(Date.from(Instant.parse("2026-10-10T10:15:30Z")));

        AddressRequestBodyDto requestBody = new AddressRequestBodyDto();
        requestBody.setFilter(filter);
        return requestBody;
    }

    protected GetDigitalAddressIniPECRequestBodyDto buildIniPecRequest(String correlationId, String taxId) {
        GetDigitalAddressIniPECRequestBodyFilterDto filter = new GetDigitalAddressIniPECRequestBodyFilterDto();
        filter.setCorrelationId(correlationId);
        filter.setTaxId(taxId);

        GetDigitalAddressIniPECRequestBodyDto requestBody = new GetDigitalAddressIniPECRequestBodyDto();
        requestBody.setFilter(filter);
        return requestBody;
    }

    protected PnAddressGatewayEvent.Payload readInputMessage(String messageBody) {
        try {
            return objectMapper.readValue(messageBody, PnAddressGatewayEvent.Payload.class);
        } catch (JsonProcessingException e) {
            throw new AssertionError("Unable to deserialize input queue message", e);
        }
    }

    protected it.pagopa.pn.national.registries.model.CodeSqsDto readOutputMessage(String messageBody) {
        try {
            return objectMapper.readValue(messageBody, it.pagopa.pn.national.registries.model.CodeSqsDto.class);
        } catch (JsonProcessingException e) {
            throw new AssertionError("Unable to deserialize output queue message", e);
        }
    }

    protected List<Map<String, AttributeValue>> findNationalRegistriesRequestsByCorrelationId(String correlationId) {
        return dynamoDbClient.scan(ScanRequest.builder()
                        .tableName(NATIONAL_REGISTRIES_REQUESTS_TABLE)
                        .filterExpression("correlationId = :correlationId")
                        .expressionAttributeValues(Map.of(":correlationId", AttributeValue.builder().s(correlationId).build()))
                        .build())
                .items();
    }

    protected Map<String, AttributeValue> getBatchRequestItem(String correlationId) {
        return dynamoDbClient.getItem(builder -> builder
                        .tableName(BATCH_REQUESTS_TABLE)
                        .key(Map.of("correlationId", AttributeValue.builder().s(correlationId).build())))
                .item();
    }

    protected Message receiveSingleMessage(String queueName) {
        List<Message> messages = receiveMessages(queueName, 1);
        if (messages.isEmpty()) {
            return null;
        }
        Message message = messages.get(0);
        deleteMessage(queueName, message);
        return message;
    }

    protected void drainQueue(String queueName) {
        List<Message> messages;
        do {
            messages = receiveMessages(queueName, 10);
            messages.forEach(message -> deleteMessage(queueName, message));
        } while (!messages.isEmpty());
    }

    protected List<Map<String, AttributeValue>> findBatchRequestsByCorrelationPrefix(String correlationPrefix) {
        return dynamoDbClient.scan(ScanRequest.builder()
                        .tableName(BATCH_REQUESTS_TABLE)
                        .filterExpression("begins_with(correlationId, :prefix)")
                        .expressionAttributeValues(Map.of(":prefix", AttributeValue.builder().s(correlationPrefix).build()))
                        .build())
                .items();
    }

    protected List<Message> receiveMessages(String queueName, int maxNumberOfMessages) {
        String queueUrl = sqsAsyncClient.getQueueUrl(request -> request.queueName(queueName)).join().queueUrl();
        return new ArrayList<>(sqsAsyncClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(maxNumberOfMessages)
                        .waitTimeSeconds(1)
                        .messageAttributeNames("All")
                        .build())
                .join()
                .messages());
    }

    protected void deleteMessage(String queueName, Message message) {
        String queueUrl = sqsAsyncClient.getQueueUrl(request -> request.queueName(queueName)).join().queueUrl();
        sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build()).join();
    }

    protected void clearTable(String tableName, String partitionKey) {
        dynamoDbClient.scanPaginator(ScanRequest.builder().tableName(tableName).build())
                .items()
                .forEach(item -> dynamoDbClient.deleteItem(builder -> builder
                        .tableName(tableName)
                        .key(Map.of(partitionKey, item.get(partitionKey)))));
    }

    protected void clearTable(String tableName, String partitionKey, String sortKey) {
        dynamoDbClient.scanPaginator(ScanRequest.builder().tableName(tableName).build())
                .items()
                .forEach(item -> dynamoDbClient.deleteItem(builder -> builder
                        .tableName(tableName)
                        .key(Map.of(partitionKey, item.get(partitionKey), sortKey, item.get(sortKey)))));
    }

    protected PnNationalRegistriesException inadNotFoundException() {
        return new PnNationalRegistriesException(
                "CF non trovato",
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                null,
                null,
                Charset.defaultCharset(),
                null
        );
    }

    protected ResponseRequestDigitalAddress inadResponse(String taxId, String address, String practicedProfession) {
        ElementDigitalAddress digitalAddress = new ElementDigitalAddress();
        digitalAddress.setDigitalAddress(address);
        digitalAddress.setPracticedProfession(practicedProfession);

        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        usageInfo.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        digitalAddress.setUsageInfo(usageInfo);

        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale(taxId);
        response.setSince(Date.from(Instant.parse("2026-10-10T10:15:30Z")));
        response.setDigitalAddress(List.of(digitalAddress));
        return response;
    }

    protected ResponseRequestDigitalAddress inadResponse(String taxId, List<ElementDigitalAddress> digitalAddresses) {
        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale(taxId);
        response.setSince(Date.from(Instant.parse("2026-10-10T10:15:30Z")));
        response.setDigitalAddress(digitalAddresses);
        return response;
    }

    protected ElementDigitalAddress inadAddress(String address, String practicedProfession) {
        ElementDigitalAddress digitalAddress = new ElementDigitalAddress();
        digitalAddress.setDigitalAddress(address);
        digitalAddress.setPracticedProfession(practicedProfession);

        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        usageInfo.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        digitalAddress.setUsageInfo(usageInfo);
        return digitalAddress;
    }

    protected AddressRegistroImprese registroImpreseAddress(String taxId, String toponimo, String via, String civico, String municipality, String province, String zip) {
        LegalAddress legalAddress = new LegalAddress();
        legalAddress.setToponimo(toponimo);
        legalAddress.setVia(via);
        legalAddress.setnCivico(civico);
        legalAddress.setComune(municipality);
        legalAddress.setProvincia(province);
        legalAddress.setCap(zip);
        legalAddress.setStato("IT");

        AddressRegistroImprese response = new AddressRegistroImprese();
        response.setCf(taxId);
        response.setIndirizzoLocalizzazione(legalAddress);
        return response;
    }

    protected RispostaE002OK anprResponse(String taxId, String species, String street, String civicNumber, String municipality, String province, String zip) {
        TipoCodiceFiscale codiceFiscale = new TipoCodiceFiscale();
        codiceFiscale.setCodFiscale(taxId);

        TipoGeneralita generalita = new TipoGeneralita();
        generalita.setCodiceFiscale(codiceFiscale);

        TipoToponimo toponimo = new TipoToponimo();
        toponimo.setSpecie(species);
        toponimo.setDenominazioneToponimo(street);

        TipoNumeroCivico numeroCivico = new TipoNumeroCivico();
        numeroCivico.setNumero(civicNumber);

        TipoComune comune = new TipoComune();
        comune.setNomeComune(municipality);
        comune.setSiglaProvinciaIstat(province);

        TipoIndirizzo indirizzo = new TipoIndirizzo();
        indirizzo.setToponimo(toponimo);
        indirizzo.setNumeroCivico(numeroCivico);
        indirizzo.setComune(comune);
        indirizzo.setCap(zip);

        TipoResidenza residenza = new TipoResidenza();
        residenza.setIndirizzo(indirizzo);
        residenza.setTipoIndirizzo("RESIDENZA");
        residenza.setDataDecorrenzaResidenza("2026-10-10");

        TipoDatiSoggettiEnte soggetto = new TipoDatiSoggettiEnte();
        soggetto.setGeneralita(generalita);
        soggetto.setResidenza(List.of(residenza));

        TipoListaSoggetti listaSoggetti = new TipoListaSoggetti();
        listaSoggetti.setDatiSoggetto(List.of(soggetto));

        RispostaE002OK response = new RispostaE002OK();
        response.setIdOperazioneANPR("operation-" + UUID.randomUUID());
        response.setListaSoggetti(listaSoggetti);
        return response;
    }

    protected WS23ResponseDto ipaFoundResponse(String taxId, String email) {
        ResultDto result = new ResultDto();
        result.setNumItems(1);
        result.setCodErr(0);
        result.setDescErr("");

        DataWS23Dto data = new DataWS23Dto();
        data.setCodAmm("cod-amm-1");
        data.setDesAmm("Comune di Test");
        data.setTipo("PA");
        data.setDomicilioDigitale(email);

        WS23ResponseDto response = new WS23ResponseDto();
        response.setResult(result);
        response.setData(List.of(data));
        return response;
    }

    protected WS23ResponseDto ipaNotFoundResponse() {
        ResultDto result = new ResultDto();
        result.setNumItems(0);
        result.setCodErr(0);
        result.setDescErr("");

        WS23ResponseDto response = new WS23ResponseDto();
        response.setResult(result);
        response.setData(List.of());
        return response;
    }

    protected IniPecBatchResponse iniPecBatchResponse(String pollingId) {
        IniPecBatchResponse response = new IniPecBatchResponse();
        response.setIdentificativoRichiesta(pollingId);
        return response;
    }

    protected IniPecPollingResponse iniPecPollingResponse(Pec... pecs) {
        IniPecPollingResponse response = new IniPecPollingResponse();
        response.setElencoPec(List.of(pecs));
        return response;
    }

    protected Pec pecResponse(String taxId, String pecImpresa) {
        Pec pec = new Pec();
        pec.setCf(taxId);
        pec.setPecImpresa(pecImpresa);
        return pec;
    }
}

