package com.woori.woorirelay.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * WebSocket 세션 안전 종료 유틸리티.
 */
@Slf4j
@Component
public class WebSocketSessionCloser {

    public void closeQuietly(WebSocketSession session, CloseStatus status) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close(status);
        } catch (IOException ex) {
            log.debug("[WebSocketCloser] Close I/O error wsId={}", session.getId(), ex);
        }
    }
}
