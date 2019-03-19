package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

public class TournaLeague extends AppCompatActivity implements CallbackRoutine {
    private static final String TAG = "TournaLeague";
    private static final String CREATE_NEW_TEAM = "Create new team";
    private SharedData mCommon;
    private String mTeamShort;
    private String mTeamLong;
    private TournaRecyclerViewAdapter mTournaAdapter;
    private TournaUtil mTUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tourna_league);
        findViewById(R.id.header_ll).setVisibility(View.GONE);
        findViewById(R.id.enter_button).setVisibility(View.GONE);
        mCommon = SharedData.getInstance();
        mTUtil = new TournaUtil(TournaLeague.this, TournaLeague.this);


        Button enterScoreBtn = findViewById(R.id.enter_button);
        enterScoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedData.getInstance().wakeUpDBConnection_profile();
                //Intent myIntent = new Intent(TournaLeague.this, LoginActivity.class);
                Intent myIntent = new Intent(TournaLeague.this, TournaLeagueEnterData.class);
                myIntent.putExtra(Constants.TOURNATYPE, Constants.LEAGUE);
                TournaLeague.this.startActivity(myIntent);
            } //onClick
        });

        Button summaryBtn = findViewById(R.id.summary_button);
        summaryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mCommon.mTournament.isEmpty()) return;
                SharedData.getInstance().wakeUpDBConnection_profile();
                Intent myIntent = new Intent(TournaLeague.this, TournaSummary.class);
                myIntent.putExtra("tournament", mCommon.mTournament);
                TournaLeague.this.startActivityForResult(myIntent, Constants.SUMMARY_ACTIVITY);
            } //onClick
        });


    }

    private void setTitle(String tourna) {
        if (!TextUtils.isEmpty(tourna)) {
            Log.d(TAG, "setTitle: " + tourna);
            String tempString = Constants.APPNAME + "  " + tourna;
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, Constants.APPNAME.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), Constants.APPNAME.length(), tempString.length(), 0);
            spanString.setSpan(new RelativeSizeSpan(0.7f), Constants.APPNAME.length(), tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(""); //workaround for title getting truncated.
            getSupportActionBar().setTitle(spanString);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        mCommon.wakeUpDBConnection_profile();
        //If this is return from other activities, show data of the already chosen tournament
        //mTUtil.fetchActiveTournaments();
        selectActivityForTourna();
    }

    protected void killActivity() {
        Log.d(TAG, "killActivity: ");
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        SharedData.getInstance().killActivity(this, RESULT_OK);
        Intent myIntent = new Intent(TournaLeague.this, TournaLanding.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TournaLeague.this.startActivity(myIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.basic_menu_main, menu);
        menu.findItem(R.id.action_logout).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                finish();
                startActivity(getIntent());
                break;
            case R.id.action_help:
                AlertDialog.Builder hBuilder = new AlertDialog.Builder(TournaLeague.this);
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
                AlertDialog.Builder builder = new AlertDialog.Builder(TournaLeague.this);
                builder.setMessage("Version: " + BuildConfig.VERSION_NAME)
                        .setTitle(SharedData.getInstance().getTitleStr(Constants.APPNAME, TournaLeague.this))
                        .setNeutralButton("Ok", null).show();
                break;
            default:
                break;
        }

        return true;
    }

    void showTeamList(final String tournament) {
        Log.v(TAG, "showTeamList for :" + tournament);
        setTitle(tournament);
        /*
        ArrayList<PlayerData> players = new ArrayList<>();
        PlayerData header = new PlayerData("");
        players.add(header); */

        RecyclerView tournaRecyclerView = findViewById(R.id.tourna_list);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        tournaRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        tournaRecyclerView.setLayoutManager(mLayoutManager);
        mTournaAdapter = null;
        mTournaAdapter = new TournaRecyclerViewAdapter(this, TournaLeague.this, tournament, findViewById(R.id.outer_ll));
        tournaRecyclerView.setAdapter(mTournaAdapter);
    }

    //CallbackRoutine Callback interfaces
    public void profileFetched() { }
    public void callback(final String key, final Object inobj) { }
    public void alertResult(final String in, final Boolean ok, final Boolean ko) {
        if(!in.equals(CREATE_NEW_TEAM)) return;
        if(ok) {
            if(mTeamShort.isEmpty() || mTeamLong.isEmpty()) return;
            updateDB_newteam(mTeamShort, mTeamLong);
            mTournaAdapter.readDBTeam();
        }
    }
    public void completed (final String in, final Boolean ok) {
        Log.w(TAG, "completed: " + in + ":" + ok);
        if(in.equals(Constants.CB_READTOURNA)) {
            if(ok) mTUtil.showTournaments(findViewById(R.id.title_tv), findViewById(R.id.outer_ll));
        } else if(in.equals(Constants.CB_SHOWTOURNA)) {
            if(ok) {
                mCommon.mTournament = mTUtil.mTourna;
                selectActivityForTourna();
            }
        }

    }

    private Boolean selectActivityForTourna() {
        Log.d(TAG, "selectActivityForTourna: ");
        if(mCommon.mTournament.isEmpty()) return false;
        if(null==mCommon.mTournaMap || mCommon.mTournaMap.size()==0) return false;
        for(Map.Entry<String,String> tourna : mCommon.mTournaMap.entrySet()) {
            if(tourna.getKey().equals(mCommon.mTournament)) {
                if(tourna.getValue().equals(Constants.DE) || tourna.getValue().equals(Constants.SE)) {
                    Intent myIntent = new Intent(TournaLeague.this, TournaTableLayout.class);
                    TournaLeague.this.startActivity(myIntent);
                } else if(tourna.getValue().equals(Constants.LEAGUE)) {
                    showTeamList(mCommon.mTournament);
                }
                break;
            }
        }
        return true;
    }
    private void updateDB_newteam(final String short_name, final String long_name) {
        if(mCommon.mTeams.contains(short_name)) {
            Toast.makeText(TournaLeague.this, "Team " + short_name + " already exists!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!mCommon.isDBConnected()) {
            Toast.makeText(TournaLeague.this, "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "updateDB_newteam:[" + short_name + ":" + long_name + "]");
        DatabaseReference teamDBRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.TEAMS).child(short_name);
        teamDBRef.child(Constants.SCORE).setValue(new TeamScoreDBEntry());
        teamDBRef.child(Constants.DESCRIPTION).setValue(long_name);
        FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(mCommon.mTournament).child(Constants.TEAMS_SUMMARY).child(short_name).setValue(true);
    }
}
