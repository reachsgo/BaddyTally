package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;


public class TournaLanding extends AppCompatActivity implements CallbackRoutine {

    private static final String TAG = "TournaLanding";
    private static final String DELETE_TOURNA = "Delete tournament";

    private TournaUtil mTUtil;
    private SharedData mCommon;
    //private TournaEditTextDialog mCustomDialog;
    private ListView mTournaLV;
    private ArrayAdapter mTournaLA;
    private ArrayList<String> mTournaList;
    private Handler mMainHandler;
    private AlertDialog mDemoAlert;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.tourna_landing);
        //Log.d(TAG, "onCreate: ");
        mTUtil = new TournaUtil(TournaLanding.this, TournaLanding.this);
        mCommon = SharedData.getInstance();
        mMainHandler = new Handler();
        mDemoAlert = null;

        //mCustomDialog = new TournaEditTextDialog(TournaLanding.this, TournaLanding.this);
        mTournaList = new ArrayList<>();
        mTournaLA = new ArrayAdapter<>(
                this, R.layout.listitem_bigbold,
                mTournaList);
        mTournaLV = findViewById(R.id.tournaments_lv);
        mTournaLV.setAdapter(mTournaLA);


        mTournaLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                int delayMillis = 0;
                if(!mCommon.isDBConnected()) {
                    mCommon.wakeUpDBConnection();
                    delayMillis = 1000; //1s time to get connected to DB. This scenario will be hit
                                  //if the Tournament Landing screen is left open for a while (~1m)
                                  //or a real internet link failure.
                }
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!mCommon.isDBConnected()) {
                            Toast.makeText(TournaLanding.this,
                                    "Check your internet connection", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (mTournaList.size() > i) {
                            //Log.d(TAG, "mTournaLV onItemClick: " + mTournaList.get(i));
                            mCommon.mTournament = mTournaList.get(i);
                            selectActivityForTourna();
                        }
                    }
                }, delayMillis);
            }
        });

        mTournaLV.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (!mCommon.isSuperPlus()) return false;
                if (i >= mTournaList.size()) return false;
                mCommon.wakeUpDBConnectionProfile();


                //Log.d(TAG, "mTournaLV onItemClick: " + mTournaList.get(i));
                final String tourna = mTournaList.get(i);


                Context wrapper = new ContextThemeWrapper(TournaLanding.this, R.style.RedPopup);
                final PopupMenu popup = new PopupMenu(wrapper, view);
                popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
                if (Build.VERSION.SDK_INT >= 23) {
                    popup.setGravity(Gravity.END);
                }
                popup.getMenu().clear();
                Menu pMenu = popup.getMenu();
                pMenu.add(DELETE_TOURNA);

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        //Log.v(TAG, "onMenuItemClick:" + menuItem.getTitle().toString());
                        String choice = menuItem.getTitle().toString();
                        if (choice.equals(DELETE_TOURNA)) deleteTourna(tourna);
                        popup.dismiss();
                        return true;
                    }
                });
                popup.show();//showing popup menu
                return true;
            }
        });

        LinearLayout ll = findViewById(R.id.tlanding_ll);
        ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show a toast message.
                Snackbar.make(mTournaLV, "Select a tournament", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        refresh();
    }

    private void refresh() {

        mCommon.startProgressDialog(TournaLanding.this,
                "Fetching data", "Connecting...");

        //what to do in case of progress-bar timeout
        mCommon.showToastAndDieOnTimeout(mMainHandler, TournaLanding.this,
                "Check your internet connection", true, false,0);

        mTUtil.fetchActiveTournaments(); //CB_READTOURNA
    }

    private Boolean selectActivityForTourna() {
        Log.d(TAG, "selectActivityForTourna: [" + mCommon.mClub + "/" + mCommon.mTournament + "]");
        if (mCommon.mTournament.isEmpty()) return false;
        if (null == mCommon.mTournaMap || mCommon.mTournaMap.size() == 0) return false;
        for (Map.Entry<String, String> tourna : mCommon.mTournaMap.entrySet()) {
            if (tourna.getKey().equals(mCommon.mTournament)) {
                if (tourna.getValue().equals(Constants.DE) || tourna.getValue().equals(Constants.SE)) {
                    Intent myIntent = new Intent(TournaLanding.this, TournaTableLayout.class);
                    //dont keep this activity in stack to reduce heap usage (mainly due to background image)
                    //history=false set in manifest
                    TournaLanding.this.startActivity(myIntent);
                } else if (tourna.getValue().equals(Constants.LEAGUE)) {
                    Intent myIntent = new Intent(TournaLanding.this, TournaLeague.class);
                    //dont keep this activity in stack to reduce heap usage (mainly due to background image)
                    //history=false set in manifest
                    TournaLanding.this.startActivity(myIntent);
                }
                break;
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.d(TAG, "onResume: [" + mCommon.mClub + "/" + mCommon.mTournament + "]");
        if (mCommon.isDBUpdated()) {
            refresh();
            mCommon.setDBUpdated(false);
        }
        mCommon.auditClub(TournaLanding.this); //check if club still exists in DB
        showDemoDialogIfNeeded();
    }

    @Override
    protected void onDestroy() {
        mMainHandler.removeCallbacksAndMessages(null);
        //Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //Log.d(TAG, "onBackPressed: ");
        if (null!=mTournaList && mTournaList.size() > 0) {
            //If there were tournaments being shown, then kill the application.
            //mCommon.killApplication(TournaLanding.this);
            mCommon.killActivity(TournaLanding.this, RESULT_OK);
            Intent intent = new Intent(TournaLanding.this, MainSelection2.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else {
            //If not, this could be a new club, we have to go back to the start page.
            mCommon.killActivity(this, RESULT_OK);
            Intent intent = new Intent(TournaLanding.this, MainSelection2.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    //CallbackRoutine Callback interfaces
    public void profileFetched() {
    }

    public void callback(final String key, final Object inobj) {
    }

    public void alertResult(final String in, final Boolean ok, final Boolean ko) {
        if (in.contains(DELETE_TOURNA)) {
            if (ok) {
                String[] parts = in.split(Constants.COLON_DELIM);
                if (parts.length != 2) {
                    Log.w(TAG, "alertResult Internal error:" + parts.length);
                    mCommon.showToast(TournaLanding.this, "Internal error!", Toast.LENGTH_SHORT);
                    return;
                }
                updateDB_deleteTourna(parts[1]);
            }
        }
    }

    public void completed(final String in, final Boolean ok) {
        //Log.w(TAG, "completed: " + in + ":" + ok);
        if (in.equals(Constants.CB_READTOURNA)) {

            //DB-read successful. stop progress.
            mMainHandler.removeCallbacksAndMessages(null);
            mCommon.stopProgressDialog(TournaLanding.this);

            if (ok) {
                //Log.d(TAG, "completed: " + mCommon.mTournaMap.size());
                ArrayList<String> tournaList = new ArrayList<>();
                for (Map.Entry<String, String> entry : mCommon.mTournaMap.entrySet()) {
                    tournaList.add(entry.getKey());
                }
                Collections.sort(tournaList);
                mTournaLA.clear(); //clear existing list (mTournaList) in adapter
                if (tournaList.size() > 0) {
                    mTournaLA.addAll(tournaList);
                    //list items are added to mTournaList here, as mTournaList is initialized as
                    //the list in mTournaLA (see onCreate)
                }
                mTournaLA.notifyDataSetChanged();
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.basic_menu_main, menu);
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
                refresh();
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:
                //wake up connection and read profile again from DB to check for password changes
                mCommon.wakeUpDBConnectionProfile();
                Intent myIntent = new Intent(TournaLanding.this, TournaSettings.class);
                TournaLanding.this.startActivity(myIntent);
                break;
            case R.id.action_logout:
                mCommon.logOut(TournaLanding.this, true);
                break;
            case R.id.action_help:
                AlertDialog.Builder hBuilder = new AlertDialog.Builder(TournaLanding.this);
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
                SharedData.showAboutAlert(TournaLanding.this);
                break;
            default:
                break;
        }
        return true;
    }

    private void deleteTourna(final String tourna) {
        if (!mCommon.isPermitted(TournaLanding.this)) {
            return;
        }
        //Log.v(TAG, "deleteTourna:" + tourna);
        SharedData.getInstance().wakeUpDBConnection();
        String msg = "Existing tournament data will be lost permanently. Are you sure? ";
        SpannableStringBuilder sb = mCommon.getColorString(
                DELETE_TOURNA + Constants.COLON_DELIM + tourna, Color.RED);
        mCommon.showAlert(this, TournaLanding.this,
                sb, msg);
    }

    private void updateDB_deleteTourna(final String tourna) {
        if (!mCommon.isDBConnected()) {
            Toast.makeText(TournaLanding.this,
                    "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
            return;
        }
        //Log.i(TAG, "updateDB_deleteTourna:" + tourna);
        DatabaseReference teamsDBRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.TOURNA);
        teamsDBRef.child(tourna).setValue(null);
        teamsDBRef.child(Constants.ACTIVE).child(tourna).setValue(null);

        mCommon.addHistory(
                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub),
                String.format(Locale.getDefault(),"DEL %s/%s",
                        Constants.TOURNA, tourna));

        refresh();
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
        if(!SharedData.getInstance().mDemoMode) {
            return;
        }
        if(!SharedData.getInstance().validFlag(Constants.DATA_FLAG_DEMO_MODE3)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(TournaLanding.this);
            builder.setTitle("Demo mode");
            builder.setMessage(
                    "You are exploring 'demo mode' of tournaments.\n\n" +
                            " ++ Click on any tournament to navigate to the tournament fixture screen.\n\n" +
                            " ++ On right top, you can see 'refresh' and settings (3 dots) icons. " +
                            "From settings you can navigate to the below screens:\n" +
                            "      > Settings: Privileged users can perform tournament admin operations.\n\n" +
                            " ++ On left top, you can see left arrow icon to go back to previous screen.\n\n" +
                            " ++ Super-user can 'long click' on any tournament and delete the tournament.\n\n"
            );
            builder.setPositiveButton("Remind me again", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            builder.setNegativeButton("Got it", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    SharedData.getInstance().addFlag(TournaLanding.this, Constants.DATA_FLAG_DEMO_MODE3);
                }
            });
            mDemoAlert = builder.create();
            mDemoAlert.show();
        }
    }

}
