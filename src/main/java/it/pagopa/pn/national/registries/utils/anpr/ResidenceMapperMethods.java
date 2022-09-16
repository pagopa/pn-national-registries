package it.pagopa.pn.national.registries.utils.anpr;

import it.pagopa.pn.national.registries.generated.openapi.anpr.client.v1.dto.*;
import it.pagopa.pn.national.registries.generated.openapi.server.anpr.residence.v1.dto.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class ResidenceMapperMethods {

    private ResidenceMapperMethods() {
    }

    public static RichiestaE002Dto convertToRichiestaE002(ConsultaResidenzaANPRRequestBodyDto requestDto) {
        RichiestaE002Dto richiestaE002Dto = new RichiestaE002Dto();
        return richiestaE002Dto;
    }

    public static Mono<ConsultaResidenzaANPROKDto> mapToResponseOk(RispostaE002OKDto rispostaE002OKDto) {
        ConsultaResidenzaANPROKDto response = new ConsultaResidenzaANPROKDto();
        if (rispostaE002OKDto.getListaSoggetti() != null
                && rispostaE002OKDto.getListaSoggetti().getDatiSoggetto() != null
                && !rispostaE002OKDto.getListaSoggetti().getDatiSoggetto().isEmpty()
                && rispostaE002OKDto.getListaSoggetti().getDatiSoggetto().get(0) != null) {
            response.setResidenza(mapToConsultaResidenzaANPR(rispostaE002OKDto.getListaSoggetti().getDatiSoggetto().get(0).getResidenza()));
        }
        return Mono.just(response);
    }

    private static List<ResidenzaInnerDto> mapToConsultaResidenzaANPR(List<TipoResidenzaDto> residenza) {
        List<ResidenzaInnerDto> list = new ArrayList<>();
        if(residenza!=null && !residenza.isEmpty()) {
            for (TipoResidenzaDto t : residenza) {
                ResidenzaInnerDto dto = new ResidenzaInnerDto();
                dto.setDataRicorrenzaResidenza(t.getDataDecorrenzaResidenza());
                if (t.getIndirizzo() != null) {
                    dto.setIndirizzo(convertToInnerIndirizzoDto(t.getIndirizzo()));
                }
                if (t.getLocalitaEstera() != null) {
                    dto.setLocalitaEstera(convertToInnerLocEsteraDto(t.getLocalitaEstera()));
                }
                dto.setPresso(t.getPresso());
                dto.setNoteIndirizzo(t.getNoteIndirizzo());
                dto.setTipoIndirizzo(t.getTipoIndirizzo());
                list.add(dto);
            }
        }
        return list;
    }

    private static LocalitaEsteraDto convertToInnerLocEsteraDto(TipoLocalitaEstera1Dto localitaEstera) {
        LocalitaEsteraDto dto = new LocalitaEsteraDto();
        if (localitaEstera.getConsolato() != null) {
            dto.setConsolato(convertConsolato(localitaEstera.getConsolato()));
        }
        if (localitaEstera.getIndirizzoEstero() != null) {
            dto.setIndirizzoEstero(convertIndirizzoEstero(localitaEstera.getIndirizzoEstero()));
        }
        return dto;
    }

    private static ConsolatoDto convertConsolato(TipoConsolatoDto consolato) {
        ConsolatoDto dto = new ConsolatoDto();
        dto.setCodiceConsolato(consolato.getCodiceConsolato());
        dto.setDescrizioneConsolato(consolato.getDescrizioneConsolato());
        return dto;
    }

    private static IndirizzoEsteroDto convertIndirizzoEstero(TipoIndirizzoEsteroDto indirizzoEstero) {
        IndirizzoEsteroDto dto = new IndirizzoEsteroDto();
        dto.setCap(indirizzoEstero.getCap());
        if (indirizzoEstero.getLocalita() != null) {
            dto.setLocalita(convertLocalita(indirizzoEstero.getLocalita()));
        }
        if (indirizzoEstero.getToponimo() != null) {
            dto.setToponimo(convertToponimoEstero(indirizzoEstero.getToponimo()));
        }
        return dto;
    }

    private static ToponimoEsteroDto convertToponimoEstero(TipoToponimoEsteroDto toponimo) {
        ToponimoEsteroDto dto = new ToponimoEsteroDto();
        dto.setDenominazione(toponimo.getDenominazione());
        dto.setNumeroCivico(toponimo.getNumeroCivico());
        return dto;
    }

    private static LocalitaDto convertLocalita(TipoDatoLocalitaEsteraDto localita) {
        LocalitaDto dto = new LocalitaDto();
        dto.setDescrizioneLocalita(localita.getDescrizioneLocalita());
        dto.setCodiceStato(localita.getCodiceStato());
        dto.setDescrizioneStato(localita.getDescrizioneStato());
        dto.setProvinciaContea(localita.getProvinciaContea());
        return dto;
    }

    private static IndirizzoDto convertToInnerIndirizzoDto(TipoIndirizzoDto indirizzo) {
        IndirizzoDto dto = new IndirizzoDto();
        dto.setCap(indirizzo.getCap());
        if (indirizzo.getComune() != null) {
            dto.setComune(convertComune(indirizzo.getComune()));
        }
        dto.setFrazione(indirizzo.getFrazione());
        if (indirizzo.getToponimo() != null) {
            dto.setToponimo(convertToponimo(indirizzo.getToponimo()));
        }
        if (indirizzo.getNumeroCivico() != null) {
            dto.setNumeroCivico(convertNumeroCivico(indirizzo.getNumeroCivico()));
        }
        return dto;
    }

    private static NumeroCivicoDto convertNumeroCivico(TipoNumeroCivicoDto numeroCivico) {
        NumeroCivicoDto dto = new NumeroCivicoDto();
        dto.setCivicoFonte(numeroCivico.getCivicoFonte());
        dto.setCodiceCivico(numeroCivico.getCodiceCivico());
        dto.setNumero(numeroCivico.getNumero());
        dto.setMetrico(numeroCivico.getMetrico());
        dto.setProgSNC(numeroCivico.getProgSNC());
        dto.setLettera(numeroCivico.getLettera());
        dto.setEsponente1(numeroCivico.getEsponente1());
        dto.setColore(numeroCivico.getColore());
        if (numeroCivico.getCivicoInterno() != null) {
            dto.setCivicoInterno(convertCivicoInterno(numeroCivico.getCivicoInterno()));
        }
        return dto;
    }

    private static CivicoInternoDto convertCivicoInterno(TipoCivicoInternoDto civicoInterno) {
        CivicoInternoDto dto = new CivicoInternoDto();
        dto.setCorte(civicoInterno.getCorte());
        dto.setScala(civicoInterno.getScala());
        dto.setInterno1(civicoInterno.getInterno1());
        dto.setEspInterno1(civicoInterno.getEspInterno1());
        dto.setInterno2(civicoInterno.getInterno2());
        dto.setEspInterno2(civicoInterno.getEspInterno2());
        dto.setScalaEsterna(civicoInterno.getScalaEsterna());
        dto.setSecondario(civicoInterno.getSecondario());
        dto.setPiano(civicoInterno.getPiano());
        dto.setNui(civicoInterno.getNui());
        dto.setIsolato(civicoInterno.getIsolato());
        return dto;

    }

    private static ToponimoDto convertToponimo(TipoToponimoDto toponimo) {
        ToponimoDto dto = new ToponimoDto();
        dto.setCodToponimo(toponimo.getCodToponimo());
        dto.setDenominazioneToponimo(toponimo.getDenominazioneToponimo());
        dto.setToponimoFonte(toponimo.getToponimoFonte());
        dto.setCodSpecie(toponimo.getCodSpecie());
        dto.setSpecie(toponimo.getSpecie());
        dto.setSpecieFonte(toponimo.getSpecieFonte());
        return dto;
    }

    private static ComuneDto convertComune(TipoComuneDto comune) {
        ComuneDto dto = new ComuneDto();
        dto.setDescrizioneLocalita(comune.getDescrizioneLocalita());
        dto.setNomeComune(comune.getNomeComune());
        dto.setCodiceIstat(comune.getCodiceIstat());
        dto.setSiglaProvinciaIstat(comune.getSiglaProvinciaIstat());
        return dto;
    }
}
