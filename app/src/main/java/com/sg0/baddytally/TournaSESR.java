package com.sg0.baddytally;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//Single Elimination Single Round
public class TournaSESR {
    private static final String TAG = "TournaSESR";
    private static final Integer MAXPOWER2 = 8; //2^^8 = 256 teams

    private List<TournaMatchNode> matchList;
    private Boolean complete;
    private String mFixtureLabel;
    private String mExtFixtureLabel;
    private HashMap<String, TournaFixtureDBEntry> mUBFixMap;

    public TournaSESR() {
        matchList = null;
    }

    public TournaSESR(final String label, final String extLabel) {
        matchList = null;
        mFixtureLabel = label;
        mExtFixtureLabel = extLabel;
    }

    public void initMatches(final List<TournaMatchNode> matches) {
        matchList = matches;
        complete = false;
    }

    public Integer getCount() {
        if (matchList == null) return 0;
        return matchList.size();
    }

    private Integer isListcomplete(final List<TournaMatchNode> list) {  //num of elements = 2^^n (1,2,4,8,16..)
        if (complete) return 0;  //dont do the time consuming Math operations if not required.
        int listLen = list.size();
        for (int i = 0; i < MAXPOWER2; i++) {
            Double power2 = Math.pow(2, i);
            if (listLen == power2) {
                complete = true;
                return 0;
            }
            if (power2 > listLen) {
                //return difference to the next 2-to-the-power, so that so many fillers (bye nodes) can be added
                //Double nextpower2 = Math.pow(2, i+1);
                int diff = power2.intValue() - listLen;
                //Log.d(TAG, "isListcomplete: diff=" + diff + " listLen=" + listLen + " power2=" + power2);
                return diff;
            }
        }
        return -1;
    }

    private Boolean makeListComplete() {
        Integer retVal = isListcomplete(matchList);
        if (retVal < 0) {
            Log.d(TAG, "makeListComplete: Internal error:" + retVal);
            return false;
        }
        if (retVal == 0) return true;

        for (int i = 0; i < retVal; i++)
            matchList.add(new TournaMatchNode(TournaMatchNode.NodeType.BYELEAF));  //append bye nodes to make the list 2^^n

        return true;
    }

    //T1 vs T2, T3 vs T4, ... etc
    public List<TournaMatchNode> createRegularMatchesForThisRound(final List<TournaMatchNode> matches,
                                                                  Boolean addByes) {
        final String FNAME = ":createRegularMatchesForThisRound - ";
        if (matches != null) initMatches(matches);
        if (null == matchList) return null;
        //Log.d(TAG, FNAME + " size=" + matchList.size());
        if (matchList.size() == 1)
            return matchList;  //only one node in this round. Will hit in lower bracket, when
        //there are many BYES in first round of upper bracket. Ex: 9 teams
        Integer listLen = matchList.size();
        List<TournaMatchNode> tmpList = new ArrayList<>();
        for (int i = 0; i < listLen; i += 2) {
            TournaMatchNode team1Node = matchList.get(i);
            TournaMatchNode team2Node = matchList.get(i + 1);
            TournaMatchNode t = new TournaMatchNode("", team1Node, team2Node,
                    TournaMatchNode.NodeType.NODE);
            if (team1Node.isExternalLink()) t.setExternalLinkDesc(team1Node.getExtFixtureLabel());
            else if (team2Node.isExternalLink())
                t.setExternalLinkDesc(team2Node.getExtFixtureLabel());
            t.setWinnerString();
            tmpList.add(t);
            //Log.d(TAG, FNAME + "Adding: " + t.toLongString());
        }
        return tmpList;
    }

    //T1 vs T16, T2 vs T15, ....
    public List<TournaMatchNode> createMatchesForThisRound(final List<TournaMatchNode> matches,
                                                           Boolean addByes) {
        final String FNAME = ":createMatchesForThisRound - ";
        if (matches != null) initMatches(matches);
        if (null == matchList) return null;

        if (matchList.size() == 1)
            return matchList;  //only one node in this round. Will hit in lower bracket, when
        //there are many BYES in first round of upper bracket. Ex: 9 teams

        if (addByes) {
            Integer before = matchList.size();
            //Log.d(TAG, "createMatchesForThisRound: BEFORE = " + matchList.toString());
            if (!makeListComplete()) return null;
            //Log.d(TAG, "createMatchesForThisRound: AFTER = " + matchList.toString());
            //Log.d(TAG, FNAME + " before=" + before + " after=" + matchList.size());
            Integer listLen = matchList.size();
            if (listLen != 1 && listLen % 2 != 0) {
                Log.e(TAG, FNAME + "internal error:" + listLen);
                return null;
            }
        }

        /*
        Integer listLen = matchList.size();
        if(listLen%2 ==1 ) {
            //teams not exactly divisible by 2. so, there is an extra team in this round.
            matchList.add(new TournaMatchNode(TournaMatchNode.NodeType.BYELEAF));
            Log.d(TAG, FNAME + "Adding last node to matchList: " + matchList.toString());
        }*/

        Integer listLen = matchList.size();
        List<TournaMatchNode> tmpList = new ArrayList<>();
        int i = 0;
        for (int j = listLen - 1; i < listLen / 2; i++, j--) {
            TournaMatchNode t = new TournaMatchNode("",
                    matchList.get(i), matchList.get(j),
                    TournaMatchNode.NodeType.NODE);
            t.setWinnerString();
            tmpList.add(t);
            //Log.d(TAG, FNAME + "Adding: " + t.toLongString());
            //createMatchesForThisRound - Adding: [: (-1,-1)=,NODE,/fixU/2-1,/fixU,(W),false]
            //   where "new8" is t1.getDesc() and "new9" is t2.getDesc()
            //   desc is set to team name only for leafnode.
        }
        return tmpList;
    }

