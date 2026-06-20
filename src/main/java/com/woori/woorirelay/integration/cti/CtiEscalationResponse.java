package com.woori.woorirelay.integration.cti;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CtiEscalationResponse {

    boolean accepted;
    String queueId;
    String message;
}
