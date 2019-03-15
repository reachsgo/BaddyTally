package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//EnterData for Single Elimination & Double Elimination tournaments
public class TournaSEDEEnterData extends BaseEnterData {
    private static final String TAG = "TournaSEDEEnterData";
    private String mTournaType;
    private String mMatchId;
    private String mFixtureLabel;
    private TournaFixtureDBEntry mMatchDBEntry;
    private Boolean mViewOnly;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tourna_activity_enter_data);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.d(TAG, "onCreate: ");
        onCreateBase();
    }



    private void prepareForInput() {
        Log.i(TAG, "prepareForInput tid=" + Thread.currentThread().getId());
        initializeSpinners();
        populateGamePoints(mGameList);
        if (mViewOnly) makeItViewOnly();
    }

    @Override
    protected void onCreateExtra() {
        Log.d(TAG, "onCreateExtra: ");
        mSingles = false;


        Intent thisIntent = getIntent(); // gets the previously created intent
        mTournaType = thisIntent.getStringExtra(Constants.TOURNATYPE);
        mMatchId = thisIntent.getStringExtra(Constants.MATCH);
        mFixtureLabel = thisIntent.getStringExtra(Constants.FIXTURE);
        mTeams = thisIntent.getStringArrayListExtra(Constants.TEAMS);
        mT1_players = thisIntent.getStringArrayListExtra(Constants.TEAM1PLAYERS);
        mT2_players = thisIntent.getStringArrayListExtra(Constants.TEAM2PLAYERS);
        String extras = thisIntent.getStringExtra(Constants.EXTRAS);
        mViewOnly = false;
        if (null != extras && !extras.isEmpty() && extras.equals(Constants.VIEWONLY)) {
            mViewOnly = true;
            Log.d(TAG, "onCreateExtra: mViewOnly");
        }

        //Winner-spinner is set from isMatchDone() invoked from spinner callbacks (onItemSelected).
        //So, initialize that first.
        mSpinner_W = findViewById(R.id.winner);
        mSpinner_W_selection = "";
        mSpinner_Teams = new ArrayList<>(mTeams);
        mSpinner_Teams.add(0, "none");
        //Log.d(TAG, "onCreateExtra: mSpinner_Teams=" + mSpinner_Teams);
        ArrayAdapter<String> winnerAdapter = new ArrayAdapter<>(this,
                R.layout.small_spinner, mSpinner_Teams);
        winnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner_W.setAdapter(winnerAdapter);
        mSpinner_W.setSelection(0);

        if (mTeams != null && mTeams.size() == 2) {
            TextView header = findViewById(R.id.enterdata_matchinfo);
            String title = mTeams.get(0) + " vs " + mTeams.get(1);
            header.setText(title);
            ((TextView) findViewById(R.id.team1_tv)).setText(mTeams.get(0));
            ((TextView) findViewById(R.id.team2_tv)).setText(mTeams.get(1));
        }

        if (mT1_players.size() == 1 || mT1_players.get(1).isEmpty()) {
            //Only one player in the team. Singles match.
            findViewById(R.id.spinner_p2_rl).setVisibility(View.GONE);
            findViewById(R.id.spinner_p4_rl).setVisibility(View.GONE);
            mSingles = true;
        }



        Log.w(TAG, "onCreateExtra :" + mTournaType + "/" + mMatchId + "/"
                + mFixtureLabel + "/" + mTeams + "/"
                + mT1_players + "/" + mT2_players);



        if (mMatchId.isEmpty() || mFixtureLabel.isEmpty()) {
            Toast.makeText(TournaSEDEEnterData.this, "Match ID or Label not available!",
                    Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "onCreateExtra: invoking fetchGames");
            fetchGames(mMatchId, true);
        }
    }



