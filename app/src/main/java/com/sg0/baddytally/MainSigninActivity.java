package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


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

        //Maintain DB connection state
        //SharedData.getInstance().setUpDBConnectionListener(); //its done from wakeUpDBConnection
        //SharedData.getInstance().wakeUpDBConnection_profile();
    }

    private void killActivity() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        if(SharedData.getInstance().mCount == Constants.EXIT_APPLICATION) killActivity();
        if (!SharedData.getInstance().mClub.isEmpty()) {
            Intent myIntent = new Intent(MainSigninActivity.this, MainSelection2.class);
            //dont keep this activity in stack to reduce heap usage (mainly due to background image)
            //history=false set in manifest
            MainSigninActivity.this.startActivity(myIntent);
        }
    }


    @Override
    public void onBackPressed() {
        SharedData.getInstance().killApplication(MainSigninActivity.this);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.basic_menu_main, menu);
        menu.findItem(R.id.action_refresh).setVisible(false);
        MenuItem settings = menu.findItem(R.id.action_settings);
        settings.setTitle("New Club");
        menu.findItem(R.id.action_logout).setTitle("Privacy Policy");
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
            // action with ID action_settings was selected
            case R.id.action_settings:
                AlertDialog.Builder newclubDialog = new AlertDialog.Builder(MainSigninActivity.this);
                newclubDialog.setMessage(
                        "You are about to send a request to ScoreTally team to create a new club login for you.\n" +
                        "ScoreTally team will get back to you after setting up your account.\n\n" +
                        "You will now be directed to your favourite email client. Please fill in the template details before sending the email.")
                        .setTitle(SharedData.getInstance().getTitleStr("Create new club:",
                                MainSigninActivity.this))
                        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sendEmail();
                            }
                        }).show();
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
                String msg = BuildConfig.VERSION_NAME;
                builder.setMessage(msg)
                        .setTitle(Constants.APPNAME)
                        .setNeutralButton("Ok", null).show();
                break;
            default:
                break;
        }
        return true;
    }

    private void sendEmail() {
        Intent email = new Intent(Intent.ACTION_SEND);
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"scoretallyteam@gmail.com"});
        email.putExtra(Intent.EXTRA_SUBJECT, "ScoreTally: Request to create new club");
        email.putExtra(Intent.EXTRA_TEXT, "Please fill in the below template and send the email. " +
                        "ScoreTally team will get back to you after setting up your account.\n" +
                "\nMy Contact Info:\n" +
                "        <name>\n        <phone>\n        <email>\n" +
                "\n\nNew Club Info:\n" +
                "        short name : <short name>\n" +
                "        description : <long name>\n" +
                "        max players : <N>\n" +
                "\n\nNotes: <any queries/comments/suggestions>\n" +
                "\n\nCheers,\n" +
                "<yours truly>\n\n"
        );
        email.setType("message/rfc822");
        startActivity(Intent.createChooser(email, "Choose an Email client :"));
    }


}