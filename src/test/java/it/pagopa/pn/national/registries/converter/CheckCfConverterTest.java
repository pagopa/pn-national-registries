package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.pn.national.registries.generated.openapi.rest.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.model.checkcf.VerificaCodiceFiscale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {CheckCfConverter.class})
@ExtendWith(SpringExtension.class)
class CheckCfConverterTest {
    @Autowired
    private CheckCfConverter checkCfConverter;

    /**
     * Method under test: {@link CheckCfConverter#convertToCfStatusDto(VerificaCodiceFiscale)}
     */
    @Test
    void testConvertToCfStatusDto() {
        VerificaCodiceFiscale verificaCodiceFiscale = new VerificaCodiceFiscale();
        verificaCodiceFiscale.setCodiceFiscale("Codice Fiscale");
        verificaCodiceFiscale.setMessaggio("Messaggio");
        verificaCodiceFiscale.setValido(true);
        CheckTaxIdOKDto actualConvertToCfStatusDtoResult = checkCfConverter.convertToCfStatusDto(verificaCodiceFiscale);
        assertNull(actualConvertToCfStatusDtoResult.getErrorCode());
        assertEquals("Codice Fiscale", actualConvertToCfStatusDtoResult.getTaxId());
        assertTrue(actualConvertToCfStatusDtoResult.getIsValid());
    }

    /**
     * Method under test: {@link CheckCfConverter#convertToCfStatusDto(VerificaCodiceFiscale)}
     */
    @Test
    void testConvertToCfStatusDto2() {
        VerificaCodiceFiscale verificaCodiceFiscale = new VerificaCodiceFiscale();
        verificaCodiceFiscale.setCodiceFiscale("Codice Fiscale");
        verificaCodiceFiscale.setMessaggio(null);
        verificaCodiceFiscale.setValido(true);
        CheckTaxIdOKDto actualConvertToCfStatusDtoResult = checkCfConverter.convertToCfStatusDto(verificaCodiceFiscale);
        assertEquals("Codice Fiscale", actualConvertToCfStatusDtoResult.getTaxId());
        assertTrue(actualConvertToCfStatusDtoResult.getIsValid());
    }

    /**
     * Method under test: {@link CheckCfConverter#convertToCfStatusDto(VerificaCodiceFiscale)}
     */
    @Test
    void testConvertToCfStatusDto3() {
        VerificaCodiceFiscale verificaCodiceFiscale = new VerificaCodiceFiscale();
        verificaCodiceFiscale.setCodiceFiscale("Codice Fiscale");
        verificaCodiceFiscale.setMessaggio("Codice fiscale non valido");
        verificaCodiceFiscale.setValido(true);
        CheckTaxIdOKDto actualConvertToCfStatusDtoResult = checkCfConverter.convertToCfStatusDto(verificaCodiceFiscale);
        assertEquals("Codice Fiscale", actualConvertToCfStatusDtoResult.getTaxId());
        assertTrue(actualConvertToCfStatusDtoResult.getIsValid());
    }

    /**
     * Method under test: {@link CheckCfConverter#convertToCfStatusDto(VerificaCodiceFiscale)}
     */
    @Test
    void testConvertToCfStatusDto4() {
        VerificaCodiceFiscale verificaCodiceFiscale = new VerificaCodiceFiscale();
        verificaCodiceFiscale.setCodiceFiscale("Codice Fiscale");
        verificaCodiceFiscale.setMessaggio("Codice fiscale non utilizzabile in quanto aggiornato in altro codice fiscale");
        verificaCodiceFiscale.setValido(true);
        CheckTaxIdOKDto actualConvertToCfStatusDtoResult = checkCfConverter.convertToCfStatusDto(verificaCodiceFiscale);
        assertEquals("Codice Fiscale", actualConvertToCfStatusDtoResult.getTaxId());
        assertTrue(actualConvertToCfStatusDtoResult.getIsValid());
    }

    /**
     * Method under test: {@link CheckCfConverter#convertToCfStatusDto(VerificaCodiceFiscale)}
     */
    @Test
    void testConvertToCfStatusDto5() {
        VerificaCodiceFiscale verificaCodiceFiscale = new VerificaCodiceFiscale();
        verificaCodiceFiscale.setCodiceFiscale("Codice Fiscale");
        verificaCodiceFiscale.setMessaggio("Codice fiscale valido, non più utilizzabile in quanto aggiornato in altro codice fiscale");
        verificaCodiceFiscale.setValido(true);
        CheckTaxIdOKDto actualConvertToCfStatusDtoResult = checkCfConverter.convertToCfStatusDto(verificaCodiceFiscale);
        assertEquals("Codice Fiscale", actualConvertToCfStatusDtoResult.getTaxId());
        assertTrue(actualConvertToCfStatusDtoResult.getIsValid());
    }

    /**
     * Method under test: {@link CheckCfConverter#decodeError(String)}
     */
    @Test
    void testDecodeError() {
        assertNull(checkCfConverter.decodeError("Not all who wander are lost"));
    }
}

