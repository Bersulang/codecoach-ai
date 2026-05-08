package com.codecoach.module.project.service;

import com.codecoach.module.project.dto.ProjectCreateRequest;
import com.codecoach.module.project.vo.ProjectCreateResponse;

public interface ProjectService {

    ProjectCreateResponse createProject(ProjectCreateRequest request);
}
