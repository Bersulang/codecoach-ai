package com.codecoach.module.document.service;

import com.codecoach.module.document.model.UserDocumentChunkCommand;
import com.codecoach.module.rag.model.RagChunkCandidate;
import java.util.List;

public interface UserDocumentChunkService {

    List<RagChunkCandidate> chunk(UserDocumentChunkCommand command);
}
