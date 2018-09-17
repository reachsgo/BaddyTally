package com.sg0.baddytally;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CallbackRoutine{

    private static final String TAG = "MainActivity";
    private FireBaseDBReader mGoldDB;
    private FireBaseDBReader mSilverDB;
    private Menu mOptionsMenu;
    private String mClub;
    private String mInnings;
    private String mRoundName;
    private boolean mInitialAttempt;
    private boolean mRefreshing;
    private boolean mDBUpdated;
    private RecyclerViewAdapter mGoldAdapter;
    private RecyclerViewAdapter mSilverAdapter;

    @Override
    public void dbUpdated() {
        //Callback routine from adapter to indicate that the DB is updated
        mDBUpdated = true;
        Log.d(TAG, "SGO: dbUpdated callback");
    }

    private void setTitle(String club) {
        if (!TextUtils.isEmpty(club)) {
            Log.d(TAG, "setTitle: " + club + ":" + SharedData.getInstance().toString());
            String tempString = Constants.APPNAME + "  " + club;
            if (SharedData.getInstance().isAdmin()) tempString += " +";
            else if (SharedData.getInstance().isRoot()) tempString += " *";
            else tempString += " ";
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, Constants.APPNAME.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), Constants.APPNAME.length(), tempString.length()-1, 0);
            spanString.setSpan(new SuperscriptSpan(), tempString.length()-1, tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanString.setSpan(new RelativeSizeSpan(0.5f), tempString.length()-1, tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(""); //workaround for title getting truncated.
            getSupportActionBar().setTitle(spanString);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: starting");
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        mOptionsMenu = null;
        mClub = "";
        mRefreshing = true;
        mGoldAdapter = null;;
        mSilverAdapter = null;
        SharedData.getInstance().mNumOfGroups = Constants.NUM_OF_GROUPS;
        //SGO: Test single user group
        //SharedData.getInstance().mNumOfGroups = 1;

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

        findViewById(R.id.gold_header_season).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null!=mGoldAdapter) {
                    mGoldAdapter.sortOnSeason();
                    selectedEffect(R.id.gold_header_season, R.id.gold_header_innings);
                }
            }
        });

        findViewById(R.id.gold_header_innings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null!=mGoldAdapter) {
                    mGoldAdapter.sortOnInnings();
                    selectedEffect(R.id.gold_header_innings, R.id.gold_header_season);
                }
            }
        });

        findViewById(R.id.silver_header_season).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null!=mSilverAdapter) {
                    mSilverAdapter.sortOnSeason();
                    selectedEffect(R.id.silver_header_season, R.id.silver_header_innings);
                }
            }
        });

        findViewById(R.id.silver_header_innings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null!=mSilverAdapter) {
                    mSilverAdapter.sortOnInnings();
                    selectedEffect(R.id.silver_header_innings, R.id.silver_header_season);
                }
            }
        });

        //Initial effect, players are sorted on innings by default.
        selectedEffect(R.id.gold_header_innings, R.id.gold_header_season);
        selectedEffect(R.id.silver_header_innings, R.id.silver_header_season);
    }

    private void selectedEffect(final int selectedViewId, final int otherViewId){
        TextView tv = findViewById(selectedViewId);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)tv.getLayoutParams();
        params.setMargins(10, 1, 1, 10); //substitute parameters for left, top, right, bottom
        tv.setLayoutParams(params);
        tv = findViewById(otherViewId);
        params = (LinearLayout.LayoutParams)tv.getLayoutParams();
        params.setMargins(0, 0, 0, 0); //substitute parameters for left, top, right, bottom
        tv.setLayoutParams(params);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mInitialAttempt = false;
        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        String club = prefs.getString(Constants.DATA_CLUB, "");
        if (club.isEmpty()) {
            mInitialAttempt = true;
            Log.d(TAG, "SGO onResume: mInitialAttempt=" + mInitialAttempt);
            Toast.makeText(this, "Click Settings to sign-in to your club ", Toast.LENGTH_LONG)
                    .show();
        } else {
            Log.d(TAG, "SGO onResume: mInitialAttempt=" + mInitialAttempt);

            mClub = club;
            SharedData.getInstance().mClub = mClub;
            SharedData.getInstance().mUser = prefs.getString(Constants.DATA_USER, "");
            SharedData.getInstance().mRole = prefs.getString(Constants.DATA_ROLE, "");
            Log.d(TAG, "onResume: " + SharedData.getInstance().toString());
            setTitle(mClub);
            fetchInnings();
            if (mOptionsMenu != null) {
                //For scenarios where onResume() is called after onCreateOptionsMenu()
                Log.d(TAG, "SGO onResume() is called after onCreateOptionsMenu()");
                //((MenuItem) mOptionsMenu.findItem(R.id.action_settings)).setVisible(true);
                mOptionsMenu.findItem(R.id.action_summary).setVisible(true);
                MenuItem mEnterDataItem = mOptionsMenu.findItem(R.id.action_enter);
                mEnterDataItem.setTitle("Enter Score");  //coming back from initial sign in, change settings menu
                if (Constants.MEMBER.equals(SharedData.getInstance().mRole))
                    mEnterDataItem.setEnabled(false);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "SGO onActivityResult: requestCode=" + requestCode + " resultCode=" + resultCode);
        /* dont worry about the activity code, RESULT_FIRST_USER is used to know if view needs to be refreshed or not.
        switch (requestCode) {  //case (SETTINGS_ACTIVITY): {
         */

        if(resultCode == Constants.RESTARTAPP) {
            Log.d(TAG, "SGO onActivityResult: RESTARTING app");
            Intent mStartActivity = new Intent(MainActivity.this, MainActivity.class);
            int mPendingIntentId = 3331;  //some random number.
            PendingIntent mPendingIntent = PendingIntent.getActivity(MainActivity.this, 0, mStartActivity,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) MainActivity.this.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);
        }

                if (SharedData.getInstance().isDBUpdated()) {
                    // cache is cleared, refresh the main view
                    finish();
                    startActivity(getIntent());
                    SharedData.getInstance().setDBUpdated(false);
                }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        mOptionsMenu = menu;
        MenuItem mEnterDataItem = mOptionsMenu.findItem(R.id.action_enter);
        Log.d(TAG, "SGO onCreateOptionsMenu: mInitialAttempt=" + mInitialAttempt);
        if (mInitialAttempt) {
            mEnterDataItem.setTitle("Club Sign-in");
            //((MenuItem) menu.findItem(R.id.action_settings)).setVisible(false);
            ((MenuItem) menu.findItem(R.id.action_summary)).setVisible(false);
            Log.d(TAG, "SGO onCreateOptionsMenu: INITIAL ATTEMPT");
        }
        else {
            //For scenarios where onCreateOptionsMenu() is called after onResume()
            Log.d(TAG, "SGO onCreateOptionsMenu() is called after onResume");
            mEnterDataItem.setTitle("Enter Score");
            ((MenuItem) menu.findItem(R.id.action_settings)).setVisible(true);
            ((MenuItem) menu.findItem(R.id.action_summary)).setVisible(true);
            if (Constants.MEMBER.equals(SharedData.getInstance().mRole))
                mEnterDataItem.setEnabled(false);
        }
        setTitle(mClub);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                finish();
                mRefreshing = true;
                startActivity(getIntent());
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:
                /*
                if (!Constants.ROOT.equals(SharedData.getInstance().mRole)) {
                    Toast.makeText(this, "Some options might not be available to you!", Toast.LENGTH_SHORT)
                            .show();
                }*/
                Intent settingsIntent = new Intent(MainActivity.this, Settings.class);
                MainActivity.this.startActivityForResult(settingsIntent, Constants.SETTINGS_ACTIVITY);
                break;
            case R.id.action_enter:
                if (mInitialAttempt) {
                    Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
                    MainActivity.this.startActivity(myIntent);
                } else if (!SharedData.getInstance().mMemCode.isEmpty()) {
                    Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
                    //Set the Player data in shared data structure. Player data is filled in a
                    //different (asynchronous) listener in FireBaseDBReader. Overwrite the player data
                    //every time, so that even if initial calls are done too fast (before player data is
                    //filled - which will not happen unless the DB is very slow), the later calls will
                    //set the latest player data from DB.
                    SharedData data = SharedData.getInstance();
                    data.mGoldPlayers = mGoldDB.getPlayers();
                    if (mSilverDB != null) data.mSilverPlayers = mSilverDB.getPlayers();
                    Log.d(TAG, "Creating LoginActivity: data = " + data.toString());
                    MainActivity.this.startActivityForResult(myIntent,Constants.LOGIN_ACTIVITY);
                } else {
                    Toast.makeText(this, "No connectivity, try after some time...", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            case R.id.action_summary:
                if (mInitialAttempt) {
                    Toast.makeText(this, "You have to Sign-in first.", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Intent myIntent = new Intent(MainActivity.this, Summary.class);
                    MainActivity.this.startActivityForResult(myIntent, Constants.SUMMARY_ACTIVITY);
                }
                break;
            case R.id.action_rules:
                showRules();
                break;
            case R.id.action_about:
                //int versionCode = BuildConfig.VERSION_CODE;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Version: " + BuildConfig.VERSION_NAME)
                        .setTitle(SharedData.getInstance().getTitleStr(Constants.APPNAME,MainActivity.this))
                        .setNeutralButton("Ok", null).show();
                break;
            default:
                break;
        }

        return true;
    }

    private void initAdapter() {
        ArrayList<PlayerData> players = new ArrayList<>();
        PlayerData header = new PlayerData("");
        players.add(header);

        RecyclerView mRecyclerGoldView = findViewById(R.id.gold_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerGoldView.setHasFixedSize(true);
        // use a linear layout manager
        if (Build.VERSION.SDK_INT >= 21) {
            //set the background image (smashing silhouette) tint to gold, only supported for API21+
            mRecyclerGoldView.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.colorGold));
        }
        LinearLayout parent = findViewById(R.id.gold_parentview);
        LinearLayoutManager mGoldLayoutManager = new LinearLayoutManager(parent.getContext());

        //firebase DB filter allows only descending order, So, reverse the order so that highest score is shown first
        //Innings score (This round) is fetched first (see below), so that the sorting is on current round score.
        mGoldLayoutManager.setReverseLayout(true);   //without this the focus stay at the end of the list
        mGoldLayoutManager.setStackFromEnd(true);
        // SGO: Above revering not needed anymore as the sorting is now done in adapter. After DB restructuring to add win%,
        //      orderByChild on a list child doesnt seem to be working. Needs more investigation.

        mRecyclerGoldView.setLayoutManager(mGoldLayoutManager);
        //mRecyclerGoldView.addItemDecoration(new DividerItemDecoration(MainActivity.this,
        //        DividerItemDecoration.VERTICAL));
        mGoldAdapter = new RecyclerViewAdapter(this, Constants.GOLD, players);
        mGoldAdapter.setBgColor("#eee8aa");  //pale gold as background for text
        mRecyclerGoldView.setAdapter(mGoldAdapter);
        mGoldDB = new FireBaseDBReader(this, mClub, Constants.GOLD, mInnings, mGoldAdapter, mRecyclerGoldView);
        mGoldDB.fetchOverallScore();

        if (SharedData.getInstance().mNumOfGroups == 1) {
            //There is only one group, dont show silver group view.
            findViewById(R.id.silver_parentview).setVisibility(View.GONE);
            mSilverDB = null;
        } else {
            RecyclerView mRecyclerSilverView = findViewById(R.id.silver_view);
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            mRecyclerSilverView.setHasFixedSize(true);
            if (Build.VERSION.SDK_INT >= 21) {
                //set the background image (smashing silhouette) tint to gold, only supported for API21+
                mRecyclerSilverView.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.colorSilver));
            }
            // use a linear layout manager
            parent = findViewById(R.id.silver_parentview);
            LinearLayoutManager mSilverLayoutManager = new LinearLayoutManager(parent.getContext());
            //firebase DB filter allows only descending order, So, reverse the order so that highest score is shown first
            //Innings score (This round) is fetched first (see below), so that the sorting is on current round score.
            mSilverLayoutManager.setReverseLayout(true);
            mSilverLayoutManager.setStackFromEnd(true);
            mRecyclerSilverView.setLayoutManager(mSilverLayoutManager);
            mSilverAdapter = new RecyclerViewAdapter(this, Constants.SILVER, players);
            mSilverAdapter.setBgColor("#eeeee0");  //color silver
            mRecyclerSilverView.setAdapter(mSilverAdapter);
            mSilverDB = new FireBaseDBReader(this, mClub, Constants.SILVER, mInnings, mSilverAdapter, mRecyclerSilverView);
            mSilverDB.fetchOverallScore();
        }
    }

    private void fetchInnings() {
        //Read DB only on startup & refresh. Otherwise on every resume of app, data is read from DB and that adds to the DB traffic.
        if(!mRefreshing) return;

        Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT)
                .show();

        //Using runTransaction instead of addListenerForSingleValueEvent, as the later was giving stale data.
        final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference().child(SharedData.getInstance().mClub).child(Constants.INNINGS);
        inningsDBRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                GenericTypeIndicator<List<InningsDBEntry>> t = new GenericTypeIndicator<List<InningsDBEntry>>() {
                };
                List<InningsDBEntry> innings = mutableData.getValue(t);
                if(null == innings) {
                    //no innings in DB
                    SharedData.getInstance().mInnings = "";
                    return Transaction.success(mutableData);
                }

                for (int i=innings.size()-1; i>=0; i--) {  //reverse to get "true" value first
                    InningsDBEntry val = innings.get(i);
                    if (null!=val)
                        Log.v(TAG, "fetchInnings: SGO Read from DB:" + innings.indexOf(val) + " data:" + val.toString());
                    if (null!=val && val.current) {
                        mInnings = val.name;
                        SharedData.getInstance().mInnings = mInnings;
                        mRoundName = val.round;
                        SharedData.getInstance().mRoundName = mRoundName;
                        SharedData.getInstance().mInningsDBKey = innings.indexOf(val);
                        Log.v(TAG, "fetchInnings: SGO key:" + SharedData.getInstance().mInningsDBKey + " data:" + val.toString());
                        break;
                    }
                }
                //There is innings table, but may be none with "current"==true
                Log.w(TAG, "fetchInnings: returning from runTransaction");
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {

            }
        });

        /*
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.INNINGS);
        Query roundQuery = dbRef.orderByChild("current");  //ordered by "current" value: false first & then true
        roundQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<List<InningsDBEntry>> t = new GenericTypeIndicator<List<InningsDBEntry>>() {
                };
                List<InningsDBEntry> innings = dataSnapshot.getValue(t);
                if(null==innings) return;
                //Log.v(TAG, "fetchInnings: key:" + dataSnapshot.getKey());
                //for (InningsDBEntry val : innings) {
                for (int i=innings.size()-1; i>=0; i--) {  //reverse to get "true" value first
                    InningsDBEntry val = innings.get(i);
                    if (null!=val)
                        Log.v(TAG, "fetchInnings: SGO Read from DB:" + innings.indexOf(val) + " data:" + val.toString());
                    if (null!=val && val.current) {
                        mInnings = val.name;
                        SharedData.getInstance().mInnings = mInnings;
                        mRoundName = val.round;
                        SharedData.getInstance().mRoundName = mRoundName;
                        SharedData.getInstance().mInningsDBKey = innings.indexOf(val);
                        Log.v(TAG, "fetchInnings: SGO key:" + SharedData.getInstance().mInningsDBKey + " data:" + val.toString());
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "fetchInnings: onCancelled", databaseError.toException());
                Toast.makeText(MainActivity.this, "Innings DB error: " + databaseError.toString(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
        */


        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.PROFILE);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.v(TAG, "fetchProfile: onDataChange");
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    if(null==child) continue;
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
                initAdapter();
                mRefreshing = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "fetchProfile: onCancelled", databaseError.toException());
                Toast.makeText(MainActivity.this, "Profile DB error: " + databaseError.toString(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRules() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("\n1.  Players of the club are divided into 2 groups: Gold (top rank) & Silver\n\n" +
                "2.  Season is divided into several \"Innings\" (say 1 per month)\n\n" +
                "3.  Players are ranked by points. Players of a winning Doubles team are awarded 1 point each." +
                " Winner of Singles game is awarded 2 points. No points for a loss.\n\n" +
                "4.  At the start of a new Innings, points accumulated over the last Innings are considered to shuffle" +
                " the players. Shuffling rules:\n" +
                "        (i)  Top 3 players of Silver move to Gold & Bottom 3 players of Gold move to Silver. If there is a tie, player selection is done on criteria in the order:" +
                " (i.1) higher win percentage, (i.2) most number of wins" +
                " (i.3) most number of games played (i.4) toss\n" +
                "        (ii)  Once the above rule is applied, the win % of the players are compared, for those who have at least played 12 games in that month." +
                " If the player with the highest win % is in Silver Group, then he is shuffled with the player with the lowest win % from Gold group." +
                " Please note that it is quite possible that the player being moved from Gold group due to this could be a player who just moved to Gold due to rule 1" +
                " or he could be a player who was already in Gold group before applying shuffling rule 1.\n\n" +
                "5.  After warm-up, games are played among players of the same group, such that all" +
                " possible combination of pairings are covered (at least once) for that day. \n\n" +
                "6.  Any new member to club starts in the Silver group.\n\n" +
                "7.  If guests are playing, points are not counted for that game.\n\n" +
                "8.  If there are less than 4 players from a group on any game day, you should play at least 1 singles per player in attendance." +
                " Ideally, singles between all possible combinations of that group on that day should be played.\n\n" +
                "9. If there are not enough players on a day, mix and match (after the singles). Points are still counted.\n\n" +
                "10. Scores and results can be entered into ScoreTally after each game.\n\n")
                .setTitle(SharedData.getInstance().getTitleStr("Rules", MainActivity.this))
                .setNeutralButton("Ok", null)
                .show();
    }
}
