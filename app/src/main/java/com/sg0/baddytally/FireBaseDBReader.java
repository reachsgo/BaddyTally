package com.sg0.baddytally;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import java.util.List;
import java.util.Map;


class FireBaseDBReader {
    private static final String TAG = "FireBaseDBReader";
    private final Context mContext;
    private final RecyclerViewAdapter mViewAdapter;
    private final RecyclerView mView;
    private final String mClub;
    private final String mGroup;
    private final String mInnings;
    private final ArrayList<PlayerData> mPlayers;
    private final String mLogStr;

    public ArrayList<PlayerData> getPlayers() {
        return mPlayers;
    }

    public FireBaseDBReader(Context context, String club, String group, String innings, RecyclerViewAdapter viewAdapter, RecyclerView view) {
        mContext = context;
        mInnings = innings;
        mClub = club;
        mGroup = group;
        mViewAdapter = viewAdapter;
        mView = view;
        mPlayers = new ArrayList<>();
        mLogStr = "[" + mClub + "." + mGroup + "." + mInnings + "]";
    }

    public void fetchOverallScore() {
        if (mInnings.isEmpty()) return;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.GROUPS).child(mGroup);
        //Children with a numeric value come next, sorted in ascending order. If multiple children have the same numerical value for the specified child node, they are sorted by key.
        String orderBy = Integer.toString(Constants.INNINGS_IDX) + "/pts";
        Log.w(TAG, "SGO: fetchOverallScore orderBy:" + orderBy);
        Query myQuery = dbRef.orderByChild("pts");
        //myQuery.addValueEventListener(new ValueEventListener() {
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.w(TAG, "onDataChange:" + mLogStr);
                //for (DataSnapshot child : dataSnapshot.getChildren()) {
                    GenericTypeIndicator<Map<String, List<PointsDBEntry>>> genericTypeIndicator = new GenericTypeIndicator<Map<String, List<PointsDBEntry>>>() {};
                    Map<String, List<PointsDBEntry>> map = dataSnapshot.getValue(genericTypeIndicator );
                    if(null==map) return;
                    Log.v(TAG, "FETCH: group:" + dataSnapshot.getKey());
                    int count = 0;
                    for (Map.Entry<String,List<PointsDBEntry> > entry : map.entrySet()) {
                        //PlayerDBEntry player = child.getValue(PlayerDBEntry.class);
                        String name = entry.getKey();
                        List<PointsDBEntry> points =  entry.getValue();
                        PointsDBEntry sPts = points.get(Constants.SEASON_IDX);
                        PointsDBEntry iPts = points.get(Constants.INNINGS_IDX);
                        Log.w(TAG, mLogStr + " name=" + name + " iPts (" + iPts.toString() + ") sPts=" + sPts.toString());
                        mPlayers.add(new PlayerData( mGroup, name, points));
                    }

                    /*
                    Integer score = child.getValue(Integer.class);
                    if(null==score) continue;
                    String name = child.getKey();
                    boolean playerFound = false;
                    Log.w(TAG, mLogStr + "] child (" + name + ") score=" + score.toString());
                    for (int i = 0; i < mPlayers.size(); i++) {
                        //Log.w(TAG, "fetchThisRoundScore[" + Integer.toString(i) + "] getName=["+mPlayers[idx].get(i).getName()+"] name=["+name+"]");
                        if (mPlayers.get(i).getName().equalsIgnoreCase(name)) {
                            mPlayers.get(i).setPts_season(score.toString());
                            //Log.w(TAG, mLogStr + "onDataChange =====> child (" + name + ") set overall score=" + score.toString());
                            playerFound = true;
                        }
                    }
                    if (!playerFound) {
                        PlayerData player = new PlayerData(name, "0", score.toString(), mGroup);
                        mPlayers.add(player);
                    }*/
              //  }
                /*
                Collections.sort(mPlayers, new Comparator<PlayerData>() {
                    @Override
                    public int compare(PlayerData playerData, PlayerData obj2) {
                        return Integer.valueOf(playerData.getPointsInt_innings()).compareTo(obj2.getPointsInt_innings());  //descending order
                    }
                });*/

