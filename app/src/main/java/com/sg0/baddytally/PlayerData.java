package com.sg0.baddytally;

import java.io.Serializable;

class PlayerData implements Serializable {
    private String name;
    private String innings_score;
    private String overall_score;
    private String group;

    public PlayerData(String group) {
        this.name = "Player";
        this.innings_score = "This\nRound";
        this.overall_score = "Overall";
        this.group = group;
    }

    public PlayerData(String name, String innings_score, String overall_score, String group) {
        this.name = name;
        this.innings_score = innings_score;
        this.overall_score = overall_score;
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInnings_score() {
        return innings_score;
    }

    public void setInnings_score(String innings_score) {
        this.innings_score = innings_score;
    }

    public String getOverall_score() {
        return overall_score;
    }

    public void setOverall_score(String overall_score) {
        this.overall_score = overall_score;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "name='" + name + '\'' +
                ", innings_score='" + innings_score + '\'' +
                ", overall_score='" + overall_score + '\'' +
                ", group='" + group + '\'' +
                '}';
    }

    public String toPrintString() {
        return group + '/' + name + '/' + innings_score + '/' +  overall_score;
    }
}
