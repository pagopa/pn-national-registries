package it.pagopa.pn.national.registries.service;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AddressService {

    private final AnprService anprService;
    private final InadService inadService;
    private final IniPecService iniPecService;

    public AddressService(AnprService anprService,
                          InadService inadService,
                          IniPecService iniPecService) {
        this.anprService = anprService;
        this.inadService = inadService;
        this.iniPecService = iniPecService;
    }

    public Mono<AddressesOKDto> retrieveDigitalOrPhysicalAddress(String cxType, AddressRequestBodyDto addressRequestBodyDto) {
        switch (cxType){
            case "PF":
                if(addressRequestBodyDto.getFilter().getDomicileType().equals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL)) {
                    return anprService.getAddressANPR(convertToGetAddressAnprRequest(addressRequestBodyDto))
                            .map(getAddressANPROKDto -> {
                                //scrivo sulla coda sqs la risposta
                                return mapToAddressesOKDto(addressRequestBodyDto.getFilter().getCorrelationId());
                            });
                }else{
                    return inadService.callEService(convertToGetDigitalAddressInadRequest(addressRequestBodyDto))
                            .map(getDigitalAddressINADOKDto -> {
                                //scrivo sulla coda sqs la risposta
                                return mapToAddressesOKDto(addressRequestBodyDto.getFilter().getCorrelationId());
                            });
                }
            case "PG":
                if(addressRequestBodyDto.getFilter().getDomicileType().equals(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL)){
                   /* return registroImpreseService.getAddress(convertToGetAddressRegistroImpreseRequest(addressRequestBodyDto))
                            .map(getAddressRegistroImpreseOKDto -> {
                                //scrivo sulla coda sqs la risposta
                                return mapToAddressesOKDto(addressRequestBodyDto.getFilter().getCorrelationId());
                            });*/
                }else{
                    return iniPecService.getDigitalAddress(convertToGetDigitalAddressIniPecRequest(addressRequestBodyDto))
                            .map(getDigitalAddressIniPECOKDto -> mapToAddressesOKDto(addressRequestBodyDto.getFilter().getCorrelationId()));
                }
            default:
                return Mono.empty(); //THROW EXCEPTION
       }
    }

    private AddressesOKDto mapToAddressesOKDto(String correlationId) {
        AddressesOKDto addressesOKDto = new AddressesOKDto();
        addressesOKDto.setCorrelationId(correlationId);
        return addressesOKDto;
    }

    private GetDigitalAddressIniPECRequestBodyDto convertToGetDigitalAddressIniPecRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetDigitalAddressIniPECRequestBodyDto dto = new GetDigitalAddressIniPECRequestBodyDto();

        return dto;
    }

    private GetAddressRegistroImpreseRequestBodyDto convertToGetAddressRegistroImpreseRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetAddressRegistroImpreseRequestBodyDto dto = new GetAddressRegistroImpreseRequestBodyDto();

        return dto;
    }

    private GetDigitalAddressINADRequestBodyDto convertToGetDigitalAddressInadRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetDigitalAddressINADRequestBodyDto dto = new GetDigitalAddressINADRequestBodyDto();
        return dto;
    }

    private GetAddressANPRRequestBodyDto convertToGetAddressAnprRequest(AddressRequestBodyDto addressRequestBodyDto) {
        GetAddressANPRRequestBodyDto dto = new GetAddressANPRRequestBodyDto();

        return dto;
    }
}
