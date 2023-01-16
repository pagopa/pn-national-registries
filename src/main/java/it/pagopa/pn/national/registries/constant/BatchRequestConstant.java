package it.pagopa.pn.national.registries.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class BatchRequestConstant {
    public static final String PK = "correlationId";
    public static final String GSI_BL = "batchId-lastReserved-index";
    public static final String GSI_BL_PK = "batchId";
    public static final String GSI_BL_SK = "lastReserved";

    public static final String GSI_S = "status-index";
    public static final String GSI_S_PK = "status";

}
