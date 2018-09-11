package com.sg0.baddytally;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.ArrayList;

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
    public String mInningsDBKey;

    private static SharedData sSoleInstance;

    private SharedData(){
        mGoldPlayers = null;
        mSilverPlayers = null;
        mMemCode = "";
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