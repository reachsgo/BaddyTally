package com.sg0.baddytally;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
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
    private RecyclerView mRecyclerSilverView;
    private RecyclerView.Adapter mSilverAdapter;

    private void killActivity(){
        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.summary);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Log.v(TAG, "onCreate :" + SharedData.getInstance().toString());

        FloatingActionButton fab = findViewById(R.id.fab_return);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                killActivity();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedData data = SharedData.getInstance();


        mRecyclerGoldView = findViewById(R.id.gold_journal_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerGoldView.setHasFixedSize(true);
        if (Build.VERSION.SDK_INT >= 21) {
            //set the background image (smashing silhouette) tint to gold, only supported for API21+
            mRecyclerGoldView.setBackgroundTintList(ContextCompat.getColorStateList(Summary.this, R.color.colorGold));
        }
        // use a linear layout manager
        LinearLayout parent = findViewById(R.id.gold_journal_parent);
        LinearLayoutManager mGoldLayoutManager = new LinearLayoutManager(parent.getContext());
        mRecyclerGoldView.setLayoutManager(mGoldLayoutManager);

        final ArrayList<GameJournal> goldGameList = new ArrayList<>();
        final ArrayList<String> goldGameListKeys = new ArrayList<>();
        DatabaseReference dbRef = mDatabase.child(data.mClub).child(Constants.JOURNAL).child(data.mInnings).child(data.mRoundName).child(Constants.GOLD);
        Query myQuery = dbRef.orderByKey();
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    GameJournal jEntry = child.getValue(GameJournal.class);
                    goldGameListKeys.add(child.getKey());
                    goldGameList.add(jEntry);
                    //Log.w(TAG, "gold fetchGames:" + child.getKey() + " data=" + jEntry.toReadableString());
                }
                mGoldAdapter = new SummaryRecyclerViewAdapter(Summary.this,  Constants.GOLD, goldGameListKeys, goldGameList);
                mRecyclerGoldView.setAdapter(mGoldAdapter);
                mGoldAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Summary.this, "DB error while fetching gold games:" + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                killActivity();
            }
        });


        if(SharedData.getInstance().mNumOfGroups==1) {
            findViewById(R.id.silver_journal_parent).setVisibility(View.GONE);
        } else {
            mRecyclerSilverView = findViewById(R.id.silver_journal_view);
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            mRecyclerSilverView.setHasFixedSize(true);
            // use a linear layout manager
            parent = findViewById(R.id.silver_journal_parent);
            LinearLayoutManager mSilverLayoutManager = new LinearLayoutManager(parent.getContext());
            //firebase DB filter allows only descending order, So, reverse the order so that highest score is shown first
            //Innings score (This round) is fetched first (see below), so that the sorting is on current round score.
            mRecyclerSilverView.setLayoutManager(mSilverLayoutManager);
            final ArrayList<GameJournal> silverGameList = new ArrayList<>();
            final ArrayList<String> silverGameListKeys = new ArrayList<>();
            DatabaseReference dbRef2 = mDatabase.child(data.mClub).child(Constants.JOURNAL).child(data.mInnings).child(data.mRoundName).child(Constants.SILVER);
            Query myQuery2 = dbRef2.orderByKey();
            myQuery2.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        GameJournal jEntry = child.getValue(GameJournal.class);
                        silverGameListKeys.add(child.getKey());
                        silverGameList.add(jEntry);
                        //Log.w(TAG, "silver fetchGames:" + child.getKey() + " data=" + jEntry.toReadableString());
                    }
                    mSilverAdapter = new SummaryRecyclerViewAdapter(Summary.this, Constants.SILVER, silverGameListKeys, silverGameList);
                    mRecyclerSilverView.setAdapter(mSilverAdapter);
                    mSilverAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(Summary.this, "DB error while fetching silver games:" + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                    killActivity();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        killActivity();
    }
}


