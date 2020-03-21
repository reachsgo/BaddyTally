package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class MainSigninActivity extends AppCompatActivity {

    private static final String TAG = "MainSignin";
    private SharedData mCommon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main_selection1);
        mCommon = SharedData.getInstance();
        mCommon.getUserID(MainSigninActivity.this);

        Button signin = findViewById(R.id.clubsignin);
        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainSigninActivity.this, LoginActivity.class);
                myIntent.putExtra(Constants.ACTIVITY, Constants.INITIAL);
                MainSigninActivity.this.startActivity(myIntent);
            }
        });

        Button trackscore = findViewById(R.id.trackscore);
        trackscore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainSigninActivity.this, TrackScores.class);
                MainSigninActivity.this.startActivity(myIntent);
            }
        });

        //forcefully setup the DB listener, even if this is a re-login.
        mCommon.setUpDBConnectionListener();

        Button floatingBtn = findViewById(R.id.ms_button);
        //Log.d(TAG, "onCreate: check for root:" + SharedData.getInstance().toString());
        if(mCommon.isRoot()) {

            //final Intent intent = getIntent();
            //final String data1 = intent.getStringExtra(Constants.INTENT_DATASTR1);
            //if(data1!=null && data1.equals(Constants.CHANNEL_NEWCLUB)) { }

            Toast.makeText(MainSigninActivity.this,
                    "........", Toast.LENGTH_SHORT).show();
            floatingBtn.setText("Root Options");
            floatingBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent myIntent = new Intent(MainSigninActivity.this, RootOptions.class);
                    MainSigninActivity.this.startActivity(myIntent);
                }
            });

            return; //for root.
        }

        floatingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainSigninActivity.this,
                        "Demo mode", Toast.LENGTH_LONG).show();
                mCommon.mClub = Constants.DEMO_CLUB;
                mCommon.mRole = Constants.MEMBER;
                mCommon.mDemoMode = true;
                startDemo();
            }
        });

    }

    void startDemo() {
        Intent myIntent = new Intent(MainSigninActivity.this, MainSelection2.class);
        MainSigninActivity.this.startActivity(myIntent);
    }


    private void killActivity() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.d(TAG, "onResume: ");
        if(mCommon.mCount == Constants.EXIT_APPLICATION) killActivity();
        //Intent myIntent = new Intent(MainSigninActivity.this, TrackScores.class);
        //MainSigninActivity.this.startActivity(myIntent);
        //dont keep this activity in stack to reduce heap usage (mainly due to background image)
        //history=false set in manifest

        //Refresh root credentials, isPermitted() will find if there are discrepancies
        mCommon.wakeupdbconnectionProfileRoot();

        if(mCommon.isDBServerConnected() && mCommon.mOfflineMode) {
            //in offline mode, remind the user
            //Do it only if there is network connectivity at this time. To make sure that the data
            //entered during offline-mode is sync-ed.
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainSigninActivity.this);
            alertBuilder.setTitle("You have enabled Offline mode");
            alertBuilder.setMessage(
                    "If you are NOT in the middle of a tournament, then disable offline mode to decrease your data usage.\n");
            alertBuilder.setPositiveButton("Disable", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mCommon.persistOfflineMode(false, MainSigninActivity.this);
                    //mCommon.setOfflineMode(false, false);
                    //Let the next app restart take care of disabling offline mode in Firebase
                    //At this time, firebase could have been already initialized
                    //and setOfflineMode() will fail on those cases. No hurry to disable offline mode!

                    moveOn();
                }
            });
            alertBuilder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //nothing to do. Enabling is done from ScoreTally.java
                    moveOn();
                }
            });
            alertBuilder.show();
        } else {
            moveOn();
        }

    }

    void moveOn() {
        if (!mCommon.mClub.isEmpty() &&
                !mCommon.isRoot()) {
            Intent myIntent = new Intent(MainSigninActivity.this, MainSelection2.class);
            //dont keep this activity in stack to reduce heap usage (mainly due to background image)
            //history=false set in manifest
            MainSigninActivity.this.startActivity(myIntent);
        } else {
            Animation shake = AnimationUtils.loadAnimation(MainSigninActivity.this, R.anim.shake_long);
            findViewById(R.id.ms_button).startAnimation(shake);
        }
    }

    @Override
    public void onBackPressed() {
        mCommon.killApplication(MainSigninActivity.this);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.basic_menu_main, menu);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_settings).setTitle("New Club");
        menu.findItem(R.id.action_logout).setTitle("Privacy Policy");
        menu.findItem(R.id.action_misc).setTitle("Contact us"); //contact us
        menu.findItem(R.id.action_misc).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainSigninActivity.this);
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                //nothing to do
                break;
            // New Club
            case R.id.action_settings:
                Intent myIntent = new Intent(MainSigninActivity.this, NewClub.class);
                MainSigninActivity.this.startActivity(myIntent);
                break;
            case R.id.action_logout:
                builder.setMessage(Html.fromHtml(
                        "<a href=\"https://sites.google.com/view/scoretally/privacy-policy\">privacy policy</a>"))
                        .setTitle(Constants.APPNAME)
                        .setNeutralButton("Ok", null);
                AlertDialog d = builder.create();
                d.show();
                // Make the textview clickable. Must be called after show()
                ((TextView)d.findViewById(android.R.id.message))
                        .setMovementMethod(LinkMovementMethod.getInstance());
                break;
            case R.id.action_help:
                builder.setMessage(Html.fromHtml(
                        "<a href=\"https://sites.google.com/view/scoretally/user-guide\">User Guide link</a>"))
                        .setTitle(Constants.APPNAME)
                        .setNeutralButton("Ok", null);
                AlertDialog help = builder.create();
                help.show();
                // Make the textview clickable. Must be called after show()
                ((TextView)help.findViewById(android.R.id.message))
                        .setMovementMethod(LinkMovementMethod.getInstance());
                break;
            case R.id.action_about:
                SharedData.showAboutAlert(MainSigninActivity.this);
                break;
            case R.id.action_misc:
                AlertDialog.Builder newclubDialog = new AlertDialog.Builder(MainSigninActivity.this);
                newclubDialog.setMessage(
                        "You are about to send an email to ScoreTally team.\n\n" +
                        "You will now be directed to your favourite email client.")
                        .setTitle(mCommon.getTitleStr("Contact us",
                                MainSigninActivity.this))
                        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sendEmail();
                            }
                        }).show();
                break;
            default:
                break;
        }
        return true;
    }

    private void sendEmail() {
        Intent email = new Intent(Intent.ACTION_SEND);
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"scoretallyteam@gmail.com"});
        email.putExtra(Intent.EXTRA_SUBJECT, "ScoreTally: support for " + mCommon.mUser);
        email.putExtra(Intent.EXTRA_TEXT,
                "Please fill in the below template and send the email. " +
                        "ScoreTally team will get back to you.\n" +
                "\nMy Contact Info:\n" +
                "        <name>\n        <phone>\n        <email>\n" +
                "\n\nClub Info:\n" +
                "        name : <short name>\n" +
                "\n\nQueries/Comments/Suggestions:\n" +
                "        <your comments>\n"
        );
        email.setType("message/rfc822");
        startActivity(Intent.createChooser(email, "Choose an Email client :"));
    }


}