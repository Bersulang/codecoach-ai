package com.codecoach.module.agent.runtime.dto;

import com.codecoach.module.guide.vo.GuideActionCardVO;
import java.util.ArrayList;
import java.util.List;

public class AgentResponse {

    private String answer;
    private Boolean personalized;
    private List<GuideActionCardVO> actions = new ArrayList<>();
    private String runId;
    private String traceId;
    private String status;
    private List<String> observations = new ArrayList<>();

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Boolean getPersonalized() {
        return personalized;
    }

    public void setPersonalized(Boolean personalized) {
        this.personalized = personalized;
    }

    public List<GuideActionCardVO> getActions() {
        return actions;
    }

    public void setActions(List<GuideActionCardVO> actions) {
        this.actions = actions == null ? new ArrayList<>() : actions;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getObservations() {
        return observations;
    }

    public void setObservations(List<String> observations) {
        this.observations = observations == null ? new ArrayList<>() : observations;
    }
}
