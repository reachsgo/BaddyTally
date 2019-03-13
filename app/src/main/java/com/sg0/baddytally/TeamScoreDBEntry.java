package com.sg0.baddytally;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
class TeamScoreDBEntry {
    //shorter names will save firebase DB space
    private int pts;  //Team points
    private int mW;   //Num of Wins in matches
    private int mP;   //Num of matches played
    private int gW;  //Num of Wins in games (could be best-of-3 games in a match)
    private int gP;  //Num of games played (could be best-of-3 games in a match)
    private int gPts;  //Num of game points (accumulation of game points scored by this team ex: 21-10, 15-21 will give 36 points to first team)
    private int gPtsA; //Num of game points Against (accumulation of game p scored by opp team ex: 21-10, 15-21 will give 36 points to first team)

    public TeamScoreDBEntry() {
        pts = 0;
        mW = 0;
        mP = 0;
        gW = 0;
        gP = 0;
        gPts = 0;
        gPtsA = 0;
    }

    public void setPts(int pts) {
        this.pts = pts;
    }

    public void setmW(int mW) {
        this.mW = mW;
    }

    public void setmP(int mP) {
        this.mP = mP;
    }

    public void setgW(int gW) {
        this.gW = gW;
    }

    public void setgP(int gP) {
        this.gP = gP;
    }

    public void setgPts(int gPts) {
        this.gPts = gPts;
    }

    public void setgPtsA(int gPtsA) {
        this.gPtsA = gPtsA;
    }

    public int getPts() {
        return pts;
    }

    public int getmW() {
        return mW;
    }

    public int getmP() {
        return mP;
    }

    public int getgW() {
        return gW;
    }

    public int getgP() {
        return gP;
    }

    public int getgPts() {
        return gPts;
    }

    public int getgPtsA() {
        return gPtsA;
    }

    public void wonGame(final int gPoints, final int gPointsAgainst){
        this.gW++;
        this.gP++;
        this.gPts += gPoints;
        this.gPtsA += gPointsAgainst;
    }

    public void lostGame(final int gPoints, final int gPointsAgainst){
        this.gP++;
        this.gPts += gPoints;
        this.gPtsA += gPointsAgainst;
    }

    public void wonMatch(){
        this.pts++;
        this.mW++;
        this.mP++;
        Log.i("Tournament", "wonMatch:" + toString());
    }

    public void lostMatch(){
        this.mP++;
        Log.i("Tournament", "lostMatch:" + toString());
    }


    @Override
    public String toString() {
        return "TeamScoreDBEntry{" +
                "pts=" + pts +
                ", mW=" + mW +
                ", mP=" + mP +
                ", gW=" + gW +
                ", gP=" + gP +
                ", gPts=" + gPts +
                ", gPtsA=" + gPtsA +
                '}';
    }

    public SpannableString toDisplayString() {
        String tempString =
                " Points: " + pts + "\n" +
                " Match Stats:\n" +
                "       Wins: " + mW + "\n" +
                "       Played: " + mP + "\n" +
                " Game Stats:\n" +
                "       Wins: " + gW + "\n" +
                "       Played: " + gP + "\n" +
                "       Total points: " + gPts + "\n" +
                "       Total points against: " + gPtsA;
        SpannableString spanString = new SpannableString(tempString);
        spanString.setSpan(new RelativeSizeSpan(0.8f), 0, tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanString;
    }
}
