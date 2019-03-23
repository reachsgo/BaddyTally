package com.sg0.baddytally;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournaUtil {
    private static final String TAG = "TournaUtil";
    private Activity pActivity;
    private CallbackRoutine cb;
    private SharedData mCommon;
    private List<String> mMSStrList;
    public HashMap<String,MatchInfo> mMSInfoMap;  //meta/<match-key>/info
    public HashMap<String,HashMap<String,Boolean>> mMatchesStatus;  //meta/<match-key>:true/false
    public String mTourna;
    public String mMSStr_chosen;
    public Integer mNumOfMatches;
    public Integer mBestOf;
    private PopupMenu mPopup;

    public TournaUtil(final Activity parentActivity, final CallbackRoutine callback_interface) {
        pActivity = parentActivity;
        cb = callback_interface;
        mCommon = SharedData.getInstance();
        mPopup = null;
    }

    public void fetchActiveTournaments() {
        Log.d(TAG, "fetchActiveTournaments..." + mCommon.mClub);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.TOURNA).child(Constants.ACTIVE);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                HashMap<String, String> tMap = new HashMap<>();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String tournaType = childSnapshot.getValue(String.class);
                    Log.i(TAG, "onDataChange:" + childSnapshot.getKey() + ":" + tournaType);
                    if (null!=tournaType && !tournaType.isEmpty())
                        tMap.put(childSnapshot.getKey(),tournaType);
                }
                mCommon.mTournaMap = tMap;

                if (mCommon.mTournaMap.size() > 0) {
                    Log.i(TAG, "onDataChange, mTournaMap:" + mCommon.mTournaMap.toString());
                    cb.completed(Constants.CB_READTOURNA, true);
                }
                else {
                    mCommon.showToast(pActivity, "No ongoing tournaments!", Toast.LENGTH_SHORT);
                    //return a 0 size mTournaMap so that the invoker can update the DB.
                    cb.completed(Constants.CB_READTOURNA, true);
                    //finish(); //finish here will cause a loop in tournament mode.
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mCommon.showToast(pActivity, "DB error on read: " + databaseError.getMessage(), Toast.LENGTH_SHORT);
                cb.completed(Constants.CB_READTOURNA, false);
            }
        });
    }

    public void showTournaments(final View popupView, final View snackbarView) {
        if(mCommon.mTournaMap.size()==0) {
            Toast.makeText(pActivity, "No tournaments to display", Toast.LENGTH_SHORT).show();
            return;
        }
        if(popupView==null || snackbarView==null) {
            Log.i(TAG, "view is null!!");
            return;
        }
        Log.d(TAG, "showTournaments...");
        if(mPopup!=null) {
            //routine is again invoked before the first one finished. happens if a view is kept open and then phone sleeps;
            //now, when phone wakes, if showTournaments() is again invoked from resume(). there will be 2 popups.
            mPopup.dismiss();
            mPopup = null;
        }
        Context wrapper = new ContextThemeWrapper(pActivity, R.style.RegularPopup);
        mPopup = new PopupMenu(wrapper, popupView);
        mPopup.getMenuInflater().inflate(R.menu.summary_popup_menu, mPopup.getMenu());
        if (Build.VERSION.SDK_INT >= 23) {
            mPopup.setGravity(Gravity.END);
        }
        mPopup.getMenu().clear();
        Menu pMenu = mPopup.getMenu();
        for(Map.Entry<String,String> tourna : mCommon.mTournaMap.entrySet()) {
            pMenu.add(tourna.getKey());
        }
        if(mCommon.mTournaMap.size()>0) {
            Snackbar.make(snackbarView, "Select a tournament", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
        mPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Log.v(TAG, "onMenuItemClick[" + menuItem.getItemId()+ "] :" + menuItem.getTitle().toString());
                mPopup.dismiss();
                mTourna = menuItem.getTitle().toString();
                cb.completed(Constants.CB_SHOWTOURNA, true);
                //showTeamList(mCommon.mTournament);
                return true;
            }
        });
        mPopup.show();//showing popup menu
    }

    public void readDBMatchMeta(final String tourna, final Boolean ignoreDone) {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(tourna).child(Constants.MATCHES).child(Constants.META);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                DataSnapshot ds = dataSnapshot.child(Constants.INFO + "/" + Constants.NUM_OF_MATCHES);
                Integer numOfMatches = ds.getValue(Integer.class);
                if(numOfMatches==null) {
                    Log.e(TAG, "Failed to read Num of matches!");
                    Toast.makeText(pActivity, "DB not configured for matches", Toast.LENGTH_SHORT).show();
                    pActivity.finish();
                    return;
                }
                mNumOfMatches = numOfMatches;
                Log.i(TAG, "readDBMatchMeta Num of Matches = " + mNumOfMatches.toString());

                ds = dataSnapshot.child(Constants.INFO + "/" + Constants.NUM_OF_GAMES);
                Integer bestOf = ds.getValue(Integer.class);
                if(bestOf==null) {
                    Log.e(TAG, "Failed to read 'Best of'!");
                    Toast.makeText(pActivity, "DB not configured for matches", Toast.LENGTH_SHORT).show();
                    pActivity.finish();
                    return;
                }
                mBestOf = bestOf;
                Log.i(TAG, "readDBMatchMeta Best of = " + mBestOf.toString());

                List<String> tmpList = new ArrayList<>();
                HashMap<String,MatchInfo> tmpListInfo = new HashMap<>();
                HashMap<String,HashMap<String,Boolean>> tmpMatchesStatus = new HashMap<>();
                for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                    final String key = childSnapshot.getKey();
                    if(key.equals("info")) continue;   //meta/info
                    Log.i(TAG,   "readDBMatchMeta childSnapshot reading: " + key);
                    HashMap<String,Boolean> tmpStatusMap = new HashMap<>();
                    for (DataSnapshot gcDS: childSnapshot.getChildren()) {
                        final String mKey = gcDS.getKey();
                        if(mKey.equals("info")) { //meta/<match-key>/info  ex: meta/3/info
                            MatchInfo info = gcDS.getValue(MatchInfo.class);
                            if (ignoreDone && info.done) {
                                Log.i(TAG, "Match already completed: " + info.toString());
                                continue;
                            } else {
                                final String match = Constants.MATCHSETID_PREFIX + key + Constants.TEAM_DELIM1 + info.T1 + Constants.TEAM_DELIM2 + info.T2;
                                tmpList.add(match);
                                tmpListInfo.put(key, info);
                                Log.i(TAG, key + ": [" + match + "] added to list");
                            }
                        } else {  //Match entries: get the "done" status and save for later.
                            Boolean status = gcDS.getValue(boolean.class);  //meta/<match-key>:true/false
                            tmpStatusMap.put(mKey, status);
                            Log.i(TAG, mKey + ": [" + status + "] added to status list");
                        }
                    }
                    tmpMatchesStatus.put(key, tmpStatusMap);
                }

                if(tmpList.size()>0) {
                    mMSStrList = tmpList;
                    Log.i(TAG, "onDataChange, mMSStrList:" + mMSStrList.toString());
                    mMSInfoMap = tmpListInfo;
                    mMatchesStatus = tmpMatchesStatus;
                    cb.completed(Constants.CB_READMATCHMETA, true);
                    //showMatches();
                }
                else {
                    mCommon.showToast(pActivity, "No scheduled matches!", Toast.LENGTH_SHORT);
                    cb.completed(Constants.CB_READMATCHMETA, false);
                    //pActivity.finish();
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mCommon.showToast(pActivity, "DB error on read: " + databaseError.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    static public MatchInfo getMatchInfoFromString(final String match) {
        if (match.isEmpty()) {
            Log.i(TAG, "getMatchInfoFromString, match is empty!");
            return null;
        }
        MatchInfo mI = new MatchInfo();
        String[] parts1 = match.split(Constants.TEAM_DELIM1);
        mI.key = parts1[0];
        mI.key = mI.key.replaceAll("\\D+","");  //get only the numbers. ex: 100 from "MS100"
        String[] teams = parts1[1].split(Constants.TEAM_DELIM2);

        if (teams.length != 2) {
            Log.e(TAG, "getMatchInfoFromString, 2 teams were expected:" + teams.toString());
            return null;
        }
        mI.T1 = teams[0];
        mI.T2 = teams[1];
        Log.i(TAG, "getMatchInfoFromString: " + mI.toString());
        return mI;
    }

    public void showMatches(final View v) {
        //final PopupMenu popup = new PopupMenu(pActivity, pActivity.findViewById(R.id.enterdata_header));
        if(mMSStrList.size()==0) return;
        if(mPopup!=null) {
            mPopup.dismiss();
            mPopup = null;
        }
        Context wrapper = new ContextThemeWrapper(pActivity, R.style.RegularPopup);
        mPopup = new PopupMenu(wrapper, v);
        mPopup.getMenuInflater().inflate(R.menu.summary_popup_menu, mPopup.getMenu());
        if (Build.VERSION.SDK_INT >= 23) {
            mPopup.setGravity(Gravity.END);
        }
        mPopup.getMenu().clear();
        Menu pMenu = mPopup.getMenu();
        for(String match : mMSStrList) {  //mMSStrList contains match strings of format "MS20> Team1 vs Team2"
            MatchInfo mInfo = TournaUtil.getMatchInfoFromString(match);
            //this matchInfo constructed from match-string does not have 'done' status in it. so, fetch that one now.
            MatchInfo fullMInfo = mMSInfoMap.get(mInfo.key);
            if(fullMInfo==null) {
                Log.v(TAG, "showMatchGames, matchkey:" + mInfo.key + " got NULL for statusMap");
                pMenu.add(match);   //cant get status now, so just show the unformatted match string
                continue;
            }
            SpannableStringBuilder formattedMatch = new SpannableStringBuilder();
            if(fullMInfo.done) {
                formattedMatch.append(SharedData.getInstance().getColorString(
                        SharedData.getInstance().getStrikethroughString(match), Color.GRAY));
                //Log.v(TAG, "showMatchGames, match:" + match + " is DONE");
            }
            else {
                formattedMatch.append(match);
                //Log.v(TAG, "showMatchGames, match:" + match + " is to be played");
            }
            pMenu.add(formattedMatch);
        }
        mPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Log.v(TAG, "onMenuItemClick: [" + menuItem.getTitle().toString() + "] " + menuItem.getItemId());
                mMSStr_chosen = menuItem.getTitle().toString();
                mPopup.dismiss();
                cb.completed(Constants.CB_SHOWMATCHES, true);
                return true;
            }
        });
        mPopup.show();//showing popup menu
    }

}
