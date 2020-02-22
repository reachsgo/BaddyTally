package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        Button schedBtn = findViewById(R.id.schedule_button);
        schedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedData.getInstance().wakeUpDBConnection_profile();
                //Intent myIntent = new Intent(TournaLeague.this, LoginActivity.class);
                Intent myIntent = new Intent(TournaLeague.this, TournaLeagueSchedule.class);
                myIntent.putExtra("tournament", mCommon.mTournament);
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

        summaryBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(!mCommon.isRoot()) return false;
                if(mCommon.mClub.isEmpty() || mCommon.mTournament.isEmpty()) {
                    return false;
                }
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                        .child(Constants.TOURNA).child(mCommon.mTournament)
                        .child(Constants.INTERNALS).child(Constants.HISTORY);
                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        StringBuilder history = new StringBuilder();
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            String storyLine = child.getValue(String.class);
                            if (null == storyLine) continue;
                            String parsed = mCommon.parseHistory(storyLine);
                            if (!parsed.isEmpty()) history.append(parsed).append("\n");
                        }
                        if(history.length()==0) return;
                        AlertDialog.Builder builder = new AlertDialog.Builder(TournaLeague.this);
                        builder.setMessage(mCommon.getSizeString(history.toString(), 0.6f))
                                .setTitle(mCommon.getTitleStr("History:", TournaLeague.this))
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                    }
                                }).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
                return true;
            }
        });

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setTitle(String tourna) {
        if (!TextUtils.isEmpty(tourna)) {
            //Log.d(TAG, "setTitle: " + tourna);
            String tempString = Constants.APPSHORT + "  " + tourna;
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, Constants.APPSHORT.length(),0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), Constants.APPSHORT.length(),
                    tempString.length(), 0);
            spanString.setSpan(new RelativeSizeSpan(0.7f), Constants.APPSHORT.length(),
                    tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(""); //workaround for title getting truncated.
            getSupportActionBar().setTitle(spanString);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.d(TAG, "onResume: ");
        //mCommon.wakeUpDBConnection_profile();
        if(!selectActivityForTourna())
            SharedData.getInstance().killActivity(this, RESULT_OK);
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
            case android.R.id.home:
                onBackPressed();
                return true;
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
                SharedData.showAboutAlert(TournaLeague.this);
                break;
            default:
                break;
        }

        return true;
    }

    boolean showTeamList(final String tournament) {
        //Log.v(TAG, "showTeamList for :" + tournament);
        setTitle(tournament);

        if(!mCommon.isDBConnected()) {
            Toast.makeText(TournaLeague.this,
                    "Check your internet connection", Toast.LENGTH_SHORT).show();
            return false;
        }

        RecyclerView tournaRecyclerView = findViewById(R.id.tourna_list);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        tournaRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        tournaRecyclerView.setLayoutManager(mLayoutManager);
        mTournaAdapter = null;
        mTournaAdapter = new TournaRecyclerViewAdapter(this, TournaLeague.this,
                tournament, findViewById(R.id.outer_ll));
        tournaRecyclerView.setAdapter(mTournaAdapter);
        return true;
    }

    //CallbackRoutine Callback interfaces
    public void profileFetched() { }
    public void callback(final String key, final Object inobj) { }
    public void alertResult(final String in, final Boolean ok, final Boolean ko) { }
    public void completed (final String in, final Boolean ok) { }

    private boolean selectActivityForTourna() {
        //Log.d(TAG, "selectActivityForTourna: ");
        if(mCommon.mTournament.isEmpty()) return false;
        return showTeamList(mCommon.mTournament);
    }
}
