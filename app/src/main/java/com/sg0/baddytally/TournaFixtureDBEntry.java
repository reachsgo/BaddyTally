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


    public TournaFixtureDBEntry() {
        this.t1 = "";
        this.t2 = "";
        this.pr1 = "";
        this.pr2 = "";
        this.W = "";
    }

    public TournaFixtureDBEntry(String t1, String t2, String pr1, String pr2, String w) {
        this.t1 = t1;
        this.t2 = t2;
        this.pr1 = pr1;
        this.pr2 = pr2;
        W = w;
    }

    public TournaFixtureDBEntry(final TournaFixtureDBEntry o) {
        this.t1 = o.t1;
        this.t2 = o.t2;
        this.pr1 = o.pr1;
        this.pr2 = o.pr2;
        this.W = o.W;
    }

    public TournaFixtureDBEntry(final TournaMatchNode mN) {
        //leaf nodes will not reach here
        if(mN.t1.isLeaf()) {
            this.t1 = mN.t1.getDesc();
            this.pr1 = "";
        } else {
            this.t1 = "";
            this.pr1 = mN.t1.getDesc();
        }
        if(mN.t2.isLeaf()) {
            this.t2 = mN.t2.getDesc();
            this.pr2 = "";
        } else {
            this.t2 = "";
            this.pr2 = mN.t2.getDesc();
        }

        this.W = "";
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

    //public Boolean isBye() ==> adds "bye" to firebase DB
    public Boolean isBye(final boolean junk) {
        if(getT1().equals(Constants.BYE) || getT2().equals(Constants.BYE))
            return true;
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
                '}';
    }
}
