package com.sg0.baddytally;

public class ClubDBEntry {
    final static int MAX_STR_LEN = 24;

    private String n; //name
    private String des;
    private String email;
    private String ph;  //phone
    private String ownr;
    private int plN;  //max no:of players
    private int ac;  //activation code
    private boolean active;
    private String cmt;  //Any comment to be displayed to the user during club activation

    ClubDBEntry() {
        clear();
    }

    void clear() {
        n = "";
        des = "";
        email = "";
        ph = "";
        ownr = "";
        plN = 0;
        ac = -1;
        active = false;
        cmt = null;
    }

    public ClubDBEntry(String name, String desc, String email, String ph, String owner,
                       int numOfPlayers) {
        this.n = name;
        this.des = desc;
        this.email = email;
        this.ph = ph;
        this.ownr = owner;
        this.plN = numOfPlayers;
        ac = -1;
        active = false;
    }

    public String getN() {
        return n;
    }

    public void setN(String n) {
        this.n = n;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPh() {
        return ph;
    }

    public void setPh(String ph) {
        this.ph = ph;
    }

    public String getOwnr() {
        return ownr;
    }

    public void setOwnr(String ownr) {
        this.ownr = ownr;
    }

    public int getPlN() {
        return plN;
    }

    public void setPlN(int plN) {
        this.plN = plN;
    }

    public int getAc() {
        return ac;
    }

    public void setAc(int ac) {
        this.ac = ac;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCmt() {
        if(cmt==null) return "";
        return cmt;
    }

    public void setCmt(String cmt) {
        this.cmt = cmt;
    }

    public void copyData(final ClubDBEntry other) {
        other.n = this.n;
        other.des = this.des;
        other.email = this.email;
        other.ph = this.ph;
        other.ownr = this.ownr;
        other.plN = this.plN;
        other.ac = this.ac;
        other.active = this.active;
        other.cmt = this.cmt;
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
        return !n.isEmpty();
    }

    @Override
    public String toString() {
        return "ClubDBEntry{" +
                "n='" + n + '\'' +
                ", des='" + des + '\'' +
                ", email='" + email + '\'' +
                ", ph='" + ph + '\'' +
                ", ownr='" + ownr + '\'' +
                ", plN=" + plN +
                ", ac=" + ac +
                ", active=" + active +
                ", cmt=" + getCmt() +
                '}';
    }
}
