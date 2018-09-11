package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class Settings extends AppCompatActivity {
    private static final String TAG = "Settings";
    private Spinner mDelSpinner;
    private List<String> mPlayerList;
    private DatabaseReference mDatabase;
    private ArrayAdapter<String> mPlayerListAdapter;

    private void killActivity(){
        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        Log.w(TAG, "onCreate :" + SharedData.getInstance().toString());
        ((EditText)findViewById(R.id.newuser)).setText("");
        ((EditText)findViewById(R.id.newuser)).setHint("new user name");
        Button newuserAddBtn = findViewById(R.id.enter_button);
        newuserAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickNewUser();
            } //onClick
        });
        ((RadioButton)findViewById(R.id.nu_gamegroup_silver)).setChecked(true);

        mDelSpinner = findViewById(R.id.del_spinner);
        RadioGroup mDelRadioGroup = findViewById(R.id.del_gamegroup_radiogroup);
        mDelRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
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
                        createNewUser(name, group);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
        builder.setMessage("You are about to create a new user:\n\n" +
                name + " to \"" + group + "\" group.\n\nAre you sure?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void createNewUser(final String name, final String group) {
        final String club = SharedData.getInstance().mClub;
        final String innings = SharedData.getInstance().mInnings;
        //Create user with 0 overall score.
        DatabaseReference dbRef = mDatabase.child(club).child(Constants.GROUPS).child(group).child(name);
        dbRef.setValue(0, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Toast.makeText(Settings.this, "New user data (GROUPS/group) could not be saved:" + databaseError.getMessage(),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(Settings.this, "New user " + name + " created in \"" + group +
                                    "\" group of " + club,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        //Added user with 0 score to current innings.
        dbRef = mDatabase.child(club).child(innings).child(group).child(name);
        dbRef.setValue(0, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Toast.makeText(Settings.this, "New user data (innings/group) could not be saved:" + databaseError.getMessage(),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(Settings.this, "New user " + name + " added to ongoing \"" + innings + "\" innings.",
                            Toast.LENGTH_LONG).show();
                    ((EditText)findViewById(R.id.newuser)).setText("");
                    ((EditText)findViewById(R.id.newuser)).setHint("new user name");
                }
                //killActivity(); //thats too soon, snackbar or toasts are not seen in this view.
            }
        });

        //hide keyboard
            if(getCurrentFocus()!=null) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if(null!=inputMethodManager)
                    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }


        Snackbar.make(findViewById(R.id.settings_ll), name + " is being added to " + club + "/" + group + "/" + innings,
                Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    private void fetchDataAndUpdateSpinner() {
        mPlayerList = null;
        mPlayerList = new ArrayList<>();
        final String club = SharedData.getInstance().mClub;
        int selectedId = ((RadioGroup) findViewById(R.id.del_gamegroup_radiogroup)).getCheckedRadioButtonId();
        final String group = ((RadioButton) findViewById(selectedId)).getText().toString();
        Log.w(TAG, "updateSpinner:" + group);
        DatabaseReference dbRef = mDatabase.child(club).child(Constants.GROUPS).child(group);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    mPlayerList.add(child.getKey());
                    Log.w(TAG, "updateSpinner, added:" + child.getKey());
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
                        deleteUser(name, group);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
        builder.setMessage("You are about to delete a player:\n\n" +
                name + " from \"" + group + "\" group.\n\nAre you sure?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void deleteUser(final String name, final String group) {
        final String club = SharedData.getInstance().mClub;
        final String innings = SharedData.getInstance().mInnings;
        //Create user with 0 overall score.
        DatabaseReference dbRef = mDatabase.child(club).child(Constants.GROUPS).child(group).child(name);
        dbRef.setValue(null, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Toast.makeText(Settings.this, "DB (delete user: GROUPS/group) error: " + databaseError.getMessage(),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(Settings.this, name + " deleted from \"" + group +
                                    "\" group of " + club,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        //Added user with 0 score to current innings.
        dbRef = mDatabase.child(club).child(innings).child(group).child(name);
        dbRef.setValue(null, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Toast.makeText(Settings.this, "DB delete user: innings/group) error: " + databaseError.getMessage(),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(Settings.this, name + " deleted from \"" + innings + "\" innings.",
                            Toast.LENGTH_LONG).show();
                    mPlayerListAdapter.notifyDataSetInvalidated();
                }
            }
        });

        Snackbar.make(findViewById(R.id.settings_ll), name + " is being deleted from " + club + "/" + group + "/" + innings,
                Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    // 1. set current innings entry under club/innings/<current> to {1:false, 2:xxx}
    // 2. create new entry under club/innings/ with {1:true, 2:""}
    // 3. create new gold player list with X top players from silver list moved up
    // 4. create new silver player list with X bottom players from gold list moved down
    // 5. Persist the new lists to DB, after user confirmation
    // 6. club/<new_innings>/<group>/<player_list> will get created automatically?
    private void onClickCreateNewInnings(){
        String tmpName =  ((EditText)findViewById(R.id.newinnings)).getText().toString();
        if(TextUtils.isEmpty(tmpName)) {
            Log.w(TAG, "onClickCreateNewInnings: empty innings name");
            return;
        }
        final String inningsName = tmpName.toUpperCase().charAt(0) + tmpName.substring(1,tmpName.length());
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        //deleteUser(name, group);
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
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        killActivity();
    }
}