/*
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
            /*
            if (!mSpinner_P1_selection.isEmpty()) adapter.remove(mSpinner_P1_selection);
            if (!mSpinner_P2_selection.isEmpty()) adapter.remove(mSpinner_P2_selection);
            spinner.setSelection(adapter.getCount() - 1);
            mSpinner_P3_selection = spinner.getItemAtPosition(0).toString();*/ /*
        } else if (spinner == mSpinner_P4) {
            //if spinner for player 3, remove player 1 & 2 selections & set first in the list
            /*
            if (!mSpinner_P1_selection.isEmpty()) adapter.remove(mSpinner_P1_selection);
            if (!mSpinner_P2_selection.isEmpty()) adapter.remove(mSpinner_P2_selection);*/ /*
            if (!mSpinner_P3_selection.isEmpty()) adapter.remove(mSpinner_P3_selection);
            spinner.setSelection(adapter.getCount() - 1);
            mSpinner_P4_selection = spinner.getItemAtPosition(0).toString();
        }
        Log.i(TAG, "rearrangeDropdownList Done:" + mSpinner_P1_selection + "/" +
                mSpinner_P2_selection + "/" + mSpinner_P3_selection + "/" + mSpinner_P4_selection);
        adapter.notifyDataSetChanged();
    }
    */


    //SGO: NOTE:
    // "The Firebase Database client performs all network and disk operations off the main thread.
    //  The Firebase Database client invokes all callbacks to your code on the main thread.
    //  So network and disk access for the database are no reason to spin up your own threads or use
    //  background tasks. But if you do disk, network I/O or CPU intensive operations in the callback,
    //  you might need to perform those off the main thread yourself."
    //So, even if fetchGames() is invoked in a worker thread, firebase call back onDataChange()
    //will be invoked in the main thread.

    private void fetchGames(final String matchId, final Boolean reinit) {

        Log.i(TAG, "fetchGames(" + Thread.currentThread().getId() + ")  matchId=" +
                matchId + " mMatchId=" + mMatchId);
        DatabaseReference dbRef = mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.MATCHES).child(mFixtureLabel)
                .child(matchId);   //MATCH data

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "fetchGames:" + dataSnapshot.getKey());
                GenericTypeIndicator<List<GameJournalDBEntry>> genericTypeIndicator =
                        new GenericTypeIndicator<List<GameJournalDBEntry>>() {
                        };
                List<GameJournalDBEntry> games = dataSnapshot.getValue(genericTypeIndicator);
                if (games == null) {
                    if (reinit) mGameList = new ArrayList<>();
                    isMatchDone(mGameList, true); //reset if the winner was already set
                } else {
                    mGameList = new ArrayList<>(games);
                    isMatchDone(mGameList, true);
                }
                prepareForInput();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TournaSEDEEnterData.this, "DB error while fetching games: "
                                + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
                mCommon.killActivity(TournaSEDEEnterData.this, RESULT_OK);
            }
        });

        dbRef = mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(mFixtureLabel)
                .child(matchId);  //FIXTURE data

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                TournaFixtureDBEntry dbEntry = dataSnapshot.getValue(TournaFixtureDBEntry.class);
                if (dbEntry == null) return;
                mMatchDBEntry = dbEntry;  //rewrite member data only if a valid match entry.
                //Incase of DE_FINALS_M2, <tourna>/F-1/2 is created from <tourna>/F-1/1
                //and hence DE_FINALS_M1 match entry should not be overwritten by null before that.
                Log.d(TAG, "fetchGames:" + dataSnapshot.getKey() + " " + mMatchDBEntry.toString());

                mMatchId = matchId;  //imp to set DE_FINALS_M2 here, if present in DB
                if (mMatchDBEntry.isThereAWinner(true)) {
                    String winner = mMatchDBEntry.getW();
                    Log.i(TAG, Thread.currentThread().getId() + " set Winner!");
                    if (mTeams.get(0).equals(winner)) mSpinner_W.setSelection(1);
                    else if (mTeams.get(1).equals(winner)) mSpinner_W.setSelection(2);
                    CheckBox checkbox = findViewById(R.id.completed);
                    checkbox.setChecked(true);
                    if (!mCommon.isRoot() && !mViewOnly) {
                        makeItViewOnly();
                        Toast.makeText(TournaSEDEEnterData.this, "Match already completed!",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TournaSEDEEnterData.this, "DB error while fetching games: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
                mCommon.killActivity(TournaSEDEEnterData.this, RESULT_OK);
            }
        });
    }

    private void populateGamePoints(ArrayList<GameJournalDBEntry> gameList) {
        //Log.i(TAG, "populateGamePoints tid=" + Thread.currentThread().getId());
        if (gameList == null) return;
        int num = 1;
        for (GameJournalDBEntry g : gameList) {
            if (g.getmWS() == 0) continue;
            //Don't have to check if playerInvolved(), as the match is added in DB for the players taken from DB.
            //Check if T1 is the winning team or losing team
            for (String p : mT1_players) {
                //Log.d(TAG, "populateGamePoints: " + g.toReadableString() + " T1 player=" + p);
                if (g.aWinner(p)) {
                    //get Winner's score as T1's score
                    setGamePointSpinner(g.getmGNo(), g.getmWS(), g.getmLS());
                    break;
                } else if (g.aLoser(p)) {
                    //get Winner's score as T1's score
                    setGamePointSpinner(g.getmGNo(), g.getmLS(), g.getmWS());
                    break;
                }

            }
            num++;
        }
    }


    private Boolean isMatchDone(final ArrayList<GameJournalDBEntry> newGameList, final Boolean setWinner) {

        if (newGameList == null || newGameList.size() == 0) {
            //reset the checkbox and winner
            mSpinner_W.setSelection(0);
            CheckBox checkbox = findViewById(R.id.completed);
            checkbox.setChecked(false);
            return false;
        }

        final Integer TEAM1_IDX = 1;
        final Integer TEAM2_IDX = 2;
        int winner_team_idx = 0;
        String randomPlayerT1 = mT1_players.get(0);   //we will see if this player has won best-of-N games.
        String randomPlayerT2 = mT2_players.get(0);   //take one from the other team too.

        Log.i(TAG, "isMatchDone:" + randomPlayerT1 + " :" + randomPlayerT2);

        int randomPlayerT1_Wins = 0;
        int randomPlayerT2_Wins = 0;
        int gamesCompleted = 0;
        for (GameJournalDBEntry jEntry : newGameList) {
            if (jEntry.getmWS() < 21) continue;
            if (jEntry.getmWS() >= 21) gamesCompleted++;
            if (jEntry.aWinner(randomPlayerT1)) randomPlayerT1_Wins++;
            if (jEntry.aWinner(randomPlayerT2)) randomPlayerT2_Wins++;
        }
        if (gamesCompleted == 1) {
            if (randomPlayerT1_Wins == 1) winner_team_idx = TEAM1_IDX;
            else if (randomPlayerT2_Wins == 1) winner_team_idx = TEAM2_IDX;
            Log.i(TAG, randomPlayerT1_Wins + ":One game completed:" + randomPlayerT2_Wins);
        } else if (randomPlayerT1_Wins > (mBestOf / 2)) {
            winner_team_idx = TEAM1_IDX;
            //Log.i(TAG, "isMatchDone: " + randomPlayerT1 + "=" + randomPlayerT1_Wins + " > " + mBestOf / 2 + " winner=" + winner_team_idx);
        } else if (randomPlayerT2_Wins > (mBestOf / 2)) {
            winner_team_idx = TEAM2_IDX;
            //Log.i(TAG, "isMatchDone: " + randomPlayerT2 + "=" + randomPlayerT2_Wins + " > " + mBestOf / 2 + " winner=" + winner_team_idx);
        }

        if (winner_team_idx > 0) {
            Log.i(TAG, "isMatchDone: YEP:");

            if (mCommon.isRoot()) {
                //change the winner only during initial phase.
                //root is free to override the winner before clicking enter.
                if (setWinner) mSpinner_W.setSelection(winner_team_idx);
            } else {
                //for other users, winner selection cannot be overridden
                mSpinner_W.setSelection(winner_team_idx);
            }
            CheckBox checkbox = findViewById(R.id.completed);

            if (gamesCompleted == 1 && mBestOf == 1) {
                //If only 1 game completed, then Tick completed checkbox only if best-of-1
                //otherwise, its error prone (completed might be checked after completing game1 of best-of-3
                checkbox.setChecked(true);
            } else if (gamesCompleted > 1)//more than 1 game and there is a clear winner.
                checkbox.setChecked(true);
            return true;
        } else {
            Log.i(TAG, "isMatchDone: NOPE");
            mSpinner_W.setSelection(0);
            CheckBox checkbox = findViewById(R.id.completed);
            checkbox.setChecked(false);
        }

        return false;
    }

    @Override
    protected boolean enterData(boolean dry_run) {
        if (null == mGameList) return false;

        String p1 = mSpinner_P1.getSelectedItem().toString();
        String p3 = mSpinner_P3.getSelectedItem().toString();
        String p2 = "";
        String p4 = "";

        if (!mSingles) {
            p2 = mSpinner_P2.getSelectedItem().toString();
            p4 = mSpinner_P4.getSelectedItem().toString();
        }

        mGameList.clear();

        for (int gameNum = 1; gameNum <= mBestOf; gameNum++) {
            Integer s1 = 0;
            Integer s2 = 0;
            Spinner tmpS = getRespectiveSpinner(gameNum, 1);
            if (tmpS != null) s1 = (Integer) tmpS.getSelectedItem();
            tmpS = getRespectiveSpinner(gameNum, 2);
            if (tmpS != null) s2 = (Integer) tmpS.getSelectedItem();
            //Log.d(TAG, "enterData: " + gameNum + " " + s1 + "-" + s2);
            if (s1 == 0 && s2 == 0) continue;

            String winners, winner1, winner2;
            String losers, loser1, loser2;
            Integer winningScore;
            Integer losingScore;

            if (s1 > s2) {
                if (mSingles) {
                    winners = p1;
                    winner1 = p1;
                    winner2 = "";
                    losers = p3;
                    loser1 = p3;
                    loser2 = "";
                } else {
                    winners = p1 + "/" + p2;
                    winner1 = p1;
                    winner2 = p2;
                    losers = p3 + "/" + p4;
                    loser1 = p3;
                    loser2 = p4;
                }
                winningScore = s1;
                losingScore = s2;
            } else {
                if (mSingles) {
                    winners = p3;
                    winner1 = p3;
                    winner2 = "";
                    losers = p1;
                    loser1 = p1;
                    loser2 = "";
                } else {
                    winners = p3 + "/" + p4;
                    winner1 = p3;
                    winner2 = p4;
                    losers = p1 + "/" + p2;
                    loser1 = p1;
                    loser2 = p2;
                }
                winningScore = s2;
                losingScore = s1;
            }

            //Log.d(TAG, "enterData: " + winners);
            if (winners.equals(losers)) {
                Toast.makeText(TournaSEDEEnterData.this, "Players configured with same names: " + winners,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            if ((winningScore<21 && losingScore!=0) || s1.equals(s2)) {
                if (!dry_run)
                    Toast.makeText(TournaSEDEEnterData.this, "Game" + gameNum + ": Bad data!", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (!mCommon.isDBConnected()) {
                if (!dry_run)
                    Toast.makeText(TournaSEDEEnterData.this, "Stale DB connection, retry.", Toast.LENGTH_SHORT).show();
                mCommon.wakeUpDBConnection();
                return false;
            }
            String dateStr = SharedData.getInstance().createNewRoundName(false, null);
            GameJournalDBEntry jEntry = new GameJournalDBEntry(dateStr, "", mCommon.mUser);
            jEntry.setResult(dateStr, Constants.DOUBLES, winner1, winner2, loser1, loser2, winningScore, losingScore);
            jEntry.setmGNo(gameNum);
            mGameList.add(jEntry);

        }

        isMatchDone(mGameList, dry_run);  //override Winner only for dry_run, for root user.

        //Do not proceed to do tha actual DB update, if this is a dry run.
        if (dry_run) return true;


        if (mGameList.size() > 0) {
            lockAndUpdateDB();
        } else
            mCommon.killActivity(TournaSEDEEnterData.this, RESULT_OK);

        return true;
    }

   @Override
    protected void workToUpdateDB() {
        Log.d(TAG, "workToUpdateDB: " + mCommon.mClub);
        Log.d(TAG, "workToUpdateDB: " + mFixtureLabel + " mId=" + mMatchId);
        Log.d(TAG, "workToUpdateDB: " + mGameList.toString());
        final DatabaseReference dbRef = mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.MATCHES).child(mFixtureLabel)
                .child(mMatchId);
        dbRef.setValue(mGameList);
        mCommon.setDBUpdated(true);

        CheckBox checkbox = findViewById(R.id.completed);
        String winner = mSpinner_W.getSelectedItem().toString();
        Log.d(TAG, "workToUpdateDB: " + mMatchId + " W=" + winner + " checked=" + checkbox.isChecked());
        if (checkbox.isChecked() && !winner.isEmpty()) {
            Log.d(TAG, "workToUpdateDB: before=" + mMatchDBEntry);
            //before=TournaFixtureDBEntry{T=[new4, new5], P=[, ], E=[Ext{fixL.0-2.true}], W=null}
            //SGO: If a null pointer exception happens while holding the DB lock,
            //DBlock will remain in the DB. Example: if mMatchDBEntry is null here.
            //Thus max DB lock period of 3 mins was implemented (refer SharedData.mDBLockedTime)
            if (null == mMatchDBEntry) {
                Log.e(TAG, "workToUpdateDB: mMatchDBEntry=null, mMatchId=" + mMatchId);
                releaseLockAndCleanup();
                return;
            }
            mMatchDBEntry.setT1(true, mTeams.get(0));
            mMatchDBEntry.setT2(true, mTeams.get(1));
            mMatchDBEntry.setW(winner);
            Log.d(TAG, "workToUpdateDB: after=" + mMatchDBEntry);
            //after=TournaFixtureDBEntry{T=[new4, new5], P=[, ], E=[Ext{fixL.0-2.true}], W=new4}

            mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
                    .child(mCommon.mTournament).child(mFixtureLabel).child(mMatchId)
                    .setValue(mMatchDBEntry);

            if (mFixtureLabel.equals(Constants.DE_FINALS)) {  //DE Finals
                //DE Finals: Check if team1 (from UB) lost. If yes, then there is one more set of games
                if (!mMatchId.equals(Constants.DE_FINALS_M2) && winner.equals(mTeams.get(1))) {
                    //This is not the second set of finals and the winner is not the UB-winner
                    mAlertTitle = "";
                    mAlertMsg = "";
                    mFinishActivity = false;

                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TournaSEDEEnterData.this);
                    alertBuilder.setTitle("");
                    alertBuilder.setMessage(mTeams.get(0) + " lost for the first time in this tournament. Play again!");
                    alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mMatchId = Constants.DE_FINALS_M2;
                            mGameList = null;
                            TournaFixtureDBEntry dbEntry = new TournaFixtureDBEntry(mMatchDBEntry);
                            dbEntry.setW("");
                            mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
                                    .child(mCommon.mTournament).child(mFixtureLabel).child(mMatchId)
                                    .setValue(dbEntry);
                            Log.d(TAG, "onClick: " + mMatchId + " created.");
                            //Fixture entry in DB should be there before fetching games
                            fetchGames(mMatchId, true);
                        }
                    });
                    alertBuilder.show();
                    releaseLockAndCleanup();
                    return;
                } else {
                    mAlertTitle = winner + " won!";
                    mAlertMsg = "";
                    mFinishActivity = true;
                    releaseLockAndCleanup();
                    return;
                }
            }

            //not the DE final match, but a final match of UB or LB
            mAlertTitle = winner + " won!";
            if (mMatchDBEntry.isExternalLink(0) && mMatchDBEntry.getF()) {
                //This is the final match and there is an external link (DE tournament)
                //So, this is the upper bracket. Create Final1 fixture.
                TournaFixtureDBEntry deFinalsDBEntry = new TournaFixtureDBEntry();
                deFinalsDBEntry.setT1(true, winner);
                mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
                        .child(mCommon.mTournament)
                        .child(Constants.DE_FINALS).child(Constants.DE_FINALS_M1)
                        .setValue(deFinalsDBEntry);
                mAlertTitle = "Finals";
                mAlertMsg =
                        "'" + winner + "' won this bracket.\n\n" +
                                "Final of the double elimination tournament should be played between '" + winner +
                                "' and winner of the lower bracket final match.\n\n" +
                                "If '" + winner + "' loses in the initial set (best-of-1 or best-of-3), " +
                                "there should be another final match set between the same teams!";
            } else if (mMatchDBEntry.getF()) {
                //This is the final match and there is an external link (DE tournament)
                //So, this is the lower bracket. update team2 of DE final1
                //DB should be read before updating, as the first team is not known here.
                //Other option was to set the 2nd team using absolute path like <tourna>/F-1/1/t/1
                //But that is not maintainable code if the structure of MatchDBEntry changes.
                updateDEFinalsTeam2(mMatchDBEntry, winner);
            }

            propogateTheWinner(mFixtureLabel, mMatchId, winner);

            //setExternalLink();  //SGO
            if (mMatchDBEntry.getExtLinkSrcFlag(0)) {
                //There is an External Link and this is the source flag. Thus, the external link needs to be
                //followed and its winner needs to be set to the loser of this match.
                //EXTERNALLEAF in lower bracket has team1 as link and team2 as Bye.
                String extLinkLabel = mMatchDBEntry.getExtLinkLabel(0);
                String extLinkMatchId = mMatchDBEntry.getExtLinkMatchId(0);
                mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
                        .child(mCommon.mTournament).child(extLinkLabel)
                        .child(extLinkMatchId).child("w")
                        .setValue(mMatchDBEntry.getLoser(true));  //set the winner
                mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
                        .child(mCommon.mTournament).child(extLinkLabel)
                        .child(extLinkMatchId).child("t").child("0")
                        .setValue(mMatchDBEntry.getLoser(true));  //set team1. team2 is bye

                // fixL 0-4 (winner) -> 1-4 (corresponding team name needs to be updated)
                propogateTheWinner(extLinkLabel, extLinkMatchId, mMatchDBEntry.getLoser(true));
            }

            mFinishActivity = true;
        } else {
            //Match not completed; no winner yet
            //May be entries for 1 or 2 games were added (out of best-of-3)
            mFinishActivity = true;
        }

        releaseLockAndCleanup();
    }

    private void propogateTheWinner(final String fixLabel, final String matchId, final String winner) {
        if (fixLabel.equals(Constants.DE_FINALS)) {
            return;
        }

        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                .child(Constants.TOURNA).child(mCommon.mTournament).child(fixLabel);
        Log.d(TAG, "propogateTheWinner: " + fixLabel + matchId + winner);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<Map<String, TournaFixtureDBEntry>> genericTypeIndicator =
                        new GenericTypeIndicator<Map<String, TournaFixtureDBEntry>>() {
                        };
                Map<String, TournaFixtureDBEntry> map = dataSnapshot.getValue(genericTypeIndicator);
                if (null == map) return;
                for (Map.Entry<String, TournaFixtureDBEntry> entry : map.entrySet()) {
                    String mId = entry.getKey();
                    TournaFixtureDBEntry dbEntry = entry.getValue();
                    //Check if the previous link is same as the match just completed.
                    //If yes, update the team name in DB
                    if (matchId.equals(dbEntry.getPr1(true))) {
                        //prev1 is the match which just completed. Update team1.
                        dbEntry.setW("");  //reset winner; This might be root doing a correction.
                        dbEntry.setT1(true, winner);
                        dbEntry.setWinnerString();  //If one is bye, set the other as winner
                        dbRef.child(mId).setValue(dbEntry);
                        Log.w(TAG, "propogateTheWinner(team1):" + mId + "=" + dbEntry.toString());
                    } else if (matchId.equals(dbEntry.getPr2(true))) {
                        //prev2 is the match which just completed. Update team2.
                        dbEntry.setW("");  //reset winner; This might be root doing a correction.
                        dbEntry.setT2(true, winner);
                        dbEntry.setWinnerString(); //If one is bye, set the other as winner
                        dbRef.child(mId).setValue(dbEntry);
                        Log.w(TAG, "propogateTheWinner(team2):" + mId + "=" + dbEntry.toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "readDB:onCancelled", databaseError.toException());
                Toast.makeText(TournaSEDEEnterData.this, "DB error while updating DB: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateDEFinalsTeam2(final TournaFixtureDBEntry dbEntry, final String winner) {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                .child(Constants.TOURNA).child(mCommon.mTournament)
                .child(Constants.DE_FINALS).child(Constants.DE_FINALS_M1);
        Log.d(TAG, "updateDEFinalsTeam2: " + dbEntry.toString() + " " + winner);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "updateDEFinalsTeam2: onDataChange: " + dataSnapshot.getKey());
                TournaFixtureDBEntry deFinalsDBEntry = dataSnapshot.getValue(TournaFixtureDBEntry.class);
                if (null == deFinalsDBEntry) return;

                deFinalsDBEntry.setT2(true, winner);
                dbRef.setValue(deFinalsDBEntry);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "updateDEFinalsTeam2:onCancelled", databaseError.toException());
                Toast.makeText(TournaSEDEEnterData.this, "DB error while updating Finals: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void makeItViewOnly() {
        findViewById(R.id.enter_button).setVisibility(View.GONE);
        findViewById(R.id.score_t1_1).setEnabled(false);
        findViewById(R.id.score_t1_2).setEnabled(false);
        findViewById(R.id.score_t1_3).setEnabled(false);
        findViewById(R.id.score_t2_1).setEnabled(false);
        findViewById(R.id.score_t2_2).setEnabled(false);
        findViewById(R.id.score_t2_3).setEnabled(false);
        findViewById(R.id.winner).setEnabled(false);
        findViewById(R.id.completed).setEnabled(false);
    }
}


