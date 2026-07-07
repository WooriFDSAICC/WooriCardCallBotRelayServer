/**
 *
 *
 * <pre>
 * <b>Description  : 양방향 WebSocket 세션 엔트리</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.session
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

package com.woori.woorirelay.session;

import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.support.SessionRegistryKeys;
import lombok.Getter;
import org.springframework.web.socket.WebSocketSession;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 단일 통화 세션의 양방향 WebSocket 핸들 및 라이프사이클 멱등성 플래그.
 */
@Getter
public class VoiceSessionEntry {

    private final String registryKey;
    private final String sessionId;
    private final CallDirection direction;
    private final String campaignId;
    private final WebSocketSession clientSession;
    private volatile WebSocketSession backendSession;
    private volatile com.woori.woorirelay.handler.FastApiBackendHandler backendHandler;
    private volatile WebSocketSession workerSession;
    private volatile com.woori.woorirelay.handler.TtsWorkerBackendHandler workerHandler;
    private final AtomicBoolean escalated = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();

    // ── 업링크 오디오 백프레셔 큐 ──
    // WS read 스레드는 큐에 적재만 하고, 실제 blocking send 는 전용 executor 가 처리한다.
    // draining 플래그로 세션당 단일 드레인 태스크만 돌게 해 프레임 순서를 보장한다.
    private final Queue<byte[]> audioQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger audioQueueDepth = new AtomicInteger(0);
    private final AtomicBoolean audioDraining = new AtomicBoolean(false);

    /** 큐에 오디오 청크를 적재. 용량 초과 시 false(드랍). */
    public boolean offerAudio(byte[] chunk, int capacity) {
        if (capacity > 0 && audioQueueDepth.get() >= capacity) {
            return false;
        }
        audioQueue.offer(chunk);
        audioQueueDepth.incrementAndGet();
        return true;
    }

    public byte[] pollAudio() {
        byte[] chunk = audioQueue.poll();
        if (chunk != null) {
            audioQueueDepth.decrementAndGet();
        }
        return chunk;
    }

    public boolean audioQueueEmpty() {
        return audioQueue.isEmpty();
    }

    public void clearAudio() {
        audioQueue.clear();
        audioQueueDepth.set(0);
    }

    public VoiceSessionEntry(
            String sessionId,
            CallDirection direction,
            String campaignId,
            WebSocketSession clientSession
    ) {
        this.sessionId = sessionId;
        this.direction = direction;
        this.campaignId = campaignId;
        this.registryKey = SessionRegistryKeys.registryKey(direction, sessionId);
        this.clientSession = clientSession;
    }

    public void bindBackendSession(
            WebSocketSession backendSession,
            com.woori.woorirelay.handler.FastApiBackendHandler backendHandler
    ) {
        this.backendSession = backendSession;
        this.backendHandler = backendHandler;
    }

    public void bindWorkerSession(
            WebSocketSession workerSession,
            com.woori.woorirelay.handler.TtsWorkerBackendHandler workerHandler
    ) {
        this.workerSession = workerSession;
        this.workerHandler = workerHandler;
    }

    public boolean isActive() {
        return !closed.get();
    }

    public boolean markEscalatedOnce() {
        return escalated.compareAndSet(false, true);
    }

    public boolean markClosedOnce() {
        return closed.compareAndSet(false, true);
    }
}
