package com.sg0.baddytally;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    private FireBaseDBReader mGoldDB;

    private FireBaseDBReader mSilverDB;

    private MenuItem mEnterDataItem;

    private ArrayList<PlayerData>[] mPlayers;
    private ArrayList<String> mGroups = new ArrayList<>();
    private String mClub = Constants.CLUB;
    private String mInnings;
    private String mRoundName;
    private boolean mInitialAttempt;

    private void initAdapter() {
        //if (mSilverAdapter != null) return; //do only once.
        //if done only once, data update is not dynamically updated on the app GUI

        ArrayList<PlayerData> players = new ArrayList<>();
        PlayerData header = new PlayerData("");
        players.add(header);

        RecyclerView mRecyclerGoldView = (RecyclerView) findViewById(R.id.gold_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerGoldView.setHasFixedSize(true);
        // use a linear layout manager
        LinearLayout parent = (LinearLayout) findViewById(R.id.gold_parentview);
        LinearLayoutManager mGoldLayoutManager = new LinearLayoutManager(parent.getContext());
        //firebase DB filter allows only descending order, So, reverse the order so that highest score is shown first
        //Innings score (This round) is fetched first (see below), so that the sorting is on current round score.
        mGoldLayoutManager.setReverseLayout(true);
        mGoldLayoutManager.setStackFromEnd(true);
        mRecyclerGoldView.setLayoutManager(mGoldLayoutManager);
        //mRecyclerGoldView.addItemDecoration(new DividerItemDecoration(MainActivity.this,
        //        DividerItemDecoration.VERTICAL));
        RecyclerViewAdapter mGoldAdapter = new RecyclerViewAdapter(this, Constants.GOLD, players);
        mGoldAdapter.setColor("#eedd82");  //color gold
        mRecyclerGoldView.setAdapter(mGoldAdapter);
        mGoldDB = new FireBaseDBReader(this, mClub, Constants.GOLD, mInnings, (RecyclerViewAdapter) mGoldAdapter, mRecyclerGoldView);
        mGoldDB.fetchThisRoundScore();

        if(SharedData.getInstance().mNumOfGroups==1) {
            //There is only one group, dont show silver group view.
            findViewById(R.id.silver_parentview).setVisibility(View.GONE);
            mSilverDB = null;
        } else {
            RecyclerView mRecyclerSilverView = (RecyclerView) findViewById(R.id.silver_view);
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            mRecyclerSilverView.setHasFixedSize(true);
            // use a linear layout manager
            parent = (LinearLayout) findViewById(R.id.silver_parentview);

            LinearLayoutManager mSilverLayoutManager = new LinearLayoutManager(parent.getContext());
        //firebase DB filter allows only descending order, So, reverse the order so that highest score is shown first
        //Innings score (This round) is fetched first (see below), so that the sorting is on current round score.
        mSilverLayoutManager.setReverseLayout(true);
        mSilverLayoutManager.setStackFromEnd(true);
        mRecyclerSilverView.setLayoutManager(mSilverLayoutManager);
            RecyclerViewAdapter mSilverAdapter = new RecyclerViewAdapter(this, Constants.SILVER, players);
        mSilverAdapter.setColor("#eeeee0");  //color silver
        mRecyclerSilverView.setAdapter(mSilverAdapter);
        mSilverDB = new FireBaseDBReader(this, mClub, Constants.SILVER, mInnings, (RecyclerViewAdapter) mSilverAdapter, mRecyclerSilverView);
        mSilverDB.fetchThisRoundScore();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: starting");
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        mEnterDataItem = null;
        SharedData.getInstance().mNumOfGroups = Constants.NUM_OF_GROUPS;
        //SGO: Test single user group
        //SharedData.getInstance().mNumOfGroups = 1;
    }


    @Override
    protected void onResume() {
        super.onResume();
        mInitialAttempt = false;
        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        String club = prefs.getString(Constants.DATA_CLUB, "");
        if (club.isEmpty()) {
            mInitialAttempt = true;
            Toast.makeText(this, "Click Settings to sign-in to your club ", Toast.LENGTH_LONG)
                    .show();
        }else {
            mClub = club;
            String tempString = Constants.APPNAME + "  [" + mClub + "]";
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, Constants.APPNAME.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), Constants.APPNAME.length(), tempString.length(), 0);
            getSupportActionBar().setTitle(spanString);
            SharedData.getInstance().mClub = mClub;
            SharedData.getInstance().mUser = prefs.getString(Constants.DATA_USER, "");
            SharedData.getInstance().mRole = prefs.getString(Constants.DATA_ROLE, "");
            Log.d(TAG, "onResume: " + SharedData.getInstance().toString());
            fetchInnings();
            if (mEnterDataItem!=null){
                mEnterDataItem.setTitle("Enter Score");  //coming back from initial sign in, change settings menu
                if (Constants.MEMBER.equals(SharedData.getInstance().mRole))
                    mEnterDataItem.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        mEnterDataItem = menu.findItem(R.id.action_enter);
        if (mInitialAttempt) mEnterDataItem.setTitle("Club Sign-in");
        else {
            mEnterDataItem.setTitle("Enter Score");
            if (Constants.MEMBER.equals(SharedData.getInstance().mRole))
                mEnterDataItem.setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT)
                        .show();
                finish();
                startActivity(getIntent());
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:
                if (Constants.ROOT.equals(SharedData.getInstance().mRole)) {
                    Intent myIntent = new Intent(MainActivity.this, Settings.class);
                    MainActivity.this.startActivity(myIntent);
                } else {
                    Toast.makeText(this, "No settings option for you!", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            case R.id.action_delcache:
                SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();
                Toast.makeText(this, "Cache cleared!", Toast.LENGTH_SHORT)
                        .show();
                finish();
                startActivity(getIntent());
                break;
            case R.id.action_enter:
                if(mInitialAttempt) {
                    Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
                    MainActivity.this.startActivity(myIntent);
                }
                else if (!SharedData.getInstance().mMemCode.isEmpty()) {
                    Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
                    //Set the Player data in shared data structure. Player data is filled in a
                    //different (asynchronous) listener in FireBaseDBReader. Overwrite the player data
                    //every time, so that even if initial calls are done too fast (before player data is
                    //filled - which will not happen unless the DB is very slow), the later calls will
                    //set the latest player data from DB.
                    SharedData data = SharedData.getInstance();
                    data.mGoldPlayers = mGoldDB.getPlayers();
                    if(mSilverDB!=null) data.mSilverPlayers = mSilverDB.getPlayers();
                    Log.d(TAG, "Creating LoginActivity: data = " + data.toString());
                    MainActivity.this.startActivity(myIntent);
                } else {
                    Toast.makeText(this, "No connectivity, try after some time...", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            case R.id.action_summary:
                if(mInitialAttempt) {
                    Toast.makeText(this, "You have to Sign-in first.", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Intent myIntent = new Intent(MainActivity.this, Summary.class);
                    MainActivity.this.startActivity(myIntent);
                }
                break;
            default:
                break;
        }

        return true;
    }

    private void fetchInnings() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.INNINGS);
        Query roundQuery = dbRef.orderByKey();
        roundQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.w(TAG, "fetchInnings: onDataChange");
                GenericTypeIndicator<List<InningsDBEntry>> t = new GenericTypeIndicator<List<InningsDBEntry>>() {};
                List<InningsDBEntry> innings = dataSnapshot.getValue(t);
                Log.w(TAG, "fetchInnings: key:" + dataSnapshot.getKey());
                int count = 0;
                for (InningsDBEntry val : innings) {
                    if(val.current) {
                        Log.i(TAG, val.toString());
                        mInnings = val.name;
                        Log.w(TAG, "fetchInnings: mInnings:" + mInnings);
                        SharedData.getInstance().mInnings = mInnings;
                        mRoundName = val.round;
                        Log.w(TAG, "fetchInnings: mRoundName:" + mRoundName);
                        SharedData.getInstance().mRoundName = mRoundName;
                        SharedData.getInstance().mInningsDBKey = Integer.toString(count);
                    }
                    count++;
                }

                /*
                GenericTypeIndicator<Map<String, List<InningsEntry>>> genericTypeIndicator = new GenericTypeIndicator<Map<String, List<InningsEntry>>>() {};
                Map<String, List<InningsEntry>> hashMap = dataSnapshot.getValue(genericTypeIndicator);

                for (Map.Entry<String,List<InningsEntry>> entry : hashMap.entrySet()) {
                    List<InningsEntry> educations = entry.getValue();
                    for (InningsEntry education: educations){
                        Log.i(TAG, education.toString());
                    }
                }

                boolean found = false;
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String inningsKey = child.getKey();
                    Log.w(TAG, "fetchInnings: inningsKey:" + inningsKey);
                    for (DataSnapshot grandchild : child.getChildren()) {
                        Log.w(TAG, "fetchInnings: grandchild:" + grandchild.getKey() + ", val=" + grandchild.getValue() );
                        if (grandchild.getKey().equals("1") && grandchild.getValue(Boolean.class)) {
                            mInnings = inningsKey;
                            Log.w(TAG, "fetchInnings: mInnings:" + mInnings);
                            SharedData.getInstance().mInnings = mInnings;
                            found = true;
                        }
                        if (grandchild.getKey().equals("2")) {
                            mRoundName = grandchild.getValue(String.class);
                            Log.w(TAG, "fetchInnings: mRoundName:" + mRoundName);
                            SharedData.getInstance().mRoundName = mRoundName;
                        }
                    }
                    if (found) break;
                }*/
                initAdapter();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "fetchInnings: onCancelled", databaseError.toException());
                // ...
            }
        });

        dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.PROFILE);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.w(TAG, "fetchProfile: onDataChange");
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    switch (child.getKey()) {
                        case "admincode":
                            SharedData.getInstance().mAdminCode = child.getValue(String.class);
                            break;
                        case "memcode":
                            SharedData.getInstance().mMemCode = child.getValue(String.class);
                            break;
                        case "rootcode":
                            SharedData.getInstance().mRootCode = child.getValue(String.class);
                            break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "fetchProfile: onCancelled", databaseError.toException());
                // ...
            }
        });
    }
}
/*
    private void fetchThisRoundScore() {
        Log.w(TAG, "SAJU: fetchThisRoundScore: " + mInnings);
        if (mInnings.isEmpty()) return;
        int index = 0;
        for (final String group: mGroups) {
            final int idx = index;
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(CLUB).child(mInnings).child(group);
            Query myQuery = dbRef.orderByKey();
            myQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot child: dataSnapshot.getChildren()) {
                        Integer score = child.getValue(Integer.class);
                        String name = child.getKey();
                        //Log.w(TAG, "fetchThisRoundScore [" + mInnings+":"+group + "] child (" + name + ") innings score=" + score.toString());
                        if (mPlayers != null) {
                            //Log.w(TAG, "fetchThisRoundScore[" + Integer.toString(idx) + "] mPlayers[idx].size()="+mPlayers[idx].size());
                            for (int i=0; i<mPlayers[idx].size(); i++) {
                                //Log.w(TAG, "fetchThisRoundScore[" + Integer.toString(i) + "] getName=["+mPlayers[idx].get(i).getName()+"] name=["+name+"]");
                                if (mPlayers[idx].get(i).getName().equalsIgnoreCase(name)) {
                                    mPlayers[idx].get(i).setInnings_score(score.toString());
                                    Log.w(TAG, "fetchThisRoundScore =====> child (" + name + ") set innings score=" + score.toString());
                                }
                            }
                        }
                    }
                    if(mGoldAdapter != null) mGoldAdapter.notifyDataSetChanged();
                    if(mSilverAdapter != null) mSilverAdapter.notifyDataSetChanged();
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting Post failed, log a message
                    Log.w(TAG, "fetchThisRoundScore:onCancelled:"+ mInnings + ":" + group, databaseError.toException());
                    // ...
                }
            });
            index ++;
        }
    }

    private void initData(){

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("kbc").child("groups");
        Query myQuery = dbRef.orderByKey();
        myQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long numOfGroups = dataSnapshot.getChildrenCount();
                Log.w(TAG, "dataSnapshot.numOfGroups=" + numOfGroups);
                mPlayers = null;
                mPlayers = new ArrayList[(int)numOfGroups];
                int index = 0;
                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    String group = child.getKey();
                    mGroups.add(group);
                    Log.w(TAG, "dataSnapshot.getChildren key=" + group);
                    ArrayList<PlayerData> groupPlayers = new ArrayList<>();
                    mPlayers[index] = groupPlayers;
                    PlayerData player = new PlayerData(group);  //add header
                    mPlayers[index].add(player);
                    for (DataSnapshot grandchild: child.getChildren()) {
                        Integer score = grandchild.getValue(Integer.class);
                        String name = grandchild.getKey();
                        Log.w(TAG, "[" + Integer.toString(index) + "] grandchild (" + name + ") score=" + score.toString());
                        player = new PlayerData(name, "0", score.toString(), group);
                        mPlayers[index].add(player);
                        //mGoldPlayers.add(grandchild.getKey());
                        //mGoldInnings_scores.add(Integer.toString(score));
                        //mGoldOverall_scores.add("xxx");
                    }
                    index ++;   //index = num of groups
                }
                initAdapter();
                if(mGoldAdapter != null) mGoldAdapter.notifyDataSetChanged();
                if(mSilverAdapter != null) mSilverAdapter.notifyDataSetChanged();
                fetchRound();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        });



        FireBaseDBReader dbr = new FireBaseDBReader(MainActivity.this, dbRef);
        mGoldPlayers.addAll(dbr.players);
        mGoldInnings_scores.addAll(dbr.scores);
        for (String member : mGoldPlayers){
            Log.i("Member name: ", member);
            mGoldOverall_scores.add("xxx");
        }
        mPlayers.add("abc1");
        mInnings_scores.add("1");
        mOverall_scores.add("101");
        mPlayers.add("abc2");
        mInnings_scores.add("2");
        mOverall_scores.add("102");
        mPlayers.add("abc3");
        mInnings_scores.add("3");
        mOverall_scores.add("103");
    }
}
*/
