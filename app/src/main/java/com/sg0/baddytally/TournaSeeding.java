package com.sg0.baddytally;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;


public class TournaSeeding extends AppCompatActivity {
    private static final String TAG = "TournaSeeding";
    private List<String> mTeams;
    private List<String> mSeededTeams;
    private ListView mTeamLV;
    private ListView mSeedLV;
    private ArrayAdapter mTeamLA;
    private ArrayAdapter mSeedLA;


    private void killActivity(){
        setResult(RESULT_OK);
        finish();
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tourna_team_seeding);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final String[] mStrings = {
                "Abbaye de Belloc", "Abbaye du Mont des Cats", "Abertam",
                "Abondance", "Ackawi",
                "Acorn", "Adelost", "Affidelice au Chablis", "Afuega'l",
                "Pitu", "Airag", "Airedale",
                "Aisy Cendre"};
        final String[] mStrings2 = {"dsds", "qwqwqq", " dsdsdsd sdsdsds",
                "dsdsds sdsds"};

        Intent thisIntent = getIntent(); // gets the previously created intent
        mTeams = thisIntent.getStringArrayListExtra(Constants.TEAMS);
        String extras = thisIntent.getStringExtra(Constants.EXTRAS);
        mSeededTeams = new ArrayList<>();

        mTeamLV = this.findViewById(R.id.team_list);
        mSeedLV = this.findViewById(R.id.seed_list);
        mTeamLA = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1,mTeams);
        mSeedLA = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1,mSeededTeams);
        mTeamLV.setAdapter(mTeamLA);
        mSeedLV.setAdapter(mSeedLA);

        mTeamLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String team = mTeams.get(i);
                Log.d(TAG, "teamL onItemClick: " + team);
                mTeams.remove(i);
                mTeamLA.notifyDataSetChanged();
                mSeededTeams.add(team);
                mSeedLA.notifyDataSetChanged();
            }
        });

        mSeedLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String team = mSeededTeams.get(i);
                Log.d(TAG, "teamS onItemClick: " + team);
                mTeams.add(team);
                mTeamLA.notifyDataSetChanged();
                mSeededTeams.remove(i);
                mSeedLA.notifyDataSetChanged();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();


    }

}


