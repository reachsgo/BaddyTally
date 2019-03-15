package com.sg0.baddytally;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class TournaLeagueEnterData extends BaseEnterData implements CallbackRoutine {


    private static final String TAG = "TournaLeagueEnterData";
    private String mGameType;
    private Integer mNumOfMatches;
    private Integer mBestOf;
    private MatchInfo mChosenMatch;
    private String mSelectedMatch;
    private HashMap<String, PlayerInfo> mDBPlayerData;
    private HashMap<String, TeamScoreDBEntry> mDBTeamScoreData;
    private int mCount;
    private String mWinnerPlayer1;
    private String mWinnerTeam;
    private TournaUtil mTUtil;
    private ArrayList<GameJournalDBEntry> mNewGameList;
    private ArrayList<GameJournalDBEntry> mDeltaGameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tourna_activity_enter_data);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.d(TAG, "onCreate: ");
        onCreateBase();
    }


    @Override
    protected void onCreateExtra() {
        Log.d(TAG, "onCreateExtra: ");
        Intent myIntent = getIntent(); // gets the previously created intent
        mGameType = myIntent.getStringExtra("gametype");
        mSingles = Constants.SINGLES.equals(mGameType);
        Log.w(TAG, "onCreate :" + mCommon.toString() + "/" + mGameType);

        //findViewById(R.id.winner).setVisibility(View.GONE);
        //findViewById(R.id.winner_tv).setVisibility(View.GONE);

        mDBPlayerData = new HashMap<>();
        mDBTeamScoreData = new HashMap<>();
        mTUtil = null;
        mTUtil = new TournaUtil(TournaLeagueEnterData.this, TournaLeagueEnterData.this);
        mTUtil.readDBMatchMeta(mCommon.mTournament, true);
        mTeams = mCommon.mTeams;
        mNewGameList = null;
        mDeltaGameList = null;

        mSpinner_W = findViewById(R.id.winner);
        mSpinner_W_selection = "";
        mSpinner_Teams = new ArrayList<>(mTeams);
        mSpinner_Teams.add(0, "none");
        Log.d(TAG, "onCreateExtra: mSpinner_Teams=" + mSpinner_Teams);
        ArrayAdapter<String> winnerAdapter = new ArrayAdapter<>(this,
                R.layout.small_spinner, mSpinner_Teams);
        winnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner_W.setAdapter(winnerAdapter);
        mSpinner_W.setSelection(0);

        //winner spinner and completed checkbox are auto filled
        //and cannot be over-ridden for league matches.
        findViewById(R.id.completed).setEnabled(false);
        findViewById(R.id.winner).setEnabled(false);

        Log.w(TAG, "onCreateExtra :" + mTeams + "/"
                + mT1_players + "/" + mT2_players);

        ((TextView)findViewById(R.id.enterdata_matchinfo)).setText("");
        //teams not known yet. Fill this in once the match to enter data is chosen.


    }

    private void showMatchGames(final String matchkey) {
        //matchkey of the format "MS2> TEAM01 vs TEAM02", where 2 is the match-set id in DB
        Log.v(TAG, "showMatchGames, matchkey:" + matchkey + " mNumOfMatches:" + mNumOfMatches);
        if (mNumOfMatches == 1) {
            readPlayers(matchkey);
            mSelectedMatch = Constants.MATCHID_PREFIX + "1";
            Log.d(TAG, "showMatchGames: mSelectedMatch = " + mSelectedMatch);
            return;
        }
        Context wrapper = new ContextThemeWrapper(TournaLeagueEnterData.this, R.style.RegularPopup);
        final PopupMenu popup = new PopupMenu(wrapper, findViewById(R.id.enterdata_header));
        popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
        if (Build.VERSION.SDK_INT >= 23) {
            popup.setGravity(Gravity.END);
        }
        popup.getMenu().clear();
        Menu pMenu = popup.getMenu();
        final String DELIM = " : ";
        final String MATCHSTR = "Match";

        MatchInfo mInfo = TournaUtil.getMatchInfoFromString(matchkey);
        HashMap<String, Boolean> statusMap = mTUtil.mMatchesStatus.get(mInfo.key);
        if (statusMap == null) {
            Log.v(TAG, "showMatchGames, matchkey:" + mInfo.key + " got NULL for statusMap");
            return;
        }

        //strike out all the already-played matches in the popup.
        //This will make it easy for the user to choose a game
        //for(Map.Entry<String,Boolean> entry: statusMap.entrySet() ) {
        for (Integer matchNum = 1; matchNum <= mNumOfMatches; matchNum++) {
            String match = String.format(Locale.getDefault(), "%s%d", Constants.MATCHID_PREFIX, matchNum);
            Boolean status = statusMap.get(match);
            if (status == null) continue;

            SpannableStringBuilder possibleGames = new SpannableStringBuilder();
            String gameName = String.format(Locale.getDefault(), "%s%s%s%s", matchkey, DELIM, MATCHSTR, matchNum);
            Log.v(TAG, "showMatchGames, gameName:" + gameName);
            if (status) {
                possibleGames.append(SharedData.getInstance().getColorString(
                        SharedData.getInstance().getStrikethroughString(gameName), Color.GRAY));
                //Log.v(TAG, "showMatchGames, match:" + match + " is DONE");
            } else {
                possibleGames.append(gameName);
                //Log.v(TAG, "showMatchGames, match:" + match + " is to be played");
            }
            pMenu.add(possibleGames);
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Log.v(TAG, "showMatchGames:" + menuItem.getTitle().toString() + " " + menuItem.getItemId());
                String title = menuItem.getTitle().toString();
                TextView tv = findViewById(R.id.enterdata_matchinfo);
                tv.setText(title);
                String[] titleParts = title.split(DELIM);
                Log.v(TAG, "showMatchGames, titleParts:" + titleParts[1]);
                mSelectedMatch = titleParts[1].replace(MATCHSTR, Constants.MATCHID_PREFIX);
                Log.v(TAG, "showMatchGames, mSelectedMatch:" + mSelectedMatch);
                popup.dismiss();
                readPlayers(matchkey);
                return true;
            }
        });
        popup.show();//showing popup menu

    }

    private void readPlayers(final String matchkey) {
        Log.d(TAG, "readPlayers: " + matchkey);
        if (matchkey.isEmpty()) {
            Log.i(TAG, "readPlayers, matchkey is empty!");
            return;
        }
        MatchInfo mI = TournaUtil.getMatchInfoFromString(matchkey);

        if (mI == null) {
            Log.e(TAG, "readPlayers, Failed to retrieve match info!");
            mCommon.killActivity(TournaLeagueEnterData.this, RESULT_OK);
        }
        mChosenMatch = mI;
        fetchPlayers(1, mChosenMatch.T1);
        fetchPlayers(2, mChosenMatch.T2);
        String mInfo = mChosenMatch.T1 + " vs " + mChosenMatch.T2;
        ((TextView)findViewById(R.id.enterdata_matchinfo)).setText(mInfo);
        ((TextView) findViewById(R.id.team1_tv)).setText(mChosenMatch.T1);
        ((TextView) findViewById(R.id.team2_tv)).setText(mChosenMatch.T2);
        //Log.d(TAG, "readPlayers: done");
    }


    private void fetchPlayers(final int team_index, final String team) {
        //Log.d(TAG, "fetchPlayers: " + team);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.PLAYERS);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> tmpList = new ArrayList<>();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    final String player_short = childSnapshot.getKey();
                    PlayerInfo pI = childSnapshot.getValue(PlayerInfo.class);
                    //Log.v(TAG, "read from DB: " + pI.toString());
                    if (pI.T.equals(team)) {
                        String player = player_short + Constants.COLON_DELIM + pI.name;
                        tmpList.add(player);
                        //Log.i(TAG, player + " added to list");
                    }
                }
                if (tmpList.size() > 0) {
                    if (team_index == 1) mT1_players = tmpList;
                    else {
                        mT2_players = tmpList;
                        initializeSpinners();
                        fetchGames();
                        Log.d(TAG, "fetchPlayers: mT1_players=" + mT1_players +
                                        " mT2_players=" + mT2_players);
                    }
                } else {
                    Log.d(TAG, "onDataChange: No players!");
                    mCommon.showToast(TournaLeagueEnterData.this, "No players found for team " + team, Toast.LENGTH_SHORT);
                    mCommon.killActivity(TournaLeagueEnterData.this, RESULT_OK);
                }

            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mCommon.showToast(TournaLeagueEnterData.this, "DB error on read: " + databaseError.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }


    private void fetchGames() {
        //mGameNum = 1;
        Log.i(TAG, "fetchGames:" + mChosenMatch.key + "/" + mSelectedMatch);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.MATCHES).child(Constants.DATA)
                .child(mChosenMatch.key).child(mSelectedMatch);
        final ArrayList<GameJournalDBEntry> gameList = new ArrayList<>();
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Log.d(TAG, "fetchGames:" + dataSnapshot.getKey());
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    GameJournalDBEntry jEntry = child.getValue(GameJournalDBEntry.class);
                    if (null == jEntry) continue;
                    gameList.add(jEntry);
                    //Log.d(TAG, "fetchGames:" + jEntry.toReadableString());
                }
                populateGamePoints(gameList);
                Log.d(TAG, "fetchGames onDataChange: " + gameList.size());
                mGameList = gameList;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TournaLeagueEnterData.this, "DB error while fetching games: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
                mCommon.killActivity(TournaLeagueEnterData.this, RESULT_OK);
            }
        });
    }

    private void populateGamePoints(ArrayList<GameJournalDBEntry> gameList) {
        Log.d(TAG, "populateGamePoints: ");
        int num = 1;
        for (GameJournalDBEntry g : gameList) {
            if (g.getmWS() == 0) continue;
            //Don't have to check if playerInvolved(), as the match is added in DB for the players taken from DB.
            //Check if T1 is the winning team or losing team
            for (String p : mT1_players) {
                String p_nick = TeamInfo.getShortName(p); //p.split(Constants.COLON_DELIM)[0];  //T1 player=P02: Player 002
                Log.d(TAG, "populateGamePoints: " + g.toReadableString() + " T1 player=" + p);
                if (g.aWinner(p_nick)) {
                    //get Winner's score as T1's score
                    setGamePointSpinner(g.getmGNo(), g.getmWS(), g.getmLS());
                    break;
                } else if (g.aLoser(p_nick)) {
                    //get Winner's score as T1's score
                    setGamePointSpinner(g.getmGNo(), g.getmLS(), g.getmWS());
                    break;
                }

            }
            num++;
        }
        if (mBestOf == 1) {
            findViewById(R.id.score_t1_2).setEnabled(false);
            findViewById(R.id.score_t1_3).setEnabled(false);
            findViewById(R.id.score_t2_2).setEnabled(false);
            findViewById(R.id.score_t2_3).setEnabled(false);
        }
        if (!mCommon.isRoot() && isMatchDone(gameList)) {
            Toast.makeText(TournaLeagueEnterData.this, "Match already completed, nothing to update!",
                    Toast.LENGTH_LONG).show();
            findViewById(R.id.enter_button).setVisibility(View.GONE);
        }
        Log.d(TAG, "populateGamePoints: done");
    }

    @Override
    protected boolean enterData(boolean dry_run) {
        Log.d(TAG, "enterData:" + dry_run);
        if (mCommon.mTeams == null || mSelectedMatch.isEmpty()) {
            Toast.makeText(TournaLeagueEnterData.this, "Bad data!", Toast.LENGTH_SHORT).show();
            return false;
        }

        String tmp1 = mSpinner_P1.getSelectedItem().toString();
        String tmp3 = mSpinner_P3.getSelectedItem().toString();
        String p1 = TeamInfo.getShortName(tmp1);
        String p3 = TeamInfo.getShortName(tmp3);
        String p2 = "";
        String p4 = "";

        Log.d(TAG, "enterData:" + p1 + "," + p2);
        if (!mSingles) {
            String tmp2 = mSpinner_P2.getSelectedItem().toString();
            String tmp4 = mSpinner_P4.getSelectedItem().toString();
            p2 = TeamInfo.getShortName(tmp2);
            p4 = TeamInfo.getShortName(tmp4);
        }

        if(mNewGameList==null) mNewGameList = new ArrayList<>();
        if(mDeltaGameList==null) mDeltaGameList = new ArrayList<>();
        mNewGameList.clear();
        mDeltaGameList.clear();

        Log.d(TAG, "enterData: mBestOf=" + mBestOf);
        for (int gameNum = 1; gameNum <= mBestOf; gameNum++) {
            Integer s1 = 0;
            Integer s2 = 0;
            Spinner tmpS = getRespectiveSpinner(gameNum, 1);
            if (tmpS != null) s1 = (Integer) tmpS.getSelectedItem();
            tmpS = getRespectiveSpinner(gameNum, 2);
            if (tmpS != null) s2 = (Integer) tmpS.getSelectedItem();

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

            if ((winningScore<21 && losingScore!=0) || s1.equals(s2) || winners.equals(losers)) {
                if (!dry_run)
                    Toast.makeText(TournaLeagueEnterData.this, "Game" + gameNum + ": Bad data!",
                            Toast.LENGTH_SHORT).show();
                return false;
            }

            String dateStr = SharedData.getInstance().createNewRoundName(false, null);
            GameJournalDBEntry jEntry = new GameJournalDBEntry(dateStr, "", mCommon.mUser);
            jEntry.setResult(dateStr, mGameType, winner1, winner2, loser1, loser2, winningScore, losingScore);
            jEntry.setmGNo(gameNum);

            GameJournalDBEntry currentEntry = null;
            if (mGameList.size() >= gameNum) currentEntry = mGameList.get(gameNum - 1);
            if (currentEntry != null && currentEntry.exactlyEqual(jEntry)) {
                Log.i(TAG, "Nothing to change for: " + jEntry.toReadableString());
                mNewGameList.add(currentEntry);
            } else {
                String msg = "Not same: NEW:" + jEntry.toReadableString();
                if (currentEntry != null) msg += " OLD:" + currentEntry.toReadableString();
                Log.i(TAG, msg);
                mDeltaGameList.add(jEntry);
                mNewGameList.add(jEntry);
            }

            mSpinner_Teams.add(1, winner1 + "/" + winner2);
            CheckBox checkbox = findViewById(R.id.completed);
            if (isMatchDone(mNewGameList)) {
                Log.d(TAG, "enterData: checkbox is CHECKED");
                checkbox.setChecked(true);
                mSpinner_W.setSelection(1);
            } else {
                checkbox.setChecked(false);
                Log.d(TAG, "enterData: checkbox is UNCHECKED");
                mSpinner_W.setSelection(0);
            }
        }

        //Do not proceed to do tha actual DB update, if this is a dry run.
        if (dry_run) return true;


        if (mDeltaGameList.size() > 0) {

            lockAndUpdateDB();

            /*
            if (null != mProgressDialog) return false;   //attempt to press Enter button repeatedly
            mProgressDialog = new ProgressDialog(TournaLeagueEnterData.this);
            mProgressDialog.setMessage("Updating database....");
            mProgressDialog.show();

            //If we were to update for 1 game at a time, there will be overlaps between
            //background processes run by firebase APIs. When 2 reads are done in parallel for the
            //same attribute, they will both get the same initial value and hence the incremented
            //new value of one will be overwritten by the other. DB read/write for the same
            //attribute will have to be done serially.
            //or collect all the data, increment in appl memory and then finally do a single write to DB at the end.

            //To avoid a conflict when 2 users are entering the same score.
            //Without lock, inconsistency is seen: missing journal entry or points are not added.
            SharedData.getInstance().acquireDBLock(mCommon.mTournament);
            //Give some time for all other threads (firebase DB updates) to catch up.
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "After DB lock wait...");
                    workToUpdateDB();
                    //updateDB(gamePlayers);
                }
            }, 2000);
*/
        } else mCommon.killActivity(TournaLeagueEnterData.this, RESULT_OK);
        return true;
    }

    @Override
    protected void workToUpdateDB() {
        Log.d(TAG, "workToUpdateDB: ");

        for (GameJournalDBEntry jEntry : mDeltaGameList) {
            Log.i(TAG, "WRITING jEntry: " + jEntry.toReadableString());
            final DatabaseReference jDBEntryRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                    .child(mCommon.mTournament).child(Constants.MATCHES).child(Constants.DATA).child(mChosenMatch.key).
                            child(mSelectedMatch).child(Integer.toString(jEntry.getmGNo() - 1));
            jDBEntryRef.setValue(jEntry);
            Snackbar.make(findViewById(R.id.enterdata_ll), jEntry.getmW1() + "/" + jEntry.getmW2() + " won!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }

        //set done flag, if applicable
        //Update team and player points, only when the match is done. This will avoid the extra overhead if the user corrects game points for every button click.
        mWinnerPlayer1 = "";
        mWinnerTeam = "";
        if (isMatchDone(mNewGameList)) {
            Log.d(TAG, "workToUpdateDB: isMatchDone=true");
            mDBPlayerData.clear();
            mDBTeamScoreData.clear();
            mCount = 0;
            collectAndUpdate(mNewGameList);
        } else {
            Log.d(TAG, "workToUpdateDB: isMatchDone=false");
            mFinishActivity = false;
            releaseLockAndCleanup();
        }
        mGameList = mNewGameList;  //to be done at the end after all DB updates.
    }

    private Boolean isMatchDone(final ArrayList<GameJournalDBEntry> newGameList) {
        Log.i(TAG, "isMatchDone:" + newGameList.size());
        if (newGameList.size() == 0) return false;
        String randomPlayerT1 = newGameList.get(0).getmW1();   //we will see if this player has won best-of-N games.
        String randomPlayerT2 = newGameList.get(0).getmL1();   //take one from the other team too.
        int randomPlayerT1_Wins = 0;
        int randomPlayerT2_Wins = 0;
        int gamesCompleted = 0;
        for (GameJournalDBEntry jEntry : newGameList) {
            if (jEntry.getmWS() < 21) continue;
            if (jEntry.getmWS() >= 21) gamesCompleted++;
            if (jEntry.aWinner(randomPlayerT1)) randomPlayerT1_Wins++;
            if (jEntry.aWinner(randomPlayerT2)) randomPlayerT2_Wins++;
        }
        if (gamesCompleted == 1 && mBestOf == 1) {
            Log.i(TAG, "isMatchDone: Best-of-1");
            if (randomPlayerT1_Wins == 1) mWinnerPlayer1 = randomPlayerT1;
            else if (randomPlayerT2_Wins == 1) mWinnerPlayer1 = randomPlayerT2;
            return true;
        }

        if (randomPlayerT1_Wins > (mBestOf / 2)) {
            mWinnerPlayer1 = randomPlayerT1;
            Log.i(TAG, "isMatchDone: " + randomPlayerT1 + "=" + randomPlayerT1_Wins + " > " + mBestOf / 2 + " winner=" + mWinnerPlayer1);
            return true;
        } else if (randomPlayerT2_Wins > (mBestOf / 2)) {
            mWinnerPlayer1 = randomPlayerT2;
            Log.i(TAG, "isMatchDone: " + randomPlayerT2 + "=" + randomPlayerT2_Wins + " > " + mBestOf / 2 + " winner=" + mWinnerPlayer1);
            return true;
        }
        Log.i(TAG, "isMatchDone: NOPE");
        return false;
    }

    private void collectTeamScoreDataFromDB(final String team) {

        TeamScoreDBEntry value = mDBTeamScoreData.get(team);
        if (value != null) {
            //assumption is that null is not a valid value, inserted into the map
            Log.i(TAG, "collectTeamScoreDataFromDB already present:" + team);
            return;
        }
        Log.i(TAG, "collectTeamScoreDataFromDB Fetching:" + team);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.TEAMS).child(team).child(Constants.SCORE);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                TeamScoreDBEntry score = dataSnapshot.getValue(TeamScoreDBEntry.class);
                if (score == null) return;
                //PlayerInfo newPI = new PlayerInfo();
                Log.i(TAG, "collectTeamScoreDataFromDB:" + score.toString());
                mDBTeamScoreData.put(team, score);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i(TAG, "collectTeamScoreDataFromDB:" + team + " DB error on read:" + databaseError.getMessage());
                mCommon.showToast(TournaLeagueEnterData.this, "DB error on read: " + databaseError.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    private void collectPlayerDataFromDB(final String player) {

        PlayerInfo value = mDBPlayerData.get(player);
        if (value != null) {
            //assumption is that null is not a valid value, inserted into the map
            Log.i(TAG, "collectPlayerDataFromDB already present:" + player);
            return;
        }
        Log.i(TAG, "collectPlayerDataFromDB Fetching:" + player);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.PLAYERS).child(player);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                PlayerInfo pI = dataSnapshot.getValue(PlayerInfo.class);
                if (pI == null) return;
                //PlayerInfo newPI = new PlayerInfo();
                Log.i(TAG, "collectPlayerDataFromDB:" + pI.toString() + " key=" + dataSnapshot.getKey() + " p=" + player + " winner=" + mWinnerPlayer1);
                mDBPlayerData.put(player, pI);
                collectTeamScoreDataFromDB(pI.T);
                if (mWinnerPlayer1.equals(player)) {
                    mWinnerTeam = pI.T;
                    Log.i(TAG, "collectPlayerDataFromDB, winner:" + dataSnapshot.getKey() + " team:" + mWinnerTeam);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i(TAG, "collectPlayerDataFromDB:" + player + " DB error on read:" + databaseError.getMessage());
                mCommon.showToast(TournaLeagueEnterData.this, "DB error on read: " + databaseError.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }


    private void collectDataFromDB(final ArrayList<GameJournalDBEntry> newGameList) {
        int delayMS = 0;
        //Inorder to avoid duplicate fetch of data from DB, add delays
        //before reading new game entries. Mostly it is going to be 3 games (best-of-3)
        //So, 500ms delay for the players in the 3rd game. Thats ok, as there is already a 2s
        //delay before checking for DB read completion.
        for (GameJournalDBEntry jEntry : newGameList) {
            if (jEntry.getmWS() < 21) continue;
            Log.v(TAG, "collectDataFromDB: " + delayMS);
            List<String> players = new ArrayList<>
                    (
                            Arrays.asList(jEntry.getmW1(), jEntry.getmW2(),
                                    jEntry.getmL1(), jEntry.getmL2())
                    );
            for (final String p : players) {
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        collectPlayerDataFromDB(p);
                    }
                }, delayMS);
            }
            delayMS += 250;
        }
    }

    private Boolean isDBReadDone() {
        Boolean retVal = false;
        if (mDBTeamScoreData.size() == 2) retVal = true;  //2 teams played the game
        Log.i(TAG, "isDBReadDone:" + retVal + " " + mDBTeamScoreData.size() + "/" + mCommon.mTeams.size());
        return retVal;
    }

    private void collectAndUpdate(final ArrayList<GameJournalDBEntry> newGameList) {
        collectDataFromDB(newGameList);
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "collectAndUpdate: After wait...");
                prepForDBUpdate(newGameList);
            }
        }, 2000);
    }

    private void prepForDBUpdate(final ArrayList<GameJournalDBEntry> newGameList) {
        if (!isDBReadDone() && mCount < 5) {
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "prepForDBUpdate: After wait..." + mCount);
                    prepForDBUpdate(newGameList);
                }
            }, 2000);
            mCount++;
            return;
        } else if (mCount >= 5) {
            Log.e(TAG, "Failed to read DB");
            mCommon.showToast(TournaLeagueEnterData.this, "Failed to read DB!", Toast.LENGTH_SHORT);
            mFinishActivity = true;
            releaseLockAndCleanup();
            return;
        }
        for (GameJournalDBEntry jEntry : newGameList) {
            if (jEntry.getmWS() < 21) continue;
            Log.i(TAG, "prepForDBUpdate looping gameList, Got:" + jEntry.toReadableString());
            prepData_player(jEntry.getmW1(), true, false, jEntry.getmWS(), jEntry.getmLS());
            prepData_player(jEntry.getmW2(), true, true, jEntry.getmWS(), jEntry.getmLS());
            prepData_player(jEntry.getmL1(), false, false, jEntry.getmWS(), jEntry.getmLS());
            prepData_player(jEntry.getmL2(), false, true, jEntry.getmWS(), jEntry.getmLS());
        }
        prepData_TeamScore_match();  //update team points for the overall game
        updateDB();
    }

    //private void updateDB_player(final String player, final Boolean won, final Boolean updateTeamScore, final ArrayList<GameJournalDBEntry> gameList) {
    private void prepData_player(final String player, final Boolean won, final Boolean updateTeamScore,
                                 final int winningScore, final int loosingScore) {
        Log.i(TAG, "updateDB_player Got:" + player + "/" + won + "/" + updateTeamScore);
        PlayerInfo pI = mDBPlayerData.get(player);
        if (pI == null) return;
        Log.i(TAG, "updateDB_player old data:" + pI.toString());
        if (won) pI.wonGame();
        else pI.lostGame();
        Log.i(TAG, "updateDB_player new data:" + pI.toString());
        if (updateTeamScore) prepData_TeamScore_game(player, pI.T, won, winningScore, loosingScore);
    }

    private void prepData_TeamScore_game(final String player, final String team, final Boolean won,
                                         final int winningScore, final int loosingScore) {
        Log.i(TAG, "updateDB_TeamScore Got games for " + player + "/" + team + " w:" + won + " WS:" +
                winningScore + " LS:" + loosingScore);
        TeamScoreDBEntry score = mDBTeamScoreData.get(team);
        if (score == null) return;
        Log.i(TAG, "updateDB_TeamScore old data:" + score.toString());
        if (won) score.wonGame(winningScore, loosingScore);
        else score.lostGame(loosingScore, winningScore);
        Log.i(TAG, "updateDB_TeamScore new data:" + score.toString());
    }

    private void prepData_TeamScore_match() {
        for (Map.Entry<String, TeamScoreDBEntry> entry : mDBTeamScoreData.entrySet()) {
            String key = entry.getKey();
            TeamScoreDBEntry score = entry.getValue();
            Log.i(TAG, "prepData_TeamScore_match (" + key + ") old data:" + score.toString());
            if (mWinnerTeam.equals(key)) score.wonMatch();
            else score.lostMatch();
            Log.i(TAG, "prepData_TeamScore_match new data:" + score.toString());
        }
    }

    private void updateDB() {
        Log.i(TAG, "updateDB Got Teams=" + mDBTeamScoreData.size() + ", Players=" + mDBPlayerData.size() +
                "Match=" + mChosenMatch.key + "/" + mSelectedMatch);

        for (Map.Entry<String, TeamScoreDBEntry> entry : mDBTeamScoreData.entrySet()) {
            final String team = entry.getKey();
            final TeamScoreDBEntry score = entry.getValue();
            Log.i(TAG, "updateDB_TeamScore " + team + " new:" + score.toString());
            DatabaseReference dbUpdateRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                    .child(mCommon.mTournament).child(Constants.TEAMS).child(team).child(Constants.SCORE);
            dbUpdateRef.setValue(score);
        }

        for (Map.Entry<String, PlayerInfo> entry : mDBPlayerData.entrySet()) {
            final String player = entry.getKey();
            final PlayerInfo pInfo = entry.getValue();
            DatabaseReference dbPlayerRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                    .child(mCommon.mTournament).child(Constants.PLAYERS).child(player);
            Log.i(TAG, "updateDB_PlayerInfo " + player + " new:" + pInfo.toString());
            dbPlayerRef.setValue(pInfo);
        }

        DatabaseReference metaDBRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.MATCHES).child(Constants.META).child(mChosenMatch.key).child(mSelectedMatch);
        metaDBRef.setValue(true);

        auditIfAllMatchesInASetCompleted();  //its ok to do this in background even when DB lock is being released
        mCommon.setDBUpdated(true);
        mFinishActivity = true;
        releaseLockAndCleanup();
    }

    private void auditIfAllMatchesInASetCompleted() {
        Log.i(TAG, "auditIfAllMatchesInASetCompleted:" + mChosenMatch.key + "/" + mSelectedMatch);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.MATCHES).child(Constants.META).child(mChosenMatch.key);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "auditIfAllMatchesInASetCompleted:" + dataSnapshot.getKey());
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    if (child.getKey() == null) return;
                    if (child.getKey().equals(Constants.INFO)) continue;
                    Boolean status = child.getValue(Boolean.class);
                    if (status == null) return;
                    Log.d(TAG, "auditIfAllMatchesInASetCompleted: " + child.getKey() + "=" + status);
                    if (!status) return;
                }

                //we reach here, only if all Matches had "true" status in metadata
                //set done flag for this match set
                DatabaseReference metaDBRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                        .child(mCommon.mTournament).child(Constants.MATCHES).child(Constants.META).child(mChosenMatch.key)
                        .child(Constants.INFO).child(Constants.COMPLETED);
                metaDBRef.setValue(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TournaLeagueEnterData.this, "DB error while fetching metadata: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    /*
    protected void releaseLockAndCleanup(final Boolean finish, final Boolean dbUpdated) {
        Log.d(TAG, "releaseLockAndCleanup: ");
        if (dbUpdated) SharedData.getInstance().setDBUpdated(true);
        SharedData.getInstance().releaseDBLock(mCommon.mTournament);
        if (null != mProgressDialog && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        if (finish) mCommon.killActivity(TournaLeagueEnterData.this, RESULT_OK);
    }
    */

    //CallbackRoutine Callback interfaces
    public void profileFetched() {
    }

    public void callback(final String key, final Object inobj) {
    }

    public void alertResult(final String in, final Boolean ok, final Boolean ko) {
        if (in.equals(Constants.CB_NOMATCHFAOUND)) {
            Log.d(TAG, "alertResult: ");
            mCommon.killActivity(TournaLeagueEnterData.this, RESULT_OK);
        }
    }

    public void completed(final String in, final Boolean ok) {
        Log.w(TAG, "completed: " + in + ":" + ok);
        if (in.equals(Constants.CB_READMATCHMETA)) {
            //callback after reading DB for meta data
            if (ok) {
                mNumOfMatches = mTUtil.mNumOfMatches;
                mBestOf = mTUtil.mBestOf;
                Log.i(TAG, "Num of Matches = " + mNumOfMatches.toString() + " bestof=" + mBestOf);
                mTUtil.showMatches(findViewById(R.id.enterdata_header));
            } else {
                mCommon.showAlert(TournaLeagueEnterData.this, TournaLeagueEnterData.this, Constants.CB_NOMATCHFAOUND,
                        "There are no scheduled matches.\nOr all the scheduled matches are already played.");
            }
        } else if (in.equals(Constants.CB_SHOWMATCHES)) {
            //callback after reading DB for meta data
            if (ok) {
                showMatchGames(mTUtil.mMSStr_chosen);
                //Log.w(TAG, "completed: " + in + ":" + mTUtil.mMSStr_chosen);
            }
        }
    }

}


