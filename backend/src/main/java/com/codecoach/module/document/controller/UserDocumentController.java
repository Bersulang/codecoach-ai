package com.codecoach.module.document.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.document.service.UserDocumentService;
import com.codecoach.module.document.vo.UserDocumentVO;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user-documents")
public class UserDocumentController {

    private final UserDocumentService userDocumentService;

    public UserDocumentController(UserDocumentService userDocumentService) {
        this.userDocumentService = userDocumentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserDocumentVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "title", required = false) String title
    ) {
        return Result.success(userDocumentService.upload(file, projectId, title));
    }

    @GetMapping
    public Result<List<UserDocumentVO>> list(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "fileType", required = false) String fileType
    ) {
        return Result.success(userDocumentService.list(projectId, fileType));
    }

    @GetMapping("/{id}")
    public Result<UserDocumentVO> detail(@PathVariable Long id) {
        return Result.success(userDocumentService.getDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userDocumentService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/reindex")
    public Result<UserDocumentVO> reindex(@PathVariable Long id) {
        return Result.success(userDocumentService.reindex(id));
    }
}
