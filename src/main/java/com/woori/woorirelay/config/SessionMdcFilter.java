/**
 *
 *
 * <pre>
 * <b>Description  : HTTP 요청 MDC correlationId 주입 필터</b>
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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 분산 추적용 MDC — sessionId/correlationId를 로그에 자동 주입.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SessionMdcFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID = "correlationId";
    private static final String SESSION_ID = "sessionId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader("X-Correlation-Id");

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put(CORRELATION_ID, correlationId);
        response.setHeader("X-Correlation-Id", correlationId);

        String sessionId = extractSessionIdFromPath(request.getRequestURI());
        if (sessionId != null) {
            MDC.put(SESSION_ID, sessionId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID);
            MDC.remove(SESSION_ID);
        }
    }

    private String extractSessionIdFromPath(String uri) {
        if (uri == null || !uri.contains("/voice/")) {
            return null;
        }
        int idx = uri.lastIndexOf('/');
        if (idx < 0 || idx == uri.length() - 1) {
            return null;
        }
        return uri.substring(idx + 1);
    }
}
