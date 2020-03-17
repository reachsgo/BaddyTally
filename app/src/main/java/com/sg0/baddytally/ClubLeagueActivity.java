package com.sg0.baddytally;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.telephony.PhoneNumberUtils;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class ClubLeagueActivity extends AppCompatActivity implements CallbackRoutine {

    private static final String TAG = "ClubLeagueActivity";
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
    private SharedData mCommon;
    private Runnable progressTimeOutRunnable;

    //private boolean[] mGoldCheckedItems = null;
    //private boolean[] mSilverCheckedItems = null;
    //private boolean[] mCheckedItems = null;
    private ArrayList<GameJournalDBEntry> mGoldPlayedGames = new ArrayList<>();
    private ArrayList<GameJournalDBEntry> mSilverPlayedGames = new ArrayList<>();
    private GestureDetector mDetector;
    private AlertDialog mDemoAlert;

    private void setTitle(String club) {
        if (!TextUtils.isEmpty(club)) {
            //Log.d(TAG, "setTitle: " + club + ":" + SharedData.getInstance().toString());
            String tempString = Constants.APPSHORT + "  " + club;
            if (SharedData.getInstance().isAdmin()) tempString += " +";
            else if (SharedData.getInstance().isSuperPlus()) tempString += " *";
            else tempString += " ";
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0,
                    Constants.APPSHORT.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC),
                    Constants.APPSHORT.length(), tempString.length() - 1, 0);
            spanString.setSpan(new SuperscriptSpan(), tempString.length() - 1,
                    tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanString.setSpan(new RelativeSizeSpan(0.5f), tempString.length() - 1,
                    tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        setContentView(R.layout.activity_clubleague);
        //Log.d(TAG, "onCreate: starting");
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
        mDemoAlert = null;
        mCommon = SharedData.getInstance();
        mCommon.mNumOfGroups = Constants.NUM_OF_GROUPS;
        //SGO: Test single user group
        //SharedData.getInstance().mNumOfGroups = 1;

        //Log.d(TAG, "onCreate: ");

        //do initAdapter once here, to avoid below error:
        //RecyclerView: No adapter attached; skipping layout
        initAdapter(); //this will be done again from profileFetched()

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
            }
        });

        TextView gold_header_season = findViewById(R.id.gold_header_season);
        gold_header_season.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mGoldAdapter) {
                    mGoldAdapter.sortOnSeason();
                    selectedEffect2(R.id.gold_header_season_ll, R.id.gold_header_innings_ll);
                    //selectedEffect(R.id.gold_header_season, R.id.gold_header_innings);
                }
            }
        });
        gold_header_season.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (null == mGoldAdapter || null == mSilverAdapter) return false;
                mGoldAdapter.setFullListOfPlayers(mSilverAdapter.getPlayers());
                mGoldAdapter.sortAllOnSeason();
                selectedEffect2(R.id.gold_header_season_ll, R.id.gold_header_innings_ll);
                return true;
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

        TextView silver_header_season = findViewById(R.id.silver_header_season);
        silver_header_season.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mSilverAdapter) {
                    mSilverAdapter.sortOnSeason();
                    selectedEffect2(R.id.silver_header_season_ll, R.id.silver_header_innings_ll);
                }
            }
        });
        silver_header_season.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (null == mGoldAdapter || null == mSilverAdapter) return false;
                mSilverAdapter.setFullListOfPlayers(mGoldAdapter.getPlayers());
                mSilverAdapter.sortAllOnSeason();
                selectedEffect2(R.id.silver_header_season_ll, R.id.silver_header_innings_ll);
                return true;
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

        ImageButton send_msg = findViewById(R.id.send_msg);
        send_msg.setOnClickListener(sendSMSOnClickListener);

        ImageButton suggestion_btn = findViewById(R.id.suggestions);
        suggestion_btn.setOnClickListener(suggestionsOnClickListener);

        //made a member to remove just this from the queue when DB read is successful.
        //postDelayed with object as param is not supported before API 28.
        progressTimeOutRunnable = new Runnable() {
            @Override
            public void run() {
                //what to do in case of progress-bar timeout (DB_READ_TIMEOUT here)
                mCommon.stopProgressDialog(ClubLeagueActivity.this);
                if(!mCommon.isDBConnected()) {
                    Toast.makeText(ClubLeagueActivity.this,
                            "Check your internet connection",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ClubLeagueActivity.this,
                            "No ongoing league innings for this club.",
                            Toast.LENGTH_SHORT).show();
                }

                //doing a finish() here gives the below errors. Only seen in logs.
                //Note that these errors are not visible to the app user.
                //2020-02-20 18:40:33.001 1083-1099/com.sg0.baddytally E/System: Uncaught exception thrown by finalizer
                //2020-02-20 18:40:33.003 1083-1099/com.sg0.baddytally E/System: java.lang.NullPointerException: Attempt to invoke virtual method 'boolean com.android.org.conscrypt.SslWrapper.isClosed()' on a null object reference
                //at com.android.org.conscrypt.ConscryptFileDescriptorSocket.free(ConscryptFileDescriptorSocket.java:986)
                //at com.android.org.conscrypt.ConscryptFileDescriptorSocket.finalize(ConscryptFileDescriptorSocket.java:1014)

                //But, then that error is seen multiple times even when the app is
                //minimized. May be its an issue in conscrypt?
                //refer: https://github.com/google/conscrypt/issues/792
                //Or if there is an issue in this app, it needs to be looked into.

                //Looks like these are due to failing firebase calls.
                // Note1: **Happens only when internet connection is not present.**
                // Note2: Issue is not seen on Sony phone (5.1.1 android)
                //mCommon.delayedKillActivity(uiHandler, ClubLeagueActivity.this,
                //                Constants.SHOWTOAST_TIMEOUT);

                /*
                We should have the Club League up and running, even if there are no players or innings.
                So that, user can go to settings and add players and start an innings.
                uiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onBackPressed();  //go back to MainSelection2 screen
                    }
                }, Constants.SHOWTOAST_TIMEOUT);
                 */


            }
        };

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        setUpGesture();
        mCommon.mTime = Calendar.getInstance().getTime().getTime();
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

    void refresh() {
        if(Calendar.getInstance().getTime().getTime() - mCommon.mTime > Constants.REFRESH_TIMEOUT) {
            mCommon.mTime = Calendar.getInstance().getTime().getTime();
            Toast.makeText(ClubLeagueActivity.this,
                    "Refreshing...", Toast.LENGTH_SHORT).show();
            recreate();
            //Following is the sequence invoked after recreate() is called:
            //02-24 20:00:25.843 31762-31762/com.sg0.baddytally D/ClubLeagueActivity: onDestroy:
            //02-24 20:00:25.888 31762-31762/com.sg0.baddytally D/ClubLeagueActivity: onCreate:
            //02-24 20:00:25.891 31762-31762/com.sg0.baddytally D/ClubLeagueActivity: onResume:
        }
    }

    @Override
    protected void onResume() {
        //Log.d(TAG, "onResume: ");
        super.onResume();
        mInitialAttempt = false;
        setFooter();
        //SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        //String club = prefs.getString(Constants.DATA_CLUB, "");
        String club = mCommon.mClub;

        if (mCommon.isDBUpdated()) {
            mRefreshing = true;
            mCommon.setDBUpdated(false);
        }

        if (club.isEmpty()) {
            mInitialAttempt = true;
            Log.d(TAG, "onResume: club=empty, mInitialAttempt=" + mInitialAttempt);
            Toast.makeText(this, "You have to Sign-in first.", Toast.LENGTH_LONG)
                    .show();
        } else {
            mClub = club;
            //Log.d(TAG, mInitialAttempt + ":onResume: " + SharedData.getInstance().toString());
            setTitle(mClub);
            fetchInnings();
            
            if (mOptionsMenu != null) {
                //For scenarios where onResume() is called after onCreateOptionsMenu()
                //Log.d(TAG, "onResume() is called after onCreateOptionsMenu()");
                //((MenuItem) mOptionsMenu.findItem(R.id.action_settings)).setVisible(true);
                mOptionsMenu.findItem(R.id.action_summary).setVisible(true);
                MenuItem mEnterDataItem = mOptionsMenu.findItem(R.id.action_enter);
                mEnterDataItem.setTitle("Enter Score");  //coming back from initial sign in, change settings menu
                if (!mCommon.isAdminPlus()) mEnterDataItem.setEnabled(false);
            }
        }

        showDemoDialogIfNeeded();
    }


    @Override
    public void onBackPressed() {
        //killActivity is required here. If not, pressing back from MainSelection2
        //will take us back to this activity.
        mCommon.killActivity(this, RESULT_OK);
        Intent intent = new Intent(ClubLeagueActivity.this, MainSelection2.class);
        /*If FLAG_ACTIVITY_CLEAR_TOP set, and the activity being launched is already running in
        the current task, then instead of launching a new instance of that activity, all of the
        other activities on top of it will be closed and this Intent will be delivered to the
        (now on top) old activity as a new Intent. */
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacksAndMessages(null);
        //Log.d(TAG, "onDestroy: " + mCommon.mCount);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + " resultCode=" + resultCode);
        /* dont worry about the activity code, RESULT_FIRST_USER is used to know if view needs to be refreshed or not.
        switch (requestCode) {  //case (SETTINGS_ACTIVITY): {
         */

        if (resultCode == Constants.RESTARTAPP) {
            Log.w(TAG, "onActivityResult: RESTARTING app");
            mCommon.restartApplication(ClubLeagueActivity.this, MainSigninActivity.class);
            /*Intent mStartActivity = new Intent(ClubLeagueActivity.this, MainSigninActivity.class);
            int mPendingIntentId = 3331;  //some random number.
            PendingIntent mPendingIntent = PendingIntent.getActivity(ClubLeagueActivity.this, 0, mStartActivity,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) ClubLeagueActivity.this.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);*/
        }

        if (mCommon.isDBUpdated()) {
            // cache is cleared, refresh the main view
            finish();
            startActivity(getIntent());
            mCommon.setDBUpdated(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        mOptionsMenu = menu;
        MenuItem mEnterDataItem = mOptionsMenu.findItem(R.id.action_enter);
        //Log.d(TAG, "onCreateOptionsMenu: mInitialAttempt=" + mInitialAttempt);
        if (mInitialAttempt) {
            mEnterDataItem.setTitle("Club Sign-in");
            //((MenuItem) menu.findItem(R.id.action_settings)).setVisible(false);
            menu.findItem(R.id.action_summary).setVisible(false);
            //Log.d(TAG, "onCreateOptionsMenu: INITIAL ATTEMPT");
        } else {
            //For scenarios where onCreateOptionsMenu() is called after onResume()
            //Log.d(TAG, "onCreateOptionsMenu() is called after onResume");
            mEnterDataItem.setTitle("Enter Score");
            menu.findItem(R.id.action_settings).setVisible(true);
            menu.findItem(R.id.action_summary).setVisible(true);
            if (Constants.MEMBER.equals(mCommon.mRole))
                mEnterDataItem.setEnabled(false);
            menu.findItem(R.id.action_new_tourna).setVisible(true);
        }
        setTitle(mClub);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                refresh();
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:
                //If DB connection is sleeping, wake it up!
                mCommon.wakeUpDBConnectionProfile();
                Intent myIntent = new Intent(ClubLeagueActivity.this, ClubLeagueSettings.class);
                myIntent.putExtra(Constants.ACTIVITY, Constants.ACTIVITY_SETTINGS);
                ClubLeagueActivity.this.startActivity(myIntent);
                break;
            case R.id.action_enter:
                //If DB connection is sleeping, wake it up!
                mCommon.wakeUpDBConnection();
                if (mInitialAttempt) {
                    myIntent = new Intent(ClubLeagueActivity.this, LoginActivity.class);
                    ClubLeagueActivity.this.startActivity(myIntent);
                } else if (!mCommon.mProfile.getMc().isEmpty()) {
                    myIntent = new Intent(ClubLeagueActivity.this, LoginActivity.class);
                    myIntent.putExtra(Constants.ACTIVITY, Constants.ACTIVITY_CLUB_ENTERDATA);
                    //Set the Player data in shared data structure. Player data is filled in a
                    //different (asynchronous) listener in FireBaseDBReader. Overwrite the player data
                    //every time, so that even if initial calls are done too fast (before player data is
                    //filled - which will not happen unless the DB is very slow), the later calls will
                    //set the latest player data from DB.
                    mCommon.mGoldPlayers = mGoldDB.getPlayers();
                    if (mSilverDB != null) mCommon.mSilverPlayers = mSilverDB.getPlayers();
                    //Log.d(TAG, "Creating LoginActivity: data = " + data.toString());
                    ClubLeagueActivity.this.startActivityForResult(myIntent, Constants.LOGIN_ACTIVITY);
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
                    mCommon.wakeUpDBConnection();
                    Intent mySumIntent = new Intent(ClubLeagueActivity.this, ClubLeagueSummary.class);
                    ClubLeagueActivity.this.startActivityForResult(mySumIntent, Constants.SUMMARY_ACTIVITY);
                }
                break;
            case R.id.action_rules:
                showRules();
                break;
            case R.id.action_new_tourna:
                if (mInitialAttempt) {
                    Toast.makeText(this, "You have to Sign-in first.",
                            Toast.LENGTH_SHORT).show();
                    break;
                }

                //Log.w(TAG, "Create internal tournament for " + mClub);
                if (!mCommon.isSuperPlus()) {
                    Toast.makeText(ClubLeagueActivity.this,
                            "You don't have permission to do this!" ,
                            Toast.LENGTH_SHORT).show();
                    break;
                }

                mCommon.mGoldPlayers = mGoldDB.getPlayers();
                if (mSilverDB != null) mCommon.mSilverPlayers = mSilverDB.getPlayers();

                mCommon.wakeUpDBConnection();
                Intent myTournaIntent = new Intent(ClubLeagueActivity.this,
                        ClubLeagueCreateTourna.class);
                ClubLeagueActivity.this.startActivity(myTournaIntent);

                /*
                final CharSequence[] groupNames = new CharSequence[Constants.NUM_OF_GROUPS];
                final Set<String> selectedGroups = new HashSet<>();
                groupNames[0] = Constants.GOLD; selectedGroups.add(Constants.GOLD);
                groupNames[1] = Constants.SILVER; selectedGroups.add(Constants.SILVER);
                boolean[] checkedItems = new boolean[Constants.NUM_OF_GROUPS];
                checkedItems[0] = checkedItems[1] = true;
                Log.v(TAG, "init: selectedGroups=" + selectedGroups.toString());


                AlertDialog.Builder tbuilder = new AlertDialog.Builder(ClubLeagueActivity.this);
                tbuilder.setTitle("Select the players present today");
                tbuilder.setMultiChoiceItems(groupNames, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        if(b) selectedGroups.add((String) groupNames[i]);   //Duplicates wont be added for Set data structure.
                        else selectedGroups.remove(groupNames[i]);
                        //if(mCheckedItems[i] != b) mCheckedItems[i] = b;
                        Log.v(TAG, "setMultiChoiceItems: onClick: selectedGroups=" + selectedGroups.toString());
                    }
                });


                tbuilder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.v(TAG, "setNeutralButton: selectedGroups=" + selectedGroups.toString());

                        final CharSequence[] formats = new CharSequence[3];
                        formats[0] = Constants.SE_LONG;
                        formats[1] = Constants.DE_LONG;
                        formats[2] = Constants.LEAGUE;
                        Log.v(TAG, "init: selectedGroups=" + selectedGroups.toString());

                        final AlertDialog.Builder tbuilder2 = new AlertDialog.Builder(ClubLeagueActivity.this);
                        tbuilder2.setTitle("Select the tournament format");
                        tbuilder2.setSingleChoiceItems(formats, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {
                                Log.v(TAG, "selected Format: " + formats[i]);
                                Toast.makeText(ClubLeagueActivity.this,
                                        formats[i] +" selected.", Toast.LENGTH_SHORT)
                                        .show();
                                uiHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        dialogInterface.dismiss();
                                    }
                                }, 800);
                            }
                        });
                        tbuilder2.show();
                    }
                });
                tbuilder.show();
                */
                break;

            case R.id.action_help:
                AlertDialog.Builder hBuilder = new AlertDialog.Builder(ClubLeagueActivity.this);
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
                SharedData.showAboutAlert(ClubLeagueActivity.this);
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
            mRecyclerGoldView.setBackgroundTintList(ContextCompat.getColorStateList(ClubLeagueActivity.this, R.color.colorGold));
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
        //mRecyclerGoldView.addItemDecoration(new DividerItemDecoration(ClubLeagueActivity.this,
        //        DividerItemDecoration.VERTICAL));
        mGoldAdapter = new RecyclerViewAdapter(this, Constants.GOLD, players);
        mGoldAdapter.setBgColor("#eee8aa");  //pale gold as background for text
        mRecyclerGoldView.setAdapter(mGoldAdapter);
        mGoldDB = new FireBaseDBReader(this, mClub, Constants.GOLD, mInnings,
                mGoldAdapter, mRecyclerGoldView);
        mGoldDB.fetchOverallScore();

        if (mCommon.mNumOfGroups == 1) {
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
                mRecyclerSilverView.setBackgroundTintList(
                        ContextCompat.getColorStateList(
                                ClubLeagueActivity.this, R.color.colorSilver));
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
            mSilverDB = new FireBaseDBReader(this, mClub, Constants.SILVER, mInnings,
                    mSilverAdapter, mRecyclerSilverView);
            mSilverDB.fetchOverallScore();
        }
    }

    private void fetchInnings() {
        //Read DB only on startup & refresh. Otherwise on every resume of app, data is read from DB and that adds to the DB traffic.
        if (!mRefreshing) {
            return;
        }

        mCommon.startProgressDialog(ClubLeagueActivity.this,
                "Fetching data", "Connecting...");
        //postDelayed with object is supported only from API 28.
        //timeout-event to be deleted from profileFetched()
        uiHandler.postDelayed(progressTimeOutRunnable, Constants.DB_READ_TIMEOUT);

        //Log.v(TAG, "fetchInnings: ....");

        //Using runTransaction instead of addListenerForSingleValueEvent, as the later was giving stale data.
        final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.INNINGS);
        inningsDBRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Log.d(TAG, "doTransaction: fetchInnings");
                GenericTypeIndicator<List<InningsDBEntry>> t = new GenericTypeIndicator<List<InningsDBEntry>>() {
                };
                //In this impl, innings has to be a list, so the entries should be in sequence and should start at 0.
                List<InningsDBEntry> innings = mutableData.getValue(t);
                if (null == innings) {
                    //no innings in DB
                    mCommon.mInnings = "";
                    mCommon.mRoundName = "";
                    mCommon.mInningsDBKey = -1;
                    Log.v(TAG, "fetchInnings: null");
                    //doTransaction with null input is called once always.
                    //Even if the DB is not connected at this time, it will get connected
                    //between this callback and the next one with real data.
                    //02-24 16:58:04.978 11480-11525/com.sg0.baddytally D/ClubLeagueActivity: doTransaction: fetchInnings
                    //02-24 16:58:04.980 11480-11525/com.sg0.baddytally V/ClubLeagueActivity: fetchInnings: null
                    //02-24 16:58:05.338 11480-11480/com.sg0.baddytally D/SharedData: isDBConnected: connected
                    //02-24 16:58:05.420 11480-11525/com.sg0.baddytally D/ClubLeagueActivity: doTransaction: fetchInnings
                    //02-24 16:58:05.421 11480-11525/com.sg0.baddytally V/ClubLeagueActivity: fetchInnings: key:1 data:InningsEntry{name='Inn2', current=true, round='2020-02-23T17:31'}
                    return Transaction.success(mutableData);
                }

                //reset the values to be read fresh from DB. This makes sure that DB is the master,
                //in case of manual updates.
                mCommon.mInnings = "";
                mCommon.mRoundName = "";
                mCommon.mInningsDBKey = -1;
                for (int i = innings.size() - 1; i >= 0; i--) {  //reverse to get "true" value first
                    InningsDBEntry val = innings.get(i);
                    //if (null != val)
                    //    Log.v(TAG, "fetchInnings: Read from DB:" + innings.indexOf(val) +
                    //            " data:" + val.toString());
                    if (null != val && val.current) {
                        mInnings = val.name;
                        mCommon.mInnings = mInnings;
                        mRoundName = val.round;
                        mCommon.mRoundName = mRoundName;
                        mCommon.mInningsDBKey = innings.indexOf(val);
                        Log.v(TAG, "fetchInnings: key:" + mCommon.mInningsDBKey
                                + " data:" + val.toString());

                        if (mOptionsMenu == null) break;

                        if (mCommon.mInnings.isEmpty()) {
                            //no innings configured in DB yet
                            mOptionsMenu.findItem(R.id.action_enter).setVisible(false);
                            mOptionsMenu.findItem(R.id.action_summary).setVisible(false);
                        } else {
                            mOptionsMenu.findItem(R.id.action_enter).setVisible(true);
                            mOptionsMenu.findItem(R.id.action_summary).setVisible(true);
                        }
                        break;
                    }
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b,
                                   @Nullable DataSnapshot dataSnapshot) {
                Log.d(TAG, "onComplete: fetchInnings");
                //when you are here, you are sure that DB is connected. Even if there is no innings
                //created yet, onComplete will be invoked.
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //do less in the firebase callback
                        mCommon.fetchProfile(ClubLeagueActivity.this, ClubLeagueActivity.this, mClub);
                        fetchGames(Constants.GOLD, mGoldPlayedGames, mCommon.mGoldPresentPlayerNames);
                        fetchGames(Constants.SILVER, mSilverPlayedGames, mCommon.mSilverPresentPlayerNames);
                    }
                });
                setFooter();
            }
        });
    }

    //CallbackRoutine Callback after profile is fetched from DB. See SharedData impl of fetchProfile()
    public void profileFetched() {
        Log.i(TAG, "profileFetched invoked: " + mCommon.toString());
        initAdapter();
        mRefreshing = false;

        //stop progress and remove timeout messages
        uiHandler.removeCallbacks(progressTimeOutRunnable);
        mCommon.stopProgressDialog(ClubLeagueActivity.this);
    }

    public void alertResult(final String in, final Boolean ok, final Boolean ko) {
    }

    public void completed(final String in, final Boolean ok) {
    }

    public void callback(final String key, final Object inobj) {}

    private void showRules() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueActivity.this);
        builder.setMessage("\n1.  Players of the club are divided into 2 pools: Gold (top rank) & Silver\n\n" +
                "2.  Season is divided into several \"Innings\" (say 1 per month)\n\n" +
                "3.  Players are ranked by points. Winner(s) of a game is awarded 1 point. No points for a loss.\n\n" +
                "4.  At the start of a new Innings, points accumulated over the last Innings are considered to shuffle" +
                " the players. Shuffling rules:\n" +
                "        (i)  Top \'N\' players (3 by default) of Silver move to Gold & Bottom \'N\' players of Gold move to Silver. If there is a tie, player selection is done on criteria in the order:\n" +
                " (i.1) Higher points, win%, number of wins and number of games played (in that order) in current innings\n" +
                " (i.2) Higher points, win%, number of wins and number of games played (in that order) in the whole season\n" +
                " (i.3) Random pick (toss)\n" +
                "        (ii)  Once the above rule is applied, the win % of the players are compared, for those who have at least played \'X\' games (12 by default) in that month." +
                " If the player with the highest win % is in Silver pool, then he/she is shuffled with the player with the lowest win % from Gold pool." +
                " Please note that it is quite possible that the player being moved from Gold pool due to this could be a player who just moved to Gold due to rule 1" +
                " or he/she could be a player who was already in Gold pool before applying shuffling rule 1.\n\n" +
                "5.  After warm-up, games are played among players of the same pool, such that all" +
                " possible combination of pairings are covered (at least once) for that day. \n\n" +
                "6.  Any new member to club starts in the Silver pool.\n\n" +
                "7.  If guests are playing, points are not counted for that game.\n\n" +
                "8.  If there are less than 4 players from a pool on any game day, you could start with Singles." +
                " If Singles are not desired, then mix the pools and play random teams (points are not counted).\n\n" +
                "9. If there are not enough players on a day, mix and match (after the singles). Points are not counted in that case.\n\n" +
                "10. Scores and results can be entered into ScoreTally after each game.\n\n")
                .setTitle(mCommon.getTitleStr("Rules", ClubLeagueActivity.this))
                .setNeutralButton("Ok", null)
                .show();
    }

    //show game suggestions for the players present on that day.
    private View.OnClickListener suggestionsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCommon.mGoldPresentPlayerNames != null) {
                showGroupPopup();
            } else {
                //Probably the first time, user might not be aware of the button functionality, help him!
                AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueActivity.this);
                builder.setMessage("To get game-suggestions, select group and the players present today for the games.")
                        .setTitle(mCommon.getTitleStr("Info!", ClubLeagueActivity.this))
                        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                showGroupPopup();
                            }
                        }).show();
            }
        }
    };

    private void showGroupPopup() {
        final ImageButton view = findViewById(R.id.suggestions);
        Context wrapper = new ContextThemeWrapper(ClubLeagueActivity.this, R.style.RegularPopup);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
        popup.getMenu().clear();
        Menu pMenu = popup.getMenu();
        pMenu.add(Constants.GOLD);
        pMenu.add(Constants.SILVER);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String group = menuItem.getTitle().toString();
                //Log.v(TAG, "showOptions showGroupPopup:" + group);
                popup.dismiss();
                showPlayersPopup(group);
                return true;
            }
        });
        popup.show();//showing popup menu
    }

    private void showPlayersPopup(final String group) {
        RecyclerViewAdapter adapter = null;

        //Players present on this game day
        Set<String> presetPlayerNames = null;

        switch (group) {
            case Constants.GOLD:
                adapter = mGoldAdapter;
                presetPlayerNames = mCommon.mGoldPresentPlayerNames;
                break;
            case Constants.SILVER:
                adapter = mSilverAdapter;
                presetPlayerNames = mCommon.mSilverPresentPlayerNames;
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

        AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueActivity.this);
        builder.setTitle("Select the players present today");
        builder.setMultiChoiceItems(playerNames, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                //Log.v(TAG, "showPlayersPopup onClick:" + i + " :" + b);
                Set<String> presetPlayerNames = null;  //Players present on this game day
                switch (group) {
                    case Constants.GOLD:
                        presetPlayerNames = mCommon.mGoldPresentPlayerNames;
                        break;
                    case Constants.SILVER:
                        presetPlayerNames = mCommon.mSilverPresentPlayerNames;
                        break;
                    default:
                        break;
                }

                if(b) presetPlayerNames.add((String) playerNames[i]);   //Duplicates wont be added for Set data structure.
                else //noinspection SuspiciousMethodCalls
                    presetPlayerNames.remove(playerNames[i]);
                //if(mCheckedItems[i] != b) mCheckedItems[i] = b;
                //Log.v(TAG, "showPlayersPopup onClick: presetPlayerNames=" + presetPlayerNames.toString());
            }
        });

        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Log.v(TAG, "showPlayersPopup OK onClick:" + players.toString());
                ArrayList<PlayerData> presentPlayers = new ArrayList<>(players.size());
                Set<String> presetPlayerNames = null;  //Players present on this game day
                ArrayList<GameJournalDBEntry> playedGames = null;
                switch (group) {
                    case Constants.GOLD:
                        presetPlayerNames = mCommon.mGoldPresentPlayerNames;
                        playedGames = mGoldPlayedGames;
                        break;
                    case Constants.SILVER:
                        presetPlayerNames = mCommon.mSilverPresentPlayerNames;
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
                        if (presetPlayerNames.contains(p.getName())) {
                            p.resetGamesPlayed_innings();
                            presentPlayers.add(p);
                            //Log.v(TAG, "SGO Players:" + p.toString());
                        }
                    }
                }

                //We want to do the game suggestions as per season points.
                mCommon.sortPlayers(presentPlayers, Constants.SEASON_IDX, false,
                        ClubLeagueActivity.this, false); //in ascending order
                Log.v(TAG, "showPlayersPopup OK onClick, presentPlayers:" +
                        presentPlayers.toString());

                if (presentPlayers.size()<4) {
                    Toast.makeText(ClubLeagueActivity.this,
                            "Minimum of 4 players should be present for suggestions of doubles games!",
                            Toast.LENGTH_LONG).show();
                    return;
                }


                ArrayList<GameJournalDBEntry> games = new ArrayList<>(playedGames);   //recommended games for the day
                SpannableStringBuilder possibleGames = new SpannableStringBuilder();  //recommended games for the day, as string to print in dialog
                SpannableStringBuilder extraGames = new SpannableStringBuilder(); //recommended extra games for the day, as string to print in dialog

                for(GameJournalDBEntry game: games) {
                    possibleGames.append(mCommon.getColorString(
                            mCommon.getStrikethroughString(game.toPlayersString()), Color.GRAY));
                    for (PlayerData p : presentPlayers) {
                        // GamesPlayed_innings is used to count the number of games suggested for each player.
                        // This is not used in current code to balance the suggested games, but might be useful in the future.
                        if (game.playerInvolved(p.getName())) p.incrGamesPlayed_innings();
                    }
                }

                // +++ Start with game suggestion logic +++
                // Its impossible to automatically generate all balanced games, with equal number of games for all players
                // and with balanced skill levels, but at the same time provide a chance for good competitive games.
                // Thus, it is decided to "suggest" only few balanced/competitive games depending on season points
                // (which will be more consistent over time compared to innings points) and let them rest of the games
                // be decided by the players.

                // Start with skillfully competitive games. Increments of 2, to restrict the number of games for the players in the middle.
                // If the set of 4 is a sliding window moving down by 1, then the players in the middle will have the bulk of the games,
                // while players at either ends will be deprived of games.

                for (int idx = presentPlayers.size() - 1; idx >= 3; idx -= 2) {  //starting from top player, pick 4 each
                    if (!playedToday(presentPlayers.get(idx), presentPlayers.get(idx - 3), games) &&
                            !playedToday(presentPlayers.get(idx - 1), presentPlayers.get(idx - 2), games)) {
                        GameJournalDBEntry game = new GameJournalDBEntry(presentPlayers.get(idx).getName(),
                                presentPlayers.get(idx - 3).getName(),
                                presentPlayers.get(idx - 1).getName(),
                                presentPlayers.get(idx - 2).getName());
                        Log.v(TAG, "showPlayersPopup Adding Game of 4:" + game.toPlayersString());
                        games.add(game);
                        possibleGames.append(game.toPlayersString());
                        presentPlayers.get(idx).incrGamesPlayed_innings();
                        presentPlayers.get(idx - 3).incrGamesPlayed_innings();
                        presentPlayers.get(idx - 1).incrGamesPlayed_innings();
                        presentPlayers.get(idx - 2).incrGamesPlayed_innings();
                    }
                }

                // Play few balanced games from players selected from either ends to form a team.
                for (int x = 0; x < presentPlayers.size() / 2; x += 2) {
                    ArrayList<PlayerData> playerDataArrayList1 = new ArrayList<>(presentPlayers);
                    mCommon.sortPlayers(playerDataArrayList1, Constants.SEASON_IDX,
                            true, ClubLeagueActivity.this, false);
                    scatterPlayers(playerDataArrayList1.get(x), presentPlayers, games,
                            playerDataArrayList1, possibleGames);
                }

                // Add few more skillfully competitive games, select the players from bottom this time.
                // This will also help if there are odd number of players present on a game day.
                for (int idx = 0; idx <= presentPlayers.size() - 4; idx += 2) {  //starting from bottom player, pick 4 each
                    if (!playedToday(presentPlayers.get(idx), presentPlayers.get(idx + 3), games) &&
                            !playedToday(presentPlayers.get(idx + 1), presentPlayers.get(idx + 2), games)) {
                        PlayerData tp1 = presentPlayers.get(idx);
                        PlayerData tp2 = presentPlayers.get(idx + 3);
                        PlayerData op1 = presentPlayers.get(idx + 1);
                        PlayerData op2 = presentPlayers.get(idx + 2);

                        if (playedToday(op1, op2, games)) {
                            Log.v(TAG, "playedToday showPlayersPopup rev: " + tp1.getName() + "/" + tp2.getName() + " v/s " + op1.getName() + "/" + op2.getName());
                            continue;
                        }
                        if (notBalanced(tp1, tp2, op1, op2)) {
                            Log.v(TAG, "UNBALANCED showPlayersPopup rev: " + tp1.getName() + "/" + tp2.getName() + " v/s " + op1.getName() + "/" + op2.getName());
                            continue;
                        }

                        GameJournalDBEntry game = new GameJournalDBEntry(presentPlayers.get(idx).getName(),
                                presentPlayers.get(idx + 3).getName(),
                                presentPlayers.get(idx + 1).getName(),
                                presentPlayers.get(idx + 2).getName());
                        Log.v(TAG, "showPlayersPopup Adding Game of 4 rev:" + game.toPlayersString());
                        games.add(game);
                        possibleGames.append(game.toPlayersString());
                        presentPlayers.get(idx).incrGamesPlayed_innings();
                        presentPlayers.get(idx + 3).incrGamesPlayed_innings();
                        presentPlayers.get(idx + 1).incrGamesPlayed_innings();
                        presentPlayers.get(idx + 2).incrGamesPlayed_innings();
                    }
                }

                // After these, if there are few players with very few games. Let them play.
                pickLeastPlayedFour(presentPlayers, games, possibleGames);


                if(extraGames.length()>0) {
                    possibleGames.append("\nOther Possible Games:\n");
                    possibleGames.append(extraGames);
                }

                //Log.v(TAG, "showPlayersPopup OK onClick, games:" + possibleGames.toString());
                AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueActivity.this);
                builder.setTitle("Game Suggestions")
                        .setMessage(possibleGames)
                        .setNeutralButton("Ok", null).show();
            }
        });
        builder.show();
    }


    private void pickLeastPlayedFour(final ArrayList<PlayerData> presentPlayers, final ArrayList<GameJournalDBEntry> games,
                                     SpannableStringBuilder possibleGames) {
        ArrayList<PlayerData> pList = new ArrayList<>(presentPlayers);
        sortByGamesPlayed(pList);  //ascending order
        //Log.v(TAG, "pickLeastPlayedFour presentPlayers=" + pList.toString());
        if (pList.size() < 4) return;
        int gamesCap = pList.get(3).getGamesPlayed_innings_int();
        for (int x = pList.size() - 1; x > 3; x--) {
            if (pList.get(x).getGamesPlayed_innings_int() > gamesCap) pList.remove(x);
            else break;
        }
        //Log.v(TAG, "pickLeastPlayedFour pList=" + pList.toString());
        mCommon.sortPlayers(pList, Constants.SEASON_IDX, true, ClubLeagueActivity.this, false);
        Log.v(TAG, "pickLeastPlayedFour pList descending=" + pList.toString());
        PlayerData tp1 = pList.get(0);
        scatterPlayers(tp1, presentPlayers, games, pList, possibleGames);
    }


    //For the input player, find a partner from the other end of the list (partner is bottom player, if the i/p player is top player).
    //Then, find opponents for this team (again one from either ends)
    private void scatterPlayers(final PlayerData tp1, final ArrayList<PlayerData> presentPlayers, final ArrayList<GameJournalDBEntry> games,
                                final ArrayList<PlayerData> playerDataArrayList, SpannableStringBuilder possibleGames) {
        Log.v(TAG, "scatterPlayers scattering:" + tp1.getName() + " playerDataArrayList=" + playerDataArrayList.toString());
        int count = 1;
        ArrayList<PlayerData> teamPlayers2 = new ArrayList<>(playerDataArrayList);
        teamPlayers2.remove(tp1);
        boolean found = false;

        //Find a team-mate for tp1
        for (int idx2 = teamPlayers2.size() - 1; idx2 >= 0; idx2--) {  //partner from the other end
            PlayerData tp2 = teamPlayers2.get(idx2);
            if(tp2==tp1) continue;
            if(playedToday(tp1, tp2, games)) continue;

            ArrayList<PlayerData> oppPlayers = new ArrayList<>(teamPlayers2);
            oppPlayers.remove(tp2);
            for (int idxo = 0; idxo <= oppPlayers.size() - 1; idxo++) {  //opponent players: again from either ends
                PlayerData op1 = oppPlayers.get(idxo);
                ArrayList<PlayerData> oppPlayers2 = new ArrayList<>(oppPlayers);
                oppPlayers2.remove(op1);
                for (int idxo2 = oppPlayers2.size() - 1; idxo2 >= 0; idxo2--) {
                    PlayerData op2 = oppPlayers2.get(idxo2);
                    if (op2 == op1) continue;
                    count++;

                    if (playedToday(op1, op2, games)) {
                        Log.v(TAG, "playedToday showPlayersPopup: " + tp1.getName() + "/" + tp2.getName() + " v/s " + op1.getName() + "/" + op2.getName());
                        continue;
                    }
                    if (notBalanced(tp1, tp2, op1, op2)) {
                        Log.v(TAG, "UNBALANCED showPlayersPopup: " + tp1.getName() + "/" + tp2.getName() + " v/s " + op1.getName() + "/" + op2.getName());
                        continue;
                    }
                    if (presentPlayers.size() > 6 && playedTodayAs4(tp1, tp2, op1, op2, games)) {
                        Log.v(TAG, "PLAYEDasFOUR showPlayersPopup: " + tp1.getName() + "/" + tp2.getName() + " v/s " + op1.getName() + "/" + op2.getName());
                        continue;
                    }

                    GameJournalDBEntry game = new GameJournalDBEntry(tp1.getName(),tp2.getName(),op1.getName(),op2.getName());
                    games.add(game);
                    possibleGames.append(game.toPlayersString());

                    // increment the number of games for the players involved. GamesPlayed_innings is used to count the number of games suggested for each player
                    for (PlayerData p : presentPlayers) {
                        if (tp1.getName().equals(p.getName())) {
                            p.incrGamesPlayed_innings();
                            continue;
                        }
                        if (tp2.getName().equals(p.getName())) {
                            p.incrGamesPlayed_innings();
                            continue;
                        }
                        if (op1.getName().equals(p.getName())) {
                            p.incrGamesPlayed_innings();
                            continue;
                        }
                        if (op2.getName().equals(p.getName())) {
                            p.incrGamesPlayed_innings();
                            continue;
                        }
                    }
                    Log.v(TAG, "scatterPlayers Adding Game:" + game.toPlayersString());
                    found = true; // once the opponents are found for a team, break out of the loop. We don't want to repeat the same team.
                    break;
                }  //opposite team inner loop
                if(found) break;
            }  //opposite team outer loop
            if (found) break;  //do only one set of games per player.
        } //team-mate loop
    }

    private boolean playedToday(final PlayerData p1, final PlayerData p2, final ArrayList<GameJournalDBEntry> games) {
        for (GameJournalDBEntry game : games) {
            if (game.playedBefore(p1.getName(), p2.getName(), "", "")) return true;
        }
        return false;
    }

    private boolean playedTodayAs4(final PlayerData p1, final PlayerData p2, final PlayerData p3, final PlayerData p4,
                                   final ArrayList<GameJournalDBEntry> games) {
        for (GameJournalDBEntry game : games) {
            if (game.playersInvolved4(p1.getName(), p2.getName(), p3.getName(), p4.getName()))
                return true;
        }
        return false;
    }

    private boolean notBalanced(final PlayerData p1, final PlayerData p2, final PlayerData p3, final PlayerData p4) {
        int t1_total = p1.getPointsInt_season() + p2.getPointsInt_season();
        int t2_total = p3.getPointsInt_season() + p4.getPointsInt_season();
        if(t1_total+t2_total < 10) return false;

        return (t1_total <= (t2_total / 2)) || (t2_total <= (t1_total / 2));
    }

    // These were used to balance out the number of games suggested for each player. But that becomes very complicated and not accurate at all.
    // Keeping it here, if needed in future.
    private boolean equalChance(final PlayerData p1, final PlayerData p2, final PlayerData p3, final PlayerData p4, final ArrayList<PlayerData> presentPlayers, int deltaNumOfGames) {
        if (deltaNumOfGames <= 0) return true;
        int minNumOfGames = 9999;
        int maxNumOfGames = 0;
        PlayerData minP, maxP;
        for (PlayerData p : presentPlayers) {
            if (p.getGamesPlayed_innings_int() > maxNumOfGames) {
                maxNumOfGames = p.getGamesPlayed_innings_int();
            } else if (p.getGamesPlayed_innings_int() < minNumOfGames) {
                minNumOfGames = p.getGamesPlayed_innings_int();
            }
        }
        String msg = TAG + " " + minNumOfGames + "/" + maxNumOfGames + ":";
        if (p1.getGamesPlayed_innings_int() > (minNumOfGames + deltaNumOfGames)) {
            Log.v(TAG, msg + "players NOT equalChance:" + p1.toStringShort());
            return false;
        }
        if (p2.getGamesPlayed_innings_int() > (minNumOfGames + deltaNumOfGames)) {
            Log.v(TAG, msg + "players NOT equalChance:" + p2.toStringShort());
            return false;
        }
        if (p3.getGamesPlayed_innings_int() > (minNumOfGames + deltaNumOfGames)) {
            Log.v(TAG, msg + "players NOT equalChance:" + p3.toStringShort());
            return false;
        }
        if (p4.getGamesPlayed_innings_int() > (minNumOfGames + deltaNumOfGames)) {
            Log.v(TAG, msg + "players NOT equalChance:" + p4.toStringShort());
            return false;
        }
        return true;
    }

    private boolean enoughChances(final ArrayList<PlayerData> presentPlayers) {
        int minNumOfGames = 9999;
        int maxNumOfGames = 0;
        PlayerData minP, maxP;
        for (PlayerData p : presentPlayers) {
            if (p.getGamesPlayed_innings_int() > maxNumOfGames) {
                maxNumOfGames = p.getGamesPlayed_innings_int();
            } else if (p.getGamesPlayed_innings_int() < minNumOfGames) {
                minNumOfGames = p.getGamesPlayed_innings_int();
            }
        }
        String msg = TAG + " " + minNumOfGames + "/" + maxNumOfGames + ":";
        Log.v(TAG, msg + " enoughChances");
        return maxNumOfGames - minNumOfGames <= 1;
    }

    private void sortByGamesPlayed(ArrayList<PlayerData> playersList) {
        Collections.sort(playersList, new Comparator<PlayerData>() {
            @Override
            public int compare(PlayerData p1, PlayerData p2) {
                //return (Integer.valueOf(p1.getGamesPlayed_innings_int()).compareTo(p2.getGamesPlayed_innings_int()));
                return Integer.compare(p1.getGamesPlayed_innings_int(), p2.getGamesPlayed_innings_int());
            }
        });
    }

    private void fetchGames(final String group, final ArrayList<GameJournalDBEntry> gameList,
                            final Set<String> presentPlayerNames){
        Log.d(TAG, "======== fetchGames (" + group + ") ========");
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub)
                .child(Constants.JOURNAL).child(mInnings).child(mRoundName).child(group);
        Query myQuery = dbRef.orderByKey();
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    GameJournalDBEntry jEntry = child.getValue(GameJournalDBEntry.class);
                    if(null==jEntry) continue;
                    gameList.add(jEntry);

                    //Save the present Players for later. Useful to sort names when entering data
                    //and to tick boxes (for present players) while suggesting games
                    if(!jEntry.getmW1().isEmpty()) presentPlayerNames.add(jEntry.getmW1());
                    if(!jEntry.getmW2().isEmpty()) presentPlayerNames.add(jEntry.getmW2());
                    if(!jEntry.getmL1().isEmpty()) presentPlayerNames.add(jEntry.getmL1());
                    if(!jEntry.getmL2().isEmpty()) presentPlayerNames.add(jEntry.getmL2());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ClubLeagueActivity.this,
                        "DB error while fetching games: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mDemoAlert!=null) {
            mDemoAlert.dismiss();
            mDemoAlert=null;
        }
    }

    private void showDemoDialogIfNeeded() {
        if(!SharedData.getInstance().mDemoMode || mDemoAlert!=null) {
            return;
        }

        if(!SharedData.getInstance().validFlag(Constants.DATA_FLAG_DEMO_MODE2)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueActivity.this);
            builder.setTitle("Demo mode");
            builder.setMessage(
                    "You are exploring 'demo mode' of the Club League screen.\n\n" +
                            " ++ Click on Gold or Silver header to maximise the group screen.\n\n" +
                            " ++ Click on Innings or Season tab header to sort on that column.\n\n" +
                            " ++ Click on a player to see statistics.\n\n" +
                            " ++ On right top, you can see 'refresh' and settings (3 dots) icons. " +
                            "From settings you can navigate to the below screens:\n" +
                            "      > Enter Score: to enter scores for games.\n" +
                            "      > Round Summary: to see summary of already played games.\n" +
                            "      > Settings: Privileged users can perform club admin operations.\n" +
                            "      > Create Tournament: Create an internal tournament from League players.\n\n" +
                            " ++ On left top, you can see left arrow icon to go back to previous screen.\n\n"
            );
            builder.setPositiveButton("Remind me again", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            builder.setNegativeButton("Got it", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    SharedData.getInstance().addFlag(ClubLeagueActivity.this, Constants.DATA_FLAG_DEMO_MODE2);
                }
            });
            mDemoAlert = builder.create();
            mDemoAlert.show();
        }
    }

    //send SMS to iPhone users (who don't have access to Android app)
    private View.OnClickListener sendSMSOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            ArrayList<PlayerData> mGoldPlayers = mGoldDB.getPlayers();
            ArrayList<PlayerData> mSilverPlayers = mSilverDB.getPlayers();
            StringBuilder gold_msg = new StringBuilder(mCommon.getShortRoundName(mRoundName) + "ST Gold>>\n");
            int len = gold_msg.length();
            for (PlayerData p : mGoldPlayers) {
                if (len != gold_msg.length()) gold_msg.append("\n");
                gold_msg.append(p.getName()).append(": ")
                        .append(p.getPoints_innings())
                        .append(" (").append(p.getWinPercentage_innings()).append("%)");
            }
            StringBuilder silver_msg = new StringBuilder(mCommon.getShortRoundName(mRoundName) + "ST Silver>>\n");
            len = silver_msg.length();
            for (PlayerData p : mSilverPlayers) {
                if (len != silver_msg.length()) silver_msg.append("\n");
                silver_msg.append(p.getName()).append(": ").append(p.getPoints_innings()).append(" (").append(p.getWinPercentage_innings()).append("%)");
            }
            final String gold_message = gold_msg.toString();
            final String silver_message = silver_msg.toString();

                /* SGO: attempt to send whatsapp message. But, it is not yet supported.
                Uri uri = Uri.parse("https://chat.whatsapp.com/GA5MrdrfxPxBCMIeTq2yj6");
                Intent sendIntent = new Intent(Intent.ACTION_VIEW, uri);
                //sendIntent.setType("text/plain");
                //sendIntent.setComponent(new ComponentName("com.whatsapp","com.whatsapp.Conversation"));
                //sendIntent.putExtra("jid", PhoneNumberUtils.stripSeparators("6133153331")+"@s.whatsapp.net");//phone number without "+" prefix
                //sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                sendIntent.putExtra(Intent.EXTRA_TEXT, "hello");
                startActivity(Intent.createChooser(sendIntent, "title"));*/

            AlertDialog.Builder alert = new AlertDialog.Builder(ClubLeagueActivity.this);
            final EditText edittext = new EditText(ClubLeagueActivity.this);
            //alert.setMessage("Enter Phone numbers (use comma as separator)");
            alert.setTitle("Send SMS with summary of Innings points");
            //alert.setMessage("Send SMS with Innings points summary");
            SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
            final String phnums = prefs.getString(Constants.DATA_PHNUMS, "");
            edittext.setHint("613613xxx,613613xxy,613613xxz");
            if (!phnums.isEmpty()) edittext.setText(phnums);
            alert.setView(edittext);

            alert.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //What ever you want to do with the value
                    String phone_numbers = edittext.getText().toString();
                    //Log.d(TAG, "send msg : phone numbers:" + phone_numbers);

                    //As of November 1, 2018, Google Play will require updates to existing apps to target
                    // API level 26 (Android 8.0) or higher
                    //Only an app that has been selected as a user's default app for making calls or text messages
                    //will be able to access call logs and SMS, respectively.
                    /*
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        Log.d(TAG, "send msg : VERSION_CODES.M");
                        if (ContextCompat.checkSelfPermission(ClubLeagueActivity.this, android.Manifest.permission.SEND_SMS)
                                != PackageManager.PERMISSION_GRANTED) {
                            // Permission is not granted
                            // Ask for permision
                            ActivityCompat.requestPermissions(ClubLeagueActivity.this, new String[]{android.Manifest.permission.SEND_SMS}, 1);
                            Log.d(TAG, "send msg : VERSION_CODES.M -- asking permission");
                            return;  //first time dont continue. User will have to press "Send" once more.
                            //if you continue, app will crash as the requestPermission is asynchronous.
                        }
                    }

                    for (String phNum : phNumList) {
                        Log.d(TAG, "send msg : phone num:" + phNum);
                        if (!PhoneNumberUtils.isGlobalPhoneNumber(phNum)) continue;
                        Log.d(TAG, "sending to " + phNum);
                        SmsManager sms = SmsManager.getDefault();
                        ArrayList<String> parts = sms.divideMessage(gold_message);
                        sms.sendMultipartTextMessage(phNum, null, parts, null, null);
                        parts = sms.divideMessage(silver_message);
                        sms.sendMultipartTextMessage(phNum, null, parts, null, null);

                        tmpPhNums += phNum + ",";
                    }
                    */

                    List<String> phNumList = Arrays.asList(phone_numbers.split(","));
                    StringBuilder tmpPhNums = new StringBuilder();
                    StringBuilder persistPhNums = new StringBuilder();

                    String separator = ";";
                    if (android.os.Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
                        //In SAMSUNG devices You have to separate the phone numbers with ','
                        //while other devices are accept the ';'.
                        separator = ",";
                    }
                    for (String phNum : phNumList) {
                        if (!PhoneNumberUtils.isGlobalPhoneNumber(phNum)) continue;
                        if (persistPhNums.length() > 0) {
                            persistPhNums.append(",");
                            tmpPhNums.append(separator);
                        }
                        tmpPhNums.append(phNum);
                        persistPhNums.append(phNum);

                        //Somehow sending to multiple recipients using ";" or "," delimiters is not working consistently.
                        //so, send one by one for now.
                        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phNum));
                        intent.putExtra("sms_body", gold_message + "\n" + silver_message);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            Log.d(TAG, "sending msg to:" + phNum);
                            startActivity(intent);
                            //Toast.makeText(ClubLeagueActivity.this, "SMS sent to " + tmpPhNums, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "ACTION_SEND.resolveActivity NULL");
                            Toast.makeText(ClubLeagueActivity.this, "SMS not sent!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    Log.d(TAG, "send msg : tmpPhNums:" + tmpPhNums);

                    /*
                    TODO: Make SMS to multiple recipients working
                    //Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + tmpPhNums));
                    //intent.setData(Uri.parse("smsto:" + tmpPhNums));  // This ensures only SMS apps respond
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:3438835349;6133153331"));
                    //intent.setData(Uri.parse("smsto:6133153331;3456"));  // This ensures only SMS apps respond
                    intent.putExtra("sms_body", gold_message);
                    //intent.putExtra(Intent.EXTRA_STREAM, attachment);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        Log.d(TAG, "ACTION_SEND.resolveActivity not null");
                        startActivity(intent);
                        Toast.makeText(ClubLeagueActivity.this, "SMS sent to " + tmpPhNums, Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "ACTION_SEND.resolveActivity NULL");
                        Toast.makeText(ClubLeagueActivity.this, "SMS not sent!", Toast.LENGTH_SHORT).show();
                    }
                    */


                    if (!persistPhNums.toString().equals(phnums)) {
                        Log.d(TAG, "New set of phone nums: " + persistPhNums);
                        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(Constants.DATA_PHNUMS, persistPhNums.toString());
                        editor.apply();
                    }
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // what ever you want to do with No option.
                }
            });

            alert.show();
        }
    };

        /*
    Below override of dispatchTouchEvent is needed for swipeLeft/Right to work
    for a scroll view.
    By default the touch listener for the scroll view get disabled and therefore scroll action
    does not happen. In order to fix this you need to override the dispatchTouchEvent method of
    the Activity and return the inherited version of this method after you're done with your own listener.
     */

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //Log.d(TAG, "dispatchTouchEvent: ");
        mDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    private void setUpGesture() {
        mDetector = new GestureDetector(ClubLeagueActivity.this,
                new ClubLeagueActivity.STGestureListener());

        findViewById(R.id.outer_ll).setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                //Log.d(TAG, "onTouch: ");
                return mDetector.onTouchEvent(event);
            }
        });
    }

    class STGestureListener implements GestureDetector.OnGestureListener {

        //Keeping the threshold pretty high so that simple scroll also works on the
        //UB and LB tables.
        private static final long VELOCITY_THRESHOLD = 7000;
        private static final String TAG = "TournaGesture";

        @Override
        public boolean onDown(final MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(final MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,
                                final float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(final MotionEvent e) {
        }

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                               final float velocityX,
                               final float velocityY) {

            //Log.d(TAG, "onFling: velocityX=" + velocityX + " velocityY=" + velocityY);
            if (Math.abs(velocityX) < VELOCITY_THRESHOLD
                    && Math.abs(velocityY) < VELOCITY_THRESHOLD) {
                return false;//if the fling is not fast enough then it's just like drag
            }

            //if velocity in Y direction is higher than velocity in X direction,
            //then the fling is vertical
            if (Math.abs(velocityY) > Math.abs(velocityX)) {
                if (velocityY >= 0) {
                    //Log.i(TAG, "swipe down");
                    refresh();
                }
            }
            return true;
        }
    }

}
