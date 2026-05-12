package com.codecoach.module.rag.service.impl;

import com.codecoach.module.project.entity.Project;
import com.codecoach.module.rag.constant.RagConstants;
import com.codecoach.module.rag.model.RagChunkCandidate;
import com.codecoach.module.rag.service.ProjectChunkService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProjectChunkServiceImpl implements ProjectChunkService {

    private static final int MIN_CHUNK_CHARS = 40;

    @Override
    public List<RagChunkCandidate> chunkProject(Project project) {
        if (project == null || project.getId() == null) {
            return List.of();
        }

        List<RagChunkCandidate> chunks = new ArrayList<>();
        addChunk(chunks, project, "项目基本信息", joinLines(
                line("项目名称", project.getName()),
                line("项目描述", project.getDescription())
        ));
        addChunk(chunks, project, "技术栈", project.getTechStack());
        addChunk(chunks, project, "负责模块", project.getRole());
        addChunk(chunks, project, "项目亮点", project.getHighlights());
        addChunk(chunks, project, "项目难点", project.getDifficulties());
        return chunks;
    }

    private void addChunk(List<RagChunkCandidate> chunks, Project project, String section, String body) {
        if (!StringUtils.hasText(body)) {
            return;
        }

        String content = "# " + safeText(project.getName()) + "\n\n"
                + "## " + section + "\n\n"
                + body.trim();
        if (content.trim().length() < MIN_CHUNK_CHARS) {
            return;
        }

        RagChunkCandidate candidate = new RagChunkCandidate();
        candidate.setChunkIndex(chunks.size());
        candidate.setContent(content);
        candidate.setTokenCount(estimateTokenCount(content));
        candidate.setMetadata(buildMetadata(project, section, chunks.size()));
        chunks.add(candidate);
    }

    private Map<String, Object> buildMetadata(Project project, String section, int chunkIndex) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceType", RagConstants.SOURCE_TYPE_PROJECT);
        metadata.put("projectId", project.getId());
        metadata.put("projectName", safeText(project.getName()));
        metadata.put("title", safeText(project.getName()));
        metadata.put("section", section);
        metadata.put("ownerType", RagConstants.OWNER_TYPE_USER);
        metadata.put("userId", project.getUserId());
        metadata.put("chunkIndex", chunkIndex);
        return metadata;
    }

    private String line(String label, String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return label + "：" + value.trim();
    }

    private String joinLines(String... lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (StringUtils.hasText(line)) {
                result.add(line);
            }
        }
        return String.join("\n", result);
    }

    private Integer estimateTokenCount(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return Math.max(1, content.length() / 2);
    }

    private String safeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : "";
    }
}
