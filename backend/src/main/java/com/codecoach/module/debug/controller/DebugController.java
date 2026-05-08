package com.codecoach.module.debug.controller;

import com.codecoach.common.result.Result;
import com.codecoach.security.UserContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/current-user")
    public Result<Map<String, Object>> currentUser() {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("userId", UserContext.getCurrentUserId());
        user.put("username", UserContext.getCurrentUsername());
        user.put("role", UserContext.getCurrentUserRole());
        return Result.success(user);
    }
}
