package com.sg0.baddytally;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserDBEntry {
    final static String CLUB_SEPARATOR = ",";
    private String clubs;
    private String ts;
    private String ph;
    private int ver;  //versionCode
    private int maxC; //Maximum num of clubs allowed

    //Firebase needs a no-argument constructor
    UserDBEntry() {
        clubs = "";
        setTs("now");
        setPh("");
        ver = 0;
        maxC = Constants.MAXNUM_CLUBS_PER_USER;
    }

    public String getClubs() {
        return clubs;
    }

    public void setClub(String club) {
        if(this.clubs.isEmpty()) {
            this.clubs = club;
        } else if(!this.clubs.contains(club)) {
            this.clubs = this.clubs + CLUB_SEPARATOR + club;
        }
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        if(ts.equals("now")) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",
                    Locale.getDefault());
            Date date = new Date();
            ts = dateFormat.format(date);
        }
        this.ts = ts;
    }

    public String getPh() {
        return ph;
    }

    public void setPh(String ph) {
        if(ph.isEmpty()) {
            ph = android.os.Build.MODEL;
        }
        this.ph = ph;
    }

    public int getVer() {
        return ver;
    }

    public void setVer(int v) {
        this.ver = v;
    }

    public void setClubs(String clubs) {
        this.clubs = clubs;
    }

    public int getMaxC() {
        return maxC;
    }

    public void setMaxC(int maxC) {
        this.maxC = maxC;
    }

    @Override
    public String toString() {
        return "UserDBEntry{" +
                "clubs='" + clubs + '\'' +
                ", ts='" + ts + '\'' +
                ", ph='" + ph + '\'' +
                ", v='" + ver + '\'' +
                '}';
    }

}
