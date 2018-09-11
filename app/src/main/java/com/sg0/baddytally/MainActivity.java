package com.sg0.baddytally;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.view.Menu;
import android.view.MenuInflater;
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
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FireBaseDBReader mGoldDB;
    private FireBaseDBReader mSilverDB;
    private MenuItem mEnterDataItem;
    private String mClub;
    private String mInnings;
    private String mRoundName;
    private boolean mInitialAttempt;

    private void setTitle(String club) {
        if (!TextUtils.isEmpty(club)) {
            Log.d(TAG, "setTitle: " + club + ":" + SharedData.getInstance().toString());
            String tempString = Constants.APPNAME + "  " + club;
            if (SharedData.getInstance().isAdmin()) tempString += " +";
            else if (SharedData.getInstance().isRoot()) tempString += " *";
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, Constants.APPNAME.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), Constants.APPNAME.length(), tempString.length()-1, 0);
            spanString.setSpan(new SuperscriptSpan(), tempString.length()-1, tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanString.setSpan(new RelativeSizeSpan(0.5f), tempString.length()-1, tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(""); //workaround for title getting truncated.
            getSupportActionBar().setTitle(spanString);
        }
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
        mGoldLayoutManager.setReverseLayout(true);
        mGoldLayoutManager.setStackFromEnd(true);
        mRecyclerGoldView.setLayoutManager(mGoldLayoutManager);
        //mRecyclerGoldView.addItemDecoration(new DividerItemDecoration(MainActivity.this,
        //        DividerItemDecoration.VERTICAL));
        RecyclerViewAdapter mGoldAdapter = new RecyclerViewAdapter(this, Constants.GOLD, players);
        mGoldAdapter.setColor("#eedd82");  //color gold
        mRecyclerGoldView.setAdapter(mGoldAdapter);
        mGoldDB = new FireBaseDBReader(this, mClub, Constants.GOLD, mInnings, mGoldAdapter, mRecyclerGoldView);
        mGoldDB.fetchThisRoundScore();

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
            RecyclerViewAdapter mSilverAdapter = new RecyclerViewAdapter(this, Constants.SILVER, players);
            mSilverAdapter.setColor("#eeeee0");  //color silver
            mRecyclerSilverView.setAdapter(mSilverAdapter);
            mSilverDB = new FireBaseDBReader(this, mClub, Constants.SILVER, mInnings, mSilverAdapter, mRecyclerSilverView);
            mSilverDB.fetchThisRoundScore();
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
        mEnterDataItem = null;
        mClub = "";
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
        } else {
            mClub = club;
            SharedData.getInstance().mClub = mClub;
            SharedData.getInstance().mUser = prefs.getString(Constants.DATA_USER, "");
            SharedData.getInstance().mRole = prefs.getString(Constants.DATA_ROLE, "");
            Log.d(TAG, "onResume: " + SharedData.getInstance().toString());
            setTitle(mClub);
            fetchInnings();
            if (mEnterDataItem != null) {
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
        setTitle(mClub);
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
                    MainActivity.this.startActivity(myIntent);
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
                    MainActivity.this.startActivity(myIntent);
                }
                break;
            case R.id.action_rules:
                showRules();
                break;
            case R.id.action_about:
                //int versionCode = BuildConfig.VERSION_CODE;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Version: " + BuildConfig.VERSION_NAME)
                        .setTitle(Constants.APPNAME)
                        .setNeutralButton("Ok", null)
                        .show();
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
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<List<InningsDBEntry>> t = new GenericTypeIndicator<List<InningsDBEntry>>() {
                };
                List<InningsDBEntry> innings = dataSnapshot.getValue(t);
                if(null==innings) return;
                Log.v(TAG, "fetchInnings: key:" + dataSnapshot.getKey());
                int count = 0;
                for (InningsDBEntry val : innings) {
                    if (val.current) {
                        mInnings = val.name;
                        SharedData.getInstance().mInnings = mInnings;
                        mRoundName = val.round;
                        SharedData.getInstance().mRoundName = mRoundName;
                        SharedData.getInstance().mInningsDBKey = Integer.toString(count);
                    }
                    count++;
                }
                initAdapter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "fetchInnings: onCancelled", databaseError.toException());
                Toast.makeText(MainActivity.this, "Innings DB error: " + databaseError.toString(), Toast.LENGTH_SHORT)
                        .show();
            }
        });

        dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.PROFILE);
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
                "4.  At the start of a new Innings, the three lowest placed players of Gold group are relegated into" +
                " Silver group and top three players of the Silver group are promoted to Gold group." +
                " For this shuffle, only the points accumulated over the last Innings are considered.\n\n" +
                "5.  First game on each court on game days are warm-up games. No groups or points for these games.\n\n" +
                "6.  After warm-up, games are played among players of the same group, such that all" +
                " possible combination of pairings are covered (at least once) for that day. \n\n" +
                "7.  Any new member to club starts in the Silver group.\n\n" +
                "8.  If guests are playing, points are not counted for that game.\n\n" +
                "9.  If there are less than 4 players from a group on any game day, you should play at least 1 singles per player in attendance." +
                " Ideally, singles between all possible combinations of that group on that day should be played.\n\n" +
                "10. If there are not enough players on a day, mix and match (after the singles). Points are still counted.\n\n" +
                "11. Scores and results can be entered into ScoreTally after each game.\n\n")
                .setTitle("Rules")
                .setNeutralButton("Ok", null)
                .show();
    }
}
