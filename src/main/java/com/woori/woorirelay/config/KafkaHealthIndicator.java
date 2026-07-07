/**
 *
 *
 * <pre>
 * <b>Description  : Kafka 브로커 도달성 헬스 인디케이터</b>
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
 *  2026.07.07        RosieOh     M8 Kafka health 추가
 *        </pre>
 */

package com.woori.woorirelay.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 브로커 도달성을 readiness 에 반영한다. 브로커 장애 시 인스턴스가 UP 으로 트래픽을 계속
 * 받으며 FDS 이벤트를 유실하는 문제(M8)를 막는다. 빈 이름이 "kafka" 헬스 컨트리뷰터가 된다.
 */
@Slf4j
@Component("kafkaHealthIndicator")
public class KafkaHealthIndicator implements HealthIndicator {

    private static final int TIMEOUT_MS = 2_000;

    private final AdminClient adminClient;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    @Override
    public Health health() {
        try {
            DescribeClusterResult cluster = adminClient.describeCluster();
            int nodeCount = cluster.nodes().get(TIMEOUT_MS, TimeUnit.MILLISECONDS).size();
            String clusterId = cluster.clusterId().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (nodeCount <= 0) {
                return Health.down().withDetail("reason", "no brokers available").build();
            }
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodes", nodeCount)
                    .build();
        } catch (Exception ex) {
            log.warn("[KafkaHealth] Broker unreachable: {}", ex.toString());
            return Health.down(ex).build();
        }
    }

    @PreDestroy
    public void close() {
        if (adminClient != null) {
            adminClient.close(java.time.Duration.ofSeconds(1));
        }
    }
}
