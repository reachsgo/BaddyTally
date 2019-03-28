package com.sg0.baddytally;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class TrackScores extends AppCompatActivity {

    private static final String TAG = "TournaScores";
    private static final String DELIM1 = ",";
    private static final String DELIM2 = ":";
    private static final String EMPTY = "";
    private Button mRedoBtn, mSwapBtn, mUndoBtn;
    private Button mLeftWinBtn, mRightWinBtn;
    private Button mGame1, mGame2, mGame3;

    private ArrayList<GameData> mGames;
    private int mGameNum;
    private Animation mShake;
    private String mWinner, mTeam1;
    private ArrayList<String> mResults;
    private boolean returnResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.track_scores);
        Log.d(TAG, "onCreate: ");
        mShake = AnimationUtils.loadAnimation(TrackScores.this, R.anim.shake);
        mResults = new ArrayList<>();
        //initialize mResults to save results of 3 games
        mResults.add(""); mResults.add(""); mResults.add("");
        returnResults = false;

        mLeftWinBtn = findViewById(R.id.left_win_btn);
        mLeftWinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: Left Win");
                v.startAnimation(mShake);
                if(!checkIfMatchDone(mGames.get(mGameNum).leftTeam.getScore(),
                        mGames.get(mGameNum).rightTeam.getScore())) {
                    mGames.get(mGameNum).wonAPoint(true); //left side won
                    //If this is the winning point, display winner
                    checkIfMatchDone(mGames.get(mGameNum).leftTeam.getScore(),
                            mGames.get(mGameNum).rightTeam.getScore());
                }
            }
        });


        mRightWinBtn = findViewById(R.id.right_win_btn);
        mRightWinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: Right Win");
                v.startAnimation(mShake);
                if(!checkIfMatchDone(mGames.get(mGameNum).leftTeam.getScore(),
                        mGames.get(mGameNum).rightTeam.getScore())) {
                    mGames.get(mGameNum).wonAPoint(false);  //right side won
                    //If this is the winning point, display winner
                    checkIfMatchDone(mGames.get(mGameNum).leftTeam.getScore(),
                            mGames.get(mGameNum).rightTeam.getScore());
                }
            }
        });


        findViewById(R.id.team_left_tv).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mGames.get(mGameNum).leftTeam.swapPlayers();
                return true;
            }
        });

        findViewById(R.id.team_right_tv).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mGames.get(mGameNum).rightTeam.swapPlayers();
                return true;
            }
        });

        View.OnLongClickListener longClickService = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.i(TAG, "longClickService: ");
                mGames.get(mGameNum).switchService();
                return true;
            }
        };
        findViewById(R.id.service_l_top_btn).setOnLongClickListener(longClickService);
        findViewById(R.id.service_l_bottom_btn).setOnLongClickListener(longClickService);
        findViewById(R.id.service_r_top_btn).setOnLongClickListener(longClickService);
        findViewById(R.id.service_r_bottom_btn).setOnLongClickListener(longClickService);

        mUndoBtn = findViewById(R.id.undo_btn);
        mUndoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: undo:" + mGames.get(mGameNum).scores.size());
                mGames.get(mGameNum).undo();
                checkIfMatchDone(mGames.get(mGameNum).leftTeam.getScore(),
                        mGames.get(mGameNum).rightTeam.getScore());
            }
        });
        mUndoBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.i(TAG, "onLongClick: CLEAR");
                Toast.makeText(TrackScores.this, "Reset of Game-" + (mGameNum+1) + " ...",
                        Toast.LENGTH_LONG).show();
                mGames.get(mGameNum).clear();
                return true;
            }
        });

        mRedoBtn = findViewById(R.id.redo_btn);
        mRedoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: redo:" + mGames.get(mGameNum).scores.size());
                mGames.get(mGameNum).redo();
                checkIfMatchDone(mGames.get(mGameNum).leftTeam.getScore(),
                        mGames.get(mGameNum).rightTeam.getScore());
            }
        });

        mSwapBtn = findViewById(R.id.swap_btn);
        mSwapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: Switching sides:" + mGames.get(mGameNum).scores.size());
                Toast.makeText(TrackScores.this,
                            "Switching sides!", Toast.LENGTH_LONG).show();
                mGames.get(mGameNum).switchSides();
            }
        });


        mGame1 = findViewById(R.id.game1_btn);
        mGame1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGameNum = 0;
                showGame(null, null);
                mGame1.setTextColor(Color.BLUE);
                mGame2.setTextColor(Color.GRAY);
                mGame3.setTextColor(Color.GRAY);
            }
        });
        mGame2 = findViewById(R.id.game2_btn);
        mGame2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGameNum = 1;
                showGame(null, null);
                mGame1.setTextColor(Color.GRAY);
                mGame2.setTextColor(Color.BLUE);
                mGame3.setTextColor(Color.GRAY);
            }
        });
        mGame3 = findViewById(R.id.game3_btn);
        mGame3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGameNum = 2;
                showGame(null, null);
                mGame1.setTextColor(Color.GRAY);
                mGame2.setTextColor(Color.GRAY);
                mGame3.setTextColor(Color.BLUE);
            }
        });


        Intent myIntent = getIntent(); // gets the previously created intent
        String team1 = myIntent.getStringExtra("team1");
        String team2 = myIntent.getStringExtra("team2");
        String t1p1 = myIntent.getStringExtra("t1p1");
        String t1p2 = myIntent.getStringExtra("t1p2");
        String t2p1 = myIntent.getStringExtra("t2p1");
        String t2p2 = myIntent.getStringExtra("t2p2");

        if(null==team1 || null==team2 || team1.isEmpty() || team2.isEmpty()) {
            TeamDialogClass customDialog = new TeamDialogClass(TrackScores.this);
            customDialog.show();
        } else {
            setTeamData(team1, t1p1, t1p2, team2, t2p1, t2p2);
            returnResults = true;
        }

        //If there are existing scores read from DB, populate the same here.
        ArrayList<String> scores = myIntent.getStringArrayListExtra("scores");
        if(scores!=null && scores.size()>0) {
            Log.d(TAG, "onCreate: existing scores=" + scores.toString());
            //If there are existing scores, set those
            for (int i = 0; i < scores.size(); i++) {
                if(scores.get(i).isEmpty()) {
                    //Scores should be filled in order. If game3 is filled, with empty game1/2,
                    //its considered bad entry to avoid complexity.
                    break;
                }
                String[] parts = scores.get(i).split("-");
                if(parts.length!=2) break;
                Integer t1Score = Integer.valueOf(parts[0]);
                Integer t2Score = Integer.valueOf(parts[1]);
                Log.d(TAG, "onCreate: mTeam1=" + mTeam1);
                switch(i) {
                    case 0:
                        Log.i(TAG, "onCreate: Filling Game1");
                        //first game is already created
                        mGames.get(0).leftTeam.score = t1Score;
                        mGames.get(0).rightTeam.score = t2Score;
                        mGames.get(0).pushNewHistory(mGames.get(0).leftTeam.toDataString(),
                                mGames.get(0).rightTeam.toDataString());
                        mGameNum = 0;
                        checkIfMatchDone(t1Score, t2Score);
                        break;
                    case 1:
                        Log.i(TAG, "onCreate: Filling Game2" + t1Score + "-");
                        if(mGames.size()>1) break;  //not expected
                        GameData g2 = new GameData(mGames.get(0), true);
                        g2.leftTeam.score = t1Score;
                        g2.rightTeam.score = t2Score;
                        mGames.add(g2);
                        mGames.get(1).pushNewHistory(mGames.get(1).leftTeam.toDataString(),
                                mGames.get(1).rightTeam.toDataString());
                        mGameNum = 1;
                        checkIfMatchDone(t1Score, t2Score);
                        break;
                    case 2:
                        Log.i(TAG, "onCreate: Filling Game3");
                        if(mGames.size()>2) break;  //not expected
                        GameData g3 = new GameData(mGames.get(0), true);
                        g3.leftTeam.score = t1Score;
                        g3.rightTeam.score = t2Score;
                        mGames.add(g3);
                        mGameNum = 2;
                        mGames.get(2).pushNewHistory(mGames.get(2).leftTeam.toDataString(),
                                mGames.get(2).rightTeam.toDataString());
                        checkIfMatchDone(t1Score, t2Score);
                        break;
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    @Override
    public void onBackPressed() {
        killActivity();
        if(!returnResults) {
            Intent intent = new Intent(TrackScores.this, MainSigninActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    void setTeamData(String team1, String t1p1, String t1p2,
                     String team2, String t2p1, String t2p2) {

        mWinner = mTeam1 = "";
        mGameNum = 0;
        mGames = new ArrayList<>();

        Log.i(TAG, "setTeamData: init");
        //init; first game to be created
        GameData g = new GameData();
        //If not data entered, use default
        if(null==team1 || team1.isEmpty()) team1 = "Team1";
        if(null==team2 || team2.isEmpty()) team2 = "Team2";
        if(null==t1p1 || t1p1.isEmpty()) {
            t1p1 = "T1 player1";
            t1p2 = "T1 player2";
        }
        if(null==t2p1 || t2p1.isEmpty()) {
            t2p1 = "T2 player1";
            t2p2 = "T2 player2";
        }
        g.init(team1, t1p1, t1p2, team2, t2p1, t2p2);
        mGames.add(g);

        //set one team as team1 so that all match scores on the
        //bottom buttons (game buttons) can be ordered as team1Score-team2Score
        mTeam1 = team1;

        //To start with, Only game1 is enabled.
        mGame1.performClick();
        mGame2.setEnabled(false);
        mGame3.setEnabled(false);
    }

    void showGame(TeamData leftTeam, TeamData rightTeam) {

        if(mGameNum > mGames.size()-1) {  //mGameNum range is 0, 1 or 2
            Log.i(TAG, "showGame: mGames=" + mGames.size());
            GameData g = new GameData(mGames.get(mGameNum-1), false);
            Log.i(TAG, "showGame: Add Game=" + g.toString());
            mGames.add(g);
            return;
        }

        //Game is already created. Fetch and display.
        Log.i(TAG, "showGame: fetch and display:" + mGameNum);
        mGames.get(mGameNum).leftTeam.setViews(true);
        mGames.get(mGameNum).rightTeam.setViews(false);
        mGames.get(mGameNum).display();
    }

    boolean checkIfMatchDone(final int score1, final int score2) {
        if(!mGames.get(mGameNum).isGameDone(score1,score2)) {
            //Game is not yet done.
            //If doing undo, game text needs to be updated!
            switch (mGameNum) {
                case 0:
                    mGame1.setText(getGameBtnStr(0, EMPTY, EMPTY));
                    break;
                case 1:
                    mGame2.setText(getGameBtnStr(1, EMPTY, EMPTY));
                    break;
                case 2:
                    mGame3.setText(getGameBtnStr(2, EMPTY, EMPTY));
                    break;
            }
            return false;
        }
        String str;
        //current game is done, enable next game button
        switch (mGameNum) {
            case 0:
                str = getGameBtnStr(0, EMPTY, EMPTY);
                mGame1.setText(str);
                mGame2.setEnabled(true);
                mGame2.setTextColor(Color.BLACK);
                mResults.remove(0);
                mResults.add(0, mGames.get(0).getScore());
                mGame2.setAnimation(mShake);
                //this animation is only seen when called from EnterData.
                //some other action might be overriding this animation is the normal flow.
                break;
            case 1:
                str = getGameBtnStr(1, EMPTY, EMPTY);
                mGame2.setText(str);
                mGame3.setEnabled(true);
                mGame3.setTextColor(Color.BLACK);
                if(mGames.get(0).winner.equals(mGames.get(1).winner)) {
                    mWinner = mGames.get(0).winner;
                }
                mResults.remove(1);
                mResults.add(1, mGames.get(1).getScore());
                mGame3.setAnimation(mShake);
                break;
            case 2:
                str = getGameBtnStr(2, EMPTY, EMPTY);
                mGame3.setText(str);
                if(mGames.get(0).winner.equals(mGames.get(2).winner)) {
                    mWinner = mGames.get(0).winner;
                } else if(mGames.get(1).winner.equals(mGames.get(2).winner)) {
                    mWinner = mGames.get(1).winner;
                }
                mResults.remove(2);
                mResults.add(2, mGames.get(2).getScore());
                break;
        }
        if(!mWinner.isEmpty()) {
            String msg = mWinner + " won this match.\n";
            if(returnResults) msg += "Return and save the scores!";
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TrackScores.this);
            alertBuilder.setTitle("Winner");
            alertBuilder.setMessage(msg);
            alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(returnResults) killActivity();
                }
            });
            alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(returnResults)
                        Toast.makeText(TrackScores.this,
                            "Enter back button when you ready to save the scores...",
                                Toast.LENGTH_LONG).show();
                }
            });
            alertBuilder.show();

        }
        mGames.get(mGameNum).print();
        return true;
    }

    String getGameBtnStr(final int gameNum, final String t1score, final String t2score) {
        Log.e(TAG, "getGameBtnStr: [" + t1score + "]" + " [" + t2score + "]"  );
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.getDefault(),"Game%d ", (gameNum+1)));
        if(t1score.isEmpty() && t2score.isEmpty()) {
            if(null!=mGames.get(gameNum)) {
                //sb.append(System.getProperty("line.separator"));
                sb.append("(");
                sb.append(mGames.get(gameNum).getScore());
                sb.append(")");
            }
        } else {
            //Adding a new line is causing display issues (button is moved down)
            //only when invoked from EnterData.
            //sb.append(System.getProperty("line.separator"));
            sb.append(String.format(Locale.getDefault(),"(%s-%s)", t1score, t2score));
        }
        return sb.toString();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.basic_menu_main, menu);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_logout).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_help:
                AlertDialog.Builder hBuilder = new AlertDialog.Builder(TrackScores.this);
                hBuilder.setMessage(Html.fromHtml(
                        "<a href=\"https://sites.google.com/view/scoretally/user-guide\">User Guide link</a>"))
                        .setTitle(Constants.APPNAME)
                        .setNeutralButton("Ok", null);
                AlertDialog help = hBuilder.create();
                help.show();
                // Make the textview clickable. Must be called after show()
                ((TextView)help.findViewById(android.R.id.message))
                        .setMovementMethod(LinkMovementMethod.getInstance());
                break;
            case R.id.action_about:
                //int versionCode = BuildConfig.VERSION_CODE;
                AlertDialog.Builder builder = new AlertDialog.Builder(TrackScores.this);
                builder.setMessage("Version: " + BuildConfig.VERSION_NAME)
                        .setTitle(SharedData.getInstance().getTitleStr(Constants.APPNAME, TrackScores.this))
                        .setNeutralButton("Ok", null).show();
                break;
            default:
                break;
        }
        return true;
    }

    private void killActivity() {
        if(returnResults) {
            Intent returnIntent = new Intent();
            returnIntent.putStringArrayListExtra("gameResults", mResults);
            setResult(RESULT_OK, returnIntent);
        }
        finish();
    }



    /********************* TEAM DATA ***********************/


    class TeamData {
        private String team;
        ArrayList<String> players;
        private int score;
        int oddPlayerIdx;
        int evenPlayerIdx;
        boolean left;
        boolean right;
        boolean service;
        private Button srvcOddBtn, srvcEvenBtn, winBtn;
        private TextView playerOddTv, playerEvenTv;
        private TextView scoreTv, teamTv;
        private GameData parentGame;
        
        private int TEAMSIZE_SINGLES;
        private int TEAMSIZE_DOUBLES;

        TeamData(final String team, final ArrayList<String> players,
                 final GameData parent) {
            this.team = team;
            if(players==null) this.players = new ArrayList<>();
            else this.players = new ArrayList<>(players);
            this.players.add(0, "");
            TEAMSIZE_SINGLES = 2;  //empty entry + 1 player
            TEAMSIZE_DOUBLES = 3;  //empty entry + 2 players
            oddPlayerIdx = 0;
            evenPlayerIdx = 0;
            score = 0;
            left = right = service = false;
            winBtn = null;
            parentGame = parent;
            Log.d(TAG, "TeamData: " + toString());
        }

        TeamData(final TeamData other, final GameData parent) {
            this(other.team, null, parent);
            //When game2/3 are copied from previous game,
            //empty player is already there at index0; just copy the entire list.
            this.players.clear();
            this.players.addAll(other.players);
            this.score = other.score;
            this.oddPlayerIdx = other.oddPlayerIdx;
            this.evenPlayerIdx = other.evenPlayerIdx;
            this.left = other.left;
            this.right = other.right;
            this.service = other.service;
            Log.d(TAG, "TeamData(other): " + toString());
        }

        void clear() {
            score = 0;
            left = right = service = false;
            if(null!=winBtn) {
                displayWinner(true);
                displayScore();
            }
        }

        public void setViews(final boolean leftTeam) {
            if(leftTeam) {
                this.left = true;
                this.right = false;
                srvcOddBtn = findViewById(R.id.service_l_top_btn);
                srvcEvenBtn = findViewById(R.id.service_l_bottom_btn);
                playerOddTv = findViewById(R.id.player_l_top_tv);
                playerEvenTv = findViewById(R.id.player_l_bottom_tv);
                scoreTv = findViewById(R.id.score_left_tv);
                teamTv = findViewById(R.id.team_left_tv);
                winBtn = findViewById(R.id.left_win_btn);
            } else {
                this.left = false;
                this.right = true;
                srvcOddBtn = findViewById(R.id.service_r_bottom_btn);
                srvcEvenBtn = findViewById(R.id.service_r_top_btn);
                playerOddTv = findViewById(R.id.player_r_bottom_tv);
                playerEvenTv = findViewById(R.id.player_r_top_tv);
                scoreTv = findViewById(R.id.score_right_tv);
                teamTv = findViewById(R.id.team_right_tv);
                winBtn = findViewById(R.id.right_win_btn);
            }
            //scoreTv.setBackgroundColor(Color.WHITE);
            //teamTv.setBackgroundColor(Color.WHITE);
            displayWinner(true);
            teamTv.setText(team);
            displayScore();
        }

        int getOddPlayerIdx() {
            if(players.size()==TEAMSIZE_SINGLES) return 1;
            else return oddPlayerIdx;
        }

        int getEvenPlayerIdx() {
            if(players.size()==TEAMSIZE_SINGLES) return 1;
            else return evenPlayerIdx;
        }
        
        boolean isSingles() {
            return players.size()==TEAMSIZE_SINGLES;
        }
        
        void displayWinner(final boolean clear) {
            if(clear) {
                winBtn.setBackground(ActivityCompat.getDrawable(TrackScores.this, R.drawable.trophy_transparent_30));
                teamTv.setBackgroundColor(getResources().getColor(R.color.colorTealGreen));
            } else {
                //scoreTv.setBackgroundColor(Color.YELLOW);
                teamTv.setBackgroundColor(Color.YELLOW);
                winBtn.setBackground(ActivityCompat.getDrawable(TrackScores.this, R.drawable.trophy_gold_transparent_50));
            }
        }

        public int getScore() {
            return score;
        }

        public String getTeam() {
            return team;
        }

        void setService(final boolean service) {
            this.service = service;
            displayService();
        }


        boolean switchService(){
            Log.e(TAG, "switchService: " + service);
            service = !service;
            displayService();
            return service;
        }

        public void setServiceChange() {
            Log.i(TAG, "setServiceChange: " + service);
            setService(false);
        }

        void switchSides() {
            Log.i(TAG, "switchSides: " + toString());
            if(this.left) {
                setViews(false);
            }
            else if (this.right) {
                setViews(true);
            }
            displayPlayers(0);
            displayService();
            Log.i(TAG, "switchSides done: " + toString());
        }

        void setTopPlayerIdx(final String player) {
            for (int i = 0; i < players.size(); i++) {
                if(players.get(i).equals(player)) {
                    if(left) oddPlayerIdx = i;
                    else if(right) evenPlayerIdx = i;
                    break;
                }
            }
            Log.d(TAG, "setTopPlayerIdx: odd:" + oddPlayerIdx + " even:" + evenPlayerIdx);
            displayPlayers(0);
        }

        void setBottomPlayerIdx(final String player) {
            for (int i = 0; i < players.size(); i++) {
                if(players.get(i).equals(player)) {
                    if(left) evenPlayerIdx = i;
                    else if(right) oddPlayerIdx = i;
                    break;
                }
            }
            Log.d(TAG, "setBottomPlayerIdx: odd:" + oddPlayerIdx + " even:" + evenPlayerIdx);
            displayPlayers(0);
        }

        public void displayService() {
            Log.i(TAG, "displayService: " + service + " score=" + score);
            if(!service) {
                srvcEvenBtn.setVisibility(View.GONE);
                srvcOddBtn.setVisibility(View.GONE);
                return;
            }
            if(score%2 == 0) {
                //Even score
                srvcEvenBtn.setVisibility(View.VISIBLE);
                srvcOddBtn.setVisibility(View.GONE);
            } else {
                //Odd Score
                srvcEvenBtn.setVisibility(View.GONE);
                srvcOddBtn.setVisibility(View.VISIBLE);
            }
            if (null!=parentGame && null!=parentGame.getOppTeam(this))
                parentGame.getOppTeam(this).displayPlayers(score);  //just your own
        }


        public void displayPlayers(final int oppScore) {
            if(isSingles()) {
                Log.d(TAG, oppScore + " displayPlayers: my=" + toString());

                if(service) {
                    //for singles, player is where the point is, if serving
                    if (score % 2 == 0) {
                        playerOddTv.setText("");
                        playerEvenTv.setText(players.get(1));
                    } else {
                        playerOddTv.setText(players.get(1));
                        playerEvenTv.setText("");
                    }
                }
                if(!service || oppScore!=0){
                    //this team not serving or
                    //the opposite came to know that they are going to serve, ahead of this team.
                    //Happens on undo/redo depending on the order of fromDataString().

                    if(0==oppScore && parentGame.gameStarted()) return;
                    //if this team is not serving, ignore, unless this is the first call (game not started)

                    Log.d(TAG, "displayPlayers: score=" + score + " opp=" + oppScore);
                    if (oppScore % 2 == 0) {
                        //even point for opp team, get ready to receive that serve
                        playerOddTv.setText("");
                        playerEvenTv.setText(players.get(1));
                    } else {
                        playerOddTv.setText(players.get(1));
                        playerEvenTv.setText("");
                    }
                }
                Log.i(TAG, "displayPlayers: Singles -- done");
            } else {
                Log.i(TAG, "displayPlayers: odd = " + players.get(oddPlayerIdx));
                Log.i(TAG, "displayPlayers: even = " + players.get(evenPlayerIdx));
                playerOddTv.setText(players.get(oddPlayerIdx));
                playerEvenTv.setText(players.get(evenPlayerIdx));
            }
        }

        public void servedAndWon() {
            Log.i(TAG, "servedAndWon: ");
            if(players.size()==2) { //including index=0 empty
                if(oddPlayerIdx==1) { oddPlayerIdx=0; evenPlayerIdx=1; }
                else { oddPlayerIdx=1; evenPlayerIdx=0; }
            } else if(players.size()==3) {
                if(oddPlayerIdx==1) { oddPlayerIdx=2; evenPlayerIdx=1; }
                else { oddPlayerIdx=1; evenPlayerIdx=2; }
            }
        }

        void displayScore() {
            String str = "0";
            if(score<10) str += score;
            else str = "" + score;
            scoreTv.setText(str);
        }

        public void wonPoint() {
            score++;
            if(service) servedAndWon();
            service = true;  //service change
            displayScore();
            displayPlayers(0);
            displayService();
        }

        public void lostPoint() {
            setServiceChange();
        }

        public void setScore(final String scoreStr) {
            //service change or not should also be handled
            score = Integer.valueOf(scoreStr);
            displayScore();
        }

        //<Team1Name>,<Left Score>,<L_serving>,<L_odd_index>,<L_even_index>:<Team2Name>,<Right Score>,<R_serving>,<R_odd_index>,<R_even_index>
        //  L_serving = Left team was serving? 1 (yes) or 0
        //  L_odd_index = Left odd position player index
        //  L_even_index = Left even position player index
        public String toDataString() {
            return String.format(Locale.getDefault(),"%s%s%d%s%s%s%d%s%d",
                    team, DELIM1, score, DELIM1,
                    Boolean.toString(service), DELIM1,
                    oddPlayerIdx, DELIM1,
                    evenPlayerIdx);
        }

        boolean validDataStringList(final ArrayList<String> dataStrList) {
            if (null==dataStrList || dataStrList.size()!=2) {
                return false;
            }
            return true;
        }

        public void fromDataString(final ArrayList<String> dataStrList) {
            //validation already done: validDataStringList() should be invoked before
            //invoking this.

            String[] parts = dataStrList.get(0).split(DELIM1);
            if(parts.length!=5) return;
            if(!parts[0].equals(team)) {
                //If there was a switch of sides in between, data might be switched from L to R
                //or vice versa
                parts = dataStrList.get(1).split(DELIM1);
                if(parts.length!=5) return;
            }
            score = Integer.valueOf(parts[1]);
            service = Boolean.valueOf(parts[2]);
            oddPlayerIdx = Integer.valueOf(parts[3]);
            evenPlayerIdx = Integer.valueOf(parts[4]);
            Log.i(TAG, "fromDataString: " + toString());
            displayScore();
            displayPlayers(0);  //just your own
            displayService();
        }

        void swapPlayers() {
            Log.i(TAG, "swapPlayers: " + toString());
            int tmp = oddPlayerIdx;
            oddPlayerIdx = evenPlayerIdx;
            evenPlayerIdx = tmp;
            displayPlayers(0);
        }

        @Override
        public String toString() {
            return "TeamData{" + (left?"LEFT":"") + (right?"RIGHT":"") +
                    " p=" + players +
                    ", s=" + score +
                    ", oddIdx=" + oddPlayerIdx +
                    ", evenIdx=" + evenPlayerIdx +
                    ", serv=" + service +
                    '}';
        }
    }


    /********************* GAME  DATA ***********************/

    class GameData {

        ArrayList<String> scores;
        TeamData leftTeam;
        TeamData rightTeam;
        int historyIdx;
        String winner;

        GameData() {
            scores = new ArrayList<>();
            historyIdx = -1;
            leftTeam = null;
            rightTeam = null;
            winner = "";
        }

        GameData(final GameData other, final boolean initOnly) {
            this();
            //initialize a new GameData from the previous one
            if(!initOnly) {
                //switch the courts, as usually would be done on court
                //from 1 game to another
                leftTeam = new TeamData(other.rightTeam, this);
                rightTeam = new TeamData(other.leftTeam, this);
            } else {
                //just initialize
                leftTeam = new TeamData(other.leftTeam, this);
                rightTeam = new TeamData(other.rightTeam, this);
            }
            leftTeam.clear();
            rightTeam.clear();

            leftTeam.setViews(true);
            rightTeam.setViews(false);
            if(other.winner.equals(leftTeam.getTeam())) {
                //last Match winner gets to serve first
                leftTeam.setService(true);
                rightTeam.setService(false);
            } else {
                leftTeam.setService(false);
                rightTeam.setService(true);
            }

            if(!initOnly) {
                initHistory(leftTeam.toDataString(),
                        rightTeam.toDataString(),
                        false);
            }
            display();
            print();
        }

        TeamData getOppTeam(final TeamData teamData){
            if(teamData.left) return rightTeam;
            else if (teamData.right) return leftTeam;
            return null;
        }

        void init(final String team1, final String t1p1, final String t1p2,
            final String team2, final String t2p1, final String t2p2) {

            ArrayList<String> t1Players = new ArrayList<>();
            t1Players.add(t1p1);
            if(!t1p2.isEmpty()) t1Players.add(t1p2);

            ArrayList<String> t2Players = new ArrayList<>();
            t2Players.add(t2p1);
            if(!t2p2.isEmpty()) t2Players.add(t2p2);

            TeamData t1 = new TeamData(team1, t1Players, this);
            TeamData t2 = new TeamData(team2, t2Players, this);
            leftTeam = t1;
            leftTeam.setViews(true);
            leftTeam.setService(true);
            rightTeam = t2;
            rightTeam.setViews(false);
            rightTeam.setService(false);

            leftTeam.setBottomPlayerIdx(t1p1);
            if(!t1p2.isEmpty()) leftTeam.setTopPlayerIdx(t1p2);
            rightTeam.setTopPlayerIdx(t2p1);
            if(!t2p2.isEmpty()) rightTeam.setBottomPlayerIdx(t2p2);
        }

        void clear() {
            scores.clear();
            historyIdx = -1;
            winner = "";
            if(null!=leftTeam) leftTeam.clear();
            if(null!=rightTeam) rightTeam.clear();
        }

        void switchService(){
            if(null==leftTeam || null==rightTeam) return;
            boolean lService = leftTeam.switchService();
            boolean rService = rightTeam.switchService();
            if(!lService && !rService) {
                //safety-net: atleast one team should be serving
                leftTeam.setService(true);  //random selection
            }
        }

        void switchSides() {
            TeamData tmp = leftTeam;
            leftTeam = rightTeam;
            rightTeam = tmp;
            leftTeam.switchSides();
            rightTeam.switchSides();
        }

        boolean gameStarted() {
            if(scores.size() > 1) {
                //if there is score history, check the current index value.
                //If current idx = 0 (only 0-0 entry), then return false.
                if(historyIdx > 0) return true;
                else return false;
            } else return false;
        }

        void initHistory(final String leftDataStr, final String rightDataStr,
                         final boolean force) {
            if(force) scores.clear();
            if(scores.size()==0) {
                //Adding 0-0 to the history
                pushNewHistory(leftDataStr,rightDataStr);
            }
        }

        void pushNewHistory(final String leftDataStr, final String rightDataStr) {
            historyIdx++;
            Log.i(TAG, historyIdx + ": pushNewHistory: initial:" + scores.toString());
            while (scores.size()!=0 && scores.size()-1 >= historyIdx) {
                //Undo was done earlier and hence there is some data stored already at
                //the next index. delete the old data.
                scores.remove(scores.size()-1);
            }
            Log.i(TAG, "pushNewHistory: before Addition:" + scores.toString());
            scores.add(leftDataStr + DELIM2 + rightDataStr);
            historyIdx = scores.size()-1;
            Log.i(TAG, historyIdx + "-pushNewHistory: after Addition:" + scores.toString());
        }

        ArrayList<String> pushHistory() {
            //if(historyIdx==-1) return null;
            if (historyIdx+1 > scores.size()-1) {
                Log.i(TAG, "pushHistory: no more history:" + (historyIdx+1) + "/" + (scores.size()-1) );
                return null;
            }

            historyIdx++;
            Log.i(TAG, "pushHistory: " + historyIdx);
            String dataStr = scores.get(historyIdx);
            String[] parts = dataStr.split(DELIM2);
            if(parts.length!=2) {
                Log.e(TAG, "pushHistory: shouldnt be here:" + dataStr + ":" + parts.length );
                return null;
            }
            ArrayList<String> retStrList = new ArrayList<>();
            retStrList.add(parts[0]);
            retStrList.add(parts[1]);
            Log.i(TAG, "pushHistory: retStrList=" + retStrList.toString());
            return retStrList;
        }

        ArrayList<String> popHistory() {
            historyIdx--;
            if(historyIdx<0) { historyIdx=0; return null; }

            if (historyIdx > scores.size()-1) {
                Log.e(TAG, "popHistory: shouldnt be here:" + historyIdx + "/" + (scores.size()-1) );
                return null;
            }
            String dataStr = scores.get(historyIdx);
            String[] parts = dataStr.split(DELIM2);
            if(parts.length!=2) {
                Log.e(TAG, "popHistory: shouldnt be here:" + dataStr + ":" + parts.length );
                return null;
            }
            ArrayList<String> retStrList = new ArrayList<>();
            retStrList.add(parts[0]);
            retStrList.add(parts[1]);
            Log.i(TAG, "popHistory: retStrList=" + retStrList.toString());
            return retStrList;
        }

        ArrayList<String> getRecentHistory() {
            if(historyIdx<0) { historyIdx=0; return null; }

            if (historyIdx > scores.size()-1) {
                Log.e(TAG, "getRecentHistory: shouldnt be here:" + historyIdx + "/" + (scores.size()-1) );
                return null;
            }
            String dataStr = scores.get(historyIdx);
            String[] parts = dataStr.split(DELIM2);
            if(parts.length!=2) {
                Log.e(TAG, "getRecentHistory: shouldnt be here:" + dataStr + ":" + parts.length );
                return null;
            }
            ArrayList<String> retStrList = new ArrayList<>();
            retStrList.add(parts[0]);
            retStrList.add(parts[1]);
            Log.i(TAG, "getRecentHistory: retStrList=" + retStrList.toString());
            return retStrList;
        }


        void wonAPoint(final boolean left) {
            if(scores.size()==0) {
                //Adding 0-0 to the history, if first entry
                initHistory(mGames.get(mGameNum).leftTeam.toDataString(),
                        mGames.get(mGameNum).rightTeam.toDataString(),
                        false);
            }
            if(left) {
                rightTeam.lostPoint();
                //lost point first so that service change is set before displayPlayers() is called
                //for opp team from displayService.
                leftTeam.wonPoint();

            } else {
                leftTeam.lostPoint();
                rightTeam.wonPoint();
            }
            pushNewHistory(mGames.get(mGameNum).leftTeam.toDataString(),
                    mGames.get(mGameNum).rightTeam.toDataString());
        }

        void display() {
            ArrayList<String> dataStrList = getRecentHistory();
            if (leftTeam.validDataStringList(dataStrList)) {
                leftTeam.fromDataString(dataStrList);
                rightTeam.fromDataString(dataStrList);
                winner = "";
                isGameDone(leftTeam.getScore(),rightTeam.getScore());
            }
        }

        void undo() {
            ArrayList<String> dataStrList = popHistory();
            if (leftTeam.validDataStringList(dataStrList)) {
                leftTeam.fromDataString(dataStrList);
                rightTeam.fromDataString(dataStrList);
                winner = "";
                leftTeam.displayWinner(true);
                rightTeam.displayWinner(true);
            }
        }

        void redo() {
            ArrayList<String> dataStrList = pushHistory();
            if (leftTeam.validDataStringList(dataStrList)) {
                leftTeam.fromDataString(dataStrList);
                rightTeam.fromDataString(dataStrList);
            }
        }

        boolean isGameDone(final int leftScore, final int rightScore) {
            if(leftScore<21 && rightScore<21) return false;
            if( (leftScore-rightScore) >= 2) {
                winner = leftTeam.getTeam();
                leftTeam.displayWinner(false);
                rightTeam.displayWinner(true);
                return true;
            }
            if ( (rightScore-leftScore) >= 2) {
                winner = rightTeam.getTeam();
                rightTeam.displayWinner(false);
                leftTeam.displayWinner(true);
                return true;
            }
            if( leftScore==30 ) {
                winner = leftTeam.getTeam();
                leftTeam.displayWinner(false);
                rightTeam.displayWinner(true);
                return true;
            }
            if( rightScore==30 ) {
                winner = rightTeam.getTeam();
                rightTeam.displayWinner(false);
                leftTeam.displayWinner(true);
                return true;
            }
            Log.i(TAG, "isGameDone: nope:" + leftScore + "-" + rightScore);
            return false;
        }

        String getScore() {
            if(null!=leftTeam && null!=rightTeam) {
                if(mTeam1.isEmpty())
                    return leftTeam.getScore() + "-" + rightTeam.getScore();
                else {
                    //always set team1 score on the left side.
                    if(mTeam1.equals(leftTeam.getTeam())) return leftTeam.getScore() + "-" + rightTeam.getScore();
                    else return rightTeam.getScore() + "-" + leftTeam.getScore();
                }
            } else
                return "";
        }

        void print() {
            Log.d(TAG, "print: w=" + winner + " hI=" + historyIdx);
            if(null!=leftTeam) Log.d(TAG, "print: Left: " + leftTeam.toString());
            if(null!=rightTeam) Log.d(TAG, "print: Right: " + rightTeam.toString());
        }
    }

    // --------------- Dialog to schedule a new match -------------------
    private class TeamDialogClass extends Dialog implements
            View.OnClickListener {

        public Activity parentActivity;
        public Dialog d;
        public Button enter, cancel;


        public TeamDialogClass(final Activity a) {
            super(a);
            this.parentActivity = a;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.game_team_dialog);

            enter = findViewById(R.id.enter_button);
            enter.setOnClickListener(this);

            cancel = findViewById(R.id.cancel_button);
            cancel.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.cancel_button:
                    setTeamData("", "", "",
                            "", "", "");
                    dismiss();
                    break;
                case R.id.enter_button:
                    String t1 = ((EditText)findViewById(R.id.newTeam1_et)).getText().toString();
                    String t2 = ((EditText)findViewById(R.id.newTeam2_et)).getText().toString();
                    if(!t1.isEmpty() && t2.isEmpty()) {
                        Toast.makeText(TrackScores.this,
                                "Enter team2 name too", Toast.LENGTH_SHORT).show();
                        return;
                    } else if(!t2.isEmpty() && t1.isEmpty()) {
                        Toast.makeText(TrackScores.this,
                                "Enter team1 name too", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //if singles, just dont enter any data for player2
                    String t1p1 = ((EditText)findViewById(R.id.t1p1_et)).getText().toString();
                    String t1p2 = ((EditText)findViewById(R.id.t1p2_et)).getText().toString();
                    String t2p1 = ((EditText)findViewById(R.id.t2p1_et)).getText().toString();
                    String t2p2 = ((EditText)findViewById(R.id.t2p2_et)).getText().toString();

                    setTeamData(t1, t1p1, t1p2, t2, t2p1, t2p2);
                    dismiss();
                    break;
                default:
                    break;
            }
        }
    } //end of TeamDialogClass
}