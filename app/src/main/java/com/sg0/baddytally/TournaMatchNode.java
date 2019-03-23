package com.sg0.baddytally;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TournaMatchNode {
    private static final String TAG = "TournaMatchNode";
    public static final String ROUNDSTR = "R";
    public static final String ROUNDSTR_FULL = "Round";
    public static final String UPPER_PREFIX = "UB";
    private static final String[] LABELS = {"Final", "SF", "QF"};
    private static final String[] LABELS_FULL = {"Final", "SemiFinal", "QuarterFinal"};
    private static final String L_DELIM = "/";
    private String id;
    private String desc;   //this holds the team name for the leaf node or external link name
    private NodeType type;
    public TournaMatchNode t1;
    public TournaMatchNode t2;
    public TournaMatchNode parent;
    private String winner;
    private Integer X;
    private Integer Y;
    private Boolean drawn;

    enum NodeType {
        UNKNOWN,
        INVALID,
        NODE,
        NODELEAF,
        BYELEAF,
        EXTERNALLEAF,
        RESERVED1,
        RESERVED2
    }

    public TournaMatchNode(String desc, TournaMatchNode t1, TournaMatchNode t2, NodeType type) {
        this(desc, t1, t2, type, -1, -1, false);
        if(isLeaf()) {
            setDrawn(true);
        }
        if(type == NodeType.NODELEAF) {
            setWinner(desc);
        }
    }

    public TournaMatchNode(final NodeType nodeType) {
        this("", null, null, NodeType.INVALID);
        if(nodeType == NodeType.UNKNOWN) {
            setAsInvalid();
        } else if (nodeType == NodeType.BYELEAF) {
            setAsBye();
        } else if (nodeType == NodeType.EXTERNALLEAF) {
            setExternalLink("F", "1");  //default values; should be overwritten
        } else if (nodeType == NodeType.RESERVED1) {
            setAsReserved(NodeType.RESERVED1);
        } else if (nodeType == NodeType.RESERVED2) {
            setAsReserved(NodeType.RESERVED2);
        }
    }

    public TournaMatchNode(final TournaMatchNode o) {
        this(o.desc, o.t1, o.t2, o.type, o.X, o.Y, o.drawn);
        setWinner(o.getWinner());
        setId(o.getId());
        parent = o.parent;
    }

    public TournaMatchNode(String desc, NodeType type) {
        this(desc, null, null, type);
    }

    public TournaMatchNode(String desc, TournaMatchNode t1, TournaMatchNode t2, NodeType type, Integer x, Integer y, Boolean d) {
        this.id = "";
        this.desc = desc;
        this.type = type;
        this.t1 = t1;
        this.t2 = t2;
        this.parent = null;
        this.winner = "";
        this.X = x;
        this.Y = y;
        this.drawn = d;
    }




    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public Boolean isLeaf() {
        return this.type == NodeType.NODELEAF ||
                this.type == NodeType.BYELEAF ||
                this.type == NodeType.EXTERNALLEAF;
    }

    public void setAsBye() {
        this.desc = Constants.BYE;
        setWinner(Constants.BYE);
        this.t1 = null;
        this.t2 = null;
        this.type = NodeType.BYELEAF;
    }

    public Boolean isExternalLink() {
        return this.type == NodeType.EXTERNALLEAF;
    }

    public void setExternalLink(final String fixtureName, final String matchId) {
        this.desc = fixtureName+ L_DELIM +matchId;
        this.t1 = null;
        this.t2 = null;
        this.type = NodeType.EXTERNALLEAF;
    }

    public void setExternalLinkDesc(final String extLinkDesc) {
        setDesc(extLinkDesc);
    }

    public String getExternalLinkDesc() {
        if(getDesc().contains(L_DELIM)) return getDesc();
        return "";
    }

    public String getExtFixtureLabel() {
        if(!isExternalLink()) return "";
        String parts[] = getDesc().split(L_DELIM);
        if(parts.length==2) {
            return parts[0];
        }
        return "";
    }

    public String getExtMatchId() {
        if(!isExternalLink()) return "";
        String parts[] = getDesc().split(L_DELIM);
        if(parts.length==2) {
            return parts[1];
        }
        return "";
    }

    public String getExtMatchIdStr() {
        if(!isExternalLink()) return "";
        String parts[] = getDesc().split(L_DELIM);
        if(parts.length==2) {
            return parts[1] + Constants.DE_EXTLINK_INDICATOR;
        }
        return "";
    }

    public void setAsInvalid() {
        this.desc = "";
        this.t1 = null;
        this.t2 = null;
        this.type = NodeType.INVALID;
    }

    public void setAsReserved(final NodeType nodeType) {
        this.desc = "";
        this.t1 = null;
        this.t2 = null;
        this.type = nodeType;
        setDrawn(true);
    }

    public Boolean isNull() {
        return t1 == null || t2 == null;
    }

    public Boolean isBye() {
        //If the winner has been set to BYE, irrespective of whether this is
        //BYELEAF or EXTERNALLEAF, consider this as a BYE.
        if(winner.equals(Constants.BYE)) return true;

        return this.type == NodeType.BYELEAF;
    }



    public Boolean winnerGettingBye() {
        //t1 & t2 must be valid
        if(t1!=null && t2!=null) {
            return t1.isBye() || t2.isBye();
        }
        return false;
    }

    public Boolean isReserved() {
        return this.type == NodeType.RESERVED1 || this.type == NodeType.RESERVED2;
    }

    public Boolean isReserved1() {
        return this.type == NodeType.RESERVED1;
    }

    public Boolean isReserved2() {
        return this.type == NodeType.RESERVED2;
    }

    public Boolean isValid() {
        return getNodeStatus() != NodeType.UNKNOWN;
    }

    public Integer getX() {
        return X;
    }

    public void setX(final Integer x) {
        X = x;
    }

    public Integer getY() {
        return Y;
    }

    public void setY(final Integer y) {
        Y = y;
    }

    public void setCoordinates(final Integer x, final Integer y) {
        setX(x);
        setY(y);
    }

    public Boolean getDrawn() {
        return drawn;
    }

    public void setDrawn(Boolean drawn) {
        this.drawn = drawn;
    }

    public String getId() {
        return id;
    }

    public String getLowerBracketDesc() {
        if(getDesc().contains(UPPER_PREFIX)) return getDesc();
        else return UPPER_PREFIX +getDesc();
    }

    public String getIdStr() {
        return getId() + "/" + getDesc();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWinner() {
        return winner;
    }

    public String getLoser() {
        if(null==winner || null==t1 || null==t2) return "";
        if(winner.isEmpty()) return "";
        Log.d(TAG, "getLoser: " + toLongString());
        if(winner.equals(t1.getWinner())) return t2.getWinner();
        else if(winner.equals(t2.getWinner())) return t1.getWinner();
        else return "";
    }

    public void setWinner(String winner) {
        if(winner.isEmpty()) return;
        this.winner = winner;
        //Log.e(TAG, "setWinner:" + toLongString());
    }

    public void setWinnerString() {
        if(t1==null && t2==null) {
            //if t1 & t2 are null, then its a leaf node.
            //NODELEAF (winner is set), BYELEAF or EXTERNALLEAF.
            Log.d(TAG, "setWinnerString: NULL: " + toLongString());
            return;
        }

        if(t1==null || t2==null) {
            //if not a leaf node, both t1 and t2 should be valid. Shouldn't be here.
            Log.d(TAG, "setWinnerString(t1==null || t2==null): " + toLongString());
            return;
        }

        Log.d(TAG, "setWinnerString:" + t1.toLongString() + " vs " + t2.toLongString());
        //if(t1.isBye() && !t2.isBye()) setWinner(t2.winner);
        //else if(!t1.isBye() && t2.isBye()) setWinner(t1.winner);

        if(t1.isBye()) {
            setWinner(t2.winner);
        }
        //set this even if T2 is bye. Depending on the no:of teams, a bye team could
        //play another bye team (in LB). In that case, we need to set winner as Bye.
        else if(t2.isBye()) setWinner(t1.winner);

        //setWinnerString:[: (-1,-1)=fixU/1-3,EXTERNALLEAF,NULL,NULL,(bye)(W),false] vs [: (-1,-1)=fixU/1-4,EXTERNALLEAF,NULL,NULL,(bye)(W),false]
        //setWinnerString: [: (-1,-1)=fixU,NODE,/fixU/1-3,/fixU/1-4,(W),false]
        //createRegularMatchesForThisRound - Adding: [: (-1,-1)=fixU,NODE,/fixU/1-3,/fixU/1-4,(W),false]
        //SGO: To handle above case of 2 EXTERNALLEAF's (which are BYEs as well)
        //isBye() is updated so that it returns true if its winner is already set as BYE.

        Log.d(TAG, "setWinnerString: " + toLongString());
    }

    String getTeam1() {
        if(isLeaf()) return "";
        //If not leaf, t1 and t2 should not be null
        if(t1==null) return "";
        return t1.getWinner().isEmpty() ? "Winner of " + t1.getId() : t1.getWinner();
    }


    String getTeam2() {
        if(isLeaf()) return "";
        //If not leaf, t1 and t2 should not be null
        if(t2==null) return "";
        if(t2.isBye()) return "";
        return t2.getWinner().isEmpty() ? "Winner of " + t2.getId() : t2.getWinner();
    }


    public NodeType getNodeStatus() {
        if (isBye()) {
            return NodeType.BYELEAF;
        } else if (isLeaf()) {
            if(t1==null && t2==null && !desc.isEmpty()) return NodeType.NODELEAF;
            else return NodeType.UNKNOWN;
        } else {
            if(t1==null || t2==null) {
                return NodeType.UNKNOWN;
            } else {
                return NodeType.NODE;
            }
        }
    }

    public static Boolean areTheseLeafNodes(final List<TournaMatchNode> matches) {
        if(matches==null) {
            Log.e(TAG, "areTheseLeafNodes: NULL matches???");
            return true;  //should never happen!
        }

        //Are *all* the nodes in this list are leaf nodes?
        Boolean notLeaf = false;
        for(TournaMatchNode m: matches) {
            if(!m.isLeaf()) return false;
        }
        //Log.d(TAG, "areTheseLeafNodes: TRUE");
        return true;
    }

    public static Boolean areTheseNullNodes(final List<TournaMatchNode> matches) {
        if(matches==null) {
            Log.e(TAG, "areTheseNullNodes: NULL matches???");
            return true;  //should never happen!
        }

        //are all the nodes in the list
        for(TournaMatchNode m: matches) {
            if(!m.isNull()) return false;
        }
        //Log.d(TAG, "areTheseNullNodes: TRUE");
        return true;
    }

    public static List<TournaMatchNode> getPreviousRoundMatches(final List<TournaMatchNode> matches) {
        if(null==matches || matches.size()==0) return null;
        List<TournaMatchNode> tmpMatches = new ArrayList<> ();
        for(TournaMatchNode m: matches) {
            //if(m.isLeaf()) continue;
            if(null!=m.t1) tmpMatches.add(m.t1);
            if(null!=m.t2) tmpMatches.add(m.t2);
        }
        //Log.d(TAG, "getPreviousRoundMatches: in="  + matches.size() + ",out=" + tmpMatches.size());
        //Log.d(TAG, "getPreviousRoundMatches: in="  + matches.toString());
        //Log.d(TAG, "getPreviousRoundMatches: out="  + tmpMatches.toString());
        return tmpMatches;
    }

    public static Integer getNumOfRounds(final List<TournaMatchNode> matches) {
        if(matches==null) return 0;

        Integer rounds = 0;
        //if(matches.size()>0) rounds++;  //at least final round
        //else return 0;

        List<TournaMatchNode> tmpMatches = matches;
        while(!areTheseLeafNodes(tmpMatches)) {  //leaf nodes are nothing but the teams themselves.
            rounds++;
            tmpMatches = getPreviousRoundMatches(tmpMatches);
            if (tmpMatches == null) break;
        }
        Log.d(TAG, "getNumOfRounds: in="  + matches.size() + ", rounds=" + rounds);
        return rounds;
    }

    public static String getIDName(final Integer reversedRoundNum) {
        //Finals is at index 0.
        if(reversedRoundNum == 0) {
            return LABELS[reversedRoundNum];  //Finals
        }
        if(reversedRoundNum < LABELS.length) {
            return LABELS[reversedRoundNum]+"-";
        }
        return ROUNDSTR+reversedRoundNum+"-";
    }

    public static void nameRounds(final List<TournaMatchNode> matches, final Boolean lowerBracket) {
        if(matches==null) return;

        Integer MAX_ROUNDS = getNumOfRounds(matches);
        List<TournaMatchNode>  roundMatches = matches;
        Integer rounds = MAX_ROUNDS;
        while(roundMatches.size()>0) {
            int count=1;
            Log.d(TAG, rounds + ") nameRounds: " + roundMatches.toString());
            for(TournaMatchNode pM: roundMatches) {
                pM.setId(rounds+"-"+count);
                count++;
            }
            rounds--;
            roundMatches = getPreviousRoundMatches(roundMatches);
            if (roundMatches == null) break;
        }
        Log.d(TAG, "nameRounds: " + TournaMatchNode.toString(matches.get(0)));
    }

    public static void setWinnerStrings(List<TournaMatchNode> matches) {
        do {
            for (TournaMatchNode node : matches) {
                node.setWinnerString();
            }
            matches = TournaMatchNode.getPreviousRoundMatches(matches);
            if(matches==null) break;
        }while (!TournaMatchNode.areTheseLeafNodes(matches));

        Log.d(TAG, "setWinnerStrings ==> " + matches.toString());
    }

    public static List<TournaMatchNode> getMatchesForThisRound(List<TournaMatchNode> matches, final Integer round) {
        String roundName = round + "-";
        List<TournaMatchNode> retList = new ArrayList<>();
        if(matches==null) return retList;
        do {
            for (TournaMatchNode node : matches) {
                //if(node.getDesc().startsWith(roundName)) {
                if(node.getId().startsWith(roundName)) {
                    retList.add(new TournaMatchNode(node));
                } else {
                    //Log.d(TAG, "SGO NO contain:" + node.toString());
                    continue;
                    //break;  //if it doesnt meet the criteria for 1 match in this list, it wont for any.
                }
            }
            if(retList.size()>0) break; //found
            matches = TournaMatchNode.getPreviousRoundMatches(matches);
            if(matches==null) break;
        }while (matches.size()>0);
        return retList;
    }

    public static TournaMatchNode getTheMatch(List<TournaMatchNode> matches, final String roundName) {
        do {
            for (TournaMatchNode node : matches) {
                //if(node.getDesc().equals(roundName)) {
                if(node.getId().equals(roundName)) {
                    return node;
                }
            }
            matches = TournaMatchNode.getPreviousRoundMatches(matches);
            if(matches==null) break;
        }while (!TournaMatchNode.areTheseLeafNodes(matches));
        return null;
    }

    public static String getFinalMatchName(List<TournaMatchNode> matches) {
        if(matches==null || matches.size()==0) return "";
        return ROUNDSTR + TournaMatchNode.getNumOfRounds(matches);
    }

    public static void print(final List<TournaMatchNode> matches) {
        Log.d(TAG, "print: ++++++++++++++++");
        List<TournaMatchNode> tL = matches;
        do {
            for (TournaMatchNode node : tL) {
                Log.d(TAG, node.toLongString());
            }
            tL = TournaMatchNode.getPreviousRoundMatches(tL);
            if(tL==null) break;
        }while (!TournaMatchNode.areTheseLeafNodes(tL));

        //print the leaf nodes too
        for (TournaMatchNode node : tL) {
            Log.d(TAG, node.toLongString());
        }
        //loop until ALL the nodes in the previos round are LEAF nodes.
        //Even if there is a single non-LEAF node, this loop will return nodes.
        //In most cases of LB, there will be a mix of NODE and EXTERNALLEAF nodes.
    }

    @Override
    public String toString() {
        return " [" + getId() + "/" + getDesc() + ":" +
                ((t1==null)?"NULL":t1.getIdStr()) + " v " +
                ((t2==null)?"NULL":t2.getIdStr()) + "] ";

    }

    public String toLongString() {
        return "[" + id + ": (" + X + "," + Y + ")=" +
                desc + "," +
                type + "," +
                ((t1==null)?"NULL":t1.getIdStr()) + "," +
                ((t2==null)?"NULL":t2.getIdStr()) + "," +
                winner + "(W)," +
                getDrawn() +
                ']';
    }

    public static String toString(TournaMatchNode m) {
        if(m.isLeaf()) return m.toString();
        String str = "(";
        if(m.t1!=null) {
            str += toString(m.t1);
        }
        if(m.t2!=null) {
            str += toString(m.t2);
        }
        str += ")";
        return str;
    }




}


