package com.sg0.baddytally;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class GameJournal {
    public String mWinner1;   //winner1
    public String mWinner2;   //winner2
    public String mLoser1;   //loser1
    public String mLoser2;   //loser2
    public int mWin_score;
    public int mOpponent_score;
    public String mDate;
    public int mGameNum;
    public String mInnings;
    public String mGameType;
    public String mUser;

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
        if (player.equalsIgnoreCase(mWinner1) || player.equalsIgnoreCase(mWinner2) || player.equalsIgnoreCase(mLoser1) || player.equalsIgnoreCase(mLoser2)) {
            return true;
        }
        return false;
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

    public String getPlayerPartner(String player) {
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

    public String getUsr() {
        return mUser;
    }

}
