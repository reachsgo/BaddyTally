package com.sg0.baddytally;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


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

public class ClubLeagueSettings extends AppCompatActivity {
    private static final String TAG = "ClubLeagueSettings";
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
    private Handler mMainHandler;
    private Handler mImportHandler;
    // Need a new Handler Q as the main handler messages are removed from the MainHandler in the
    // below scenario:
    // checkforDuplicateName() -> starts new thread -> mMainHandler.removeCallbacksAndMessages()



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clubleague_settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mCommon = SharedData.getInstance();
        mWinPercInfo = "";
        mMainHandler = new Handler();
        mImportHandler = new Handler();
        //Log.i(TAG, "onCreate :" + mCommon.toString());
        mCommon.auditClub(ClubLeagueSettings.this); //check if club still exists in DB

        if (!mCommon.isPermitted(ClubLeagueSettings.this)) return;

        ((CheckBox) findViewById(R.id.club_datafile_cb)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //Log.d(TAG, "onCheckedChanged: " + b);
                SharedData.enableDisableView(findViewById(R.id.newuser_ll), !b);
                SharedData.enableDisableView(findViewById(R.id.nu_gamegroup_radiogroup), !b);
                if(b) {
                    Animation shake = AnimationUtils.loadAnimation(
                            ClubLeagueSettings.this, R.anim.shake);
                    findViewById(R.id.enter_button).startAnimation(shake);
                }
            }
        });

        //new user button is allowed for admin & super+
        ((EditText) findViewById(R.id.newuser)).setText("");
        ((EditText) findViewById(R.id.newuser)).setHint("new user name");
        Button newuserAddBtn = findViewById(R.id.enter_button);
        newuserAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCommon.mClub.isEmpty()) {
                    return;
                }  //not yet ready

                if (((CheckBox) findViewById(R.id.club_datafile_cb)).isChecked()) {
                    //Log.d(TAG, "Enter Btn: data to be imported from file");
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            //club league data to be updated from a file
                            if (mCommon.isExternalStorageWritable(ClubLeagueSettings.this)) {
                                importData();
                            } else {
                                Toast.makeText(ClubLeagueSettings.this,
                                        "External storage not writable!\n" +
                                                "Give 'storage' app permission and try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    return;
                }

                onClickNewUser(null, null);
            } //onClick
        });

        Button history_btn = findViewById(R.id.history_btn);
        history_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCommon.mClub.isEmpty() || !mCommon.isAdminPlus()) {
                    return;
                }  //not yet ready
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                        .child(mCommon.mClub).child(Constants.INTERNALS).child(Constants.HISTORY);
                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final StringBuilder history = new StringBuilder();
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            String storyLine = child.getValue(String.class);
                            if (null == storyLine) continue;
                            String parsed = mCommon.parseHistory(storyLine);
                            if (!parsed.isEmpty()) history.append(parsed).append("\n");
                        }

                        if(history.length()<=0) return;

                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueSettings.this);
                                builder.setMessage(mCommon.getSizeString(history.toString(), 0.7f))
                                        .setTitle(mCommon.getTitleStr("History:", ClubLeagueSettings.this))
                                        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                return;
                                            }
                                        }).show();
                            }
                        });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w(TAG, "history_btn:onCancelled", databaseError.toException());
                    }
                });
            } //onClick
        });

        if (mCommon.isMemberRole()) {
            Snackbar.make(findViewById(R.id.settings_ll), "Some options might not be available to you!",
                    Snackbar.LENGTH_LONG).setAction("Action", null).show();
            SharedData.enableDisableView(findViewById(R.id.club_datafile_ll), false);
            SharedData.enableDisableView(findViewById(R.id.enter_ll), false);
            SharedData.enableDisableView(findViewById(R.id.delete_ll), false);
            SharedData.enableDisableView(findViewById(R.id.newinnings_ll), false);
            SharedData.enableDisableView(findViewById(R.id.winPercNum_ll), false);
            SharedData.enableDisableView(findViewById(R.id.history_ll), false);
            SharedData.enableDisableView(findViewById(R.id.users_btn), false);
            SharedData.enableDisableView(findViewById(R.id.history_btn), false);
            SharedData.enableDisableView(findViewById(R.id.reset_ll), false);
            //enableDisableView(findViewById(R.id.reset_pts), false);
            //enableDisableView(findViewById(R.id.delete_all), false);
        } else if (mCommon.isAdmin()) {
            Snackbar.make(findViewById(R.id.settings_ll), "Some options might not be available to you!",
                    Snackbar.LENGTH_LONG).setAction("Action", null).show();
            SharedData.enableDisableView(findViewById(R.id.club_datafile_ll), false);
            SharedData.enableDisableView(findViewById(R.id.delete_ll), false);
            SharedData.enableDisableView(findViewById(R.id.newinnings_ll), false);
            SharedData.enableDisableView(findViewById(R.id.winPercNum_ll), false);
            //enableDisableView(findViewById(R.id.history_ll), false);
            SharedData.enableDisableView(findViewById(R.id.users_btn), false);
            //enableDisableView(findViewById(R.id.history_btn), false);
            SharedData.enableDisableView(findViewById(R.id.reset_ll), false);
            //enableDisableView(findViewById(R.id.reset_pts), false);
            //enableDisableView(findViewById(R.id.delete_all), false);
        } else {
            //root or super-user
            if (mCommon.mNumOfGroups == 1) { //only 1 group enabled
                ((RadioButton) findViewById(R.id.nu_gamegroup_gold)).setChecked(true);
                ((RadioButton) findViewById(R.id.nu_gamegroup_silver)).setEnabled(false);  //only gold group
                ((RadioButton) findViewById(R.id.del_gamegroup_gold)).setChecked(true);
                ((RadioButton) findViewById(R.id.del_gamegroup_silver)).setEnabled(false);  //only gold group
                fetchDataAndUpdateSpinner();
            } else {
                //no default selection, as we dont need this in case of 'import from file'.
                ((RadioButton) findViewById(R.id.nu_gamegroup_gold)).setChecked(false);
                ((RadioButton) findViewById(R.id.nu_gamegroup_silver)).setChecked(false);
            }

            mDelSpinner = findViewById(R.id.del_spinner);
            RadioGroup mDelRadioGroup = findViewById(R.id.del_gamegroup_radiogroup);

            mDelRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (mCommon.mClub.isEmpty()) {
                        return;
                    }  //not yet ready
                    if (!mCommon.isDBConnected()) {
                        mCommon.showToast(ClubLeagueSettings.this,
                                "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT);
                        mCommon.wakeUpDBConnectionProfile();
                        return;
                    }
                    // checkedId is the RadioButton selected
                    fetchDataAndUpdateSpinner();
                }
            });
            Button deleteBtn = findViewById(R.id.del_button);
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mCommon.mClub.isEmpty()) {
                        return;
                    }  //not yet ready
                    onClickDelete();
                } //onClick
            });

            Button createNewInningsBtn = findViewById(R.id.createNewInnings_btn);
            createNewInningsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mCommon.mClub.isEmpty()) {
                        return;
                    }  //not yet ready
                    onClickCreateNewInnings(null);
                } //onClick
            });

            activateUserButton();

        } //root user else

        String winPercNumStr = "" + Constants.SHUFFLE_WINPERC_NUM_GAMES;
        ((EditText) findViewById(R.id.winPercNum)).setText(winPercNumStr);

        FloatingActionButton fab = findViewById(R.id.fab_return);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCommon.killActivity(ClubLeagueSettings.this, RESULT_OK);
            }
        });

        findViewById(R.id.reset_pts).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetData(false);
            }
        });

        findViewById(R.id.delete_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetData(true);
            }
        });
        //hide keyboard
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (null != inputMethodManager)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMainHandler.removeCallbacksAndMessages(null);
        mImportHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Log.d(TAG, "onOptionsItemSelected: ");
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void activateUserButton() {
        Button users_btn = findViewById(R.id.users_btn);
        users_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                        .child(mCommon.mClub).child(Constants.ACTIVE_USERS);
                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final StringBuilder userList = new StringBuilder();
                        //support for releases previous to 4.0.0, where user data
                        //was ProfileDBEntry.class instead of String.
                        try {
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                if (child == null) continue;
                                String userData = child.getValue(String.class);
                                if (userData != null) {
                                    userList.append(String.format("%s - %s\n",
                                            child.getKey(), userData));
                                }
                            }
                            /*
                            ProfileDBEntry userData = child.getValue(ProfileDBEntry.class);
                            if (userData != null) {
                                userList.append(String.format("%s, %s, %.10s, %s, %s\n",
                                        child.getKey(), userData.getR(),userData.getD(), userData.getLl(), userData.getV()));
                            }*/

                            if (userList.length() == 0) return;

                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueSettings.this);
                                    builder.setMessage(mCommon.getSizeString(userList.toString(), 0.8f))
                                            .setTitle(mCommon.getTitleStr("Users:", ClubLeagueSettings.this))
                                            .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    return;
                                                }
                                            }).show();
                                }
                            });

                        } catch (com.google.firebase.database.DatabaseException e) {
                            //Old DB had ProfileDBEntry.class in DB instead of String.
                            //just ignore it.
                            Log.e(TAG, "activateUserButton: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w(TAG, "activateUserButton:onCancelled", databaseError.toException());
                    }
                });
            } //onClick
        });
    }


    private void onClickNewUser(String name, String group) {
        boolean showDiag = true;
        if(null==name || name.isEmpty()) {
            String tmpName = ((EditText) findViewById(R.id.newuser)).getText().toString().trim();
            if (TextUtils.isEmpty(tmpName) || !SharedData.isValidString(tmpName)) {
                //Log.w(TAG, "onClickNewUser: invalid user name [" + tmpName + "]");
                Toast.makeText(ClubLeagueSettings.this,
                        "Invalid player name (" + tmpName + ")!", Toast.LENGTH_SHORT).show();
                return;
            }
            //final String name = tmpName.toUpperCase().charAt(0) + tmpName.substring(1, tmpName.length());
            name = tmpName.trim();
            if (!SharedData.isValidString(name)) {
                mCommon.showToast(ClubLeagueSettings.this,
                        "No special characters in the name!", Toast.LENGTH_SHORT);
                return;
            }
        } else showDiag = false; //data being imported, cant show dialogues here as many players are added together

        if(null==group || group.isEmpty()) {
            int selectedId = ((RadioGroup) findViewById(R.id.nu_gamegroup_radiogroup)).getCheckedRadioButtonId();
            //Log.w(TAG, "onClickNewUser:" + name + "selct:" + selectedId);
            if (selectedId < 0) {
                mCommon.showToast(ClubLeagueSettings.this,
                        "Select a group", Toast.LENGTH_SHORT);
                return;
            }
            group = ((RadioButton) findViewById(selectedId)).getText().toString();
            //Log.i(TAG, "onClickNewUser:" + name + ":" + group);
        }

        final String finalName = SharedData.truncate(name, false,
                getResources().getInteger(R.integer.max_longname_len));
        final String finalGroup = group;

        if(!showDiag) {
            checkforDuplicateName(finalName, finalGroup, false);
            return; //done, dont go to dialoge code.
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        checkforDuplicateName(finalName, finalGroup, true);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueSettings.this);
        builder.setTitle(mCommon.getTitleStr("Creating New Player", ClubLeagueSettings.this));
        builder.setMessage("You are about to create a new user:\n\n" +
                finalName + " to \"" + finalGroup + "\" group.\n\nAre you sure?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void checkforDuplicateName(final String name, final String group, final boolean notify) {
        //Retrieve latest list from DB
        if (!mCommon.isDBConnected()) {
            mCommon.wakeUpDBConnectionProfile();
            mCommon.showToast(ClubLeagueSettings.this,
                    "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT);
            return;
        }

        mCommon.startProgressDialog(ClubLeagueSettings.this, "", "");
        mCommon.showToastAndDieOnTimeout(mMainHandler, ClubLeagueSettings.this,
                "Check your internet connection", true, true, 0);

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub)
                .child(Constants.GROUPS);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    if (null == child) return;
                    String local_group = child.getKey();
                    GenericTypeIndicator<Map<String, List<PointsDBEntry>>> genericTypeIndicator =
                            new GenericTypeIndicator<Map<String, List<PointsDBEntry>>>() {
                    };
                    Map<String, List<PointsDBEntry>> map = child.getValue(genericTypeIndicator);
                    if (null == map) return;
                    for (Map.Entry<String, List<PointsDBEntry>> entry : map.entrySet()) {
                        count ++;
                        //Log.d(TAG, count + ":onDataChange: looking at player=" + entry.getKey());
                        if(entry.getKey().equals(name)) {
                            //Log.w(TAG, "duplicate name [" + name + "] in " + local_group);
                            mCommon.stopProgressDialog(ClubLeagueSettings.this);
                            mMainHandler.removeCallbacksAndMessages(null);
                            mCommon.showToast(ClubLeagueSettings.this,
                                    "Name '" + name + "' is already taken! Try another name.",
                                    Toast.LENGTH_SHORT);
                            return;
                        }
                    }
                }
                createNewUser(name, group, null, notify);  //no season points to carry over for a new user
                mCommon.stopProgressDialog(ClubLeagueSettings.this);
                mMainHandler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "checkforDuplicateName:onCancelled", databaseError.toException());
                Toast.makeText(ClubLeagueSettings.this,
                        "DB error while fetching players:" + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createNewUser(final String name, final String group, final PointsDBEntry seasonPts,
                               final boolean notify) {
        final String club = mCommon.mClub;

        DatabaseReference dbRef = mDatabase.child(club).child(Constants.GROUPS).child(group).child(name);
        final List<PointsDBEntry> points = new ArrayList<>();
        if (null == seasonPts)  //if nothing to be carried over, set all 0s
            points.add(Constants.SEASON_IDX, new PointsDBEntry());
        else //carry over season points.
            points.add(Constants.SEASON_IDX, seasonPts);
        points.add(Constants.INNINGS_IDX, new PointsDBEntry());

        dbRef.setValue(points, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    mCommon.showToast(ClubLeagueSettings.this, "New user data (GROUPS/group) could not be saved:" + databaseError.getMessage(),
                            Toast.LENGTH_LONG);
                } else {
                    if (notify)
                        mCommon.showToast(ClubLeagueSettings.this, "New user " + name + " created in \"" + group +
                                        "\" group of " + club,
                                Toast.LENGTH_SHORT);
                    mCommon.setDBUpdated(true);
                    mCommon.addNewUserCreation2History(group + "/" + name);
                    ((EditText) findViewById(R.id.newuser)).setText("");
                    ((EditText) findViewById(R.id.newuser)).setHint("new user name");
                }
            }
        });

        //hide keyboard
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (null != inputMethodManager)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        //if(notify && mCommon.isUserNotifyEnabled())
        //    Snackbar.make(findViewById(R.id.settings_ll), name + " is being added to " + club + "/" + group + "/" + innings,
        //            Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    private void fetchDataAndUpdateSpinner() {
        mPlayerList = null;
        mPlayerList = new ArrayList<>();
        final String club = mCommon.mClub;
        int selectedId = ((RadioGroup) findViewById(R.id.del_gamegroup_radiogroup)).getCheckedRadioButtonId();
        final String group = ((RadioButton) findViewById(selectedId)).getText().toString();
        //Log.v(TAG, "fetchDataAndUpdateSpinner:" + group);
        DatabaseReference dbRef = mDatabase.child(club).child(Constants.GROUPS).child(group);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    mPlayerList.add(child.getKey());
                    //Log.w(TAG, "fetchDataAndUpdateSpinner, added:" + child.getKey());
                }
                updateSpinner();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "fetchDataAndUpdateSpinner:onCancelled", databaseError.toException());
            }
        });
    }

    private void updateSpinner() {
        mPlayerListAdapter = null;
        mPlayerListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, mPlayerList);
        mPlayerListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDelSpinner.setAdapter(mPlayerListAdapter);
        if (mCommon.mNumOfGroups > 1) {
            //if just one group, dont show the drop down menu at the start of the activity
            mDelSpinner.performClick();
        }

        //Set the text color of the Spinner's selected view (not a drop down list view)
        mDelSpinner.setSelection(0, true);
        View v = mDelSpinner.getSelectedView();
        if (v != null) ((TextView) v).setTextColor(getResources().getColor(R.color.colorWhite));

        //Set the listener for when each option is clicked.
        mDelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Change the selected item's text color
                ((TextView) view).setTextColor(getResources().getColor(R.color.colorWhite));
                //Log.v(TAG, "updateSpinner, mDelSpinner:onItemSelected:" + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        //Log.v(TAG, "updateSpinner... done");
    }

    private void onClickDelete() {
        if (null == mDelSpinner.getSelectedItem()) return;
        final String name = mDelSpinner.getSelectedItem().toString();
        int selectedId = ((RadioGroup) findViewById(R.id.del_gamegroup_radiogroup)).getCheckedRadioButtonId();
        final String group = ((RadioButton) findViewById(selectedId)).getText().toString();
        //Log.v(TAG, "onClickDelete:" + name + ":" + group);

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

        AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueSettings.this);
        builder.setTitle(mCommon.getTitleStr("Deleting a Player", ClubLeagueSettings.this));
        builder.setMessage("You are about to delete a player:\n\n" +
                name + " from \"" + group + "\" group.\n\nAre you sure?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void deleteUser(final String name, final String group, final boolean notify) {
        if (!mCommon.isDBConnected()) {
            mCommon.showToast(ClubLeagueSettings.this,
                    "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT);
            return;
        }
        final String club = mCommon.mClub;
        //Create user with 0 overall score.
        DatabaseReference dbRef = mDatabase.child(club).child(Constants.GROUPS).child(group).child(name);
        dbRef.setValue(null, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    mCommon.showToast(ClubLeagueSettings.this, "DB (delete user: GROUPS/group) error: " + databaseError.getMessage(),
                            Toast.LENGTH_LONG);
                } else {
                    if (notify)
                        mCommon.showToast(ClubLeagueSettings.this, name + " deleted from \"" + group +
                                "\" group of " + club, Toast.LENGTH_SHORT);
                    if (null != mPlayerList) mPlayerList.remove(name);
                    if (null != mPlayerListAdapter) mPlayerListAdapter.notifyDataSetChanged();
                    mCommon.setDBUpdated(true);
                    mCommon.addUserDeletion2History(group + "/" + name);
                }
            }
        });

        //if(notify && mCommon.isUserNotifyEnabled())
        //        Snackbar.make(findViewById(R.id.settings_ll), name + " is being deleted from " + club + "/" + group + "/" + innings,
        //            Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    // 1. if there is no innings before, create a new one & return.
    // 2. Dry run: apply shuffling rule1: top 3 silver players swapped to bottom 3 gold players
    // 3. Dry run: apply shuffling rule2: highest win% player (if in silver) to be moved to gold
    // 4. Wait for user confirmation if the shuffling rules applied make sense
    // 5. Do the player swaps in DB (in a background task, after making sure that there is a DB connection)
    // 6. set current innings entry under club/innings/<current> to {1:false, 2:xxx}
    // 7. create new entry under club/innings/ with {1:true, 2:""}
    private void onClickCreateNewInnings(final String innings) {
        if(null==innings || innings.isEmpty()) {
            String tmpName = ((EditText) findViewById(R.id.newinnings)).getText().toString().trim();
            if (TextUtils.isEmpty(tmpName) || !SharedData.isValidString(tmpName)) {
                //Log.w(TAG, "onClickCreateNewInnings: invalid innings name [" + tmpName + "]");
                Toast.makeText(ClubLeagueSettings.this,
                        "Invalid Innings (" + tmpName + ")!", Toast.LENGTH_SHORT).show();
                return;
            }
            mNewInningsName = tmpName.trim();
        } else mNewInningsName = innings;

        mNewInningsName = SharedData.truncate(mNewInningsName.trim(), false,
                getResources().getInteger(R.integer.max_short_id_len)).trim();

        mCommon.mWinPercNum = Constants.SHUFFLE_WINPERC_NUM_GAMES;
        boolean dataError = false;
        try {
            Integer val = Integer.valueOf(((EditText) findViewById(R.id.winPercNum)).getText().toString());
            if (val > 0 && val < 100) SharedData.getInstance().mWinPercNum = val;
            else dataError = true;
        } catch (NumberFormatException e) {
            dataError = true;
        }
        if (dataError)
            Toast.makeText(ClubLeagueSettings.this,
                    "Bad entry for win% qualification number! Will use default:" +
                            Constants.SHUFFLE_WINPERC_NUM_GAMES, Toast.LENGTH_LONG).show();

        Log.i(TAG, mNewInningsName + ":onClickCreateNewInnings: mWinPercNum=" +
                mCommon.mWinPercNum);

        //If there is no lock parameter in DB, create it now.
        mCommon.createDBLock(null);

        if (mCommon.mInnings.isEmpty()) {
            if (!mCommon.isDBConnected()) {
                mCommon.showToast(ClubLeagueSettings.this,
                        "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT);
                return;
            }

            // There are no innings created yet or there are no "current" innings. Just create the first innings with current=true.
            // No need of any shuffling. : createNewInnings

            final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                    .child(mCommon.mClub).child(Constants.INNINGS);
            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    GenericTypeIndicator<List<InningsDBEntry>> t = new GenericTypeIndicator<List<InningsDBEntry>>() {
                    };
                    List<InningsDBEntry> innings = dataSnapshot.getValue(t);
                    if (innings == null) {
                        //SGO: this is hit when there is no innings table in the DB.
                        Log.e(TAG, "onClickCreateNewInnings, innings==null");
                        innings = new ArrayList<>(1);
                        innings.add(new InningsDBEntry(mNewInningsName, true, ""));
                        dbRef.setValue(innings);
                    } else {
                        Log.i(TAG, "There are NO current innings! innings.size=" + innings.size());
                        innings.add(new InningsDBEntry(mNewInningsName, true, ""));
                        dbRef.setValue(innings);
                    }
                    mCommon.setDBUpdated(true); //notify Main to refresh view
                    mCommon.addInningsCreation2History(mNewInningsName);
                    Toast.makeText(ClubLeagueSettings.this, "New Innings (" + mNewInningsName + ") created", Toast.LENGTH_LONG).show();
                    mCommon.killActivity(ClubLeagueSettings.this, RESULT_OK);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w(TAG, "onClickCreateNewInnings: databaseError=" + databaseError);
                }
            });

        } else {  //innings already exists
            //Log.w(TAG, "innings exists:" + mCommon.mInnings);
            if (mNewInningsName.equals(mCommon.mInnings)) {
                //Log.w(TAG, "onClickCreateNewInnings: same innings name:" + mNewInningsName);
                Toast.makeText(ClubLeagueSettings.this,
                        "Current innings name is already set to " + mNewInningsName, Toast.LENGTH_LONG)
                        .show();
            } else userConfirmation1();
        }
    }

    //You really meant to create a new innings?
    private void userConfirmation1() {

        //hide keyboard
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (null != inputMethodManager)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        if (Objects.equals(mCommon.mNumOfGroups, Constants.NUM_OF_GROUPS)) { //if 2
                            getNumOfPlayersToShuffle();
                        } else {  //only 1 group, no shuffling
                            identifyPlayersToShuffle(0); //mAllPlayers has to be filled before invoking createNewInningsWithoutShuffling
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueSettings.this);
        StringBuilder sb = new StringBuilder();
        sb.append("\nThere is already an active innings: '");
        sb.append(mCommon.mInnings); sb.append("'.\n\n");
        sb.append("If you create a new one now, you will shuffle players between the 2 pools!\n\n");
        if (Objects.equals(mCommon.mNumOfGroups, Constants.NUM_OF_GROUPS)) { //if 2
            sb.append("Gold & Silver player lists will be updated as needed.\n\n");
            sb.append("Are you sure?");
        } else {
            sb.append("You are about to create a new Innings.\n\n");
            sb.append("Are you sure?");
        }
        builder.setMessage(sb.toString())
                .setTitle(mCommon.getColorString("Warning: You are about to shuffle", Color.RED))
                .setPositiveButton("Yes, Proceed", dialogClickListener)
                .setNegativeButton("No, Cancel", dialogClickListener).show();
    }

    private void getNumOfPlayersToShuffle() {

        AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueSettings.this);
        final Spinner shuffleNumSpinner = new Spinner(ClubLeagueSettings.this);
        List<Integer> spinnerArray = new ArrayList<>();
        spinnerArray.add(0);
        spinnerArray.add(1);
        spinnerArray.add(2);
        spinnerArray.add(3);
        spinnerArray.add(4);
        spinnerArray.add(5);
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                ClubLeagueSettings.this,
                android.R.layout.simple_spinner_item,
                spinnerArray
        );
        shuffleNumSpinner.setAdapter(adapter);
        shuffleNumSpinner.setSelection(3);
        // TODO: spinner text shows up to the left of the popup. Make in centre gravity somehow.

        builder.setTitle(mCommon.getTitleStr("Enter the number of players to be shuffled:", ClubLeagueSettings.this));
        //input.setSelection(input.getText().length());  //move cursor to end
        builder.setView(shuffleNumSpinner);
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Integer shufNumPlayers = (Integer) shuffleNumSpinner.getSelectedItem();
                if (shufNumPlayers == 0) {
                    //if no shuffling to be done, just create the new innings.
                    Toast.makeText(ClubLeagueSettings.this, "Created new innings, no shuffling to be performed!", Toast.LENGTH_SHORT)
                            .show();
                    identifyPlayersToShuffle(0); //mAllPlayers has to be filled before invoking createNewInningsWithoutShuffling
                    return;
                }
                identifyPlayersToShuffle(shufNumPlayers);
            }
        });
        Dialog d = builder.show();

    }

    private void identifyPlayersToShuffle(final int shufNumPlayers) {
        if (mCommon.mInnings.isEmpty()) return;
        mAllPlayers = new HashMap<String, ArrayList<PlayerData>>() {
        };
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
                    //Log.w(TAG, "identifyPlayersToShuffle group:" + logStr);
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
                        //Log.v(TAG, logStr + " name=" + name + " iPts (" + iPts.toString() + ") sPts=" + sPts.toString());
                        pList.add(new PlayerData(group, name, points));
                    }
                    if (Constants.SILVER.equals(group))
                        mCommon.sortPlayers(pList, Constants.INNINGS_IDX, false, ClubLeagueSettings.this, true);  //sort ascending for silver (last 3 to be shuffled)
                    else
                        mCommon.sortPlayers(pList, Constants.INNINGS_IDX, true, ClubLeagueSettings.this, true);  //sort descending for gold (last 3 to be shuffled)
                    //Log.w(TAG, logStr + " pList =" + pList.toString());
                    mAllPlayers.put(group, pList);
                }

                if (shufNumPlayers == 0) {
                    createNewInningsWithoutShuffling();
                    return;
                }


                //TODO: Make group names configurable
                //Map.Entry<String, ArrayList<PlayerData>> entry = mAllPlayers.entrySet().iterator().next();
                ArrayList<PlayerData> sList = mAllPlayers.get(Constants.SILVER);
                if(sList==null) {
                    Toast.makeText(ClubLeagueSettings.this,
                            "No Silver players found!",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                ArrayList<PlayerData> gList = mAllPlayers.get(Constants.GOLD);
                if(gList==null) {
                    Toast.makeText(ClubLeagueSettings.this,
                            "No Gold players found!",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                final ArrayList<PlayerData> silverNewPL = new ArrayList<>(sList);
                final ArrayList<PlayerData> goldNewPL = new ArrayList<>(gList);
                Log.i(TAG, "identifyPlayersToShuffle: Silver players:" + silverNewPL.toString());
                Log.i(TAG, "identifyPlayersToShuffle: Gold players:" + goldNewPL.toString());

                if (silverNewPL.size() < shufNumPlayers || goldNewPL.size() < shufNumPlayers) {
                    Toast.makeText(ClubLeagueSettings.this, "Not enough players to shuffle: " + silverNewPL.size() + "," + goldNewPL.size(),
                            Toast.LENGTH_LONG).show();
                    AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueSettings.this);
                    builder.setMessage("You asked to shuffle " + shufNumPlayers + " players. But, not enough players in the groups.\n" +
                            "Gold group has " + goldNewPL.size() + ".\n" +
                            "Silver group has " + silverNewPL.size() + ".")
                            .setTitle(SharedData.getInstance().getColorString("Play nice!", Color.RED))
                            .setNeutralButton("Ok", null).show();
                    return;
                }

                final ArrayList<PlayerData> goldOldPL = mAllPlayers.get(Constants.GOLD);
                final ArrayList<PlayerData> silverOldPL = mAllPlayers.get(Constants.SILVER);

                hwPD = null;
                lwPD = null;
                //Note that xxNewPL (new Player Lists) are used as input during the dry run so that the new lists can be marked.
                String gTrace = shufflePlayers(shufNumPlayers, Constants.GOLD, silverOldPL, goldNewPL, true); //dry_run
                String sTrace = shufflePlayers(shufNumPlayers, Constants.SILVER, goldOldPL, silverNewPL, true); //dry_run
                final ArrayList<PlayerData> fullPL = createWinPercentageList(silverNewPL, goldNewPL);
                String winPercTrace = shuffleThePlayerWithHighestWinPercentage(fullPL, true);  //post-Shuffle Action dry_run

                //Log.w(TAG, "AfterShuffle-rule1: Silver players:" + silverNewPL.toString());
                //Log.w(TAG, "AfterShuffle-rule1: Gold players:" + goldNewPL.toString());
                //Log.w(TAG, "AfterShuffle-rule1: " + sTrace);
                //Log.w(TAG, "AfterShuffle-rule1: " + gTrace);

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked, now do all the DB operations together

                                // This is the CRITICAL SECTION of this operation. All the DB operations involved to
                                // create a new innings are done together here. But, still the n/w connectivity could
                                // go down amidst the operations. That would leave the DB in a bad state!
                                // So, DB state is checked first and a lock is acquired.

                                TaskParams tp = new TaskParams(shufNumPlayers, goldOldPL, goldNewPL, silverOldPL, silverNewPL, fullPL);
                                mCommon.addShuffleStart2History(mNewInningsName);
                                mCommon.acquireDBLock();
                                BackgroundTask task = new BackgroundTask(ClubLeagueSettings.this, ClubLeagueSettings.this);
                                mCommon.disableUserNotify(ClubLeagueSettings.this);
                                task.execute(tp);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(ClubLeagueSettings.this);
                builder.setTitle(mCommon.getTitleStr("Shuffling", ClubLeagueSettings.this));
                builder.setMessage("You are about to shuffle the below players. You cannot undo these changes." +
                        gTrace + sTrace + winPercTrace + "\n\nAre you sure?")
                        .setTitle(mCommon.getColorString("There is no coming back!", Color.RED))
                        .setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "identifyPlayersToShuffle:onCancelled", databaseError.toException());
                Toast.makeText(ClubLeagueSettings.this, "DB error while fetching players to shuffle: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });

    }


    private String shufflePlayers(final int shufNumPlayers, final String groupName,
                                  ArrayList<PlayerData> srcList,
                                  ArrayList<PlayerData> targetList,
                                  final boolean dry_run) {
        //remove last 3 from targetList
        String header = "\n\n" + groupName.toUpperCase() + ":\n";
        StringBuilder trace = new StringBuilder();
        for (int idx = 1; idx <= shufNumPlayers; idx++) {
            if (dry_run) {
                trace.append("\t\t").append(targetList.get(targetList.size() - idx).getName()).append(" (").append(targetList.get(targetList.size() - idx).getPoints_innings()).append(")");
                if (Constants.SILVER.equals(groupName))
                    targetList.get(targetList.size() - idx).markToPromote();
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
        for (int idx = 1; idx <= shufNumPlayers; idx++) {
            if (dry_run) {
                //SGO: No need to mark the source list (which will be the old list). Marking of target list is already done above.
                //trace += srcList.get(srcList.size() - idx).getDesc() + " (" + srcList.get(srcList.size() - idx).getPoints_innings() + ") ";
                //if(Constants.SILVER.equals(groupName)) targetList.get(targetList.size() - idx).markToRelegate();
                //else targetList.get(targetList.size() - idx).markToPromote();
            } else {
                //create New user in DB: carry over season points from srcList
                createNewUser(srcList.get(srcList.size() - idx).getName(), groupName,
                        srcList.get(srcList.size() - idx).getPointsDBEntry_season(), false);
                //Assume that create to DB was successful and continue. If DB interaction failed, then refresh the main page and come back to retry
                //Toast msg is shown from delete failure code above.
                targetList.add(srcList.get(srcList.size() - idx));
            }
        }
        if (trace.length() > 0) {
            return header + trace;
        } else return trace.toString();  //nothing to do.
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
        //Log.i(TAG, "createWinPercentageList Full PL:" + fullPL.toString());
        return fullPL;
    }

    //It is assumed that the function is first called with dry_run and then w/o it.
    //Required data is constructed during dyr_run to be used during the actual execution.
    private String shuffleThePlayerWithHighestWinPercentage(ArrayList<PlayerData> fullPL, boolean dry_run) {
        final String funStr = "ClubLeague" + " win% ";
        if (dry_run) {
            mWinPercInfo = "";
            //create a win% list with no duplicates
            LinkedHashSet<Integer> winPercSet = new LinkedHashSet<>();
            for (PlayerData pd : fullPL) {
                winPercSet.add(Integer.valueOf(pd.getWinPercentage_innings()));   //descending order
            }
            //Log.i(funStr, winPercSet.toString());
            Integer[] winPercArray = winPercSet.toArray(new Integer[0]);  //convert to array, to iterate through the set

            //check if the Player with highest win% is in Gold group or already marked to be shuffled.
            //Log.i(funStr, "Full PL:" + fullPL.toString());
            //ArrayList<PlayerData> hwpList = new ArrayList<>();
            hwPD = null;
            Integer highestWinPerc = winPercArray[0];  //get first element; list is in descending order
            //Log.i(TAG, " highestWinPerc=" + highestWinPerc);
            for (PlayerData pd : fullPL) {
                if (Integer.valueOf(pd.getGamesPlayed_innings()) < SharedData.getInstance().mWinPercNum) {
                    Log.w(funStr, "not considered (< min games):" + pd.getName());
                    continue;
                }
                if (pd.getWinPercentage_innings().equals(highestWinPerc.toString())) {
                    //Log.i(funStr, "high win%:" + pd.toStringShort());
                    if (Constants.GOLD.equals(pd.getGroup())) {
                        //Log.i(funStr, pd.getName() + " already in GOLD");
                        mWinPercInfo = pd.getName() + "(" + pd.getWinPercentage_innings() + "%) is already in gold group";
                    } else {  //highest win percentage is in silver group
                        if (pd.isMarkedToPromote()) {
                            //Log.i(funStr, pd.getName() + " already marked to be promoted to GOLD");
                            mWinPercInfo = pd.getName() + "(" + pd.getWinPercentage_innings() + "%) is already getting promoted to gold group";
                        } else {
                            //Log.i(funStr, pd.getName() + " to be +++promoted+++ to GOLD");
                            mWinPercInfo = pd.getName() + "(" + pd.getWinPercentage_innings() + "%) will be promoted to gold group";
                            hwPD = new PlayerData(pd);
                            break;
                        }
                    }
                }
            } //fullPL loop

            if (hwPD == null) {
                if (mWinPercInfo.isEmpty())
                    mCommon.showToast(ClubLeagueSettings.this, "Highest win% player not found in Silver group", Toast.LENGTH_LONG);
                else
                    mCommon.showToast(ClubLeagueSettings.this, "Win% shuffling result: " + mWinPercInfo, Toast.LENGTH_LONG);
                //nothing to do; Player with highest win% is either already in Gold or is marked to be shuffled.
                return "";
            }

            //Log.i(funStr, "hwpList:" + hwPD.toStringShort());

            //find the lowest win% player from Gold group who is not marked to be shuffled.
            //ArrayList<PlayerData> lwpList = new ArrayList<>();
            lwPD = null;  //lowest win% Player Data
            boolean found = false;
            for (int i = winPercArray.length - 1; i >= 0; i--) {  //ascending order
                Integer lowestWinPerc = winPercArray[i];
                //Log.i(funStr, " lowestWinPerc=" + lowestWinPerc);
                for (ListIterator iterator = fullPL.listIterator(fullPL.size()); iterator.hasPrevious(); ) {  //iterate reverse
                    final PlayerData pd = (PlayerData) iterator.previous();
                    if (pd.getWinPercentage_innings().equals(lowestWinPerc.toString())) {
                        //Log.i(funStr, "low win%:" + pd.toStringShort());
                        if (Constants.SILVER.equals(pd.getGroup())) {
                            //Log.i(funStr, pd.getName() + " already in SILVER");
                            //But, Is he marked to be promoted?
                            if (pd.isMarkedToPromote()) {
                                Log.i(funStr, pd.getName() + " to be +++relegated +++ to SILVER, though he just got promoted");
                                lwPD = new PlayerData(pd);
                                found = true;
                                mWinPercInfo += " & " + pd.getName() + "(" + pd.getWinPercentage_innings() + "%) will be moved BACK to silver group.";
                                break;
                            }
                        } else {  //lowest win percentage is in Gold group
                            if (pd.isMarkedToRelegate()) {
                                //Log.i(funStr, pd.getName() + " already marked to be relegated  to SILVER");
                            } else {
                                Log.i(funStr, pd.getName() + " to be +++relegated +++ to SILVER");
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
                if (mWinPercInfo.isEmpty())
                    mCommon.showToast(ClubLeagueSettings.this, "Lowest win% player not found in Gold group", Toast.LENGTH_LONG);  //should never happen
                else
                    mCommon.showToast(ClubLeagueSettings.this, "Win% shuffling result: " + mWinPercInfo, Toast.LENGTH_LONG);
                //nothing to do; could not find a player with low Win % in Gold.
                return "";
            }
            //Log.i(funStr, "lwpList:" + lwPD.toStringShort());

            if (hwPD.getWinPercentage_innings().equals(lwPD.getWinPercentage_innings())) {
                mCommon.showToast(ClubLeagueSettings.this, "No win% Shuffling: Highest win% (" + hwPD.getWinPercentage_innings() +
                        "%) in Silver is equal to Lowest win% in Gold!", Toast.LENGTH_LONG);
                return "";  //Nothing more to be shuffled.
            }

            //if you reached here, you have decided to shuffle. show the shuffling results.
            if (!mWinPercInfo.isEmpty()) {
                mCommon.showToast(ClubLeagueSettings.this, "Win% shuffling result: " + mWinPercInfo, Toast.LENGTH_LONG);
            }

        } else { //not a dry_run
            if (hwPD == null && lwPD == null) {
                //Win% shuffling rule application did not find any changes to be done.
                return "";
            } else if (hwPD == null || lwPD == null) {
                //Win% shuffling rule application went wrong somewhere.
                mCommon.showToast(ClubLeagueSettings.this, "Win% shuffling went wrong! ", Toast.LENGTH_SHORT);
                Log.i(funStr, "Win% shuffling went wrong!");
                return "";
            }

            if (hwPD.getWinPercentage_innings().equals(lwPD.getWinPercentage_innings())) {
                Log.i(funStr, "High Win% == Low Win%:" + hwPD.toStringShort() + " & " + lwPD.toStringShort());
                return "";
            }

            Log.i(funStr, "WIN% SHUFFLE:" + mWinPercInfo + " high%=" + hwPD.toStringShort() + " low%=" + lwPD.toStringShort());
            //Toast.makeText(ClubLeagueSettings.this, "Win% shuffling result: " + mWinPercInfo, Toast.LENGTH_LONG)
            //        .show();

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

    private void createNewInnings() {
        final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.INNINGS);
        Query roundQuery = inningsDBRef.orderByKey();
        roundQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<List<InningsDBEntry>> t = new GenericTypeIndicator<List<InningsDBEntry>>() {
                };
                List<InningsDBEntry> innings = dataSnapshot.getValue(t);
                if (null == innings) return;
                //Log.i(TAG, "createNewInnings: key:" + dataSnapshot.getKey());
                if (-1 != mCommon.mInningsDBKey) innings.get(mCommon.mInningsDBKey).current = false;
                innings.add(new InningsDBEntry(mNewInningsName, true, ""));
                //now, write back the updated list with new innings.
                inningsDBRef.setValue(innings);
                mCommon.resetForNewInnings(mNewInningsName);
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mCommon.showToast(ClubLeagueSettings.this,
                        "Innings DB error on write: " + databaseError.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    private void createNewInningsWithoutShuffling() {

        // This is the CRITICAL SECTION of this operation. All the DB operations involved to
        // create a new innings are done together here. But, still the n/w connectivity could
        // go down amidst the operations. That would leave the DB in a bad state!
        // So, DB state is checked first and a lock is acquired.
        TaskParams tp = new TaskParams(0, null, null, null, null, null);
        mCommon.acquireDBLock();
        mCommon.addInningsCreation2History(mNewInningsName);
        BackgroundTask task = new BackgroundTask(ClubLeagueSettings.this, ClubLeagueSettings.this);
        mCommon.disableUserNotify(ClubLeagueSettings.this);
        task.execute(tp);
    }


    //reset the innings data of all players
    private void resetInningsData() {
        DatabaseReference mClubDBRef = mDatabase.child(mCommon.mClub);
        for (ArrayList<PlayerData> pList : mAllPlayers.values()) {
            for (PlayerData playerData : pList) {
                mClubDBRef.child(Constants.GROUPS).child(playerData.getGroup())
                        .child(playerData.getName()).child(Integer.toString(Constants.INNINGS_IDX))
                        .setValue(new PointsDBEntry());
            }
        }
    }

    //everything including players.
    private void resetData(final boolean everything) {
        if(mCommon.mClub.isEmpty()) return;

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(ClubLeagueSettings.this);
        String title = "Reset all points/scores";
        String msg = "Innings & Season points of all players will be reset now.\n\n" +
                "You will have to create a new Innings & Round before you can enter the scores again.\n\n" +
                "Really sure?\n";
        if(everything) {
            title = "Reset all data";
            msg = "All data including players will be deleted now.\n\n" +
                    "You will have to create the below items again before you can enter the scores:\n" +
                    "  + Players\n" +
                    "  + Innings\n" +
                    "  + Round\n\n" +
                    "Really really sure?\n";
        }
        alertBuilder.setTitle(mCommon.getColorString(title, Color.RED));
        alertBuilder.setMessage(msg);
        alertBuilder.setPositiveButton("Yes, I am sure", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                        .child(Constants.JOURNAL)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()) {
                                    //Log.d(TAG, "resetData: Removing " + dataSnapshot.getKey()); //journal
                                    dataSnapshot.getRef().removeValue();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.w(TAG, "resetData:onCancelled", databaseError.toException());
                            }
                        });

                FirebaseDatabase.getInstance().getReference()
                        .child(mCommon.mClub)
                        .child(Constants.GROUPS)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) { //gold,silver
                            if (null == child) break;
                            if(everything) {
                                //Log.d(TAG, "resetData: Resetting:" + child.getKey());
                                child.getRef().removeValue();
                                continue; //no need to iterate through the children, they are gone.
                            }
                            for (DataSnapshot gchild : child.getChildren()) { //player1, player2,..
                                if (null == gchild) break;
                                for (DataSnapshot ggchild : gchild.getChildren()) { //0,1,..
                                    if (null == ggchild) break;
                                    PointsDBEntry ptsDB = ggchild.getValue(PointsDBEntry.class);
                                    if(ptsDB!=null) {
                                        //Log.d(TAG, "resetData: Resetting:" + ggchild.getKey());
                                        ptsDB.reset();
                                        ggchild.getRef().setValue(ptsDB);
                                    }
                                }

                            }
                        }
                        mCommon.addReset2History(everything);
                        mCommon.deleteOldHistory();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w(TAG, "resetData:onCancelled", databaseError.toException());
                    }
                });

                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                        .child(Constants.INNINGS)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            //Log.d(TAG, "resetData: Removing " + dataSnapshot.getKey()); //innings
                            Toast.makeText(ClubLeagueSettings.this,
                                    "To start fresh, create a new Innings first!",
                                    Toast.LENGTH_LONG).show();
                            dataSnapshot.getRef().removeValue();
                        }
                        mCommon.mInnings = ""; //reset internal var even if it is not in DB
                        mCommon.mRoundName = "";
                        mCommon.mInningsDBKey = -1;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w(TAG, "resetData:onCancelled", databaseError.toException());
                    }
                });


            }
        });  //end of setPositiveButton
        alertBuilder.setNegativeButton("No, My mistake!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        alertBuilder.show();
    }


    private void importData() {
        //Log.d(TAG, "importData: ");
        mCommon.performFileSearch(ClubLeagueSettings.this,
                new String[]{"application/vnd.ms-excel", "text/plain"});
        Toast.makeText(ClubLeagueSettings.this,
                "Choose the file (text) to import team and player data",
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
            mCommon.wakeUpDBConnectionProfile();
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                if(null == uri) return;
                //Log.i(TAG, "Uri: " + uri.toString());
                final Map<String, ArrayList<String>> dataMap = new HashMap<>();
                try {
                    if(uri.toString().contains(".xls")) {
                        dataMap.putAll(SharedData.readClubLeagueExcel(this, uri));
                    } else {
                        dataMap.putAll(SharedData.readClubLeagueText(this, uri));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onActivityResult: Exception in readClubLeagueExcel:" + e.toString());
                    //Toast.makeText(TournaSettings.this, "Failure in parsing the xls file: " + e.getMessage(),
                    //        Toast.LENGTH_SHORT).show();
                    mCommon.showAlert(null, ClubLeagueSettings.this, "Bad input. ",
                            "Failed to parse the input file.\n" +
                                    "Make sure it is excel 97-2003 ('.xls') or plain text file format.");
                    return;

                }

                checkAndImportPlayerData(Constants.GOLD, dataMap);
            }
        }
    }

    //Flow is : Gold players -> Silver players -> Innings
    void moveToNextPool(final String group, final Map<String, ArrayList<String>> dataMap) {
        Log.d(TAG, "moveToNextPool: " + group);
        //It is done sequentially as Dialog needs to be shown to user one after the other.
        //If done all together, only the last operation is seen.
        if(group.equals(Constants.GOLD))
            checkAndImportPlayerData(Constants.SILVER, dataMap);
        else if(group.equals(Constants.SILVER)){
            //After all players are added, add innings if there is one.
            //This should be sequential, otherwise innings
            importInningsData(dataMap);
        }
        else {
            //shudnt be here
            Log.e(TAG, "moveToNextPool: why here? :" + group );
        }
    }

    void checkAndImportPlayerData(final String group, final Map<String, ArrayList<String>> dataMap) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub)
                .child(Constants.GROUPS).child(group);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final long count = dataSnapshot.getChildrenCount();
                if(count>0) {
                    mImportHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(ClubLeagueSettings.this);
                            alertBuilder.setTitle("Import players");
                            String str = "There are already " + count + " players in '" + group +
                                    "' pool. You want to continue?";
                            alertBuilder.setMessage(str);
                            alertBuilder.setPositiveButton("Yes, proceed", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    importPlayerData(group, dataMap);
                                }
                            });
                            alertBuilder.setNegativeButton("No, Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    moveToNextPool(group, dataMap);
                                }
                            });
                            alertBuilder.show();
                        }
                    }); //end of post
                } else {
                    //no players in DB, go ahead and create new players
                    mImportHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            importPlayerData(group, dataMap);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "checkAndImportPlayerData:onCancelled", databaseError.toException());
            }
        });
    }

    void importPlayerData(final String group, final Map<String, ArrayList<String>> dataMap) {
        //Log.d(TAG, "importPlayerData: " + group);
        for (Map.Entry<String, ArrayList<String>> entry : dataMap.entrySet()) {
            if(!group.equals(entry.getKey())) continue;
            final ArrayList<String> players = entry.getValue();

                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(ClubLeagueSettings.this);
                SpannableStringBuilder title = new SpannableStringBuilder("Review players being imported to ");
                title.append(SharedData.getInstance().getStyleString(group, Typeface.ITALIC));
                title.append(" pool");
                alertBuilder.setTitle(SharedData.getInstance().getColorString(title,
                        getResources().getColor(R.color.colorTealGreen)));
                alertBuilder.setMessage(players.toString());
                alertBuilder.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(players.size()>0)
                            mCommon.showToast(ClubLeagueSettings.this,
                                    "Adding players to '" + group + "' pool...",
                                    Toast.LENGTH_SHORT);

                        for(String player: players) {
                            onClickNewUser(player, group);
                        }

                        moveToNextPool(group, dataMap);
                    }
                });
                alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        moveToNextPool(group, dataMap);
                    }
                });
                alertBuilder.show();
        }
    }

    void importInningsData(final Map<String, ArrayList<String>> dataMap) {
        //Log.d(TAG, "importInningsData: ");
        for (Map.Entry<String, ArrayList<String>> entry : dataMap.entrySet()) {
            if(entry.getKey().equals(Constants.INNINGS)) {
                onClickCreateNewInnings(entry.getValue().get(0));  //first name is the innings name
                break;
            }
        }
    }

    //// Background task to update DB

    //TODO: Ideally AsyncTask should be static or leaks might occur.
    /*
    Non-static inner classes holds a reference to the containing class. When you declare AsyncTask as an inner class,
    it might live longer than the containing Activity class. This is because of the implicit reference to the containing class.
    This will prevent the activity from being garbage collected, hence the memory leak.
    We should avoid using non-static inner classes in an activity if instances of the inner class could outlive the activity’s lifecycle.
    But, it is not a problem in our case as the Activity is closed only after the Asynctask is done. See completeShuffling.
     */
    private class BackgroundTask extends AsyncTask<TaskParams, Void, String> {
        private ProgressDialog dialog;
        private Context mContext;

        public BackgroundTask(Activity activity, Context context) {
            dialog = new ProgressDialog(activity);
            this.mContext = context;

        }

        @Override
        protected void onPreExecute() {
            if (Objects.equals(mCommon.mNumOfGroups, Constants.NUM_OF_GROUPS)) { //if 2
                dialog.setMessage("Shuffling in progress....");
            } else {
                dialog.setMessage("Creating new innings....");
            }
            dialog.show();
        }

        @Override
        protected void onPostExecute(final String result) {
            //Log.v(TAG, "BackgroundTask: onPostExecute");
            //Give some time for all other threads (firebase DB updates) to catch up.
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    //Log.v(TAG, "BackgroundTask: onPostExecute, after 5s");
                    completeShuffling(result);
                }
            }, Constants.DB_READ_TIMEOUT);
        }

        private void completeShuffling(final String result) {
            Log.v(TAG, "BackgroundTask: completeShuffling");
            mCommon.enableUserNotify(ClubLeagueSettings.this);   //Toasts are allowed after this.
            if (result.isEmpty()) {
                Toast.makeText(ClubLeagueSettings.this, "New Innings created successfully!", Toast.LENGTH_SHORT)
                        .show();
                mCommon.addShuffleSuccess2History(mNewInningsName);
            }
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            //mNewInningsName = "";   //This resulted in new innings to be created with empty name; not thread safe!

            if (result.isEmpty())
                mCommon.killActivity(ClubLeagueSettings.this, RESULT_OK);
                //new innings created, go back to main page.
            else { //error string in result
                Toast.makeText(ClubLeagueSettings.this, result, Toast.LENGTH_SHORT)
                        .show();
                mCommon.addShuffleFailure2History(mNewInningsName);
            }
        }

        @Override
        protected String doInBackground(TaskParams... params) {
            TaskParams tp = params[0];
            try {
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(2000);
                    if (!mCommon.isDBConnected()) {
                        Log.e(TAG, "doInBackground: DB is not connected");
                        return "DB is not connected, try again later...";
                    }

                    if (mCommon.isDBLocked()) {

                        //Below routines, cant do toasts methods. Otherwise, the following error will happen:
                        //java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()

                        resetInningsData();  //do first before mAllPlayers list becomes stale. Thus, avoiding another DB read.

                        if (Objects.equals(mCommon.mNumOfGroups, Constants.NUM_OF_GROUPS)) { //if 2 groups in the club, do shuffling
                            //Remove last 3 from silver, list is in ascending order. Player with most points at the end;
                            //Add last 3 from gold, Gold list is in descending order. Player with least points at the end;
                            shufflePlayers(tp.shufNumPlayers, Constants.SILVER, tp.goldOldPL, tp.silverNewPL, false); //not a dry_run, real thing!
                            shufflePlayers(tp.shufNumPlayers, Constants.GOLD, tp.silverOldPL, tp.goldNewPL, false); //not a dry_run, real thing!
                            shuffleThePlayerWithHighestWinPercentage(tp.fullPL, false); //real thing!
                        }
                        createNewInnings();  //this is assumed to be the last operation always.
                        mCommon.releaseDBLock();  //release the lock
                        //Log.i(TAG, "doInBackground: success!");
                        return "";
                    }
                }

            } catch (InterruptedException e) {
                Log.w(TAG, "doInBackground: InterruptedException=" + e.getMessage());
                mCommon.releaseDBLock();
            } catch (Exception e) {
                Log.w(TAG, "doInBackground: Exception:" + e.getMessage());
                e.printStackTrace();
                mCommon.releaseDBLock();
            }

            Log.w(TAG, "doInBackground: failure! DB is not reachable, try again later...");
            return "DB is not reachable, try again later...";
        }
    }  //BackgroundTask


}


