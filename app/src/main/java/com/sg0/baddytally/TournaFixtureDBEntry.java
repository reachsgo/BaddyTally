package com.sg0.baddytally;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
class TournaFixtureDBEntry {
    //shorter names will save firebase DB space
    private String t1;
    private String t2;
    private String pr1;
    private String pr2;
    private String W;
    private String ext;


    public TournaFixtureDBEntry() {
        this.t1 = "";
        this.t2 = "";
        this.pr1 = "";
        this.pr2 = "";
        this.W = "";
        this.ext = "";
    }

    public TournaFixtureDBEntry(String t1, String t2, String pr1, String pr2, String w) {
        this.t1 = t1;
        this.t2 = t2;
        this.pr1 = pr1;
        this.pr2 = pr2;
        W = w;
        this.ext = "";
    }

    public TournaFixtureDBEntry(final TournaFixtureDBEntry o) {
        this.t1 = o.t1;
        this.t2 = o.t2;
        this.pr1 = o.pr1;
        this.pr2 = o.pr2;
        this.W = o.W;
        this.ext = o.ext;
    }

    public TournaFixtureDBEntry(final TournaMatchNode mN) {

        if(mN.isExternalLink()) {
            //If a leaf node,
            setExternalLink(mN.getDesc(), true);
            this.t1 = mN.getExtMatchIdStr();
            Log.d("TournaFixtureDBEntry", "isExternalLink: " + toString());
            return;
        } else if(mN.isBye()) {
            this.t1 = Constants.BYE;
            this.pr1 = "";
            this.t2 = "";
            this.pr2 = "";
            this.W = "";
            this.ext = "";
            Log.d("TournaFixtureDBEntry", "isBye: " + toString());
            return;
        } else if(mN.isLeaf()) {
            Log.d("TournaFixtureDBEntry", "++isLeaf:++");
        }

        if(mN.t1.isExternalLink()) {
            //team1 node is an external link
            this.t1 = mN.t1.getExtMatchIdStr();
            //if previous link is not set, then vertical lines will not be drawn
            //between EXTERNALLEAF and regular NODE (with teams linking to EXTERNALLEAF nodes) in the same round.
            this.pr1 = mN.t1.getId();
            Log.d("TournaFixtureDBEntry", "mN.t1.isExternalLink: " + toString());
        } else if(mN.t1.isLeaf()) {
            this.t1 = mN.t1.getDesc();   //desc has the team name, id has row-matchId
            this.pr1 = "";
            Log.d("TournaFixtureDBEntry", "mN.t1.isLeaf: " + toString());
            //this.pr1 = mN.t1.getId();
        } else {
            this.t1 = "";
            this.pr1 = mN.t1.getId();
            Log.d("TournaFixtureDBEntry", "mN.t1.isLeaf else: " + toString());
        }

        if(mN.t2.isExternalLink()) {
            //team2 node is an external link
            this.t2 = mN.t2.getExtMatchIdStr();
            //if previous link is not set, then vertical lines will not be drawn
            //between EXTERNALLEAF and regular NODE (with teams linking to EXTERNALLEAF nodes) in the same round.
            this.pr2 = mN.t2.getId();
            Log.d("TournaFixtureDBEntry", "mN.t2.isExternalLink: " + toString());
        } else if(mN.t2.isLeaf()) {
            this.t2 = mN.t2.getDesc();  //desc has the team name, id has row-matchId
            this.pr2 = "";
            //this.pr2 = mN.t2.getId();
            Log.d("TournaFixtureDBEntry", "mN.t2.isLeaf: " + toString());
        } else {
            this.t2 = "";
            this.pr2 = mN.t2.getId();
            Log.d("TournaFixtureDBEntry", "mN.t2.isLeaf else: " + toString());
        }

        this.W = "";
        this.ext = mN.getExternalLinkDesc();
        //carry over whatever is set in external link desc.
        //For the successor of EXTERNALLEAF node, desc is set external fixture label.
        //This helps to display external links properly

    }

    public String getT1() {
        return t1;
    }

    public void setT1(String t1) {
        this.t1 = t1;
    }

    public String getT2() {
        return t2;
    }

    public void setT2(String t2) {
        this.t2 = t2;
    }

    public String getPr1() {
        return pr1;
    }

    public void setPr1(String pr1) {
        this.pr1 = pr1;
    }

    public String getPr2() {
        return pr2;
    }

    public void setPr2(String pr2) {
        this.pr2 = pr2;
    }

    public String getW() {
        return W;
    }

    public void setW(String w) {
        W = w;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getExtFixtureLabel(final boolean junk) {
        return getExt();
    }

    //public Boolean isBye() ==> adds "bye" to firebase DB
    public Boolean isBye(final boolean junk) {
        if(getT1().equals(Constants.BYE) && getT2().isEmpty())
            return true;
        if(getT2().equals(Constants.BYE) && getT1().isEmpty())
            return true;
        return false;
    }

    //public Boolean isBye() ==> adds "bye" to firebase DB
    public Boolean oneTeamGettingABye(final boolean junk) {
        if(getT1().equals(Constants.BYE) || getT2().equals(Constants.BYE))
            return true;
        return false;
    }

    //junk is a dummy param added so that firebase doesnt add another attribute to DB
    public void setExternalLink(final String desc, final boolean junk) {
        setExt(desc); //fixtureName+"_"+matchId
        this.t1 = "";
        this.pr1 = "";
        this.t2 = Constants.BYE;
        this.pr2 = "";
        this.W = "";
    }

    public Boolean isExternalLink(final String fixtureName, final String matchId) {
        if(getExt().equals(fixtureName + "_" + matchId)) return true;
        return false;
    }

    @Override
    public String toString() {
        return "FixtureDBEntry{" +
                "t1='" + t1 + '\'' +
                ", t2='" + t2 + '\'' +
                ", pr1='" + pr1 + '\'' +
                ", pr2='" + pr2 + '\'' +
                ", W='" + W + '\'' +
                ", ext='" + ext + '\'' +
                '}';
    }
}
