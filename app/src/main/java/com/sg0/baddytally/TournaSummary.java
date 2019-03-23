package com.sg0.baddytally;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class TournaSummary extends AppCompatActivity implements CallbackRoutine{
    private static final String TAG = "TournaSummary";

    private RecyclerView mRecyclerView;
    private TournaSummaryRecyclerViewAdapter mAdapter;
    private DatabaseReference mDatabase;
    private SharedData mCommon;
    private MatchInfo mSelectedMatch;
    private TournaUtil mTUtil;
    private String mTourna;


    private void killActivity(){
        Log.d(TAG, "SGO: killActivity: returning OK");
        setResult(RESULT_OK);
        finish();
    }

    private void setTitle(String tourna) {
        if (!TextUtils.isEmpty(tourna)) {
            Log.d(TAG, "setTitle: " + tourna);
            String tempString = Constants.APPNAME + "  " + tourna;
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, Constants.APPNAME.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), Constants.APPNAME.length(), tempString.length(), 0);
            spanString.setSpan(new RelativeSizeSpan(0.7f), Constants.APPNAME.length(), tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(""); //workaround for title getting truncated.
            getSupportActionBar().setTitle(spanString);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clubleague_summary);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        Intent myIntent = getIntent(); // gets the previously created intent
        mTourna = myIntent.getStringExtra("tournament");
        Log.v(TAG, "onCreate :" + mTourna);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mCommon = SharedData.getInstance();
        mSelectedMatch = new MatchInfo();

        findViewById(R.id.gold_parentview).setVisibility(View.GONE);

        FloatingActionButton fab = findViewById(R.id.fab_return);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: ");
                recreate();
            }
        });

        TextView mHeader = findViewById(R.id.silver_group);
        mHeader.setText("");
        TextView summaryHeader = findViewById(R.id.header);

        mRecyclerView = findViewById(R.id.silver_journal_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        LinearLayout parent = findViewById(R.id.silver_parentview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(parent.getContext());
        //firebase DB filter allows only descending order, So, reverse the order so that highest score is shown first
        //Innings score (This round) is fetched first (see below), so that the sorting is on current round score.
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new TournaSummaryRecyclerViewAdapter(TournaSummary.this);
        mRecyclerView.setAdapter(mAdapter);
        //mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
        //drawViews(SharedData.getInstance().mRoundName);
        mTUtil = null;
        mTUtil = new TournaUtil(TournaSummary.this, TournaSummary.this);

        //invoking showtournaments right away causes "PopupWindow $BadTokenException: Unable to add window — token null is not valid" error.
        //https://stackoverflow.com/questions/4187673/problems-creating-a-popup-window-in-android-activity
        //To avoid BadTokenException, you need to defer showing the popup until after all the lifecycle methods are called (-> activity window is displayed):
        findViewById(R.id.silver_journal_view).post(new Runnable() {
            public void run() {
                if(mTourna==null || mTourna.isEmpty())
                    mTUtil.showTournaments(findViewById(R.id.header), findViewById(R.id.silver_journal_view));
                else
                    mTUtil.readDBMatchMeta(mTourna, false);  //invoked from tourna main activity, no need to ask for tournament choice

            }
        });

    }

    //CallbackRoutine Callback interfaces
    public void profileFetched() {}
    public void alertResult(final String in, final Boolean ok, final Boolean ko) {}
    public void callback(final String key, final Object inobj) {}
    public void completed (final String in, final Boolean ok) {
        Log.w(TAG, "completed: " + in + ":" + ok);
        switch (in) {
            case Constants.CB_SHOWTOURNA:
                //callback after reading DB for meta data
                if (ok) {
                    mTourna = mTUtil.mTourna;
                    mTUtil.readDBMatchMeta(mTourna, false);
                    setTitle(mTourna);
                }
                break;
            case Constants.CB_READMATCHMETA:
                //callback after reading DB for meta data
                if (ok) mTUtil.showMatches(findViewById(R.id.header));
                break;
            case Constants.CB_SHOWMATCHES:
                //callback after reading DB for meta data
                if (ok) {
                    MatchInfo mInfo = TournaUtil.getMatchInfoFromString(mTUtil.mMSStr_chosen);
                    if (mInfo != null && mTUtil.mMSInfoMap != null)
                        mSelectedMatch = mTUtil.mMSInfoMap.get(mInfo.key);
                    String matchDesc = mSelectedMatch.T1 + Constants.TEAM_DELIM2 + mSelectedMatch.T2;
                    if (mSelectedMatch != null && !mSelectedMatch.desc.isEmpty())
                        matchDesc += "\n" + mSelectedMatch.desc;
                    Log.w(TAG, "completed: " + in + ":" + mSelectedMatch.toString());
                    TextView mHeader = findViewById(R.id.silver_group);
                    mHeader.setText(matchDesc);
                    mAdapter.setMatch(mTourna, mSelectedMatch);
                }
                break;
        }
    }
}


