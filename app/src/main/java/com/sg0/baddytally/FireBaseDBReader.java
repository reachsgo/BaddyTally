package com.sg0.baddytally;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;

class PlayerData implements Serializable {
    private String name;
    private String innings_score;
    private String overall_score;
    private String group;

    public PlayerData(String group) {
        this.name = "Player";
        this.innings_score = "This\nRound";
        this.overall_score = "Overall";
        this.group = group;
    }

    public PlayerData(String name, String innings_score, String overall_score, String group) {
        this.name = name;
        this.innings_score = innings_score;
        this.overall_score = overall_score;
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInnings_score() {
        return innings_score;
    }

    public void setInnings_score(String innings_score) {
        this.innings_score = innings_score;
    }

    public String getOverall_score() {
        return overall_score;
    }

    public void setOverall_score(String overall_score) {
        this.overall_score = overall_score;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}

public class FireBaseDBReader {
    private static final String TAG = "FireBaseDBReader";
    private Context mContext;
    private RecyclerViewAdapter mViewAdapter;
    private RecyclerView mView;
    private String mClub;
    private String mGroup;
    private String mInnings;

    //private String mAdminCode;
    //private String mMemCode;
    private ArrayList<PlayerData> mPlayers;
    private String mLogStr;

    public ArrayList<PlayerData> getPlayers() {
        return mPlayers;
    }

    public FireBaseDBReader(Context context, String club, String group, String innings, RecyclerViewAdapter viewAdapter, RecyclerView view) {
        this.mContext = context;
        mInnings = innings;
        mClub = club;
        mGroup = group;
        mViewAdapter = viewAdapter;
        mView = view;
        mPlayers = new ArrayList<>();
        mLogStr = "[" + mClub + "." + mGroup + "." + mInnings + "]";
    }

    public void fetchOverallScore() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.GROUPS).child(mGroup);
        Query myQuery = dbRef.orderByValue();
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
        //myQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.w(TAG, "onDataChange:" + mLogStr);
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Integer score = child.getValue(Integer.class);
                    String name = child.getKey();
                    boolean playerFound = false;
                    Log.w(TAG, mLogStr + "] child (" + name + ") score=" + score.toString());
                        for (int i = 0; i < mPlayers.size(); i++) {
                            //Log.w(TAG, "fetchThisRoundScore[" + Integer.toString(i) + "] getName=["+mPlayers[idx].get(i).getName()+"] name=["+name+"]");
                            if (mPlayers.get(i).getName().equalsIgnoreCase(name)) {
                                mPlayers.get(i).setOverall_score(score.toString());
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

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "fetchOverallScore:onCancelled", databaseError.toException());
                // ...
            }
        });
        mViewAdapter.setPlayers(mPlayers);
    }

    public void fetchThisRoundScore() {
        Log.w(TAG, mLogStr+ ": fetchThisRoundScore");
        if (mInnings.isEmpty()) return;
        //int index = 0;

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(mInnings).child(mGroup);
        Query myQuery = dbRef.orderByValue();
        //myQuery.addValueEventListener(new ValueEventListener() {
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Integer score = child.getValue(Integer.class);
                    String name = child.getKey();
                    boolean playerFound = false;
                    //Log.w(TAG, "fetchThisRoundScore [" + mRound+":"+group + "] child (" + name + ") innings score=" + score.toString());
                    if (mPlayers != null) {
                        //Log.w(TAG, mLogStr + "fetchThisRoundScore "+ name);
                        for (int i = 0; i < mPlayers.size(); i++) {
                            //Log.w(TAG, "fetchThisRoundScore[" + Integer.toString(i) + "] getName=["+mPlayers[idx].get(i).getName()+"] name=["+name+"]");
                            if (mPlayers.get(i).getName().equalsIgnoreCase(name)) {
                                mPlayers.get(i).setInnings_score(score.toString());
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
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "fetchThisRoundScore:onCancelled:" + mLogStr, databaseError.toException());
                // ...
            }
        });
    }
}


        /*ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());
                //Toast.makeText(MainActivity.this, "onChildAdded Key:"+dataSnapshot.getKey(), Toast.LENGTH_SHORT).show();
                // A new comment has been added, add it to the displayed list
                Integer score = dataSnapshot.getValue(Integer.class);
                Log.w(TAG, "onChildAdded score=" + score.toString());
                players.add(dataSnapshot.getKey());
                scores.add(Integer.toString(score));
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.w(TAG, "onChildChanged:" + dataSnapshot.getKey());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.w(TAG, "onChildRemoved:" + dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.w(TAG, "onChildMoved:" + dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                Toast.makeText(mContext, "Failed to load comments.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        //mDBInnings = FirebaseDatabase.getInstance().getReference().child("kbc").child("sept").child("gold");
        mDBRef.addChildEventListener(childEventListener);
    }*/

