package com.codecoach.module.guide.controller;

import com.codecoach.common.result.Result;
import com.codecoach.module.guide.dto.GuideChatRequest;
import com.codecoach.module.guide.service.GuideChatService;
import com.codecoach.module.guide.vo.GuideChatResponseVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guide")
public class GuideController {

    private final GuideChatService guideChatService;

    public GuideController(GuideChatService guideChatService) {
        this.guideChatService = guideChatService;
    }

    @PostMapping("/chat")
    public Result<GuideChatResponseVO> chat(@RequestBody GuideChatRequest request) {
        return Result.success(guideChatService.chat(request));
    }
}
