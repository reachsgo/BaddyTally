package com.sg0.baddytally;

import android.util.Log;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

class ExternalLink {
    private String l;  //label
    private String m;  //match Id
    private Boolean s;  //source or not


    public ExternalLink() {}

    public ExternalLink(String l, String m, Boolean s) {
        this.l = l;
        this.m = m;
        this.s = s;
    }

    public String getL() {
        return l;
    }

    public void setL(String l) {
        this.l = l;
    }

    public String getM() {
        return m;
    }

    public void setM(String m) {
        this.m = m;
    }

    public Boolean getS() {
        return s;
    }

    public void setS(Boolean s) {
        this.s = s;
    }


    @Override
    public String toString() {
        return "Ext{" +
                l + '.' + m + '.' + s +
                '}';
    }
}

@IgnoreExtraProperties
class TournaFixtureDBEntry {
    final static private String TAG = "TournaFixtureDBEntry";
    //supports only 2 teams
    private final int TEAM1_IDX = 0;
    private final int TEAM2_IDX = 1;

    //shorter names will save firebase DB space
    private List<String> T;  //teams
    private List<String> P;  //Previous links
    private List<ExternalLink> E;  //Previous links
    private String W;        //winner
    private Boolean F;  //final or not

    private void clear() {
        this.T = null;  //null is better, as an object with null is not written into firebase DB
        this.P = null;
        this.E = null;
        this.W = null;
        this.F = false;
    }

    public TournaFixtureDBEntry() {
        clear();
    }

    public TournaFixtureDBEntry(final TournaFixtureDBEntry o) {
        clear();
        if(null!=o.getT()) this.T = new ArrayList<>(o.getT());
        if(null!=o.getP()) this.P = new ArrayList<>(o.getP());
        if(null!=o.getE()) this.E = new ArrayList<>(o.getE());
        this.W = o.W;
    }

