package com.codecoach.module.user.service.impl;

import com.codecoach.common.exception.BusinessException;
import com.codecoach.common.result.ResultCode;
import com.codecoach.common.storage.AliyunOssService;
import com.codecoach.module.user.entity.User;
import com.codecoach.module.user.mapper.UserMapper;
import com.codecoach.module.user.service.UserService;
import com.codecoach.module.user.vo.AvatarUploadResponse;
import com.codecoach.module.user.vo.CurrentUserVO;
import com.codecoach.security.UserContext;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {

    private static final long MAX_AVATAR_SIZE = 2L * 1024L * 1024L;

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final UserMapper userMapper;

    private final AliyunOssService aliyunOssService;

    public UserServiceImpl(UserMapper userMapper, AliyunOssService aliyunOssService) {
        this.userMapper = userMapper;
        this.aliyunOssService = aliyunOssService;
    }

    @Override
    public CurrentUserVO getCurrentUser() {
        User user = getCurrentUserEntity();

        return new CurrentUserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getCreatedAt(),
                "已登录"
        );
    }

    @Override
    public AvatarUploadResponse uploadAvatar(MultipartFile file) {
        User user = getCurrentUserEntity();
        validateAvatar(file);
        try {
            String avatarUrl = aliyunOssService.upload(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    "avatars/" + user.getId()
            );
            User update = new User();
            update.setId(user.getId());
            update.setAvatarUrl(avatarUrl);
            userMapper.updateById(update);
            return new AvatarUploadResponse(avatarUrl);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "头像上传失败，请稍后重试");
        }
    }

    @Override
    public Boolean deleteCurrentUser() {
        User user = getCurrentUserEntity();
        User update = new User();
        update.setId(user.getId());
        update.setStatus(0);
        update.setIsDeleted(1);
        update.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(update);
        return true;
    }

    private User getCurrentUserEntity() {
        Long currentUserId = UserContext.getCurrentUserId();
        User user = userMapper.selectById(currentUserId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录状态已失效");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "用户已被禁用");
        }
        return user;
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "头像图片不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_AVATAR_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持 JPG、PNG、WEBP 图片");
        }
    }
}
