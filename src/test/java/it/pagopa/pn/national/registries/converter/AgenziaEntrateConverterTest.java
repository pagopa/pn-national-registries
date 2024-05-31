package it.pagopa.pn.national.registries.converter;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AgenziaEntrateConverterTest {
    @InjectMocks
    private AgenziaEntrateConverter agenziaEntrateConverter;


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

