package com.codecoach.module.guide.model;

import java.util.ArrayList;
import java.util.List;

public class GuideAiSuggestion {

    private String answer;

    private List<String> actions = new ArrayList<>();

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }
}
