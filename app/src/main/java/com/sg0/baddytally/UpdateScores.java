package com.sg0.baddytally;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

class UpdateScores implements ValueEventListener {
    private static final String TAG = "UpdateScores";
    private final Context mContext;
    private final boolean mSingles;
    private final DatabaseReference mDBRef;
    private final String mWinner;
    private final boolean mDelete;
    private final boolean mShowToasts;

    public UpdateScores(Context context, boolean singles, String winner, DatabaseReference dbRef, boolean deleteFlag, boolean showToasts) {
        mContext = context;
        mSingles = singles;
        mDBRef = dbRef;
        mWinner = winner;
        mDelete = deleteFlag;
        mShowToasts = showToasts;
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        int score = dataSnapshot.getValue(Integer.class);
        int prevScore = score;
        if(mDelete) {
            score--;
            if (mSingles) score--;   //-2 for singles
        } else {
            score++;
            if (mSingles) score++;   //+2 for singles
        }
        Log.w(TAG, "Points updated for "+ mWinner + ": " + prevScore + " -> " + score);
        mDBRef.setValue(score);
        if(mShowToasts) {
            Toast.makeText(mContext, "Points updated for " + mWinner + ": " + prevScore + " -> " + score,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
        Toast.makeText(mContext, "DB error while fetching score of " + mWinner, Toast.LENGTH_LONG).show();
    }
}
