package com.sg0.baddytally;


import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class PointsDBEntry {
    private int pts;  //points
    private int P;    //Games played
    private int W;    //Games won

    public PointsDBEntry() {
        this.pts = 0;
        this.P = 0;
        this.W = 0;
    }

    public PointsDBEntry(PointsDBEntry pDBEntry) {
        this.pts = pDBEntry.pts;
        this.P = pDBEntry.P;
        this.W = pDBEntry.W;
    }

    ////////// generated

    public int getPts() {
        return pts;
    }

    public void setPts(int pts) {
        this.pts = pts;
    }

    public int getP() {
        return P;
    }

    public void setP(int p) {
        P = p;
    }

    public int getW() {
        return W;
    }

    public void setW(int w) {
        W = w;
    }


    /////////////////

    //public void setPoints(int pts) {
    //    this.pts = pts;
    //}

    @Exclude     //dont add in the Firebase DB on invoking setValue()
    public int getWinPercentage () {
        if(P>0) return ((W * 100) / P);     // games_won / games_played x 100
        else return 0;
    }

    public int incrGamesPlayed() {
        return ++P;
    }

    public int wonMatch(boolean singles){
        this.pts++;
        //if(singles) this.pts++; //singles will also have ony 1 point.
        this.P++;
        this.W++;
        return this.pts;
    }

    public void lostMatch(){
        this.P++;   //just increment the num of games played
    }

    public int deleteMatch (final boolean singles, final boolean winner){
        if(winner) {
            //for the winner, reduce points
            this.pts--; //points
            //if(singles) this.pts--;   //singles will also have ony 1 point.
            if(this.pts<0) this.pts=0;
            //and the number of wins
            this.W--;  //number of wins
            if(this.W<0) this.W=0;
        }

        //reduce total_games_played for both winner & loser
        this.P--;  //games played
        if(this.P<0) this.P=0;

        return this.pts;
    }

    @Override
    public String toString() {
        return "PointsDBEntry{" +
                "pts = " + pts +
                ", Games Played = " + P +
                ", Games Won = " + W +
                '}';
    }

    public String toStringShort() {
        return pts + "=>" + W +"/" + P;
    }

}
