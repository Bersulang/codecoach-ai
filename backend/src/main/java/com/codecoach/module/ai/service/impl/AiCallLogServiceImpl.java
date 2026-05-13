package com.codecoach.module.ai.service.impl;

import com.codecoach.module.ai.entity.AiCallLog;
import com.codecoach.module.ai.mapper.AiCallLogMapper;
import com.codecoach.module.ai.service.AiCallLogService;
import com.codecoach.module.observability.trace.TraceContextHolder;
import java.time.LocalDateTime;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiCallLogServiceImpl implements AiCallLogService {

    private static final Logger log = LoggerFactory.getLogger(AiCallLogServiceImpl.class);

    private final AiCallLogMapper aiCallLogMapper;

    public AiCallLogServiceImpl(AiCallLogMapper aiCallLogMapper) {
        this.aiCallLogMapper = aiCallLogMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiCallLog callLog) {
        try {
            if (callLog.getCreatedAt() == null) {
                callLog.setCreatedAt(LocalDateTime.now());
            }
            if (!StringUtils.hasText(callLog.getTraceId())) {
                callLog.setTraceId(TraceContextHolder.getTraceId());
            }
            aiCallLogMapper.insert(callLog);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist AI call log", exception);
        }
    }
}
