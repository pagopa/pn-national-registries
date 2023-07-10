package it.pagopa.pn.national.registries.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BatchRequestConstant {

    public static final String PK = "correlationId";

    public static final String COL_CF = "cf";
    public static final String COL_BATCH_ID = "batchId";
    public static final String COL_RETRY = "retry";
    public static final String COL_TTL = "ttl";
    public static final String COL_CLIENT_ID = "clientId";
    public static final String COL_STATUS = "status";
    public static final String COL_LAST_RESERVED = "lastReserved";
    public static final String COL_RESERVATION_ID = "reservationId";
    public static final String COL_TIMESTAMP = "timeStamp";
    public static final String COL_SEND_STATUS = "sendStatus";
    public static final String COL_MESSAGE = "message";

    public static final String COL_ESERVICE = "eservice";

    public static final String GSI_BL = "batchId-lastReserved-index";
    public static final String GSI_S = "status-index";
    public static final String GSI_SSL = "sendStatus-lastReserved-index";
}
