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
    private Context mContext;
    private boolean mSingles;
    private DatabaseReference mDBRef;
    private String mWinner;
    private boolean mShowToasts;

    public UpdateScores(Context context, boolean singles, String winner, DatabaseReference dbRef, boolean showToasts) {
        mContext = context;
        mSingles = singles;
        mDBRef = dbRef;
        mWinner = winner;
        mShowToasts = showToasts;
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        int score = dataSnapshot.getValue(Integer.class);
        int prevScore = score;
        score++;
        if (mSingles) score++;   //+2 for singles
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
