package com.codecoach.common.concurrency;

import com.codecoach.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SingleFlightService {

    private static final Logger log = LoggerFactory.getLogger(SingleFlightService.class);

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Set<String> localLocks = ConcurrentHashMap.newKeySet();
    private final SingleFlightTraceRecorder traceRecorder;

    public SingleFlightService(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            SingleFlightTraceRecorder traceRecorder
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.traceRecorder = traceRecorder;
    }

    public <T> T execute(
            String requestKey,
            Duration lockTtl,
            Duration cacheTtl,
            Class<T> resultType,
            Supplier<T> task,
            Supplier<T> fallbackOnLock,
            int inProgressCode,
            String inProgressMessage
    ) {
        if (!StringUtils.hasText(requestKey) || task == null || resultType == null) {
            throw new IllegalArgumentException("single-flight arguments are invalid");
        }
        String cacheKey = cacheKey(requestKey);
        long start = System.currentTimeMillis();
        T cached = readCache(cacheKey, resultType);
        if (cached != null) {
            record(requestKey, "CACHE_HIT", true, start, null);
            return cached;
        }
        String lockValue = UUID.randomUUID().toString();
        if (!tryLock(lockKey(requestKey), lockValue, lockTtl)) {
            T fallback = fallbackOnLock == null ? null : fallbackOnLock.get();
            if (fallback != null) {
                record(requestKey, "LOCK_FALLBACK", true, start, "LOCK_BUSY_EXISTING_RESULT");
                return fallback;
            }
            record(requestKey, "LOCK_REJECTED", false, start, "LOCK_BUSY");
            throw new BusinessException(inProgressCode, inProgressMessage);
        }
        try {
            record(requestKey, "LOCK_ACQUIRED", true, start, null);
            cached = readCache(cacheKey, resultType);
            if (cached != null) {
                record(requestKey, "CACHE_HIT_AFTER_LOCK", true, start, null);
                return cached;
            }
            T value = task.get();
            writeCache(cacheKey, value, cacheTtl);
            record(requestKey, "EXECUTE_SUCCESS", true, start, null);
            return value;
        } catch (RuntimeException exception) {
            record(requestKey, "EXECUTE_FAILED", false, start, exception.getClass().getSimpleName());
            throw exception;
        } finally {
            releaseLock(lockKey(requestKey), lockValue);
        }
    }

    private void record(String requestKey, String action, boolean success, long startTime, String fallbackReason) {
        traceRecorder.record(requestKey, action, success, System.currentTimeMillis() - startTime, fallbackReason);
    }

    private <T> T readCache(String cacheKey, Class<T> resultType) {
        try {
            String json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, resultType);
        } catch (Exception exception) {
            return null;
        }
    }

    private void writeCache(String cacheKey, Object value, Duration ttl) {
        if (value == null || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception exception) {
            log.warn("Single-flight cache write failed, cacheKey={}", cacheKey, exception);
        }
    }

    private boolean tryLock(String lockKey, String lockValue, Duration ttl) {
        try {
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttl);
            if (Boolean.TRUE.equals(locked)) {
                return true;
            }
        } catch (Exception exception) {
            log.warn("Redis single-flight lock unavailable, fallback to local lock, key={}", lockKey, exception);
            return localLocks.add(lockKey);
        }
        return localLocks.add(lockKey);
    }

    private void releaseLock(String lockKey, String lockValue) {
        try {
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, java.util.List.of(lockKey), lockValue);
        } catch (Exception exception) {
            log.debug("Redis single-flight lock release failed, key={}", lockKey, exception);
        } finally {
            localLocks.remove(lockKey);
        }
    }

    private String lockKey(String requestKey) {
        return "singleflight:lock:" + requestKey;
    }

    private String cacheKey(String requestKey) {
        return "singleflight:cache:" + requestKey;
    }
}
