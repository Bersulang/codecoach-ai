package com.codecoach.module.user.vo;

import java.time.LocalDateTime;

public class CurrentUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatarUrl;

    private String role;

    private LocalDateTime createdAt;

    private String loginStatus;

    public CurrentUserVO(Long id, String username, String nickname, String avatarUrl, String role) {
        this(id, username, nickname, avatarUrl, role, null, "已登录");
    }

    public CurrentUserVO(
            Long id,
            String username,
            String nickname,
            String avatarUrl,
            String role,
            LocalDateTime createdAt,
            String loginStatus
    ) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.createdAt = createdAt;
        this.loginStatus = loginStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLoginStatus() {
        return loginStatus;
    }

    public void setLoginStatus(String loginStatus) {
        this.loginStatus = loginStatus;
    }
}
