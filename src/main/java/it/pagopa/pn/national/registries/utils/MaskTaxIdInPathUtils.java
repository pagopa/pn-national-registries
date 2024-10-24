package it.pagopa.pn.national.registries.utils;

public class MaskTaxIdInPathUtils {
    public static String maskTaxIdInPathInad(String message) {
        return message.replaceFirst("/extract/.*\\?", "/extract/***?");
    }

    public static String maskTaxIdInPathICRegistroImprese(String message) {
        return message.replaceFirst("/sede/.*\\?", "/sede/***?");
    }

}
