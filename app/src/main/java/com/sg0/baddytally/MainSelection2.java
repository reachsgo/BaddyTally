package com.sg0.baddytally;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;


public class MainSelection2 extends AppCompatActivity {

    private static final String TAG = "MainSelection2";
    private Button mTournaBtn;
    private Button mClubLeagueBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main_selection2);
        Log.d(TAG, "onCreate: ");
        SharedData.getInstance().initData(getApplicationContext());
        mTournaBtn = findViewById(R.id.tournaments);
        mTournaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainSelection2.this, TournaLanding.class);
                MainSelection2.this.startActivity(myIntent);
            }
        });

        mClubLeagueBtn = findViewById(R.id.clubleague);
        mClubLeagueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainSelection2.this, MainActivity.class);
                MainSelection2.this.startActivity(myIntent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        //if (SharedData.getInstance().mTournaMode) {
        //    mTournaBtn.performClick();
        //} else {
            //mClubLeagueBtn.performClick();
        //}
    }

}