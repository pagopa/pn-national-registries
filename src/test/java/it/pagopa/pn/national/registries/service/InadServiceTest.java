package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.MotivationTermination;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.UsageInfo;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADRequestBodyDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.GetDigitalAddressINADRequestBodyFilterDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InadServiceTest {

    @InjectMocks
    private InadService inadService;

    @Mock
    private InadClient inadClient;

    private static final String TAX_ID = "CMUJFD29M22L916P";
    private static final String PRACTICAL_REFERENCE = "00001";

    private static final String DIGITAL_ADDRESS_1 = "da1@test.it";
    private static final String DIGITAL_ADDRESS_2 = "da2@test.it";
    private static final String DIGITAL_ADDRESS_3 = "da3@test.it";

    @Test
    void callEServiceShouldReturnClientResponseUnchanged() {

        Date now = new Date();

        UsageInfo usageInfo1 = new UsageInfo();
        usageInfo1.setDateEndValidity(
                Date.from(
                        LocalDate.EPOCH
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                )
        );
        usageInfo1.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);

        ElementDigitalAddress address1 = new ElementDigitalAddress();
        address1.setDigitalAddress(DIGITAL_ADDRESS_1);
        address1.setUsageInfo(usageInfo1);

        UsageInfo usageInfo2 = new UsageInfo();
        usageInfo2.setDateEndValidity(
                Date.from(
                        LocalDate.now()
                                .plusDays(1)
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                )
        );
        usageInfo2.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);

        ElementDigitalAddress address2 = new ElementDigitalAddress();
        address2.setDigitalAddress(DIGITAL_ADDRESS_2);
        address2.setUsageInfo(usageInfo2);

        UsageInfo usageInfo3 = new UsageInfo();
        usageInfo3.setDateEndValidity(
                Date.from(
                        LocalDate.now()
                                .plusDays(1)
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                )
        );
        usageInfo3.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);

        ElementDigitalAddress address3 = new ElementDigitalAddress();
        address3.setDigitalAddress(DIGITAL_ADDRESS_3);
        address3.setPracticedProfession("practicedProfession");
        address3.setUsageInfo(usageInfo3);

        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale(TAX_ID);
        response.setSince(now);
        response.setDigitalAddress(List.of(address1, address2, address3));

        when(inadClient.callEService(TAX_ID, PRACTICAL_REFERENCE))
                .thenReturn(Mono.just(response));

        GetDigitalAddressINADRequestBodyDto request = buildRequest();

        StepVerifier.create(inadService.callEService(request))
                .expectNext(response)
                .verifyComplete();

        verify(inadClient).callEService(TAX_ID, PRACTICAL_REFERENCE);
        verifyNoMoreInteractions(inadClient);
    }

    @Test
    void callEServiceShouldReturnPersonalPecWithoutFiltering() {

        Date now = new Date();

        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        usageInfo.setDateEndValidity(
                Date.from(
                        LocalDate.now()
                                .plusDays(1)
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                )
        );

        ElementDigitalAddress personalAddress = new ElementDigitalAddress();
        personalAddress.setDigitalAddress(DIGITAL_ADDRESS_2);
        personalAddress.setUsageInfo(usageInfo);

        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale(TAX_ID);
        response.setSince(now);
        response.setDigitalAddress(List.of(personalAddress));

        when(inadClient.callEService(TAX_ID, PRACTICAL_REFERENCE))
                .thenReturn(Mono.just(response));

        GetDigitalAddressINADRequestBodyDto request = buildRequest();

        StepVerifier.create(inadService.callEService(request))
                .expectNext(response)
                .verifyComplete();

        verify(inadClient).callEService(TAX_ID, PRACTICAL_REFERENCE);
        verifyNoMoreInteractions(inadClient);
    }

    @Test
    void callEServiceShouldReturnProfessionalAddressesWithoutFiltering() {

        Date now = new Date();

        UsageInfo usageInfo2 = new UsageInfo();
        usageInfo2.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        usageInfo2.setDateEndValidity(
                Date.from(
                        LocalDate.now()
                                .plusDays(1)
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                )
        );

        ElementDigitalAddress address2 = new ElementDigitalAddress();
        address2.setDigitalAddress(DIGITAL_ADDRESS_2);
        address2.setPracticedProfession("practicedProfession2");
        address2.setUsageInfo(usageInfo2);

        UsageInfo usageInfo3 = new UsageInfo();
        usageInfo3.setMotivation(MotivationTermination.CESSAZIONE_UFFICIO);
        usageInfo3.setDateEndValidity(
                Date.from(
                        LocalDate.now()
                                .plusDays(1)
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                )
        );

        ElementDigitalAddress address3 = new ElementDigitalAddress();
        address3.setDigitalAddress(DIGITAL_ADDRESS_3);
        address3.setPracticedProfession("practicedProfession");
        address3.setUsageInfo(usageInfo3);

        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale(TAX_ID);
        response.setSince(now);
        response.setDigitalAddress(List.of(address2, address3));

        when(inadClient.callEService(TAX_ID, PRACTICAL_REFERENCE))
                .thenReturn(Mono.just(response));

        GetDigitalAddressINADRequestBodyDto request = buildRequest();

        StepVerifier.create(inadService.callEService(request))
                .expectNext(response)
                .verifyComplete();

        verify(inadClient).callEService(TAX_ID, PRACTICAL_REFERENCE);
        verifyNoMoreInteractions(inadClient);
    }

    @Test
    void callEServiceShouldPreserveNullDigitalAddressElements() {

        ElementDigitalAddress validAddress = new ElementDigitalAddress();
        validAddress.setDigitalAddress(DIGITAL_ADDRESS_2);

        ResponseRequestDigitalAddress response = new ResponseRequestDigitalAddress();
        response.setCodiceFiscale(TAX_ID);
        response.setDigitalAddress(Arrays.asList(null, validAddress));

        when(inadClient.callEService(TAX_ID, PRACTICAL_REFERENCE))
                .thenReturn(Mono.just(response));

        GetDigitalAddressINADRequestBodyDto request = buildRequest();

        StepVerifier.create(inadService.callEService(request))
                .expectNext(response)
                .verifyComplete();

        verify(inadClient).callEService(TAX_ID, PRACTICAL_REFERENCE);
        verifyNoMoreInteractions(inadClient);
    }

    @Test
    void callEServiceShouldPropagateClientError() {

        RuntimeException error = new RuntimeException("INAD unavailable");

        when(inadClient.callEService(TAX_ID, PRACTICAL_REFERENCE))
                .thenReturn(Mono.error(error));

        GetDigitalAddressINADRequestBodyDto request = buildRequest();

        StepVerifier.create(inadService.callEService(request))
                .expectError(RuntimeException.class)
                .verify();

        verify(inadClient).callEService(TAX_ID, PRACTICAL_REFERENCE);
        verifyNoMoreInteractions(inadClient);
    }

    private GetDigitalAddressINADRequestBodyDto buildRequest() {

        GetDigitalAddressINADRequestBodyFilterDto filter =
                new GetDigitalAddressINADRequestBodyFilterDto();

        filter.setTaxId(TAX_ID);
        filter.setPracticalReference(PRACTICAL_REFERENCE);

        GetDigitalAddressINADRequestBodyDto request =
                new GetDigitalAddressINADRequestBodyDto();

        request.setFilter(filter);

        return request;
    }
}