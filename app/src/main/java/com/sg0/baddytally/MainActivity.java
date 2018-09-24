package com.sg0.baddytally;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements CallbackRoutine {

    private static final String TAG = "MainActivity";
    private FireBaseDBReader mGoldDB;
    private FireBaseDBReader mSilverDB;
    private Menu mOptionsMenu;
    private String mClub;
    private String mInnings;
    private String mRoundName;
    private boolean mInitialAttempt;
    private boolean mRefreshing;
    //private boolean mDBUpdated;
    private RecyclerViewAdapter mGoldAdapter;
    private RecyclerViewAdapter mSilverAdapter;
    private Handler uiHandler;

    //private boolean[] mGoldCheckedItems = null;
    //private boolean[] mSilverCheckedItems = null;
    //private boolean[] mCheckedItems = null;
    private ArrayList<GameJournalDBEntry> mGoldPlayedGames = new ArrayList<>();
    private ArrayList<GameJournalDBEntry> mSilverPlayedGames = new ArrayList<>();

    private void setTitle(String club) {
        if (!TextUtils.isEmpty(club)) {
            Log.d(TAG, "setTitle: " + club + ":" + SharedData.getInstance().toString());
            String tempString = Constants.APPNAME + "  " + club;
            if (SharedData.getInstance().isAdmin()) tempString += " +";
            else if (SharedData.getInstance().isRoot()) tempString += " *";
            else tempString += " ";
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, Constants.APPNAME.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), Constants.APPNAME.length(), tempString.length() - 1, 0);
            spanString.setSpan(new SuperscriptSpan(), tempString.length() - 1, tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanString.setSpan(new RelativeSizeSpan(0.5f), tempString.length() - 1, tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(""); //workaround for title getting truncated.
            getSupportActionBar().setTitle(spanString);
        }
    }

    private void setFooter() {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                String footerStr = "Innings:" + mInnings;
                if (mInnings.isEmpty()) footerStr = "Innings not started";
                if (!mRoundName.isEmpty()) footerStr += "/" + SharedData.getInstance().getShortRoundName(mRoundName);
                if (!SharedData.getInstance().mUser.isEmpty())
                    footerStr += ",   logged in as " + SharedData.getInstance().mUser;
                ((TextView) findViewById(R.id.footer)).setText(footerStr);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: starting");
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        uiHandler = new Handler();
        mOptionsMenu = null;
        mClub = "";
        mInnings = "";
        mRoundName = "";
        mRefreshing = true;
        mGoldAdapter = null;
        mSilverAdapter = null;
        SharedData.getInstance().mNumOfGroups = Constants.NUM_OF_GROUPS;
        //SGO: Test single user group
        //SharedData.getInstance().mNumOfGroups = 1;

        TextView mGoldHeader = findViewById(R.id.gold_group);
        TextView mSilverHeader = findViewById(R.id.silver_group);
        mGoldHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show only Gold group, if done a 2nd time, go back to normal.
                LinearLayout silverView = findViewById(R.id.silver_parentview);
                if (silverView.getVisibility() == View.GONE)
                    silverView.setVisibility(View.VISIBLE);
                else
                    silverView.setVisibility(View.GONE);
                return;
            }
        });
        mSilverHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show only Silver group, if done a 2nd time, go back to normal.
                LinearLayout goldView = findViewById(R.id.gold_parentview);
                if (goldView.getVisibility() == View.GONE)
                    goldView.setVisibility(View.VISIBLE);
                else
                    goldView.setVisibility(View.GONE);
                return;
            }
        });

        findViewById(R.id.gold_header_season).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mGoldAdapter) {
                    mGoldAdapter.sortOnSeason();
                    selectedEffect2(R.id.gold_header_season_ll, R.id.gold_header_innings_ll);
                    //selectedEffect(R.id.gold_header_season, R.id.gold_header_innings);
                }
            }
        });

        findViewById(R.id.gold_header_innings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mGoldAdapter) {
                    mGoldAdapter.sortOnInnings();
                    selectedEffect2(R.id.gold_header_innings_ll, R.id.gold_header_season_ll);
                }
            }
        });

        findViewById(R.id.silver_header_season).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mSilverAdapter) {
                    mSilverAdapter.sortOnSeason();
                    selectedEffect2(R.id.silver_header_season_ll, R.id.silver_header_innings_ll);
                }
            }
        });

        findViewById(R.id.silver_header_innings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mSilverAdapter) {
                    mSilverAdapter.sortOnInnings();
                    selectedEffect2(R.id.silver_header_innings_ll, R.id.silver_header_season_ll);
                    //selectedEffect(R.id.silver_header_innings, R.id.silver_header_season);
                }
            }
        });

        //Initial effect, players are sorted on innings by default.
        selectedEffect2(R.id.gold_header_innings_ll, R.id.gold_header_season_ll);
        selectedEffect2(R.id.silver_header_innings_ll, R.id.silver_header_season_ll);
        //selectedEffect(R.id.silver_header_innings, R.id.silver_header_season);

        ImageButton suggestion_btn = findViewById(R.id.suggestions);
        suggestion_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(SharedData.getInstance().mGoldPresentPlayerNames!=null) {
                    showGroupPopup();
                } else {
                    //Probably the first time, user might not be aware of the button functionality, help him!
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("To get game-suggestions, select group and the players present today for the games.")
                            .setTitle(SharedData.getInstance().getTitleStr("Info!", MainActivity.this))
                            .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    showGroupPopup();
                                }
                            }).show();
                }
            }
        });

    }

    private void selectedEffect2(final int selectedViewId, final int otherViewId) {
        findViewById(selectedViewId).setBackgroundColor(getResources().getColor(R.color.colorWhite));
        findViewById(otherViewId).setBackgroundColor(getResources().getColor(R.color.colorBlack));
    }

    private void selectedEffect(final int selectedViewId, final int otherViewId) {
        TextView tv = findViewById(selectedViewId);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tv.getLayoutParams();
            params.setMargins(5, 1, 1, 5); //substitute parameters for left, top, right, bottom
            tv.setLayoutParams(params);
            tv = findViewById(otherViewId);
            params = (LinearLayout.LayoutParams) tv.getLayoutParams();
            params.setMargins(0, 0, 0, 0); //substitute parameters for left, top, right, bottom
            tv.setLayoutParams(params);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Maintain DB connection state
        SharedData.getInstance().setUpDBConnectionListener();
        mInitialAttempt = false;
        setFooter();
        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        String club = prefs.getString(Constants.DATA_CLUB, "");
        if (club.isEmpty()) {
            mInitialAttempt = true;
            Log.d(TAG, "onResume: mInitialAttempt=" + mInitialAttempt);
            Toast.makeText(this, "Click Settings to sign-in to your club ", Toast.LENGTH_LONG)
                    .show();
        } else {
            Log.d(TAG, "onResume: mInitialAttempt=" + mInitialAttempt);

            mClub = club;
            SharedData.getInstance().mClub = mClub;
            SharedData.getInstance().mUser = prefs.getString(Constants.DATA_USER, "");
            SharedData.getInstance().mRole = prefs.getString(Constants.DATA_ROLE, "");
            Log.d(TAG, "onResume: " + SharedData.getInstance().toString());
            setTitle(mClub);
            fetchInnings();
            if (mOptionsMenu != null) {
                //For scenarios where onResume() is called after onCreateOptionsMenu()
                Log.d(TAG, "onResume() is called after onCreateOptionsMenu()");
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
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + " resultCode=" + resultCode);
        /* dont worry about the activity code, RESULT_FIRST_USER is used to know if view needs to be refreshed or not.
        switch (requestCode) {  //case (SETTINGS_ACTIVITY): {
         */

        if (resultCode == Constants.RESTARTAPP) {
            Log.d(TAG, "onActivityResult: RESTARTING app");
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
        Log.d(TAG, "onCreateOptionsMenu: mInitialAttempt=" + mInitialAttempt);
        if (mInitialAttempt) {
            mEnterDataItem.setTitle("Club Sign-in");
            //((MenuItem) menu.findItem(R.id.action_settings)).setVisible(false);
            menu.findItem(R.id.action_summary).setVisible(false);
            Log.d(TAG, "onCreateOptionsMenu: INITIAL ATTEMPT");
        } else {
            //For scenarios where onCreateOptionsMenu() is called after onResume()
            Log.d(TAG, "onCreateOptionsMenu() is called after onResume");
            mEnterDataItem.setTitle("Enter Score");
            menu.findItem(R.id.action_settings).setVisible(true);
            menu.findItem(R.id.action_summary).setVisible(true);
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
                //If DB connection is sleeping, wake it up!
                SharedData.getInstance().wakeUpDBConnection();
                Intent settingsIntent = new Intent(MainActivity.this, Settings.class);
                MainActivity.this.startActivityForResult(settingsIntent, Constants.SETTINGS_ACTIVITY);
                break;
            case R.id.action_enter:
                //If DB connection is sleeping, wake it up!
                SharedData.getInstance().wakeUpDBConnection();
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
                    MainActivity.this.startActivityForResult(myIntent, Constants.LOGIN_ACTIVITY);
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
                    //If DB connection is sleeping, wake it up!
                    SharedData.getInstance().wakeUpDBConnection();
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
                        .setTitle(SharedData.getInstance().getTitleStr(Constants.APPNAME, MainActivity.this))
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
        if (!mRefreshing) return;

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
                if (null == innings) {
                    //no innings in DB
                    SharedData.getInstance().mInnings = "";
                    return Transaction.success(mutableData);
                }
                //reset the values to be read fresh from DB. This makes sure that DB is the master, in case of manual updates.
                SharedData.getInstance().mInnings = "";
                SharedData.getInstance().mRoundName = "";
                SharedData.getInstance().mInningsDBKey = -1;
                for (int i = innings.size() - 1; i >= 0; i--) {  //reverse to get "true" value first
                    InningsDBEntry val = innings.get(i);
                    if (null != val)
                        Log.v(TAG, "fetchInnings: Read from DB:" + innings.indexOf(val) + " data:" + val.toString());
                    if (null != val && val.current) {
                        mInnings = val.name;
                        SharedData.getInstance().mInnings = mInnings;
                        mRoundName = val.round;
                        SharedData.getInstance().mRoundName = mRoundName;
                        SharedData.getInstance().mInningsDBKey = innings.indexOf(val);
                        Log.v(TAG, "fetchInnings: key:" + SharedData.getInstance().mInningsDBKey + " data:" + val.toString());
                        break;
                    }
                }
                fetchGames(Constants.GOLD, mGoldPlayedGames);
                fetchGames(Constants.SILVER, mSilverPlayedGames);
                setFooter();
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {

            }
        });

        SharedData.getInstance().fetchProfile(MainActivity.this, MainActivity.this, mClub);
    }

    //Callback after profile is fetched from DB. See SharedData impl of fetchProfile()
    public void profileFetched() {
        SharedData data = SharedData.getInstance();
        Log.w(TAG, "profileFetched invoked ...." + data.toString());
        initAdapter();
        mRefreshing = false;
    }

    private void showRules() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("\n1.  Players of the club are divided into 2 groups: Gold (top rank) & Silver\n\n" +
                "2.  Season is divided into several \"Innings\" (say 1 per month)\n\n" +
                "3.  Players are ranked by points. Players of a winning Doubles team are awarded 1 point each." +
                " Winner of Singles game is awarded 2 points. No points for a loss.\n\n" +
                "4.  At the start of a new Innings, points accumulated over the last Innings are considered to shuffle" +
                " the players. Shuffling rules:\n" +
                "        (i)  Top \'N\' players (3 by default) of Silver move to Gold & Bottom \'N\' players of Gold move to Silver. If there is a tie, player selection is done on criteria in the order:" +
                " (i.1) higher win percentage, (i.2) most number of wins" +
                " (i.3) most number of games played (i.4) toss\n" +
                "        (ii)  Once the above rule is applied, the win % of the players are compared, for those who have at least played \'X\' games (12 by default) in that month." +
                " If the player with the highest win % is in Silver Group, then he/she is shuffled with the player with the lowest win % from Gold group." +
                " Please note that it is quite possible that the player being moved from Gold group due to this could be a player who just moved to Gold due to rule 1" +
                " or he/she could be a player who was already in Gold group before applying shuffling rule 1.\n\n" +
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

    private void showGroupPopup() {
        final ImageButton view = findViewById(R.id.suggestions);
        final PopupMenu popup = new PopupMenu(MainActivity.this, view);
        popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
        popup.getMenu().clear();
        Menu pMenu = popup.getMenu();
        pMenu.add(Constants.GOLD);
        pMenu.add(Constants.SILVER);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String group = menuItem.getTitle().toString();
                Log.v(TAG, "showOptions showGroupPopup:" + group);
                popup.dismiss();
                showPlayersPopup(group);
                return true;
            }
        });
        popup.show();//showing popup menu
    }

    private void showPlayersPopup(final String group) {
        final SharedData data = SharedData.getInstance();
        RecyclerViewAdapter adapter = null;

        //Players present on this game day
        Set<String> presetPlayerNames = null;
        if(data.mGoldPresentPlayerNames==null) data.mGoldPresentPlayerNames = new HashSet<>();  //initialize for the first time.
        if(data.mSilverPresentPlayerNames==null) data.mSilverPresentPlayerNames = new HashSet<>(); //initialize for the first time.

        switch (group) {
            case Constants.GOLD:
                adapter = mGoldAdapter;
                presetPlayerNames = data.mGoldPresentPlayerNames;
                break;
            case Constants.SILVER:
                adapter = mSilverAdapter;
                presetPlayerNames = data.mSilverPresentPlayerNames;
                break;
            default:
                break;
        }
        if(adapter==null) return;
        if(null==presetPlayerNames) {
            Log.v(TAG, "showPlayersPopup onClick: null presetPlayerNames");
            return;
        }
        //if(presetPlayerNames==null) presetPlayerNames = new HashSet<>();  //cant do it this way, as the class member variables like mGoldPresentPlayerNames wont be initialized

        final ArrayList<PlayerData> players = new ArrayList<>(adapter.getPlayers());  //in ascending order

        //create checked items, Remember last checked items
        boolean[] checkedItems = new boolean[players.size()];
        for (int i = 0; i < players.size(); i++) {
                if(presetPlayerNames.contains(players.get(i).getName())) checkedItems[i] = true;
                else checkedItems[i] = false;
        }

        final CharSequence[] playerNames = new CharSequence[players.size()];
        for (int i = 0; i < players.size(); i++) {
            playerNames[i] = players.get(i).getName();  //all the payer names to be shown to the user to select the players present on that day
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Select the players present today");
        builder.setMultiChoiceItems(playerNames, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                Log.v(TAG, "showPlayersPopup onClick:" + i + " :" + b);
                Set<String> presetPlayerNames = null;  //Players present on this game day
                switch (group) {
                    case Constants.GOLD:
                        presetPlayerNames = data.mGoldPresentPlayerNames;
                        break;
                    case Constants.SILVER:
                        presetPlayerNames = data.mSilverPresentPlayerNames;
                        break;
                    default:
                        break;
                }

                if(b) presetPlayerNames.add((String) playerNames[i]);   //Duplicates wont be added for Set data structure.
                else presetPlayerNames.remove(playerNames[i]);
                //if(mCheckedItems[i] != b) mCheckedItems[i] = b;
                Log.v(TAG, "showPlayersPopup onClick: presetPlayerNames=" + presetPlayerNames.toString());
            }
        });

        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Log.v(TAG, "showPlayersPopup OK onClick:" + players.toString());
                ArrayList<PlayerData> presentPlayers = new ArrayList<>(players.size());
                Set<String> presetPlayerNames = null;  //Players present on this game day
                ArrayList<GameJournalDBEntry> playedGames = null;
                //fetchGames(group, playedGames);
                switch (group) {
                    case Constants.GOLD:
                        presetPlayerNames = data.mGoldPresentPlayerNames;
                        playedGames = mGoldPlayedGames;
                        break;
                    case Constants.SILVER:
                        presetPlayerNames = data.mSilverPresentPlayerNames;
                        playedGames = mSilverPlayedGames;
                        break;
                    default:
                        break;
                }

                if(null==presetPlayerNames) {
                    Log.v(TAG, "showPlayersPopup OK onClick, NULL presetPlayerNames");
                    return;
                } else {
                    for (PlayerData p : players) {
                        if (presetPlayerNames.contains(p.getName())) presentPlayers.add(p);
                    }
                }

                Log.v(TAG, "showPlayersPopup OK onClick, presentPlayers:" + presentPlayers.toString());

                if (presentPlayers.size()<4) {
                    Toast.makeText(MainActivity.this, "Minimum of 4 players should be present for suggestions of doubles games!",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                ArrayList<PlayerData> presentPlayersReverse = new ArrayList<>(presentPlayers);
                SharedData.getInstance().sortPlayers(presentPlayersReverse, Constants.INNINGS_IDX, true, MainActivity.this, false);
                Log.v(TAG, "showPlayersPopup OK onClick, presentPlayersReverse:" + presentPlayersReverse.toString());
                ArrayList<GameJournalDBEntry> games = new ArrayList<>(playedGames);   //recommended games for the day
                SpannableStringBuilder possibleGames = new SpannableStringBuilder();  //recommended games for the day, as string to print in dialog
                SpannableStringBuilder extraGames = new SpannableStringBuilder(); //recommended extra games for the day, as string to print in dialog

                for(GameJournalDBEntry game: games) {
                    possibleGames.append(SharedData.getInstance().getColorString(
                            SharedData.getInstance().getStrikethroughString(game.toPlayersString()), Color.GRAY));
                }

                // +++ Start with recommendation logic +++

                //Add 2 equally balanced games first:
                // (a) first 4
                // (b) last 4
                if (presentPlayers.size()>4) {
                    GameJournalDBEntry game = new GameJournalDBEntry(presentPlayers.get(0).getName(),
                            presentPlayers.get(3).getName(),
                            presentPlayers.get(1).getName(),
                            presentPlayers.get(2).getName());
                    Log.v(TAG, "showPlayersPopup Adding Game1:" + game.toPlayersString());
                    games.add(game);
                    possibleGames.append(game.toPlayersString());
                    /*
                    if (playedGames.contains(game)) {
                        possibleGames.append(SharedData.getInstance().getStrikethroughString(game.toPlayersString()));
                        //possibleGames = (Spanned) TextUtils.concat( possibleGames, SharedData.getInstance().getStrikethroughString(game.toPlayersString());
                        //games.add(game);
                    } else {
                        possibleGames.append(game.toPlayersString());
                    }

                    Log.v(TAG, "showPlayersPopup(" + 0 + "): " + presentPlayers.get(0).getName() + "/" +
                            presentPlayers.get(3).getName() + " v/s " +
                            presentPlayers.get(1).getName() + "/" +
                            presentPlayers.get(2).getName()); */
                    int lastidx = presentPlayers.size()-1;
                    game = new GameJournalDBEntry(presentPlayers.get(lastidx).getName(),
                            presentPlayers.get(lastidx-3).getName(),
                            presentPlayers.get(lastidx-1).getName(),
                            presentPlayers.get(lastidx-2).getName());
                    Log.v(TAG, "showPlayersPopup Adding Game2:" + game.toPlayersString());
                    games.add(game);
                    possibleGames.append(game.toPlayersString());
                    /*
                    if (playedGames.contains(game)) {
                        possibleGames.append(SharedData.getInstance().getStrikethroughString(game.toPlayersString()));
                        //possibleGames = (Spanned) TextUtils.concat( possibleGames, SharedData.getInstance().getStrikethroughString(game.toPlayersString());
                        //games.add(game);
                    } else {
                        possibleGames.append(game.toPlayersString());
                    }
                    Log.v(TAG, "showPlayersPopup(" + 1 + "): " + presentPlayers.get(lastidx).getName() + "/" +
                            presentPlayers.get(lastidx-3).getName() + " v/s " +
                            presentPlayers.get(lastidx-1).getName() + "/" +
                            presentPlayers.get(lastidx-2).getName()); */
                }

                // Now, scatter top player, then bottom player
                //      then top-2 player (top-1 player is skipped as top-2 will be the main opponent in the top player games)
                //      then bottom-2 player, etc.
                int lowestIdx = 0;
                for (int idx = presentPlayers.size()-1; idx >= lowestIdx; idx-=2) {  //top player first; ascending order
                    scatterPlayers(presentPlayers.get(idx), presentPlayers, games, playedGames, possibleGames, extraGames);
                    //scatterPlayers.add(presentPlayers.get(idx));
                    if(idx>lowestIdx) scatterPlayers(presentPlayers.get(lowestIdx), presentPlayersReverse, games, playedGames, possibleGames, extraGames);
                    // /scatterPlayers.add(presentPlayers.get(lowestIdx));
                    lowestIdx += 2;
                }
                if(extraGames.length()>0) {
                    possibleGames.append("\nOther Possible Games:\n");
                    possibleGames.append(extraGames);
                }

                //Log.v(TAG, "showPlayersPopup OK onClick, games:" + possibleGames.toString());
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Game Suggestions")
                        .setMessage(possibleGames)
                        .setNeutralButton("Ok", null).show();
            }
        });
        AlertDialog diag = builder.show();
    }

    //For the input player, find a partner from the other end of the list (partner is bottom player, if the i/p player is top player).
    //Then, find opponents for this team (again one from either ends)
    private void scatterPlayers(final PlayerData tp1, final ArrayList<PlayerData> presentPlayers, final ArrayList<GameJournalDBEntry> games,
                                ArrayList<GameJournalDBEntry> playedGames, SpannableStringBuilder possibleGames, SpannableStringBuilder extraGames) {
        int count = 1;
        ArrayList<PlayerData> teamPlayers2 = new ArrayList<>(presentPlayers);
        teamPlayers2.remove(tp1);
        boolean found = false;
        //Find a team-mate for tp1
        for (int idx2 = 0 ; idx2 <= teamPlayers2.size()-1; idx2++) {  //bottom player as partner
            PlayerData tp2 = teamPlayers2.get(idx2);
            if(tp2==tp1) continue;
            if(playedToday(tp1, tp2, games)) continue;

            ArrayList<PlayerData> oppPlayers = new ArrayList<>(teamPlayers2);
            oppPlayers.remove(tp2);
            for (int idxo = oppPlayers.size() - 1; idxo >= 0; idxo--) {  //top player first of the remaining list
                PlayerData op1 = oppPlayers.get(idxo);
                ArrayList<PlayerData> oppPlayers2 = new ArrayList<>(oppPlayers);
                oppPlayers2.remove(op1);
                for(PlayerData op2: oppPlayers2) {
                    if (op2 == op1) continue;
                    if (playedToday(op1, op2, games)) continue;
                    if (notBalanced(tp1, tp2, op1, op2)) {
                        Log.v(TAG, "UNBALANCED showPlayersPopup: " + tp1.getName() + "/" + tp2.getName() + " v/s " + op1.getName() + "/" + op2.getName());
                        continue;
                    }
                    Log.v(TAG, "showPlayersPopup(" + count + "): " + tp1.getName() + "/" + tp2.getName() + " v/s " + op1.getName() + "/" + op2.getName());
                    count++;
                    GameJournalDBEntry game = new GameJournalDBEntry(tp1.getName(),tp2.getName(),op1.getName(),op2.getName());
                    games.add(game);
                    possibleGames.append(game.toPlayersString());
                    Log.v(TAG, "scatterPlayers Adding Game:" + game.toPlayersString());
                    /*
                    if (playedGames.contains(game)) {
                        possibleGames.append(SharedData.getInstance().getStrikethroughString(game.toPlayersString()));
                    } else {
                        possibleGames.append(game.toPlayersString());
                    } */
                    found = true; // once the opponents are found for a team, break out of the loop. We don't want to repeat the same team.
                    break;
                }  //opposite team inner loop
                if(found) break;
            }  //opposite team outer loop
        } //team-mate loop
    }

    private boolean playedToday(final PlayerData p1, final PlayerData p2, final ArrayList<GameJournalDBEntry> games) {
        for (GameJournalDBEntry game : games) {
            if (game.playedBefore(p1.getName(), p2.getName(), "", "")) return true;
        }
        return false;
    }

    private boolean notBalanced(final PlayerData p1, final PlayerData p2, final PlayerData p3, final PlayerData p4) {
        int t1_total = p1.getPointsInt_innings()+p2.getPointsInt_innings();
        int t2_total = p3.getPointsInt_innings()+p4.getPointsInt_innings();
        if(t1_total+t2_total < 10) return false;

        if((t1_total <= (t2_total/2)) || (t2_total <= (t1_total/2))) {
            return true;
        }

        return false;
    }

    private void fetchGames(final String group, final ArrayList<GameJournalDBEntry> gameList){
        Log.d(TAG, "======== fetchGames ========");
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.JOURNAL).child(mInnings).child(mRoundName).child(group);
        Query myQuery = dbRef.orderByKey();
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    GameJournalDBEntry jEntry = child.getValue(GameJournalDBEntry.class);
                    if(null==jEntry) continue;
                    gameList.add(jEntry);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "DB error while fetching games: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
                return;
            }
        });
    }
}
