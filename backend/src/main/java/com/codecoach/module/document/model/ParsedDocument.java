package com.codecoach.module.document.model;

public class ParsedDocument {

    private String text;

    private Integer pageCount;

    public ParsedDocument() {
    }

    public ParsedDocument(String text, Integer pageCount) {
        this.text = text;
        this.pageCount = pageCount;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
}
