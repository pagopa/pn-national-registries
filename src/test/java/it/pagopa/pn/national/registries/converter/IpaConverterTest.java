package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.IPAPecOKDto;
import it.pagopa.pn.national.registries.model.ipa.DataWS23Dto;
import it.pagopa.pn.national.registries.model.ipa.ResultDto;
import it.pagopa.pn.national.registries.model.ipa.WS23ResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class IpaConverterTest {
    @InjectMocks
    private IpaConverter ipaConverter;


    @Test
    void testConvertToIPAPecOKDto() {
        DataWS23Dto dataWS23Dto = new DataWS23Dto();
        dataWS23Dto.setCodEnte("Cod Ente");
        dataWS23Dto.setDenominazione("Denominazione");
        dataWS23Dto.setDomicilioDigitale("Domicilio Digitale");
        dataWS23Dto.setType("Type");
        List<DataWS23Dto> dataWS23DtoList = new ArrayList<>();
        dataWS23DtoList.add(dataWS23Dto);

        ResultDto resultDto = new ResultDto();
        resultDto.setCodError(-1);
        resultDto.setDescError("An error occurred");
        resultDto.setNumItems(1000);

        WS23ResponseDto ws23ResponseDto = new WS23ResponseDto();
        ws23ResponseDto.setData(dataWS23DtoList);
        ws23ResponseDto.setResult(resultDto);
        IPAPecOKDto actualConvertToIPAPecOKDtoResult = ipaConverter.convertToIPAPecOKDto(ws23ResponseDto);
        assertEquals("Cod Ente", actualConvertToIPAPecOKDtoResult.getDomiciliDigitali().get(0).getCodEnte());
        assertEquals("Type", actualConvertToIPAPecOKDtoResult.getDomiciliDigitali().get(0).getTipo());
        assertEquals("Domicilio Digitale", actualConvertToIPAPecOKDtoResult.getDomiciliDigitali().get(0).getDomicilioDigitale());
        assertEquals("Denominazione", actualConvertToIPAPecOKDtoResult.getDomiciliDigitali().get(0).getDenominazione());
    }

}

