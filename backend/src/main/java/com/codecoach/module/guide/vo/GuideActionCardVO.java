package com.codecoach.module.guide.vo;

import com.codecoach.module.agent.tool.dto.ToolActionVO;
import com.codecoach.module.agent.tool.dto.ToolDefinition;

public class GuideActionCardVO extends ToolActionVO {

    public GuideActionCardVO() {
    }

    public GuideActionCardVO(String actionType, String title, String description, String targetPath) {
        setActionType(actionType);
        setToolName(actionType);
        setTitle(title);
        setDescription(description);
        setTargetPath(targetPath);
    }

    public static GuideActionCardVO fromDefinition(ToolDefinition definition) {
        ToolActionVO action = ToolActionVO.fromDefinition(definition);
        GuideActionCardVO vo = new GuideActionCardVO();
        vo.setActionType(action.getActionType());
        vo.setToolName(action.getToolName());
        vo.setToolType(action.getToolType());
        vo.setRiskLevel(action.getRiskLevel());
        vo.setExecutionMode(action.getExecutionMode());
        vo.setDisplayType(action.getDisplayType());
        vo.setTitle(action.getTitle());
        vo.setDescription(action.getDescription());
        vo.setTargetPath(action.getTargetPath());
        vo.setRequiresConfirmation(action.isRequiresConfirmation());
        vo.setParams(action.getParams());
        return vo;
    }
}
