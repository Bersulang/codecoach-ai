package com.codecoach.module.rag.service.impl;

import com.codecoach.module.rag.service.RagQueryRewriteService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RuleBasedRagQueryRewriteService implements RagQueryRewriteService {

    private static final Map<String, String> EXPANSIONS = new LinkedHashMap<>();

    static {
        EXPANSIONS.put("redis", "Redis 缓存击穿 缓存穿透 缓存雪崩 缓存一致性 分布式锁 面试题 项目表达");
        EXPANSIONS.put("缓存", "Redis 缓存击穿 缓存穿透 缓存雪崩 缓存一致性 热点 key 面试题");
        EXPANSIONS.put("mysql", "MySQL 索引 B+树 事务 MVCC 锁机制 查询优化 面试题");
        EXPANSIONS.put("jvm", "JVM 内存区域 类加载 GC G1 调优 OOM 面试题");
        EXPANSIONS.put("线程池", "Java 线程池 核心参数 拒绝策略 队列选择 线程隔离 面试题");
        EXPANSIONS.put("项目", "项目难点 技术选型 个人贡献 指标 线上问题 面试追问");
        EXPANSIONS.put("简历", "简历风险 项目经历 量化指标 技术细节 面试追问");
    }

    @Override
    public String rewrite(String query, List<String> sourceTypes) {
        if (!StringUtils.hasText(query)) {
            return query;
        }
        String normalized = query.trim();
        String lower = normalized.toLowerCase();
        for (Map.Entry<String, String> entry : EXPANSIONS.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        if (normalized.length() <= 8 && normalized.contains("练")) {
            return normalized + " Java 后端 面试题 常见追问 答题思路";
        }
        return normalized;
    }
}
