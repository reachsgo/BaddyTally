package com.sg0.baddytally;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//Base class with the common functionalities needed to EnterData
//Mostly around DB lock & release.

public class BaseEnterData extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    protected static final List<Integer> scoreList = new ArrayList<Integer>() {{
        add(0);         add(1);        add(2);
        add(3);        add(4);        add(5);
        add(6);        add(7);        add(8);
        add(9);        add(10);        add(11);
        add(12);        add(13);        add(14);
        add(15);        add(16);        add(17);
        add(18);        add(19);        add(20);
        add(21);        add(22);        add(23);
        add(24);        add(25);        add(26);
        add(27);        add(28);        add(29);
        add(MAX_POINT); add(INVALID_POINT);
    }};

    protected static int mBestOf = 3;
    protected SharedData mCommon;
    protected Spinner mSpinner_P1;
    protected Spinner mSpinner_P2;
    protected Spinner mSpinner_P3;
    protected Spinner mSpinner_P4;
    protected Spinner mSpinner_T1_1;
    protected Spinner mSpinner_T1_2;
    protected Spinner mSpinner_T1_3;
    protected Spinner mSpinner_T2_1;
    protected Spinner mSpinner_T2_2;
    protected Spinner mSpinner_T2_3;
    protected String mSpinner_P1_selection;
    protected String mSpinner_P2_selection;
    protected String mSpinner_P3_selection;
    protected String mSpinner_P4_selection;
    protected Spinner mSpinner_W;   //winner
    protected String mSpinner_W_selection;
    protected List<String> mSpinner_Teams;  //list to be displayed on "winner" spinner
    protected DatabaseReference mDatabase;
    protected ProgressDialog mProgressDialog;
    protected List<String> mT1_players;
    protected List<String> mT2_players;
    protected ArrayList<GameJournalDBEntry> mGameList;
    protected boolean mSingles;
    protected List<String> mTeams;
    protected String mAlertTitle;
    protected String mAlertMsg;
    protected Boolean mFinishActivity;
    protected Handler mMainHandler;
    protected Integer mDBLockCount;
    protected String mType;
    protected boolean mGamesReadFromDB;
    protected boolean mDeleteMS; //delete Match Set

    protected static final int INVALID_POINT = -9999;
    protected static final int MAX_POINT = 30;
    private static final String TAG = "BaseEnterData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //NOTE: setContentView is expected to be invoked in the derived class.
        //and onCreateBase() should be invoked after that oif those initializations are needed.

        mCommon = SharedData.getInstance();
        if (!mCommon.isPermitted(BaseEnterData.this))
            mCommon.killActivity(BaseEnterData.this, RESULT_OK);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mProgressDialog = null;
        mMainHandler = new Handler();
        mAlertTitle = "";
        mAlertMsg = "";
        mFinishActivity = false;
        mDBLockCount = 0;
        mType = "";
        mGamesReadFromDB = false;
        mDeleteMS = false;
    }

    //OnCreate() is expected to be implemented in the derived class.
    //which will invoke onCreateBase()
    protected void onCreateBase() {
        FloatingActionButton fab = findViewById(R.id.fab_cancel);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG, "onClick: ");
                mCommon.killActivity(BaseEnterData.this, RESULT_OK);
            }
        });
        onCreateExtra();
    }

    protected void initializeSpinners() {

        mCommon.wakeUpDBConnection_profile();

        if(mT1_players.size()==1 || mT2_players.size()==1) mSingles = true;
        else {
            mT1_players.add("");
            mT2_players.add("");
        }

        mSpinner_P1 = findViewById(R.id.spinner_p1);
        mSpinner_P2 = findViewById(R.id.spinner_p2);
        mSpinner_P3 = findViewById(R.id.spinner_p3);
        mSpinner_P4 = findViewById(R.id.spinner_p4);
        mSpinner_T1_1 = findViewById(R.id.score_t1_1);
        mSpinner_T1_2 = findViewById(R.id.score_t1_2);
        mSpinner_T1_3 = findViewById(R.id.score_t1_3);
        mSpinner_T2_1 = findViewById(R.id.score_t2_1);
        mSpinner_T2_2 = findViewById(R.id.score_t2_2);
        mSpinner_T2_3 = findViewById(R.id.score_t2_3);
        mSpinner_P1_selection = "";
        mSpinner_P2_selection = "";
        mSpinner_P3_selection = "";
        mSpinner_P4_selection = "";


        if (mSingles) {
            mSpinner_P2.setVisibility(View.GONE);
            mSpinner_P4.setVisibility(View.GONE);
        }

        final Button trackScores = findViewById(R.id.scoretrack_button);
        trackScores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mTeams.size()<2 || mT1_players.size()==0 || mT2_players.size()==0) {
                    Toast.makeText(BaseEnterData.this, "Teams not known!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                ArrayList<String> results = getGamePoints();
                Intent myIntent = new Intent(BaseEnterData.this, TrackScores.class);
                myIntent.putExtra("team1", mTeams.get(0));
                myIntent.putExtra("team2", mTeams.get(1));
                myIntent.putExtra("t1p1", mSpinner_P1_selection);
                myIntent.putExtra("t1p2", mSpinner_P2_selection);
                myIntent.putExtra("t2p1", mSpinner_P3_selection);
                myIntent.putExtra("t2p2", mSpinner_P4_selection);
                myIntent.putStringArrayListExtra("scores", results);
                BaseEnterData.this.startActivityForResult(myIntent, Constants.TRACKSCORES_ACTIVITY);
            }
        });

        final Button enterButton = findViewById(R.id.enter_button);
        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG, "enterButton.onClick: " +
                //        String.format("%s,%s vs %s,%s", mSpinner_P1_selection, mSpinner_P2_selection,
                //                mSpinner_P3_selection, mSpinner_P4_selection));
                if (mSpinner_P1_selection.isEmpty() || mSpinner_P3_selection.isEmpty()) {
                    Toast.makeText(BaseEnterData.this, "Enter both players...", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mSpinner_P1_selection.equals(mSpinner_P2_selection)) {
                    Toast.makeText(BaseEnterData.this, "Bad data: duplicate entries:" + mSpinner_P1_selection,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mSpinner_P3_selection.equals(mSpinner_P4_selection)) {
                    Toast.makeText(BaseEnterData.this, "Bad data: duplicate entries:" + mSpinner_P3_selection,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                //If P2 is entered, then P4 should also be entered.
                //enterButton.onClick: player 22s,player 22s vs player 2g,
                if ((!mSpinner_P2_selection.isEmpty() && mSpinner_P4_selection.isEmpty()) ||
                        (mSpinner_P2_selection.isEmpty() && !mSpinner_P4_selection.isEmpty())) {
                    Toast.makeText(BaseEnterData.this, "Enter all 4 players or just 2!", Toast.LENGTH_SHORT).show();
                    return;
                }
                mCommon.wakeUpDBConnection();
                enterData(false);
            }
        });


        //Log.w(TAG, "T1 players:" + mT1_players.toString());
        //Log.w(TAG, "T2 players:" + mT2_players.toString());

        List<String> p1List = new ArrayList<>(mT1_players);
        ArrayAdapter<String> dataAdapterP1 = new ArrayAdapter<>(this,
                R.layout.small_spinner, p1List);
        dataAdapterP1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mSpinner_P1.setSelection(0);
        mSpinner_P1.setAdapter(dataAdapterP1);

        List<String> p2List = new ArrayList<>(mT1_players);
        final ArrayAdapter<String> dataAdapterP2 = new ArrayAdapter<>(this,
                R.layout.small_spinner, p2List);
        dataAdapterP2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mSpinner_P2.setSelection(1);
        mSpinner_P2.setAdapter(dataAdapterP2);

        List<String> p3List = new ArrayList<>(mT2_players);
        final ArrayAdapter<String> dataAdapterP3 = new ArrayAdapter<>(this,
                R.layout.small_spinner, p3List);
        dataAdapterP3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mSpinner_P3.setSelection(2);
        mSpinner_P3.setAdapter(dataAdapterP3);

        List<String> p4List = new ArrayList<>(mT2_players);
        final ArrayAdapter<String> dataAdapterP4 = new ArrayAdapter<>(this,
                R.layout.small_spinner, p4List);
        dataAdapterP4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mSpinner_P4.setSelection(3);
        mSpinner_P4.setAdapter(dataAdapterP4);

        //For P1, adjust drop down menus of P2, P3 & P4
        mSpinner_P1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mSpinner_P1_selection = (String) adapterView.getItemAtPosition(position);
                // Notify the selected item text
                //Log.v(TAG, "mSpinner_P1 onItemSelected mSpinner_P1_selection:" + mSpinner_P1_selection);


                //If there are games read from DB, then dont rearrange the players.
                //We just want to show what is read from DB.
                if(mGamesReadFromDB) return;

                mSpinner_P2_selection = "";
                //When P1 is selected, re-arrange P2/3/4 lists to remove P1 selection.
                if (!mSingles) rearrangeDropdownList(mSpinner_P2, dataAdapterP2, mT1_players);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //For P2, adjust drop down menus of P3 & P4.
        //This spinner will not be visible for Singles.
        mSpinner_P2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mSpinner_P2_selection = (String) adapterView.getItemAtPosition(position);
                //Log.v(TAG, "mSpinner_P2 onItemSelected mSpinner_P2_selection:" + mSpinner_P2_selection);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //For P3, adjust drop down menus of P4
        mSpinner_P3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mSpinner_P3_selection = (String) adapterView.getItemAtPosition(position);
                //Log.v(TAG, "mSpinner_P3 onItemSelected mSpinner_P3_selection:" + mSpinner_P3_selection);
                if(mGamesReadFromDB) return;
                mSpinner_P4_selection = "";
                if (!mSingles) rearrangeDropdownList(mSpinner_P4, dataAdapterP4, mT2_players);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        mSpinner_P4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mSpinner_P4_selection = (String) adapterView.getItemAtPosition(position);
                //Log.v(TAG, "mSpinner_P4 onItemSelected mSpinner_P4_selection:" + mSpinner_P4_selection);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        ArrayAdapter<Integer> scoreAdapter = new ArrayAdapter<>(this,
                R.layout.small_spinner, scoreList);
        scoreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner_T1_1.setAdapter(scoreAdapter);
        mSpinner_T1_2.setAdapter(scoreAdapter);
        mSpinner_T1_3.setAdapter(scoreAdapter);
        mSpinner_T2_1.setAdapter(scoreAdapter);
        mSpinner_T2_2.setAdapter(scoreAdapter);
        mSpinner_T2_3.setAdapter(scoreAdapter);
        mSpinner_T1_1.setSelection(0);
        mSpinner_T2_1.setSelection(0);


        mSpinner_T1_1.setOnItemSelectedListener(this);
        mSpinner_T1_2.setOnItemSelectedListener(this);
        mSpinner_T1_3.setOnItemSelectedListener(this);
        mSpinner_T2_1.setOnItemSelectedListener(this);
        mSpinner_T2_2.setOnItemSelectedListener(this);
        mSpinner_T2_3.setOnItemSelectedListener(this);

        Log.d(TAG, "initializeSpinners: done-----");
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        Integer i = (Integer) adapterView.getItemAtPosition(position);
        if(i == INVALID_POINT) {
            //Spinner reserves enough space to display its widest entry.
            //So, in order to keep the drop-down wide enough (makes it easier for user to select),
            //just add a longer integer to the end. If this is selected, its ignored.
            //Log.d(TAG, "onItemSelected: " + INVALID_POINT);
            adapterView.setSelection(0);
            return;
        }

        //If the losing score is entered first, then auto-populate 21 in the other team's spinner.
        if(i>0 && i<21) {
            if(adapterView==mSpinner_T1_1 && (Integer)mSpinner_T2_1.getSelectedItem()==0)
                mSpinner_T2_1.setSelection(21);
            else if(adapterView==mSpinner_T2_1 && (Integer)mSpinner_T1_1.getSelectedItem()==0)
                mSpinner_T1_1.setSelection(21);
            else if(adapterView==mSpinner_T1_2 && (Integer)mSpinner_T2_2.getSelectedItem()==0)
                mSpinner_T2_2.setSelection(21);
            else if(adapterView==mSpinner_T2_2 && (Integer)mSpinner_T1_2.getSelectedItem()==0)
                mSpinner_T1_2.setSelection(21);
            else if(adapterView==mSpinner_T1_3 && (Integer)mSpinner_T2_3.getSelectedItem()==0)
                mSpinner_T2_3.setSelection(21);
            else if(adapterView==mSpinner_T2_3 && (Integer)mSpinner_T1_3.getSelectedItem()==0)
                mSpinner_T1_3.setSelection(21);
        }
        //String s = adapterView.getItemAtPosition(position).toString();
        //Log.d(TAG, "onItemSelected: " + s);
        enterData(true);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    protected Integer getGamePoint(final Spinner s) {
        if (s == null) return 0;
        Integer pt = (Integer) s.getSelectedItem();
        if(pt<0 || pt>MAX_POINT) return 0;
        return pt;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Log.d(TAG, "onActivityResult: " + requestCode + "," + resultCode);
        mCommon.wakeUpDBConnection_profile();
        if(requestCode==Constants.TRACKSCORES_ACTIVITY &&
                (resultCode==RESULT_OK || resultCode==RESULT_CANCELED)){
            //for resultCode=RESULT_CANCELED (when app goes to background while tracking scores), data will be NULL
            //ArrayList<String> results = data.getStringArrayListExtra("gameResults");
            ArrayList<String> results = SharedData.getInstance().mStrList;
            if(null!=results) {
                Log.d(TAG, "onActivityResult: " + results);
                for (int i = 0; i < results.size(); i++) {
                    if (results.get(i).isEmpty()) continue;
                    String[] parts = results.get(i).split("-");
                    if (parts.length != 2) continue;
                    setGamePointSpinner(i + 1,
                            Integer.valueOf(parts[0]),
                            Integer.valueOf(parts[1]));
                }
            } else {
                Log.e(TAG, "onActivityResult: null results");
            }
            //findViewById(R.id.scoretrack_button).setVisibility(View.GONE);
        }

    }

    @Override
    public void onBackPressed() {
        mCommon.killActivity(BaseEnterData.this, RESULT_OK);
    }

    protected void onCreateExtra() {
        Log.d(TAG, "onCreateExtra: ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.v(TAG, "onStart:" + Thread.currentThread().getId());
    }




    protected void matchCompleted() {
        Log.d(TAG, "matchCompleted: ");
        findViewById(R.id.scoretrack_button).setVisibility(View.GONE);
    }

    protected void rearrangeDropdownList(Spinner spinner, ArrayAdapter<String> adapter, List<String> players) {

        //Log.v(TAG, "rearrangeDropdownList:" + mSpinner_P1_selection + "/" + mSpinner_P2_selection + "/"
        //        + mSpinner_P3_selection + "/" + mSpinner_P4_selection);
        adapter.clear();
        //Collections.sort(players);  //sorted already so that players present on the court comes first.
        adapter.addAll(players);

        //For ClubLeague, the player list is the same for all players.
        //For tournament games, p1/p2 are from first team and p2/p4 are from second team.
        boolean singlePool = false;
        if(mType.equals(Constants.CLUBLEAGUE)) singlePool = true;

        if (spinner == mSpinner_P2) {
            //if spinner for player 2, remove player 1 selection & set first in the list
            if (!mSpinner_P1_selection.isEmpty()) adapter.remove(mSpinner_P1_selection);
            //spinner.setSelection(adapter.getCount() - 1);
            spinner.setSelection(0);
            mSpinner_P2_selection = spinner.getItemAtPosition(0).toString();
        } else if (spinner == mSpinner_P3) {
            if(singlePool) {
                //if spinner for player 3, remove player 1 & 2 selections & set first in the list
                if (!mSpinner_P1_selection.isEmpty()) adapter.remove(mSpinner_P1_selection);
                if (!mSpinner_P2_selection.isEmpty()) adapter.remove(mSpinner_P2_selection);
                //spinner.setSelection(adapter.getCount() - 1);
                spinner.setSelection(0);
                mSpinner_P3_selection = spinner.getItemAtPosition(0).toString();
            }
        } else if (spinner == mSpinner_P4) {
            if(singlePool) {
                //if spinner for player 3, remove player 1 & 2 selections & set first in the list
                if (!mSpinner_P1_selection.isEmpty()) adapter.remove(mSpinner_P1_selection);
                if (!mSpinner_P2_selection.isEmpty()) adapter.remove(mSpinner_P2_selection);
            }
            if (!mSpinner_P3_selection.isEmpty()) adapter.remove(mSpinner_P3_selection);
            //spinner.setSelection(adapter.getCount() - 1);
            spinner.setSelection(0);
            mSpinner_P4_selection = spinner.getItemAtPosition(0).toString();
        }
        //Log.i(TAG, "rearrangeDropdownList Done:" + mSpinner_P1_selection + "/" +
        //        mSpinner_P2_selection + "/" + mSpinner_P3_selection + "/" + mSpinner_P4_selection);
        adapter.notifyDataSetChanged();
    }

    protected Spinner getRespectiveSpinner(final int gameNum, final int teamNum) {
        Spinner tmpS = null;
        switch (gameNum) {
            case 1:
                tmpS = teamNum == 1 ? mSpinner_T1_1 : mSpinner_T2_1;
                break;
            case 2:
                tmpS = teamNum == 1 ? mSpinner_T1_2 : mSpinner_T2_2;
                break;
            case 3:
                tmpS = teamNum == 1 ? mSpinner_T1_3 : mSpinner_T2_3;
                break;
            default:
                Log.e(TAG, "getRespectiveSpinner, bad game Num:" + gameNum);
                break;
        }
        return tmpS;
    }

    protected void setGamePointSpinner(final int gameNum, final int t1Score, final int t2Score) {
        //Log.i(TAG, "setGamePointSpinner,case " + gameNum + ": " + t1Score + "/" + t2Score);
        Spinner tmpS = getRespectiveSpinner(gameNum, 1);
        if (tmpS != null) tmpS.setSelection(t1Score);
        tmpS = getRespectiveSpinner(gameNum, 2);
        if (tmpS != null) tmpS.setSelection(t2Score);
        mGamesReadFromDB = true; //Data read from DB is set in the spinners.
    }

    protected ArrayList<String> getGamePoints() {
        ArrayList<String> results = new ArrayList<>(
                Arrays.asList("", "", "")
        );
        //Log.d(TAG, "getGamePoints: " + results.toString());
        for (int i = 1; i <= 3; i++) {
            Integer s1 = 0, s2 = 0;
            Spinner tmpS = getRespectiveSpinner(i, 1);
            if (tmpS != null) s1 = (Integer) tmpS.getSelectedItem();
            tmpS = getRespectiveSpinner(i, 2);
            if (tmpS != null) s2 = (Integer) tmpS.getSelectedItem();
            if(s1==0 && s2==0) continue;
            results.remove(i-1);
            results.add(i-1, s1.toString() + "-" + s2.toString());
        }
        return results;
    }

    protected void setPlayersSpinner(final String t1P1, final String t1P2, final String t2P1, final String t2P2) {
        //Log.d(TAG, "setPlayersSpinner: " + String.format("T1=%s,%s T2=%s,%s", t1P1, t1P2, t2P1, t2P2));
        //Log.d(TAG, "setPlayersSpinner: " + String.format("mT1=%s mT2=%s", mT1_players, mT2_players));
        for(int pos=0; pos < mT1_players.size(); pos++) {
            if(mT1_players.get(pos).equals(t1P1)) {
                mSpinner_P1.setSelection(pos);
                //Log.d(TAG, "setPlayersSpinner: mSpinner_P1.setSelection" + pos);
            }
            else if(mT1_players.get(pos).equals(t1P2)) {
                mSpinner_P2.setSelection(pos);
                //Log.d(TAG, "setPlayersSpinner: mSpinner_P2.setSelection" + pos);
            }
        }
        for(int pos=0; pos < mT2_players.size(); pos++) {
            if(mT2_players.get(pos).equals(t2P1)) mSpinner_P3.setSelection(pos);
            else if(mT2_players.get(pos).equals(t2P2)) mSpinner_P4.setSelection(pos);
        }
    }

    //
    protected void populateGamePoints() {
       //This needs to be invoked after the derived class does it stuff
       //to populate data from DB. this is needed so that, user can make changes
       //to data read from DB and that will cause auto-rearrangement.
        //flag is reset after a delay so that the UI thread has enough time to
        //complete the ongoing callbacks (onItemSelected -> rearrangeDropdownList)
        if(null!=mMainHandler)
            mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mGamesReadFromDB = false;
            }
        }, 1000);
    }

    protected boolean enterData(boolean dry_run) {
        //Log.d(TAG, "enterData: ");
        return true;
    }

    protected Boolean lockAndUpdateDB() {
        if (null != mProgressDialog) return false;   //attempt to press Enter button repeatedly

        if (!mCommon.isDBConnected()) {
            Toast.makeText(BaseEnterData.this, "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
            mCommon.wakeUpDBConnection_profile();
            return false;
        }

        mProgressDialog = new ProgressDialog(BaseEnterData.this);
        mProgressDialog.setMessage("Updating database....");
        mProgressDialog.show();

        //If we were to update for 1 game at a time, there will be overlaps between
        //background processes run by firebase APIs. When 2 reads are done in parallel for the
        //same attribute, they will both get the same initial value and hence the incremented
        //new value of one will be overwritten by the other. DB read/write for the same
        //attribute will have to be done serially.
        //or collect all the data, increment in appl memory and then finally do a single write to DB at the end.

        //To avoid a conflict when 2 users are entering the same score.
        //Without lock, inconsistency is seen: missing journal entry or points are not added.
        SharedData.getInstance().acquireDBLock(mCommon.mTournament);
        waitForDBLock();
        return true;
    }

    //To not block the UI thread, waitForDBLock() is invoked every second by posting
    //it delayed (by 1s) back into the mainhandler of UI thread. If it is found that DB is locked
    //in one of the iterations, the loop is broken. Otherwise a max of 20 loops.
    protected void waitForDBLock() {
        if (!mCommon.isDBLocked()) {
            mDBLockCount++;
            if (mDBLockCount < 20) {  //max of 20s to get DB lock
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        waitForDBLock();
                    }
                }, 1000);
            } else {
                //Log.e(TAG, "waitForDBLock: Failed to acquire lock:" + mDBLockCount);
                mAlertTitle = "";
                Log.e(TAG, "waitForDBLock: Failed to update DB, please refresh and try again later...");
                mAlertMsg = "DB lock held by another user, please try again later...";
                mFinishActivity = true;
                mCommon.setDBUpdated(true);
                releaseLockAndCleanup();
            }
        } else {
            workToUpdateDB();
            //releaseLockAndCleanup() should be invoked from any exit points of workToUpdateDB()
        }
    }

    protected void workToUpdateDB() {
        //Log.d(TAG, "workToUpdateDB: " + mCommon.mClub);
        releaseLockAndCleanup();
    }

    protected void releaseLockAndCleanup() {
        Log.d(TAG, "releaseLockAndCleanup: fin=" + mFinishActivity + ", dbUpd=" + mCommon.isDBUpdated());
        mCommon.releaseDBLock(mCommon.mTournament);
        if (null != mProgressDialog && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;  //whether the activity is in the foreground or not, nullify it.
        //mWorker.quit();

        //it could happen that the user moves this app to background while the background loop is running.
        //In thats case, dialog will fail: "WindowManager$BadTokenException: Unable to add window"
        //So, check if this activity is in foreground before displaying dialogue.
        if (isFinishing()) return;
        if (!ScoreTally.isActivityVisible()) return;

        if (!mAlertTitle.isEmpty() && mAlertMsg.isEmpty()) {
            //Show snackbar
            Snackbar.make(findViewById(R.id.enterdata_ll), mAlertTitle, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            mAlertTitle = "";
            //Give time to show snackbar before closing the activity
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mFinishActivity) mCommon.killActivity(BaseEnterData.this, RESULT_OK);
                }
            }, 1000);
        } else if (!mAlertMsg.isEmpty()) {
            //show dialog
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(BaseEnterData.this);
            alertBuilder.setTitle(mAlertTitle);
            alertBuilder.setMessage(mAlertMsg);
            mAlertMsg = mAlertTitle = "";
            alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (mFinishActivity) mCommon.killActivity(BaseEnterData.this, RESULT_OK);
                }
            });
            alertBuilder.show();
        } else {
            if (mFinishActivity) mCommon.killActivity(BaseEnterData.this, RESULT_OK);
        }
    }

    @Override
    protected void onResume() {
        //Log.d(TAG, "onResume: ");
        super.onResume();
        ScoreTally.activityResumed();
    }

    @Override
    protected void onPause() {
        //Log.d(TAG, "onPause: ");
        super.onPause();
        ScoreTally.activityPaused();
    }

    @Override
    protected void onDestroy() {
        //Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        mMainHandler = null;
    }
}


