package com.woori.woorirelay.integration.cti;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CtiEscalationRequest {

    String sessionId;
    String customerId;
    String priority;
    String fdsFlag;
    String lastSttText;
    String reason;
    String eventType;
}
