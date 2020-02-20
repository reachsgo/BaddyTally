package com.sg0.baddytally;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import java.util.ArrayList;
import java.util.Objects;

public class TeamInfo {
    private static final String TAG = "TeamInfo";
    public String name, desc;
    public ArrayList<String> players;
    public ArrayList<String> p_nicks;
    public TeamScoreDBEntry score;

    public TeamInfo() {
        this.name = "";
        this.desc = "";
        players = new ArrayList<>();
        p_nicks = new ArrayList<>();
        Log.d(TAG, "TeamInfo constructed" + toString());
    }

    public TeamInfo(final String name) {
        this.name = name; //short name
        this.desc = name; //long name
        players = new ArrayList<>();
        p_nicks = new ArrayList<>();
        score = new TeamScoreDBEntry();
        Log.d(TAG, "TeamInfo constructed:" + toString());
    }

    boolean isValid() {
        if(name.isEmpty()) return false;
        if(players.size() == 0) return false;
        if(p_nicks.size() == 0) return false;
        return true;
    }

    public String getPlayerNameLong(final int i) {
        if(p_nicks.size()<=0 || i>=p_nicks.size()) return "NoData";
        return p_nicks.get(i) + Constants.COLON_DELIM + players.get(i);
    }

    public static String getShortName(final String name){
        if(name.contains(Constants.COLON_DELIM)) {
            //expected that input is a concatinated string (short + long name)
            return name.split(Constants.COLON_DELIM)[0];  //example: "P02: Player 002"
        } else {
            return name;
        }
    }

    public String get1LineDesc() {
        //String descStr = desc + " "; //Saving screen space to have multiple teams on the same page
        String descStr = "";
        if(p_nicks.size()>0) {
            descStr += "(";
            int idx = 0;
            for (String p: p_nicks) {
                if(idx>0) descStr += ", ";
                descStr += p;
                if(descStr.length()>40) {
                    descStr += "..";
                    break; //save screen space
                }
                idx++;
            }
            descStr += ")";
        }
        return descStr;
    }

    public SpannableString get2LinesDesc() {
        String tempString = name + "\n" + get1LineDesc();
        SpannableString spanString = new SpannableString(tempString);
        spanString.setSpan(new RelativeSizeSpan(0.7f), name.length(),
                tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamInfo teamInfo = (TeamInfo) o;
        return Objects.equals(name, teamInfo.name) &&
                Objects.equals(desc, teamInfo.desc) &&
                Objects.equals(players, teamInfo.players) &&
                Objects.equals(p_nicks, teamInfo.p_nicks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc);
    }

    SpannableStringBuilder toDisplayString() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append("\t");
        sb.append(SharedData.getInstance().getStyleString(name, Typeface.BOLD_ITALIC));
        sb.append(" : ");
        sb.append(SharedData.getInstance().getStyleString(desc, Typeface.BOLD));
        sb.append("\n");
        if(players.size() != p_nicks.size()) {
            //this code might never be hit
            sb.append(p_nicks.toString());
            sb.append("/");
            sb.append(players.toString());
        } else {
            for (int i = 0; i < players.size(); i++) {
                sb.append("\t\t\t\t");
                sb.append(SharedData.getInstance().getStyleString(
                        p_nicks.get(i), Typeface.ITALIC));
                sb.append(" : ");
                sb.append(players.get(i));
                sb.append("\n");
            }
        }
        return sb;
    }

    @Override
    public String toString() {
        if(name.isEmpty() && desc.isEmpty()) return "[]";
        else {
            String retStr = "TeamInfo{" +
                    "name='" + name + '\'' +
                    ", desc='" + desc + '\'' +
                    ", players=" + players.toString() +
                    ", p_nicks=" + p_nicks.toString();
            if(null!=score) retStr += ", score=" + score.toString();
            retStr += '}';
            return retStr;
        }
    }
}