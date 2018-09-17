package com.sg0.baddytally;

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
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


class TaskParams {
    int shufNumPlayers;
    ArrayList<PlayerData> goldOldPL;
    ArrayList<PlayerData> goldNewPL;
    ArrayList<PlayerData> silverOldPL;
    ArrayList<PlayerData> silverNewPL;
    ArrayList<PlayerData> fullPL;

    public TaskParams(final int shufNumPlayers,
                      final ArrayList<PlayerData> goldOldPL, final ArrayList<PlayerData> goldNewPL,
                      final ArrayList<PlayerData> silverOldPL, final ArrayList<PlayerData> silverNewPL,
                      final ArrayList<PlayerData> fullPL) {
        this.shufNumPlayers = shufNumPlayers;
        this.goldOldPL = goldOldPL;
        this.goldNewPL = goldNewPL;
        this.silverOldPL = silverOldPL;
        this.silverNewPL = silverNewPL;
        this.fullPL = fullPL;
    }
}

public class Settings extends AppCompatActivity {
    private static final String TAG = "Settings";
    private Spinner mDelSpinner;
    private List<String> mPlayerList;
    private DatabaseReference mDatabase;
    private SharedData mCommon;
    private ArrayAdapter<String> mPlayerListAdapter;
    private String mNewInningsName;
    private Map<String, ArrayList<PlayerData>> mAllPlayers;
    private PlayerData hwPD;  //highest win% Player Data
    private PlayerData lwPD;  //lowest win% Player Data
    private String mWinPercInfo; //Info on Win % shuffling rule application
    private int mTimeNeededInMs;
    private boolean mDBConnected;
    private boolean mDBLock;

    private void killActivity(){
        finish();
    }

