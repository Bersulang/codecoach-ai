package com.codecoach.module.auth.vo;

public class LoginResponse {

    private String token;

    private LoginUserVO user;

    public LoginResponse(String token, LoginUserVO user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LoginUserVO getUser() {
        return user;
    }

    public void setUser(LoginUserVO user) {
        this.user = user;
    }
}
