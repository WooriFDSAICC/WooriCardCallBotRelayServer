/**
 *
 *
 * <pre>
 * <b>Description  : Prometheus 운영 메트릭 등록</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.config
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.22        RosieOh     최초생성
 *        </pre>
 */

package com.woori.woorirelay.config;

import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.registry.VoiceSessionRegistry;
import com.woori.woorirelay.service.CtiEscalationOutboxService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class RelayMetrics {

    private final Counter gatewayConnectionFailures;
    private final Timer gatewaySttResponseSeconds;
    private final Counter gatewayEscalationsTotal;

    public RelayMetrics(
            MeterRegistry meterRegistry,
            VoiceSessionRegistry sessionRegistry,
            CtiEscalationOutboxService outboxService
    ) {
        Gauge.builder("relay.active_sessions", sessionRegistry, VoiceSessionRegistry::activeSessionCount)
                .description("Active voice WebSocket sessions on this node")
                .register(meterRegistry);

        for (CallDirection direction : CallDirection.values()) {
            Gauge.builder("relay.active_sessions", sessionRegistry, reg -> reg.activeSessionCount(direction))
                    .description("Active voice WebSocket sessions by direction")
                    .tags(Tags.of("direction", direction.name().toLowerCase()))
                    .register(meterRegistry);
        }

        Gauge.builder("relay.cti_outbox_pending", outboxService, CtiEscalationOutboxService::pendingCount)
                .description("Pending CTI escalation outbox items")
                .register(meterRegistry);

        this.gatewayConnectionFailures = Counter.builder("relay.gateway_connection_failures")
                .description("Failed Relay to Callbot Gateway WebSocket connections")
                .register(meterRegistry);

        this.gatewaySttResponseSeconds = Timer.builder("relay.gateway_stt_response_seconds")
                .description("Gateway STT JSON response latency observed at Relay")
                .register(meterRegistry);

        this.gatewayEscalationsTotal = Counter.builder("relay.gateway_escalations_total")
                .description("Agent escalation events received from Gateway")
                .register(meterRegistry);
    }

    public void recordGatewayConnectionFailure() {
        gatewayConnectionFailures.increment();
    }

    public Timer.Sample startGatewaySttResponseTimer() {
        return Timer.start();
    }

    public void recordGatewayEscalation() {
        gatewayEscalationsTotal.increment();
    }
}
