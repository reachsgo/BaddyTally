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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
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

    public ArrayList<PlayerData> getPlayers() {
        return mPlayers;
    }

    public void fetchOverallScore() {
        Log.w(TAG, "fetchOverallScore:" + mLogStr);
        if (mClub.isEmpty()) return;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.GROUPS).child(mGroup);
        //firebase documentation: Children with a numeric value come next, sorted in ascending order.
        //         If multiple children have the same numerical value for the specified child node, they are sorted by key.
        //String orderBy = Integer.toString(Constants.INNINGS_IDX) + "/pts";
        //Log.w(TAG, "SGO: fetchOverallScore orderBy:" + orderBy);
        //Query myQuery = dbRef.orderByChild("pts");
        //SGO: orderByChild did not work! Is it due to the list of data in each player DB field?
        //workaround is to do the list sorting in java.

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.w(TAG, "onDataChange:" + mLogStr);
                //for (DataSnapshot child : dataSnapshot.getChildren()) {
                GenericTypeIndicator<Map<String, List<PointsDBEntry>>> genericTypeIndicator = new GenericTypeIndicator<Map<String, List<PointsDBEntry>>>() {
                };
                Map<String, List<PointsDBEntry>> map = dataSnapshot.getValue(genericTypeIndicator);
                if (null == map) return;
                Log.v(TAG, "FETCH: group:" + dataSnapshot.getKey());
                int count = 0;
                for (Map.Entry<String, List<PointsDBEntry>> entry : map.entrySet()) {
                    //PlayerDBEntry player = child.getValue(PlayerDBEntry.class);
                    String name = entry.getKey();
                    List<PointsDBEntry> points = entry.getValue();
                    PointsDBEntry sPts = points.get(Constants.SEASON_IDX);
                    PointsDBEntry iPts = points.get(Constants.INNINGS_IDX);
                    Log.w(TAG, mLogStr + " name=" + name + " iPts (" + iPts.toString() + ") sPts=" + sPts.toString());
                    mPlayers.add(new PlayerData(mGroup, name, points));
                }
                if (mViewAdapter != null) {
                    mView.smoothScrollToPosition(mPlayers.size() - 1);  //scroll back to the top of the list to show highest point scorers
                    mViewAdapter.sortPlayers();
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

}
