package com.codecoach.module.user.vo;

public class AvatarUploadResponse {

    private String avatarUrl;

    public AvatarUploadResponse() {
    }

    public AvatarUploadResponse(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
