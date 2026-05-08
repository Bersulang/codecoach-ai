package com.codecoach.module.user.vo;

public class CurrentUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatarUrl;

    private String role;

    public CurrentUserVO(Long id, String username, String nickname, String avatarUrl, String role) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.role = role;
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
}
