package com.codecoach.module.guide.service;

import com.codecoach.module.guide.dto.GuideChatRequest;
import com.codecoach.module.guide.vo.GuideChatResponseVO;

public interface GuideChatService {

    GuideChatResponseVO chat(GuideChatRequest request);
}
