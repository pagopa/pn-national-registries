package it.pagopa.pn.national.registries.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProcessStatus {


    public static final String PROCESS_NAME_AGENZIA_ENTRATE_LEGAL = "[AGENZIA ENTRATE] legal";
    public static final String PROCESS_CHECKING_AGENZIAN_ENTRATE_LEGAL = "[AGENZIA ENTRATE] legal";
    public static final String PROCESS_SERVICE_AGENZIA_ENTRATE_LEGAL = "[AGENZIA ENTRATE] checking taxId and vatNumber";

    public static final String PROCESS_NAME_ANPR_ADDRESS = "[ANPR] address";
    public static final String PROCESS_SERVICE_ANPR_ADDRESS = "[ANPR] retrieving address";


    public static final String PROCESS_NAME_INIPEC_PEC = "[INIPEC] pec";

    public static final String PROCESS_NAME_REGISTRO_IMPRESE_ADDRESS = "[REGISTRO IMPRESE] address";
    public static final String PROCESS_SERVICE_REGISTRO_IMPRESE_ADDRESS = "[REGISTRO IMPRESE] retrieving address";

    public static final String PROCESS_NAME_INFO_CAMERE_LEGAL = "[INFO CAMERE] legal";
    public static final String PROCESS_CHEKING_INFO_CAMERE_LEGAL = "[INFO CAMERE] legal";
    public static final String PROCESS_SERVICE_INFO_CAMERE_LEGAL = "[INFO CAMERE] checking taxId";

    public static final String PROCESS_NAME_INFO_CAMERE_LEGAL_INSTITUTIONS = "[INFO CAMERE] legal institutions";
    public static final String PROCESS_CHEKING_INFO_CAMERE_LEGAL_INSTITUTIONS = "[INFO CAMERE] legal institutions";
    public static final String PROCESS_SERVICE_INFO_CAMERE_LEGAL_INSTITUTIONS = "[INFO CAMERE] checking taxId";
    public static final String PROCESS_SERVICE_INFO_CAMERE_GET_TOKEN = "[INFO CAMERE] getting token";

    public static final String PROCESS_NAME_INAD_ADDRESS = "[INAD] pec";
    public static final String PROCESS_SERVICE_INAD_ADDRESS = "[INAD] retrieving pec";
    public static final String PROCESS_SERVICE_WS05_PEC = "[IPA WS05] retrieving pec";
    public static final String PROCESS_SERVICE_WS23_PEC = "[IPA WS23] retrieving pec";


    public static final String PROCESS_SERVICE_PDND_TOKEN = "[PDND] retrieving token";
    public static final String PROCESS_CHECKING_VALIDATION_TAX_ID = "[VALIDATION] tax id";




}
