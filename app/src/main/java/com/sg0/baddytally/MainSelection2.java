package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
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
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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

public class MainSelection2 extends AppCompatActivity {

    private static final String TAG = "MainSelection2";
    private SharedData mCommon;
    private AlertDialog mDemoAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main_selection2);
        mCommon = SharedData.getInstance();
        mDemoAlert = null;
        //Log.d(TAG, "onCreate: ");

        Button mTournaBtn = findViewById(R.id.tournaments);
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

        Button mClubLeagueBtn = findViewById(R.id.clubleague);
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

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if(!mCommon.mDemoMode) {
            findViewById(R.id.scroll_tv).setVisibility(View.GONE);
        } else {
            //demo mode: set user id in DB and show any msg from admin
            syncWithDB(Constants.DEMO_CLUB);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCommon.auditClub(MainSelection2.this); //check if club still exists in DB
        setTitle(mCommon.mClub);
        //Log.d(TAG, "onResume: ");
        displayNews(false);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.basic_menu_main, menu);
        menu.findItem(R.id.action_refresh).setVisible(false);
        if (mCommon.isSuperPlus()) {
            MenuItem offlineMode = menu.findItem(R.id.action_settings);
            if(mCommon.mOfflineMode)
                offlineMode.setTitle("Disable Offline mode");
            else
                offlineMode.setTitle("Enable Offline mode");

            MenuItem miscOption = menu.findItem(R.id.action_misc);
            miscOption.setTitle("Broadcast News");
            miscOption.setVisible(true);
        } else {
            menu.findItem(R.id.action_settings).setVisible(false);
            MenuItem miscOption = menu.findItem(R.id.action_misc);
            miscOption.setTitle("Club News");
            miscOption.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(mCommon.mDemoMode) {
                    mCommon.logOut(MainSelection2.this, false);
                } else onBackPressed();
                return true;
            case R.id.action_refresh:
                //nothing to do
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:
                mCommon.wakeUpDBConnectionProfile();
                if(mCommon.mOfflineMode) {
                    //in offline mode, disable it
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainSelection2.this);
                    alertBuilder.setTitle("Disable Offline mode");
                    alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(mCommon.isDBServerConnected()) {
                                mCommon.persistOfflineMode(false, MainSelection2.this);
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
                            if(mCommon.isDBServerConnected()) {
                                mCommon.persistOfflineMode(true, MainSelection2.this);
                                mCommon.restartApplication(MainSelection2.this, MainSelection2.class);
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
                mCommon.logOut(MainSelection2.this, false);
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
                SharedData.showAboutAlert(MainSelection2.this);
                break;
            case R.id.action_misc:
                
                //Allow root user to broadcast info. This will be shown as popup window for every user.
                mCommon.wakeUpDBConnectionProfile(); //read news again; might not be in time for this view, but atleast for future.
                if (mCommon.isSuperPlus()) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(MainSelection2.this);
                    final EditText edittext = new EditText(MainSelection2.this);
                    edittext.setTextColor(Color.WHITE);
                    //final EditText edittext = dialogView.findViewById(R.id.et);
                    alert.setTitle(mCommon.getColorString("Message to broadcast:\n", Color.CYAN));

                    edittext.setHint("Type message here to be displayed to all users");

                    final String mNews = mCommon.mProfile.getNews();
                    if (!mNews.isEmpty()) edittext.setText(mNews);
                    edittext.setSelection(edittext.getText().length());
                    alert.setView(edittext);

                    alert.setPositiveButton("Broadcast", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String new_msg = edittext.getText().toString();
                            if (!new_msg.equals(mNews)) {
                                //write the new msg to DB
                                FirebaseDatabase.getInstance().getReference()
                                        .child(mCommon.mClub).child(Constants.PROFILE).child(Constants.NEWS)
                                        .setValue(new_msg);
                                Log.i(TAG, "News set: [" + new_msg + "]");
                                mCommon.wakeUpDBConnectionProfile(); //read news again
                            }
                        }
                    });

                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // what ever you want to do with No option.
                        }
                    });
                    AlertDialog dialog = alert.create();
                    dialog.show();
                    Window window = dialog.getWindow();
                    if(window != null) {
                        WindowManager.LayoutParams windowParams = window.getAttributes();
                        windowParams.dimAmount = 0.90f;
                        windowParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                        window.setAttributes(windowParams);
                        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    }

                } else {
                    displayNews(true);
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        mCommon.killApplication(MainSelection2.this);
    }

    private void displayNews(final boolean showAlways) {
        if (!showAlways) {
            //if the news was already read by this user, dont show it.
            if (mCommon.mReadNews.equals(mCommon.mProfile.getNews())) return;
        }
        if(mCommon.mProfile.getNews().isEmpty()) {
            Toast.makeText(MainSelection2.this, "No news!", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainSelection2.this);
        //alertBuilder.setTitle("Club News");
        alertBuilder.setMessage(mCommon.getColorString(mCommon.mProfile.getNews(), Color.WHITE));
        alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mCommon.mReadNews = mCommon.mProfile.getNews();
            }
        });

        //finally creating the alert dialog and displaying it
        AlertDialog alertDialog = alertBuilder.create();
        // Change the alert dialog background color
        //alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.YELLOW));
        alertDialog.show();
        Window window = alertDialog.getWindow();
        if(window != null) {
            WindowManager.LayoutParams windowParams = window.getAttributes();
            windowParams.dimAmount = 0.90f;
            windowParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(windowParams);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            //Log.d(TAG, "setTitle: " + title);
            String tempString = Constants.APPNAME + "  " + title;
            if(title.equals(mCommon.mClub)) {
                if (mCommon.isAdmin()) tempString += " +";
                else if (mCommon.isSuperPlus()) tempString += " *";
                else tempString += " ";
            }
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0,
                    Constants.APPNAME.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC),
                    Constants.APPNAME.length(), tempString.length(), 0);
            spanString.setSpan(new RelativeSizeSpan(0.7f),
                    Constants.APPNAME.length(), tempString.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(""); //workaround for title getting truncated.
            getSupportActionBar().setTitle(spanString);
            //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mDemoAlert!=null) {
            mDemoAlert.dismiss();
            mDemoAlert=null;
        }
    }
    
    

    private void showDemoDialogIfNeeded() {

        if(!mCommon.validFlag(Constants.DATA_FLAG_DEMO_MODE1)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainSelection2.this);
            builder.setTitle("Demo mode");
            builder.setMessage(
                    "You are exploring 'demo mode' of the app.\n\n" +
                            " ++ Demo club has ongoing club league and tournaments.\n\n" +
                            " ++ You can explore the club league and tournament pages to get a feel of the app.\n\n" +
                            " ++ You can browse points table, fixture, game scores, player statistics etc\n\n" +
                            " ++ You have only 'member' role access. So no updates can be made.\n\n" +
                            " ++ To exit the demo mode, go to settings and click 'Logout'.\n\n"
            );
            builder.setPositiveButton("Remind me again", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            builder.setNegativeButton("Got it", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mCommon.addFlag(MainSelection2.this, Constants.DATA_FLAG_DEMO_MODE1);
                }
            });
            mDemoAlert = builder.create();
            mDemoAlert.show();
        }
    }

    //See if there is a personal message to be shown to this user from teh admin
    void syncWithDB(final String role) {
        if(mCommon.mUser.isEmpty() || role.isEmpty()) { showDemoDialogIfNeeded(); return; }

        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub)
                .child(Constants.ACTIVE_USERS).child(mCommon.mUser);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean alert2bShown = true;
                Date c = Calendar.getInstance().getTime();
                SimpleDateFormat df = new SimpleDateFormat(Constants.ROUND_DATEFORMAT_SHORT, Locale.CANADA);
                final String todaysDate = df.format(c);
                if(dataSnapshot.exists()) {
                    Log.d(TAG, "onDataChange: " + dataSnapshot.toString());
                    String entry = dataSnapshot.getValue(String.class);
                    if(null!=entry && entry.contains(Constants.NEWS)) {
                        String[] entries = entry.split("=");
                        if(entries.length<2) { alert2bShown = false; }
                        else {
                            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainSelection2.this);
                            alertBuilder.setTitle(mCommon.getColorString("Message from admin", Color.RED));
                            String msg = entries[1].trim();
                            alertBuilder.setMessage(msg);
                            alertBuilder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dbRef.setValue(todaysDate); //alert shown, over-write now.
                                    showDemoDialogIfNeeded();
                                }
                            });
                            alertBuilder.show();
                        }
                    } else alert2bShown = false; //no msg to show
                }

                if(!alert2bShown) {
                    //does not exist, set in DB
                    Log.d(TAG, "syncWithDB: no alert from admin");
                    dbRef.setValue(todaysDate);
                    showDemoDialogIfNeeded();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "syncWithDB: onCancelled", databaseError.toException());
                Toast.makeText(MainSelection2.this,
                        "DB connection error:" + databaseError.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }

}