package com.sg0.baddytally;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;


public class MainSigninActivity extends AppCompatActivity {

    private static final String TAG = "MainSignin";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main_selection1);
        Log.d(TAG, "onCreate: ");
        //Toolbar myToolbar = findViewById(R.id.my_toolbar);
        //setSupportActionBar(myToolbar);

        Button signin = findViewById(R.id.clubsignin);
        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainSigninActivity.this, LoginActivity.class);
                myIntent.putExtra(Constants.ACTIVITY, Constants.INITIAL);
                MainSigninActivity.this.startActivity(myIntent);
            }
        });

        Button settings = findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //SharedData.getInstance().wakeUpDBConnection();
                Intent settingsIntent = new Intent(MainSigninActivity.this, Settings.class);
                MainSigninActivity.this.startActivityForResult(settingsIntent, Constants.SETTINGS_ACTIVITY);
                //Intent myIntent = new Intent(MainSigninActivity.this, LoginActivity.class);
                //myIntent.putExtra(Constants.ACTIVITY, Constants.ACTIVITY_SETTINGS);
                //MainSigninActivity.this.startActivity(myIntent);

            }
        });

        //Maintain DB connection state
        SharedData.getInstance().setUpDBConnectionListener();
        SharedData.getInstance().wakeUpDBConnection_profile();

        //login once when the app is started
        //Intent myIntent = new Intent(MainSigninActivity.this, LoginActivity.class);
        //myIntent.putExtra(Constants.ACTIVITY, Constants.INITIAL);
        //MainSigninActivity.this.startActivity(myIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        //SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        //String club = prefs.getString(Constants.DATA_CLUB, "");
        if (!SharedData.getInstance().mClub.isEmpty()) {
            Intent myIntent = new Intent(MainSigninActivity.this, MainSelection2.class);
            MainSigninActivity.this.startActivity(myIntent);
        }
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        finish();
    }

}