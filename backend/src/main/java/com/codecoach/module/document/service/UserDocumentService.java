package com.codecoach.module.document.service;

import com.codecoach.module.document.vo.UserDocumentVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UserDocumentService {

    UserDocumentVO upload(MultipartFile file, Long projectId, String title);

    List<UserDocumentVO> list(Long projectId, String fileType);

    UserDocumentVO getDetail(Long id);

    void delete(Long id);

    UserDocumentVO reindex(Long id);
}
