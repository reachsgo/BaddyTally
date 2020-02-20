package com.sg0.baddytally;

public class ProfileDBEntry {
    final static int MAX_STR_LEN = 24;
    private String admincode;
    private String description;
    private String memcode;
    private String rootcode;
    private Boolean wake;
    private int minver;
    private String news;

    ProfileDBEntry() {
        clear();
    }

    void clear() {
        this.admincode = "";
        this.description = "";
        this.memcode = "";
        this.rootcode = "";
        this.wake = false;
        this.minver = 0;
        this.news = "";
    }

    public String getAdmincode() {
        return admincode;
    }

    public void setAdmincode(String admincode) {
        this.admincode = getValidStr(admincode);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = getValidStr(description);
    }

    public String getMemcode() {
        return memcode;
    }

    public void setMemcode(String memcode) {
        this.memcode = getValidStr(memcode);
    }

    public String getRootcode() {
        return rootcode;
    }

    public void setRootcode(String rootcode) {
        this.rootcode = getValidStr(rootcode);
    }

    public Boolean getWake() {
        return wake;
    }

    public void setWake(Boolean wake) {
        this.wake = wake;
    }

    public int getMinver() {
        return minver;
    }

    public void setMinver(int minver) {
        this.minver = minver;
    }

    public String getNews() {
        return news;
    }

    public void setNews(String news) {
        this.news = news;
    }

    public void copyProfile(final ProfileDBEntry other) {
        other.admincode = this.admincode;
        other.description = this.description;
        other.memcode = this.memcode;
        other.rootcode = this.rootcode;
        other.wake = this.wake;
        other.minver = this.minver;
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
        if(rootcode.isEmpty()) return false;
        return true;
    }

    @Override
    public String toString() {
        return "ProfileDBEntry{" +
                "admincode=" + admincode.length() +
                ", description=" + description +
                ", memcode=" + memcode.length() +
                ", rootcode=" + rootcode.length()+
                ", wake=" + wake +
                ", minver=" + minver +
                '}';
    }

    public String toSecretString() {
        return "ProfileDBEntry{" +
                "admincode='" + admincode + '\'' +
                ", description='" + description + '\'' +
                ", memcode='" + memcode + '\'' +
                ", rootcode='" + rootcode + '\'' +
                ", wake=" + wake +
                ", minver='" + minver + '\'' +
                '}';
    }
}