    public HashMap<String, TournaFixtureDBEntry> createFixture(final List<TournaMatchNode> matchTree) {
        Integer MAX_ROUNDS = TournaMatchNode.getNumOfRounds(matchTree);
        //Log.d(TAG, "createFixture: MAX_ROUNDS=" + MAX_ROUNDS + " tree=" + matchTree.toString());
        HashMap<String, TournaFixtureDBEntry> fixtureMap = new HashMap<>();
        for (int i = 0; i <= MAX_ROUNDS; i++) {
            List<TournaMatchNode> m = TournaMatchNode.getMatchesForThisRound(matchTree, i);
            //Log.d(TAG, i + "createFixture ==> " + m.toString());
            for (TournaMatchNode node : m) {
                //if(node.isLeaf()) continue;
                //if(node.t1==null || node.t2==null) continue;
                //node.setWinnerString();
                TournaFixtureDBEntry dbEntry = new TournaFixtureDBEntry(node);
                if (i == MAX_ROUNDS) dbEntry.setF(true); //This is the final match.
                fixtureMap.put(node.getId(), dbEntry);
            }
            //Log.d(TAG, i + "createFixture fixtureMap ==> " + fixtureMap.toString());
        }
        return fixtureMap;
    }

    public List<TournaMatchNode> createSEFixtureTree(ArrayList<String> seededTeamList) {
        List<TournaMatchNode> matches = new ArrayList<>();
        for (String team : seededTeamList) {
            matches.add(new TournaMatchNode(team, null, null, TournaMatchNode.NodeType.NODELEAF));
        }

        //List<TournaMatchNode> matches = new ArrayList<>(seededTeamList);
        //Log.d(TAG, "createFixture: matches.size=" + matches.size() + " " + matches.toString());
        Integer totalTeamCount = 0;  //including fillers for byes
        //TournaSESR sesr = new TournaSESR();
        initMatches(matches);
        while (matches.size() > 1) {
            matches = createMatchesForThisRound(matches, true);
            if (matches == null) break;
            if (totalTeamCount == 0) totalTeamCount = getCount();
        }
        Log.d(TAG, "createFixture: totalTeamCount=" + totalTeamCount);
        //Log.d(TAG, "createFixture: ==> " + TournaMatchNode.toString(matches.get(0)));
        //Log.d(TAG, "createFixture: getNumOfRounds ==> " + TournaMatchNode.getNumOfRounds(matches));
        TournaMatchNode.nameRounds(matches, false);
        //Log.d(TAG, "createFixture: nameRounds ==> " + matches.toString());
        TournaMatchNode.print(matches);

        Integer MAX_ROUNDS = TournaMatchNode.getNumOfRounds(matches);
        for (int i = 1; i <= MAX_ROUNDS; i++) {
            List<TournaMatchNode> m = TournaMatchNode.getMatchesForThisRound(matches, i);
            //Log.d(TAG, "createFixtureTree: ==> " + m.toString());
        }

        return matches;
    }

    public HashMap<String, TournaFixtureDBEntry> createSEFixture(final ArrayList<String> seededTeamList) {
        return createFixture(createSEFixtureTree(seededTeamList));
    }

