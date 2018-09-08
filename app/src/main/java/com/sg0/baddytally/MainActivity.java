package com.sg0.baddytally;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    private RecyclerView mRecyclerGoldView;
    private RecyclerViewAdapter mGoldAdapter;
    private LinearLayoutManager mGoldLayoutManager;
    private FireBaseDBReader mGoldDB;

    private RecyclerView mRecyclerSilverView;
    private RecyclerViewAdapter mSilverAdapter;
    private LinearLayoutManager mSilverLayoutManager;
    private FireBaseDBReader mSilverDB;

    private MenuItem mEnterDataItem;

    private ArrayList<PlayerData>[] mPlayers;
    private ArrayList<String> mGroups = new ArrayList<>();
    private String mClub = Constants.CLUB;
    private String mInnings;
    private String mRoundName;
    private String mAdminCode;
    private String mMemCode;
    private boolean mInitialAttempt;


    private void initAdapter() {
        //if (mSilverAdapter != null) return; //do only once.
        //if done only once, data update is not dynamically updated on the app GUI

        ArrayList<PlayerData> players = new ArrayList<>();
        PlayerData header = new PlayerData("");
        players.add(header);

        mRecyclerGoldView = (RecyclerView) findViewById(R.id.gold_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerGoldView.setHasFixedSize(true);
        // use a linear layout manager
        LinearLayout parent = (LinearLayout) findViewById(R.id.gold_parentview);
        mGoldLayoutManager = new LinearLayoutManager(parent.getContext());
        //firebase DB filter allows only descending order, So, reverse the order so that highest score is shown first
        //Innings score (This round) is fetched first (see below), so that the sorting is on current round score.
        mGoldLayoutManager.setReverseLayout(true);
        mRecyclerGoldView.setLayoutManager(mGoldLayoutManager);
        //mRecyclerGoldView.addItemDecoration(new DividerItemDecoration(MainActivity.this,
        //        DividerItemDecoration.VERTICAL));
        mGoldAdapter = new RecyclerViewAdapter(this,  Constants.GOLD, players);
        mGoldAdapter.setColor("#eedd82");  //color gold
        mRecyclerGoldView.setAdapter(mGoldAdapter);
        mGoldDB = new FireBaseDBReader(this, mClub, Constants.GOLD, mInnings, (RecyclerViewAdapter) mGoldAdapter, mRecyclerGoldView);
        mGoldDB.fetchThisRoundScore();

        mRecyclerSilverView = (RecyclerView) findViewById(R.id.silver_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerSilverView.setHasFixedSize(true);
        // use a linear layout manager
        parent = (LinearLayout) findViewById(R.id.silver_parentview);
        mSilverLayoutManager = new LinearLayoutManager(parent.getContext());
        //firebase DB filter allows only descending order, So, reverse the order so that highest score is shown first
        //Innings score (This round) is fetched first (see below), so that the sorting is on current round score.
        mSilverLayoutManager.setReverseLayout(true);
        mRecyclerSilverView.setLayoutManager(mSilverLayoutManager);
        mSilverAdapter = new RecyclerViewAdapter(this, Constants.SILVER, players);
        mSilverAdapter.setColor("#eeeee0");  //color silver
        mRecyclerSilverView.setAdapter(mSilverAdapter);
        mSilverDB = new FireBaseDBReader(this, mClub, Constants.SILVER, mInnings, (RecyclerViewAdapter) mSilverAdapter, mRecyclerSilverView);
        mSilverDB.fetchThisRoundScore();
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
    }


    @Override
    protected void onResume() {
        super.onResume();
        mInitialAttempt = false;
        mMemCode = "";
        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        String club = prefs.getString(Constants.DATA_CLUB, "");
        if (club.isEmpty()) {
            mInitialAttempt = true;
            Toast.makeText(this, "Click Settings to sign-in to your club ", Toast.LENGTH_LONG)
                    .show();
            return; //nothing to show, club not known yet!
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
            if (mEnterDataItem!=null) mEnterDataItem.setTitle("Enter Score");  //coming back from initial sign in, change settings menu
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        mEnterDataItem = menu.findItem(R.id.action_enter);
        if (mInitialAttempt) mEnterDataItem.setTitle("Club Sign-in");
        else mEnterDataItem.setTitle("Enter Score");
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
                Toast.makeText(this, "Patience!", Toast.LENGTH_SHORT)
                        .show();
                break;
            case R.id.action_delcache:
                SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.commit();
                Toast.makeText(this, "Cache cleared!", Toast.LENGTH_SHORT)
                        .show();
                break;
            case R.id.action_enter:
                if(mInitialAttempt) {
                    Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
                    MainActivity.this.startActivity(myIntent);
                }
                else if (!mMemCode.isEmpty()) {
                    Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
                    SharedData data = SharedData.getInstance();
                    data.mInnings = mInnings;
                    data.mMemCode = mMemCode;
                    data.mAdminCode = mAdminCode;
                    data.mRoundName = mRoundName;
                    data.mGoldPlayers = mGoldDB.getPlayers();
                    data.mSilverPlayers = mSilverDB.getPlayers();
                    Log.d(TAG, "Creating LoginActivity: data = " + data.toString());
                    MainActivity.this.startActivity(myIntent);
                } else {
                    Toast.makeText(this, "No connectivity, try after some time...", Toast.LENGTH_SHORT)
                            .show();
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
                boolean found = false;
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String inningsKey = child.getKey();
                    Log.w(TAG, "fetchInnings: inningsKey:" + inningsKey);
                    for (DataSnapshot grandchild : child.getChildren()) {
                        Log.w(TAG, "fetchInnings: grandchild:" + grandchild.getKey() + ", val=" + grandchild.getValue() );
                        if (grandchild.getKey().equals("1") && grandchild.getValue(Boolean.class)) {
                            mInnings = inningsKey;
                            Log.w(TAG, "fetchInnings: mInnings:" + mInnings);
                            found = true;
                        }
                        if (grandchild.getKey().equals("2")) {
                            mRoundName = grandchild.getValue(String.class);
                            Log.w(TAG, "fetchInnings: mRoundName:" + mRoundName);
                        }
                    }
                    if (found) break;
                }
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
                    if (child.getKey().equals("admincode")) {
                        mAdminCode = child.getValue(String.class);
                    } else if (child.getKey().equals("memcode")) {
                        mMemCode = child.getValue(String.class);
                    }
                    Log.w(TAG, "fetchProfile: onDataChange:" + mAdminCode + "/" + mMemCode);
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
