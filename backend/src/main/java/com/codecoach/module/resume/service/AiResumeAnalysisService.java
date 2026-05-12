package com.codecoach.module.resume.service;

import com.codecoach.module.resume.model.ResumeAnalysisResult;

public interface AiResumeAnalysisService {

    ResumeAnalysisResult analyze(String resumeText, String targetRole);
}