    public List<TournaMatchNode> createDEFixtureTree(final List<TournaMatchNode> ubMatches, String upperFixtureLabel) {
        final String FUNC = "createDEFixtureTree: ";

        //Log.e(TAG, " ================== LOWER BRACKET ================");

        List<TournaMatchNode> lowerMatches = new ArrayList<>();  //lower bracket matches
        Integer MAX_ROUNDS = TournaMatchNode.getNumOfRounds(ubMatches);
        Boolean pickLowerHalfFirst = false;
        for (int i = 1; i <= MAX_ROUNDS; i++) {
            List<TournaMatchNode> m = TournaMatchNode.getMatchesForThisRound(ubMatches, i);
            lowerMatches = seedLowerRound(lowerMatches, m, pickLowerHalfFirst, upperFixtureLabel, true);
            pickLowerHalfFirst = !pickLowerHalfFirst;
            lowerMatches = createRegularMatchesForThisRound(lowerMatches, false);
            if (lowerMatches == null) return null;
            while(m.size() == lowerMatches.size()) {
                //Log.d(TAG, "createDEFixtureTree: ONE MORE ROUND:" + m.size() + " lower=" + lowerMatches.size());
                lowerMatches = createRegularMatchesForThisRound(lowerMatches, false);
                if (lowerMatches == null) return null;
                if(lowerMatches.size()==1) break;
            }
            //Log.d(TAG, FUNC + i+"==> " + lowerMatches.toString());
            TournaMatchNode.print(lowerMatches);
        }

        //Log.e(TAG, FUNC + lowerMatches.size() + " lowerMatches1=" + lowerMatches.toString());

        //Log.d(TAG, FUNC + "==> " + TournaMatchNode.toString(lowerMatches.get(0)));
        //Log.e(TAG, FUNC + "getNumOfRounds ==> " + TournaMatchNode.getNumOfRounds(lowerMatches));
        TournaMatchNode.nameRounds(lowerMatches, false);
        //Log.d(TAG, FUNC + "nameRounds ==> " + lowerMatches.toString());
        TournaMatchNode.print(lowerMatches);

        return lowerMatches;
    }

    public HashMap<String, TournaFixtureDBEntry> createDEFixture(final List<TournaMatchNode> ubMatches, String upperFixtureLabel) {
        return createFixture(createDEFixtureTree(ubMatches, upperFixtureLabel));
    }

    public List<TournaMatchNode> seedLowerRound(final List<TournaMatchNode> lowerMatches,
                                                final List<TournaMatchNode> upperMatches,
                                                final Boolean pickLowerHalfFirst,
                                                final String upperFixtureLabel,
                                                final Boolean interlace) {
        //Log.e(TAG, "seedLowerRound --start: LOW=" + lowerMatches.toString() +
        //        " \n UPPER=" + upperMatches.toString() +
        //       "\n pickLowerHalfFirst=" + pickLowerHalfFirst + " interlace=" + interlace);
        if (upperMatches.size() == 0) {
            return lowerMatches;
        }

        if (upperMatches.size() == 1) { //Final match of UB
            TournaMatchNode t = new TournaMatchNode(TournaMatchNode.NodeType.EXTERNALLEAF);
            t.setExternalLink(upperFixtureLabel, upperMatches.get(0).getId());
            lowerMatches.add(0, t);
            //Log.d(TAG, "seedLowerRound:  --returning:" + lowerMatches.toString());
            return lowerMatches;
        }


        List<TournaMatchNode> upperM = new ArrayList<>(upperMatches);
        if (pickLowerHalfFirst) {
            upperM.clear();
            for (int i = upperMatches.size() / 2; i < upperMatches.size(); i++) {
                upperM.add(upperMatches.get(i));
            }
            for (int i = 0; i < upperMatches.size() / 2; i++) {
                upperM.add(upperMatches.get(i));
            }
        }

        //Log.d(TAG, "seedLowerRound2: LOW=" + lowerMatches.size() +
        //        " \n UPPER=" + upperM.size());
        //Log.d(TAG, "seedLowerRound2: LOW=" + lowerMatches.toString() +
        //        " \n UPPER=" + upperM.toString());

        List<TournaMatchNode> retList = new ArrayList<>();
        for (int i = 0; i < upperM.size(); i++) {
            TournaMatchNode t = new TournaMatchNode(TournaMatchNode.NodeType.EXTERNALLEAF);
            t.setExternalLink(upperFixtureLabel, upperM.get(i).getId());
            //if UB match loser is already known (say BYE), set it here.
            //While creating fixture, UB match result is already known only in case of a BYE.
            //In this case, Winner is already set as BYE. This needs to be done, as there will not be a
            //EnterScore activity for the UB match which already has a BYE. Thus, if the winner is not
            //set before the starting to enter scores, this LB match team name will never be set to
            //loser of the UB match (which is BYE here)
            t.setWinner(upperM.get(i).getLoser());
            retList.add(t);
            //Log.d(TAG, "seedLowerRound(" + i + ") t=" + t.toLongString() +
            //        " upperM=" + upperM.get(i).toLongString());
            if (interlace && lowerMatches.size() > i) {
                //lowerMatches.get(i).setWinnerString();
                retList.add(lowerMatches.get(i));
            }
        }

        if (!interlace) retList.addAll(lowerMatches);

        //Log.d(TAG, "seedLowerRound:  --returning:" + retList.toString());
        return retList;

    }
}