                        /*Comparator<PlayerData>(){
                    public int compare(EmployeeClass obj1, EmployeeClass obj2) {
                        // ## Ascending order
                        return obj1.firstName.compareToIgnoreCase(obj2.firstName); // To compare string values
                        // return Integer.valueOf(obj1.empId).compareTo(obj2.empId); // To compare integer values

                        // ## Descending order
                        // return obj2.firstName.compareToIgnoreCase(obj1.firstName); // To compare string values
                        // return Integer.valueOf(obj2.empId).compareTo(obj1.empId); // To compare integer values
                    }
                });*/
                if (mViewAdapter != null) {
                    mView.smoothScrollToPosition(mPlayers.size()-1);  //scroll back to the top of the list to show highest point scorers
                    mViewAdapter.sortPlayers();
                    mViewAdapter.notifyDataSetChanged();
                }
            }

            /*
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.w(TAG, "onDataChange:" + mLogStr);
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Integer score = child.getValue(Integer.class);
                    if(null==score) continue;
                    String name = child.getKey();
                    boolean playerFound = false;
                    Log.w(TAG, mLogStr + "] child (" + name + ") score=" + score.toString());
                        for (int i = 0; i < mPlayers.size(); i++) {
                            //Log.w(TAG, "fetchThisRoundScore[" + Integer.toString(i) + "] getName=["+mPlayers[idx].get(i).getName()+"] name=["+name+"]");
                            if (mPlayers.get(i).getName().equalsIgnoreCase(name)) {
                                mPlayers.get(i).setPts_season(score.toString());
                                //Log.w(TAG, mLogStr + "onDataChange =====> child (" + name + ") set overall score=" + score.toString());
                                playerFound = true;
                            }
                        }
                    if (!playerFound) {
                        PlayerData player = new PlayerData(name, "0", score.toString(), mGroup);
                        mPlayers.add(player);
                    }
                }
                if (mViewAdapter != null) {
                    mView.smoothScrollToPosition(mPlayers.size()-1);  //scroll back to the top of the list to show highest point scorers
                    mViewAdapter.notifyDataSetChanged();
                }
            }*/

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "fetchOverallScore:onCancelled", databaseError.toException());
                Toast.makeText(mContext, "DB error while fetching overall score: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
        mViewAdapter.setPlayers(mPlayers);
    }

    public void fetchThisRoundScore() {
        if (mInnings.isEmpty()) return;
        //fetchOverallScore();
/*
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(mInnings).child(mGroup);
        Query myQuery = dbRef.orderByValue();
        //myQuery.addValueEventListener(new ValueEventListener() {
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Integer score = child.getValue(Integer.class);
                    if(null==score) continue;
                    String name = child.getKey();
                    boolean playerFound = false;
                    //Log.w(TAG, "fetchThisRoundScore [" + mRound+":"+group + "] child (" + name + ") innings score=" + score.toString());
                    if (mPlayers != null) {
                        //Log.w(TAG, mLogStr + "fetchThisRoundScore "+ name);
                        for (int i = 0; i < mPlayers.size(); i++) {
                            //Log.w(TAG, "fetchThisRoundScore[" + Integer.toString(i) + "] getName=["+mPlayers[idx].get(i).getName()+"] name=["+name+"]");
                            if (mPlayers.get(i).getName().equalsIgnoreCase(name)) {
                                mPlayers.get(i).setPts_innings(score.toString());
                                //Log.w(TAG, mLogStr + "fetchThisRoundScore =====> child (" + name + ") set innings score=" + score.toString());
                                playerFound = true;
                            }
                        }
                    }
                    if (!playerFound) {
                        PlayerData player = new PlayerData(name, score.toString(),"0", mGroup);
                        mPlayers.add(player);
                    }
                }
                if (mViewAdapter != null) mViewAdapter.notifyDataSetChanged();
                fetchOverallScore();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "fetchThisRoundScore:onCancelled:" + mLogStr, databaseError.toException());
                Toast.makeText(mContext, "DB error while fetching round score: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        }); */
    }
}
