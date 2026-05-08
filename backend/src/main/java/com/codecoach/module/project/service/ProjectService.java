package com.codecoach.module.project.service;

import com.codecoach.common.result.PageResult;
import com.codecoach.module.project.dto.ProjectCreateRequest;
import com.codecoach.module.project.dto.ProjectPageRequest;
import com.codecoach.module.project.vo.ProjectCreateResponse;
import com.codecoach.module.project.vo.ProjectVO;

public interface ProjectService {

    ProjectCreateResponse createProject(ProjectCreateRequest request);

    PageResult<ProjectVO> pageProjects(ProjectPageRequest request);

    ProjectVO getProjectDetail(Long id);
}
