package it.pagopa.pn.national.registries.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.ADELegalOKDto;
import it.pagopa.pn.national.registries.generated.openapi.server.v1.dto.CheckTaxIdOKDto;
import it.pagopa.pn.national.registries.model.agenziaentrate.CheckValidityRappresentanteResp;
import it.pagopa.pn.national.registries.model.agenziaentrate.TaxIdVerification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AgenziaEntrateConverterTest {
    @InjectMocks
    private AgenziaEntrateConverter agenziaEntrateConverter;


    /**
     * Method under test: {@link AgenziaEntrateConverter#convertToCfStatusDto(TaxIdVerification)}
     */
    @Test
    void testConvertToCfStatusDto3() {
        TaxIdVerification taxIdVerification = new TaxIdVerification();
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
        CheckValidityRappresentanteResp checkValidityRappresentanteResp = new CheckValidityRappresentanteResp();
        checkValidityRappresentanteResp.setCodiceRitorno("00");
        checkValidityRappresentanteResp.setValido(true);
        checkValidityRappresentanteResp.setDettaglioEsito("XX00");

        ADELegalOKDto adeLegalOKDto = agenziaEntrateConverter.adELegalResponseToDto(checkValidityRappresentanteResp);

        assertEquals(adeLegalOKDto.getResultCode(), ADELegalOKDto.ResultCodeEnum.fromValue(checkValidityRappresentanteResp.getCodiceRitorno()));
        assertEquals(adeLegalOKDto.getVerificationResult(), checkValidityRappresentanteResp.getValido());
        assertEquals(adeLegalOKDto.getResultDetail(), ADELegalOKDto.ResultDetailEnum.fromValue(checkValidityRappresentanteResp.getDettaglioEsito()));
    }

   /* @Test
    void  toEnvelopeBodyTest() {
        ADELegalRequestBodyFilterDto adeLegalRequestBodyFilterDto = new ADELegalRequestBodyFilterDto();
        adeLegalRequestBodyFilterDto.setVatNumber("testVatNumber");
        adeLegalRequestBodyFilterDto.setTaxId("testTaxId");

        CheckValidityRappresentanteResp checkValidityRappresentanteType = agenziaEntrateConverter.toEnvelopeBody(adeLegalRequestBodyFilterDto);

        assertEquals(checkValidityRappresentanteType.getCfEnte(), adeLegalRequestBodyFilterDto.getVatNumber());
        assertEquals(checkValidityRappresentanteType.getCfRappresentante(), adeLegalRequestBodyFilterDto.getTaxId());
    } */
}

