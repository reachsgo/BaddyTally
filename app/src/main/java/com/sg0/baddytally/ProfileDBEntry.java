package com.sg0.baddytally;

public class ProfileDBEntry {
    final static int MAX_STR_LEN = 24;
    private String des;
    private String rc;
    private String ac;
    private String mc;
    private int ver;
    private Boolean wake;
    private String news;

    ProfileDBEntry() {
        clear();
    }

    void clear() {
        this.ac = "";
        this.des = "";
        this.mc = "";
        this.rc = "";
        this.wake = false;
        this.ver = 0;
        this.news = "";
    }

    public String getAc() {
        return ac;
    }

    public void setAc(String ac) {
        this.ac = getValidStr(ac);
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = getValidStr(des);
    }

    public String getMc() {
        return mc;
    }

    public void setMc(String mc) {
        this.mc = getValidStr(mc);
    }

    public String getRc() {
        return rc;
    }

    public void setRc(String rc) {
        this.rc = getValidStr(rc);
    }

    public Boolean getWake() {
        return wake;
    }

    public void setWake(Boolean wake) {
        this.wake = wake;
    }

    public int getVer() {
        return ver;
    }

    public void setVer(int ver) {
        this.ver = ver;
    }

    public String getNews() {
        return news;
    }

    public void setNews(String news) {
        this.news = news;
    }

    public void copyProfile(final ProfileDBEntry other) {
        other.ac = this.ac;
        other.des = this.des;
        other.mc = this.mc;
        other.rc = this.rc;
        other.wake = this.wake;
        other.ver = this.ver;
        other.news = this.news;
    }

    String getValidStr(final String str) {
        if(str==null || str.isEmpty()) return "";
        if(str.length()> MAX_STR_LEN)
            return str.substring(0, MAX_STR_LEN);
            //The substring begins at the specified beginIndex and extends to the character at
            //index endIndex - 1. Thus the length of the substring is endIndex-beginIndex.
        else
            return str;
    }

    boolean isValid() {
        return !rc.isEmpty();
    }

    @Override
    public String toString() {
        return "ProfileDBEntry{" +
                "ac=" + ac.length() +
                ", des=" + des +
                ", mc=" + mc.length() +
                ", rc=" + rc.length()+
                ", wake=" + wake +
                ", ver=" + ver +
                '}';
    }

    public String toSecretString() {
        return "ProfileDBEntry{" +
                "ac='" + ac + '\'' +
                ", des='" + des + '\'' +
                ", mc='" + mc + '\'' +
                ", rc='" + rc + '\'' +
                ", wake=" + wake +
                ", ver='" + ver + '\'' +
                '}';
    }
}
