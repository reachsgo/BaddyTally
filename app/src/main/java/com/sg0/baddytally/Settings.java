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
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Settings extends AppCompatActivity {
    private static final String TAG = "Settings";
    private Spinner mDelSpinner;
    private List<String> mPlayerList;
    private DatabaseReference mDatabase;
    private ArrayAdapter<String> mPlayerListAdapter;
    private String mNewInningsName;
    private Map<String, ArrayList<PlayerData>> mAllPlayers;



    private void killActivity(){
        finish();
    }

    private void enableDisableView(View view, boolean enabled) {
        view.setEnabled(enabled);
        view.setClickable(false);
        view.setAlpha(.5f);  //making it semi-transparent
        Log.w(TAG, "enableDisableView called...");
        //now do the same for all the children views.
        if ( view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for ( int idx = 0 ; idx < group.getChildCount() ; idx++ ) {
                enableDisableView(group.getChildAt(idx), enabled);
            }
        }
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        Log.w(TAG, "onCreate :" + SharedData.getInstance().toString());

        if (!Constants.ROOT.equals(SharedData.getInstance().mRole)) {
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
            return;
        }

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
                        createNewUser(name, group, null);  //no season points to carry over for a new user
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
        builder.setTitle("Creating New Player");
        builder.setMessage("You are about to create a new user:\n\n" +
                name + " to \"" + group + "\" group.\n\nAre you sure?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void createNewUser(final String name, final String group, final PointsDBEntry seasonPts) {
        final String club = SharedData.getInstance().mClub;
        final String innings = SharedData.getInstance().mInnings;

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
                    Toast.makeText(Settings.this, "New user data (GROUPS/group) could not be saved:" + databaseError.getMessage(),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(Settings.this, "New user " + name + " created in \"" + group +
                                    "\" group of " + club,
                            Toast.LENGTH_LONG).show();
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


        Snackbar.make(findViewById(R.id.settings_ll), name + " is being added to " + club + "/" + group + "/" + innings,
                Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    private void fetchDataAndUpdateSpinner() {
        mPlayerList = null;
        mPlayerList = new ArrayList<>();
        final String club = SharedData.getInstance().mClub;
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
                        deleteUser(name, group);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
        builder.setTitle("Deleting a Player");
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
        mNewInningsName = tmpName.toUpperCase().charAt(0) + tmpName.substring(1,tmpName.length());
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
                .setTitle(SharedData.getInstance().getRedString("Warning"))
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void getNumOfPlayersToShuffle() {
        final Integer NUMPLAYERS_TO_SHUFFLE = 3;
        AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
        // Set an EditText view to get user input
        final EditText input = new EditText(Settings.this);
        input.setText(NUMPLAYERS_TO_SHUFFLE.toString());
        builder.setTitle("Enter the number of players to be shuffled:");
        input.setSelection(input.getText().length());  //move cursor to end
        builder.setView(input);
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Integer shufNumPlayers;
                try {
                    shufNumPlayers = Integer.parseInt(input.getText().toString());
                } catch (NumberFormatException nfe) {
                    Toast.makeText(Settings.this, "Invalid entry!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //final int position = holder.getAdapterPosition();
                //Toast.makeText(Settings.this, shufNumPlayers + " will be shuffled", Toast.LENGTH_LONG).show();
                identifyPlayersToShuffle(shufNumPlayers);
            }
        });
        builder.show();
    }
    private void identifyPlayersToShuffle(final int shufNumPlayers) {
        if (SharedData.getInstance().mInnings.isEmpty()) return;
        mAllPlayers  = new HashMap<String, ArrayList<PlayerData>>() {};
        //Retrieve latest list from DB
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(SharedData.getInstance().mClub)
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
                        SharedData.getInstance().sortPlayers(pList, false);  //sort ascending for silver (last 3 to be shuffled)
                    else
                        SharedData.getInstance().sortPlayers(pList, true);  //sort descending for gold (last 3 to be shuffled)
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


                //Note that xxNewPL (new Player Lists) are used as input during the dry run so that the new lists can be marked.
                String sTrace = shufflePlayers(shufNumPlayers, Constants.SILVER, goldOldPL, silverNewPL,   true); //dry_run
                String gTrace = shufflePlayers(shufNumPlayers, Constants.GOLD, silverOldPL, goldNewPL, true); //dry_run
                final ArrayList<PlayerData> fullPL = createWinPercentageList(silverNewPL, goldNewPL);
                shuffleThePlayerWithHighestWinPercentage(fullPL);  //post-Shuffle Action

                Log.w(TAG, "AfterShuffle-rule1: Silver players:" + silverNewPL.toString());
                Log.w(TAG, "AfterShuffle-rule1: Gold players:" + goldNewPL.toString());
                Log.w(TAG, "AfterShuffle-rule1: " + sTrace);
                Log.w(TAG, "AfterShuffle-rule1: " + gTrace);

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                resetInningsData();  //do first before mAllPlayers list becomes stale. Thus, avoiding another DB read.
                                //Remove last 3 from silver, list is in ascending order. Player with most points at the end;
                                //Add last 3 from gold, Gold list is in descending order. Player with least points at the end;
                                shufflePlayers(shufNumPlayers, Constants.SILVER, goldOldPL, silverNewPL,  false); //not a dry_run, real thing!
                                shufflePlayers(shufNumPlayers, Constants.GOLD, silverOldPL, goldNewPL,  false); //not a dry_run, real thing!
                                createNewInnings();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
                builder.setTitle("Shuffling");
                builder.setMessage("You are about to do the below shuffling actions. You cannot undo these changes.\n" +
                                sTrace + gTrace + "\n\nAre you sure?")
                        .setTitle(SharedData.getInstance().getRedString("There is no coming back!"))
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
//                                  ArrayList<PlayerData> fullPL,
                                  final boolean dry_run){
        //remove last 3 from targetList
        String trace = "\n" + groupName + ": remove ";
        for (int idx=1; idx<=shufNumPlayers; idx++) {
            if(dry_run) {
                trace += targetList.get(targetList.size() - idx).getName() + " (" + targetList.get(targetList.size() - idx).getPoints_innings() + ") ";
                if(Constants.SILVER.equals(groupName)) targetList.get(targetList.size() - idx).markToPromote();
                else targetList.get(targetList.size() - idx).markToRelegate();
            } else {
                deleteUser(targetList.get(targetList.size() - 1).getName(), groupName);  //delete from DB
                //Assume that delete to DB was successful and continue. If DB interaction failed, then refresh the main page and come back to retry
                //Toast msg is shown from delete failure code above.
                targetList.remove(targetList.size() - 1);
            }
        }

        //add last 3 from srcList to targetList
        trace += "\n" + groupName + ": add ";
        for (int idx=1; idx<=shufNumPlayers; idx++) {
            if(dry_run) {
                trace += srcList.get(srcList.size() - idx).getName() + " (" + srcList.get(srcList.size() - idx).getPoints_innings() + ") ";
                //if(Constants.SILVER.equals(groupName)) targetList.get(targetList.size() - idx).markToRelegate();
                //else targetList.get(targetList.size() - idx).markToPromote();
            } else {
                //create New user in DB: carry over season points from srcList
                createNewUser(srcList.get(srcList.size() - idx).getName(), groupName, srcList.get(srcList.size() - idx).getPointsDBEntry_season());
                //Assume that create to DB was successful and continue. If DB interaction failed, then refresh the main page and come back to retry
                //Toast msg is shown from delete failure code above.
                targetList.add(srcList.get(srcList.size() - idx));
            }
        }
        return trace;
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

    private String shuffleThePlayerWithHighestWinPercentage(ArrayList<PlayerData> fullPL) {

        Log.w(TAG, "shuffleThePlayerWithHighestWinPercentage Full PL:" + fullPL.toString());
        ArrayList<PlayerData> hwpList = new ArrayList<>();
        ArrayList<PlayerData> lwpList = new ArrayList<>();
        String highestWinPerc = fullPL.get(0).getWinPercentage_innings();
        String lowestWinPerc = fullPL.get(fullPL.size()-1).getWinPercentage_innings();
        for (PlayerData pd : fullPL) {
            if (pd.getWinPercentage_innings().equals(highestWinPerc)) {
                Log.w(TAG, "shuffleThePlayerWithHighestWinPercentage high win%:" + pd.toString());
                if(Constants.GOLD.equals(pd.getGroup())) {
                    Log.w(TAG, "shuffleThePlayerWithHighestWinPercentage" + pd.getName() + " already in GOLD");
                    continue;
                } else {  //highest win percentage is in silver group
                    if (pd.isMarkedToPromote()){
                        Log.w(TAG, "shuffleThePlayerWithHighestWinPercentage" + pd.getName() + " already marked to promote to GOLD");
                        continue;
                    } else {
                        Log.w(TAG, "shuffleThePlayerWithHighestWinPercentage" + pd.getName() + " to be +++promoted+++ to GOLD");
                    }
                }

            }
            else if (pd.getWinPercentage_innings().equals(lowestWinPerc))
                lwpList.add(pd);
        }

        Log.w(TAG, "shuffleThePlayerWithHighestWinPercentage hwpList:" + hwpList.toString() + " lwpList:" + lwpList.toString());
        return "";

    }

    private void createNewInnings(){
        final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference().child(SharedData.getInstance().mClub).child(Constants.INNINGS);
        Query roundQuery = inningsDBRef.orderByKey();
        roundQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<List<InningsDBEntry>> t = new GenericTypeIndicator<List<InningsDBEntry>>() {
                };
                List<InningsDBEntry> innings = dataSnapshot.getValue(t);
                if(null==innings) return;
                Log.v(TAG, "fetchInnings: key:" + dataSnapshot.getKey());
                innings.get(SharedData.getInstance().mInningsDBKey).current = false;
                innings.add(new InningsDBEntry(mNewInningsName, true, ""));
                SharedData.getInstance().mInningsDBKey = innings.size()-1;
                //now, write back the updated list with new innings.
                inningsDBRef.setValue(innings);
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "createNewInnings: onCancelled", databaseError.toException());
                Toast.makeText(Settings.this, "Innings DB error on write: " + databaseError.toString(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    //reset the innings data of all players
    private void resetInningsData() {
        DatabaseReference mClubDBRef = mDatabase.child(SharedData.getInstance().mClub);
        for (ArrayList<PlayerData>  pList : mAllPlayers.values()) {
            for(PlayerData playerData : pList) {
                mClubDBRef.child(Constants.GROUPS).child(playerData.getGroup())
                        .child(playerData.getName()).child(Integer.toString(Constants.INNINGS_IDX))
                        .setValue(new PointsDBEntry());
            }
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        killActivity();
    }
}


