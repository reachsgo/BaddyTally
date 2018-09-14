package com.sg0.baddytally;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SharedData {
    public int mNumOfGroups;
    public String mUser;
    public String mClub;
    public String mRole;
    public String mInnings;
    public String mRoundName;
    public String mAdminCode;
    public String mMemCode;
    public String mRootCode;
    public ArrayList<PlayerData> mGoldPlayers;
    public ArrayList<PlayerData> mSilverPlayers;
    public Integer mInningsDBKey;

    private static SharedData sSoleInstance;
    private static final String TAG = "SharedData";

    private SharedData(){
        mGoldPlayers = null;
        mSilverPlayers = null;
        mMemCode = "";
        mRole = "unknown";
        mNumOfGroups = Constants.NUM_OF_GROUPS;
    }  //private constructor.

    public static SharedData getInstance(){
        if (sSoleInstance == null){ //if there is no instance available... create new one
            sSoleInstance = new SharedData();
        }
        return sSoleInstance;
    }

    public boolean isRoot() {
        return Constants.ROOT.equals(mRole);
    }

    public boolean isAdmin() {
        return Constants.ADMIN.equals(mRole);
    }

    public SpannableStringBuilder getRedString(String text) {

        // Initialize a new foreground color span instance
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.RED);

        // Initialize a new spannable string builder instance
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);

        // Apply the text color span
        ssBuilder.setSpan(
                foregroundColorSpan,
                0,
                text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return ssBuilder;
    }

    public void sortPlayers(ArrayList<PlayerData> playersList, final boolean descending) {
        //playersList is obj reference of the actual list being passed in.
        //Any updates to the contents will be visible to the caller.
        //But, you cannot change the obj reference itself (ex: playersList = another_list;).
        Collections.sort(playersList, new Comparator<PlayerData>() {
            @Override
            public int compare(PlayerData p1, PlayerData p2) {
                    //Integer.valueOf(p1.getPointsInt_innings()).compareTo(p2.getPointsInt_innings()); //ascending order
                    int value1 = Integer.valueOf(p2.getPointsInt_innings()).compareTo(p1.getPointsInt_innings());  //descending order
                    if (value1 == 0) {
                        /* Sorting order criteria:
                        Top 3 players of Silver move to Gold & Bottom 3 players of Gold move to Silver.
                        If there is a tie, player selection is done on the below criteria in that order:
                            1. higher win % (number_of_wins / number_of_games_played x 100).
                            2. most number of wins
                            3. most number of games played
                            4. toss
                        */
                        int value2 = Integer.valueOf(p2.getWinPercentage_innings()).compareTo(Integer.valueOf(p1.getWinPercentage_innings()));
                        if (value2 == 0) {
                            int value3 = Integer.valueOf(p2.getGamesWon_innings()).compareTo(Integer.valueOf(p1.getGamesWon_innings()));
                            if (value3 == 0) {
                                return Integer.valueOf(p2.getGamesPlayed_innings()).compareTo(Integer.valueOf(p1.getGamesPlayed_innings()));
                            } else return value3;
                        } else return value2;
                    }
                    return value1;
            }
        });
        if(!descending) Collections.reverse(playersList);
        Log.d(TAG, "sortPlayers: Sorted playersList size: " + Integer.toString(playersList.size()));
    }

    @Override
    public String toString() {
        String str = "SharedData{" +
                "mUser='" + mUser + '\'' +
                ", mRole='" + mRole + '\'' +
                ", mClub='" + mClub + '\'' +
                ", mInnings='" + mInnings + '\'' +
                ", mRoundName='" + mRoundName + '\'';
                //", mAdminCode='" + mAdminCode + '\'' +
                //", mMemCode='" + mMemCode + '\'';
        if (mGoldPlayers != null) str += ", mGoldPlayers=" + mGoldPlayers.size();
        if (mSilverPlayers != null) str += ", mSilverPlayers=" + mSilverPlayers.size();
        str += '}';
        return str;
    }
}