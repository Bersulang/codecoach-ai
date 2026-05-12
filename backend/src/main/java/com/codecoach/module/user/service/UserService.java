package com.codecoach.module.user.service;

import com.codecoach.module.user.vo.CurrentUserVO;
import com.codecoach.module.user.vo.AvatarUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    CurrentUserVO getCurrentUser();

    AvatarUploadResponse uploadAvatar(MultipartFile file);
}
