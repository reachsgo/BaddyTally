package com.sg0.baddytally;


import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ActiveUserDBEntry {
    private String r;  //role
    private String d;  //device name
    private String ll;  //last login
    private String v;  //version num

    public ActiveUserDBEntry() {}

    public ActiveUserDBEntry(final String role, final String device, final String last_login, final String ver) {
        this.r = role;
        this.d = device;
        this.ll = last_login;
        this.v = ver;
    }

    public String getR() {
        return r;
    }

    public String getD() {
        return d;
    }

    public String getLl() {
        return ll;
    }

    public String getV() {
        return v;
    }

    /* SGO: Adding the below adds a new attribute "Role" in firebase DB
    public String getRole() {
        return getR();
    } */

    /*
    public Integer getTotalNumOfRoots() {
        return Integer.valueOf(getR());
    }

    public Integer getTotalNumOfAdmins() {
        return Integer.valueOf(getLl());
    }

    public Integer getTotalNumOfMembers() {
        return Integer.valueOf(getD());
    }*/

    private void incrTotalNumOfRoots() {
        Integer count = Integer.valueOf(getR());
        count++;
        this.r = count.toString();
    }

    private void incrTotalNumOfAdmins() {
        Integer count = Integer.valueOf(getLl());
        count++;
        this.ll = count.toString();
    }

    private void incrTotalNumOfMembers() {
        Integer count = Integer.valueOf(getD());
        count++;
        this.d = count.toString();
    }

    public void incrCountForNewUser(final String role) {
        switch (role) {
            case Constants.ROOT:
                incrTotalNumOfRoots();
                break;
            case Constants.ADMIN:
                incrTotalNumOfAdmins();
                break;
            case Constants.MEMBER:
                incrTotalNumOfMembers();
                break;
        }
    }

    @Override
    public String toString() {
        return "ActiveUserDBEntry{" +
                "ver='" + v + '\'' +
                ", role='" + r + '\'' +
                ", device='" + d + '\'' +
                ", last_login='" + ll + '\'' +
                '}';
    }
}
