package com.sg0.baddytally;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Objects;

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

    public GameJournalDBEntry(String p1, String p2, String p3, String p4) {
        this.mGT = Constants.DOUBLES;
        this.mW1 = p1;
        this.mW2 = p2;
        this.mL1 = p3;
        this.mL2 = p4;
        this.mGNo = 1;
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

    public boolean playerInvolved(final String player) {
        return player.equalsIgnoreCase(mW1) || player.equalsIgnoreCase(mW2) || player.equalsIgnoreCase(mL1) || player.equalsIgnoreCase(mL2);
    }

    public boolean aWinner(final String player) {
        return player.equalsIgnoreCase(mW1) || player.equalsIgnoreCase(mW2);
    }

    public boolean aLoser(final String player) {
        return player.equalsIgnoreCase(mL1) || player.equalsIgnoreCase(mL2);
    }

    public boolean playersInvolved4(final String player1, final String player2, final String player3, final String player4) {
        return playerInvolved(player1) &&
                playerInvolved(player2) &&
                playerInvolved(player3) &&
                playerInvolved(player4);
    }

    public boolean playersInvolved3(final String player1, final String player2, final String player3, final String player4) {
        return ((playerInvolved(player1) && playerInvolved(player2) && playerInvolved(player3)) ||
                (playerInvolved(player1) && playerInvolved(player2) && playerInvolved(player4)) ||
                (playerInvolved(player1) && playerInvolved(player3) && playerInvolved(player4)) ||
                (playerInvolved(player2) && playerInvolved(player3) && playerInvolved(player4)));
    }

    public boolean playedBefore(final String player1, final String player2, final String player3, final String player4) {
        if(player1.isEmpty()) return false;
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
                if (player3.isEmpty() || player4.isEmpty()) return false;
                if ( getPlayerPartner(player3).equalsIgnoreCase(player4) )
                    return true;
                break;
        }
        return false;
    }

    public String getPlayerPartner(final String player) {
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

    public boolean exactlyEqual(GameJournalDBEntry o) {
        return this.mW1.equals(o.mW1) && this.mW2.equals(o.mW2) &&
                this.mL1.equals(o.mL1) && this.mL2.equals(o.mL2) &&
                this.mWS == o.mWS && this.mLS == o.mLS &&
                this.mGNo == o.mGNo;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameJournalDBEntry that = (GameJournalDBEntry) o;

        //returns true if all the teams are the same. Winner/Loser/Score is not considered.
        String p2 = getPlayerPartner(that.getmW1());  //get that.W1's partner
        String op2 = getPlayerPartner(that.getmL1());  //get opposite team's partner
        if(p2.isEmpty() || op2.isEmpty()) return false; //if they are not found, this game involves different players
        return p2.equals(that.getmW2()) && op2.equals(that.getmL2());
        //return playersInvolved4(that.getmW1(), that.getmW2(), that.getmL1(), that.getmL2());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getmW1(), getmW2(), getmL1(), getmL2());
    }

    //public String getWinnersString() ==> adds "winnersString" to firebase DB
    public String getWinnersString(final boolean junk) {
        if (getmW2().isEmpty()) return getmW1();
        else return getmW1() + "/" + getmW2();
    }

    //public String getLosersString() ==> adds "losersString" to firebase DB
    public String getLosersString(final boolean junk) {
        if (getmL2().isEmpty()) return getmL1();
        else return getmL1() + "/" + getmL2();
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
                loser + "\n" + mWS + "-" + mLS;
    }

    public String toPlayersString() {
        String winner = mW1;
        String loser = mL1;
        if(!mW2.isEmpty()) winner += "/" + mW2;
        if(!mL2.isEmpty()) loser += "/" + mL2;

        return  winner + "  vs  " + loser + "\n";
    }

    public String toPlayersString(final String p1) {
        String team1, team2;
        if(aWinner(p1)) {
            team1 = mW1;
            team2 = mL1;
            if(!mW2.isEmpty()) team1 += "/" + mW2;
            if(!mL2.isEmpty()) team2 += "/" + mL2;
        } else {
            team1 = mL1;
            team2 = mW1;
            if(!mL2.isEmpty()) team1 += "/" + mL2;
            if(!mW2.isEmpty()) team2 += "/" + mW2;
        }
        return  team1 + "  vs  " + team2 + "\n";
    }

    public String toScoreString(final String p1) {
        if(aWinner(p1)) {
            return mWS + "-" + mLS;
        } else {
            return mLS + "-" + mWS;
        }
    }

    Integer scoreForPlayers(final String p1, final String p2) {
        if(aWinner(p1) && aWinner(p2)) return getmWS();
        if(aLoser(p1) && aLoser(p2)) return getmLS();
        return 0;
    }

}
