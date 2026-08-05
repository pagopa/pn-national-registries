package it.pagopa.pn.national.registries.utils;

public class MaskTaxIdInPathUtils {

    public static String maskTaxIdInPath(String message) {
        return message.replaceFirst("/extract/.*\\?", "/extract/***?")
                .replaceFirst("/sede/.*\\?", "/sede/***?")
                .replaceFirst("/listaLegaleRappresentante/.*\\?", "/listaLegaleRappresentante/***?");
    }

}
