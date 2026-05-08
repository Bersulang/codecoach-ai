package com.codecoach.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codecoach.common.result.PageResult;
import com.codecoach.module.project.dto.ProjectCreateRequest;
import com.codecoach.module.project.dto.ProjectPageRequest;
import com.codecoach.module.project.entity.Project;
import com.codecoach.module.project.mapper.ProjectMapper;
import com.codecoach.module.project.service.ProjectService;
import com.codecoach.module.project.vo.ProjectCreateResponse;
import com.codecoach.module.project.vo.ProjectVO;
import com.codecoach.security.UserContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final String STATUS_NORMAL = "NORMAL";

    private static final long DEFAULT_PAGE_NUM = 1L;

    private static final long DEFAULT_PAGE_SIZE = 10L;

    private static final long MAX_PAGE_SIZE = 100L;

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

    @Override
    public PageResult<ProjectVO> pageProjects(ProjectPageRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        long pageNum = normalizePageNum(request.getPageNum());
        long pageSize = normalizePageSize(request.getPageSize());

        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, currentUserId)
                .eq(Project::getIsDeleted, 0)
                .like(StringUtils.hasText(request.getKeyword()), Project::getName, request.getKeyword())
                .orderByDesc(Project::getCreatedAt);

        Page<Project> page = projectMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        List<ProjectVO> records = page.getRecords().stream()
                .map(this::toProjectVO)
                .toList();

        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private long normalizePageNum(Long pageNum) {
        if (pageNum == null || pageNum < DEFAULT_PAGE_NUM) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private ProjectVO toProjectVO(Project project) {
        return new ProjectVO(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getTechStack(),
                project.getRole(),
                project.getHighlights(),
                project.getDifficulties(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
