package com.codecoach.module.document.service;

import com.codecoach.module.document.model.ParsedDocument;

public interface DocumentParseService {

    ParsedDocument parse(byte[] content, String fileType);
}