    public TournaFixtureDBEntry(final TournaMatchNode mN) {
        clear();
        if(mN.isExternalLink()) {
            if(!mN.getWinner().isEmpty()) {
                setW(mN.getWinner());  //There could be a winner in case of a BYE in UB match.
            } else {
                //EXTERNALLEAF node: only created in Lower Bracket. So, sourceFlag is false;
                setExtLink(TEAM1_IDX, mN.getExtFixtureLabel(), mN.getExtMatchId(), false);
                setTeam(TEAM1_IDX, mN.getExtMatchIdStr());
                setTeam(TEAM2_IDX, Constants.BYE);
            }
            //Log.d("TournaFixtureDBEntry", "isExternalLink: " + toString());
            return;
        } else if(mN.isBye()) {
            setW(Constants.BYE);
            //setTeam(TEAM1_IDX,Constants.BYE);
            //setTeam(TEAM2_IDX, "");
            //Log.d("TournaFixtureDBEntry", "isBye: " + toString());
            return;
        } else if(mN.isLeaf()) {
            //Log.d("TournaFixtureDBEntry", "++isLeaf:++");
        }

        if(null==mN.t1) {
            //Log.d("TournaFixtureDBEntry", "++ T1 is null ++");
        } else if(mN.t1.isExternalLink()) {
            //team1 node is an external link
            if(!mN.t1.getWinner().isEmpty()) {
                setTeam(TEAM1_IDX, mN.t1.getWinner());
            } else {
                setTeam(TEAM1_IDX, mN.t1.getExtMatchIdStr());
            }
            //if previous link is not set, then vertical lines will not be drawn
            //between EXTERNALLEAF and regular NODE (with teams linking to EXTERNALLEAF nodes) in the same round.
            setPrevLink(TEAM1_IDX, mN.t1.getId());
            //Log.d("TournaFixtureDBEntry", "mN.t1.isExternalLink: " + toString());
        } else if(mN.t1.isLeaf()) {
            if(!mN.t1.getWinner().isEmpty()) {
                setTeam(TEAM1_IDX, mN.t1.getWinner());
            } else setTeam(TEAM1_IDX, mN.t1.getDesc());  //desc has the team name, id has row-matchId
            //For Bye's coming into LB, SESR sets the winner as Bye. there is no
            //createRegularMatchesForThisRound() -> setWinnerString() sets the winner as Bye.
            //setWinnerString:[: (-1,-1)=fixU/1-3,EXTERNALLEAF,NULL,NULL,(bye)(W),false] vs [: (-1,-1)=fixU/1-4,EXTERNALLEAF,NULL,NULL,(bye)(W),false]
            //setWinnerString: [: (-1,-1)=fixU,NODE,/fixU/1-3,/fixU/1-4,(bye)(W),false]

            //createRegularMatchesForThisRound - Adding: [: (-1,-1)=fixU,NODE,/fixU/1-3,/fixU/1-4,(bye)(W),false]
            //setWinnerString:[: (-1,-1)=fixU/1-3,EXTERNALLEAF,NULL,NULL,(bye)(W),false] vs [: (-1,-1)=fixU/1-4,EXTERNALLEAF,NULL,NULL,(bye)(W),false]
            //setWinnerString: [: (-1,-1)=fixU,NODE,/fixU/1-3,/fixU/1-4,(bye)(W),false]

            //createRegularMatchesForThisRound - Adding: [: (-1,-1)=fixU,NODE,/fixU/2-10,/fixU,(W),false]
            //setWinnerString:[: (-1,-1)=fixU/2-10,EXTERNALLEAF,NULL,NULL,(W),false] vs [: (-1,-1)=fixU,NODE,/fixU/1-3,/fixU/1-4,(bye)(W),false]
            //setWinnerString: [: (-1,-1)=fixU,NODE,/fixU/2-10,/fixU,(W),false]

            setPrevLink(TEAM1_IDX, "");
            //Log.d("TournaFixtureDBEntry", "mN.t1.isLeaf: " + toString());
            //this.pr1 = mN.t1.getId();
        } else {
            setTeam(TEAM1_IDX, "");
            /*
                                             -------------
                                             |  EXTLEAF   |
                                             |  2-10*(W)  |__________
                                             --------------          |
                                                                     |
                      -------------           ------------           |       -------------
                      |  EXTLEAF   |_________|    NODE-A  |          |_______|    NODE-B  |
                      |   bye(W)   |         |    bye(W)  |                  |            |
                      --------------     ____|            |__________________|            |
                                         |    ------------                   |            |
                                         |                                    -------------
                      -------------      |
                      | EXTLEAF   |______|
                      |   bye(W)  |
                      ------------

                      For LB TournaMatchNode structure like above where 2 BYEs (losers from UB) are
                      coming to LB, the immediate node (NODE-A) created will have the winner set as bye.
                            createRegularMatchesForThisRound() -> setWinnerString()
                            //setWinnerString:[: (-1,-1)=fixU/1-3,EXTERNALLEAF,NULL,NULL,(bye)(W),false] vs [: (-1,-1)=fixU/1-4,EXTERNALLEAF,NULL,NULL,(bye)(W),false]
                            //setWinnerString: [: (-1,-1)=fixU,NODE,/fixU/1-3,/fixU/1-4,(bye)(W),false]
                      In this case, NODE-B when translated to TournaFixtureDBEntry should have its team2 name
                      set properly to "bye". This should happen here, as this will not be done later from EnterScore.
            */
            if(!mN.t1.getWinner().isEmpty()) {
                setTeam(TEAM1_IDX, mN.t1.getWinner());
            }
            setPrevLink(TEAM1_IDX, mN.t1.getId());
            //Log.d("TournaFixtureDBEntry", "mN.t1.isLeaf else: " + toString());
        }

        if(null==mN.t2) {
            //Log.d("TournaFixtureDBEntry", "++ T2 is null ++");
        } else if(mN.t2.isExternalLink()) {
            //team2 node is an external link
            if(!mN.t2.getWinner().isEmpty()) {
                setTeam(TEAM2_IDX, mN.t2.getWinner());
            } else {
                setTeam(TEAM2_IDX, mN.t2.getExtMatchIdStr());
            }

            //if previous link is not set, then vertical lines will not be drawn
            //between EXTERNALLEAF and regular NODE (with teams linking to EXTERNALLEAF nodes) in the same round.
            setPrevLink(TEAM2_IDX, mN.t2.getId());
            //Log.d("TournaFixtureDBEntry", "mN.t2.isExternalLink: " + toString());
        } else if(mN.t2.isLeaf()) {
            if(!mN.t2.getWinner().isEmpty()) {
                setTeam(TEAM2_IDX, mN.t2.getWinner());
            } else setTeam(TEAM2_IDX, mN.t2.getDesc());  //desc has the team name, id has row-matchId
            setPrevLink(TEAM2_IDX, "");
            //this.pr2 = mN.t2.getId();
            //Log.d("TournaFixtureDBEntry", "mN.t2.isLeaf: " + toString());
        } else {
            setTeam(TEAM2_IDX, "");
            if(!mN.t2.getWinner().isEmpty()) {
                setTeam(TEAM2_IDX, mN.t2.getWinner());
            }
            setPrevLink(TEAM2_IDX, mN.t2.getId());
            //Log.d("TournaFixtureDBEntry", "mN.t2.isLeaf else: " + toString());
        }
        if(!mN.getWinner().isEmpty()) setW(mN.getWinner());

        /*
        if(!mN.getExternalLinkDesc().isEmpty()) {
            //carry over whatever is set in external link desc.
            //For the successor of EXTERNALLEAF node, desc is set external fixture label.
            //This helps to display external links properly
            setExtLink(TEAM1_IDX, mN.getExtFixtureLabel(), mN.getExtMatchId(), false);
        }*/
        Log.d("TournaFixtureDBEntry: ", toString());
    }

    public Boolean validTeams() {
       if(T==null) return false;
        return T.size() == 2;
    }

    public List<String> getT() {
        return T;
    }

    public void setT(List<String> t) {
        T = t;
    }

