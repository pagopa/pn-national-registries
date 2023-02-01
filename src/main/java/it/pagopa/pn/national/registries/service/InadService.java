package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.client.inad.InadClient;
import it.pagopa.pn.national.registries.converter.DigitalAddressInadConverter;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.DigitalAddressDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressINADRequestBodyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class InadService {

    private final InadClient inadClient;

    public InadService(InadClient inadClient) {
        this.inadClient = inadClient;
    }

    public Mono<GetDigitalAddressINADOKDto> callEService(GetDigitalAddressINADRequestBodyDto request) {
        return inadClient.callEService(request.getFilter().getTaxId(), request.getFilter().getPracticalReference())
                .map(response -> {
                    GetDigitalAddressINADOKDto dto = DigitalAddressInadConverter.mapToResponseOk(response);
                    dto.setDigitalAddress(filterValidAddresses(dto.getDigitalAddress()));
                    return dto;
                });
    }

    private List<DigitalAddressDto> filterValidAddresses(List<DigitalAddressDto> addresses) {
        List<DigitalAddressDto> validAddresses = addresses;
        if (addresses != null) {
            Date now = new Date();
            validAddresses = addresses.stream()
                    .filter(a -> isValid(a, now))
                    .toList();
            log.info("inad digital addresses: {} - valid at {}: {}", addresses.size(), now, validAddresses.size());
        } else {
            log.info("inad digital addresses is null");
        }
        return validAddresses;
    }

    private boolean isValid(DigitalAddressDto address, Date date) {
        return address.getUsageInfo() == null
                || address.getUsageInfo().getDateEndValidity() == null
                || address.getUsageInfo().getDateEndValidity().equals(date)
                || address.getUsageInfo().getDateEndValidity().after(date);
    }
}