    private void enableDisableView(View view, boolean enabled) {
        view.setEnabled(enabled);
        //view.setClickable(false);
        view.setAlpha(.5f);  //making it semi-transparent
        Log.w(TAG, "enableDisableView called..." + view.getId());
        //now do the same for all the children views.
        if ( view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for ( int idx = 0 ; idx < group.getChildCount() ; idx++ ) {
                enableDisableView(group.getChildAt(idx), enabled);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        killActivity();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mTimeNeededInMs = 0;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mCommon = SharedData.getInstance();
        mWinPercInfo = "";
        Log.w(TAG, "onCreate :" + mCommon.toString());

        if (!Constants.ROOT.equals(mCommon.mRole)) {
            //non root user
            Snackbar.make(findViewById(R.id.settings_ll), "Some options might not be available to you!",
                    Snackbar.LENGTH_LONG).setAction("Action", null).show();
            LinearLayout ll = findViewById(R.id.enter_ll);
            enableDisableView(ll, false);
            //ll.setEnabled(false);
            ll = findViewById(R.id.delete_ll);
            enableDisableView(ll, false);
            ll = findViewById(R.id.newinnings_ll);
            enableDisableView(ll, false);
            Log.w(TAG, "onCreate : LL is disabled");

            findViewById(R.id.history_btn).setVisibility(View.GONE);

        } else {
            //root user
            ((EditText) findViewById(R.id.newuser)).setText("");
            ((EditText) findViewById(R.id.newuser)).setHint("new user name");
            Button newuserAddBtn = findViewById(R.id.enter_button);
            newuserAddBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickNewUser();
                } //onClick
            });
            ((RadioButton) findViewById(R.id.nu_gamegroup_silver)).setChecked(true);

            mDelSpinner = findViewById(R.id.del_spinner);
            RadioGroup mDelRadioGroup = findViewById(R.id.del_gamegroup_radiogroup);
            mDelRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    // checkedId is the RadioButton selected
                    fetchDataAndUpdateSpinner();
                }
            });
            Button deleteBtn = findViewById(R.id.del_button);
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickDelete();
                } //onClick
            });

            Button createNewInningsBtn = findViewById(R.id.createNewInnings_btn);
            createNewInningsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickCreateNewInnings();
                } //onClick
            });


            Button history_btn = findViewById(R.id.history_btn);
            history_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.INTERNALS).child(Constants.HISTORY);
                    dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            String history = "";
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                String storyLine = child.getValue(String.class);
                                if(null==storyLine) continue;
                                String parsed = mCommon.parseHistory(storyLine);
                                if (!parsed.isEmpty()) history += parsed + "\n";
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
                            builder.setMessage(mCommon.getSizeString(history, 0.7f))
                                    .setTitle(mCommon.getTitleStr("History:", Settings.this))
                                    .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            return;
                                        }
                                    }).show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                } //onClick
            });

            if (!mCommon.mClub.isEmpty()) {
                //If already signed in for a club, dont show this.
                Log.w(TAG, "createNewClub_btn.setOnClickListener: NOT empty club name");

            }
        } //root user else

        Switch clearcache_sw = findViewById(R.id.clearcache_sw);
        clearcache_sw.setChecked(false);
        clearcache_sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean bChecked) {
                if (bChecked) {
                    SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.commit();
                    mCommon.clear();
                    mCommon.setDBUpdated(true); //notify Main to refresh view
                    Toast.makeText(Settings.this, "Cache cleared!", Toast.LENGTH_SHORT)
                            .show();

                    //Restart the app: Needed to re-invoke Application.onCreate() to disable DB persistence,
                    //though that behavior is very inconsistent. See comments in ScoreTally.java.
                    setResult(Constants.RESTARTAPP);
                    finish();
                }
            }
        });

        Button createNewClub_btn = findViewById(R.id.createNewClub_btn);
        createNewClub_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
                builder.setMessage("You are about to send a request to ScoreTally team to create a new club login for you.\n" +
                        "ScoreTally team will get back to you after setting up your account.\n\n" +
                        "You will now be directed to your favourite email client. Please fill in the template details before sending the email.")
                        .setTitle(mCommon.getTitleStr("Create new club:", Settings.this))
                        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sendEmail();
                            }
                        }).show();
            } //onClick
        });

        if (!mCommon.mClub.isEmpty()) {
            //If already signed in for a club, dont show this.
            Log.w(TAG, "createNewClub_btn.setOnClickListener: NOT empty club name");
            createNewClub_btn.setVisibility(View.GONE);
        }

        FloatingActionButton fab = findViewById(R.id.fab_return);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                killActivity();
            }
        });

    }

    private void sendEmail() {
        Intent email = new Intent(Intent.ACTION_SEND);
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"reach.sgo@gmail.com"});
        email.putExtra(Intent.EXTRA_SUBJECT, "ScoreTally: Request to create new club");
        email.putExtra(Intent.EXTRA_TEXT, "Please fill in the below template and send the email. ScoreTally team will get back to you after setting up your account.\n" +
                "\nMy Contact Info:\n" +
                "        <name>\n        <phone>\n" +
                "\n\nNew Club Info:\n" +
                "        name : <name>\n" +
                "        max players : <N>\n" +
                "        frequency of game days : <times per week/month>\n" +
                "\n\nNotes: <any queries/comments/suggestions>\n" +
                "\n\nCheers,\n" +
                "<yours truly>\n\n"
        );
        email.setType("message/rfc822");
        startActivity(Intent.createChooser(email, "Choose an Email client :"));
    }
    private void onClickNewUser(){
        String tmpName =  ((EditText)findViewById(R.id.newuser)).getText().toString();
        if(TextUtils.isEmpty(tmpName)) {
            Log.w(TAG, "newuserAddBtn.setOnClickListener: empty user name");
            return;
        }
        final String name = tmpName.toUpperCase().charAt(0) + tmpName.substring(1,tmpName.length());
        int selectedId = ((RadioGroup)findViewById(R.id.nu_gamegroup_radiogroup)).getCheckedRadioButtonId();
        Log.w(TAG, "onClickNewUser:" + name + "selct:" + selectedId);
        if (selectedId < 0) {
            Log.w(TAG, "newuserAddBtn.setOnClickListener: button not selected");
            return;
        }
        final String group = ((RadioButton)findViewById(selectedId)).getText().toString();
        Log.i(TAG, "newuserAddBtn.setOnClickListener:" + name + ":" + group);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        createNewUser(name, group, null, true);  //no season points to carry over for a new user
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
        builder.setTitle(mCommon.getTitleStr("Creating New Player", Settings.this));
        builder.setMessage("You are about to create a new user:\n\n" +
                name + " to \"" + group + "\" group.\n\nAre you sure?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void createNewUser(final String name, final String group, final PointsDBEntry seasonPts, final boolean notify) {
        final String club = mCommon.mClub;
        final String innings = mCommon.mInnings;

        DatabaseReference dbRef = mDatabase.child(club).child(Constants.GROUPS).child(group).child(name);
        final List<PointsDBEntry> points =  new ArrayList<>();
        if(null==seasonPts)  //if nothing to be carried over, set all 0s
            points.add(Constants.SEASON_IDX, new PointsDBEntry());
        else //carry over season points.
            points.add(Constants.SEASON_IDX, seasonPts);
        points.add(Constants.INNINGS_IDX, new PointsDBEntry());

        dbRef.setValue(points, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    mCommon.showToast(Settings.this, "New user data (GROUPS/group) could not be saved:" + databaseError.getMessage(),
                            Toast.LENGTH_LONG);
                } else {
                    if(notify)
                        mCommon.showToast(Settings.this, "New user " + name + " created in \"" + group +
                                    "\" group of " + club,
                            Toast.LENGTH_SHORT);
                    mCommon.setDBUpdated(true);
                    mCommon.addNewUserCreation2History(Settings.this, group + "/" + name);
                    ((EditText)findViewById(R.id.newuser)).setText("");
                    ((EditText)findViewById(R.id.newuser)).setHint("new user name");
                }
            }
        });

         //hide keyboard
            if(getCurrentFocus()!=null) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if(null!=inputMethodManager)
                    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

        if(notify && mCommon.isUserNotifyEnabled())
            Snackbar.make(findViewById(R.id.settings_ll), name + " is being added to " + club + "/" + group + "/" + innings,
                    Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    private void fetchDataAndUpdateSpinner() {
        mPlayerList = null;
        mPlayerList = new ArrayList<>();
        final String club = mCommon.mClub;
        int selectedId = ((RadioGroup) findViewById(R.id.del_gamegroup_radiogroup)).getCheckedRadioButtonId();
        final String group = ((RadioButton) findViewById(selectedId)).getText().toString();
        Log.w(TAG, "fetchDataAndUpdateSpinner:" + group);
        DatabaseReference dbRef = mDatabase.child(club).child(Constants.GROUPS).child(group);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    mPlayerList.add(child.getKey());
                    Log.w(TAG, "fetchDataAndUpdateSpinner, added:" + child.getKey());
                }
                updateSpinner();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
    private void updateSpinner() {
        mPlayerListAdapter = null;
        mPlayerListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, mPlayerList);
        mPlayerListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDelSpinner.setAdapter(mPlayerListAdapter);
        mDelSpinner.performClick();

        //Set the text color of the Spinner's selected view (not a drop down list view)
        mDelSpinner.setSelection(0, true);
        View v = mDelSpinner.getSelectedView();
        if (v!=null) ((TextView)v).setTextColor(getResources().getColor(R.color.colorWhite));

        //Set the listener for when each option is clicked.
        mDelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                //Change the selected item's text color
                ((TextView) view).setTextColor(getResources().getColor(R.color.colorWhite));
                Log.v(TAG, "updateSpinner, mDelSpinner:onItemSelected:" + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
    }

    private void onClickDelete(){
        if(null==mDelSpinner.getSelectedItem()) return;
        final String name = mDelSpinner.getSelectedItem().toString();
        int selectedId = ((RadioGroup)findViewById(R.id.del_gamegroup_radiogroup)).getCheckedRadioButtonId();
        final String group = ((RadioButton)findViewById(selectedId)).getText().toString();
        Log.v(TAG, "newuserAddBtn.setOnClickListener:" + name + ":" + group);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        deleteUser(name, group, true);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
        builder.setTitle(mCommon.getTitleStr("Deleting a Player", Settings.this));
        builder.setMessage("You are about to delete a player:\n\n" +
                name + " from \"" + group + "\" group.\n\nAre you sure?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void deleteUser(final String name, final String group, final boolean notify) {
        final String club = mCommon.mClub;
        final String innings = mCommon.mInnings;
        //Create user with 0 overall score.
        DatabaseReference dbRef = mDatabase.child(club).child(Constants.GROUPS).child(group).child(name);
        dbRef.setValue(null, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    mCommon.showToast(Settings.this, "DB (delete user: GROUPS/group) error: " + databaseError.getMessage(),
                            Toast.LENGTH_LONG);
                } else {
                    if(notify)
                        mCommon.showToast(Settings.this, name + " deleted from \"" + group +
                                    "\" group of " + club, Toast.LENGTH_SHORT);
                    if(null!=mPlayerList)  mPlayerList.remove(name);
                    if(null!=mPlayerListAdapter) mPlayerListAdapter.notifyDataSetChanged();
                    mCommon.setDBUpdated(true);
                    mCommon.addUserDeletion2History(Settings.this, group + "/" + name);
                }
            }
        });

        if(notify && mCommon.isUserNotifyEnabled())
                Snackbar.make(findViewById(R.id.settings_ll), name + " is being deleted from " + club + "/" + group + "/" + innings,
                    Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    // 1. if there is no innings before, create a new one.
    // 2. create new entry under club/innings/ with {1:true, 2:""}
    // 3. Dry run: apply shuffling rule1: top 3 silver players swapped to bottom 3 gold players
    // 4. Dry run: apply shuffling rule2: highest win% player (if in silver) to be moved to gold
    // 5. Wait for user confirmation if the shuffling rules applied make sense
    // 6. Do the player swaps in DB
    // 1. set current innings entry under club/innings/<current> to {1:false, 2:xxx}
    // 2. create new entry under club/innings/ with {1:true, 2:""}
    private void onClickCreateNewInnings() {
        String tmpName = ((EditText) findViewById(R.id.newinnings)).getText().toString();
        if (TextUtils.isEmpty(tmpName)) {
            Log.w(TAG, "onClickCreateNewInnings: empty innings name");
            return;
        }
        mNewInningsName = tmpName.toUpperCase().charAt(0) + tmpName.substring(1, tmpName.length());

        if (mCommon.mInnings.isEmpty()) {
            // There are no innings created yet or there are no "current" innings. Just create the first innings with current=true.
            // No need of any shuffling. : createNewInnings

            //SGO: Using runTransaction as addListenerForSingleValueEvent gets stale data from local cache,
            //even without persistence enabled.
            final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.INNINGS);
            inningsDBRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    GenericTypeIndicator<List<InningsDBEntry>> t = new GenericTypeIndicator<List<InningsDBEntry>>() {
                    };
                    List<InningsDBEntry> innings = mutableData.getValue(t);

                    //From firebase docs: Note: Because doTransaction() is called multiple times, it must be able to handle null data.
                    //Even if there is existing data in your remote database, it may not be locally cached when the
                    // transaction function is run, resulting in null for the initial value.
                    if(null == innings) {
                        //no innings in DB

                        //java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
                        //Toast.makeText(Settings.this, "Found no existing innings already in DB", Toast.LENGTH_SHORT).show();

                        Log.w(TAG, "No existing innings in DB");
                        innings = new ArrayList<>();
                        innings.add(new InningsDBEntry(mNewInningsName, true, ""));
                        mutableData.setValue(innings);
                        Log.i(TAG, "First innings created: " + mNewInningsName);
                        return Transaction.success(mutableData);
                    }

                    /* This might get called twice! But, no harm done, as the New entry will get added only once in DB.
                    09-16 21:44:28.897 12340-12453/com.sg0.baddytally W/Settings: No existing innings in DB
                    09-16 21:44:28.900 12340-12453/com.sg0.baddytally I/Settings: First innings created: New2
                    09-16 21:44:29.488 12340-12453/com.sg0.baddytally W/Settings: There are NO current innings! innings.size=1
                    09-16 21:44:29.490 12340-12453/com.sg0.baddytally I/Settings: New innings added: New2
                     */
                    //There is innings table, but may be none with "current"==true
                    Log.w(TAG, "There are NO current innings! innings.size=" + innings.size());
                    innings.add(new InningsDBEntry(mNewInningsName, true, ""));
                    mutableData.setValue(innings);
                    //postInningsCreate(); canot be called from here. "java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare("
                    Log.i(TAG, "New innings added: " + mNewInningsName);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                    Log.i(TAG, "onClickCreateNewInnings, runTransaction, onComplete: " + mNewInningsName);
                    if (error != null || !committed || dataSnapshot == null) {
                        if(error != null) Log.w(TAG, "onClickCreateNewInnings: onComplete: Failed:", error.toException());
                        else Log.w(TAG, "getDBLock: onComplete: Failed:");
                        return;
                    } else {
                        Log.w(TAG, "onClickCreateNewInnings: onComplete: Success:");
                        //handle data here
                    }
                    createUnlockedDBLock();
                    mCommon.setDBUpdated(true); //notify Main to refresh view
                    mTimeNeededInMs = 2000;
                    //postInningsCreate();   //SGO: You can create a new thread here, but not in doTransaction
                    finish();
                }
            });
            /*
            roundQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    GenericTypeIndicator<List<InningsDBEntry>> t = new GenericTypeIndicator<List<InningsDBEntry>>() {
                    };
                    List<InningsDBEntry> innings = dataSnapshot.getValue(t);
                    if (null != innings && innings.size() > 0) {
                        Toast.makeText(Settings.this, "Found " + innings.size() + " innings already in DB", Toast.LENGTH_SHORT)
                                .show();
                        Log.w(TAG, "innings already in DB:" + innings.toString());
                        userConfirmation1();
                        return;
                    }

                    //No innings found in DB, create a new one.
                    innings = new ArrayList<>();
                    innings.add(new InningsDBEntry(mNewInningsName, true, ""));
                    inningsDBRef.setValue(innings);

                    Log.i(TAG, "First innings created: " + mNewInningsName);
                    postInningsCreate();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w(TAG, "onClickCreateNewInnings: onCancelled", databaseError.toException());
                    Toast.makeText(Settings.this, "Innings DB error on read: " + databaseError.toString(), Toast.LENGTH_SHORT)
                            .show();
                }
            }); */
        } else {  //innings already exists
            Log.w(TAG, "innings exists:" + mCommon.mInnings.toString());
            if(mNewInningsName.equals(mCommon.mInnings)) {
                Log.w(TAG, "onClickCreateNewInnings: same innings name:" + mNewInningsName);
                Toast.makeText(Settings.this, "Current innings name is already set to " + mNewInningsName, Toast.LENGTH_LONG)
                        .show();
            }
            else userConfirmation1();
        }

    }

    private void userConfirmation1 () {

        //hide keyboard
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if(null!=inputMethodManager)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        getNumOfPlayersToShuffle();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
        builder.setMessage("You are about to create a new Innings.\n" +
                "Gold & Silver player lists will be updated accordingly.\n\n" +
                "Are you sure?")
                .setTitle(mCommon.getColorString("Warning", Color.RED))
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void getNumOfPlayersToShuffle() {

        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);

        final Spinner shuffleNumSpinner = new Spinner(Settings.this);
        List<Integer> spinnerArray = new ArrayList<>();
        spinnerArray.add(0);
        spinnerArray.add(1);
        spinnerArray.add(2);
        spinnerArray.add(3);
        spinnerArray.add(4);
        spinnerArray.add(5);
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(
                Settings.this,
                android.R.layout.simple_spinner_item,
                spinnerArray
        );
        shuffleNumSpinner.setAdapter(adapter);
        shuffleNumSpinner.setSelection(3);
        // TODO: spinner text shows up to the left of the popup. Make in centre gravity somehow.

        builder.setTitle(mCommon.getTitleStr("Enter the number of players to be shuffled:", Settings.this));
        //input.setSelection(input.getText().length());  //move cursor to end
        builder.setView(shuffleNumSpinner);
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Integer shufNumPlayers = (Integer) shuffleNumSpinner.getSelectedItem();
                if(shufNumPlayers==0) {
                    //if no shuffling to be done, just create the new innings.
                    createNewInnings();
                    Toast.makeText(Settings.this, "Created new innings, no shuffling to be performed!", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                identifyPlayersToShuffle(shufNumPlayers);
            }
        });
        Dialog d = builder.show();

    }
    private void identifyPlayersToShuffle(final int shufNumPlayers) {
        if (mCommon.mInnings.isEmpty()) return;
        mTimeNeededInMs = 0;
        mAllPlayers  = new HashMap<String, ArrayList<PlayerData>>() {};
        //Retrieve latest list from DB
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub)
                .child(Constants.GROUPS);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String group = child.getKey();
                    ArrayList<PlayerData> pList = new ArrayList<>();
                    String logStr = "[" + group + "]";
                    Log.w(TAG, "identifyPlayersToShuffle group:" + logStr);
                    GenericTypeIndicator<Map<String, List<PointsDBEntry>>> genericTypeIndicator = new GenericTypeIndicator<Map<String, List<PointsDBEntry>>>() {
                    };
                    Map<String, List<PointsDBEntry>> map = child.getValue(genericTypeIndicator);
                    if (null == map) return;
                    for (Map.Entry<String, List<PointsDBEntry>> entry : map.entrySet()) {
                        //PlayerDBEntry player = child.getValue(PlayerDBEntry.class);
                        String name = entry.getKey();
                        List<PointsDBEntry> points = entry.getValue();
                        PointsDBEntry sPts = points.get(Constants.SEASON_IDX);
                        PointsDBEntry iPts = points.get(Constants.INNINGS_IDX);
                        Log.w(TAG, logStr + " name=" + name + " iPts (" + iPts.toString() + ") sPts=" + sPts.toString());
                        pList.add(new PlayerData(group, name, points));
                    }
                    if(Constants.SILVER.equals(group))
                        mCommon.sortPlayers(pList, Constants.INNINGS_IDX,false, Settings.this, true);  //sort ascending for silver (last 3 to be shuffled)
                    else
                        mCommon.sortPlayers(pList,Constants.INNINGS_IDX, true, Settings.this, true);  //sort descending for gold (last 3 to be shuffled)
                    Log.w(TAG, logStr + " pList =" + pList.toString());
                    mAllPlayers.put(group, pList);
                }


                //TODO: Make group names configurable
                Map.Entry<String, ArrayList<PlayerData>> entry = mAllPlayers.entrySet().iterator().next();
                final ArrayList<PlayerData> silverNewPL = new ArrayList<> (mAllPlayers.get(Constants.SILVER));
                final ArrayList<PlayerData> goldNewPL = new ArrayList<> (mAllPlayers.get(Constants.GOLD));
                Log.w(TAG, "identifyPlayersToShuffle: Silver players:" + silverNewPL.toString());
                Log.w(TAG, "identifyPlayersToShuffle: Gold players:" + goldNewPL.toString());

                if(silverNewPL.size()<shufNumPlayers || goldNewPL.size()<shufNumPlayers) {
                    Toast.makeText(Settings.this, "Not enough players to shuffle: " + silverNewPL.size() + "," + goldNewPL.size(),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                final ArrayList<PlayerData> goldOldPL = mAllPlayers.get(Constants.GOLD);
                final ArrayList<PlayerData> silverOldPL = mAllPlayers.get(Constants.SILVER);

                hwPD = null;
                lwPD = null;
                //Note that xxNewPL (new Player Lists) are used as input during the dry run so that the new lists can be marked.
                String gTrace = shufflePlayers(shufNumPlayers, Constants.GOLD, silverOldPL, goldNewPL, true); //dry_run
                String sTrace = shufflePlayers(shufNumPlayers, Constants.SILVER, goldOldPL, silverNewPL,   true); //dry_run
                final ArrayList<PlayerData> fullPL = createWinPercentageList(silverNewPL, goldNewPL);
                String winPercTrace = shuffleThePlayerWithHighestWinPercentage(fullPL, true);  //post-Shuffle Action dry_run

                Log.w(TAG, "AfterShuffle-rule1: Silver players:" + silverNewPL.toString());
                Log.w(TAG, "AfterShuffle-rule1: Gold players:" + goldNewPL.toString());
                Log.w(TAG, "AfterShuffle-rule1: " + sTrace);
                Log.w(TAG, "AfterShuffle-rule1: " + gTrace);

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked, now do all the DB operations together

                                // This is the CRITICAL SECTION on this operation. All the DB operations involved to
                                // create a new innings are done together here. But, still the n/w connectivity could
                                // go down amidst the operations. That would leave the DB in a bad state!
                                // So, persistence is enabled for "root" user (who has the permission to create a new innings)
                                TaskParams tp = new TaskParams(shufNumPlayers, goldOldPL, goldNewPL, silverOldPL, silverNewPL, fullPL);
                                mDBConnected = false;
                                mDBLock = false;
                                mCommon.addShuffleStart2History(Settings.this, mNewInningsName);
                                getDBLock();
                                BackgroundTask task = new BackgroundTask(Settings.this, Settings.this);
                                mCommon.disableUserNotify(Settings.this);
                                task.execute(tp);

                                /*
                                                    resetInningsData();  //do first before mAllPlayers list becomes stale. Thus, avoiding another DB read.
                    //Remove last 3 from silver, list is in ascending order. Player with most points at the end;
                    //Add last 3 from gold, Gold list is in descending order. Player with least points at the end;
                    shufflePlayers(shufNumPlayers, Constants.SILVER, goldOldPL, silverNewPL,  false); //not a dry_run, real thing!
                    shufflePlayers(shufNumPlayers, Constants.GOLD, silverOldPL, goldNewPL,  false); //not a dry_run, real thing!
                    shuffleThePlayerWithHighestWinPercentage(fullPL, false); //real thing!
                    createNewInnings();  //this is assumed to be the last operation always.

*/

                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
                builder.setTitle(mCommon.getTitleStr("Shuffling",Settings.this));
                builder.setMessage("You are about to shuffle the below players. You cannot undo these changes." +
                        gTrace +  sTrace + winPercTrace + "\n\nAre you sure?")
                        .setTitle(mCommon.getColorString("There is no coming back!", Color.RED))
                        .setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "identifyPlayersToShuffle:onCancelled", databaseError.toException());
                Toast.makeText(Settings.this, "DB error while fetching players to shuffle: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });

    }


    private String shufflePlayers(final int shufNumPlayers, final String groupName,
                                  ArrayList<PlayerData> srcList,
                                  ArrayList<PlayerData> targetList,
                                  final boolean dry_run){
        //remove last 3 from targetList
        String header = "\n\n" + groupName.toUpperCase() + ":\n";
        String trace = "";
        for (int idx=1; idx<=shufNumPlayers; idx++) {
            if(dry_run) {
                trace += "\t\t" + targetList.get(targetList.size() - idx).getName() + " (" + targetList.get(targetList.size() - idx).getPoints_innings() + ")";
                if(Constants.SILVER.equals(groupName)) targetList.get(targetList.size() - idx).markToPromote();
                else targetList.get(targetList.size() - idx).markToRelegate();
            } else {
                deleteUser(targetList.get(targetList.size() - 1).getName(), groupName, false);  //delete from DB
                //Assume that delete to DB was successful and continue. If DB interaction failed, then refresh the main page and come back to retry
                //Toast msg is shown from delete failure code above.
                targetList.remove(targetList.size() - 1);
            }
        }

        //add last 3 from srcList to targetList
        //trace += "\n" + groupName + ": add ";
        for (int idx=1; idx<=shufNumPlayers; idx++) {
            if(dry_run) {
                //trace += srcList.get(srcList.size() - idx).getName() + " (" + srcList.get(srcList.size() - idx).getPoints_innings() + ") ";
                //if(Constants.SILVER.equals(groupName)) targetList.get(targetList.size() - idx).markToRelegate();
                //else targetList.get(targetList.size() - idx).markToPromote();
                ;
            } else {
                //create New user in DB: carry over season points from srcList
                createNewUser(srcList.get(srcList.size() - idx).getName(), groupName,
                        srcList.get(srcList.size() - idx).getPointsDBEntry_season(), false);
                //Assume that create to DB was successful and continue. If DB interaction failed, then refresh the main page and come back to retry
                //Toast msg is shown from delete failure code above.
                targetList.add(srcList.get(srcList.size() - idx));
            }
        }
        if (!trace.isEmpty()) {
            //there are things to do!
            mTimeNeededInMs += 3000;  //add to the progress bar timing; it is not done twice as trace is filled only for dry_run.
            return header + trace;
        }
        else return trace;  //nothing to do.
    }

    private ArrayList<PlayerData> createWinPercentageList(ArrayList<PlayerData> sPL, ArrayList<PlayerData> gPL) {
        ArrayList<PlayerData> fullPL = new ArrayList<>(sPL);
        fullPL.addAll(gPL);

        Collections.sort(fullPL, new Comparator<PlayerData>() {
            @Override
            public int compare(PlayerData p1, PlayerData p2) {
                return Integer.valueOf(p2.getWinPercentage_innings()).compareTo(Integer.valueOf(p1.getWinPercentage_innings()));  //descending order
                //add number of Wins & number of games too
            }
        });
        Log.w(TAG, "createWinPercentageList Full PL:" + fullPL.toString());
        return fullPL;
    }

    //It is assumed that the function is first called with dry_run and then w/o it.
    //Required data is constructed during dyr_run to be used during the actual execution.
    private String shuffleThePlayerWithHighestWinPercentage(ArrayList<PlayerData> fullPL, boolean dry_run) {
        final String funStr = TAG + " shuffle win% ";
        if (dry_run) {
            mWinPercInfo = "";
            //create a win% list with no duplicates
            LinkedHashSet<Integer> winPercSet = new LinkedHashSet();
            for (PlayerData pd : fullPL) {
                winPercSet.add(Integer.valueOf(pd.getWinPercentage_innings()));   //descending order
            }
            Log.w(funStr, winPercSet.toString());
            Integer[] winPercArray =  winPercSet.toArray(new Integer[winPercSet.size()]);  //convert to array, to iterate through the set

            //check if the Player with highest win% is in Gold group or already marked to be shuffled.
            Log.w(funStr, "Full PL:" + fullPL.toString());
            //ArrayList<PlayerData> hwpList = new ArrayList<>();
            hwPD = null;
            boolean found = false;
            Integer highestWinPerc = winPercArray[ 0 ];  //get first element; list is in descending order
            //for (Integer highestWinPerc : winPercSet.descendingSet()) {  //descending order
                Log.w(TAG, " highestWinPerc=" + highestWinPerc);
                for (PlayerData pd : fullPL) {
                    if (Integer.valueOf(pd.getGamesPlayed_innings()) < Constants.SHUFFLE_WINPERC_NUM_GAMES) {
                        Log.w(funStr, "not considered (< min games):" + pd.getName());
                        continue;
                    }
                    if (pd.getWinPercentage_innings().equals(highestWinPerc.toString())) {
                        Log.w(funStr, "high win%:" + pd.toStringShort());
                        if (Constants.GOLD.equals(pd.getGroup())) {
                            Log.w(funStr, pd.getName() + " already in GOLD");
                            mWinPercInfo = pd.getName() + "(" + pd.getWinPercentage_innings() + "%) is already in gold group.";
                            continue;
                        } else {  //highest win percentage is in silver group
                            if (pd.isMarkedToPromote()) {
                                Log.w(funStr, pd.getName() + " already marked to be promoted to GOLD");
                                mWinPercInfo = pd.getName() + "(" + pd.getWinPercentage_innings() + "%) is already getting promoted to gold group.";
                                continue;
                            } else {
                                Log.w(funStr, pd.getName() + " to be +++promoted+++ to GOLD");
                                mWinPercInfo = "";
                                hwPD = new PlayerData(pd);
                                found = true;
                                break;
                            }
                        }
                    }
                } //fullPL loop
                //if (found) break;
            //} //winPercSet loop

            if (hwPD == null) {
                if(mWinPercInfo.isEmpty())
                    mCommon.showToast(Settings.this, "Highest win% player not found in Silver group", Toast.LENGTH_LONG);
                else
                    mCommon.showToast(Settings.this, "Win% shuffling result: " + mWinPercInfo, Toast.LENGTH_LONG);
                //nothing to do; Player with highest win% is either already in Gold or is marked to be shuffled.
                return "";
            }

            Log.w(funStr, "hwpList:" + hwPD.toStringShort());

            //find the lowest win% player from Gold group who is not marked to be shuffled.
            //ArrayList<PlayerData> lwpList = new ArrayList<>();
            lwPD = null;  //lowest win% Player Data
            found = false;
            for (int i = winPercArray.length - 1; i>=0; i--) {  //ascending order
                Integer lowestWinPerc = winPercArray[i];
                Log.w(funStr, " lowestWinPerc=" + lowestWinPerc);
                for (ListIterator iterator = fullPL.listIterator(fullPL.size()); iterator.hasPrevious(); ) {  //iterate reverse
                    final PlayerData pd = (PlayerData) iterator.previous();
                    if (pd.getWinPercentage_innings().equals(lowestWinPerc.toString())) {
                        Log.w(funStr, "low win%:" + pd.toStringShort());
                        if (Constants.SILVER.equals(pd.getGroup())) {
                            Log.w(funStr, pd.getName() + " already in SILVER");
                            //But, Is he marked to be promoted?
                            if(pd.isMarkedToPromote()) {
                                Log.w(funStr, pd.getName() + " to be +++relegated +++ to SILVER, though he just got promoted");
                                lwPD = new PlayerData(pd);
                                found = true;
                                mWinPercInfo += " & " + pd.getName() + "(" + pd.getWinPercentage_innings() + "%) will be moved back to silver group.";
                                break;
                            }
                            continue;
                        } else {  //lowest win percentage is in Gold group
                            if (pd.isMarkedToRelegate()) {
                                Log.w(funStr, pd.getName() + " already marked to be relegated  to SILVER");
                                continue;
                            } else {
                                Log.w(funStr, pd.getName() + " to be +++relegated +++ to SILVER");
                                lwPD = new PlayerData(pd);
                                found = true;
                                mWinPercInfo += " & " + pd.getName() + "(" + pd.getWinPercentage_innings() + "%) will be moved to silver group.";
                                break;
                            }
                        }
                    }
                } //fullPL iterator
                if (found) break;
            } //winPercSet loop



            if (lwPD == null) {
                if(mWinPercInfo.isEmpty())
                    mCommon.showToast(Settings.this, "Lowest win% player not found in Gold group", Toast.LENGTH_LONG);  //should never happen
                else
                    mCommon.showToast(Settings.this, "Win% shuffling result: " + mWinPercInfo, Toast.LENGTH_LONG);
                //nothing to do; could not find a player with low Win % in Gold.
                return "";
            }
            Log.w(funStr, "lwpList:" + lwPD.toStringShort());

            if (hwPD.getWinPercentage_innings().equals(lwPD.getWinPercentage_innings())) {
                mCommon.showToast(Settings.this, "No win% Shuffling: Highest win% (" + hwPD.getWinPercentage_innings() +
                        "%) in Silver is equal to Lowest win% in Gold!", Toast.LENGTH_LONG);
                return "";  //Nothing more to be shuffled.
            }

            //if you reached here, you have decided to shuffle. show the shuffling results.
            if(!mWinPercInfo.isEmpty()) {
                mCommon.showToast(Settings.this, "Win% shuffling result: " + mWinPercInfo, Toast.LENGTH_LONG);
            }

        }  else { //not a dry_run
            if(hwPD==null && lwPD==null) {
                //Win% shuffling rule application did not find any changes to be done.
                return "";
            }else if(hwPD==null || lwPD==null) {
                //Win% shuffling rule application went wrong somewhere.
                mCommon.showToast(Settings.this, "Win% shuffling went wrong! ", Toast.LENGTH_SHORT);
                Log.w(funStr, "Win% shuffling went wrong!");
                return "";
            }

            if(hwPD.getWinPercentage_innings().equals(lwPD.getWinPercentage_innings())) {
                Log.w(funStr, "High Win% == Low Win%:" + hwPD.toStringShort() + " & " + lwPD.toStringShort());
                return "";
            }

            Log.w(funStr, "WIN% SHUFFLE:" + mWinPercInfo + " high%=" + hwPD.toStringShort() + " low%=" + lwPD.toStringShort());
            //Toast.makeText(Settings.this, "Win% shuffling result: " + mWinPercInfo, Toast.LENGTH_LONG)
            //        .show();

            if(!dry_run) mTimeNeededInMs += 2000;  //add to the progress bar timing
            //Move the highest win% player to Gold
            deleteUser(hwPD.getName(), Constants.SILVER, false);  //delete from Silver DB
            createNewUser(hwPD.getName(), Constants.GOLD, hwPD.getPointsDBEntry_season(), false);

            //Move the lowest win% player to Silver
            deleteUser(lwPD.getName(), Constants.GOLD, false);  //delete from Gold DB
            createNewUser(lwPD.getName(), Constants.SILVER, lwPD.getPointsDBEntry_season(), false);
        } //else for dry_run

        return "\n\n Highest Win% shuffle:\n".toUpperCase() +
                "\t\t" + lwPD.getName() + " (" + lwPD.getWinPercentage_innings() + "%)" +
                "  <-->  " + hwPD.getName() + " (" + hwPD.getWinPercentage_innings() + "%)";

    }

    private void createNewInnings(){
        final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.INNINGS);
        Query roundQuery = inningsDBRef.orderByKey();
        roundQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<List<InningsDBEntry>> t = new GenericTypeIndicator<List<InningsDBEntry>>() {
                };
                List<InningsDBEntry> innings = dataSnapshot.getValue(t);
                if(null==innings) return;
                Log.v(TAG, "createNewInnings: key:" + dataSnapshot.getKey());
                innings.get(mCommon.mInningsDBKey).current = false;
                innings.add(new InningsDBEntry(mNewInningsName, true, ""));
                //now, write back the updated list with new innings.
                inningsDBRef.setValue(innings);
                mCommon.resetForNewInnings(mNewInningsName);
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mCommon.showToast(Settings.this, "Innings DB error on write: " + databaseError.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    //reset the innings data of all players
    private void resetInningsData() {
        DatabaseReference mClubDBRef = mDatabase.child(mCommon.mClub);
        for (ArrayList<PlayerData>  pList : mAllPlayers.values()) {
            for(PlayerData playerData : pList) {
                mClubDBRef.child(Constants.GROUPS).child(playerData.getGroup())
                        .child(playerData.getName()).child(Integer.toString(Constants.INNINGS_IDX))
                        .setValue(new PointsDBEntry());
            }
        }
    }

    private void createUnlockedDBLock() {
        final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.INTERNALS).child("locked");
        inningsDBRef.setValue(false);
        Log.w(TAG, "createUnlockedDBLock: Lock created/unlocked");
    }

    private void getDBLock() {
        // Why a lock?
        //    -- main reason being Innings creation is done by only one root user.
        //    -- added advantage is that a DB online check is done before the critical operation is performed
        //    -- there are comments saying that performing a write makes sure that the DB is in sync.
        //       (but that might be only for that particular DB tree, anyways!)
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    Log.w(TAG, "connected");
                    mDBConnected = true;
                } else {
                    Log.w(TAG, "not connected");
                    mDBConnected = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Listener was cancelled");
                mDBConnected = false;
            }
        });

        final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub).child(Constants.INTERNALS).child("locked");
        inningsDBRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Boolean locked = mutableData.getValue(Boolean.class);
                if(locked == null) return Transaction.success(mutableData);
                else {
                    if(locked) {
                        Log.w(TAG, "getDBLock: Already locked");
                        return Transaction.abort();
                    } else {
                        Log.w(TAG, "getDBLock: Lock available");
                        mutableData.setValue(true);
                    }
                    return Transaction.success(mutableData);
                }

            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                if (error != null || !committed || dataSnapshot == null) {
                    if(null!=error) Log.w(TAG, "getDBLock: onComplete: Failed:", error.toException());
                    else Log.w(TAG, "getDBLock: onComplete: Failed:");
                    mDBLock = false;
                } else {
                    Log.w(TAG, "getDBLock: onComplete: Success:");
                    Boolean locked = dataSnapshot.getValue(Boolean.class);
                    mDBLock = locked;
                    //handle data here
                }
            }
        });
    }

    private void postInningsCreate() {
        //Some cleanup & final activities.
        //((EditText) findViewById(R.id.newinnings)).setText("");
        //((EditText) findViewById(R.id.newinnings)).setHint("new innings name");
        Log.w(TAG, "postInningsCreate: mTimeNeededInMs=" + mTimeNeededInMs);
        //BackgroundTask task = new BackgroundTask(Settings.this, Settings.this);
        //task.execute();

    }


    private class BackgroundTask extends AsyncTask<TaskParams, Void, String> {
        private ProgressDialog dialog;
        private Context mContext;

        public BackgroundTask(Activity activity, Context context) {
            dialog = new ProgressDialog(activity);
            this.mContext = context;

        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Shuffling in progress....");
            dialog.show();
        }

        @Override
        protected void onPostExecute(final String result) {
            Log.v(TAG, "BackgroundTask: onPostExecute");
            //Give some time for all other threads (firebase DB updates) to catch up.
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    Log.v(TAG, "BackgroundTask: onPostExecute, after 5s");
                    completeShuffling(result);
                }
            }, 5000);
        }

        private void completeShuffling(final String result){
            Log.v(TAG, "BackgroundTask: completeShuffling");
            mCommon.enableUserNotify(Settings.this);   //Toasts are allowed after this.
            if(result.isEmpty()) {
                Toast.makeText(Settings.this, "New Innings created successfully!", Toast.LENGTH_SHORT)
                        .show();
                mCommon.addShuffleSuccess2History(Settings.this, mNewInningsName);
            }
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            //mNewInningsName = "";   //This resulted in new innings to be created with empty name; not thread safe!

            if(result.isEmpty())
                killActivity();   //new innings created, go back to main page.
            else {
                Toast.makeText(Settings.this, result, Toast.LENGTH_SHORT)
                        .show();
                mCommon.addShuffleFailure2History(Settings.this, mNewInningsName);
            }
        }

        @Override
        protected String doInBackground(TaskParams... params) {
            String errStr = "";
            TaskParams tp = params[0];
            try {
                Log.w(TAG, "BackgroundTask: mTimeNeededInMs=" + mTimeNeededInMs);
                for(int i=0; i<5; i++) {
                    Thread.sleep(2000);
                    if(!mDBConnected) {
                        Log.w(TAG, "doInBackground: DB is not connected");
                        errStr =  "DB is not connected, try again later...";
                        return errStr;
                    } else Log.w(TAG, "doInBackground: DB is connected");

                    if (mDBLock) {

                        //Below routines, cant do toasts methods. Otherwise, the following error will happen:
                        //java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()

                        resetInningsData();  //do first before mAllPlayers list becomes stale. Thus, avoiding another DB read.
                        //Remove last 3 from silver, list is in ascending order. Player with most points at the end;
                        //Add last 3 from gold, Gold list is in descending order. Player with least points at the end;
                        shufflePlayers(tp.shufNumPlayers, Constants.SILVER, tp.goldOldPL, tp.silverNewPL, false); //not a dry_run, real thing!
                        shufflePlayers(tp.shufNumPlayers, Constants.GOLD, tp.silverOldPL, tp.goldNewPL, false); //not a dry_run, real thing!
                        shuffleThePlayerWithHighestWinPercentage(tp.fullPL, false); //real thing!
                        createNewInnings();  //this is assumed to be the last operation always.
                        createUnlockedDBLock();  //release the lock
                        Log.w(TAG, "doInBackground: success!");
                        return "";
                    }
                }

            } catch (InterruptedException e) {
                Log.w(TAG, "doInBackground: InterruptedException=" + e.getMessage());
                createUnlockedDBLock();
            } catch (Exception e) {
                Log.w(TAG, "doInBackground: Exception:" + e.getMessage());
                e.printStackTrace();
                createUnlockedDBLock();
            }

            Log.w(TAG, "doInBackground: failure! DB is not reachable, try again later...");
            errStr = "DB is not reachable, try again later...";
            return errStr;
        }

    }


}


