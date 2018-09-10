package com.sg0.baddytally;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
class GameJournal {
    private String mWinner1;   //winner1
    private String mWinner2;   //winner2
    private String mLoser1;   //loser1
    private String mLoser2;   //loser2
    private int mWin_score;
    private int mOpponent_score;
    private String mDate;
    private int mGameNum;
    private String mInnings;
    private String mGameType;
    private String mUser;

    public void setmWinner1(String mWinner1) {
        this.mWinner1 = mWinner1;
    }

    public void setmWinner2(String mWinner2) {
        this.mWinner2 = mWinner2;
    }

    public void setmLoser1(String mLoser1) {
        this.mLoser1 = mLoser1;
    }

    public void setmLoser2(String mLoser2) {
        this.mLoser2 = mLoser2;
    }

    public void setmWin_score(int mWin_score) {
        this.mWin_score = mWin_score;
    }

    public void setmOpponent_score(int mOpponent_score) {
        this.mOpponent_score = mOpponent_score;
    }

    public void setmDate(String mDate) {
        this.mDate = mDate;
    }

    public void setmGameNum(int mGameNum) {
        this.mGameNum = mGameNum;
    }

    public void setmInnings(String mInnings) {
        this.mInnings = mInnings;
    }

    public void setmGameType(String mGameType) {
        this.mGameType = mGameType;
    }

    public void setmUser(String mUser) {
        this.mUser = mUser;
    }

    public String getmWinner1() {
        return mWinner1;
    }

    public String getmWinner2() {
        return mWinner2;
    }

    public String getmLoser1() {
        return mLoser1;
    }

    public String getmLoser2() {
        return mLoser2;
    }

    public int getmWin_score() {
        return mWin_score;
    }

    public int getmOpponent_score() {
        return mOpponent_score;
    }

    public String getmDate() {
        return mDate;
    }

    public int getmGameNum() {
        return mGameNum;
    }

    public String getmInnings() {
        return mInnings;
    }

    public String getmGameType() {
        return mGameType;
    }

    public String getmUser() {
        return mUser;
    }

    public GameJournal() {
        this.mDate = "Nov 20 1978";
        this.mInnings = "godha";
    }

    public GameJournal(String mDate, String mInnings, String user) {
        this.mDate = mDate;
        this.mInnings = mInnings;
        this.mUser = user;
    }

    public void setResult(String datestr, String gameType, String winner1, String winner2, String loser1, String loser2, int win_score, int opp_score) {
        this.mDate = datestr;
        this.mGameType = gameType;
        this.mWinner1 = winner1;
        this.mWinner2 = winner2;
        this.mLoser1 = loser1;
        this.mLoser2 = loser2;
        this.mWin_score = win_score;
        this.mOpponent_score = opp_score;
        this.mGameNum = 1;
    }

    public boolean playerInvolved(String player) {
        return player.equalsIgnoreCase(mWinner1) || player.equalsIgnoreCase(mWinner2) || player.equalsIgnoreCase(mLoser1) || player.equalsIgnoreCase(mLoser2);
    }


    public boolean playedBefore(String player1, String player2, String player3, String player4) {
        switch (mGameType){
            case Constants.SINGLES:
                if ((player1.equalsIgnoreCase(mWinner1) && player3.equalsIgnoreCase(mLoser1)) ||
                        (player1.equalsIgnoreCase(mLoser1) && player3.equalsIgnoreCase(mWinner1))) {
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
        if (Constants.SINGLES.equals(mGameType)) {
            return "";
        }
        if (player.equalsIgnoreCase(mWinner1)) {
            return mWinner2;
        } else if (player.equalsIgnoreCase(mWinner2)) {
            return mWinner1;
        } else if (player.equalsIgnoreCase(mLoser1)) {
            return mLoser2;
        } else if (player.equalsIgnoreCase(mLoser2)) {
            return mLoser1;
        }
        return "";
    }

    public String toReadableString() {
        return mInnings + "/" + mDate + "/" + mGameType + "/" + mGameNum + ": " +
                mWinner1 + "/" + mWinner2 + " vs " +
                mLoser1 + "/" + mLoser2 + " : " +
                mWin_score + "-" + mOpponent_score;
    }

    public String toJournalEntry() {
        String winner = mWinner1;
        String loser = mLoser1;
        if (Constants.DOUBLES.equals(mGameType)) {
            winner += "/" + mWinner2;
            loser += "/" + mLoser2;
        }

        return  winner + " vs " +
                loser + " : " + mWin_score + "-" + mOpponent_score;
    }

}
