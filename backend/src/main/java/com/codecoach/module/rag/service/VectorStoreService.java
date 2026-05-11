package com.codecoach.module.rag.service;

import com.codecoach.module.rag.model.VectorSearchRequest;
import com.codecoach.module.rag.model.VectorSearchResult;
import com.codecoach.module.rag.model.VectorUpsertRequest;
import java.util.List;

public interface VectorStoreService {

    void ensureCollection();

    void upsert(VectorUpsertRequest request);

    List<VectorSearchResult> search(VectorSearchRequest request);

    void delete(String vectorId);
}
