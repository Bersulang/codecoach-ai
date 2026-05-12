package com.codecoach.module.resume.service;

import com.codecoach.module.resume.dto.ResumeCreateRequest;
import com.codecoach.module.resume.dto.ResumeProjectLinkRequest;
import com.codecoach.module.resume.vo.ResumeListItemVO;
import com.codecoach.module.resume.vo.ResumeProfileVO;
import java.util.List;

public interface ResumeService {

    ResumeProfileVO create(ResumeCreateRequest request);

    List<ResumeListItemVO> list();

    ResumeProfileVO detail(Long id);

    ResumeProfileVO analyze(Long id);

    void delete(Long id);

    ResumeProfileVO linkProject(Long resumeId, Long resumeProjectId, ResumeProjectLinkRequest request);
}
