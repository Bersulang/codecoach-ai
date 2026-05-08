package com.codecoach.module.user.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.module.user.entity.User;
import com.codecoach.module.user.mapper.UserMapper;
import com.codecoach.module.user.service.UserService;
import com.codecoach.module.user.vo.CurrentUserVO;
import com.codecoach.security.UserContext;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public CurrentUserVO getCurrentUser() {
        Long currentUserId = UserContext.getCurrentUserId();
        User user = userMapper.selectById(currentUserId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录状态已失效");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "用户已被禁用");
        }

        return new CurrentUserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole()
        );
    }
}
