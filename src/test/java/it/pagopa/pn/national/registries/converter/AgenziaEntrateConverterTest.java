package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.msclient.ade.v1.dto.VerificaCodiceFiscale;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

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
        List<String> list = new ArrayList<>();
        list.add("messaggio errato");
        list.add("Codice fiscale non valido");
        list.add("Codice fiscale non utilizzabile in quanto aggiornato in altro codice fiscale");
        list.add("Codice fiscale valido, non più utilizzabile in quanto aggiornato in altro codice fiscale");
        for(String s: list){
            taxIdVerification.setMessaggio(s);
            CheckTaxIdOKDto actualConvertToCfStatusDtoResult = agenziaEntrateConverter.convertToCfStatusDto(taxIdVerification);
            assertEquals("Codice Fiscale", actualConvertToCfStatusDtoResult.getTaxId());
        }
        taxIdVerification.setMessaggio(null);
        CheckTaxIdOKDto actualConvertToCfStatusDtoResult = agenziaEntrateConverter.convertToCfStatusDto(taxIdVerification);
        assertEquals("Codice Fiscale", actualConvertToCfStatusDtoResult.getTaxId());
        assertNull(actualConvertToCfStatusDtoResult.getErrorCode());
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
    }
}

