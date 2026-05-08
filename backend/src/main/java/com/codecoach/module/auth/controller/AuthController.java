package com.codecoach.module.auth.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.auth.dto.LoginRequest;
import com.codecoach.module.auth.dto.RegisterRequest;
import com.codecoach.module.auth.service.AuthService;
import com.codecoach.module.auth.vo.LoginResponse;
import com.codecoach.module.auth.vo.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
