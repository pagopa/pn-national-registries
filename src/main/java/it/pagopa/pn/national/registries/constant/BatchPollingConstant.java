package it.pagopa.pn.national.registries.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BatchPollingConstant {

    public static final String PK = "batchId";

    public static final String COL_POLLING_ID = "pollingId";
    public static final String COL_RETRY = "retry";

    public static final String COL_RETRY_IN_PROGRESS = "inProgressRetry";
    public static final String COL_TTL = "ttl";
    public static final String COL_STATUS = "status";
    public static final String COL_RESERVATION_ID = "reservationId";
    public static final String COL_LAST_RESERVED = "lastReserved";
    public static final String COL_TIMESTAMP = "timeStamp";

    public static final String GSI_S = "status-index";
}
