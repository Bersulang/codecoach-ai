package com.codecoach.module.document.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.document.model.ParsedDocument;
import com.codecoach.module.document.service.DocumentParseService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DocumentParseServiceImpl implements DocumentParseService {

    @Override
    public ParsedDocument parse(byte[] content, String fileType) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        String text;
        Integer pageCount = null;
        try {
            if ("PDF".equals(fileType)) {
                try (PDDocument document = Loader.loadPDF(content)) {
                    pageCount = document.getNumberOfPages();
                    text = new PDFTextStripper().getText(document);
                }
            } else {
                text = new String(content, StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new BusinessException(5302, "文档解析失败");
        }
        text = normalizeText(text);
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(5302, "文档解析后内容为空");
        }
        return new ParsedDocument(text, pageCount);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\p{Cntrl}&&[^\n\t]]", "")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
