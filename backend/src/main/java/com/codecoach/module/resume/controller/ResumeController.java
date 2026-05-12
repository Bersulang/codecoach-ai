package com.codecoach.module.resume.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.resume.dto.ResumeCreateRequest;
import com.codecoach.module.resume.dto.ResumeProjectLinkRequest;
import com.codecoach.module.resume.service.ResumeService;
import com.codecoach.module.resume.vo.ResumeListItemVO;
import com.codecoach.module.resume.vo.ResumeProfileVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public Result<ResumeProfileVO> create(@Valid @RequestBody ResumeCreateRequest request) {
        return Result.success(resumeService.create(request));
    }

    @GetMapping
    public Result<List<ResumeListItemVO>> list() {
        return Result.success(resumeService.list());
    }

    @GetMapping("/{id}")
    public Result<ResumeProfileVO> detail(@PathVariable Long id) {
        return Result.success(resumeService.detail(id));
    }

    @PostMapping("/{id}/analyze")
    public Result<ResumeProfileVO> analyze(@PathVariable Long id) {
        return Result.success(resumeService.analyze(id));
    }

    @PutMapping("/{resumeId}/projects/{resumeProjectId}/link")
    public Result<ResumeProfileVO> linkProject(
            @PathVariable Long resumeId,
            @PathVariable Long resumeProjectId,
            @Valid @RequestBody ResumeProjectLinkRequest request
    ) {
        return Result.success(resumeService.linkProject(resumeId, resumeProjectId, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resumeService.delete(id);
        return Result.success();
    }
}
