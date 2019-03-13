package com.sg0.baddytally;

public class MatchInfo {
    public String T1;
    public String T2;
    public String desc;
    public Boolean done;
    public String key;

    public MatchInfo() {}

    public MatchInfo(String key, String t1, String t2) {
        this.key = key;
        T1 = t1;
        T2 = t2;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    @Override
    public String toString() {
        return "MatchInfo{" +
                "T1='" + T1 + '\'' +
                ", T2='" + T2 + '\'' +
                ", desc='" + desc + '\'' +
                ", done=" + done +
                ", key=" + key +
                '}';
    }
}