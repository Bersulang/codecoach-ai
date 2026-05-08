package com.codecoach.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.auth.dto.RegisterRequest;
import com.codecoach.module.auth.service.AuthService;
import com.codecoach.module.auth.vo.RegisterResponse;
import com.codecoach.module.user.entity.User;
import com.codecoach.module.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int USERNAME_EXISTS_CODE = 1001;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "两次密码不一致");
        }

        Long sameUsernameCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getIsDeleted, 0));
        if (sameUsernameCount > 0) {
            throw new BusinessException(USERNAME_EXISTS_CODE, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setRole("USER");
        user.setStatus(1);
        user.setIsDeleted(0);

        userMapper.insert(user);
        return new RegisterResponse(user.getId());
    }
}
