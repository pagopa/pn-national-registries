package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ElementDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.MotivationTermination;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.ResponseRequestDigitalAddress;
import it.pagopa.pn.national.registries.generated.openapi.msclient.inad.v1.dto.UsageInfo;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.utils.ValidateTaxIdUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InadServiceTest {

    @InjectMocks
    InadService inadService;

    @Mock
    InadClient inadClient;

    @Mock
    ValidateTaxIdUtils validateTaxIdUtils;

    private static final String TAX_ID = "taxId";
    private static final String DIGITAL_ADDRESS_1 = "da_1";
    private static final String DIGITAL_ADDRESS_2 = "da_2";
    private static final String DIGITAL_ADDRESS_3 = "da_3";

    @Test
    void callEService() {
        String practicalReference = "00001";
        Date now = new Date();

        UsageInfo usageInfo1 = new UsageInfo();
        usageInfo1.setDateEndValidity(Date.from(LocalDate.EPOCH.atStartOfDay(ZoneOffset.UTC).toInstant()));
        usageInfo1.setMotivation(MotivationTermination.UFFICIO);
        ElementDigitalAddress elementDigitalAddressDto1 = new ElementDigitalAddress();
        elementDigitalAddressDto1.setDigitalAddress(DIGITAL_ADDRESS_1);
        elementDigitalAddressDto1.setUsageInfo(usageInfo1);

        UsageInfo usageInfo2 = new UsageInfo();
        usageInfo2.setMotivation(MotivationTermination.UFFICIO);
        usageInfo2.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        ElementDigitalAddress elementDigitalAddressDto2 = new ElementDigitalAddress();
        elementDigitalAddressDto2.setDigitalAddress(DIGITAL_ADDRESS_2);
        elementDigitalAddressDto2.setUsageInfo(usageInfo2);

        UsageInfo usageInfo3 = new UsageInfo();
        usageInfo3.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        usageInfo3.setMotivation(MotivationTermination.UFFICIO);
        ElementDigitalAddress elementDigitalAddressDto3 = new ElementDigitalAddress();
        elementDigitalAddressDto3.setDigitalAddress(DIGITAL_ADDRESS_3);
        elementDigitalAddressDto3.setUsageInfo(usageInfo3);

        ResponseRequestDigitalAddress responseRequestDigitalAddressDto = new ResponseRequestDigitalAddress();
        responseRequestDigitalAddressDto.setCodiceFiscale(TAX_ID);
        responseRequestDigitalAddressDto.setSince(now);
        List<ElementDigitalAddress> lista = List.of(elementDigitalAddressDto1, elementDigitalAddressDto2, elementDigitalAddressDto3);
        responseRequestDigitalAddressDto.setDigitalAddress(lista);

        when(inadClient.callEService(TAX_ID, practicalReference))
                .thenReturn(Mono.just(responseRequestDigitalAddressDto));

        GetDigitalAddressINADRequestBodyDto req = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();
        filterDto.setTaxId(TAX_ID);
        filterDto.setPracticalReference(practicalReference);
        req.setFilter(filterDto);

        UsageInfoDto usageInfoDto1 = new UsageInfoDto();
        usageInfoDto1.setDateEndValidity(Date.from(usageInfo3.getDateEndValidity().toInstant().minus(3, ChronoUnit.DAYS)));
        usageInfoDto1.setMotivation(UsageInfoDto.MotivationEnum.UFFICIO);
        usageInfoDto1.setDateEndValidity(Date.from(usageInfo1.getDateEndValidity().toInstant()));
        DigitalAddressDto digitalAddressDto1 = new DigitalAddressDto();
        digitalAddressDto1.setDigitalAddress(DIGITAL_ADDRESS_2);
        digitalAddressDto1.setUsageInfo(usageInfoDto1);

        UsageInfoDto usageInfoDto2 = new UsageInfoDto();
        usageInfoDto2.setDateEndValidity(usageInfo3.getDateEndValidity());
        usageInfoDto2.setMotivation(UsageInfoDto.MotivationEnum.UFFICIO);
        DigitalAddressDto digitalAddressDto2 = new DigitalAddressDto();
        digitalAddressDto2.setDigitalAddress(DIGITAL_ADDRESS_3);
        digitalAddressDto2.setUsageInfo(usageInfoDto2);

        GetDigitalAddressINADOKDto response = new GetDigitalAddressINADOKDto();
        response.setTaxId(TAX_ID);
        response.setSince(now);
        response.setDigitalAddress(digitalAddressDto1);

        StepVerifier.create(inadService.callEService(req, "PF"))
                .expectError(PnNationalRegistriesException.class)
                .verify();
    }
}
