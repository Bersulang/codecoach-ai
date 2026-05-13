package com.codecoach.module.memory.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class UserMemorySummaryVO {

    private String targetRole;

    private List<UserMemoryItemVO> goals = new ArrayList<>();

    private List<UserMemoryItemVO> topWeaknesses = new ArrayList<>();

    private List<UserMemoryItemVO> topResumeRisks = new ArrayList<>();

    private List<UserMemoryItemVO> topProjectRisks = new ArrayList<>();

    private List<UserMemoryItemVO> recentNextActions = new ArrayList<>();

    private List<UserMemoryItemVO> masteredTopics = new ArrayList<>();

    private boolean empty;
}
