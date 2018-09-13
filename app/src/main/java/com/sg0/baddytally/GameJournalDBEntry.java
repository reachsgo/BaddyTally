package com.sg0.baddytally;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
class GameJournalDBEntry {
    //shorter names will save firebase DB space
    private String mW1;  //winner1
    private String mW2;  //winner2
    private String mL1;  //loser1
    private String mL2;  //loser2
    private int mWS;  //Winning Score
    private int mLS;  //Losing score
    private String mDate;  //mDate
    private int mGNo;  //Game Num
    private String mIn;  //Innings
    private String mGT;  //Game Type
    private String mU;  //User


    public void setmW1(String mW1) {
        this.mW1 = mW1;
    }

    public void setmW2(String mW2) {
        this.mW2 = mW2;
    }

    public void setmL1(String mL1) {
        this.mL1 = mL1;
    }

    public void setmL2(String mL2) {
        this.mL2 = mL2;
    }

    public void setmWS(int mWS) {
        this.mWS = mWS;
    }

    public void setmLS(int mLS) {
        this.mLS = mLS;
    }

    public void setmDate(String mDate) {
        this.mDate = mDate;
    }

    public void setmGNo(int mGNo) {
        this.mGNo = mGNo;
    }

    public void setmIn(String mIn) {
        this.mIn = mIn;
    }

    public void setmGT(String mGT) {
        this.mGT = mGT;
    }

    public void setmU(String mU) {
        this.mU = mU;
    }

    public String getmW1() {
        return mW1;
    }

    public String getmW2() {
        return mW2;
    }

    public String getmL1() {
        return mL1;
    }

    public String getmL2() {
        return mL2;
    }

    public int getmWS() {
        return mWS;
    }

    public int getmLS() {
        return mLS;
    }

    public String getmDate() {
        return mDate;
    }

    public int getmGNo() {
        return mGNo;
    }

    public String getmIn() {
        return mIn;
    }

    public String getmGT() {
        return mGT;
    }

    public String getmU() {
        return mU;
    }

    public GameJournalDBEntry() {
        this.mDate = "Nov 20 1978";
        this.mIn = "godha";
    }

    public GameJournalDBEntry(String mDate, String mIn, String user) {
        this.mDate = mDate;
        this.mIn = mIn;
        this.mU = user;
    }

    public void setResult(String datestr, String gameType, String winner1, String winner2, String loser1, String loser2, int win_score, int opp_score) {
        this.mDate = datestr;
        this.mGT = gameType;
        this.mW1 = winner1;
        this.mW2 = winner2;
        this.mL1 = loser1;
        this.mL2 = loser2;
        this.mWS = win_score;
        this.mLS = opp_score;
        this.mGNo = 1;
    }

    public boolean playerInvolved(String player) {
        return player.equalsIgnoreCase(mW1) || player.equalsIgnoreCase(mW2) || player.equalsIgnoreCase(mL1) || player.equalsIgnoreCase(mL2);
    }


    public boolean playedBefore(String player1, String player2, String player3, String player4) {
        switch (mGT){
            case Constants.SINGLES:
                if ((player1.equalsIgnoreCase(mW1) && player3.equalsIgnoreCase(mL1)) ||
                        (player1.equalsIgnoreCase(mL1) && player3.equalsIgnoreCase(mW1))) {
                    return true;
                }
                break;
            case Constants.DOUBLES:
                //Have the doubles partners played as a team before?
                if ( getPlayerPartner(player1).equalsIgnoreCase(player2) )
                    return true;
                if ( getPlayerPartner(player3).equalsIgnoreCase(player4) )
                    return true;
                break;
        }
        return false;
    }

    private String getPlayerPartner(String player) {
        if (Constants.SINGLES.equals(mGT)) {
            return "";
        }
        if (player.equalsIgnoreCase(mW1)) {
            return mW2;
        } else if (player.equalsIgnoreCase(mW2)) {
            return mW1;
        } else if (player.equalsIgnoreCase(mL1)) {
            return mL2;
        } else if (player.equalsIgnoreCase(mL2)) {
            return mL1;
        }
        return "";
    }

    public String toReadableString() {
        return mIn + "/" + mDate + "/" + mGT + "/" + mGNo + ": " +
                mW1 + "/" + mW2 + " vs " +
                mL1 + "/" + mL2 + " : " +
                mWS + "-" + mLS;
    }

    public String toJournalEntry() {
        String winner = mW1;
        String loser = mL1;
        if (Constants.DOUBLES.equals(mGT)) {
            winner += "/" + mW2;
            loser += "/" + mL2;
        }

        return  winner + " vs " +
                loser + " : " + mWS + "-" + mLS;
    }

}
