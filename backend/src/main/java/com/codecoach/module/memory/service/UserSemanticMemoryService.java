package com.codecoach.module.memory.service;

import com.codecoach.module.memory.model.MemorySemanticHit;
import java.util.List;

public interface UserSemanticMemoryService {

    int indexActiveMemories(Long userId);

    List<MemorySemanticHit> search(Long userId, String query, int topK);
}
