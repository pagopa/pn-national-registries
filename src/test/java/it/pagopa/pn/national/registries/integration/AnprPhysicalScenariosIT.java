package it.pagopa.pn.national.registries.integration;

import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RichiestaE002;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.RispostaE002OK;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoDatiSoggettiEnte;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoListaSoggetti;
import it.pagopa.pn.national.registries.generated.openapi.msclient.anpr.v1.dto.TipoResidenza;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.national.registries.model.CodeSqsDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = PnNationalRegistriesTestApplication.class)
class AnprPhysicalScenariosIT extends AbstractNationalRegistriesComponentIT {

    @Test
    void PF_Physical_ANPR_WithoutUsefulResidences() {
        String correlationId = "corr-no-residence-" + UUID.randomUUID();
        RispostaE002OK response = anprResponse("RSSMRA80A01H501U", "VIA", "ROMA", "10", "Roma", "RM", "00100");
        when(anprClient.callEService(any())).thenReturn(Mono.just(response));

        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        CodeSqsDto payload = output();
        assertEquals(correlationId, payload.getCorrelationId());
        assertEquals("ANPR", payload.getRegistry());
        assertNull(payload.getPhysicalAddress());
    }

    @Test
    void PF_Physical_ANPR_ForeignResidence() throws Exception {
        String correlationId = "corr-foreign-" + UUID.randomUUID();
        RispostaE002OK response = anprResponse(VALID_PF_CF, "VIA", "IGNORED", "1", "Roma", "RM", "00100");
        TipoResidenza residence = response.getListaSoggetti().getDatiSoggetto().getFirst().getResidenza().getFirst();
        residence.setIndirizzo(null);

        Object foreignLocation = newForSetter(residence, "setLocalitaEstera");
        Object foreignAddress = newForSetter(foreignLocation, "setIndirizzoEstero");
        Object foreignToponym = newForSetter(foreignAddress, "setToponimo");
        Object locality = newForSetter(foreignAddress, "setLocalita");
        set(foreignToponym, "setDenominazione", "RUE DE PARIS");
        set(foreignToponym, "setNumeroCivico", "12");
        set(foreignAddress, "setCap", "75001");
        set(locality, "setDescrizioneStato", "FRANCIA");
        set(locality, "setDescrizioneLocalita", "Parigi");
        set(locality, "setProvinciaContea", "Ile-de-France");
        set(foreignAddress, "setToponimo", foreignToponym);
        set(foreignAddress, "setLocalita", locality);
        set(foreignLocation, "setIndirizzoEstero", foreignAddress);
        set(residence, "setLocalitaEstera", foreignLocation);
        when(anprClient.callEService(any())).thenReturn(Mono.just(response));

        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        CodeSqsDto payload = output();
        assertNotNull(payload.getPhysicalAddress());
        assertEquals("RUE DE PARIS 12", payload.getPhysicalAddress().getAddress());
        assertEquals("75001", payload.getPhysicalAddress().getZip());
        assertEquals("Parigi", payload.getPhysicalAddress().getMunicipality());
        assertEquals("Ile-de-France", payload.getPhysicalAddress().getProvince());
        assertEquals("FRANCIA", payload.getPhysicalAddress().getForeignState());
    }

    @Test
    void PF_Physical_ANPR_MultipleSubjectsUsesOnlyRequestedTaxId() {
        String correlationId = "corr-multiple-subjects-" + UUID.randomUUID();
        RispostaE002OK wrong = anprResponse("RSSMRA80A01H501U", "VIA", "SBAGLIATA", "1", "Roma", "RM", "00100");
        RispostaE002OK correct = anprResponse(VALID_PF_CF, "VIA", "CORRETTA", "99", "Milano", "MI", "20100");
        TipoDatiSoggettiEnte wrongSubject = wrong.getListaSoggetti().getDatiSoggetto().getFirst();
        TipoDatiSoggettiEnte correctSubject = correct.getListaSoggetti().getDatiSoggetto().getFirst();
        TipoListaSoggetti subjects = new TipoListaSoggetti();
        subjects.setDatiSoggetto(List.of(wrongSubject, correctSubject));
        correct.setListaSoggetti(subjects);
        when(anprClient.callEService(any())).thenReturn(Mono.just(correct));

        enqueueGatewayRequest("PF", VALID_PF_CF.toLowerCase(), AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId);
        processNextGatewayInputMessage();

        CodeSqsDto payload = output();
        assertNotNull(payload.getPhysicalAddress());
        assertEquals("VIA CORRETTA 99", payload.getPhysicalAddress().getAddress());
    }

    @Test
    void PF_Physical_ANPR_CounterAndRequestFieldsAreMapped() {
        String correlationId1 = "corr-request-1-" + UUID.randomUUID();
        String correlationId2 = "corr-request-2-" + UUID.randomUUID();
        when(anprClient.callEService(any())).thenReturn(
                Mono.just(anprResponse(VALID_PF_CF, "VIA", "ROMA", "10", "Roma", "RM", "00100")),
                Mono.just(anprResponse(VALID_PF_CF, "VIA", "MILANO", "20", "Milano", "MI", "20100"))
        );

        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId1);
        enqueueGatewayRequest("PF", VALID_PF_CF, AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL, correlationId2);
        processNextGatewayInputMessage();
        processNextGatewayInputMessage();
        assertNotNull(output());
        assertNotNull(output());

        ArgumentCaptor<RichiestaE002> captor = ArgumentCaptor.forClass(RichiestaE002.class);
        verify(anprClient, times(2)).callEService(captor.capture());
        List<RichiestaE002> requests = captor.getAllValues();
        assertEquals(2, requests.size());
        assertEquals(2, Set.of(requests.get(0).getIdOperazioneClient(), requests.get(1).getIdOperazioneClient()).size());
        assertEquals(Set.of(correlationId1, correlationId2), Set.of(
                requests.get(0).getDatiRichiesta().getMotivoRichiesta(),
                requests.get(1).getDatiRichiesta().getMotivoRichiesta()
        ));
        assertTrue(requests.stream().allMatch(request -> "C001".equals(request.getDatiRichiesta().getCasoUso())));
        assertTrue(requests.stream().allMatch(request -> "2026-10-10".equals(request.getDatiRichiesta().getDataRiferimentoRichiesta())));
        assertTrue(requests.stream().allMatch(request -> VALID_PF_CF.equalsIgnoreCase(request.getCriteriRicerca().getCodiceFiscale())));
    }

    private CodeSqsDto output() {
        Message message = receiveSingleMessage(OUTPUT_QUEUE);
        assertNotNull(message);
        return readOutputMessage(message.body());
    }

    private Object newForSetter(Object target, String setterName) throws Exception {
        Method setter = findSetter(target, setterName);
        return setter.getParameterTypes()[0].getDeclaredConstructor().newInstance();
    }

    private void set(Object target, String setterName, Object value) throws Exception {
        findSetter(target, setterName).invoke(target, value);
    }

    private Method findSetter(Object target, String setterName) {
        return Arrays.stream(target.getClass().getMethods())
                .filter(method -> setterName.equals(method.getName()) && method.getParameterCount() == 1)
                .findFirst()
                .orElseThrow();
    }
}
