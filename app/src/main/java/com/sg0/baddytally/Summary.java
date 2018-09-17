package com.sg0.baddytally;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
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
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class Summary extends AppCompatActivity {
    private static final String TAG = "Summary";

    private RecyclerView mRecyclerGoldView;
    private RecyclerView.Adapter mGoldAdapter;
    private RecyclerView mRecyclerSilverView;
    private RecyclerView.Adapter mSilverAdapter;

    private void killActivity(){
        /*
        if(mDBUpdated) {
            Intent resultIntent = new Intent();
            //setResult(Activity.RESULT_FIRST_USER, resultIntent);  //hint to refresh the main page
            setResult(Constants.RESULT_DBUPDATED);
            Log.d(TAG, "SGO: DB updated: setting result");
        }*/
        Log.d(TAG, "SGO: killActivity: returning OK");
        setResult(RESULT_OK);
        finish();
    }

    private void setTitle(String round) {
        if (!TextUtils.isEmpty(round)) {
            //show a more human readable round name (date format w/o HH:mm)
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.ROUND_DATEFORMAT, Locale.CANADA);
            try {
                Date d = sdf.parse(round);
                sdf.applyPattern("yyyy-MM-dd");
                round = sdf.format(d);
            } catch (ParseException ex) {
                Log.w(TAG, "setTitle ParseException:" + ex.getMessage());
            }
            final String title = Constants.APPSHORT + "  Summary";
            String tempString = title + "\nRound: " + round;
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), title.length(), tempString.length(), 0);
            spanString.setSpan(new RelativeSizeSpan(0.8f), title.length(), tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView header = findViewById(R.id.header);
            header.setText(spanString);
        }
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

        TextView mGoldHeader = findViewById(R.id.gold_group);
        TextView mSilverHeader = findViewById(R.id.silver_group);
        mGoldHeader.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //show only Gold group, if done a 2nd time, go back to normal.
                LinearLayout silverView = findViewById(R.id.silver_parentview);
                if (silverView.getVisibility() == View.GONE)
                    silverView.setVisibility(View.VISIBLE);
                else
                    silverView.setVisibility(View.GONE);
                return false;
            }
        });
        mSilverHeader.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //show only Silver group, if done a 2nd time, go back to normal.
                LinearLayout goldView = findViewById(R.id.gold_parentview);
                if (goldView.getVisibility() == View.GONE)
                    goldView.setVisibility(View.VISIBLE);
                else
                    goldView.setVisibility(View.GONE);
                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedData data = SharedData.getInstance();
        setTitle(SharedData.getInstance().mRoundName);

        mRecyclerGoldView = findViewById(R.id.gold_journal_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerGoldView.setHasFixedSize(true);
        if (Build.VERSION.SDK_INT >= 21) {
            //set the background image (smashing silhouette) tint to gold, only supported for API21+
            mRecyclerGoldView.setBackgroundTintList(ContextCompat.getColorStateList(Summary.this, R.color.colorGold));
        }
        // use a linear layout manager
        LinearLayout parent = findViewById(R.id.gold_parentview);
        LinearLayoutManager mGoldLayoutManager = new LinearLayoutManager(parent.getContext());
        mRecyclerGoldView.setLayoutManager(mGoldLayoutManager);

        final ArrayList<GameJournalDBEntry> goldGameList = new ArrayList<>();
        final ArrayList<String> goldGameListKeys = new ArrayList<>();
        DatabaseReference dbRef = mDatabase.child(data.mClub).child(Constants.JOURNAL).child(data.mInnings).child(data.mRoundName).child(Constants.GOLD);
        Query myQuery = dbRef.orderByKey();
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    GameJournalDBEntry jEntry = child.getValue(GameJournalDBEntry.class);
                    goldGameListKeys.add(child.getKey());
                    goldGameList.add(jEntry);
                    //Log.w(TAG, "gold fetchGames:" + child.getKey() + " data=" + jEntry.toReadableString());
                }
                mGoldAdapter = new SummaryRecyclerViewAdapter(Summary.this, Constants.GOLD, goldGameListKeys, goldGameList);
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
            findViewById(R.id.silver_parentview).setVisibility(View.GONE);
        } else {
            mRecyclerSilverView = findViewById(R.id.silver_journal_view);
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            mRecyclerSilverView.setHasFixedSize(true);
            // use a linear layout manager
            parent = findViewById(R.id.silver_parentview);
            LinearLayoutManager mSilverLayoutManager = new LinearLayoutManager(parent.getContext());
            //firebase DB filter allows only descending order, So, reverse the order so that highest score is shown first
            //Innings score (This round) is fetched first (see below), so that the sorting is on current round score.
            mRecyclerSilverView.setLayoutManager(mSilverLayoutManager);
            final ArrayList<GameJournalDBEntry> silverGameList = new ArrayList<>();
            final ArrayList<String> silverGameListKeys = new ArrayList<>();
            DatabaseReference dbRef2 = mDatabase.child(data.mClub).child(Constants.JOURNAL).child(data.mInnings).child(data.mRoundName).child(Constants.SILVER);
            Query myQuery2 = dbRef2.orderByKey();
            myQuery2.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        GameJournalDBEntry jEntry = child.getValue(GameJournalDBEntry.class);
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


