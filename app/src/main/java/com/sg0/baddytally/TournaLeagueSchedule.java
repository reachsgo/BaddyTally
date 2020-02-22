package com.sg0.baddytally;


import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TournaLeagueSchedule extends AppCompatActivity implements CallbackRoutine{
    private static final String TAG = "TournaLeagSched";
    private SharedData mCommon;
    private TournaUtil mTUtil;
    private String mTourna;
    private TournaLeagueScheduleRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tourna_leag_schedule);
        mCommon = SharedData.getInstance();
        mTUtil = new TournaUtil(TournaLeagueSchedule.this, TournaLeagueSchedule.this);

        Intent myIntent = getIntent(); // gets the previously created intent
        mTourna = myIntent.getStringExtra("tournament");
        Log.v(TAG, "onCreate :" + mTourna);

        mTUtil.readDBMatchMeta(mTourna, false);  //invoked from tourna main activity, no need to ask for tournament choice


        RecyclerView mRecyclerView = findViewById(R.id.tourna_match_schedule_rv);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        // use a linear layout manager
        LinearLayout parent = findViewById(R.id.schedule_parent);
        LinearLayoutManager layoutManager = new LinearLayoutManager(parent.getContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new TournaLeagueScheduleRecyclerViewAdapter(TournaLeagueSchedule.this);
        mRecyclerView.setAdapter(mAdapter);

        findViewById(R.id.header_date).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(TournaLeagueSchedule.this,
                        "Sorting..." ,
                        Toast.LENGTH_SHORT).show();
                mAdapter.sortOnDate();
            }
        });

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setTitle(String tourna) {
        if (!TextUtils.isEmpty(tourna)) {
            //Log.d(TAG, "setTitle: " + tourna);
            String tempString = Constants.APPNAME + "  " + tourna;
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, Constants.APPNAME.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), Constants.APPNAME.length(),
                    tempString.length(), 0);
            spanString.setSpan(new RelativeSizeSpan(0.7f), Constants.APPNAME.length(),
                    tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(""); //workaround for title getting truncated.
            getSupportActionBar().setTitle(spanString);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Log.d(TAG, "onOptionsItemSelected: ");
        switch (item.getItemId()) {
            case android.R.id.home:
                SharedData.getInstance().killActivity(TournaLeagueSchedule.this, RESULT_OK);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //CallbackRoutine Callback interfaces
    public void profileFetched() {}
    public void alertResult(final String in, final Boolean ok, final Boolean ko) {}
    public void callback(final String key, final Object inobj) {}
    public void completed (final String in, final Boolean ok) {
        //Log.w(TAG, "completed: " + in + ":" + ok);
        switch (in) {
            case Constants.CB_READMATCHMETA:
                //callback after reading DB for meta data
                if (ok)
                {
                    Log.d(TAG, "completed: " + in + ", nNum=" + mTUtil.mNumOfMatches);
                    ArrayList<MatchInfo> msInfoList = new ArrayList<>();
                    if (mTUtil.mMSInfoMap == null) return;
                    for(int i=1; i<=mTUtil.mMSInfoMap.size(); i++) {
                        MatchInfo mSelectedMatch = mTUtil.mMSInfoMap.get(Integer.toString(i));
                        if(mSelectedMatch==null) {
                            Log.e(TAG, i+ ":Selected match is null!");
                            return;
                        }
                        msInfoList.add(mSelectedMatch);
                        //Log.i(TAG, "SGO adding: " + i + " " + mSelectedMatch.toString());
                    }
                    mAdapter.setMatch(mTourna, msInfoList);
                    mAdapter.notifyDataSetChanged();
                }
                break;
            default:
                Log.d(TAG, "completed: default");
                break;

        }
    }

}
