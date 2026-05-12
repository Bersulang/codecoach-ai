package com.codecoach.module.auth.vo;

public class LoginUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String role;

    private String avatarUrl;

    public LoginUserVO(Long id, String username, String nickname, String role) {
        this(id, username, nickname, role, null);
    }

    public LoginUserVO(Long id, String username, String nickname, String role, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
        this.avatarUrl = avatarUrl;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
