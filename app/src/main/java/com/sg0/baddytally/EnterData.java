package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;




public class EnterData extends AppCompatActivity {

    static final List<Integer> scoreList = new ArrayList<Integer>() {{
        add(30);
        add(29);
        add(28);
        add(27);
        add(26);
        add(25);
        add(24);
        add(23);
        add(22);
        add(21);
        add(20);
        add(19);
        add(18);
        add(17);
        add(16);
        add(15);
        add(14);
        add(13);
        add(12);
        add(11);
        add(10);
        add(9);
        add(8);
        add(7);
        add(6);
        add(5);
        add(4);
        add(3);
        add(2);
        add(1);
        add(0);
    }};
    private static final String TAG = "EnterData";
    private String mRole;
    private String mGroup;
    private String mGameType;
    private String mInnings;
    private String mNewRoundFlag;
    private String mRoundName;
    private String mClub;
    private int mGameNum = 1;
    private boolean mSingles;

    private Spinner mSpinner_P1;
    private Spinner mSpinner_P2;
    private Spinner mSpinner_P3;
    private Spinner mSpinner_P4;
    private Spinner mSpinner_T1;
    private Spinner mSpinner_T2;

    private String mSpinner_P1_selection;
    private String mSpinner_P2_selection;
    private String mSpinner_P3_selection;
    private String mSpinner_P4_selection;

    private DatabaseReference mDatabase;

