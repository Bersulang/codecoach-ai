package com.codecoach.module.health;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.Result;
import com.codecoach.module.user.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final UserMapper userMapper;

    public HealthController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/api/health")
    public Result<String> health() {
        return Result.success("ok");
    }

    @GetMapping("/api/health/db")
    public Result<Long> dbHealth() {
        return Result.success(userMapper.selectCount(null));
    }

    @GetMapping("/api/health/business-error")
    public Result<Void> businessError() {
        throw new BusinessException(400, "测试业务异常");
    }

    @GetMapping("/api/health/system-error")
    public Result<Void> systemError() {
        throw new RuntimeException("测试系统异常");
    }
}
