package it.pagopa.pn.national.registries.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaskDataUtils {

    private static final Pattern CF_URI_PATH = Pattern.compile("(/extract/|/legaleRappresentante/|/sede/)(.*?)(\\?)");
    private static final Pattern ELENCO_CF = Pattern.compile("(\"elencoCf\")\\s*:\\s*\\[\"(.*?)\"");
    private static final Pattern TAX_ID = Pattern.compile("(\"taxId\"|\"legalTaxId\"|\"businessTaxId\"|\"cfPersona\"|\"cfImpresa\"|\"codice_fiscale\")\\s*:\\s*\"(.*?)\"");
    private static final Pattern ADDRESS_1 = Pattern.compile("(\"description\"|\"at\"|\"address\"|\"zip\"|\"municipality\"|" +
            "\"municipalityDetails\"|\"province\"|\"foreignState\"|\"codiceStato\"|\"descrizioneStato\")\\s*:\\s*\"(.*?)\"");
    private static final Pattern ADDRESS_2 = Pattern.compile("(\"descrizioneLocalita\"|\"denominazione\"|\"numeroCivico\"|" +
            "\"digitalAddress\"|\"comune\"|\"toponimo\"|\"via\"|\"cap\"|\"provincia\"|\"nCivico\")\\s*:\\s*\"(.*?)\"");
    private static final Pattern ADDRESS_3 = Pattern.compile("(\"domicilioDigitale\"|\"tipo\"|\"codEnte\"" +
            "|\"domicilio_digitale\"|\"cod_amm\"|\"des_amm\")\\s*:\\s*\"(.*?)\"");
    private static final Pattern IDENTITY = Pattern.compile("(\"pecProfessionista\"|\"cf\"|\"codFiscale\"|\"codiceFiscale\"|" +
            "\"cognome\"|\"nome\"|\"sesso\"|\"dataNascita\")\\s*:\\s*\"(.*?)\"");
    private static final Pattern ACCESS_TOKEN = Pattern.compile("(\"access_token\")\\s*:\\s*\"(.*?)\"");
    private static final Pattern ELENCO_CF_NOT_JSON = Pattern.compile("(CF)\\s*=\\s*(.*)");

    private static final int STRING_LENGTH_UNDER_FOUR = 4;
    private static final int REMOVE_LAST_THREE_CHARACTERS = 3;
    private static final int NUMBER_GROUP_TO_BE_MATCHED = 2;

    private MaskDataUtils(){}

    public static String maskInformation(String dataBuffered) {
        if (dataBuffered == null) {
            return null;
        }

        dataBuffered = maskMatcher(CF_URI_PATH, dataBuffered);
        dataBuffered = maskMatcher(ELENCO_CF, dataBuffered);
        dataBuffered = maskMatcher(TAX_ID, dataBuffered);
        dataBuffered = maskMatcher(ADDRESS_1, dataBuffered);
        dataBuffered = maskMatcher(ADDRESS_2, dataBuffered);
        dataBuffered = maskMatcher(ADDRESS_3, dataBuffered);
        dataBuffered = maskMatcher(IDENTITY, dataBuffered);
        dataBuffered = maskMatcher(ACCESS_TOKEN, dataBuffered);
        dataBuffered = maskMatcher(ELENCO_CF_NOT_JSON,dataBuffered);

        return dataBuffered;
    }

    private static String maskMatcher(Pattern pattern, String dataBuffered) {
        Matcher matcher = pattern.matcher(dataBuffered);
        while(matcher.find()){
            String toBeMasked = matcher.group(NUMBER_GROUP_TO_BE_MATCHED);
            String valueMasked = mask(toBeMasked);
            if(!toBeMasked.isBlank()){
                dataBuffered = dataBuffered.replace(toBeMasked, valueMasked);
            }
        }
        return dataBuffered;
    }

    private static String mask(String unmasked) {
        if(unmasked.contains(",")){
            return maskAddress(unmasked);
        }
        else if(unmasked.contains("@")) {
            return maskEmailAddress(unmasked);
        }
        else {
            return maskString(unmasked);
        }
    }

    private static String maskAddress(String strAddress) {
        String[] parts = strAddress.split(",");
        StringBuilder masked = new StringBuilder();
        for (String part : parts) {
            masked.append(maskString(part)).append(",");
        }
        return masked.substring(0,masked.length()-1);
    }

    private static String maskEmailAddress(String strEmail) {
        String[] parts = strEmail.split("@");
        String strId = maskString(parts[0]);
        return strId + "@" + parts[1];
    }

    public static String maskString(String strText) {
        int start = 1;
        int end = strText.length()-REMOVE_LAST_THREE_CHARACTERS;
        String maskChar = String.valueOf('*');

        if("".equals(strText)){
            return "";
        }
        if(strText.length() < STRING_LENGTH_UNDER_FOUR){
            end = strText.length();
        }
        int maskLength = end - start;
        if(maskLength == 0){
            return maskChar;
        }
        String sbMaskString = maskChar.repeat(Math.max(0, maskLength));
        return strText.substring(0, start)
                + sbMaskString
                + strText.substring(start + maskLength);
    }

}
