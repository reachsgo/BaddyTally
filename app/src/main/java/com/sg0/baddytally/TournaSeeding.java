package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class TournaSeeding extends AppCompatActivity implements CallbackRoutine {
    private static final String TAG = "TournaSeeding";
    private static final String SEEDING = "Team Seeding";
    private List<String> mTeams;
    private ArrayList<String> mSeededTeams;
    private ListView mTeamLV;
    private ListView mSeedLV;
    private ArrayAdapter mTeamLA;
    private ArrayAdapter mSeedLA;
    private Button enter, cancel, random;
    private String mTourna;
    private SharedData mCommon;
    private ArrayList<TeamDBEntry> mTeamsFromDB;
    private Menu mOptionsMenu;
    private MenuItem mCountText;

    private void killActivity(){
        setResult(RESULT_OK);
        finish();
    }

    private void setTitle(String tourna) {
        if (!TextUtils.isEmpty(tourna)) {
            //Log.d(TAG, "setTitle: " + tourna);
            String tempString = Constants.APPSHORT + "  " + tourna;
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, Constants.APPSHORT.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), Constants.APPSHORT.length(),
                    tempString.length(), 0);
            spanString.setSpan(new RelativeSizeSpan(0.7f), Constants.APPSHORT.length(),
                    tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(""); //workaround for title getting truncated.
            getSupportActionBar().setTitle(spanString);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tourna_team_seeding);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Intent thisIntent = getIntent(); // gets the previously created intent
        mTourna = thisIntent.getStringExtra(Constants.TOURNA);
        if(null==mTourna || mTourna.isEmpty()) {
            Log.e(TAG, "onCreate: Internal error: no tournament");
            return;
        }
        mCommon = SharedData.getInstance();

        if(!mCommon.isAdminOrRoot()) finish();

        setTitle(mTourna);
        mTeams = new ArrayList<>();
        mSeededTeams = new ArrayList<>();
        mTeamsFromDB = new ArrayList<>();
        mOptionsMenu = null;
        mCountText = null;
        //Log.d(TAG, "onCreate: "+ mTourna);

        mTeamLV = this.findViewById(R.id.team_list);
        mSeedLV = this.findViewById(R.id.seed_list);
        mTeamLA = new ArrayAdapter<>(
                this, R.layout.listitem_blackbold, mTeams);
        mSeedLA = new ArrayAdapter<>(
                this, R.layout.listitem_greenbold, mSeededTeams);
        mTeamLV.setAdapter(mTeamLA);
        mSeedLV.setAdapter(mSeedLA);

        mTeamLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String team = mTeams.get(i);
                Log.d(TAG, "teamL onItemClick: " + team);
                mTeams.remove(i);
                mTeamLA.notifyDataSetChanged();
                mSeededTeams.add(team);
                mSeedLA.notifyDataSetChanged();
                mCountText.setTitle(Integer.toString(mSeededTeams.size()));
                /*
                if(null!=mOptionsMenu) {
                    MenuItem mCountText = mOptionsMenu.findItem(R.id.action_text);
                    mCountText.setTitle(mSeededTeams.size());
                }*/
            }
        });

        mSeedLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String team = mSeededTeams.get(i);
                Log.d(TAG, "teamS onItemClick: " + team);
                mTeams.add(team);
                mTeamLA.notifyDataSetChanged();
                mSeededTeams.remove(i);
                mSeedLA.notifyDataSetChanged();
                mCountText.setTitle(Integer.toString(mSeededTeams.size()));
            }
        });

        enter = findViewById(R.id.enter_button);
        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: CREATE FIXTURE");
                if(mTeams.size()==0 && mSeededTeams.size()==0) {
                    return;
                }

                if(mTeams.size()>0 && mSeededTeams.size()==0) {
                    Toast.makeText(TournaSeeding.this,
                            "Complete seeding by selecting teams from the list on the left.",
                            Toast.LENGTH_LONG).show();
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TournaSeeding.this);
                    alertBuilder.setTitle("Team seeding");
                    alertBuilder.setMessage(
                            "Use the same order as you see on the left side?");
                    alertBuilder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //copy into mSeededTeams
                            mSeededTeams = new ArrayList<>(mTeams);
                            mTeams.clear();
                            seedingCompleted();
                        }
                    });
                    alertBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //nothing to do
                        }
                    });
                    alertBuilder.show();
                } else {
                    seedingCompleted();
                }

            }
        });

        random = findViewById(R.id.random_button);
        random.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String team;
                while (mTeams.size()>0) {
                    int random = new Random().nextInt(mTeams.size());
                    team = mTeams.get(random);
                    Log.e(TAG, "onClick: random=" + random + ", team=" + team );
                    mTeams.remove(team);
                    mSeededTeams.add(team);
                }
                Toast.makeText(TournaSeeding.this,
                        mSeededTeams.size() + " teams seeded",
                        Toast.LENGTH_SHORT).show();
                mTeamLA.notifyDataSetChanged();
                mSeedLA.notifyDataSetChanged();
                mCountText.setTitle(Integer.toString(mSeededTeams.size()));
                /*
                if(null!=mOptionsMenu) {
                    MenuItem mCountText = mOptionsMenu.findItem(R.id.action_text);

                }*/
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

        readDBTeamInfo();

    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    void seedingCompleted() {
        if(mTeams.size()>0 && mSeededTeams.size()>0) {
            mCommon.showAlert(TournaSeeding.this, TournaSeeding.this, SEEDING,
                    "There are some more teams to be seeded.\n" +
                            "Once fixture is created, new teams cannot be added to this tournament.\n\n" +
                            "Are you sure to continue creating fixture without these teams?");
        } else {
            mCommon.showAlert(TournaSeeding.this, TournaSeeding.this, SEEDING,
                    "Once fixture is created, new teams cannot be added to this tournament.\n" +
                            "Are you sure to continue with creating fixture?");
        }
    }

    void readDBTeamInfo() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                .child(Constants.TOURNA).child(mTourna).child(Constants.TEAMS);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "readDBTeamInfo: onDataChange:" + dataSnapshot.getKey() + dataSnapshot.toString());
                GenericTypeIndicator<List<TeamDBEntry>> genericTypeIndicator =
                        new GenericTypeIndicator<List<TeamDBEntry>>() {
                        };
                List<TeamDBEntry> teamList = dataSnapshot.getValue(genericTypeIndicator);
                if (null == teamList) return;
                mTeamsFromDB = new ArrayList<> (teamList);
                Log.v(TAG, "readDBTeamInfo: " + mTeamsFromDB.toString());
                for(TeamDBEntry dbEntry: mTeamsFromDB) {
                    mTeams.add(dbEntry.getId());
                }

                if(mTeams.size()>0) {
                    mTeamLA.notifyDataSetChanged();
                    mCommon.showAlert(null, TournaSeeding.this, SEEDING,
                            "You have "+ mTeams.size() + " teams to seed.\n\n" +
                                    "Select teams from the list on left in the order of seeding " +
                                    "(i.e, No.1 seeded team to be selected first.)\n\n" +
                                    "Or click random for automatic random seeding.");
                } else {
                    Toast.makeText(TournaSeeding.this,
                            "No teams added!",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "readDBTeamInfo:onCancelled", databaseError.toException());
                Toast.makeText(TournaSeeding.this,
                        "DB error while fetching team entry: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    ArrayList<String> getUnitTestList(int i) {
        switch(i) {
            case 6:
                return new ArrayList<>(
                        Arrays.asList("new1", "new2", "new3", "new4", "new5", "new6"));
            case 8:
                return new ArrayList<>(
                        Arrays.asList("new1", "new2", "new3", "new4", "new5", "new6", "new7", "new8"));
            case 9:
                return new ArrayList<>(
                        Arrays.asList("new1", "new2", "new3", "new4", "new5", "new6", "new7", "new8", "new9"));
            case 16:
                return new ArrayList<>(
                        Arrays.asList("new1", "new2", "new3", "new4", "new5", "new6", "new7", "new8", "new9", "new10",
                                "new11", "new12", "new13", "new14", "new15", "new16"));
            case 32:
                return new ArrayList<>(
                        Arrays.asList("new1", "new2", "new3", "new4", "new5", "new6", "new7", "new8", "new9", "new10",
                                "new11", "new12", "new13", "new14", "new15", "new16", "new17", "new18", "new19", "new20",
                                "new21", "new22", "new23", "new24", "new25", "new26", "new27", "new28", "new29", "new30",
                                "new31", "new32"));
            case 35:
                return new ArrayList<>(
                        Arrays.asList("new1", "new2", "new3", "new4", "new5", "new6", "new7", "new8", "new9", "new10",
                                "new11", "new12", "new13", "new14", "new15", "new16", "new17", "new18", "new19", "new20",
                                "new21", "new22", "new23", "new24", "new25", "new26", "new27", "new28", "new29", "new30",
                                "new31", "new32", "new33", "new34", "new35"));
        }
        return null;
    }
    
    private void createFixture() {
        //write seeding into DB
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                .child(Constants.TOURNA).child(mTourna).child(Constants.TEAMS);
        Integer seed = 1;
        for(String team: mSeededTeams) {
            for(TeamDBEntry dbEntry: mTeamsFromDB) {
                if(dbEntry.getId().equals(team)) {
                    dbEntry.setSeed(seed);
                    break;
                }
            }
            seed++;
        }
        Log.d(TAG, "createFixture: mTeamsFromDB=" + mTeamsFromDB.toString());
        dbRef.setValue(mTeamsFromDB);

        if(mCommon.isSETournament(mTourna)) {
            singleElimination();
        } else if (mCommon.isDETournament(mTourna)) {
            doubleElimination();
        }
    }

    public void singleElimination() {
        SharedData.getInstance().wakeUpDBConnection();
        //ArrayList<String> seededTeamList = getUnitTestList(6);
        TournaSESR sesr = new TournaSESR(Constants.FIXTURE_UPPER, "");
        HashMap<String,TournaFixtureDBEntry> fixMap = sesr.createSEFixture(mSeededTeams);
        writeFixtureToDB(fixMap, Constants.FIXTURE_UPPER);
        //unitTest(fixMap);

        mCommon.mTournament = mTourna;
        mCommon.killActivity(this, RESULT_OK);
        Intent myIntent = new Intent(TournaSeeding.this, TournaLanding.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TournaSeeding.this.startActivity(myIntent);
    }

    public void doubleElimination() {
        SharedData.getInstance().wakeUpDBConnection();
        //ArrayList<String> seededTeamList = getUnitTestList(6);
        TournaSESR sesr = new TournaSESR(Constants.FIXTURE_UPPER,Constants.FIXTURE_LOWER);
        final List<TournaMatchNode> ubMatches = sesr.createSEFixtureTree(mSeededTeams);
        final HashMap<String,TournaFixtureDBEntry> ubFixMap = sesr.createFixture(ubMatches);
        writeFixtureToDB(ubFixMap, Constants.FIXTURE_UPPER);
        HashMap<String,TournaFixtureDBEntry> fixMap = sesr.createDEFixture(ubMatches, Constants.FIXTURE_UPPER);
        writeFixtureToDB(fixMap, Constants.FIXTURE_LOWER);
        updateFixtureForExtLinks(ubFixMap, fixMap, Constants.FIXTURE_LOWER, Constants.FIXTURE_UPPER);
        //unitTest(fixMap);

        mCommon.mTournament = mTourna;
        mCommon.killActivity(this, RESULT_OK);
        Intent myIntent = new Intent(TournaSeeding.this, TournaLanding.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TournaSeeding.this.startActivity(myIntent);
    }
    
    void isFixtureInDB(final String fixLabel) {
        //Check if fixture is already present in the DB
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                .child(Constants.TOURNA).child(mTourna).child(fixLabel);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getChildrenCount()>0) {
                    Log.e(TAG, "onDataChange: Fixture already in DB");
                    Toast.makeText(TournaSeeding.this,
                            "Fixture already in DB!",
                            Toast.LENGTH_LONG).show();
                    if(!mCommon.isRoot()) return;

                    //Give root an option to overwrite
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TournaSeeding.this);
                    alertBuilder.setTitle("Warning");
                    alertBuilder.setMessage("Fixture exists for this tournament.\n\n" +
                            "You will lose all the tournament data, if you overwrite the data in DB.\n" +
                            "Select cancel below or you are the destroyer!");
                    alertBuilder.setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            createFixture();
                        }
                    });
                    alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            
                        }
                    });
                    alertBuilder.show();
                } else { //no fixture in DB
                    createFixture();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TournaSeeding.this,
                        "DB error while fetching fixture: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    void writeFixtureToDB(final HashMap<String,TournaFixtureDBEntry> fixtureMap,
                          final String fixLabel) {
        Log.e(TAG, "writeFixtureToDB:" + fixtureMap.toString() );

        final DatabaseReference setDBRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                .child(Constants.TOURNA).child(mTourna).child(fixLabel);
        setDBRef.setValue(fixtureMap);
        /*
        Map<String, Object> childUpdates = new HashMap<>();
        for(Map.Entry<String,TournaFixtureDBEntry> entry : fixtureMap.entrySet()) {
            childUpdates.put(entry.getKey(),entry.getValue());
        }
        dbRef.updateChildren(childUpdates);
         */
    }

    void updateFixtureForExtLinks(final HashMap<String,TournaFixtureDBEntry> ubFixMap,
                                  final HashMap<String,TournaFixtureDBEntry> lbFixMap,
                                  final String lbFixtureLabel, final String ubFixtureLabel) {
        Log.e(TAG, "updateFixtureForExtLinks:" + lbFixtureLabel );

        //get the external links from lower bracket Matches and
        //update the external link match Ids in the UB matches.
        for(Map.Entry<String,TournaFixtureDBEntry> lbEntry : lbFixMap.entrySet()) {
            TournaFixtureDBEntry lbFixDBEntry = lbEntry.getValue();
            if(null==lbFixDBEntry) continue;
            String ubMatchId = lbFixDBEntry.getExtLinkMatchId(0);
            if(ubMatchId.isEmpty()) continue;

            for(Map.Entry<String,TournaFixtureDBEntry> ubEntry : ubFixMap.entrySet()) {
                if(!ubMatchId.equals(ubEntry.getKey())) continue;
                TournaFixtureDBEntry ubFixDBEntry = ubEntry.getValue();
                if(null==ubFixDBEntry) continue;
                ubFixDBEntry.setExtLink(0, lbFixtureLabel, lbEntry.getKey(), true);
                //Updating UB match with LB match details
                ubFixMap.put(ubEntry.getKey(),ubFixDBEntry);
                break;
            }
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                .child(Constants.TOURNA).child(mTourna).child(ubFixtureLabel);
        //dbRef.updateChildren(childUpdates);
        dbRef.setValue(ubFixMap);
    }

    //CallbackRoutine Callback interfaces
    public void profileFetched() { }
    public void callback(final String key, final Object inobj) { }
    public void alertResult(final String in, final Boolean ok, final Boolean ko) {
        if(!in.equals(SEEDING)) return;
        if(ok) {
            //Check if fixture is already present in DB
            isFixtureInDB(Constants.FIXTURE_UPPER);
        }
    }
    public void completed (final String in, final Boolean ok) {}

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.text_menu_main, menu);
        mOptionsMenu = menu;
        mCountText = mOptionsMenu.findItem(R.id.action_text);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_text:
                break;
            case android.R.id.home:
                SharedData.getInstance().killActivity(TournaSeeding.this, RESULT_OK);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}


