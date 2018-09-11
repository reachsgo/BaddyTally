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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


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

    private void fetchOverallScore() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.GROUPS).child(mGroup);
        Query myQuery = dbRef.orderByValue();
        //myQuery.addValueEventListener(new ValueEventListener() {
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
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
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "fetchThisRoundScore:onCancelled:" + mLogStr, databaseError.toException());
                Toast.makeText(mContext, "DB error while fetching round score: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
