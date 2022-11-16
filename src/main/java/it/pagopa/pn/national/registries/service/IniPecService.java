package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.converter.IniPecConverter;
import it.pagopa.pn.national.registries.entity.BatchPolling;
import it.pagopa.pn.national.registries.entity.BatchRequest;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECOKDto;
import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.GetDigitalAddressIniPECRequestBodyDto;
import it.pagopa.pn.national.registries.repository.IniPecRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class IniPecService {

    private final IniPecConverter iniPecConverter;
    private final IniPecRepository iniPecRepository;

    public IniPecService(IniPecConverter iniPecConverter,
                         IniPecRepository iniPecRepository) {
        this.iniPecConverter = iniPecConverter;
        this.iniPecRepository = iniPecRepository;
    }

    public Mono<GetDigitalAddressIniPECOKDto> getDigitalAddress(GetDigitalAddressIniPECRequestBodyDto getDigitalAddressIniPECRequestBodyDto) {
        return iniPecRepository.saveRequestCF(getDigitalAddressIniPECRequestBodyDto)
               .map(requestCorrelation -> iniPecConverter.convertToGetAddressIniPecOKDto(requestCorrelation));
    }

    public Mono<BatchPolling> get(){
        String batchId = "5c00aebc-2f4e-4bc7-b0f2-a51df4e2e1e5";
        List<BatchRequest> batchRequests = new ArrayList<>();
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCf("AAA");
        BatchRequest batchRequest1 = new BatchRequest();
        batchRequest1.setCf("BBB");
        batchRequests.add(batchRequest1);
        batchRequests.add(batchRequest);

        return iniPecRepository.callIniPecAndAggregateCorrelationId(batchRequests,batchId);
    }

}
