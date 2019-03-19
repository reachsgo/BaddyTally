package com.sg0.baddytally;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class ClubLeagueEnterData extends BaseEnterData {
    private static final String TAG = "ClubLeagueEnterData";
    private String mGroup;
    private String mGameType;
    private String mInnings;
    private String mRoundName;
    private String mClub;
    private int mGameNum = 1;
    private GamePlayers mGamePlayers;

    private void killActivity(){
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mType = Constants.CLUBLEAGUE;
        setContentView(R.layout.activity_clubleague_enter_data);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        FloatingActionButton fab = findViewById(R.id.fab_cancel);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCommon.killActivity(ClubLeagueEnterData.this, RESULT_OK);
            }
        });

        mClub = SharedData.getInstance().mClub;
        mInnings = SharedData.getInstance().mInnings;
        Intent myIntent = getIntent(); // gets the previously created intent
        mGameType = myIntent.getStringExtra("gametype");
        mGroup = myIntent.getStringExtra("group");
        String mNewRoundFlag = myIntent.getStringExtra("new_round");
        Log.w(TAG, "onCreate :" + SharedData.getInstance().toString() + "/" + mGroup + "/" + mGameType + "/" + mNewRoundFlag);
        mSingles = Constants.SINGLES.equals(mGameType);
        mGamePlayers = null;
        mRoundName = "";
        if(!mNewRoundFlag.equals(Constants.NEWROUND)) {
            mRoundName = SharedData.getInstance().mRoundName;  //use the value read from DB
        }
        if(mRoundName.isEmpty()) {
            //mRoundName = SharedData.getInstance().createNewRoundName(true, ClubLeagueEnterData.this);
            Toast.makeText(ClubLeagueEnterData.this, "Create a new round first!",
                    Toast.LENGTH_LONG).show();
            finish();
        }
        Log.w(TAG, "mRoundName=" + mRoundName);

        String title = (String)((TextView)findViewById(R.id.enterdata_header)).getText();
        title += ": " + mGroup;
        ((TextView)findViewById(R.id.enterdata_header)).setText(title);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mSpinner_P1 = findViewById(R.id.spinner_p1);
        mSpinner_P2 = findViewById(R.id.spinner_p2);
        mSpinner_P3 = findViewById(R.id.spinner_p3);
        mSpinner_P4 = findViewById(R.id.spinner_p4);
        mSpinner_T1_1 = findViewById(R.id.score_t1);
        mSpinner_T2_1 = findViewById(R.id.score_t2);
        mSpinner_P1_selection = "";
        mSpinner_P2_selection = "";
        mSpinner_P3_selection = "";
        mSpinner_P4_selection = "";

        if (mSingles) {
            mSpinner_P2.setVisibility(View.GONE);
            mSpinner_P4.setVisibility(View.GONE);
        }

        ArrayList<PlayerData> players;
        Set<String> presentPlayerNames;
        if (Constants.GOLD.equals(mGroup)) {
            players = new ArrayList<>(SharedData.getInstance().mGoldPlayers);
            presentPlayerNames = SharedData.getInstance().mGoldPresentPlayerNames;
        } else {
            players = new ArrayList<>(SharedData.getInstance().mSilverPlayers);
            presentPlayerNames = SharedData.getInstance().mSilverPresentPlayerNames;
        }

        if(mSingles && players.size()<2){
            Toast.makeText(ClubLeagueEnterData.this, "Not enough players to play Singles in group " + mGroup,
                    Toast.LENGTH_LONG).show();
            killActivity();
        } else if(!mSingles && players.size()<4){
            Toast.makeText(ClubLeagueEnterData.this, "Not enough players to play Doubles in group " + mGroup,
                    Toast.LENGTH_LONG).show();
            killActivity();
        }

        if(SharedData.getInstance().mInningsDBKey == -1) {
            Toast.makeText(ClubLeagueEnterData.this, "No current innings, Create innings first!",
                    Toast.LENGTH_LONG).show();
            killActivity();
        }

        final Button enterButton = findViewById(R.id.enter_button);
        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mSpinner_P1_selection.isEmpty() || mSpinner_P3_selection.isEmpty()) {
                    Toast.makeText(ClubLeagueEnterData.this, "Enter both players...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!mSingles && (mSpinner_P2_selection.isEmpty() || mSpinner_P4_selection.isEmpty())) {
                    Toast.makeText(ClubLeagueEnterData.this, "Enter all 4 players...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!SharedData.getInstance().isDBConnected()) {
                    Toast.makeText(ClubLeagueEnterData.this,
                            "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
                    mCommon.wakeUpDBConnection_profile();
                    return;
                }
                fetchGames();
            }
        });
        Button summaryButton = findViewById(R.id.summary_button);
        summaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(ClubLeagueEnterData.this, ClubLeagueSummary.class);
                ClubLeagueEnterData.this.startActivityForResult(myIntent, Constants.SUMMARY_ACTIVITY);
            }
        });

        final List<String> playerList = new ArrayList<>();
        //Order the players such that players present on that game day are added first.
        for (int i = 0; i < players.size(); i++) {
            if (null!=presentPlayerNames && presentPlayerNames.contains(players.get(i).getName())) {
                //add the players present today first. This helps the user to make enter score quickly.
                playerList.add(players.get(i).getName());
                players.get(i).markToRelegate();  //reusing relegate flag here
                //Log.w(TAG, "SGO PRESENT players[" + Integer.toString(i) + "] getDesc=[" + players.get(i).getDesc() + "]");
            }
        }
        for (int i = 0; i < players.size(); i++) {
            if (! players.get(i).isMarkedToRelegate()) { //if not added in the loop before, then add now
                playerList.add(players.get(i).getName());
                //Log.w(TAG, "SGO NOT PRESENT players[" + Integer.toString(i) + "] getDesc=[" + players.get(i).getDesc() + "]");
            }
        }
        playerList.add("");  //blank as the last name (default)
        Log.w(TAG, "players:" + playerList.toString());

        List<String> p1List = new ArrayList<>(playerList);
        ArrayAdapter<String> dataAdapterP1 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, p1List);
        dataAdapterP1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mSpinner_P1.setSelection(0);
        mSpinner_P1.setAdapter(dataAdapterP1);

        List<String> p2List = new ArrayList<>(playerList);
        final ArrayAdapter<String> dataAdapterP2 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, p2List);
        dataAdapterP2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mSpinner_P2.setSelection(1);
        mSpinner_P2.setAdapter(dataAdapterP2);

        List<String> p3List = new ArrayList<>(playerList);
        final ArrayAdapter<String> dataAdapterP3 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, p3List);
        dataAdapterP3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //mSpinner_P3.setSelection(2);
        mSpinner_P3.setAdapter(dataAdapterP3);

        List<String> p4List = new ArrayList<>(playerList);
        final ArrayAdapter<String> dataAdapterP4 = new ArrayAdapter<>(this,
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
                Log.v(TAG, "mSpinner_P1 onItemSelected mSpinner_P1_selection:" + mSpinner_P1_selection);
                mSpinner_P2_selection = "";
                mSpinner_P3_selection = "";
                mSpinner_P4_selection = "";
                //When P1 is selected, re-arrange P2/3/4 lists to remove P1 selection.
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
                Log.v(TAG, "mSpinner_P2 onItemSelected mSpinner_P2_selection:" + mSpinner_P2_selection);
                mSpinner_P3_selection = "";
                mSpinner_P4_selection = "";
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
                Log.v(TAG, "mSpinner_P3 onItemSelected mSpinner_P3_selection:" + mSpinner_P3_selection);
                mSpinner_P4_selection = "";
                if(!mSingles) rearrangeDropdownList(mSpinner_P4, dataAdapterP4, playerList);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {  }
        });


        ArrayAdapter<Integer> scoreAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, scoreList);
        scoreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner_T1_1.setAdapter(scoreAdapter);
        mSpinner_T2_1.setAdapter(scoreAdapter);
        mSpinner_T1_1.setSelection(0);
        mSpinner_T2_1.setSelection(0);
    }

    /*
    private void rearrangeDropdownList(Spinner spinner, ArrayAdapter<String> adapter, List<String> players) {
        Log.v(TAG, "rearrangeDropdownList:" + mSpinner_P1_selection + "/" + mSpinner_P2_selection + "/" + mSpinner_P3_selection + "/" + mSpinner_P4_selection);
        adapter.clear();
        //Collections.sort(players);  //sorted already so that players present on the court comes first.
        adapter.addAll(players);
        if (spinner == mSpinner_P2) {
            //if spinner for player 2, remove player 1 selection & set first in the list
            if(!mSpinner_P1_selection.isEmpty()) adapter.remove(mSpinner_P1_selection);
            spinner.setSelection(adapter.getCount()-1);
            mSpinner_P2_selection = spinner.getItemAtPosition(0).toString();
        } else if (spinner == mSpinner_P3) {
            //if spinner for player 3, remove player 1 & 2 selections & set first in the list
            if(!mSpinner_P1_selection.isEmpty()) adapter.remove(mSpinner_P1_selection);
            if(!mSpinner_P2_selection.isEmpty()) adapter.remove(mSpinner_P2_selection);
            spinner.setSelection(adapter.getCount()-1);
            mSpinner_P3_selection = spinner.getItemAtPosition(0).toString();
        } else if (spinner == mSpinner_P4) {
            //if spinner for player 3, remove player 1 & 2 selections & set first in the list
            if(!mSpinner_P1_selection.isEmpty()) adapter.remove(mSpinner_P1_selection);
            if(!mSpinner_P2_selection.isEmpty()) adapter.remove(mSpinner_P2_selection);
            if(!mSpinner_P3_selection.isEmpty()) adapter.remove(mSpinner_P3_selection);
            spinner.setSelection(adapter.getCount()-1);
            mSpinner_P4_selection = spinner.getItemAtPosition(0).toString();
        }
        Log.i(TAG, "rearrangeDropdownList Done:" + mSpinner_P1_selection + "/" + mSpinner_P2_selection + "/" + mSpinner_P3_selection + "/" + mSpinner_P4_selection);
        adapter.notifyDataSetChanged();
    }
    */

    private void fetchGames(){
        mGameNum = 1;
        DatabaseReference dbRef = mDatabase.child(mClub).child(Constants.JOURNAL).child(mInnings).child(mRoundName).child(mGroup);
        Query myQuery = dbRef.orderByKey();
        final ArrayList<GameJournalDBEntry> gameList= new ArrayList<>();
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    GameJournalDBEntry jEntry = child.getValue(GameJournalDBEntry.class);
                    if(null==jEntry) continue;
                    gameList.add(jEntry);
                    Log.d(TAG, "fetchGames:" + jEntry.toReadableString());
                }
                checkData(gameList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ClubLeagueEnterData.this, "DB error while fetching games: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
                killActivity();
            }
        });
    }

    private void checkData(ArrayList<GameJournalDBEntry> gameList) {

        if (!enterData(true))  //dry run returned failure
            return;

        //check player data, if the same team is being repeated
        String p1 = mSpinner_P1.getSelectedItem().toString();
        String p3 = mSpinner_P3.getSelectedItem().toString();
        String p2 = "";
        String p4 = "";

        if (p1.isEmpty() || p3.isEmpty()) {
            Toast.makeText(ClubLeagueEnterData.this, "Bad data!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mSingles) {
            p2 = mSpinner_P2.getSelectedItem().toString();
            p4 = mSpinner_P4.getSelectedItem().toString();
            if (p2.isEmpty() || p4.isEmpty()) {
                Toast.makeText(ClubLeagueEnterData.this, "Bad data!", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        int numOfSingles = 0;
        int numOfDoubles = 0;
        for (GameJournalDBEntry games : gameList) {
            if (Constants.SINGLES.equals(games.getmGT()))
                numOfSingles++;
            else
                numOfDoubles++;
        }
        Log.d(TAG, "checkData: singles=" + numOfSingles + "doubles=" + numOfDoubles);

        for (GameJournalDBEntry games : gameList) {
            if (!games.getmGT().equals(mGameType)) continue;

            if (games.playedBefore(p1, p2, p3, p4)) {
                if (mSingles)
                    showAlert(p1 + " has already played against " + p3 + " today!");
                else {  //doubles
                    String alertStr = "";
                    if ( games.getPlayerPartner(p1).equalsIgnoreCase(p2) )
                        alertStr = p1 + " and " + p2 + " have already played as a team today!";
                    if ( games.getPlayerPartner(p3).equalsIgnoreCase(p4) )
                        alertStr += "\n" + p3 + " and " + p4 + " have already played as a team today!";
                    showAlert(alertStr);
                }
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
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueEnterData.this);
        builder.setTitle(SharedData.getInstance().getColorString("Really?", Color.RED));
        builder.setMessage(message + "\n\nWould be nice if you can play all combinations in your group, before repeating." +
                "\n\nYou still want to enter this score?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }


    @Override
    protected boolean enterData(boolean dry_run) {
        if(-1 == SharedData.getInstance().mInningsDBKey) {
            Toast.makeText(ClubLeagueEnterData.this, "No current innings, Create innings first!", Toast.LENGTH_SHORT).show();
            return false;
        }
        String p1 = mSpinner_P1.getSelectedItem().toString();
        String p3 = mSpinner_P3.getSelectedItem().toString();
        String p2 = "";
        String p4 = "";


        if (!mSingles) {
            p2 = mSpinner_P2.getSelectedItem().toString();
            p4 = mSpinner_P4.getSelectedItem().toString();
        }

        Integer s1 = (Integer) mSpinner_T1_1.getSelectedItem();
        Integer s2 = (Integer) mSpinner_T2_1.getSelectedItem();
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

        if ((winningScore < 21) || s1.equals(s2) || winners.equals(losers)) {
            Toast.makeText(ClubLeagueEnterData.this, "Bad data!", Toast.LENGTH_SHORT).show();
            return false;
        }

        //Do not proceed to do tha actual DB update, if this is a dry run.
        if (dry_run) return true;

        Log.i(TAG, "enterData: " + winners + " vs " + losers + " : " + winningScore.toString() + "-" + losingScore.toString());
        mGamePlayers = null;
        mGamePlayers = new GamePlayers(winner1, winner2, loser1, loser2, winners, winningScore, losingScore);


        lockAndUpdateDB();
/*

        if(null!=mProgressDialog) return false;   //attempt to press Enter button repeatedly
        mProgressDialog = new ProgressDialog(ClubLeagueEnterData.this);
        mProgressDialog.setMessage("Updating database....");
        mProgressDialog.show();
        //To avoid a conflict when 2 users are entering the same score.
        //Without lock, inconsistency is seen: missing journal entry or points are not added.
        SharedData.getInstance().acquireDBLock();
        //Give some time for all other threads (firebase DB updates) to catch up.
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "After DB lock wait...");
                updateDB(gamePlayers);
            }
        }, 2000);
        */
        return true;
    }

    protected void workToUpdateDB() {
        Log.d(TAG, "workToUpdateDB: " + mCommon.mClub);

        final String winner1 = mGamePlayers.winner1;
        final String winner2 = mGamePlayers.winner2;
        final String loser1 = mGamePlayers.loser1;
        final String loser2 = mGamePlayers.loser2;

        if (winner1.isEmpty()) {
            Toast.makeText(ClubLeagueEnterData.this, "winner name is empty!", Toast.LENGTH_LONG).show();
            killActivity();
        }

        Snackbar.make(findViewById(R.id.enterdata_ll), mGamePlayers.winners + " won!", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

        GameJournalDBEntry jEntry = new GameJournalDBEntry(mRoundName, mInnings, SharedData.getInstance().mUser);
        jEntry.setResult(SharedData.getInstance().createNewRoundName(false, ClubLeagueEnterData.this),
                mGameType, winner1, winner2, loser1, loser2, mGamePlayers.winningScore, mGamePlayers.losingScore);
        jEntry.setmGNo(mGameNum);
        DatabaseReference jDBEntryRef = mDatabase.child(mClub).child(Constants.JOURNAL).child(mInnings).child(mRoundName).child(mGroup).push();
        jDBEntryRef.setValue(jEntry);
        Log.i(TAG, "WRITTEN jEntry: " + jEntry.toReadableString());
        //updateDB(winner1, winner2, loser1, loser2);
        //mDatabase.child(mClub).child(Constants.INNINGS).child(SharedData.getInstance().mInningsDBKey.toString()).child("round").setValue(mRoundName);
        //SharedData.getInstance().mRoundName = mRoundName;
        //Log.d(TAG, "WRITTEN mRoundName: " + mRoundName + " data=" + SharedData.getInstance().toString());

        DatabaseReference mClubDBRef = mDatabase.child(mClub);
        DatabaseReference dbRef_winner1 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(winner1);
        dbRef_winner1.addListenerForSingleValueEvent(new UpdateScores(ClubLeagueEnterData.this, mSingles, true, dbRef_winner1, false,true));
        DatabaseReference dbRef_loser1 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(loser1);
        dbRef_loser1.addListenerForSingleValueEvent(new UpdateScores(ClubLeagueEnterData.this, mSingles, false, dbRef_loser1, false,false));
        if (!mSingles) {
            DatabaseReference dbRef_winner2 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(winner2);
            dbRef_winner2.addListenerForSingleValueEvent(new UpdateScores(ClubLeagueEnterData.this, mSingles, true, dbRef_winner2, false,true));
            DatabaseReference dbRef_loser2 = mClubDBRef.child(Constants.GROUPS).child(mGroup).child(loser2);
            dbRef_loser2.addListenerForSingleValueEvent(new UpdateScores(ClubLeagueEnterData.this, mSingles, false, dbRef_loser2, false,false));
        }

        releaseLockAndCleanup();
    }

    class GamePlayers {
        private String winner1, winner2;
        private String loser1, loser2;
        private String winners;
        private int winningScore, losingScore;

        public GamePlayers(String winner1, String winner2, String loser1, String loser2, String winners, int winningScore, int losingScore) {
            this.winner1 = winner1;
            this.winner2 = winner2;
            this.loser1 = loser1;
            this.loser2 = loser2;
            this.winners = winners;
            this.winningScore = winningScore;
            this.losingScore = losingScore;
        }
    }

}


