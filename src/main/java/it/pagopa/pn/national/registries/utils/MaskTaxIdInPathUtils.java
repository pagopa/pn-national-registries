package it.pagopa.pn.national.registries.utils;

import java.util.function.Function;

public class MaskTaxIdInPathUtils {

    public static Function<String, String> maskTaxIdInPath() {
        return message -> message.replaceFirst("/extract/.*\\?", "/extract/***?")
                .replaceFirst("/sede/.*\\?", "/sede/***?")
                .replaceFirst("/listaLegaleRappresentante/.*\\?", "/listaLegaleRappresentante/***?");
    }

}
