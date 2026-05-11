package com.codecoach.module.rag.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.rag.model.RagSearchRequest;
import com.codecoach.module.rag.model.RagSearchResponse;
import com.codecoach.module.rag.service.RagRetrievalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagSearchController {

    private final RagRetrievalService ragRetrievalService;

    public RagSearchController(RagRetrievalService ragRetrievalService) {
        this.ragRetrievalService = ragRetrievalService;
    }

    @PostMapping("/search")
    public Result<RagSearchResponse> search(@Valid @RequestBody RagSearchRequest request) {
        return Result.success(ragRetrievalService.search(request));
    }
}
