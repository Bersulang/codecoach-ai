package com.codecoach.module.guide.vo;

public class GuideActionCardVO {

    private String actionType;

    private String title;

    private String description;

    private String targetPath;

    public GuideActionCardVO() {
    }

    public GuideActionCardVO(String actionType, String title, String description, String targetPath) {
        this.actionType = actionType;
        this.title = title;
        this.description = description;
        this.targetPath = targetPath;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }
}
