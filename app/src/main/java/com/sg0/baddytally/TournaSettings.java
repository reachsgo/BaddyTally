package com.sg0.baddytally;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingFormatArgumentException;

public class TournaSettings extends AppCompatActivity implements CallbackRoutine{
    private static final String TAG = "TournaSettings";
    private static final String CREATE_NEW_TEAM = "Create new team";
    private DatabaseReference mDatabase;
    private SharedData mCommon;
    private TournaUtil mTUtil;
    private String mTourna;
    private String mTeamShort;
    private String mTeamLong;
    private TournaEditTextDialog mCustomDialog;

    public void killActivity() {
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        killActivity();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        Intent myIntent = new Intent(TournaSettings.this, TournaLanding.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TournaSettings.this.startActivity(myIntent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tourna_settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mCommon = SharedData.getInstance();
        mTUtil = new TournaUtil(TournaSettings.this, TournaSettings.this);
        mCustomDialog = new TournaEditTextDialog(TournaSettings.this, TournaSettings.this);
        Log.w(TAG, "onCreate :" + mCommon.toString());


        Button createNewTournaBtn = findViewById(R.id.createNewTourna_btn);
        createNewTournaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mCommon.isPermitted(TournaSettings.this)) return;
                if (!mCommon.isRoot()) {
                    Toast.makeText(TournaSettings.this, "You don't have permissions to perform this.", Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                TournaDialogClass customDialog = new TournaDialogClass(TournaSettings.this);
                customDialog.show();
            }
        });

        Button addNewteamBtn = findViewById(R.id.addNewTeam_btn);
        addNewteamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mCommon.isPermitted(TournaSettings.this)) return;
                if (!mCommon.isRoot()) {
                    Toast.makeText(TournaSettings.this, "You don't have permissions to perform this.", Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                if(mTUtil!=null) mTUtil.fetchActiveTournaments();
                //callback is completed()
            }
        });


        Button addNewMatchBtn = findViewById(R.id.addNewMatch_btn);
        addNewMatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mCommon.isPermitted(TournaSettings.this)) return;
                if (!mCommon.isRoot()) {
                    Toast.makeText(TournaSettings.this, "You don't have permissions to perform this.", Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                onClickNewMatch();
            }
        });


        Switch clearcache_sw = findViewById(R.id.clearcache_sw);
        clearcache_sw.setChecked(false);
        clearcache_sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean bChecked) {
                if (bChecked) {
                    SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.commit();
                    mCommon.clear();
                    mCommon.setDBUpdated(true); //notify Main to refresh view
                    Toast.makeText(TournaSettings.this, "Cache cleared!", Toast.LENGTH_SHORT)
                            .show();

                    //Restart the app: Needed to re-invoke Application.onCreate() to disable DB persistence,
                    //though that behavior is very inconsistent. See comments in ScoreTally.java.
                    //setResult(Constants.RESTARTAPP);
                    //killActivity();
                    Intent intent = new Intent(TournaSettings.this, MainSigninActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab_return);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void onClickNewteam(final String key, final Object inobj) {

        //Log.v(TAG, "onClickNewteam... " + key);
        if(inobj==null) {
            Log.d(TAG, "callback: null");
            return;
        }
        ArrayList<String> strList = (ArrayList<String>)inobj;
        if(strList.size()!=3) {
            Log.e(TAG, "callback: unexpected input! " + strList.size());
            Toast.makeText(TournaSettings.this, "Internal error in callback", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        final String tourna = strList.get(0);
        mTeamShort = strList.get(1).toUpperCase();
        mTeamLong = strList.get(2);
        Log.d(TAG, "callback: got back:" + tourna + mTeamShort + mTeamLong);
        Boolean errVal = false;
        Log.i(TAG, "team name: [" + mTeamShort + "] [" + mTeamLong + "]");
        if(!mTeamShort.matches("[A-Z0-9]+")) errVal = true;
        if(!mTeamLong.matches("[A-Za-z0-9 ]+")) errVal = true;
        if(errVal) {
            Toast.makeText(TournaSettings.this, "Bad Input! Enter only alphanumeric values", Toast.LENGTH_SHORT)
                    .show();
            Log.i(TAG, "Bad Input! Enter only alphanumeric values: [" + mTeamShort + "] [" + mTeamLong + "]");
        } else {
            checkIfTeamAlreadyExists(tourna, mTeamShort);
        }
    }

    public void checkIfTeamAlreadyExists(final String tourna, final String teamShort) {
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(tourna).child(Constants.TEAMS_SUMMARY);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                DataSnapshot ds = dataSnapshot.child(teamShort);
                Boolean teamStatus = ds.getValue(Boolean.class);
                if(teamStatus!=null && teamStatus) {
                    Log.d(TAG, "checkIfTeamAlreadyExists: " + teamShort + " exists in " + tourna);
                    Toast.makeText(TournaSettings.this, "Team name '" + mTeamShort + "' is taken. Use something else.", Toast.LENGTH_SHORT)
                            .show();
                    Log.i(TAG,  "Team name '" + mTeamShort + "' is taken. Use another short name. [" + mTeamLong + "]");
                } else {
                    Log.d(TAG, "checkIfTeamAlreadyExists: " + teamShort + " will be created in " + tourna);
                    String msg = " You are about to create a new team with:" +
                            "\n   short name = " + mTeamShort +
                            "\n   full name = " + mTeamLong;
                    mCommon.showAlert(TournaSettings.this, TournaSettings.this, CREATE_NEW_TEAM, msg);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "checkIfTeamAlreadyExists: databaseError=" + databaseError);
            }
        });

    }

    // --------------- Dialog to create a new tournament -------------------
    private class TournaDialogClass extends Dialog implements
            View.OnClickListener {

        public Activity parentActivity;
        public Dialog d;
        public Button enter, cancel;
        private Spinner mSpinner0;
        private Spinner mSpinner1;
        private Spinner mSpinner2;
        private SharedData mCommon;
        private List<String> mMatchTypeListLong;
        private List<String> mMatchTypeListShort;

        public TournaDialogClass(Activity a) {
            super(a);
            this.parentActivity = a;
            mCommon = SharedData.getInstance();
            mMatchTypeListLong  = new ArrayList<>(Arrays.asList(
                    Constants.LEAGUE,
                    Constants.SE_LONG,
                    Constants.DE_LONG));
            mMatchTypeListShort  = new ArrayList<>(Arrays.asList(
                    Constants.LEAGUE,
                    Constants.SE,
                    Constants.DE));
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.tourna_new_tourna_dialog);

            mSpinner0 = findViewById(R.id.tourna_type_spinner);
            mSpinner1 = findViewById(R.id.mNum_spinner);
            mSpinner2 = findViewById(R.id.bestOf_spinner);

            final ArrayAdapter<String> dataAdapter0 = new ArrayAdapter<>(parentActivity,
                    android.R.layout.simple_spinner_item, mMatchTypeListLong);
            dataAdapter0.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner0.setAdapter(dataAdapter0);
            mSpinner0.setSelection(1);

            mSpinner0.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Log.d(TAG, "mSpinner0:onItemSelected: " + mMatchTypeListLong.get(i));
                    if(i==0) {
                        mSpinner1.setVisibility(View.VISIBLE);
                        findViewById(R.id.league_mnum_ll).setVisibility(View.VISIBLE);
                        findViewById(R.id.league_mnum_line).setVisibility(View.VISIBLE);
                        mSpinner2.setVisibility(View.VISIBLE);
                        findViewById(R.id.league_bestof_ll).setVisibility(View.VISIBLE);
                        findViewById(R.id.league_bestof_line).setVisibility(View.VISIBLE);

                    } else {
                        findViewById(R.id.league_mnum_line).setVisibility(View.GONE);
                        findViewById(R.id.league_mnum_ll).setVisibility(View.GONE);
                        mSpinner1.setVisibility(View.GONE);
                        findViewById(R.id.league_bestof_line).setVisibility(View.GONE);
                        findViewById(R.id.league_bestof_ll).setVisibility(View.GONE);
                        mSpinner2.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });


            List<Integer> matchNumList = new ArrayList<Integer>(Arrays.asList(1,2,3,4));
            final ArrayAdapter<Integer> dataAdapter1 = new ArrayAdapter<>(parentActivity,
                    android.R.layout.simple_spinner_item, matchNumList);
            dataAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner1.setSelection(0);
            mSpinner1.setAdapter(dataAdapter1);


            List<Integer> bestOfList = new ArrayList<Integer>(Arrays.asList(1,3));
            final ArrayAdapter<Integer> dataAdapter2 = new ArrayAdapter<>(parentActivity,
                    android.R.layout.simple_spinner_item, bestOfList);
            dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner2.setSelection(2);
            mSpinner2.setAdapter(dataAdapter2);

            enter = findViewById(R.id.enter_button);
            enter.setOnClickListener(this);

            cancel = findViewById(R.id.cancel_button);
            cancel.setOnClickListener(this);

        }


        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.cancel_button:
                    //parentActivity.finish();
                    break;
                case R.id.enter_button:
                    createTournament();
                    break;
                default:
                    break;
            }
            dismiss();
        }

        private void createTournament() {
            int idx =  mSpinner0.getSelectedItemPosition();
            String type  = mMatchTypeListShort.get(idx);
            String mNum = "";
            if(View.VISIBLE==mSpinner1.getVisibility()) mNum = mSpinner1.getSelectedItem().toString();
            String bestOf = "";
            if(View.VISIBLE==mSpinner2.getVisibility()) bestOf = mSpinner2.getSelectedItem().toString();


            EditText et_tourna = findViewById(R.id.et_newTourna);
            String tName = et_tourna.getText().toString().toUpperCase();
            Log.v(TAG, "createTournament: " + type + "," + tName + ": " + mNum + "," + bestOf);
            if(tName.isEmpty()) {
                Log.e(TAG, "createTournament : name is empty");
                Toast.makeText(TournaSettings.this, "Enter tournament name!", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            EditText et_tourna_desc = findViewById(R.id.et_newTourna_desc);
            String desc = et_tourna_desc.getText().toString();

            final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                    .child(mCommon.mClub).child(Constants.TOURNA);
            dbRef.child(Constants.ACTIVE).child(tName).setValue(type);
            dbRef.child(tName).child(Constants.DESCRIPTION).setValue(desc);
            dbRef.child(tName).child(Constants.TYPE).setValue(type);
            if(!mNum.isEmpty()) {
                dbRef.child(tName).child(Constants.MATCHES).child(Constants.META)
                        .child(Constants.INFO).child(Constants.NUM_OF_MATCHES)
                        .setValue(Integer.valueOf(mNum));
            }
            if(!bestOf.isEmpty()) {
                dbRef.child(tName).child(Constants.MATCHES).child(Constants.META)
                        .child(Constants.INFO).child(Constants.NUM_OF_GAMES)
                        .setValue(Integer.valueOf(bestOf));
            }
            mCommon.createDBLock(tName);
            mCommon.setDBUpdated(true);
            Toast.makeText(TournaSettings.this, "Tournament '" + tName +"' created.", Toast.LENGTH_LONG)
                    .show();

        }
    } //end of TournaDialogClass





    private void onClickNewMatch() {
        Context wrapper = new ContextThemeWrapper(TournaSettings.this, R.style.WhitePopup);
        final PopupMenu popup = new PopupMenu(wrapper, findViewById(R.id.addNewMatch_btn));
        popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
        if (Build.VERSION.SDK_INT >= 23) {
            popup.setGravity(Gravity.END);
        }
        popup.getMenu().clear();
        Menu pMenu = popup.getMenu();
        for(Map.Entry<String,String> tourna : mCommon.mTournaMap.entrySet()) {
            pMenu.add(tourna.getKey());
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Log.v(TAG, "onMenuItemClick[" + menuItem.getItemId()+ "] :" + menuItem.getTitle().toString());
                mTourna = menuItem.getTitle().toString();
                mCommon.readDBTeam(mTourna,TournaSettings.this, TournaSettings.this);
                //callback is completed()
                popup.dismiss();
                return true;
            }
        });
        popup.show();//showing popup menu
    }

    private void getMatchInputs(final String tourna) {

        if(mCommon.isLeagueTournament(tourna)) {
            Log.v(TAG, "getMatchInputs: " + tourna);
            MatchDialogClass customDialog = new MatchDialogClass(TournaSettings.this);
            customDialog.setTourna(tourna);
            customDialog.show();
        } else if (mCommon.isEliminationTournament(tourna)) {
            Log.d(TAG, "EliminationTournament: to create fixture");
            Intent myIntent = new Intent(TournaSettings.this, TournaSeeding.class);
            myIntent.putExtra(Constants.TOURNA, tourna);
            TournaSettings.this.startActivity(myIntent);
        }
    }

    //CallbackRoutine Callback interfaces
    public void profileFetched() { }
    public void callback(final String key, final Object inobj) {
        if(key.equals(CREATE_NEW_TEAM)) {
            //callback for ADD_PLAYER
            onClickNewteam(key, inobj);
        }
    }
    public void alertResult(final String in, final Boolean ok, final Boolean ko) {
        if(!in.equals(CREATE_NEW_TEAM)) return;
        if(ok) {
            if(mTeamShort.isEmpty() || mTeamLong.isEmpty()) return;
            updateDB_newteam(mTeamShort, mTeamLong);
        }
    }
    public void completed (final String in, final Boolean ok) {
        Log.w(TAG, "completed: " + in + ":" + ok);
        if(in.equals(Constants.CB_READTOURNA)) {
            //if(ok) showTournaments();
            if(ok) mTUtil.showTournaments(findViewById(R.id.addNewTeam_btn), findViewById(R.id.settings_ll));
        } else if(in.equals(Constants.CB_SHOWTOURNA)) {
            if(ok) {
                mTourna = mTUtil.mTourna;
                if (null==mCommon.mTournaMap || mCommon.mTournaMap.size() == 0) {
                    Log.w(TAG, "completed: tournament map not found");
                    return;
                }

                if(mCommon.isLeagueTournament(mTourna)) {
                    mCustomDialog.setContents(mTourna, CREATE_NEW_TEAM,
                            "Add new team to " + mTourna,
                            "Short name", " P4F ",   //8 chars
                            "Full name", "  Play for Fun  ");
                    mCustomDialog.setTitle(mTourna);
                    mCustomDialog.show();  //callback is handled by onClickNewteam()
                } else if (mCommon.isEliminationTournament(mTourna)) {
                    TeamDialogClass customDialog = new TeamDialogClass(TournaSettings.this, mTourna);
                    customDialog.show();
                }
            }
        } else {
            //callback from readDBTeam
            if (ok) getMatchInputs(in);
        }
    }

    private void updateDB_newteam(final String short_name, final String long_name) {
        if(mCommon.mTeams.contains(short_name)) {
            Toast.makeText(TournaSettings.this, "Team " + short_name + " already exists!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!mCommon.isDBConnected()) {
            Toast.makeText(TournaSettings.this, "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "updateDB_newteam:[" + short_name + ":" + long_name + "]");
        DatabaseReference teamDBRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(mTourna).child(Constants.TEAMS).child(short_name);
        teamDBRef.child(Constants.SCORE).setValue(new TeamScoreDBEntry());
        teamDBRef.child(Constants.DESCRIPTION).setValue(long_name);
        FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                .child(mTourna).child(Constants.TEAMS_SUMMARY).child(short_name).setValue(true);

        Toast.makeText(TournaSettings.this, "New team '" + short_name + "'" +
                " created for '" + mTourna +"'", Toast.LENGTH_SHORT).show();
    }


    // --------------- Dialog to schedule a new match -------------------
    private class TeamDialogClass extends Dialog implements
            View.OnClickListener {

        public Activity parentActivity;
        public Dialog d;
        private String mTourna;
        public Button enter, cancel;
        private Spinner mSpinner0;
        private SharedData mCommon;
        private ArrayList<Integer> mResIdList;
        private Boolean mDialogueDismiss, mDialogueDone;
        private String mTitle;


        public TeamDialogClass(final Activity a, final String tourna) {
            super(a);
            this.parentActivity = a;
            this.mTourna = tourna;
            mCommon = SharedData.getInstance();
            mResIdList = new ArrayList<>(
                    new ArrayList<>( Arrays.asList(
                            R.id.p1_et, R.id.p2_et,
                            R.id.p3_et, R.id.p4_et,
                            R.id.p5_et, R.id.p6_et,
                            R.id.p7_et, R.id.p8_et
                    ))
            );
            mDialogueDismiss = mDialogueDone = false;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.tourna_new_team_dialog);
            mDialogueDismiss = mDialogueDone = false;
            mSpinner0 = findViewById(R.id.numPlayers);

            String tStr = "Add new team to " + mTourna;
            TextView titleTV = findViewById(R.id.newTeam_tv);
            titleTV.setText(tStr);

            final ArrayAdapter<Integer> dataAdapter0 = new ArrayAdapter<>(parentActivity,
                    android.R.layout.simple_spinner_item,
                    new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8)));
            dataAdapter0.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner0.setAdapter(dataAdapter0);
            mSpinner0.setSelection(1);

            mSpinner0.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Log.d(TAG, "mSpinner0:onItemSelected: " + (i+1));

                    for (int j = 0; j <= i; j++) {  //i is the position index [0-7]
                        findViewById(mResIdList.get(j)).setVisibility(View.VISIBLE);
                    }
                    for (int j = i+1; j < mResIdList.size(); j++) {
                        findViewById(mResIdList.get(j)).setVisibility(View.GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            enter = findViewById(R.id.enter_button);
            enter.setOnClickListener(this);

            cancel = findViewById(R.id.cancel_button);
            cancel.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.cancel_button:
                    //parentActivity.finish();
                    dismiss();
                    break;
                case R.id.enter_button:
                    createTeam();
                    //wait if the DB update was successful
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                for (int i = 0; i < 2*60*5; i++) {  //max of 5m for entry
                                    Thread.sleep(500);
                                    if (mDialogueDismiss) {
                                        dismiss();
                                        return;
                                    }
                                    if(mDialogueDone) return;
                                }
                                dismiss();

                            } catch (InterruptedException e) {
                                Log.w(TAG, "doInBackground: InterruptedException=" + e.getMessage());
                                dismiss();
                            } catch (Exception e) {
                                Log.w(TAG, "doInBackground: Exception:" + e.getMessage());
                                e.printStackTrace();
                                dismiss();
                            }
                        }
                    }, 1000);
                    break;
                default:
                    break;
            }


        }


        private void createTeam() {
            EditText et_team = findViewById(R.id.newTeam_et);
            final String tName = et_team.getText().toString();

            if(tName.isEmpty()) {
                Log.e(TAG, "createTeam : name is empty");
                Toast.makeText(TournaSettings.this, "Enter team name!", Toast.LENGTH_LONG)
                        .show();
                mDialogueDone = true; //stop the background loop waiting to dismiss the dialogue window
                return;
            }

            int idx =  mSpinner0.getSelectedItemPosition();
            Integer resId  = mResIdList.get(idx);
            Boolean err = false;
            List<String> playerList = new ArrayList<>();
            for (int j = 0; j <= idx; j++) {
                EditText et = findViewById(mResIdList.get(j));
                String pName = et.getText().toString();
                if(pName.isEmpty()) err = true;
                else playerList.add(pName);
            }

            Log.v(TAG, "createTeam: " + tName + "," + playerList.toString());
            if(err) {
                Log.e(TAG, "createTeam : Invalid player names");
                Toast.makeText(TournaSettings.this, "Invalid player names!",
                        Toast.LENGTH_LONG).show();
                mDialogueDone = true; //stop the background loop waiting to dismiss the dialogue window
                return;
            }

            final TeamDBEntry teamDBEntry = new TeamDBEntry();
            teamDBEntry.setId(tName);
            teamDBEntry.setP(playerList);

            final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                    .child(Constants.TOURNA).child(mTourna).child(Constants.TEAMS);
            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d(TAG, mTourna + " createTeam: onDataChange:" + dataSnapshot.getKey() + dataSnapshot.toString());
                    GenericTypeIndicator<List<TeamDBEntry>> genericTypeIndicator =
                            new GenericTypeIndicator<List<TeamDBEntry>>() {
                            };
                    List<TeamDBEntry> teamList = dataSnapshot.getValue(genericTypeIndicator);
                    if (null == teamList) {
                        Log.d(TAG, "onDataChange: First team added in DB");
                        ArrayList<TeamDBEntry> mTeams = new ArrayList<> ();
                        mTeams.add(teamDBEntry);
                        dbRef.setValue(mTeams);
                        Toast.makeText(TournaSettings.this, "Team '" + tName +"' created.",
                                Toast.LENGTH_LONG).show();
                        mDialogueDismiss = true; //notify background loop to dismiss the dialogue window
                        return;
                    }

                    final ArrayList<TeamDBEntry> mTeams = new ArrayList<> (teamList);
                    Log.v(TAG, "createTeam: " + mTeams.toString());
                    Boolean replaceFlag = false;
                    for (int i=0; i < teamList.size(); i++) {
                        TeamDBEntry dbEntry = teamList.get(i);
                        if(dbEntry.getId().equals(teamDBEntry.getId())) {
                            if(!mCommon.isRoot()) {
                                Toast.makeText(TournaSettings.this, dbEntry.getId() + " already exists!",
                                        Toast.LENGTH_LONG).show();
                                mDialogueDismiss = true; //notify background loop to dismiss the dialogue window
                                return;
                            } else {
                                replaceFlag = true;
                                Log.v(TAG, "createTeam: Existing team " + dbEntry.getId() +
                                        " can be replaced");
                                mTeams.set(i,teamDBEntry);
                            }
                        }
                    }
                    if(!replaceFlag) {
                        mTeams.add(teamDBEntry);
                        dbRef.setValue(mTeams);
                        Toast.makeText(TournaSettings.this, "Team '" + tName +"' created.",
                                Toast.LENGTH_LONG).show();
                        mDialogueDismiss = true; //notify background loop to dismiss the dialogue window
                    } else {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TournaSettings.this);
                        alertBuilder.setTitle("Replace existing team?");
                        alertBuilder.setMessage("Replace existing team by " +
                                teamDBEntry.toDispString() + "?");
                        alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dbRef.setValue(mTeams);
                                Toast.makeText(TournaSettings.this, "Team '" + tName +"' created.",
                                        Toast.LENGTH_LONG).show();
                                mDialogueDismiss = true; //notify background loop to dismiss the dialogue window
                            }
                        });
                        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mDialogueDone = true; //stop the background loop waiting to dismiss the dialogue window
                                return;
                            }
                        });
                        alertBuilder.show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w(TAG, "createTeam:onCancelled", databaseError.toException());
                    Toast.makeText(TournaSettings.this, "DB error while fetching team entry: " + databaseError.toString(),
                            Toast.LENGTH_LONG).show();
                    mDialogueDismiss = true; //notify background loop to dismiss the dialogue window
                }
            });
        }
    } //end of TeamDialogClass

    // --------------- Dialog to schedule a new match -------------------
    private class MatchDialogClass extends Dialog implements
            View.OnClickListener {

        public Activity parentActivity;
        public Dialog d;
        public Button enter, cancel;
        private Spinner mSpinner_T1;
        private Spinner mSpinner_T2;
        private SharedData mCommon;
        private Integer mNumOfMatches;
        private Integer mNextKey;
        private String mTourna;

        public MatchDialogClass(Activity a) {
            super(a);
            this.parentActivity = a;
            mCommon = SharedData.getInstance();
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.tourna_new_match_dialog);

            String tStr = "Add new match to " + mTourna;
            TextView titleTV = findViewById(R.id.newMatch_tv);
            titleTV.setText(tStr);

            mSpinner_T1 = findViewById(R.id.t1_spinner);
            List<String> teamList = new ArrayList<>(mCommon.mTeams);
            final ArrayAdapter<String> dataAdapterT1 = new ArrayAdapter<>(parentActivity,
                    android.R.layout.simple_spinner_item, teamList);
            dataAdapterT1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            //mSpinner_P4.setSelection(3);
            mSpinner_T1.setAdapter(dataAdapterT1);
            if(teamList.size()>1) mSpinner_T1.setSelection(0);

            mSpinner_T2 = findViewById(R.id.t2_spinner);
            final ArrayAdapter<String> dataAdapterT2 = new ArrayAdapter<>(parentActivity,
                    android.R.layout.simple_spinner_item, teamList);
            dataAdapterT2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner_T2.setAdapter(dataAdapterT2);
            if(teamList.size()>1) mSpinner_T1.setSelection(1);

            enter = findViewById(R.id.enter_button);
            enter.setOnClickListener(this);

            cancel = findViewById(R.id.cancel_button);
            cancel.setOnClickListener(this);

        }

        public void setTourna(final String tourna) {
            mTourna = tourna;
        }

        @Override
        public void onClick(View v) {
            String team1 = mSpinner_T1.getSelectedItem().toString();
            String team2 = mSpinner_T2.getSelectedItem().toString();
            Log.v(TAG, "CustomDialogClass: " + team1 + ":" + team2);
            switch (v.getId()) {
                case R.id.cancel_button:
                    parentActivity.finish();
                    break;
                case R.id.enter_button:
                    readDB(team1, team2);
                    break;
                default:
                    break;
            }
            dismiss();
        }

        private void readDB(final String team1, final String team2) {
            if(mTourna.isEmpty()) return;
            final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                    .child(mTourna).child(Constants.MATCHES).child(Constants.META);
            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    DataSnapshot ds = dataSnapshot.child(Constants.INFO + "/" + Constants.NUM_OF_MATCHES);
                    Integer numOfMatches = ds.getValue(Integer.class);
                    if(numOfMatches==null) {
                        Log.e(TAG, "Failed to read Num of matches!");
                        Toast.makeText(parentActivity, "There are no matches configured.", Toast.LENGTH_LONG).show();
                        ((TournaSettings)parentActivity).killActivity();
                        return;
                    }
                    mNumOfMatches = numOfMatches;
                    Log.i(TAG, "Num of Matches = " + mNumOfMatches.toString());

                    mNextKey = 0;
                    for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                        final String keyStr = childSnapshot.getKey();
                        if(keyStr.equals("info")) continue;
                        Integer key = Integer.valueOf(keyStr);
                        if(key > mNextKey) mNextKey = key;
                        Log.i(TAG,   "childSnapshot mNextKey: " + mNextKey);
                    }
                    mNextKey++;
                    if(mNumOfMatches>0) {
                        createMatch(team1, team2, mNextKey, mNumOfMatches);
                    }
                    else {
                        mCommon.showToast(parentActivity, "No scheduled matches!", Toast.LENGTH_SHORT);
                        finish();
                    }
                }


                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    mCommon.showToast(parentActivity, "DB error on read: " + databaseError.getMessage(), Toast.LENGTH_SHORT);
                }
            });
        }

        private void createMatch(final String team1, final String team2, Integer nextKey, Integer numMatches) {
            Log.v(TAG, "createMatch: " + team1 + ":" + team2 + " nextkey=" + nextKey.toString() + " numMatches=" + numMatches);
            if(team1.equals(team2)) {
                mCommon.showToast(parentActivity, "Bad input! Select different teams.", Toast.LENGTH_LONG);
                return;
            }
            final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.TOURNA)
                    .child(mTourna).child(Constants.MATCHES).child(Constants.META).child(nextKey.toString());
            MatchInfo mInfo = new MatchInfo();
            mInfo.T1 = team1;
            mInfo.T2 = team2;
            EditText et_desc = findViewById(R.id.et_newMatch);
            mInfo.desc = et_desc.getText().toString();
            mInfo.done = false;
            mInfo.key = nextKey.toString();
            dbRef.child(Constants.INFO).setValue(mInfo);
            Log.v(TAG, "createMatch: mInfo set:" + mInfo.toString());

            for(int i=0; i<numMatches; i++) {
                String matchNum = String.format(Locale.getDefault(),"%s%d",Constants.MATCHID_PREFIX, i+1);
                dbRef.child(matchNum).setValue(false);
                Log.v(TAG, "createMatch: MX set:" + matchNum);
            }

            mCommon.showToast(parentActivity, "Match scheduled in tournament '" + mTourna + "'", Toast.LENGTH_LONG);
        }
    }
}


