package com.sg0.baddytally;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class Summary extends AppCompatActivity {
    private static final String TAG = "Summary";

    private RecyclerView mRecyclerGoldView;
    private RecyclerView.Adapter mGoldAdapter;
    private RecyclerView mRecyclerSilverView;
    private RecyclerView.Adapter mSilverAdapter;
    private DatabaseReference mDatabase;
    private SharedData mData;
    private ArrayList<String> mRounds;
    private String mCurrentRound;

    private void killActivity(){
        /*
        if(mDBUpdated) {
            Intent resultIntent = new Intent();
            //setResult(Activity.RESULT_FIRST_USER, resultIntent);  //hint to refresh the main page
            setResult(Constants.RESULT_DBUPDATED);
            Log.d(TAG, "SGO: DB updated: setting result");
        }*/
        Log.d(TAG, "SGO: killActivity: returning OK");
        setResult(RESULT_OK);
        finish();
    }

    private void setTitle(String round) {
        if (!TextUtils.isEmpty(round)) {
            round = SharedData.getInstance().getShortRoundName(round);
            final String title = Constants.APPSHORT + "  Summary";
            String tempString = title + "\n";
            if(!SharedData.getInstance().mInnings.isEmpty()) tempString += SharedData.getInstance().mInnings;
            tempString += "/" + round;
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), 0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), title.length(), tempString.length(), 0);
            spanString.setSpan(new RelativeSizeSpan(0.8f), title.length(), tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView header = findViewById(R.id.header);
            header.setText(spanString);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clubleague_summary);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Log.v(TAG, "onCreate :" + SharedData.getInstance().toString());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mData = SharedData.getInstance();

        FloatingActionButton fab = findViewById(R.id.fab_return);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                killActivity();
            }
        });

        TextView mGoldHeader = findViewById(R.id.gold_group);
        TextView mSilverHeader = findViewById(R.id.silver_group);
        mGoldHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show only Gold group, if done a 2nd time, go back to normal.
                LinearLayout silverView = findViewById(R.id.silver_parentview);
                if (silverView.getVisibility() == View.GONE)
                    silverView.setVisibility(View.VISIBLE);
                else
                    silverView.setVisibility(View.GONE);
                return;
            }
        });

        mSilverHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show only Silver group, if done a 2nd time, go back to normal.
                LinearLayout goldView = findViewById(R.id.gold_parentview);
                if (goldView.getVisibility() == View.GONE)
                    goldView.setVisibility(View.VISIBLE);
                else
                    goldView.setVisibility(View.GONE);
                return;
            }
        });

        TextView summaryHeader = findViewById(R.id.header);
        summaryHeader.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                fetchAllRounds(view);
                //TODO: show a drop down list with the round names
                // when clicked, update the view for that round.
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        drawViews(SharedData.getInstance().mRoundName);
    }

     private void drawViews(final String round) {
        mCurrentRound = round;
        setTitle(round);
        mRecyclerGoldView = findViewById(R.id.gold_journal_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerGoldView.setHasFixedSize(true);
        if (Build.VERSION.SDK_INT >= 21) {
            //set the background image (smashing silhouette) tint to gold, only supported for API21+
            mRecyclerGoldView.setBackgroundTintList(ContextCompat.getColorStateList(Summary.this, R.color.colorGold));
        }
        // use a linear layout manager
        LinearLayout parent = findViewById(R.id.gold_parentview);
        LinearLayoutManager mGoldLayoutManager = new LinearLayoutManager(parent.getContext());
        mRecyclerGoldView.setLayoutManager(mGoldLayoutManager);

        final ArrayList<GameJournalDBEntry> goldGameList = new ArrayList<>();
        final ArrayList<String> goldGameListKeys = new ArrayList<>();
        DatabaseReference dbRef = mDatabase.child(mData.mClub).child(Constants.JOURNAL).child(mData.mInnings).child(round).child(Constants.GOLD);
        Query myQuery = dbRef.orderByKey();
        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    GameJournalDBEntry jEntry = child.getValue(GameJournalDBEntry.class);
                    goldGameListKeys.add(child.getKey());
                    goldGameList.add(jEntry);
                    //Log.w(TAG, "gold fetchGames:" + child.getKey() + " data=" + jEntry.toReadableString());
                }
                mGoldAdapter = new SummaryRecyclerViewAdapter(Summary.this, Constants.GOLD, goldGameListKeys, goldGameList);
                mRecyclerGoldView.setAdapter(mGoldAdapter);
                mGoldAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Summary.this, "DB error while fetching gold games:" + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                killActivity();
            }
        });


        if(SharedData.getInstance().mNumOfGroups==1) {
            findViewById(R.id.silver_parentview).setVisibility(View.GONE);
        } else {
            mRecyclerSilverView = findViewById(R.id.silver_journal_view);
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            mRecyclerSilverView.setHasFixedSize(true);
            // use a linear layout manager
            parent = findViewById(R.id.silver_parentview);
            LinearLayoutManager mSilverLayoutManager = new LinearLayoutManager(parent.getContext());
            //firebase DB filter allows only descending order, So, reverse the order so that highest score is shown first
            //Innings score (This round) is fetched first (see below), so that the sorting is on current round score.
            mRecyclerSilverView.setLayoutManager(mSilverLayoutManager);
            final ArrayList<GameJournalDBEntry> silverGameList = new ArrayList<>();
            final ArrayList<String> silverGameListKeys = new ArrayList<>();
            DatabaseReference dbRef2 = mDatabase.child(mData.mClub).child(Constants.JOURNAL).child(mData.mInnings).child(round).child(Constants.SILVER);
            Query myQuery2 = dbRef2.orderByKey();
            myQuery2.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        GameJournalDBEntry jEntry = child.getValue(GameJournalDBEntry.class);
                        silverGameListKeys.add(child.getKey());
                        silverGameList.add(jEntry);
                        //Log.w(TAG, "silver fetchGames:" + child.getKey() + " data=" + jEntry.toReadableString());
                    }
                    mSilverAdapter = new SummaryRecyclerViewAdapter(Summary.this, Constants.SILVER, silverGameListKeys, silverGameList);
                    mRecyclerSilverView.setAdapter(mSilverAdapter);
                    mSilverAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(Summary.this, "DB error while fetching silver games:" + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                    killActivity();
                }
            });
        }
    }

    private void fetchAllRounds(final View view) {
        mRounds = new ArrayList<>();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        DatabaseReference dbRef2 = mDatabase.child(mData.mClub).child(Constants.JOURNAL).child(mData.mInnings);
        Query myQuery2 = dbRef2.orderByKey();
        myQuery2.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    if(null == child) return;
                    mRounds.add(child.getKey());
                    Log.w(TAG, "fetchAllRounds: added" + child.getKey());
                }
                if(mRounds.size()>1) showOptions(view);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "fetchAllRounds: dberror" + databaseError.getMessage());
                Toast.makeText(Summary.this, "DB error while fetching innings" + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showOptions(final View view) {
        Context wrapper = new ContextThemeWrapper(Summary.this, R.style.RegularPopup);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
        popup.getMenu().clear();
        Menu pMenu = popup.getMenu();
        for (String round: mRounds) {
            pMenu.add(round);
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String selectedRound = menuItem.getTitle().toString();
                Log.v(TAG, "showOptions onMenuItemClick:" + selectedRound);
                if(!selectedRound.equals(mCurrentRound)) {
                    drawViews(selectedRound);
                }
                popup.dismiss();
                return true;
            }
        });
        popup.show();//showing popup menu
        Snackbar.make(view, "Select the round you want to display", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }


    @Override
    protected void onPause() {
        super.onPause();
        killActivity();
    }
}


