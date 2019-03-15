package com.sg0.baddytally;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
        add(30);
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


    private static final String TAG = "BaseEnterData";
    private String mTournaType;
    private String mMatchId;
    private String mFixtureLabel;
    private TournaFixtureDBEntry mMatchDBEntry;
    private Boolean mViewOnly;


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
    }

    //OnCreate() is expected to be implemented in the derived class.
    //which will invoke onCreateBase()
    protected void onCreateBase() {
        Log.d(TAG, "onCreateBase: ");
        FloatingActionButton fab = findViewById(R.id.fab_cancel);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: ");
                mCommon.killActivity(BaseEnterData.this, RESULT_OK);
            }
        });
        onCreateExtra();
    }

    protected void initializeSpinners() {

        mCommon.wakeUpDBConnection_profile();

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

        final Button enterButton = findViewById(R.id.enter_button);
        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSpinner_P1_selection.isEmpty() || mSpinner_P3_selection.isEmpty()) {
                    Toast.makeText(BaseEnterData.this, "Enter both players...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!mSingles && (mSpinner_P2_selection.isEmpty() || mSpinner_P4_selection.isEmpty())) {
                    Toast.makeText(BaseEnterData.this, "Enter all 4 players...", Toast.LENGTH_SHORT).show();
                    return;
                }
                mCommon.wakeUpDBConnection();
                enterData(false);
            }
        });


        Log.w(TAG, "T1 players:" + mT1_players.toString());
        Log.w(TAG, "T2 players:" + mT2_players.toString());

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
                Log.v(TAG, "mSpinner_P1 onItemSelected mSpinner_P1_selection:" + mSpinner_P1_selection);
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
                Log.v(TAG, "mSpinner_P2 onItemSelected mSpinner_P2_selection:" + mSpinner_P2_selection);
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
                Log.v(TAG, "mSpinner_P3 onItemSelected mSpinner_P3_selection:" + mSpinner_P3_selection);
                mSpinner_P4_selection = "";
                if (!mSingles) rearrangeDropdownList(mSpinner_P4, dataAdapterP4, mT2_players);
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


    protected void onCreateExtra() {
        Log.d(TAG, "onCreateExtra: ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart:" + Thread.currentThread().getId());
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        String s = adapterView.getItemAtPosition(position).toString();
        Log.d(TAG, "onItemSelected: " + s);
        enterData(true);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }


    protected void rearrangeDropdownList(Spinner spinner, ArrayAdapter<String> adapter, List<String> players) {
        Log.v(TAG, "rearrangeDropdownList:" + mSpinner_P1_selection + "/" + mSpinner_P2_selection + "/"
                + mSpinner_P3_selection + "/" + mSpinner_P4_selection);
        adapter.clear();
        //Collections.sort(players);  //sorted already so that players present on the court comes first.
        adapter.addAll(players);
        if (spinner == mSpinner_P2) {
            //if spinner for player 2, remove player 1 selection & set first in the list
            if (!mSpinner_P1_selection.isEmpty()) adapter.remove(mSpinner_P1_selection);
            spinner.setSelection(adapter.getCount() - 1);
            mSpinner_P2_selection = spinner.getItemAtPosition(0).toString();
        } else if (spinner == mSpinner_P3) {
            //if spinner for player 3, remove player 1 & 2 selections & set first in the list
            if (!mSpinner_P1_selection.isEmpty()) adapter.remove(mSpinner_P1_selection);
            if (!mSpinner_P2_selection.isEmpty()) adapter.remove(mSpinner_P2_selection);
            spinner.setSelection(adapter.getCount() - 1);
            mSpinner_P3_selection = spinner.getItemAtPosition(0).toString();
        } else if (spinner == mSpinner_P4) {
            //if spinner for player 3, remove player 1 & 2 selections & set first in the list
            if (!mSpinner_P1_selection.isEmpty()) adapter.remove(mSpinner_P1_selection);
            if (!mSpinner_P2_selection.isEmpty()) adapter.remove(mSpinner_P2_selection);
            if (!mSpinner_P3_selection.isEmpty()) adapter.remove(mSpinner_P3_selection);
            spinner.setSelection(adapter.getCount() - 1);
            mSpinner_P4_selection = spinner.getItemAtPosition(0).toString();
        }
        Log.i(TAG, "rearrangeDropdownList Done:" + mSpinner_P1_selection + "/" +
                mSpinner_P2_selection + "/" + mSpinner_P3_selection + "/" + mSpinner_P4_selection);
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
    }

    protected boolean enterData(boolean dry_run) {
        Log.d(TAG, "enterData: ");
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
                Log.e(TAG, "waitForDBLock: Failed to acquire lock:" + mDBLockCount);
                mAlertTitle = "";
                Log.e(TAG, "workToUpdateDB: Failed to update DB, please refresh and try again later...");
                mAlertMsg = "DB not accessible, please refresh and try again later...";
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
        Log.d(TAG, "workToUpdateDB: " + mCommon.mClub);
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
        Log.d(TAG, "onResume: ");
        super.onResume();
        ScoreTally.activityResumed();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        ScoreTally.activityPaused();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        mMainHandler = null;
    }
}


