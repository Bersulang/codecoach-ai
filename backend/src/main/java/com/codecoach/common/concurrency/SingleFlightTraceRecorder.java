package com.codecoach.common.concurrency;

import com.codecoach.security.UserContext;
import com.codecoach.module.observability.mapper.SingleFlightTraceMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SingleFlightTraceRecorder {

    private static final Logger log = LoggerFactory.getLogger(SingleFlightTraceRecorder.class);
    private static final int MAX_KEY_LENGTH = 180;
    private static final int MAX_REASON_LENGTH = 240;

    private final SingleFlightTraceMapper mapper;

    public SingleFlightTraceRecorder(SingleFlightTraceMapper mapper) {
        this.mapper = mapper;
    }

    public void record(String requestKey, String action, boolean success, long latencyMs, String fallbackReason) {
        try {
            SingleFlightTrace trace = new SingleFlightTrace();
            trace.setTraceId(UUID.randomUUID().toString().replace("-", ""));
            trace.setUserId(currentUserIdOrNull());
            trace.setRequestKey(safe(requestKey, MAX_KEY_LENGTH));
            trace.setAction(safe(action, 64));
            trace.setSuccess(success ? 1 : 0);
            trace.setLatencyMs(Math.max(0, latencyMs));
            trace.setFallbackReason(safe(fallbackReason, MAX_REASON_LENGTH));
            trace.setCreatedAt(LocalDateTime.now());
            mapper.insert(trace);
        } catch (Exception exception) {
            log.debug("Single-flight trace record failed, action={}, key={}", action, requestKey, exception);
        }
    }

    private Long currentUserIdOrNull() {
        try {
            return UserContext.getCurrentUserId();
        } catch (Exception exception) {
            return null;
        }
    }

    private String safe(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value
                .replaceAll("(?i)(api[_-]?key|secret|token)\\s*[:=]\\s*\\S+", "$1:[REDACTED]")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
