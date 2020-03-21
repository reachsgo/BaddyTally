package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class ClubLeagueCreateTourna extends AppCompatActivity {
    private static final String TAG = "CLCreateTourna";
    private Set<String> mGroups;
    private ArrayList<String> mPlayers;
    private ArrayList<String> mTeams;
    private ArrayList<TeamDBEntry> mTeamDBEntries;

    private int mSteps = 1;
    private ListView mPlayersLV;
    private ListView mTeamsLV;
    private ArrayAdapter mPlayersLA;
    private ArrayAdapter mTeamsLA;
    private Button enter, cancel;
    private String mTourna;
    private SharedData mCommon;
    private boolean mSingles;
    private List<String> mMatchTypeListLong;
    private List<String> mMatchTypeListShort;

    private TeamDBEntry mTeamEntry;
    private Handler mMainHandler;

    private void killActivity(){
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clubleague_create_tourna);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mMatchTypeListLong  = new ArrayList<>(Arrays.asList(
                Constants.LEAGUE,
                Constants.SE_LONG,
                Constants.DE_LONG));
        mMatchTypeListShort  = new ArrayList<>(Arrays.asList(
                Constants.LEAGUE,
                Constants.SE,
                Constants.DE));
        mTeamDBEntries = new ArrayList<>();
        mTeamEntry = null;
        mMainHandler = new Handler();

        findViewById(R.id.step2_v1).setVisibility(View.GONE);
        findViewById(R.id.step2_v2).setVisibility(View.GONE);
        findViewById(R.id.step2_v3).setVisibility(View.GONE);
        findViewById(R.id.step3_v1).setVisibility(View.GONE);
        findViewById(R.id.step3_v2).setVisibility(View.GONE);
        findViewById(R.id.step4_v1).setVisibility(View.GONE);
        findViewById(R.id.step4_v2).setVisibility(View.GONE);
        findViewById(R.id.step4_v3).setVisibility(View.GONE);
        findViewById(R.id.step4_v4).setVisibility(View.GONE);
        findViewById(R.id.step4_v5).setVisibility(View.GONE);
        teamSelection(false);

        mCommon = SharedData.getInstance();
        if(!mCommon.isSuperPlus()) finish();

        mGroups = new HashSet<>();
        mPlayers = new ArrayList<>();
        mTeams = new ArrayList<>();
        mTourna = "";
        //Log.d(TAG, "onCreate: "+ mTourna);

        findViewById(R.id.gold_group).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view;
                if (checkBox.isChecked()) {
                    mGroups.add(Constants.GOLD);
                } else {
                    mGroups.remove(Constants.GOLD);
                }
                //Log.d(TAG, "mGroups: "+ mGroups.toString());
            }
        });

        findViewById(R.id.silver_group).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view;
                if (checkBox.isChecked()) {
                    mGroups.add(Constants.SILVER);
                } else {
                    mGroups.remove(Constants.SILVER);
                }
                //Log.d(TAG, "mGroups: "+ mGroups.toString());
            }
        });

        Spinner mSpinner0 = findViewById(R.id.tourna_type_spinner);
        final ArrayAdapter<String> dataAdapter0 = new ArrayAdapter<>(ClubLeagueCreateTourna.this,
                android.R.layout.simple_spinner_item, mMatchTypeListLong);
        dataAdapter0.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner0.setAdapter(dataAdapter0);
        mSpinner0.setSelection(1); //SE

        mSingles = true;
        findViewById(R.id.pla_form_singles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSingles = true;
                //Log.d(TAG, "mSingles: "+ mSingles);
            }
        });
        findViewById(R.id.pla_form_doubles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSingles = false;
                //Log.d(TAG, "mSingles: "+ mSingles);
            }
        });

        mPlayersLV = this.findViewById(R.id.player_list);
        mTeamsLV = this.findViewById(R.id.team_list);
        mPlayersLA = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, mPlayers);
        //mTeamsLA = new ArrayAdapter<>(
        //        this, android.R.layout.simple_list_item_1, mTeams);

        //Without creating a Custom adapter, the desired functionality (fill 2 lines of simple_list_item_2)
        //can be achieved using ArrayAdapter with a very easy trick.
        //You can override the getView method of the ArrayAdapter: the trick is to supply
        //android.R.id.text1 as (principally unneccessary) parameter, otherwise the call to
        //super will cause an exception.
        mTeamsLA = new ArrayAdapter<String>(ClubLeagueCreateTourna.this,
                android.R.layout.simple_list_item_2, android.R.id.text1, mTeams)
        {
            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if(mTeamDBEntries.size()>position) {
                    TextView text1 = view.findViewById(android.R.id.text1);
                    TextView text2 = view.findViewById(android.R.id.text2);
                    text1.setText(mCommon.getStyleString(mTeams.get(position), Typeface.BOLD));
                    text2.setText(mCommon.getStyleString(
                            mTeamDBEntries.get(position).toPlayersStr(), Typeface.BOLD_ITALIC));
                }
                return view;
            }
        };
        mPlayersLV.setAdapter(mPlayersLA);
        mTeamsLV.setAdapter(mTeamsLA);

        mPlayersLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mSingles) return;
                final String player = mPlayers.get(i);
                //Log.d(TAG, "teamL onItemClick: " + player);

                if(mTeamEntry==null) mTeamEntry = new TeamDBEntry();
                List<String> players = mTeamEntry.getP();
                if(players==null) {
                    players = new ArrayList<>();
                }
                if(players.size()==0) {
                    //first player
                    players.add(player);
                    mTeamEntry.setP(players);
                    //Log.d(TAG, "teamL onItemClick: first player added:" + player);

                    int TOAST_DELAY = 0;
                    //if not the first entry, delay the toast so that we dont show the toast every time.
                    if(mTeamDBEntries.size()>0) TOAST_DELAY = 3000;
                    mMainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ClubLeagueCreateTourna.this,
                                    "Select team mate for " + player,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }, TOAST_DELAY);

                } else {
                    //second player being added
                    mMainHandler.removeCallbacksAndMessages(null);
                    players.add(player);
                    mTeamEntry.setP(players);
                    String tName = getTeamName(players);
                    mTeamEntry.setId( tName );
                    mTeams.add(tName);
                    mTeamDBEntries.add(mTeamEntry);
                    //Log.d(TAG, "teamL onItemClick: added to team:" + mTeamEntry.toDispString());
                    mTeamsLA.notifyDataSetChanged();
                    mTeamEntry = null;
                }
                mPlayers.remove(i);
                mPlayersLA.notifyDataSetChanged();
            }
        });

        mTeamsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mSingles) return;
                String team = mTeams.get(i);
                //Log.d(TAG, "teamS onItemClick: " + team);
                for(TeamDBEntry tE: mTeamDBEntries) {
                    //Log.d(TAG, "teamS onItemClick: TE=" + tE.toDispString());
                    if(tE.getId().equals(team)) {
                        mTeams.remove(team);
                        mTeamDBEntries.remove(tE);
                        //removing an item while iterating is not good, but ok here as the
                        //loop is not continued after this
                        mTeamsLA.notifyDataSetChanged();
                        mPlayers.addAll(tE.getP());
                        mPlayersLA.notifyDataSetChanged();
                        break;
                    }
                }
            }
        });

        List<Integer> bestOfList = new ArrayList<>(Arrays.asList(1, 3));
        final ArrayAdapter<Integer> dataAdapter2 = new ArrayAdapter<>(
                ClubLeagueCreateTourna.this,
                android.R.layout.simple_spinner_item, bestOfList);
        dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner mSpinner2 = findViewById(R.id.bestOf_spinner);
        mSpinner2.setAdapter(dataAdapter2);
        mSpinner2.setSelection(1);

        enter = findViewById(R.id.enter_button);
        mSteps = 1;
        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG, "onClick: Next/Create button: mSteps=" + mSteps);
                switch (mSteps) {
                    case 1:   //select groups; followed by Enter button.

                        if(mGroups.size()==0) {
                            Toast.makeText(ClubLeagueCreateTourna.this,
                                    "Select the groups participating in this tournament",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        findViewById(R.id.step1_v1).setVisibility(View.GONE);
                        findViewById(R.id.step1_v2).setVisibility(View.GONE);
                        findViewById(R.id.step1_v3).setVisibility(View.GONE);

                        findViewById(R.id.step2_v1).setVisibility(View.VISIBLE);
                        findViewById(R.id.step2_v2).setVisibility(View.VISIBLE);
                        findViewById(R.id.step2_v3).setVisibility(View.VISIBLE);
                        mSteps++;
                        break;

                    //name/desc is entered and "next" button is pressed after that
                    case 2:  //Enter name / desc; followed by Enter button.
                        EditText et = findViewById(R.id.et_newTourna);
                        mTourna = SharedData.makeTournaName(ClubLeagueCreateTourna.this,
                                                            et.getText().toString());
                        if(mTourna.isEmpty()) return;

                        //hide keyboard
                        InputMethodManager inputMethodManager =
                                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (null != inputMethodManager)
                            inputMethodManager.hideSoftInputFromWindow(
                                    enter.getWindowToken(), 0);

                        findViewById(R.id.step2_v1).setVisibility(View.GONE);
                        findViewById(R.id.step2_v2).setVisibility(View.GONE);
                        findViewById(R.id.step2_v3).setVisibility(View.GONE);

                        findViewById(R.id.step3_v1).setVisibility(View.VISIBLE);
                        findViewById(R.id.step3_v2).setVisibility(View.VISIBLE);
                        mSteps++;
                        break;

                    case 3:   //Select type; followed by Enter button.
                        findViewById(R.id.step3_v1).setVisibility(View.GONE);
                        findViewById(R.id.step3_v2).setVisibility(View.GONE);

                        findViewById(R.id.step4_v1).setVisibility(View.VISIBLE);
                        findViewById(R.id.step4_v2).setVisibility(View.VISIBLE);
                        findViewById(R.id.step4_v3).setVisibility(View.VISIBLE);
                        findViewById(R.id.step4_v4).setVisibility(View.VISIBLE);
                        findViewById(R.id.step4_v5).setVisibility(View.VISIBLE);
                        mSteps++;
                        break;

                    case 4:  //Singles / Doubles + best-of-?; followed by Enter button.
                        setPlayers();
                        findViewById(R.id.step4_v1).setVisibility(View.GONE);
                        findViewById(R.id.step4_v2).setVisibility(View.GONE);
                        findViewById(R.id.step4_v3).setVisibility(View.GONE);
                        findViewById(R.id.step4_v4).setVisibility(View.GONE);
                        findViewById(R.id.step4_v5).setVisibility(View.GONE);
                        if(!mSingles) {
                            //if doubles, team needs to be selected
                            teamSelection(true);
                            mSteps++;
                            break;
                        } else {
                            mSteps++;
                            //no team selection to be done, go to next case. No break here.
                        }

                    case 5:  //applicable only for doubles
                        Spinner mSpinner0 = findViewById(R.id.tourna_type_spinner);
                        int idx =  mSpinner0.getSelectedItemPosition();

                        findViewById(R.id.final_step_tv).setVisibility(View.VISIBLE);
                        String msg = "You are about to create '" + mTourna + "' tournament!\n" +
                                "(" + (mSingles ? "Singles" : "Doubles") +
                                " , " + mMatchTypeListLong.get(idx) + ")\n";
                        TextView tv = findViewById(R.id.final_step_tv);
                        tv.setText(msg);

                        Spinner mSpinner2 = findViewById(R.id.bestOf_spinner);
                        String bestOf = mSpinner2.getSelectedItem().toString();
                        //Log.d(TAG, "onClick: mNewTournaType=" + mNewTournaType +
                        //        " bestOf=" + bestOf);

                        enter.setText("create");
                        //findViewById(R.id.back_button).setVisibility(View.INVISIBLE);

                        mSteps++;
                        break;

                    case 6: //create button
                        createSubTournament();
                        break;
                    default:
                        Log.w(TAG, "default case: mSteps=" + mSteps);

                }
            }
        });

        Button backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG, "onClick: Back button: mSteps=" + mSteps);
                switch (mSteps) {
                    case 1:   //groups; followed by 'back' button.
                        break;

                    case 2:  //name / desc; followed by 'back' button.
                        InputMethodManager inputMethodManager =
                                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (null != inputMethodManager)
                            inputMethodManager.hideSoftInputFromWindow(
                                    enter.getWindowToken(), 0);

                        findViewById(R.id.step2_v1).setVisibility(View.GONE);
                        findViewById(R.id.step2_v2).setVisibility(View.GONE);
                        findViewById(R.id.step2_v3).setVisibility(View.GONE);

                        findViewById(R.id.step1_v1).setVisibility(View.VISIBLE);
                        findViewById(R.id.step1_v2).setVisibility(View.VISIBLE);
                        findViewById(R.id.step1_v3).setVisibility(View.VISIBLE);
                        mSteps--;
                        break;

                    case 3: //type; followed by 'back' button.
                        findViewById(R.id.step3_v1).setVisibility(View.GONE);
                        findViewById(R.id.step3_v2).setVisibility(View.GONE);

                        findViewById(R.id.step2_v1).setVisibility(View.VISIBLE);
                        findViewById(R.id.step2_v2).setVisibility(View.VISIBLE);
                        findViewById(R.id.step2_v3).setVisibility(View.VISIBLE);
                        mSteps--;
                        break;

                    case 4:   //Singles/Doubles + Select best-of-1/3
                        findViewById(R.id.step4_v1).setVisibility(View.GONE);
                        findViewById(R.id.step4_v2).setVisibility(View.GONE);
                        findViewById(R.id.step4_v3).setVisibility(View.GONE);
                        findViewById(R.id.step4_v4).setVisibility(View.GONE);
                        findViewById(R.id.step4_v5).setVisibility(View.GONE);

                        findViewById(R.id.step3_v1).setVisibility(View.VISIBLE);
                        findViewById(R.id.step3_v2).setVisibility(View.VISIBLE);
                        mSteps--;
                        break;

                    case 5:
                    case 6:
                        enter.setText("next");
                        findViewById(R.id.team_selection_view1).setVisibility(View.GONE);
                        findViewById(R.id.team_selection_view2).setVisibility(View.GONE);
                        findViewById(R.id.team_selection_view3).setVisibility(View.GONE);
                        findViewById(R.id.final_step_tv).setVisibility(View.GONE);

                        findViewById(R.id.step4_v1).setVisibility(View.VISIBLE);
                        findViewById(R.id.step4_v2).setVisibility(View.VISIBLE);
                        findViewById(R.id.step4_v3).setVisibility(View.VISIBLE);
                        findViewById(R.id.step4_v4).setVisibility(View.VISIBLE);
                        findViewById(R.id.step4_v5).setVisibility(View.VISIBLE);

                        mSteps=4;
                        break;
                    default:
                        Log.w(TAG, "default case: mSteps=" + mSteps);

                }
            }
        });

        cancel = findViewById(R.id.cancel_button);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                killActivity();
            }
        });

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

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

    void createSubTournament() {
        final String FN = "createSubTournament";
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.TOURNA);
        dbRef.child(Constants.ACTIVE).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(mTourna)) {
                    //Log.w(TAG, FN + " tournament already exists: " + mTourna);
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                            ClubLeagueCreateTourna.this);

                    DialogInterface.OnClickListener posBtn = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //delete existing data, especially needed for League format so that
                            //stale team data is removed
                            dbRef.child(mTourna).setValue(null);
                            dbRef.child(Constants.ACTIVE).child(mTourna).setValue(null);
                            createSubTournamentInDB(dbRef, mTourna, mTeamDBEntries);
                        }};

                    DialogInterface.OnClickListener negBtn = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //allow the user to change the tournament name
                            Button backBtn = findViewById(R.id.back_button);
                            backBtn.performClick();
                        }};

                    alertBuilder.setTitle("Overwrite?")
                            .setMessage(mTourna + " already exists! Overwrite?")
                            .setPositiveButton("yes", posBtn)
                            .setNegativeButton("No", negBtn)
                            .show();
                } else {
                    //tournament does not exist in DB, create new.
                    createSubTournamentInDB(dbRef, mTourna, mTeamDBEntries);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void createSubTournamentInDB(final DatabaseReference dbRef, final String newTourna,
                                         final List<TeamDBEntry> teamDBEntries) {
        Spinner mSpinner0 = findViewById(R.id.tourna_type_spinner);
        int idx =  mSpinner0.getSelectedItemPosition();
        String mNewTournaType  = mMatchTypeListShort.get(idx);

        if(mSingles) {
            //build the team data structure
            mTeamDBEntries.clear();  //for singles, team data structures are filled here.
            mTeams.clear();          //reset teh data here, as clicking back and then next again
                                     //should not cause duplicate entries.

            for (String p : mPlayers) {
                List<String> players = new ArrayList<>();
                if(mTeamEntry==null) mTeamEntry = new TeamDBEntry();
                players.add(p);  //player (full) names are already unique in the club league DB
                mTeamEntry.setP(players);
                String tName = getTeamName(players);  //get unique team names
                mTeamEntry.setId(tName);
                mTeams.add(tName);
                mTeamDBEntries.add(mTeamEntry);
                //Log.d(TAG, "teamL onItemClick: added to team:" + mTeamEntry.toDispString());
                mTeamEntry=null;

            }
        }
        if(mNewTournaType.equals(Constants.LEAGUE)) {
            createLeagueTeamData(dbRef, newTourna, mTeamDBEntries);
         } else if(mNewTournaType.equals(Constants.SE) || mNewTournaType.equals(Constants.DE)) {
            createSEDESubTournamentInDB(dbRef, newTourna, mTeamDBEntries, mNewTournaType);
        }
    }
    private void createSEDESubTournamentInDB(final DatabaseReference dbRef, final String newTourna,
                                         final List<TeamDBEntry> teamDBEntries, final String mNewTournaType) {

        String desc = "Internal Club Tournament";
        EditText et = findViewById(R.id.et_newTourna_desc);
        if(et!=null) desc = et.getText().toString();

        dbRef.child(Constants.ACTIVE).child(newTourna).setValue(mNewTournaType);
        dbRef.child(newTourna).child(Constants.DESCRIPTION).setValue(desc);
        dbRef.child(newTourna).child(Constants.TYPE).setValue(mNewTournaType);
        mCommon.createDBLock(newTourna);
        dbRef.child(newTourna).child(Constants.TEAMS).setValue(teamDBEntries);
        mCommon.setDBUpdated(true);
        Toast.makeText(ClubLeagueCreateTourna.this, newTourna +
                        " created successfully. Go ahead and 'create fixture' for the new tournament.",
                Toast.LENGTH_LONG).show();
        Log.i(TAG, "createSubTournamentInDB: created " + teamDBEntries.size() + " teams");

        Spinner mSpinner2 = findViewById(R.id.bestOf_spinner);
        String bestOf = mSpinner2.getSelectedItem().toString();
        if(!bestOf.isEmpty())
            dbRef.child(mTourna).child(Constants.MATCHES).child(Constants.META)
                    .child(Constants.INFO).child(Constants.NUM_OF_GAMES)
                    .setValue(Integer.valueOf(bestOf));

        mCommon.addHistory(
                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub),
                String.format(Locale.getDefault(),"NEW %s/%s",
                        Constants.TOURNA, newTourna));

        killActivity();

        //wake up connection and read profile again from DB to check for password changes
        mCommon.wakeUpDBConnectionProfile();
        Intent myIntent = new Intent(ClubLeagueCreateTourna.this, TournaSettings.class);
        myIntent.putExtra("animation", "fixture");
        /*If FLAG_ACTIVITY_CLEAR_TOP set, and the activity being launched is already running in
        the current task, then instead of launching a new instance of that activity, all of the
        other activities on top of it will be closed and this Intent will be delivered to the
        (now on top) old activity as a new Intent. */
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ClubLeagueCreateTourna.this.startActivity(myIntent);
    }


    private void createLeagueTeamData(final DatabaseReference dbRef, final String newTourna,
                                      final List<TeamDBEntry> teamDBEntries) {

        //Log.d(TAG, newTourna + ":createLeagueTeamData: " + teamDBEntries.size());
        ArrayList<TeamInfo> teamList = new ArrayList<>();
        ArrayList<String> currPNickList = new ArrayList<>();
        for (TeamDBEntry teamDBEntry: teamDBEntries) {
            TeamInfo tI = new TeamInfo(teamDBEntry.getId());
            for (String p: teamDBEntry.getP()) {
                tI.players.add(p);
                //User name in Club League is 20 chars. activity_clubleague_settings.xml > android:max_longname_len="20"
                String nickName = SharedData.getUniqIDStr(p, 0, currPNickList).toLowerCase();
                tI.p_nicks.add( nickName );
                //There is no check for length of nick name anywhere else: see readTournaExcel()
                currPNickList.add(nickName); //so that there are no duplicate nick names
            }
            teamList.add(tI);
            //Log.d(TAG, "createLeagueTeamData: Adding:" + tI.toString());
        }

        if(!isValidLeagueTeamData(teamList)) return;

        dbRef.child(mTourna).child(Constants.MATCHES).child(Constants.META)
                    .child(Constants.INFO).child(Constants.NUM_OF_MATCHES)
                    .setValue(1);  //only 1 match per match-set in this case
        Spinner mSpinner2 = findViewById(R.id.bestOf_spinner);
        String bestOf = mSpinner2.getSelectedItem().toString();
        if(!bestOf.isEmpty())
            dbRef.child(mTourna).child(Constants.MATCHES).child(Constants.META)
                    .child(Constants.INFO).child(Constants.NUM_OF_GAMES)
                    .setValue(Integer.valueOf(bestOf));


        Log.v(TAG, "createLeagueTeamData: " + teamList.toString());
        for (TeamInfo tI: teamList) {
            final String teamShortName = tI.name;
            final String teamLongName = tI.desc;
            if(teamShortName==null || teamShortName.isEmpty() || teamLongName.isEmpty()) continue;

            if(tI.p_nicks.size() != tI.players.size()) {
                Toast.makeText(ClubLeagueCreateTourna.this, "Bad data for team '" + teamShortName +"'.",
                        Toast.LENGTH_SHORT).show();
                continue;
            }

            //Add team data
            DatabaseReference teamDBRef = dbRef.child(mTourna).child(Constants.TEAMS)
                    .child(teamShortName);
            teamDBRef.child(Constants.SCORE).setValue(new TeamScoreDBEntry());
            teamDBRef.child(Constants.DESCRIPTION).setValue(teamLongName);
            dbRef.child(newTourna).child(Constants.TEAMS_SUMMARY)
                    .child(teamShortName).setValue(true);
            //Log.d(TAG, "createLeagueTeamData: Team created:" + teamShortName + ":" + teamLongName);

            //Add players data
            for(int i=0; i < tI.p_nicks.size(); i++) {
                final String playerShortName = tI.p_nicks.get(i);
                final String playerLongName = tI.players.get(i);
                if(playerShortName.isEmpty() || playerLongName.isEmpty()) continue;
                PlayerInfo pInfo = new PlayerInfo();
                pInfo.T = teamShortName;
                pInfo.name = playerLongName;
                //Log.i(TAG, "createLeagueTeamData Adding player:" + playerShortName +
                //        " info:" + pInfo.toString());
                DatabaseReference teamsDBRef = dbRef.child(mTourna).child(Constants.PLAYERS);
                teamsDBRef.child(playerShortName).setValue(pInfo);
            }
        }

        dbRef.child(Constants.ACTIVE).child(newTourna).setValue(Constants.LEAGUE);

        mCommon.createDBLock(newTourna);
        mCommon.setDBUpdated(true);
        Toast.makeText(ClubLeagueCreateTourna.this, newTourna +
                        " created successfully. Go ahead and 'create fixture' for the new tournament.",
                Toast.LENGTH_LONG).show();
        Log.i(TAG, "createLeagueTeamData: created " + teamList.size() + " teams");

        mCommon.addHistory(
                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub),
                String.format(Locale.getDefault(),"NEW %s/%s",
                        Constants.TOURNA, newTourna));

        killActivity();

        //wake up connection and read profile again from DB to check for password changes
        mCommon.wakeUpDBConnectionProfile();
        Intent myIntent = new Intent(ClubLeagueCreateTourna.this, TournaSettings.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        myIntent.putExtra("animation", "fixture");
        ClubLeagueCreateTourna.this.startActivity(myIntent);
    }

    private boolean isValidLeagueTeamData(final ArrayList<TeamInfo> teamList) {
        //Log.v(TAG, "isValidLeagueTeamData: " + teamList.toString());
        for (TeamInfo tI: teamList) {
            for (TeamInfo otherTI: teamList) {
                if(tI.equals(otherTI)) continue;
                if(tI.name.equals(otherTI.name)) {
                    Toast.makeText(ClubLeagueCreateTourna.this, "Duplicate team name '" + tI.name +"'.",
                            Toast.LENGTH_SHORT).show();
                    //Log.d(TAG, "isValidLeagueTeamData1: " + teamList.toString());
                    return false;
                }
                if(tI.desc.equals(otherTI.desc)) {
                    Toast.makeText(ClubLeagueCreateTourna.this, "Duplicate team name '" + tI.desc +"'.",
                            Toast.LENGTH_SHORT).show();
                    //Log.d(TAG, "isValidLeagueTeamData2: " + teamList.toString());
                    return false;
                }
                for(String nick1 : tI.p_nicks){
                    for(String nick2 : otherTI.p_nicks){
                        if(nick1.equals(nick2)) {
                            Toast.makeText(ClubLeagueCreateTourna.this, "Duplicate player name '" +
                                            nick1 +"'.",
                                    Toast.LENGTH_SHORT).show();
                            //Log.d(TAG, "isValidLeagueTeamData3: " + tI.p_nicks.toString());
                            return false;
                        }
                    }
                }

                for(String name1 : tI.players){
                    for(String name2 : otherTI.players){
                        if(name1.equals(name2)) {
                            Toast.makeText(ClubLeagueCreateTourna.this, "Duplicate player name '" +
                                            name1 +"'.",
                                    Toast.LENGTH_SHORT).show();
                            //Log.d(TAG, "isValidLeagueTeamData4: " + otherTI.players.toString());
                            return false;
                        }
                    }
                }
            }

        }
        return true;
    }

    //get unique team name
    String getTeamName(final List<String> players) {
        if(players==null || players.size()==0) return "null";

        String teamName;
        if(players.size()==1) {
            teamName = players.get(0);  //singles: only 1 player; getUniqIDStr() is called below.
        } else {
            //doubles: form a team name from both the player names
            teamName = String.format("%s-%s",
                    SharedData.getUniqIDStr(players.get(0),
                            TeamDBEntry.MAX_ID_LEN / 2 - 1, null),
                    SharedData.getUniqIDStr(players.get(1),
                            TeamDBEntry.MAX_ID_LEN / 2 - 1, null));
        }
        //Log.d(TAG, teamName +":getTeamName: mTeams=[" + mTeams.toString() + "]");

        //Now that we have a team name whether singles or doubles, make it a unique name
        //pass the current list of teams so that a unique name is generated
        return SharedData.getUniqIDStr(teamName, TeamDBEntry.MAX_ID_LEN, mTeams).toLowerCase();
    }

    void setPlayers() {
        //reset the player/team data
        mTeams.clear();
        mTeamDBEntries.clear();
        mPlayers.clear();

        //Log.d(TAG, "setPlayers: group=" + mGroups +
        //        " singles=" + mSingles + " steps=" + mSteps +
        //        " players=" + mPlayers + " teamdb.size=" + mTeamDBEntries.size());

        for (String group: mGroups) {
            if(group.equals(Constants.GOLD)) {
                if(mCommon.mGoldPlayers.size()>0) {
                    for(PlayerData pd: mCommon.mGoldPlayers)
                        mPlayers.add(pd.getName());
                }
            } else if(group.equals(Constants.SILVER)) {
                if(mCommon.mSilverPlayers.size()>0) {
                    for(PlayerData pd: mCommon.mSilverPlayers)
                        mPlayers.add(pd.getName());
                }
            }
        }
        //Log.d(TAG, "setPlayers: players=" + mPlayers);
        mPlayersLA.notifyDataSetChanged();
        mTeamsLA.notifyDataSetChanged();
    }

    void teamSelection(final boolean show) {
        if(!show) {
            this.findViewById(R.id.team_selection_view1).setVisibility(View.GONE);
            this.findViewById(R.id.team_selection_view2).setVisibility(View.GONE);
            this.findViewById(R.id.team_selection_view3).setVisibility(View.GONE);
            this.findViewById(R.id.final_step_tv).setVisibility(View.GONE);
            return;
        }

        this.findViewById(R.id.team_selection_view1).setVisibility(View.VISIBLE);
        this.findViewById(R.id.team_selection_view2).setVisibility(View.VISIBLE);
        this.findViewById(R.id.team_selection_view3).setVisibility(View.VISIBLE);
        this.findViewById(R.id.final_step_tv).setVisibility(View.VISIBLE);

        String msg = "\nSelect 2 players from the left\nto form a team.\n";
        TextView tv = findViewById(R.id.final_step_tv);
        tv.setText(mCommon.getBgColorString(msg, Color.LTGRAY));

        //reset the team data
        mTeamDBEntries.clear();
    }

}


