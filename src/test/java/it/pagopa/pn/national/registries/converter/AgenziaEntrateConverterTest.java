package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.dto.VerificaCodiceFiscale;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AgenziaEntrateConverterTest {

    @InjectMocks
    private AgenziaEntrateConverter agenziaEntrateConverter;

    @Test
    void testConvertToCfStatusDto3() {
        VerificaCodiceFiscale taxIdVerification = new VerificaCodiceFiscale();
        taxIdVerification.setCodiceFiscale("Codice Fiscale");
        taxIdVerification.setValido(false);
        Map<String, CheckTaxIdOKDto.ErrorCodeEnum> expected = new LinkedHashMap<>();
        expected.put("messaggio errato", null);
        expected.put("Codice fiscale non valido", CheckTaxIdOKDto.ErrorCodeEnum.B001_CHECK_TAX_ID_ERR03);
        expected.put("Codice fiscale non utilizzabile in quanto aggiornato in altro codice fiscale", CheckTaxIdOKDto.ErrorCodeEnum.B001_CHECK_TAX_ID_ERR02);
        expected.put("Codice fiscale valido, non più utilizzabile in quanto aggiornato in altro codice fiscale", CheckTaxIdOKDto.ErrorCodeEnum.B001_CHECK_TAX_ID_ERR01);
        for (Map.Entry<String, CheckTaxIdOKDto.ErrorCodeEnum> entry : expected.entrySet()) {
            taxIdVerification.setMessaggio(entry.getKey());
            CheckTaxIdOKDto result = agenziaEntrateConverter.convertToCfStatusDto(taxIdVerification);
            assertEquals("Codice Fiscale", result.getTaxId());
            assertEquals(false, result.getIsValid());
            assertEquals(entry.getValue(), result.getErrorCode());
        }
        taxIdVerification.setMessaggio(null);
        CheckTaxIdOKDto actualConvertToCfStatusDtoResult = agenziaEntrateConverter.convertToCfStatusDto(taxIdVerification);
        assertEquals("Codice Fiscale", actualConvertToCfStatusDtoResult.getTaxId());
        assertNull(actualConvertToCfStatusDtoResult.getErrorCode());
    }

    @Test
    void convertToCfStatusDtoWithValidTaxId() {
        VerificaCodiceFiscale taxIdVerification = new VerificaCodiceFiscale();
        taxIdVerification.setCodiceFiscale("Codice Fiscale");
        taxIdVerification.setValido(true);
        taxIdVerification.setMessaggio(null);
        CheckTaxIdOKDto result = agenziaEntrateConverter.convertToCfStatusDto(taxIdVerification);
        assertEquals("Codice Fiscale", result.getTaxId());
        assertEquals(true, result.getIsValid());
        assertNull(result.getErrorCode());
    }

    /**
     * Method under test: {@link AgenziaEntrateConverter#decodeError(String)}
     */
    @Test
    void testDecodeError() {
        assertNull(agenziaEntrateConverter.decodeError("Not all who wander are lost"));
    }

    @Test
    void adELegalResponseToDtoTest() {
        CheckValidityRappresentanteResp checkValidityRappresentanteResp = mock(CheckValidityRappresentanteResp.class);
        checkValidityRappresentanteResp.valido = true;
        checkValidityRappresentanteResp.dettaglioEsito = "XX00";
        checkValidityRappresentanteResp.codiceRitorno = "00";

        ADELegalOKDto adeLegalOKDto = agenziaEntrateConverter.adELegalResponseToDto(checkValidityRappresentanteResp);

        assertEquals(true, adeLegalOKDto.getVerificationResult());
        assertEquals("XX00", adeLegalOKDto.getResultDetail());
        assertEquals("00", adeLegalOKDto.getResultCode());
        assertEquals("richiesta corretta", adeLegalOKDto.getResultDetailMessage());
    }

    @Test
    void adELegalResponseToDtoMapsUnknownCodesToDefaults() {
        CheckValidityRappresentanteResp resp = new CheckValidityRappresentanteResp();
        resp.valido = false;
        resp.dettaglioEsito = "ZZZZ";
        resp.codiceRitorno = "99";

        ADELegalOKDto dto = agenziaEntrateConverter.adELegalResponseToDto(resp);

        assertEquals(false, dto.getVerificationResult());
        assertEquals("UNKNOWN", dto.getResultCode());
        assertEquals("XXXX", dto.getResultDetail());
        assertEquals("ZZZZ", dto.getResultDetailMessage());
    }

    @Test
    void decodeErrorMapsEveryKnownMessage() {
        assertEquals(CheckTaxIdOKDto.ErrorCodeEnum.B001_CHECK_TAX_ID_ERR01, agenziaEntrateConverter.decodeError(AgenziaEntrateConverter.CODICE_FISCALE_VALIDO_NON_UTILIZZABILE));
        assertEquals(CheckTaxIdOKDto.ErrorCodeEnum.B001_CHECK_TAX_ID_ERR02, agenziaEntrateConverter.decodeError(AgenziaEntrateConverter.CODICE_FISCALE_NON_VALIDO_AGGIORNATO_IN_ALTRO));
        assertEquals(CheckTaxIdOKDto.ErrorCodeEnum.B001_CHECK_TAX_ID_ERR03, agenziaEntrateConverter.decodeError(AgenziaEntrateConverter.CODICE_FISCALE_NON_VALIDO));
    }
}