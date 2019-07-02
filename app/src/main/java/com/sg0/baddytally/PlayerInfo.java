package com.sg0.baddytally;

public class PlayerInfo {
    public String T; //Team name (short)
    public int gW; //Number of game wins
    public int gP;  //Number of games played
    public String name;

    public PlayerInfo() {
        gW = 0;
        gP = 0;
    }

    public void wonGame(){
        this.gW++;
        this.gP++;
    }

    public void lostGame(){
        this.gP++;
    }

    void deleteGame(final boolean won){
        if(won) this.gW--;
        this.gP--;
    }

    @Override
    public String toString() {
        return "PlayerInfo{" +
                "T='" + T + '\'' +
                ", wins='" + gW + '\'' +
                ", games-played='" + gP + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}