    private String createNewRoundName(boolean commit) {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        String rndName = df.format(c);
        if (commit) {
            Log.w(TAG, "createNewRoundName: committing:" + rndName);
            SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.NEWROUND, rndName);
            editor.commit();
        }
        return rndName;
    }

    private void killActivity(){
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_data);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onStart() {
        super.onStart();


        mRole = SharedData.getInstance().mRole;
        mClub = SharedData.getInstance().mClub;
        mInnings = SharedData.getInstance().mInnings;
        Intent myIntent = getIntent(); // gets the previously created intent
        mGameType = myIntent.getStringExtra("gametype");
        mGroup = myIntent.getStringExtra("group");
        mNewRoundFlag = myIntent.getStringExtra("new_round");
        Log.w(TAG, "onStart :" + SharedData.getInstance().toString() + "/" + mGroup + "/" + mGameType + "/" + mNewRoundFlag);

        mSingles = false;
        if (Constants.SINGLES.equals(mGameType)) mSingles = true;

        mSpinner_P1 = findViewById(R.id.spinner_p1);
        mSpinner_P2 = findViewById(R.id.spinner_p2);
        mSpinner_P3 = findViewById(R.id.spinner_p3);
        mSpinner_P4 = findViewById(R.id.spinner_p4);
        mSpinner_T1 = findViewById(R.id.score_t1);
        mSpinner_T2 = findViewById(R.id.score_t2);
        mSpinner_P1_selection = new String();
        mSpinner_P2_selection = new String();
        mSpinner_P3_selection = new String();
        mSpinner_P4_selection = new String();

        // ...
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mRoundName = "";
        if(!mNewRoundFlag.equals(Constants.NEWROUND)) {
            mRoundName = SharedData.getInstance().mRoundName;  //use the value read from DB
        }
        if(mRoundName.isEmpty()) {
            mRoundName = createNewRoundName(true);
        }
        Log.w(TAG, "mRoundName=" + mRoundName);

        if (mSingles) {
            mSpinner_P2.setVisibility(View.GONE);
            mSpinner_P4.setVisibility(View.GONE);
        }
        final Button enterButton = (Button) findViewById(R.id.enter_button);
        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchGames();
            }
        });
        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                killActivity();
            }
        });
        Button summaryButton = (Button) findViewById(R.id.summary_button);
        summaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(EnterData.this, Summary.class);
                EnterData.this.startActivity(myIntent);
            }
        });

        ArrayList<PlayerData> players;
        if (Constants.GOLD.equals(mGroup)) {
            players = SharedData.getInstance().mGoldPlayers;
        } else {
            players = SharedData.getInstance().mSilverPlayers;
        }

        final List<String> playerList = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            playerList.add(players.get(i).getName());
            Log.w(TAG, "players[" + Integer.toString(i) + "] getName=[" + players.get(i).getName() + "]");
        }

        List<String> p1List = new ArrayList<>(playerList);
        ArrayAdapter<String> dataAdapterP1 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, p1List);
        dataAdapterP1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mSpinner_P1.setSelection(0);
        mSpinner_P1.setAdapter(dataAdapterP1);

        List<String> p2List = new ArrayList<>(playerList);
        final ArrayAdapter<String> dataAdapterP2 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, p2List);
        dataAdapterP2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mSpinner_P2.setSelection(1);
        mSpinner_P2.setAdapter(dataAdapterP2);

        List<String> p3List = new ArrayList<>(playerList);
        final ArrayAdapter<String> dataAdapterP3 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, p3List);
        dataAdapterP3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mSpinner_P3.setSelection(2);
        mSpinner_P3.setAdapter(dataAdapterP3);

        List<String> p4List = new ArrayList<>(playerList);
        final ArrayAdapter<String> dataAdapterP4 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, p4List);
        dataAdapterP4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mSpinner_P4.setSelection(3);
        mSpinner_P4.setAdapter(dataAdapterP4);

        //For P1, adjust drop down menus of P2, P3 & P4
        mSpinner_P1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mSpinner_P1_selection = (String) adapterView.getItemAtPosition(position);
                // Notify the selected item text
                Log.w(TAG, "mSpinner_P1 onItemSelected mSpinner_P1_selection:" + mSpinner_P1_selection);
                mSpinner_P2_selection = "";
                mSpinner_P3_selection = "";
                mSpinner_P4_selection = "";
                //if(mSpinner_P1_selection.equals(mSpinner_P2_selection)) mSpinner_P2_selection = "";
                //if(mSpinner_P1_selection.equals(mSpinner_P3_selection)) mSpinner_P3_selection = "";
                //if(mSpinner_P1_selection.equals(mSpinner_P4_selection)) mSpinner_P4_selection = "";
                if(!mSingles) rearrangeDropdownList(mSpinner_P2, dataAdapterP2, playerList);
                rearrangeDropdownList(mSpinner_P3, dataAdapterP3, playerList);
                if(!mSingles) rearrangeDropdownList(mSpinner_P4, dataAdapterP4, playerList);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {  }
        });

        //For P2, adjust drop down menus of P3 & P4.
        //This spinner will not be visible for Singles.
        mSpinner_P2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mSpinner_P2_selection = (String) adapterView.getItemAtPosition(position);
                // Notify the selected item text
                Log.w(TAG, "mSpinner_P2 onItemSelected mSpinner_P2_selection:" + mSpinner_P2_selection);
                mSpinner_P3_selection = "";
                mSpinner_P4_selection = "";
                //if(mSpinner_P2_selection.equals(mSpinner_P3_selection)) mSpinner_P3_selection = "";
                //if(mSpinner_P2_selection.equals(mSpinner_P4_selection)) mSpinner_P4_selection = "";
                rearrangeDropdownList(mSpinner_P3, dataAdapterP3, playerList);
                rearrangeDropdownList(mSpinner_P4, dataAdapterP4, playerList);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {  }
        });

        //For P3, adjust drop down menus of P4
        mSpinner_P3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mSpinner_P3_selection = (String) adapterView.getItemAtPosition(position);
                // Notify the selected item text
                Log.w(TAG, "mSpinner_P3 onItemSelected mSpinner_P3_selection:" + mSpinner_P3_selection);
                mSpinner_P4_selection = "";
                if(!mSingles) rearrangeDropdownList(mSpinner_P4, dataAdapterP4, playerList);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {  }
        });


        ArrayAdapter<Integer> scoreAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, scoreList);
        scoreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner_T1.setAdapter(scoreAdapter);
        mSpinner_T2.setAdapter(scoreAdapter);
        mSpinner_T1.setSelection(9);   //score 21
        mSpinner_T2.setSelection(15);  //score 15
    }

    private void rearrangeDropdownList(Spinner spinner, ArrayAdapter<String> adapter, List<String> players) {
        Log.w(TAG, "rearrangeDropdownList:" + mSpinner_P1_selection + "/" + mSpinner_P2_selection + "/" + mSpinner_P3_selection + "/" + mSpinner_P4_selection);
        adapter.clear();
        Collections.sort(players);
        adapter.addAll(players);
        if (spinner == mSpinner_P2) {
            //if spinner for player 2, remove player 1 selection & set first in the list
            adapter.remove(mSpinner_P1_selection);
            spinner.setSelection(0);
            mSpinner_P2_selection = spinner.getItemAtPosition(0).toString();
        }
        if (spinner == mSpinner_P3) {
            //if spinner for player 3, remove player 1 & 2 selections & set first in the list
            adapter.remove(mSpinner_P1_selection);
            adapter.remove(mSpinner_P2_selection);
            spinner.setSelection(0);
            mSpinner_P3_selection = spinner.getItemAtPosition(0).toString();
        }
        if (spinner == mSpinner_P4) {
            //if spinner for player 3, remove player 1 & 2 selections & set first in the list
            adapter.remove(mSpinner_P1_selection);
            adapter.remove(mSpinner_P2_selection);
            adapter.remove(mSpinner_P3_selection);
            spinner.setSelection(0);
            mSpinner_P4_selection = spinner.getItemAtPosition(0).toString();
        }
        Log.w(TAG, "rearrangeDropdownList Done:" + mSpinner_P1_selection + "/" + mSpinner_P2_selection + "/" + mSpinner_P3_selection + "/" + mSpinner_P4_selection);
        adapter.notifyDataSetChanged();
    }

    private void fetchGames(){
        mGameNum = 1;
        DatabaseReference dbRef = mDatabase.child(mClub).child(Constants.JOURNAL).child(mInnings).child(mRoundName).child(mGroup);
        Query myQuery = dbRef.orderByKey();
        final ArrayList<GameJournal> gameList= new ArrayList<>();
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    GameJournal jEntry = child.getValue(GameJournal.class);
                    gameList.add(jEntry);
                    Log.w(TAG, "fetchGames:" + jEntry.toReadableString());
                }
                checkData(gameList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(EnterData.this, "DB error while fetching games!", Toast.LENGTH_LONG).show();
                killActivity();
            }
        });
    }

    private void checkData(ArrayList<GameJournal> gameList) {

        if (false == enterData(true))  //dry run returned failure
            return;

        //check player data, if the same team is being repeated
        String p1 = mSpinner_P1.getSelectedItem().toString();
        String p3 = mSpinner_P3.getSelectedItem().toString();
        String p2 = "";
        String p4 = "";

        if (!mSingles) {
            p2 = mSpinner_P2.getSelectedItem().toString();
            p4 = mSpinner_P4.getSelectedItem().toString();
        }
        int numOfSingles = 0;
        int numOfDoubles = 0;
        for (GameJournal games : gameList) {
            if (Constants.SINGLES.equals(games.mGameType))
                numOfSingles++;
            else
                numOfDoubles++;
        }
        Log.d(TAG, "checkData: singles=" + numOfSingles + "doubles=" + numOfDoubles);

        for (GameJournal games : gameList) {
            if (!games.mGameType.equals(mGameType)) continue;

            if (games.playedBefore(p1, p2, p3, p4)) {
                if (mSingles)
                    showAlert(p1 + " has already played " + p3 + " today!");
                else
                    showAlert(p1 + "/" + p2 + " or " + p3 + "/" + p4 + " have already played as a team today!");
                return;
            }
        }

        //Things look good, enter the data in DB
        enterData(false);  //not a dry run
    }

    private void showAlert(String message) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        mGameNum++;
                        enterData(false);  //not a dry run
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        //killActivity();
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(EnterData.this);
        builder.setMessage(message + "\n\nWould be nice if you can play all combinations in your group, before repeating." +
                "\n\nYou still want to enter this score?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private boolean enterData(boolean dry_run) {
        String p1 = mSpinner_P1.getSelectedItem().toString();
        String p3 = mSpinner_P3.getSelectedItem().toString();
        String p2 = "";
        String p4 = "";

        if (!mSingles) {
            p2 = mSpinner_P2.getSelectedItem().toString();
            p4 = mSpinner_P4.getSelectedItem().toString();
        }

        Integer s1 = (Integer) mSpinner_T1.getSelectedItem();
        Integer s2 = (Integer) mSpinner_T2.getSelectedItem();
        String winners, winner1, winner2;
        String losers, loser1, loser2;
        Integer winningScore;
        Integer losingScore;

        if (s1 > s2) {
            if (mSingles) {
                winners = p1; winner1 = p1; winner2 = "";
                losers = p3; loser1 = p3; loser2 = "";
            } else {
                winners = p1 + "/" + p2; winner1 = p1; winner2 = p2;
                losers = p3 + "/" + p4; loser1 = p3; loser2 = p4;
            }
            winningScore = s1;
            losingScore = s2;
        } else {
            if (mSingles) {
                winners = p3; winner1 = p3; winner2 = "";
                losers = p1; loser1 = p1; loser2 = "";
            } else {
                winners = p3 + "/" + p4; winner1 = p3; winner2 = p4;
                losers = p1 + "/" + p2; loser1 = p1; loser2 = p2;
            }
            winningScore = s2;
            losingScore = s1;
        }

        if ((winningScore < 21) || s1 == s2 || winners.equals(losers)) {
            Toast.makeText(EnterData.this, "Bad data!", Toast.LENGTH_SHORT).show();
            return false;
        }

        //Do not proceed to do tha actual DB update, if this is a dry run.
        if (dry_run) return true;

        Toast.makeText(EnterData.this, winners + " won!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "enterData: " + winners + " vs " + losers + " : " + winningScore.toString() + "-" + losingScore.toString());

        GameJournal jEntry = new GameJournal(mRoundName, mInnings, SharedData.getInstance().mUser);
        jEntry.setResult(createNewRoundName(false), mGameType, winner1, winner2, loser1, loser2, winningScore, losingScore);
        jEntry.mGameNum = mGameNum;
        DatabaseReference jDBEntryRef = mDatabase.child(mClub).child(Constants.JOURNAL).child(mInnings).child(mRoundName).child(mGroup).push();
        jDBEntryRef.setValue(jEntry);
        Log.w(TAG, "WRITTEN jEntry: " + jEntry.toReadableString());
        updateDB(winner1, winner2);
        mDatabase.child(mClub).child(Constants.INNINGS).child(mInnings).child(Constants.ROUND_INDEX).setValue(mRoundName);
        SharedData.getInstance().mRoundName = mRoundName;
        Log.w(TAG, "WRITTEN mRoundName: " + mRoundName + " data=" + SharedData.getInstance().toString());
        //killActivity();
        return true;
    }

    private void updateDB(final String winner1, final String winner2){
        if (winner1.isEmpty()) {
            Toast.makeText(EnterData.this, "winner name is empty!", Toast.LENGTH_LONG).show();
            killActivity();
        }

        DatabaseReference dbRef_winner1 = mDatabase.child(mClub).child(mInnings).child(mGroup).child(winner1);
        dbRef_winner1.addListenerForSingleValueEvent(new UpdateScores(EnterData.this, mSingles, winner1, dbRef_winner1, true));
        dbRef_winner1 = mDatabase.child(mClub).child(Constants.GROUPS).child(mGroup).child(winner1);
        dbRef_winner1.addListenerForSingleValueEvent(new UpdateScores(EnterData.this, mSingles, winner1, dbRef_winner1, false));
        if (mSingles) return;
        DatabaseReference dbRef_winner2 = mDatabase.child(mClub).child(mInnings).child(mGroup).child(winner2);
        dbRef_winner2.addListenerForSingleValueEvent(new UpdateScores(EnterData.this, mSingles, winner2, dbRef_winner2, true));
        dbRef_winner2 = mDatabase.child(mClub).child(Constants.GROUPS).child(mGroup).child(winner2);
        dbRef_winner2.addListenerForSingleValueEvent(new UpdateScores(EnterData.this, mSingles, winner2, dbRef_winner2, false));

    }


}
