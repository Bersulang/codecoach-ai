package com.codecoach.module.auth.service;

import com.codecoach.module.auth.dto.LoginRequest;
import com.codecoach.module.auth.dto.RegisterRequest;
import com.codecoach.module.auth.vo.LoginResponse;
import com.codecoach.module.auth.vo.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
