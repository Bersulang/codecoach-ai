package com.codecoach.module.rag.service;

import com.codecoach.module.project.entity.Project;
import com.codecoach.module.rag.model.RagChunkCandidate;
import java.util.List;

public interface ProjectChunkService {

    List<RagChunkCandidate> chunkProject(Project project);
}
