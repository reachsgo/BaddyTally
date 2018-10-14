package com.sg0.baddytally;

//import java.io.Serializable;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import java.util.ArrayList;
import java.util.List;


enum ShuffleFlag {
    UNKNOWN,
    CANDIDATE,
    RELEGATE,
    PROMOTE
}

class PlayerData {
    private String name;
    private String group;
    public List<PointsDBEntry> points;
    private ShuffleFlag shuffleFlag;

    public PlayerData(String group) {
        this.name = "Player";
        this.group = group;
        shuffleFlag = ShuffleFlag.UNKNOWN;
        points = new ArrayList<>(2);
        points.add(new PointsDBEntry ());  //season
        points.add(new PointsDBEntry ());  //innings
    }

    public PlayerData(String group, String name, List<PointsDBEntry> pts) {
        this.name = name;
        //this.innings_score = innings_score;
        //this.overall_score = overall_score;
        this.group = group;
        this.points = pts;
    }

    public PlayerData(PlayerData pd) {
        this.name = pd.name;
        this.group = pd.group;
        this.points = pd.points;
        this.shuffleFlag = pd.shuffleFlag;
    }

    public void resetGamesPlayed_innings() {
        //reset Games Played
        points.get(Constants.INNINGS_IDX).setP(0);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getPoints_innings() {
        return Integer.toString(points.get(Constants.INNINGS_IDX).getPts());
    }

    public int getPointsInt_innings() {
        return points.get(Constants.INNINGS_IDX).getPts();
    }

    public String getPoints(final int idx) {
        return Integer.toString(points.get(idx).getPts());
    }

    public String getPoints_season() {
        return Integer.toString(points.get(Constants.SEASON_IDX).getPts());
    }

    public int getPointsInt_season() {
        return points.get(Constants.SEASON_IDX).getPts();
    }


    public void setPts_innings(Integer innings_score) {
        points.get(Constants.INNINGS_IDX).setPts(innings_score);
    }

    public void setPts_season(Integer overall_score) {
        points.get(Constants.SEASON_IDX).setPts(overall_score);
    }

    public void setPoints(final int idx, Integer points) {
        switch(idx) {
            case Constants.INNINGS_IDX:
                setPts_innings(points);
                break;
            case Constants.SEASON_IDX:
                setPts_season(points);
                break;
        }
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public PointsDBEntry getPointsDBEntry_season() {
        return points.get(Constants.SEASON_IDX);
    }

    public PointsDBEntry getPointsDBEntry_innings() {
        return points.get(Constants.INNINGS_IDX);
    }

    public String getGamesWon (final int idx) {
        return Integer.toString(points.get(idx).getW());
    }

    public String getGamesWon_season () {
        return Integer.toString(points.get(Constants.SEASON_IDX).getW());
    }

    public String getGamesWon_innings () {
        return Integer.toString(points.get(Constants.INNINGS_IDX).getW());
    }

    public String getGamesPlayed (final int idx) {
        return Integer.toString(points.get(idx).getP());
    }

    public String getGamesPlayed_season () {
        return Integer.toString(points.get(Constants.SEASON_IDX).getP());
    }

    public String getGamesPlayed_innings () {
        return Integer.toString(points.get(Constants.INNINGS_IDX).getP());
    }

    public int getGamesPlayed_innings_int() {
        return points.get(Constants.INNINGS_IDX).getP();
    }

    public String getWinPercentage (final int idx) {
        return Integer.toString(points.get(idx).getWinPercentage());
    }

    public String getWinPercentage_season () {
        return Integer.toString(points.get(Constants.SEASON_IDX).getWinPercentage());
    }

    public String getWinPercentage_innings () {
        return Integer.toString(points.get(Constants.INNINGS_IDX).getWinPercentage());
    }

    public void setShuffleFlag(ShuffleFlag shuffleFlag) {
        this.shuffleFlag = shuffleFlag;
    }

    public int incrGamesPlayed_innings() {
        return points.get(Constants.INNINGS_IDX).incrGamesPlayed();
    }

    public int wonMatch(boolean singles){
        points.get(Constants.SEASON_IDX).wonMatch(singles);
        return points.get(Constants.INNINGS_IDX).wonMatch(singles);
    }

    public void lostMatch(boolean singles){
        points.get(Constants.SEASON_IDX).wonMatch(singles);
        points.get(Constants.INNINGS_IDX).wonMatch(singles);
    }

    public void markToRelegate(){
        shuffleFlag = ShuffleFlag.RELEGATE;
    }

    public void markToPromote(){
        shuffleFlag = ShuffleFlag.PROMOTE;
    }

    public void mark(){
        shuffleFlag = ShuffleFlag.CANDIDATE;
    }

    public boolean isMarked(){
        return ((shuffleFlag == ShuffleFlag.CANDIDATE) || isMarkedToRelegate() || isMarkedToPromote());
    }

    public boolean isMarkedToRelegate(){
        return shuffleFlag == ShuffleFlag.RELEGATE;
    }

    public boolean isMarkedToPromote(){
        return shuffleFlag == ShuffleFlag.PROMOTE;
    }

    public SpannableString getPtsFormat_season(){
        String tempString = getPoints_season() + "\n" + getWinPercentage_season() + "%";
        SpannableString spanString = new SpannableString(tempString);
        spanString.setSpan(new RelativeSizeSpan(0.7f), getPoints_season().length(), tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanString;
    }

    public SpannableString getPtsFormat_innings(){
        String tempString = getPoints_innings() + "\n" + getWinPercentage_innings() + "%";
        SpannableString spanString = new SpannableString(tempString);
        spanString.setSpan(new RelativeSizeSpan(0.7f), getPoints_innings().length(), tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanString;
    }

    public SpannableString getPtsDetailFormat_season(){
        String heading = "\n Season ";
        String tempString = heading + "\n\n\tPoints: " + getPoints_season() +
                "\n\tWon: " + getGamesWon_season() + "\n\tPlayed: " +
                getGamesPlayed_season()  +"\n\tWin%: " +
                getWinPercentage_season() + "%";
        SpannableString spanString = new SpannableString(tempString);
        spanString.setSpan(new UnderlineSpan(), 0, heading.length(), 0);
        spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, heading.length(), 0);
        return spanString;
    }

    public SpannableString getPtsDetailFormat_innings(){
        String heading = "\n Innings ";
        String tempString = heading + "\n\n\tPoints: " + getPoints_innings() +
                "\n\tWon: " + getGamesWon_innings() +
                "\n\tPlayed: " + getGamesPlayed_innings()  +
                "\n\tWin%: " + getWinPercentage_innings()  + "%";
        SpannableString spanString = new SpannableString(tempString);
        spanString.setSpan(new UnderlineSpan(), 0, heading.length()+1, 0);
        spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, heading.length(), 0);
        return spanString;
    }

    @Override
    public String toString() {
        return toStringShort();
        /*
        String shuFlag = isMarkedToPromote() ? "P" : (isMarkedToRelegate() ? "R" : "U");
        return "PlayerData{" + group + "/" + name + ": flag=" + shuFlag + " season=" +
                points.get(Constants.SEASON_IDX).toString() + " innings=" +
                points.get(Constants.INNINGS_IDX).toString() + "}";
                */
    }

    public String toStringShort() {
        String shuFlag = isMarkedToPromote() ? "P" : (isMarkedToRelegate() ? "R" : "U");
        return group + "/" + name + ": \'" + shuFlag + "\' S=[" +
                points.get(Constants.SEASON_IDX).toStringShort() + "] I=" +
                points.get(Constants.INNINGS_IDX).toStringShort() + "] ";
    }

    public String toPrintString() {
        return group + '/' + name + '/' + points.get(Constants.INNINGS_IDX).getPts() + '/' +  points.get(Constants.SEASON_IDX).getPts() ;
    }
}
