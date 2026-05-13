package com.codecoach.module.agent.runtime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codecoach.module.agent.runtime.entity.AgentStep;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentStepMapper extends BaseMapper<AgentStep> {
}
