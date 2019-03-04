package com.sg0.baddytally;

import android.util.Log;

import java.util.List;

public class TeamDBEntry {
    final private int MAX_ID_LEN = 12;
    private String id;
    private List<String> p;

    public TeamDBEntry() {
        id = "";
    }

    public TeamDBEntry(String id, List<String> p) {
        this.id = id;
        this.p = p;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id.toLowerCase();
        if(this.id.length()>MAX_ID_LEN)
            this.id = this.id.substring(0,MAX_ID_LEN-1);
        Log.e("TeamDBEntry", "setId: " + this.id);
    }

    public List<String> getP() {
        return p;
    }

    public void setP(List<String> p) {
        this.p = p;
    }

    public String toDispString() {
        return "name=" + id + ", players=" + p.toString();
    }

    @Override
    public String toString() {
        return "TeamDBEntry{" +
                "id='" + id + '\'' +
                ", p='" + p.toString() + '\'' +
                '}';
    }
}