package it.pagopa.pn.national.registries.converter;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS05Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.DataWS23Dto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS05ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.msclient.ipa.v1.dto.WS23ResponseDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.IPAPecDto;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
class IpaConverterTest {
    private final IpaConverter ipaConverter = new IpaConverter();
    @Test
    void convertFromWS05MapsCodeDescriptionAndFirstMailAsPec() {
        DataWS05Dto data = new DataWS05Dto();
        data.setCodAmm("Cod Amm");
        data.setDesAmm("Des Amm");
        data.setMail1("pec1@pec.it");
        data.setMail2("pec2@pec.it");
        WS05ResponseDto response = new WS05ResponseDto();
        response.setData(data);
        IPAPecDto result = ipaConverter.convertToIPAPecDtoFromWS05(response);
        assertEquals("Cod Amm", result.getCodEnte());
        assertEquals("Des Amm", result.getDenominazione());
        assertEquals(IpaConverter.ADDRESS_TYPE, result.getTipo());
        assertEquals("pec1@pec.it", result.getDomicilioDigitale());
    }
    @Test
    void convertFromWS05WithoutDataReturnsEmptyDto() {
        IPAPecDto result = ipaConverter.convertToIPAPecDtoFromWS05(new WS05ResponseDto());
        assertNull(result.getCodEnte());
        assertNull(result.getDenominazione());
        assertNull(result.getTipo());
        assertNull(result.getDomicilioDigitale());
    }
    @Test
    void convertFromWS05WithoutMailKeepsNullDomicilioDigitale() {
        DataWS05Dto data = new DataWS05Dto();
        data.setCodAmm("Cod Amm");
        data.setDesAmm("Des Amm");
        WS05ResponseDto response = new WS05ResponseDto();
        response.setData(data);
        IPAPecDto result = ipaConverter.convertToIPAPecDtoFromWS05(response);
        assertEquals("Cod Amm", result.getCodEnte());
        assertEquals(IpaConverter.ADDRESS_TYPE, result.getTipo());
        assertNull(result.getDomicilioDigitale());
    }
    @Test
    void convertFromWS23MapsFirstElementOnly() {
        DataWS23Dto first = ws23("Cod Ente 1", "Denominazione 1", "first@pec.it", "Pec");
        DataWS23Dto second = ws23("Cod Ente 2", "Denominazione 2", "second@pec.it", "Altro");
        List<DataWS23Dto> data = new ArrayList<>(List.of(first, second));
        WS23ResponseDto response = new WS23ResponseDto();
        response.setData(data);
        IPAPecDto result = ipaConverter.convertToIpaPecDtoFromWS23(response);
        assertEquals("Cod Ente 1", result.getCodEnte());
        assertEquals("Denominazione 1", result.getDenominazione());
        assertEquals("Pec", result.getTipo());
        assertEquals("first@pec.it", result.getDomicilioDigitale());
    }
    @Test
    void convertFromWS23WithEmptyDataReturnsEmptyDto() {
        WS23ResponseDto response = new WS23ResponseDto();
        response.setData(new ArrayList<>());
        IPAPecDto result = ipaConverter.convertToIpaPecDtoFromWS23(response);
        assertNull(result.getCodEnte());
        assertNull(result.getDomicilioDigitale());
    }
    @Test
    void convertFromWS23WithNullDataReturnsEmptyDto() {
        WS23ResponseDto response = new WS23ResponseDto();
        response.setData(null);
        IPAPecDto result = ipaConverter.convertToIpaPecDtoFromWS23(response);
        assertNull(result.getCodEnte());
        assertNull(result.getDomicilioDigitale());
    }
    private static DataWS23Dto ws23(String codAmm, String desAmm, String domicilioDigitale, String tipo) {
        DataWS23Dto data = new DataWS23Dto();
        data.setCodAmm(codAmm);
        data.setDesAmm(desAmm);
        data.setDomicilioDigitale(domicilioDigitale);
        data.setTipo(tipo);
        return data;
    }
}
