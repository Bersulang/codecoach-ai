package com.codecoach.module.user.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.user.service.UserService;
import com.codecoach.module.user.vo.AvatarUploadResponse;
import com.codecoach.module.user.vo.CurrentUserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<CurrentUserVO> me() {
        return Result.success(userService.getCurrentUser());
    }

    @PostMapping("/avatar")
    public Result<AvatarUploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.uploadAvatar(file));
    }
}
