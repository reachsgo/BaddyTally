package com.sg0.baddytally;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TournaLeague extends AppCompatActivity implements CallbackRoutine {
    private static final String TAG = "TournaLeague";
    private static final String CREATE_NEW_TEAM = "Create new team";
    private SharedData mCommon;
    private TournaRecyclerViewAdapter mTournaAdapter;
    private Handler mMainHandler;
    private GestureDetector mDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tourna_league);

        mCommon = SharedData.getInstance();
        //Log.d(TAG, "onCreate: " + mCommon.mTournament);
        if(mCommon.mTournament.isEmpty()) {
            Toast.makeText(TournaLeague.this,
                    "Tournament not known!", Toast.LENGTH_SHORT).show();
            return;
        }

        setTitle(mCommon.mTournament);
        findViewById(R.id.header_ll).setVisibility(View.GONE);
        findViewById(R.id.enter_button).setVisibility(View.GONE);
        mMainHandler = new Handler();

        Button enterScoreBtn = findViewById(R.id.enter_button);
        enterScoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedData.getInstance().wakeUpDBConnectionProfile();
                //Intent myIntent = new Intent(TournaLeague.this, LoginActivity.class);
                Intent myIntent = new Intent(TournaLeague.this, TournaLeagueEnterData.class);
                myIntent.putExtra(Constants.TOURNATYPE, Constants.LEAGUE);
                TournaLeague.this.startActivity(myIntent);
            } //onClick
        });

        Button schedBtn = findViewById(R.id.schedule_button);
        schedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedData.getInstance().wakeUpDBConnectionProfile();
                //Intent myIntent = new Intent(TournaLeague.this, LoginActivity.class);
                Intent myIntent = new Intent(TournaLeague.this, TournaLeagueSchedule.class);
                myIntent.putExtra("tournament", mCommon.mTournament);
                TournaLeague.this.startActivity(myIntent);
            } //onClick
        });

        Button summaryBtn = findViewById(R.id.summary_button);
        summaryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mCommon.mTournament.isEmpty()) return;
                SharedData.getInstance().wakeUpDBConnectionProfile();
                Intent myIntent = new Intent(TournaLeague.this, TournaSummary.class);
                myIntent.putExtra("tournament", mCommon.mTournament);
                TournaLeague.this.startActivityForResult(myIntent, Constants.SUMMARY_ACTIVITY);
            } //onClick
        });

        summaryBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(!mCommon.isSuperPlus()) return false;
                if(mCommon.mClub.isEmpty() || mCommon.mTournament.isEmpty()) {
                    return false;
                }
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                        .child(Constants.TOURNA).child(mCommon.mTournament)
                        .child(Constants.INTERNALS).child(Constants.HISTORY);
                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        StringBuilder history = new StringBuilder();
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            String storyLine = child.getValue(String.class);
                            if (null == storyLine) continue;
                            String parsed = mCommon.parseHistory(storyLine);
                            if (!parsed.isEmpty()) history.append(parsed).append("\n");
                        }
                        if(history.length()==0) return;
                        AlertDialog.Builder builder = new AlertDialog.Builder(TournaLeague.this);
                        builder.setMessage(mCommon.getSizeString(history.toString(), 0.6f))
                                .setTitle(mCommon.getTitleStr("History:", TournaLeague.this))
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                    }
                                }).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
                return true;
            }
        });

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        setUpGesture();
        mCommon.mTime = Calendar.getInstance().getTime().getTime();

        RecyclerView tournaRecyclerView = findViewById(R.id.tourna_list);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        tournaRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        tournaRecyclerView.setLayoutManager(mLayoutManager);
        mTournaAdapter = new TournaRecyclerViewAdapter(this, TournaLeague.this,
                mCommon.mTournament, findViewById(R.id.outer_ll), mMainHandler);
        tournaRecyclerView.setAdapter(mTournaAdapter);
    }

    private void setTitle(String tourna) {
        if (!TextUtils.isEmpty(tourna)) {
            //Log.d(TAG, "setTitle: " + tourna);
            String tempString = Constants.APPSHORT + "  " + tourna;
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, Constants.APPSHORT.length(),0);
            spanString.setSpan(new StyleSpan(Typeface.ITALIC), Constants.APPSHORT.length(),
                    tempString.length(), 0);
            spanString.setSpan(new RelativeSizeSpan(0.7f), Constants.APPSHORT.length(),
                    tempString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(""); //workaround for title getting truncated.
            getSupportActionBar().setTitle(spanString);
        }
    }

    void refresh() {
        if(Calendar.getInstance().getTime().getTime() - mCommon.mTime > Constants.REFRESH_TIMEOUT) {
            mCommon.mTime = Calendar.getInstance().getTime().getTime();
            Toast.makeText(TournaLeague.this,
                    "Refreshing...", Toast.LENGTH_SHORT).show();
            recreate();
            //Following is the sequence invoked after recreate() is called:
            //onDestroy(), onCreate(), onResume()
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        mCommon.startProgressDialog(TournaLeague.this,
                "TournaLeague", "Connecting..");

        int delayTime = 0;
        if(!mCommon.isDBConnected()) {
            mCommon.wakeUpDBConnectionProfile();
            delayTime = 2000;  //is DB not connected, give it time to get connected.
        }
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mTournaAdapter!=null) mTournaAdapter.readDBTeam(false);
                mCommon.showToastAndDieOnTimeout(mMainHandler, TournaLeague.this,
                        "Check your internet connection", true, true,
                        Constants.DB_READ_TIMEOUT);
                //events removed in success case from adapter:readDBTeam()
            }
        }, delayTime);

    }


    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        mCommon.killActivity(this, RESULT_OK);
        Intent myIntent = new Intent(TournaLeague.this, TournaLanding.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TournaLeague.this.startActivity(myIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.basic_menu_main, menu);
        menu.findItem(R.id.action_logout).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                refresh();
                break;
            case R.id.action_help:
                AlertDialog.Builder hBuilder = new AlertDialog.Builder(TournaLeague.this);
                hBuilder.setMessage(Html.fromHtml(
                        "<a href=\"https://sites.google.com/view/scoretally/user-guide\">User Guide link</a>"))
                        .setTitle(Constants.APPNAME)
                        .setNeutralButton("Ok", null);
                AlertDialog help = hBuilder.create();
                help.show();
                // Make the textview clickable. Must be called after show()
                ((TextView)help.findViewById(android.R.id.message))
                        .setMovementMethod(LinkMovementMethod.getInstance());
                break;
            case R.id.action_about:
                SharedData.showAboutAlert(TournaLeague.this);
                break;
            default:
                break;
        }

        return true;
    }

    //CallbackRoutine Callback interfaces
    public void profileFetched() { }
    public void callback(final String key, final Object inobj) { }
    public void alertResult(final String in, final Boolean ok, final Boolean ko) { }
    public void completed (final String in, final Boolean ok) { }

    /*
    Below override of dispatchTouchEvent is needed for swipeLeft/Right to work
    for a scroll view.
    By default the touch listener for the scroll view get disabled and therefore scroll action
    does not happen. In order to fix this you need to override the dispatchTouchEvent method of
    the Activity and return the inherited version of this method after you're done with your own listener.
     */

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //Log.d(TAG, "dispatchTouchEvent: ");
        mDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    private void setUpGesture() {
        mDetector = new GestureDetector(TournaLeague.this, new TournaLeague.STGestureListener());

        findViewById(R.id.outer_ll).setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                //Log.d(TAG, "onTouch: ");
                return mDetector.onTouchEvent(event);
            }
        });
    }

    class STGestureListener implements GestureDetector.OnGestureListener {

        //Keeping the threshold pretty high so that simple scroll also works on the
        //UB and LB tables.
        private static final long VELOCITY_THRESHOLD = 7000;
        private static final String TAG = "TournaGesture";

        @Override
        public boolean onDown(final MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(final MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,
                                final float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(final MotionEvent e) {
        }

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                               final float velocityX,
                               final float velocityY) {

            //Log.d(TAG, "onFling: velocityX=" + velocityX + " velocityY=" + velocityY);
            if (Math.abs(velocityX) < VELOCITY_THRESHOLD
                    && Math.abs(velocityY) < VELOCITY_THRESHOLD) {
                return false;//if the fling is not fast enough then it's just like drag
            }

            //if velocity in Y direction is higher than velocity in X direction,
            //then the fling is vertical
            if (Math.abs(velocityY) > Math.abs(velocityX)) {
                if (velocityY >= 0) {
                    //Log.i(TAG, "swipe down");
                    refresh();
                }
            }
            return true;
        }
    }
}
