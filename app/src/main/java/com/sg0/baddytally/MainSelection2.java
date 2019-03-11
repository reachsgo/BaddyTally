package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


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
        SharedData.getInstance().initData(MainSelection2.this);
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

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.basic_menu_main, menu);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                //nothing to do
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:
                break;
            case R.id.action_logout:
                SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.commit();
                SharedData.getInstance().clear();
                Toast.makeText(MainSelection2.this, "Cache cleared!", Toast.LENGTH_SHORT)
                        .show();
                Intent intent = new Intent(MainSelection2.this, MainSigninActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            case R.id.action_about:
                //int versionCode = BuildConfig.VERSION_CODE;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainSelection2.this);
                builder.setMessage("Version: " + BuildConfig.VERSION_NAME)
                        .setTitle(SharedData.getInstance().getTitleStr(Constants.APPNAME, MainSelection2.this))
                        .setNeutralButton("Ok", null).show();
                break;
            default:
                break;
        }
        return true;
    }

    private void killActivity() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        SharedData.getInstance().mCount = Constants.EXIT_APPLICATION;
        killActivity();
    }


}