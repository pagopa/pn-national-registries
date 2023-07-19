package it.pagopa.pn.national.registries.model.agenziaentrate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Values:  _'XX00'_: richiesta corretta  _'XX01'_: codice fiscale Ente e/o Rappresentante errato  _'XX02'_: codice fiscale Ente non di soggetto diverso da persona fisica  _'XX0A'_: errore nell’elaborazione  _'XX03'_: timeout interno all'elaborazione  _'XX04'_: errore di autenticazione  _'XXXX'_: errore non codificato
 */
public enum ResultDetailEnum {
    XX00("XX00", "richiesta corretta"),

    XX01("XX01", "codice fiscale Ente e/o Rappresentante errato"),

    XX02("XX02", "codice fiscale Ente non di soggetto diverso da persona fisica"),

    XX0A("XX0A", "errore nell’elaborazione"),

    XX03("XX03", "timeout interno all'elaborazione"),

    XX04("XX04", "errore di autenticazione"),

    XXXX("XXXX", "errore non codificato");

    private String code;
    private String value;

    ResultDetailEnum(String code, String value) {
        this.value = value;
        this.code = code;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static String fromValue(String value) {
        for (ResultDetailEnum b : ResultDetailEnum.values()) {
            if (b.code.equals(value)) {
                return b.value;
            }
        }
        return value;
    }
}