    public List<String> getP() {
        return P;
    }

    public void setP(List<String> p) {
        P = p;
    }

    public List<ExternalLink> getE() {
        return E;
    }

    public void setE(List<ExternalLink> e) {
        E = e;
    }

    public Boolean getF() {  //is it final?
        if(null==F) return false;
        return F;
    }

    public void setF(Boolean f) {
        F = f;
    }

    public void setTeam(final int idx, final String team){
        if(null==T) T = new ArrayList<>();
        for (int i = T.size(); i <= idx; i++) {
            T.add(""); //add to list if not already there
        }
        T.set(idx, team);
    }

    public void setT1(final Boolean junk, final String team){
        setTeam(TEAM1_IDX, team);
    }

    public void setT2(final Boolean junk, final String team){
        setTeam(TEAM2_IDX, team);
    }

    public void setPrevLink(final int idx, final String prev){
        if(null==P) P = new ArrayList<>();
        for (int i = P.size(); i <= idx; i++) {
            P.add(""); //add to list if not already there
        }
        P.set(idx, prev);
    }

    public void setExtLink(final int idx, final String label, final String matchId, final Boolean src){
        if(null==E) E = new ArrayList<>();
        for (int i = E.size(); i <= idx; i++) {
            E.add(null); //add to list if not already there
        }
        E.set(idx, new ExternalLink(label, matchId, src));
    }

    public String getTeam(final int idx){
        if(null==T) return "";
        if(idx > T.size()-1) return "";
        return T.get(idx);
    }

    public String getT1(final Boolean junk) {
        return getTeam(TEAM1_IDX);
    }

    public String getT2(final Boolean junk) {
        return getTeam(TEAM2_IDX);
    }

    public String getPrevLink(final int idx){
        if(null==P) return "";
        if(idx > P.size()-1) return "";
        return P.get(idx);
    }

    public String getPr1(final Boolean junk) {
        return getPrevLink(TEAM1_IDX);
    }

    public String getPr2(final Boolean junk) {
        return getPrevLink(TEAM2_IDX);
    }

    public String getExtLinkLabel(final int idx){
        if(null==E) return "";
        if(idx > E.size()-1) return "";
        return E.get(idx).getL();
    }

    public String getExtLinkMatchId(final int idx){
        if(null==E) return "";
        if(idx > E.size()-1) return "";
        return E.get(idx).getM();
    }

    public Boolean getExtLinkSrcFlag(final int idx){
        if(null==E) return false;
        if(idx > E.size()-1) return false;
        return E.get(idx).getS();
    }

    public String getW() {
        return W;
    }

    public String getLoser(final Boolean junk) {
        if(null==getW() || getW().isEmpty()) return "";
        //Log.d(TAG, "DB:getLoser: " + toString());
        if(getW().equals(getT1(true))) return getT2(true);
        else if(getW().equals(getT2(true))) return getT1(true);
        else return "";
    }

    public void setW(String w) {
        W = w;
    }

    public void setWinnerString() {
        String team1 = getT1(true);
        String team2 = getT2(true);
        if(team1.isEmpty() && team2.isEmpty()) {
            //if t1 & t2 are null, then its a leaf node.
            //NODELEAF (winner is set), BYELEAF or EXTERNALLEAF.
            //Log.d(TAG, "setWinnerString, nothing to do: " + toString());
            return;
        }

        //Log.d(TAG, "setWinnerString:" + team1 + " vs " + team2);
        if(team1.equals(Constants.BYE)) setW(team2);
        else if(team2.equals(Constants.BYE)) setW(team1);

        Log.d(TAG, "setWinnerString: " + toString());
    }

    //public Boolean isBye() ==> adds "bye" to firebase DB
    public Boolean isBye(final boolean junk) {
        //If this is a BYE LEAF NODE, team1 will be set as Bye and team2 will be empty

        if(null!=getW() && getW().equals(Constants.BYE)) return true;
        return getT1(true).equals(Constants.BYE) && getT2(true).equals(Constants.BYE);
        //if(getTeam(TEAM1_IDX).equals(Constants.BYE) && getTeam(TEAM2_IDX).isEmpty())
        //    return true;

    }

    //public Boolean isBye() ==> adds "bye" to firebase DB
    public Boolean oneTeamGettingABye(final boolean junk) {
        //If this is a BYE NODE, one of the teams will be BYE
        return getTeam(TEAM1_IDX).equals(Constants.BYE) || getTeam(TEAM2_IDX).equals(Constants.BYE);
    }

    public Boolean isExternalLink(final int junk) {
        if(E==null) return false;
        return E.size() > 0;
    }

    Boolean isThereAWinner(Boolean junk) {
        return !isEmpty(getW());
    }

    Boolean isEmpty(final String s) {
        if(null==s) return true;
        return s.isEmpty();
    }

    @Override
    public String toString() {
        return "TournaFixtureDBEntry{" +
                "T=" + T +
                ", P=" + P +
                ", E=" + E +
                ", W=" + W + '}';
    }
}
