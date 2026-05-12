package com.codecoach.module.guide.vo;

import java.util.ArrayList;
import java.util.List;

public class GuideChatResponseVO {

    private String answer;

    private Boolean personalized;

    private List<GuideActionCardVO> actions = new ArrayList<>();

    public GuideChatResponseVO() {
    }

    public GuideChatResponseVO(String answer, Boolean personalized, List<GuideActionCardVO> actions) {
        this.answer = answer;
        this.personalized = personalized;
        this.actions = actions;
    }

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
        this.actions = actions;
    }
}
