package it.pagopa.pn.national.registries.utils;

import java.util.function.Function;

public class MaskTaxIdInPathUtils {

    public static Function<String, String> maskTaxIdInPath() {
        return message -> message.replaceFirst("/extract/[^?\\s]*", "/extract/***")
                .replaceFirst("/sede/[^?\\s]*", "/sede/***")
                .replaceFirst("/listaLegaleRappresentante/[^?\\s]*", "/listaLegaleRappresentante/***");
    }

}
