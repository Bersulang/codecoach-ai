package com.codecoach.module.resume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.common.storage.AliyunOssService;
import com.codecoach.module.document.entity.UserDocument;
import com.codecoach.module.document.mapper.UserDocumentMapper;
import com.codecoach.module.document.model.ParsedDocument;
import com.codecoach.module.document.service.DocumentParseService;
import com.codecoach.module.project.entity.Project;
import com.codecoach.module.project.mapper.ProjectMapper;
import com.codecoach.module.resume.dto.ResumeCreateRequest;
import com.codecoach.module.resume.dto.ResumeProjectLinkRequest;
import com.codecoach.module.resume.dto.ResumeProjectSaveRequest;
import com.codecoach.module.resume.entity.ResumeProfile;
import com.codecoach.module.resume.entity.ResumeProjectExperience;
import com.codecoach.module.resume.mapper.ResumeProfileMapper;
import com.codecoach.module.resume.mapper.ResumeProjectExperienceMapper;
import com.codecoach.module.resume.model.ResumeAnalysisResult;
import com.codecoach.module.resume.service.AiResumeAnalysisService;
import com.codecoach.module.resume.service.ResumeService;
import com.codecoach.module.resume.vo.ResumeListItemVO;
import com.codecoach.module.resume.vo.ResumeProfileVO;
import com.codecoach.module.resume.vo.ResumeProjectDraftVO;
import com.codecoach.module.resume.vo.ResumeProjectExperienceVO;
import com.codecoach.module.resume.vo.ResumeProjectSaveResponseVO;
import com.codecoach.security.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ResumeServiceImpl implements ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeServiceImpl.class);

    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;
    private static final int MAX_RESUME_CHARS = 14000;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ANALYZING = "ANALYZING";
    private static final String STATUS_ANALYZED = "ANALYZED";
    private static final String STATUS_FAILED = "FAILED";
    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("TXT", "MARKDOWN", "PDF");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(?:\\+?86[-\\s]?)?1[3-9]\\d{9}(?!\\d)");

    private final ResumeProfileMapper resumeProfileMapper;
    private final ResumeProjectExperienceMapper resumeProjectExperienceMapper;
    private final UserDocumentMapper userDocumentMapper;
    private final ProjectMapper projectMapper;
    private final AliyunOssService aliyunOssService;
    private final DocumentParseService documentParseService;
    private final AiResumeAnalysisService aiResumeAnalysisService;
    private final ObjectMapper objectMapper;

    public ResumeServiceImpl(
            ResumeProfileMapper resumeProfileMapper,
            ResumeProjectExperienceMapper resumeProjectExperienceMapper,
            UserDocumentMapper userDocumentMapper,
            ProjectMapper projectMapper,
            AliyunOssService aliyunOssService,
            DocumentParseService documentParseService,
            AiResumeAnalysisService aiResumeAnalysisService,
            ObjectMapper objectMapper
    ) {
        this.resumeProfileMapper = resumeProfileMapper;
        this.resumeProjectExperienceMapper = resumeProjectExperienceMapper;
        this.userDocumentMapper = userDocumentMapper;
        this.projectMapper = projectMapper;
        this.aliyunOssService = aliyunOssService;
        this.documentParseService = documentParseService;
        this.aiResumeAnalysisService = aiResumeAnalysisService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ResumeProfileVO create(ResumeCreateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        UserDocument document = getCurrentUserDocument(request.getDocumentId(), userId);
        validateDocumentForResume(document);

        LocalDateTime now = LocalDateTime.now();
        ResumeProfile profile = new ResumeProfile();
        profile.setUserId(userId);
        profile.setDocumentId(document.getId());
        profile.setTitle(normalizeTitle(request.getTitle(), document.getTitle()));
        profile.setTargetRole(normalizeTargetRole(request.getTargetRole()));
        profile.setAnalysisStatus(STATUS_PENDING);
        profile.setSummary(null);
        profile.setAnalysisResult(null);
        profile.setErrorMessage(null);
        profile.setIsDeleted(NOT_DELETED);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        resumeProfileMapper.insert(profile);
        return detail(profile.getId());
    }

    @Override
    public List<ResumeListItemVO> list() {
        Long userId = UserContext.getCurrentUserId();
        List<ResumeProfile> profiles = resumeProfileMapper.selectList(new LambdaQueryWrapper<ResumeProfile>()
                .eq(ResumeProfile::getUserId, userId)
                .eq(ResumeProfile::getIsDeleted, NOT_DELETED)
                .orderByDesc(ResumeProfile::getCreatedAt)
                .orderByDesc(ResumeProfile::getId));
        return profiles.stream().map(this::toListItemVO).toList();
    }

    @Override
    public ResumeProfileVO detail(Long id) {
        ResumeProfile profile = getCurrentUserResume(id);
        return toProfileVO(profile, listProjectExperiences(profile.getId()));
    }

    @Override
    public ResumeProfileVO analyze(Long id) {
        ResumeProfile profile = getCurrentUserResume(id);
        UserDocument document = getCurrentUserDocument(profile.getDocumentId(), profile.getUserId());
        validateDocumentForResume(document);

        markAnalyzing(profile);
        try {
            String resumeText = loadSanitizedResumeText(document);
            ResumeAnalysisResult result = aiResumeAnalysisService.analyze(resumeText, profile.getTargetRole());
            LocalDateTime now = LocalDateTime.now();
            profile.setAnalysisStatus(STATUS_ANALYZED);
            profile.setSummary(safeText(result.getSummary()));
            profile.setAnalysisResult(toJson(result));
            profile.setErrorMessage(null);
            profile.setAnalyzedAt(now);
            profile.setUpdatedAt(now);
            resumeProfileMapper.updateById(profile);
            replaceProjectExperiences(profile, result);
            log.info("Resume analyzed, userId={}, resumeId={}, documentId={}, projectCount={}",
                    profile.getUserId(),
                    profile.getId(),
                    document.getId(),
                    result.getProjectExperiences() == null ? 0 : result.getProjectExperiences().size());
            return detail(profile.getId());
        } catch (BusinessException exception) {
            markFailed(profile, exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            markFailed(profile, "简历分析失败，请稍后重试");
            log.warn("Resume analysis failed, userId={}, resumeId={}, documentId={}, error={}",
                    profile.getUserId(),
                    profile.getId(),
                    document.getId(),
                    abbreviate(exception.getMessage()));
            throw new BusinessException(3003, "AI 调用失败，请稍后重试");
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ResumeProfile profile = getCurrentUserResume(id);
        LocalDateTime now = LocalDateTime.now();
        profile.setIsDeleted(DELETED);
        profile.setDeletedAt(now);
        profile.setUpdatedAt(now);
        resumeProfileMapper.updateById(profile);
    }

    @Override
    @Transactional
    public ResumeProfileVO linkProject(Long resumeId, Long resumeProjectId, ResumeProjectLinkRequest request) {
        ResumeProfile profile = getCurrentUserResume(resumeId);
        Project project = getCurrentUserProject(request.getProjectId(), profile.getUserId());
        ResumeProjectExperience experience = getCurrentUserResumeProjectExperience(profile, resumeProjectId);
        experience.setProjectId(project.getId());
        experience.setMatchReason("用户手动关联项目档案");
        experience.setUpdatedAt(LocalDateTime.now());
        resumeProjectExperienceMapper.updateById(experience);
        return detail(profile.getId());
    }

    @Override
    public ResumeProjectDraftVO generateProjectDraft(Long resumeId, Long resumeProjectId) {
        ResumeProfile profile = getCurrentUserResume(resumeId);
        ResumeProjectExperience experience = getCurrentUserResumeProjectExperience(profile, resumeProjectId);
        List<String> techStack = parseStringList(experience.getTechStack());
        List<String> highlights = parseStringList(experience.getHighlights());
        List<String> risks = parseStringList(experience.getRiskPoints());
        List<String> questions = parseStringList(experience.getRecommendedQuestions());
        List<String> pendingItems = new ArrayList<>();

        if (!StringUtils.hasText(experience.getDescription())) {
            pendingItems.add("补充项目背景、目标用户或业务场景");
        }
        if (techStack.isEmpty()) {
            pendingItems.add("补充真实使用过的技术栈");
        }
        if (!StringUtils.hasText(experience.getRole())) {
            pendingItems.add("补充个人职责、负责模块和协作边界");
        }
        if (highlights.isEmpty()) {
            pendingItems.add("补充可以被你真实解释清楚的项目亮点");
        }
        if (risks.isEmpty() && questions.isEmpty()) {
            pendingItems.add("补充面试中可能被追问的难点或风险点");
        }

        ResumeProjectDraftVO draft = new ResumeProjectDraftVO();
        draft.setResumeId(profile.getId());
        draft.setResumeProjectId(experience.getId());
        draft.setName(limit(defaultText(experience.getProjectName(), "未命名项目"), 128));
        draft.setDescription(defaultText(experience.getDescription(), "待补充：请根据简历原文补充项目背景、业务目标和核心功能。"));
        draft.setTechStack(techStack.isEmpty() ? "待补充" : String.join("、", techStack));
        draft.setRole(defaultText(experience.getRole(), "待补充：请补充你本人负责的模块、职责边界和关键产出。"));
        draft.setHighlights(highlights.isEmpty() ? "待补充：只填写你真实参与并能解释清楚的亮点。" : joinLines(highlights));
        draft.setDifficulties(buildDifficulties(risks, questions));
        draft.setRiskPoints(risks);
        draft.setPendingItems(pendingItems);
        draft.setSafetyNotice("草稿仅基于简历分析结果生成；缺失或不确定内容已标记为待补充，请确认真实准确后再保存。");
        return draft;
    }

    @Override
    @Transactional
    public ResumeProjectSaveResponseVO saveProjectFromDraft(Long resumeId, Long resumeProjectId, ResumeProjectSaveRequest request) {
        ResumeProfile profile = getCurrentUserResume(resumeId);
        ResumeProjectExperience experience = getCurrentUserResumeProjectExperience(profile, resumeProjectId);

        Project project = new Project();
        project.setUserId(profile.getUserId());
        project.setName(limit(request.getName(), 128));
        project.setDescription(safeText(request.getDescription()));
        project.setTechStack(safeText(request.getTechStack()));
        project.setRole(safeText(request.getRole()));
        project.setHighlights(safeText(request.getHighlights()));
        project.setDifficulties(safeText(request.getDifficulties()));
        project.setStatus("NORMAL");
        project.setIsDeleted(NOT_DELETED);
        projectMapper.insert(project);

        experience.setProjectId(project.getId());
        experience.setMatchReason("由简历项目经历确认生成");
        experience.setUpdatedAt(LocalDateTime.now());
        resumeProjectExperienceMapper.updateById(experience);

        return new ResumeProjectSaveResponseVO(project.getId(), detail(profile.getId()));
    }

    private void replaceProjectExperiences(ResumeProfile profile, ResumeAnalysisResult result) {
        resumeProjectExperienceMapper.delete(new LambdaQueryWrapper<ResumeProjectExperience>()
                .eq(ResumeProjectExperience::getResumeId, profile.getId())
                .eq(ResumeProjectExperience::getUserId, profile.getUserId()));
        List<ResumeAnalysisResult.ProjectExperienceItem> items = result.getProjectExperiences() == null
                ? List.of()
                : result.getProjectExperiences();
        List<Project> projects = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, profile.getUserId())
                .eq(Project::getIsDeleted, NOT_DELETED));
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < items.size(); i++) {
            ResumeAnalysisResult.ProjectExperienceItem item = items.get(i);
            ProjectMatch match = matchProject(item, projects);
            ResumeProjectExperience experience = new ResumeProjectExperience();
            experience.setResumeId(profile.getId());
            experience.setUserId(profile.getUserId());
            experience.setProjectId(match.projectId());
            experience.setProjectName(defaultText(item.getProjectName(), "未命名项目经历"));
            experience.setDescription(safeText(item.getDescription()));
            experience.setTechStack(toJson(item.getTechStack()));
            experience.setRole(safeText(item.getRole()));
            experience.setHighlights(toJson(item.getHighlights()));
            experience.setRiskPoints(toJson(item.getRiskPoints()));
            experience.setRecommendedQuestions(toJson(item.getRecommendedQuestions()));
            experience.setMatchReason(match.reason());
            experience.setSortOrder(i + 1);
            experience.setCreatedAt(now);
            experience.setUpdatedAt(now);
            resumeProjectExperienceMapper.insert(experience);
        }
    }

    private ProjectMatch matchProject(ResumeAnalysisResult.ProjectExperienceItem item, List<Project> projects) {
        if (item == null || projects == null || projects.isEmpty()) {
            return new ProjectMatch(null, null);
        }
        String resumeName = normalizeForMatch(firstText(item.getProjectName(), item.getPossibleProjectName()));
        if (!StringUtils.hasText(resumeName)) {
            return new ProjectMatch(null, null);
        }
        for (Project project : projects) {
            String projectName = normalizeForMatch(project.getName());
            if (StringUtils.hasText(projectName) && (projectName.contains(resumeName) || resumeName.contains(projectName))) {
                return new ProjectMatch(project.getId(), "根据项目名称自动匹配");
            }
        }
        Set<String> resumeTokens = tokens(resumeName);
        for (Project project : projects) {
            Set<String> projectTokens = tokens(normalizeForMatch(project.getName() + " " + project.getTechStack()));
            Set<String> intersection = new HashSet<>(resumeTokens);
            intersection.retainAll(projectTokens);
            if (intersection.size() >= 2) {
                return new ProjectMatch(project.getId(), "根据项目关键词自动匹配");
            }
        }
        return new ProjectMatch(null, null);
    }

    private String loadSanitizedResumeText(UserDocument document) {
        byte[] content = aliyunOssService.download(document.getOssKey());
        ParsedDocument parsed = documentParseService.parse(content, document.getFileType());
        String text = parsed == null ? "" : parsed.getText();
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "简历文档没有可分析内容");
        }
        String sanitized = PHONE_PATTERN.matcher(EMAIL_PATTERN.matcher(text).replaceAll("[邮箱已脱敏]")).replaceAll("[手机号已脱敏]");
        sanitized = sanitized.replaceAll("\\s{3,}", "\n\n").trim();
        return sanitized.length() <= MAX_RESUME_CHARS ? sanitized : sanitized.substring(0, MAX_RESUME_CHARS);
    }

    private void markAnalyzing(ResumeProfile profile) {
        profile.setAnalysisStatus(STATUS_ANALYZING);
        profile.setErrorMessage(null);
        profile.setUpdatedAt(LocalDateTime.now());
        resumeProfileMapper.updateById(profile);
    }

    private void markFailed(ResumeProfile profile, String errorMessage) {
        profile.setAnalysisStatus(STATUS_FAILED);
        profile.setErrorMessage(abbreviate(errorMessage));
        profile.setUpdatedAt(LocalDateTime.now());
        resumeProfileMapper.updateById(profile);
    }

    private ResumeProfile getCurrentUserResume(Long id) {
        Long userId = UserContext.getCurrentUserId();
        ResumeProfile profile = id == null ? null : resumeProfileMapper.selectById(id);
        if (profile == null || !Integer.valueOf(NOT_DELETED).equals(profile.getIsDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "简历档案不存在");
        }
        if (!userId.equals(profile.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return profile;
    }

    private UserDocument getCurrentUserDocument(Long id, Long userId) {
        UserDocument document = id == null ? null : userDocumentMapper.selectById(id);
        if (document == null || !Integer.valueOf(NOT_DELETED).equals(document.getIsDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文档不存在或已删除");
        }
        if (!userId.equals(document.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return document;
    }

    private Project getCurrentUserProject(Long id, Long userId) {
        Project project = id == null ? null : projectMapper.selectById(id);
        if (project == null || !Integer.valueOf(NOT_DELETED).equals(project.getIsDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "项目不存在");
        }
        if (!userId.equals(project.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return project;
    }

    private ResumeProjectExperience getCurrentUserResumeProjectExperience(ResumeProfile profile, Long resumeProjectId) {
        ResumeProjectExperience experience = resumeProjectExperienceMapper.selectById(resumeProjectId);
        if (experience == null
                || !profile.getId().equals(experience.getResumeId())
                || !profile.getUserId().equals(experience.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "简历项目经历不存在");
        }
        return experience;
    }

    private void validateDocumentForResume(UserDocument document) {
        if (!SUPPORTED_FILE_TYPES.contains(document.getFileType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持 TXT、Markdown、PDF 简历文档");
        }
        if (!"PARSED".equals(document.getParseStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档还未解析完成，请稍后再分析");
        }
    }

    private List<ResumeProjectExperience> listProjectExperiences(Long resumeId) {
        return resumeProjectExperienceMapper.selectList(new LambdaQueryWrapper<ResumeProjectExperience>()
                .eq(ResumeProjectExperience::getResumeId, resumeId)
                .orderByAsc(ResumeProjectExperience::getSortOrder)
                .orderByAsc(ResumeProjectExperience::getId));
    }

    private ResumeListItemVO toListItemVO(ResumeProfile profile) {
        ResumeListItemVO vo = new ResumeListItemVO();
        vo.setId(profile.getId());
        vo.setDocumentId(profile.getDocumentId());
        vo.setDocumentTitle(getDocumentTitle(profile.getDocumentId(), profile.getUserId()));
        vo.setTitle(profile.getTitle());
        vo.setTargetRole(profile.getTargetRole());
        vo.setAnalysisStatus(profile.getAnalysisStatus());
        vo.setSummary(profile.getSummary());
        vo.setErrorMessage(profile.getErrorMessage());
        vo.setAnalyzedAt(profile.getAnalyzedAt());
        vo.setCreatedAt(profile.getCreatedAt());
        vo.setUpdatedAt(profile.getUpdatedAt());
        return vo;
    }

    private ResumeProfileVO toProfileVO(ResumeProfile profile, List<ResumeProjectExperience> experiences) {
        ResumeProfileVO vo = new ResumeProfileVO();
        vo.setId(profile.getId());
        vo.setDocumentId(profile.getDocumentId());
        vo.setDocumentTitle(getDocumentTitle(profile.getDocumentId(), profile.getUserId()));
        vo.setTitle(profile.getTitle());
        vo.setTargetRole(profile.getTargetRole());
        vo.setAnalysisStatus(profile.getAnalysisStatus());
        vo.setSummary(profile.getSummary());
        vo.setAnalysisResult(parseAnalysisResult(profile.getAnalysisResult()));
        vo.setErrorMessage(profile.getErrorMessage());
        vo.setAnalyzedAt(profile.getAnalyzedAt());
        vo.setCreatedAt(profile.getCreatedAt());
        vo.setUpdatedAt(profile.getUpdatedAt());
        vo.setProjectExperiences(experiences.stream().map(this::toProjectExperienceVO).toList());
        return vo;
    }

    private ResumeProjectExperienceVO toProjectExperienceVO(ResumeProjectExperience experience) {
        ResumeProjectExperienceVO vo = new ResumeProjectExperienceVO();
        vo.setId(experience.getId());
        vo.setProjectId(experience.getProjectId());
        vo.setProjectName(experience.getProjectName());
        vo.setDescription(experience.getDescription());
        vo.setTechStack(parseStringList(experience.getTechStack()));
        vo.setRole(experience.getRole());
        vo.setHighlights(parseStringList(experience.getHighlights()));
        vo.setRiskPoints(parseStringList(experience.getRiskPoints()));
        vo.setRecommendedQuestions(parseStringList(experience.getRecommendedQuestions()));
        vo.setMatchReason(experience.getMatchReason());
        vo.setCreatedAt(experience.getCreatedAt());
        vo.setUpdatedAt(experience.getUpdatedAt());
        return vo;
    }

    private String getDocumentTitle(Long documentId, Long userId) {
        UserDocument document = documentId == null ? null : userDocumentMapper.selectById(documentId);
        if (document == null || !userId.equals(document.getUserId()) || Integer.valueOf(DELETED).equals(document.getIsDeleted())) {
            return "原文档已删除";
        }
        return document.getTitle();
    }

    private ResumeAnalysisResult parseAnalysisResult(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ResumeAnalysisResult.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return values == null ? List.of() : values;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Resume JSON serialization failed", exception);
        }
    }

    private String normalizeTitle(String title, String fallback) {
        String value = StringUtils.hasText(title) ? title.trim() : fallback;
        if (!StringUtils.hasText(value)) {
            return "未命名简历";
        }
        return value.length() <= 128 ? value : value.substring(0, 128);
    }

    private String normalizeTargetRole(String targetRole) {
        String value = StringUtils.hasText(targetRole) ? targetRole.trim() : "Java 后端实习";
        return value.length() <= 128 ? value : value.substring(0, 128);
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String limit(String value, int maxLength) {
        String text = safeText(value);
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String joinLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("\n", values.stream().filter(StringUtils::hasText).map(String::trim).toList());
    }

    private String buildDifficulties(List<String> risks, List<String> questions) {
        List<String> lines = new ArrayList<>();
        if (risks != null) {
            risks.stream()
                    .filter(StringUtils::hasText)
                    .map(item -> "风险点：" + item.trim())
                    .forEach(lines::add);
        }
        if (questions != null) {
            questions.stream()
                    .filter(StringUtils::hasText)
                    .map(item -> "可追问：" + item.trim())
                    .forEach(lines::add);
        }
        return lines.isEmpty() ? "待补充：请列出你真实遇到并能复盘的项目难点。" : String.join("\n", lines);
    }

    private String firstText(String left, String right) {
        return StringUtils.hasText(left) ? left : right;
    }

    private String normalizeForMatch(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> tokens(String value) {
        Set<String> tokens = new HashSet<>();
        if (!StringUtils.hasText(value)) {
            return tokens;
        }
        for (String token : value.split("\\s+")) {
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String abbreviate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 512);
    }

    private record ProjectMatch(Long projectId, String reason) {
    }
}
