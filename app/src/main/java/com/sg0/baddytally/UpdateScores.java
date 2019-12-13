package com.sg0.baddytally;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import androidx.annotation.NonNull;

class UpdateScores implements ValueEventListener {
    private static final String TAG = "UpdateScores";
    private final Context mContext;
    private final boolean mSingles;
    private final DatabaseReference mDBRef;
    private final boolean mDelete;
    private final boolean mWinner;
    private final boolean mShowToasts;

    public UpdateScores(Context context, boolean singles, boolean wonTheMatch, DatabaseReference dbRef, boolean deleteFlag, boolean showToasts) {
        mContext = context;
        mSingles = singles;
        mDBRef = dbRef;
        mWinner = wonTheMatch;
        mDelete = deleteFlag;
        mShowToasts = showToasts;
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

        GenericTypeIndicator<List<PointsDBEntry>> genericTypeIndicator = new GenericTypeIndicator<List<PointsDBEntry>>() {};
        List<PointsDBEntry> list = dataSnapshot.getValue(genericTypeIndicator );
        if(null==list || list.size() != 2) return;
        String mPlayerName = dataSnapshot.getKey();
        Log.v(TAG, "FETCH: player=" + mPlayerName + " innings:" + list.get(Constants.INNINGS_IDX).toString());
        int prevScore = list.get(Constants.INNINGS_IDX).getPts();
        if(mDelete) {  //match delete from summary by root user
            list.get(Constants.INNINGS_IDX).deleteMatch(mSingles, mWinner);
            list.get(Constants.SEASON_IDX).deleteMatch(mSingles, mWinner);
        } else {  //new match entry
            if(mWinner) {
                list.get(Constants.INNINGS_IDX).wonMatch(mSingles);
                list.get(Constants.SEASON_IDX).wonMatch(mSingles);
            } else { //lost the game
                list.get(Constants.INNINGS_IDX).lostMatch();
                list.get(Constants.SEASON_IDX).lostMatch();
            }
        }
        int newScore = list.get(Constants.INNINGS_IDX).getPts();
        mDBRef.setValue(list);
        if(mShowToasts) {
            Toast.makeText(mContext, "Points updated for " + mPlayerName + ": " + prevScore + " -> " + newScore,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
        Toast.makeText(mContext, "DB error while fetching score", Toast.LENGTH_LONG).show();
    }
}
