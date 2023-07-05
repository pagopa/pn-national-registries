package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.exceptions.PnNationalRegistriesException;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.national.registries.model.inad.ElementDigitalAddressDto;
import it.pagopa.pn.national.registries.model.inad.MotivationTerminationDto;
import it.pagopa.pn.national.registries.model.inad.ResponseRequestDigitalAddressDto;
import it.pagopa.pn.national.registries.model.inad.UsageInfo;
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
        usageInfo1.setMotivation(MotivationTerminationDto.UFFICIO);
        ElementDigitalAddressDto elementDigitalAddressDto1 = new ElementDigitalAddressDto();
        elementDigitalAddressDto1.setDigitalAddress(DIGITAL_ADDRESS_1);
        elementDigitalAddressDto1.setUsageInfo(usageInfo1);

        UsageInfo usageInfo2 = new UsageInfo();
        usageInfo2.setMotivation(MotivationTerminationDto.UFFICIO);
        ElementDigitalAddressDto elementDigitalAddressDto2 = new ElementDigitalAddressDto();
        elementDigitalAddressDto2.setDigitalAddress(DIGITAL_ADDRESS_2);
        elementDigitalAddressDto2.setUsageInfo(usageInfo2);

        UsageInfo usageInfo3 = new UsageInfo();
        usageInfo3.setDateEndValidity(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        usageInfo3.setMotivation(MotivationTerminationDto.UFFICIO);
        ElementDigitalAddressDto elementDigitalAddressDto3 = new ElementDigitalAddressDto();
        elementDigitalAddressDto3.setDigitalAddress(DIGITAL_ADDRESS_3);
        elementDigitalAddressDto3.setUsageInfo(usageInfo3);

        ResponseRequestDigitalAddressDto responseRequestDigitalAddressDto = new ResponseRequestDigitalAddressDto();
        responseRequestDigitalAddressDto.setTaxId(TAX_ID);
        responseRequestDigitalAddressDto.setSince(now);
        List<ElementDigitalAddressDto> lista = List.of(elementDigitalAddressDto1, elementDigitalAddressDto2, elementDigitalAddressDto3);
        responseRequestDigitalAddressDto.setDigitalAddress(lista);

        when(inadClient.callEService(TAX_ID, practicalReference))
                .thenReturn(Mono.just(responseRequestDigitalAddressDto));

        GetDigitalAddressINADRequestBodyDto req = new GetDigitalAddressINADRequestBodyDto();
        GetDigitalAddressINADRequestBodyFilterDto filterDto = new GetDigitalAddressINADRequestBodyFilterDto();
        filterDto.setTaxId(TAX_ID);
        filterDto.setPracticalReference(practicalReference);
        req.setFilter(filterDto);

        UsageInfoDto usageInfoDto1 = new UsageInfoDto();
        usageInfoDto1.setMotivation(UsageInfoDto.MotivationEnum.UFFICIO);
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
