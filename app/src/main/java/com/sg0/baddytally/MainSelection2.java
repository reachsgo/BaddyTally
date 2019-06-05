package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

        mTournaBtn = findViewById(R.id.tournaments);
        mTournaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainSelection2.this, TournaLanding.class);
                //dont keep this activity in stack to reduce heap usage (mainly due to background image)
                //history=false set in manifest
                //myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //this wont work as TournaLanding is on top of this activity
                MainSelection2.this.startActivity(myIntent);
            }
        });

        mClubLeagueBtn = findViewById(R.id.clubleague);
        mClubLeagueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainSelection2.this, ClubLeagueActivity.class);
                //dont keep this activity in stack to reduce heap usage (mainly due to background image)
                //history=false set in manifest
                MainSelection2.this.startActivity(myIntent);
            }
        });

        Button trackscore = findViewById(R.id.trackscore);
        trackscore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainSelection2.this, TrackScores.class);
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
        if (SharedData.getInstance().isRoot()) {
            MenuItem mEnterDataItem = menu.findItem(R.id.action_settings);
            if(SharedData.getInstance().mOfflineMode)
                mEnterDataItem.setTitle("Disable Offline mode");
            else
                mEnterDataItem.setTitle("Enable Offline mode");
        } else {
            menu.findItem(R.id.action_settings).setVisible(false);
        }
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
                SharedData.getInstance().wakeUpDBConnection_profile();
                if(SharedData.getInstance().mOfflineMode) {
                    //in offline mode, disable it
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainSelection2.this);
                    alertBuilder.setTitle("Disable Offline mode");
                    alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(SharedData.getInstance().isDBServerConnected()) {
                                SharedData.getInstance().persistOfflineMode(false, MainSelection2.this);
                            } else {
                                Toast.makeText(MainSelection2.this,
                                        "There is no data connection now, try again when you are connected to wifi/cellular data",
                                        Toast.LENGTH_LONG).show();
                            }

                        }
                    });
                    alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //
                        }
                    });
                    alertBuilder.show();

                } else {
                    //in online mode, enable offline mode
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainSelection2.this);
                    alertBuilder.setTitle("Enable Offline mode");
                    alertBuilder.setMessage("By enabling offline mode, you can update scores even there is no data connection (wifi or cellular).\n\n" +
                            "Make sure that there is data connection at this moment to synchronise local data with server.\n");
                    alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(SharedData.getInstance().isDBServerConnected()) {
                                SharedData.getInstance().persistOfflineMode(true, MainSelection2.this);
                                SharedData.getInstance().restartApplication(MainSelection2.this, MainSelection2.class);
                            } else {
                                Toast.makeText(MainSelection2.this,
                                        "There is no data connection now, try again when you are connected to wifi/cellular data",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //don't enable offline mode
                        }
                    });
                    alertBuilder.show();

                }

                break;
            case R.id.action_logout:
                SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.commit();  //using commit instead of apply for immediate write
                SharedData.getInstance().clear();
                Toast.makeText(MainSelection2.this, "Cache cleared!", Toast.LENGTH_SHORT)
                        .show();
                Intent intent = new Intent(MainSelection2.this, MainSigninActivity.class);
                startActivity(intent);
                break;
            case R.id.action_help:
                AlertDialog.Builder hBuilder = new AlertDialog.Builder(MainSelection2.this);
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
        SharedData.getInstance().killApplication(MainSelection2.this);
    }
}