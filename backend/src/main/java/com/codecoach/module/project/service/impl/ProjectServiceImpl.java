package com.codecoach.module.project.service.impl;

import com.codecoach.module.project.dto.ProjectCreateRequest;
import com.codecoach.module.project.entity.Project;
import com.codecoach.module.project.mapper.ProjectMapper;
import com.codecoach.module.project.service.ProjectService;
import com.codecoach.module.project.vo.ProjectCreateResponse;
import com.codecoach.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final String STATUS_NORMAL = "NORMAL";

    private final ProjectMapper projectMapper;

    public ProjectServiceImpl(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Override
    @Transactional
    public ProjectCreateResponse createProject(ProjectCreateRequest request) {
        Project project = new Project();
        project.setUserId(UserContext.getCurrentUserId());
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setTechStack(request.getTechStack());
        project.setRole(request.getRole());
        project.setHighlights(request.getHighlights());
        project.setDifficulties(request.getDifficulties());
        project.setStatus(STATUS_NORMAL);
        project.setIsDeleted(0);

        projectMapper.insert(project);
        return new ProjectCreateResponse(project.getId());
    }
}
