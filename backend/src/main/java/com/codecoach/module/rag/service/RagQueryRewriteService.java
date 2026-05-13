package com.codecoach.module.rag.service;

import java.util.List;

public interface RagQueryRewriteService {

    String rewrite(String query, List<String> sourceTypes);
}
