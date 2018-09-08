package com.sg0.baddytally;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;



public class Summary extends AppCompatActivity {
    private static final String TAG = "Summary";

    private RecyclerView mRecyclerGoldView;
    private RecyclerView.Adapter mGoldAdapter;
    private LinearLayoutManager mGoldLayoutManager;
    private FireBaseDBReader mGoldDB;

    private RecyclerView mRecyclerSilverView;
    private RecyclerView.Adapter mSilverAdapter;
    private LinearLayoutManager mSilverLayoutManager;
    private FireBaseDBReader mSilverDB;

    private void killActivity(){
        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.summary);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Log.w(TAG, "onCreate :" + SharedData.getInstance().toString());
    }

    @Override
    protected void onStart() {
        super.onStart();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedData data = SharedData.getInstance();


        mRecyclerGoldView = (RecyclerView) findViewById(R.id.gold_journal_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerGoldView.setHasFixedSize(true);
        // use a linear layout manager
        LinearLayout parent = (LinearLayout) findViewById(R.id.gold_journal_parent);
        mGoldLayoutManager = new LinearLayoutManager(parent.getContext());
        mRecyclerGoldView.setLayoutManager(mGoldLayoutManager);

        final ArrayList<GameJournal> goldGameList= new ArrayList<>();
        DatabaseReference dbRef = mDatabase.child(data.mClub).child(Constants.JOURNAL).child(data.mInnings).child(data.mRoundName).child(Constants.GOLD);
        Query myQuery = dbRef.orderByKey();
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    GameJournal jEntry = child.getValue(GameJournal.class);
                    goldGameList.add(jEntry);
                    Log.w(TAG, "gold fetchGames:" + jEntry.toReadableString());
                }
                mGoldAdapter = new SummaryRecyclerViewAdapter(Summary.this,  Constants.GOLD, goldGameList);
                mRecyclerGoldView.setAdapter(mGoldAdapter);
                mGoldAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Summary.this, "DB error while fetching gold games!", Toast.LENGTH_LONG).show();
                killActivity();
            }
        });



        mRecyclerSilverView = (RecyclerView) findViewById(R.id.silver_journal_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerSilverView.setHasFixedSize(true);
        // use a linear layout manager
        parent = (LinearLayout) findViewById(R.id.silver_journal_parent);
        mSilverLayoutManager = new LinearLayoutManager(parent.getContext());
        //firebase DB filter allows only descending order, So, reverse the order so that highest score is shown first
        //Innings score (This round) is fetched first (see below), so that the sorting is on current round score.
        mRecyclerSilverView.setLayoutManager(mSilverLayoutManager);
        final ArrayList<GameJournal> silverGameList= new ArrayList<>();
        DatabaseReference dbRef2 = mDatabase.child(data.mClub).child(Constants.JOURNAL).child(data.mInnings).child(data.mRoundName).child(Constants.SILVER);
        Query myQuery2 = dbRef2.orderByKey();
        myQuery2.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    GameJournal jEntry = child.getValue(GameJournal.class);
                    silverGameList.add(jEntry);
                    Log.w(TAG, "silver fetchGames:" + jEntry.toReadableString());
                }
                mSilverAdapter = new SummaryRecyclerViewAdapter(Summary.this,  Constants.SILVER, silverGameList);
                mRecyclerSilverView.setAdapter(mSilverAdapter);
                mSilverAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Summary.this, "DB error while fetching silver games!", Toast.LENGTH_LONG).show();
                killActivity();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        killActivity();
    }
}


