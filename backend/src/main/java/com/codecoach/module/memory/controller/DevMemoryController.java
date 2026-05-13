package com.codecoach.module.memory.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.memory.model.MemorySemanticHit;
import com.codecoach.module.memory.service.UserMemoryService;
import com.codecoach.security.UserContext;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/memory")
public class DevMemoryController {

    private final UserMemoryService userMemoryService;

    public DevMemoryController(UserMemoryService userMemoryService) {
        this.userMemoryService = userMemoryService;
    }

    @PostMapping("/semantic-index")
    public Result<SemanticIndexResponse> indexCurrentUserMemory() {
        int indexed = userMemoryService.indexActiveSemanticMemory(UserContext.getCurrentUserId());
        return Result.success(new SemanticIndexResponse(indexed));
    }

    @GetMapping("/summary")
    public Result<?> summary() {
        return Result.success(userMemoryService.getSummary(UserContext.getCurrentUserId()));
    }

    @PostMapping("/{memoryId}/archive")
    public Result<Boolean> archive(@org.springframework.web.bind.annotation.PathVariable Long memoryId) {
        return Result.success(userMemoryService.archiveMemory(UserContext.getCurrentUserId(), memoryId));
    }

    @PostMapping("/{memoryId}/inaccurate")
    public Result<Boolean> inaccurate(@org.springframework.web.bind.annotation.PathVariable Long memoryId) {
        return Result.success(userMemoryService.markMemoryInaccurate(UserContext.getCurrentUserId(), memoryId));
    }

    @GetMapping("/semantic-search")
    public Result<List<MemorySemanticHit>> search(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "5") Integer topK
    ) {
        return Result.success(userMemoryService.semanticSearch(UserContext.getCurrentUserId(), query, topK));
    }

    public record SemanticIndexResponse(int indexedCount) {
    }
}
