package com.codecoach.module.project.controller;

import com.codecoach.common.result.PageResult;
import com.codecoach.common.result.Result;
import com.codecoach.module.project.dto.ProjectCreateRequest;
import com.codecoach.module.project.dto.ProjectPageRequest;
import com.codecoach.module.project.service.ProjectService;
import com.codecoach.module.project.vo.ProjectCreateResponse;
import com.codecoach.module.project.vo.ProjectVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public Result<ProjectCreateResponse> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return Result.success(projectService.createProject(request));
    }

    @GetMapping
    public Result<PageResult<ProjectVO>> pageProjects(ProjectPageRequest request) {
        return Result.success(projectService.pageProjects(request));
    }

    @GetMapping("/{id}")
    public Result<ProjectVO> getProjectDetail(@PathVariable Long id) {
        return Result.success(projectService.getProjectDetail(id));
    }
}
