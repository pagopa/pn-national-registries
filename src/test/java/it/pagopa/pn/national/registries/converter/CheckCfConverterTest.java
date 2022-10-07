package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.model.checkcf.VerificaCodiceFiscale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class CheckCfConverterTest {
    @InjectMocks
    private CheckCfConverter checkCfConverter;


    /**
     * Method under test: {@link CheckCfConverter#convertToCfStatusDto(VerificaCodiceFiscale)}
     */
    @Test
    void testConvertToCfStatusDto3() {
        VerificaCodiceFiscale verificaCodiceFiscale = new VerificaCodiceFiscale();
        verificaCodiceFiscale.setCodiceFiscale("Codice Fiscale");
        List<String> list = new ArrayList<>();
        list.add("messaggio errato");
        list.add("Codice fiscale non valido");
        list.add("Codice fiscale non utilizzabile in quanto aggiornato in altro codice fiscale");
        list.add("Codice fiscale valido, non più utilizzabile in quanto aggiornato in altro codice fiscale");
        for(String s: list){
            verificaCodiceFiscale.setMessaggio(s);
            CheckTaxIdOKDto actualConvertToCfStatusDtoResult = checkCfConverter.convertToCfStatusDto(verificaCodiceFiscale);
            assertEquals("Codice Fiscale", actualConvertToCfStatusDtoResult.getTaxId());
        }
        verificaCodiceFiscale.setMessaggio(null);
        CheckTaxIdOKDto actualConvertToCfStatusDtoResult = checkCfConverter.convertToCfStatusDto(verificaCodiceFiscale);
        assertEquals("Codice Fiscale", actualConvertToCfStatusDtoResult.getTaxId());
        assertNull(actualConvertToCfStatusDtoResult.getErrorCode());
    }

    /**
     * Method under test: {@link CheckCfConverter#decodeError(String)}
     */
    @Test
    void testDecodeError() {
        assertNull(checkCfConverter.decodeError("Not all who wander are lost"));
    }
}

