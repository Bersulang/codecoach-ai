package com.codecoach.module.health;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Result<String> health() {
        return Result.success("ok");
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
