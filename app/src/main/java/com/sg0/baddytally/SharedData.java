package com.sg0.baddytally;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public String toString() {
        String str = "SharedData{" +
                "mUser='" + mUser + '\'' +
                ", mRole='" + mRole + '\'' +
                ", mClub='" + mClub + '\'' +
                ", mInnings='" + mInnings + '\'' +
                ", mRoundName='" + mRoundName + '\'' +
                ", mAdminCode='" + mAdminCode + '\'' +
                ", mMemCode='" + mMemCode + '\'';
        if (mGoldPlayers != null) str += ", mGoldPlayers=" + mGoldPlayers.size();
        if (mSilverPlayers != null) str += ", mSilverPlayers=" + mSilverPlayers.size();
        str += '}';
        return str;
    }
}