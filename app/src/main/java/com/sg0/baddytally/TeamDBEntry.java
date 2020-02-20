package com.sg0.baddytally;

import java.util.List;

public class TeamDBEntry {
    final static int MAX_ID_LEN = 12;
    private String id;
    private List<String> p;
    private Integer seed;

    public TeamDBEntry() {
        id = "";
        seed = 0;
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
            this.id = this.id.substring(0,MAX_ID_LEN);
        //The substring begins at the specified beginIndex and extends to the character at
        //index endIndex - 1. Thus the length of the substring is endIndex-beginIndex.
        //Log.e("TeamDBEntry", "setId: " + this.id);
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

    public String toPlayersStr() {
        StringBuilder players = new StringBuilder("[");
        for(int i = 0; i < p.size(); i++) {
            players.append(p.get(i));
            if(i != p.size()-1) players.append(", ");
        }
        players.append("]");
        return players.toString();
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    @Override
    public String toString() {
        return "TeamDBEntry{" +
                "id='" + id + '\'' +
                ", seed='" + seed + '\'' +
                ", p='" + p.toString() + '\'' +
                '}';
    }
}
