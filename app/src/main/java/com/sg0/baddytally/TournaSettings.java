package com.sg0.baddytally;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
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
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

class NewTournaData {
    String desc;
    String mNum;
    String bestOf;
    NewTournaData() {
        desc = "";
        mNum = "";
        bestOf = "";
    }
}

public class TournaSettings extends AppCompatActivity implements CallbackRoutine{
    private static final String TAG = "TournaSettings";
    private static final String CREATE_NEW_TOURNA = "Create new tournament";
    private static final String CREATE_NEW_TEAM = "Create new team";
    private static final String CREATE_NEW_MATCH = "Create new match";
    private DatabaseReference mDatabase;
    private SharedData mCommon;
    private TournaUtil mTUtil;
    private String mTourna;
    private String mTeamShort;
    private String mTeamLong;
    private TournaEditTextDialog mCustomDialog;
    private TeamDialogClass mTeamDialog;
    private Handler mMainHandler;
    private String mNewTournaType;
    private String mScenario;
    private NewTournaData newTournaData;

    public void killActivity() {
        Log.d(TAG, "killActivity: ");
        finish();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        //killActivity();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        mCommon.killActivity(this, RESULT_OK);
        Intent myIntent = new Intent(TournaSettings.this, TournaLanding.class);
        /*If FLAG_ACTIVITY_CLEAR_TOP set, and the activity being launched is already running in
        the current task, then instead of launching a new instance of that activity, all of the
        other activities on top of it will be closed and this Intent will be delivered to the
        (now on top) old activity as a new Intent. */
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TournaSettings.this.startActivity(myIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.basic_menu_main, menu);
        menu.findItem(R.id.action_logout).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                recreate();
                break;
            case R.id.action_help:
                AlertDialog.Builder hBuilder = new AlertDialog.Builder(TournaSettings.this);
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
                SharedData.showAboutAlert(TournaSettings.this);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tourna_settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mCommon = SharedData.getInstance();
        mTUtil = new TournaUtil(TournaSettings.this, TournaSettings.this);
        mCustomDialog = null;
        // = new TournaEditTextDialog(TournaSettings.this, TournaSettings.this);
        Log.w(TAG, "onCreate :" + mCommon.toString());
        mMainHandler = new Handler();
        mNewTournaType = "";
        mScenario = "";
        mTeamDialog = null;

        Intent myIntent = getIntent(); // gets the previously created intent
        String animFlag = myIntent.getStringExtra("animation");

        Button createNewTournaBtn = findViewById(R.id.createNewTourna_btn);
        createNewTournaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mCommon.isPermitted(TournaSettings.this)) return;
                if (!mCommon.isRoot()) {
                    Toast.makeText(TournaSettings.this,
                            "You don't have permissions to perform this.", Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                mScenario = CREATE_NEW_TOURNA;
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
                mScenario = CREATE_NEW_TEAM;
                if(mTUtil!=null) mTUtil.fetchActiveTournaments();
                //callback is completed()
            }
        });

        if(animFlag!=null && animFlag.equals("newteam")) {
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            addNewteamBtn.startAnimation(shake);
        }


        //schedule new match or create fixture
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
                mScenario = CREATE_NEW_MATCH;
                onClickNewMatch();
            }
        });

        if(animFlag!=null && animFlag.equals("fixture")) {
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake_long);
            addNewMatchBtn.startAnimation(shake);
        }

        Switch clearcache_sw = findViewById(R.id.clearcache_sw);
        clearcache_sw.setChecked(false);
        clearcache_sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean bChecked) {
                if (bChecked) {
                    mCommon.clearData(TournaSettings.this, true);
                    mCommon.setDBUpdated(true); //notify Main to refresh view

                    //Restart the app: Needed to re-invoke Application.onCreate() to disable DB persistence,
                    //though that behavior is very inconsistent. See comments in ScoreTally.java.
                    //setResult(Constants.RESTARTAPP);
                    //killActivity();
                    SharedData.getInstance().killActivity(TournaSettings.this, RESULT_OK);
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

        TournaUtil mTUtil = new TournaUtil(TournaSettings.this, TournaSettings.this);
        mTUtil.fetchActiveTournaments();
    }

    //only for LeagueTournament
    private void onClickNewteam(final String key, final Object inobj) {

        //Log.v(TAG, "onClickNewteam... " + key);
        if(inobj==null) {
            Log.d(TAG, "callback: null");
            return;
        }
        //noinspection unchecked
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
        //Log.d(TAG, "callback: got back:" + tourna + mTeamShort + mTeamLong);
        Boolean errVal = false;
        //Log.i(TAG, "team name: [" + mTeamShort + "] [" + mTeamLong + "]");
        if(!mTeamShort.matches("[A-Z0-9]+")) errVal = true;
        if(!mTeamLong.matches("[A-Za-z0-9 ]+")) errVal = true;
        if(errVal) {
            Toast.makeText(TournaSettings.this, "Bad Input! Enter only alphanumeric values", Toast.LENGTH_SHORT)
                    .show();
            Log.e(TAG, "Bad Input! Enter only alphanumeric values: [" + mTeamShort + "] [" + mTeamLong + "]");
        } else {
            checkIfTeamAlreadyExists(tourna, mTeamShort);
        }
    }

    public void checkIfTeamAlreadyExists(final String tourna, final String teamShort) {
        final DatabaseReference dbRef = mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
                .child(tourna).child(Constants.TEAMS_SUMMARY);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                DataSnapshot ds = dataSnapshot.child(teamShort);
                Boolean teamStatus = ds.getValue(Boolean.class);
                if(teamStatus!=null && teamStatus) {
                    Toast.makeText(TournaSettings.this, "Team name '" + mTeamShort + "' is taken. Use something else.", Toast.LENGTH_SHORT)
                            .show();
                    Log.e(TAG,  "Team name '" + mTeamShort + "' is taken. Use another short name. [" + mTeamLong + "]");
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
            mNewTournaType = "";
            newTournaData = null;
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
                    //Log.d(TAG, "mSpinner0:onItemSelected: " + mMatchTypeListLong.get(i));
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


            List<Integer> matchNumList = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
            final ArrayAdapter<Integer> dataAdapter1 = new ArrayAdapter<>(parentActivity,
                    android.R.layout.simple_spinner_item, matchNumList);
            dataAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner1.setSelection(0);
            mSpinner1.setAdapter(dataAdapter1);


            List<Integer> bestOfList = new ArrayList<>(Arrays.asList(1, 3));
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
                    if(!mCommon.isDBConnected()) {
                        Toast.makeText(TournaSettings.this,
                                "DB connection is stale, retry...",
                                Toast.LENGTH_SHORT).show();
                        mCommon.wakeUpDBConnection_profile();
                        return;
                    }
                    //make sure that External storage is writable, if file option is used.
                    if(((CheckBox)findViewById(R.id.tourna_datafile_cb)).isChecked()) {
                        //The first time this is done, app will get external permission.
                        if (mCommon.isExternalStorageWritable(TournaSettings.this)) {
                            preCreateTournament();
                        } else {
                            Toast.makeText(TournaSettings.this,
                                    "You have to give this app external storage permission once. If already given, check android settings for this app.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        preCreateTournament();
                    }

                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(((CheckBox)findViewById(R.id.tourna_datafile_cb)).isChecked()) {
                                //tournament data to be updated from a file
                                if (mCommon.isExternalStorageWritable(TournaSettings.this)) {
                                    importData();
                                } else {
                                    Toast.makeText(TournaSettings.this,
                                            "External storage not writable!\nGive 'storage' app permission and try again.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                //Create tournament In DB is done later to complete validation checks
                                //when a file is provided. In this case, create it in DB right away.
                                checkIfTournaAlreadyExists(mTourna,null);
                            }
                        }
                    });
                    break;
                default:
                    break;
            }
            dismiss();
        }

        private void preCreateTournament() {
            int idx =  mSpinner0.getSelectedItemPosition();
            mNewTournaType  = mMatchTypeListShort.get(idx);
            newTournaData = new NewTournaData();
            //String mNum = "";
            if(View.VISIBLE==mSpinner1.getVisibility()) newTournaData.mNum = mSpinner1.getSelectedItem().toString();
            //String bestOf = "";
            if(View.VISIBLE==mSpinner2.getVisibility()) newTournaData.bestOf = mSpinner2.getSelectedItem().toString();


            EditText et_tourna = findViewById(R.id.et_newTourna);
            mTourna = et_tourna.getText().toString().toUpperCase();
            Log.v(TAG, "preCreateTournament: " + mNewTournaType + "," + mTourna + ": " +
                    newTournaData.mNum + "," + newTournaData.bestOf);
            if(mTourna.isEmpty()) {
                Log.e(TAG, "preCreateTournament : name is empty");
                Toast.makeText(TournaSettings.this, "Enter tournament name!",
                        Toast.LENGTH_LONG).show();
                return;
            }
            EditText et_tourna_desc = findViewById(R.id.et_newTourna_desc);
            newTournaData.desc = et_tourna_desc.getText().toString();
        }

    } //end of TournaDialogClass



    public void checkIfTournaAlreadyExists(final String tourna, final ArrayList<TeamInfo> teamList) {
        final DatabaseReference dbRef = mDatabase.child(mCommon.mClub).child(Constants.TOURNA);
        dbRef.child(Constants.ACTIVE).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(tourna)) {
                    Log.w(TAG, "createSubTournament: tournament already exists: " + tourna);
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TournaSettings.this);
                    alertBuilder.setTitle("Duplicate name");
                    alertBuilder.setMessage(
                            tourna + " already exists!\nDelete the old one if you want to use the same name.");
                    alertBuilder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //nothing to do
                        }
                    });
                    alertBuilder.show();
                } else {
                    //tournament does not exist in DB, create new.
                    createTournamentInDB(teamList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private boolean createTournamentInDB(final ArrayList<TeamInfo> teamList) {
        if(null==newTournaData) return false;

        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.TOURNA);
        dbRef.child(Constants.ACTIVE).child(mTourna).setValue(mNewTournaType);
        dbRef.child(mTourna).child(Constants.DESCRIPTION).setValue(newTournaData.desc);
        dbRef.child(mTourna).child(Constants.TYPE).setValue(mNewTournaType);
        if(!newTournaData.mNum.isEmpty()) {
            dbRef.child(mTourna).child(Constants.MATCHES).child(Constants.META)
                    .child(Constants.INFO).child(Constants.NUM_OF_MATCHES)
                    .setValue(Integer.valueOf(newTournaData.mNum));
        }
        if(!newTournaData.bestOf.isEmpty()) {
            dbRef.child(mTourna).child(Constants.MATCHES).child(Constants.META)
                    .child(Constants.INFO).child(Constants.NUM_OF_GAMES)
                    .setValue(Integer.valueOf(newTournaData.bestOf));
        }


        mCommon.createDBLock(mTourna);  //create lock node in DB
        mCommon.setDBUpdated(true);
        newTournaData = null;
        mCustomDialog = null;

        mCommon.addHistory(
                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub),
                String.format(Locale.getDefault(),"NEW %s/%s [%s]",
                        Constants.TOURNA, mTourna, mNewTournaType));

        if(null==teamList) {
            //not teams or players to be created. User will create this manually.
            Toast.makeText(TournaSettings.this, "Tournament '" + mTourna +"' created." +
                    "\nAdd teams to the new tournament.",
                    Toast.LENGTH_SHORT).show();
            killActivity();

            //wake up connection and read profile again from DB to check for password changes
            mCommon.wakeUpDBConnection_profile();
            Intent myIntent = new Intent(this, TournaSettings.class);
            myIntent.putExtra("animation", "newteam");
            myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(myIntent);


            return true;
        }

        if(mNewTournaType.equals(Constants.LEAGUE)) {
            createLeagueTeamData(teamList);
        } else {
            createSEDETeamData(teamList);
        }
        return true;
    }

    private void importData() {
        Log.d(TAG, "importData: ");
        mCommon.performFileSearch(TournaSettings.this);
        Toast.makeText(TournaSettings.this,
                "Choose the excel file to import team and player data",
                Toast.LENGTH_LONG).show();
    }

    //callback when reading team/player data from file
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        Log.d(TAG, "onActivityResult: " + requestCode);

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == SharedData.READ_REQUEST_CODE && resultCode == RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                if(null == uri) return;
                Log.i(TAG, "Uri: " + uri.toString());
                ArrayList<TeamInfo> teamList = null;
                try {
                    if(uri.toString().contains(".xlsx")) {
                        //not implemented, see comments in SharedData
                        //teamList = SharedData.getInstance().readXLSXFile(this, uri);
                    } else if(uri.toString().contains(".xls")) {
                        teamList = SharedData.getInstance().readExcel(this, uri);
                    } else {
                        teamList = SharedData.getInstance().readTextFromUri(this, uri);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onActivityResult: Exception in readExcel:" + e.toString());
                    //Toast.makeText(TournaSettings.this, "Failure in parsing the xls file: " + e.getMessage(),
                    //        Toast.LENGTH_SHORT).show();
                    mCommon.showAlert(null, TournaSettings.this, "Bad input. ",
                            "Failed to parse the input file.\n" +
                                "Make sure it is excel 97-2003 ('.xls') or plain text file format.");
                    return;

                }

                if(!validTeamData(teamList)) return;
                final ArrayList<TeamInfo> finalTeamList = teamList;

                Log.i(TAG, "onActivityResult: " + teamList.size() + " teams added to " + mTourna);

                SpannableStringBuilder sb = new SpannableStringBuilder("\n");
                int count = 1;
                sb.append("\n");
                for(TeamInfo tI: teamList) {
                    sb.append(Integer.toString(count)); sb.append(")\t");
                    sb.append(tI.toDisplayString());
                    sb.append("\n");
                    count++;
                }
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TournaSettings.this);
                SpannableStringBuilder title = new SpannableStringBuilder("Review new teams for ");
                title.append(SharedData.getInstance().getStyleString(mTourna, Typeface.ITALIC));
                alertBuilder.setTitle(SharedData.getInstance().getColorString(title,
                        getResources().getColor(R.color.colorTealGreen)));
                alertBuilder.setMessage(sb);
                alertBuilder.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        checkIfTournaAlreadyExists(mTourna, finalTeamList);
                    }
                });
                alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                alertBuilder.show();
            }
        }
    }

    boolean validTeamData(ArrayList<TeamInfo> teamList) {
        if(null==teamList || teamList.size()==0) {
            Log.i(TAG, "validTeamData: No teams found for " + mTourna);
            mCommon.showAlert(null, TournaSettings.this,
                    "Error", "No teams found for " + mTourna);
            return false;
        }

        //Do some data validation
        for(TeamInfo ti1: teamList) {
            for(TeamInfo ti2: teamList) {
                if(ti1 == ti2) continue;
                if(ti1.name.equals(ti2.name)) {
                    mCommon.showAlert(null, TournaSettings.this,
                            "Error",
                            "Duplicate team ID: " + ti2.name + "\nUse unique team IDs and retry.");
                    return false;
                }
                if(ti1.desc.equals(ti2.desc)) {
                    mCommon.showAlert(null, TournaSettings.this,
                            "Error",
                            "Duplicate team name: " + ti2.desc + "\nUse unique team names and retry.");
                    return false;
                }
                for(String p1: ti1.players) {
                    if(p1.isEmpty()) continue;
                    for(String p2: ti2.players) {
                        if(p2.isEmpty()) continue;
                        if(p1.equals(p2)) {
                            mCommon.showAlert(null, TournaSettings.this,
                                    "Error",
                                    "Duplicate player names: " + p2 + "\nUse unique player names and retry.");
                            return false;
                        }
                    }
                }
                if(mNewTournaType.equals(Constants.LEAGUE)) {
                    for(String p1: ti1.p_nicks) {
                        if(p1.isEmpty()) continue;
                        for(String p2: ti2.p_nicks) {
                            if(p2.isEmpty()) continue;
                            if(p1.equals(p2)) {
                                mCommon.showAlert(null, TournaSettings.this,
                                        "Error",
                                        "Duplicate player ID: " + p2 + "\nUse unique player IDs and retry.");
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }



    private void createLeagueTeamData(final ArrayList<TeamInfo> teamList) {
        //Log.v(TAG, "createLeagueTeamData: " + teamList.toString());
        for (TeamInfo tI: teamList) {
            final String teamShortName = tI.name;
            final String teamLongName = tI.desc;
            if(teamShortName.isEmpty() || teamLongName.isEmpty()) continue;

            if(tI.p_nicks.size() != tI.players.size()) {
                Toast.makeText(TournaSettings.this, "Bad data for team '" + teamShortName +"'.",
                        Toast.LENGTH_SHORT).show();
                continue;
            }

            DatabaseReference teamDBRef = mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
                    .child(mTourna).child(Constants.TEAMS).child(teamShortName);
            teamDBRef.child(Constants.SCORE).setValue(new TeamScoreDBEntry());
            teamDBRef.child(Constants.DESCRIPTION).setValue(teamLongName);
            mDatabase.child(mCommon.mClub).child(Constants.TOURNA).child(mTourna)
                    .child(Constants.TEAMS_SUMMARY)
                    .child(teamShortName).setValue(true);
            Log.d(TAG, "createLeagueTeamData: Team created:" + teamShortName + ":" + teamLongName);

            for(int i=0; i < tI.p_nicks.size(); i++) {
                final String playerShortName = tI.p_nicks.get(i);
                final String playerLongName = tI.players.get(i);
                if(playerShortName.isEmpty() || playerLongName.isEmpty()) continue;
                PlayerInfo pInfo = new PlayerInfo();
                pInfo.T = teamShortName;
                pInfo.name = playerLongName;
                //Log.i(TAG, "createLeagueTeamData Adding player:" + playerShortName +
                //        " info:" + pInfo.toString());
                DatabaseReference teamsDBRef = mDatabase
                        .child(mCommon.mClub).child(Constants.TOURNA)
                        .child(mTourna).child(Constants.PLAYERS);
                teamsDBRef.child(playerShortName).setValue(pInfo);
            }
        }
        mCommon.setDBUpdated(true); //notify Main to refresh view
        mCommon.killActivity(TournaSettings.this, RESULT_OK);

        Toast.makeText(TournaSettings.this, mTourna +
                        " created successfully. Go ahead and 'create fixture' for the new tournament.",
                Toast.LENGTH_LONG).show();

        //wake up connection and read profile again from DB to check for password changes
        mCommon.wakeUpDBConnection_profile();
        Intent myIntent = new Intent(this, TournaSettings.class);
        myIntent.putExtra("animation", "fixture");
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.startActivity(myIntent);
    }

    private void createSEDETeamData(final ArrayList<TeamInfo> teamList) {
        //Log.v(TAG, "createSEDETeamData: " + teamList.toString());
        List<TeamDBEntry> dbTeamList = new ArrayList<>();
        for (TeamInfo tI:teamList) {
            TeamDBEntry teamDBEntry = new TeamDBEntry();
            teamDBEntry.setId(tI.name);
            teamDBEntry.setP(tI.players);
            dbTeamList.add(teamDBEntry);
            //Log.d(TAG, "createSEDETeamData: Adding:" + teamDBEntry.toString());
        }
        if(dbTeamList.size()==0) return;
        mDatabase.child(mCommon.mClub)
                .child(Constants.TOURNA).child(mTourna).child(Constants.TEAMS)
                .setValue(dbTeamList);
        Log.i(TAG, "createSEDETeamData: created " + teamList.size() + " teams");
        mCommon.setDBUpdated(true); //notify Main to refresh view
        mCommon.killActivity(TournaSettings.this, RESULT_OK);

        Toast.makeText(TournaSettings.this, mTourna +
                        " created successfully. Go ahead and 'create fixture' for the new tournament.",
                Toast.LENGTH_LONG).show();

        //wake up connection and read profile again from DB to check for password changes
        mCommon.wakeUpDBConnection_profile();
        Intent myIntent = new Intent(this, TournaSettings.class);
        myIntent.putExtra("animation", "fixture");
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.startActivity(myIntent);
    }

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
                //Log.v(TAG, "onMenuItemClick[" + menuItem.getItemId()+ "] :" + menuItem.getTitle().toString());
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
            //Log.v(TAG, "getMatchInputs: " + tourna);
            MatchDialogClass customDialog = new MatchDialogClass(TournaSettings.this);
            customDialog.setTourna(tourna);
            customDialog.show();
        } else if (mCommon.isEliminationTournament(tourna)) {
            //Log.d(TAG, "EliminationTournament: to create fixture");
            killActivity();
            Intent myIntent = new Intent(TournaSettings.this, TournaSeeding.class);
            myIntent.putExtra(Constants.TOURNA, tourna);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            TournaSettings.this.startActivity(myIntent);
        }
    }

    //CallbackRoutine Callback interfaces
    public void profileFetched() { }
    public void callback(final String key, final Object inobj) {
        if(key.equals(CREATE_NEW_TEAM)) {
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
        if(mScenario.isEmpty()) return;
        Log.w(TAG, "completed: " + in + ":" + ok);
        if(in.equals(Constants.CB_READTOURNA)) {
            //Callback from add new team
            if(ok) mTUtil.showTournaments(findViewById(R.id.addNewTeam_btn), findViewById(R.id.settings_ll));
        } else if(in.equals(Constants.CB_SHOWTOURNA)) {
            if(ok) {
                mTourna = mTUtil.mTourna;
                if (null==mCommon.mTournaMap || mCommon.mTournaMap.size() == 0) {
                    Log.w(TAG, "completed: tournament map not found");
                    return;
                }

                if(mCommon.isLeagueTournament(mTourna)) {
                    mCustomDialog = new TournaEditTextDialog(TournaSettings.this, TournaSettings.this);
                    mCustomDialog.setContents(mTourna, CREATE_NEW_TEAM,
                            "Add new team to " + mTourna,
                            "Short name", " P4F ",   //8 chars
                            "Full name", "  Play for Fun  ");
                    mCustomDialog.setTitle(mTourna);
                    mCustomDialog.show();  //callback is handled by onClickNewteam()
                } else if (mCommon.isEliminationTournament(mTourna)) {
                    mTeamDialog = new TeamDialogClass(TournaSettings.this, mTourna);
                    mTeamDialog.show();
                }
            }
        } else {
            //callback from readDBTeam
            if (ok) getMatchInputs(in);
        }
    }

    //only for LeagueTournament
    private void updateDB_newteam(final String short_name, final String long_name) {
        if(mCommon.mTeams.contains(short_name)) {
            Toast.makeText(TournaSettings.this, "Team " + short_name + " already exists!",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if(!mCommon.isDBConnected()) {
            Toast.makeText(TournaSettings.this,
                    "DB connection is stale, refresh and retry...",
                    Toast.LENGTH_SHORT).show();
            mCommon.wakeUpDBConnection_profile();
            return;
        }
        Log.i(TAG, "updateDB_newteam:[" + short_name + ":" + long_name + "]");
        DatabaseReference teamDBRef = mDatabase
                .child(mCommon.mClub).child(Constants.TOURNA)
                .child(mTourna).child(Constants.TEAMS).child(short_name);
        teamDBRef.child(Constants.SCORE).setValue(new TeamScoreDBEntry());
        teamDBRef.child(Constants.DESCRIPTION).setValue(long_name);
        mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
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
        private int mCount;


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
            mCount = 0;
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

            mCommon.wakeUpDBConnection_profile();
        }

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.cancel_button:
                    //parentActivity.finish();
                    dismiss();
                    break;
                case R.id.enter_button:
                    if(!mCommon.isDBConnected()) {
                        Toast.makeText(TournaSettings.this,
                                "DB connection is stale, retry...",
                                Toast.LENGTH_SHORT).show();
                        mCommon.wakeUpDBConnection_profile();
                        return;
                    }
                    createTeam();
                    //wait if the DB update was successful
                    mCount = 0;
                    mMainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mTeamDialog.checkAndDismiss();
                        }
                    }, 1000);
                    break;
                default:
                    break;
            }
        }

        void checkAndDismiss() {
            mCount++;
            Log.d(TAG, mCount + " checkAndDismiss: "+mDialogueDismiss + " done=" + mDialogueDone);
            if (mDialogueDismiss) {
                dismiss();
                return;
            }
            if(mDialogueDone) return;
            if(mCount>60*5) { dismiss(); return; }
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTeamDialog.checkAndDismiss();
                }
            }, 1000);
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

            final DatabaseReference dbRef = mDatabase.child(mCommon.mClub)
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

                        mCommon.addHistory(
                                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                                        .child(Constants.TOURNA).child(mTourna),
                                String.format(Locale.getDefault(),"NEW %s/%s/%s/%s",
                                        Constants.TOURNA, mTourna, Constants.TEAMS, tName));

                        Toast.makeText(TournaSettings.this, "Team '" + tName +"' created.",
                                Toast.LENGTH_LONG).show();
                        mDialogueDismiss = true; //notify background loop to dismiss the dialogue window
                        return;
                    }

                    final ArrayList<TeamDBEntry> mTeams = new ArrayList<> (teamList);
                    Log.v(TAG, "createTeam: " + mTeams.toString());
                    boolean replaceFlag = false;
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
                        for(String newPlayer: teamDBEntry.getP()) {
                            if(newPlayer.isEmpty()) continue;
                            for (String pInDB : dbEntry.getP()) {
                                if(pInDB.isEmpty()) continue;
                                if(newPlayer.equals(pInDB)) {
                                    Toast.makeText(TournaSettings.this, "Player name '" + pInDB + "' already exists!",
                                            Toast.LENGTH_LONG).show();
                                    mDialogueDone = true; //stop the background loop waiting to dismiss the dialogue window
                                    return;
                                }
                            }
                        }
                    }
                    if(!replaceFlag) {
                        mTeams.add(teamDBEntry);
                        dbRef.setValue(mTeams);

                        mCommon.addHistory(
                                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                                        .child(Constants.TOURNA).child(mTourna),
                                String.format(Locale.getDefault(),"NEW %s/%s/%s/%s",
                                        Constants.TOURNA, mTourna, Constants.TEAMS, tName));

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

                                mCommon.addHistory(
                                        FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                                                .child(Constants.TOURNA).child(mTourna),
                                        String.format(Locale.getDefault(),"NEW %s/%s/%s/%s",
                                                Constants.TOURNA, mTourna, Constants.TEAMS, tName));

                                Toast.makeText(TournaSettings.this, "Team '" + tName +"' created.",
                                        Toast.LENGTH_LONG).show();
                                mDialogueDismiss = true; //notify background loop to dismiss the dialogue window
                            }
                        });
                        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mDialogueDone = true; //stop the background loop waiting to dismiss the dialogue window
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
        public Button enter, cancel, auto;
        private Spinner mSpinner_T1;
        private Spinner mSpinner_T2;
        private SharedData mCommon;
        private Integer mNumOfMatches;
        private Integer mNextKey;
        private String mTourna;
        private String AUTO_GEN = "auto-generate";

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
            TextView titleTV = findViewById(R.id.title_tv);
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

            auto = findViewById(R.id.auto_button);
            auto.setOnClickListener(this);

            mCommon.wakeUpDBConnection_profile();

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
                    if(!team1.isEmpty() && !team2.isEmpty()) {
                        readDB(team1, team2);
                    }
                    break;
                case R.id.auto_button:
                    team1 = AUTO_GEN;
                    readDB(team1, team2);
                    break;
                default:
                    break;
            }
            dismiss();
        }

        private void readDB(final String team1, final String team2) {
            if(mTourna.isEmpty()) return;
            final DatabaseReference dbRef = mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
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
                        EditText et_desc = findViewById(R.id.et_newMatch);
                        createMatch(team1, team2, mNextKey, mNumOfMatches,
                                et_desc.getText().toString(), false);
                    }
                    else {
                        mCommon.showToast(parentActivity, "Number of matches not configured!", Toast.LENGTH_SHORT);
                        finish();
                    }
                }


                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    mCommon.showToast(parentActivity, "DB error on read: " + databaseError.getMessage(), Toast.LENGTH_SHORT);
                }
            });
        }

        private void autoGenerate(Integer numMatches) {
            Log.v(TAG, "autoGenerate: numMatches=" + numMatches);
            if(numMatches <= 0) {
                return;
            }
            if(null==mCommon.mTeams || mCommon.mTeams.size() < 2) {
                return;
            }

            Integer nextKey = 1;
            //List<String> teamList = new ArrayList<>(mCommon.mTeams);

            StringBuilder matchesStr = new StringBuilder();
            for (int i = 0; i < mCommon.mTeams.size(); i++) {
                for (int j = i+1; j < mCommon.mTeams.size(); j++) {
                    matchesStr.append("Match" + nextKey + ": " +
                            mCommon.mTeams.get(i) + " vs " + mCommon.mTeams.get(j) + "\n");
                    nextKey++;
                }
            }

            if(matchesStr.length()>0) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(parentActivity);
                alertBuilder.setTitle("Following matches to be scheduled?");
                alertBuilder.setMessage(matchesStr.toString());
                alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int idx) {
                        Integer nextKey = 1;
                        for (int i = 0; i < mCommon.mTeams.size(); i++) {
                            for (int j = i+1; j < mCommon.mTeams.size(); j++) {
                                createMatch(mCommon.mTeams.get(i), mCommon.mTeams.get(j), nextKey,
                                        mNumOfMatches, ("Match" + nextKey),
                                        false);
                                nextKey++;
                            }
                        }
                        mCommon.showToast(parentActivity,
                                String.format(Locale.getDefault(),"%d matches scheduled in '%s' tournament", nextKey-1, mTourna),
                                Toast.LENGTH_LONG);
                    }
                });
                alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
                alertBuilder.show();
            }
        }

        private void createMatch(final String team1, final String team2, Integer nextKey,
                                 Integer numMatches, final String desc, final Boolean toast) {
            Log.v(TAG, "createMatch: " + team1 + ":" + team2 + " nextkey=" + nextKey.toString() + " numMatches=" + numMatches);
            if(team1.equals(AUTO_GEN)) {
                autoGenerate(numMatches);
                return;
            }
            if(team1.equals(team2)) {
                mCommon.showToast(parentActivity, "Bad input! Select different teams.", Toast.LENGTH_LONG);
                return;
            }
            final DatabaseReference dbRef = mDatabase.child(mCommon.mClub).child(Constants.TOURNA)
                    .child(mTourna).child(Constants.MATCHES).child(Constants.META).child(nextKey.toString());
            MatchInfo mInfo = new MatchInfo();
            mInfo.T1 = team1;
            mInfo.T2 = team2;
            mInfo.desc = desc;
            mInfo.done = false;
            mInfo.key = nextKey.toString();
            dbRef.child(Constants.INFO).setValue(mInfo);
            Log.v(TAG, "createMatch: mInfo set:" + mInfo.toString());

            for(int i=0; i<numMatches; i++) {
                String matchNum = String.format(Locale.getDefault(),"%s%d",
                        Constants.MATCHID_PREFIX, i+1);
                dbRef.child(matchNum).setValue(false);
                Log.v(TAG, "createMatch: MX set:" + matchNum);
            }

            if(toast)  mCommon.showToast(parentActivity, "Match scheduled in tournament '" +
                    mTourna + "'", Toast.LENGTH_LONG);
        }
    }
}


