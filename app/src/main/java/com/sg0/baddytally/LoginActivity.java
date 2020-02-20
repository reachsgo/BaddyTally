package com.sg0.baddytally;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class LoginActivity extends AppCompatActivity implements CallbackRoutine{

    private static final String TAG = "LoginActivity";

    private EditText mClubView;
    private TextView mUserView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private RadioGroup mGameTypeRadioGroup;
    private RadioButton mGameTypeRadioButton;
    private RadioGroup mGroupRadioGroup;
    private RadioButton mGroupRadioButton;
    private String mAdminCode;
    private String mMemCode;
    private String mRootCode;
    private String mClub;
    private String mUser;
    private String mRole;
    private boolean mInitialAttempt;
    private boolean mNewRoundFlag;
    private boolean mTournaFlag;
    private String mActToStart;
    private Handler mMainHandler;
    private SharedData mCommon;
    private int mVerCode;
    private ProgressDialog progressDialog;


    private void killActivity(){
        finish();
    }


    @Override
    public void finish() {
        showProgress(false);
        super.finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ScoreTally.activityPaused();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScoreTally.activityResumed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mInitialAttempt = false;
        mClubView = findViewById(R.id.clubname);
        mUserView = findViewById(R.id.username);
        mPasswordView =  findViewById(R.id.password);
        mGameTypeRadioGroup = findViewById(R.id.gametype_radiogroup);
        mGroupRadioGroup = findViewById(R.id.gamegroup_radiogroup);
        mNewRoundFlag = false;
        mTournaFlag = false;
        mMainHandler = new Handler();
        mCommon = SharedData.getInstance();
        mCommon.mCount = 0;
        mVerCode = SharedData.getAppVersionCode(LoginActivity.this);

        Intent thisIntent = getIntent(); // gets the previously created intent
        String tType = thisIntent.getStringExtra(Constants.TOURNATYPE);
        if(null!=tType && !tType.isEmpty()) {
            mTournaFlag = true;
        }
        mActToStart = "";
        String actToStart = thisIntent.getStringExtra(Constants.ACTIVITY);
        if(null!=actToStart && !actToStart.isEmpty()) {
            mActToStart = actToStart;
        }

        //checked attribute in XML doesnt seem to work in the new API version.
        //so, setting the defaults in code here.
        ((RadioButton)findViewById(R.id.gametype_default)).setChecked(true);
        ((RadioButton)findViewById(R.id.gamegroup_silver)).setChecked(true);

        //hide keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if(null!=inputMethodManager)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        if(mCommon.mNumOfGroups==1) {
            //Only one group exists
            findViewById(R.id.gamegroup_silver).setEnabled(false);
            ((RadioButton)findViewById(R.id.gamegroup_gold)).setChecked(true);
        }
        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        final String secpd = prefs.getString(Constants.DATA_SEC, "");
        if (!secpd.isEmpty()) {
            mPasswordView.setText(secpd);
        }

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat(Constants.ROUND_DATEFORMAT_SHORT, Locale.CANADA);
        String todaysDate = df.format(c);
        Log.d(TAG, "onCreate LoginActivity["+ todaysDate + "]: data = " + mCommon.toString());

        final String club = prefs.getString(Constants.DATA_CLUB, "");
        mUser = mCommon.getUserID(LoginActivity.this);
        String userText = "usr: " + mUser;
        mUserView.setText(userText);
        if (club.isEmpty()) {
            mInitialAttempt = true;
            mGameTypeRadioGroup.setVisibility(View.GONE);
            mGroupRadioGroup.setVisibility(View.GONE);
            findViewById(R.id.new_round_btn).setVisibility(View.GONE);
            findViewById(R.id.current_round).setVisibility(View.GONE);
            findViewById(R.id.time_now).setVisibility(View.GONE);
        }else {
            mClubView.setText(club);
            String roundStr =  "Active round: ";
            if(mCommon.mRoundName.isEmpty()) roundStr += "None";
            else roundStr += mCommon.getShortRoundName(mCommon.mRoundName);
            ((TextView)findViewById(R.id.current_round)).setText(roundStr);
            ((TextView)findViewById(R.id.time_now)).setText(("Today's date: " + df.format(c)));
            if (!mCommon.mRoundName.contains(todaysDate)) {
                findViewById(R.id.new_round_btn).setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
        }

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    prepareForLogin(club, secpd);
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        //mEmailSignInButton.setFocus
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                prepareForLogin(club, secpd);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        findViewById(R.id.new_round_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mNewRoundFlag = true;
                prepareForLogin(club, secpd);
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab_return);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                killActivity();
            }
        });

        //if (data.mTournaMode) {
        if(mTournaFlag || mActToStart.equals(Constants.ACTIVITY_SETTINGS) ||
                mActToStart.equals(Constants.ACTIVITY_TOURNA_SETTINGS) ||
                mActToStart.equals(Constants.INITIAL)) {
            Log.d(TAG, "onCreate Tournament mode");
            findViewById(R.id.options_ll).setVisibility(View.GONE);
            findViewById(R.id.new_round_btn).setVisibility(View.GONE);
            findViewById(R.id.current_round).setVisibility(View.GONE);
            findViewById(R.id.time_now).setVisibility(View.GONE);
        }


        String lockedTimeStr = prefs.getString(Constants.DATA_LOCKED, "");
        if(!lockedTimeStr.isEmpty()) {
            Long lockTime = 0L;
            try {
                lockTime = Long.parseLong(lockedTimeStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "NumberFormatException: " + lockedTimeStr );
                lockTime = 0L;
            }
            Log.i(TAG, "lockTime: " + lockTime );
            Long now = System.currentTimeMillis();
            //if((now - lockTime) < 300) {
            if((now - lockTime) < 18000000) {   //5 hrs = 5 * 60 * 60000
                lockedMsg(lockedTimeStr);
            } else {
                Log.i(TAG, "onCreate: Unlocking the system now:" + now.toString());
            }
        }


    }

    private void prepareForLogin(String club, String secpd) {
        mClub = mClubView.getText().toString().trim();
        fetchInitialData();
    }

    //TODO: This DB fetch is done every time data is entered. This could be optimized to
    //reduce number of DB reads.
    private void fetchInitialData() {
        Log.w(TAG, "fetchInitialData: " + mClub + "/" + mCommon.mClub);

        if(!mClub.equals(mCommon.mClub)) {
            //If the club's name is changed during login, then fetch data again
            mInitialAttempt = true;
            mCommon.mProfile.clear();
            mCommon.clearData(LoginActivity.this, false);
        }
        if (!mCommon.mProfile.getMemcode().isEmpty() && !mInitialAttempt) {
            Log.w(TAG, "fetchInitialData: data already populated!");
            mAdminCode = mCommon.mProfile.getAdmincode();
            mMemCode = mCommon.mProfile.getMemcode();
            mRootCode = mCommon.mProfile.getRootcode();
            attemptLogin();
            return;
        }

        //hide keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if(null!=inputMethodManager)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        showProgress(true);

        mCommon.fetchProfile(LoginActivity.this, LoginActivity.this, mClub);

        SharedData.showToastAndDieOnTimeout(mMainHandler, LoginActivity.this,
                "Club not found!\nInvalid Club or bad network connectivity!",
                true, 0);
        //showProgress(false) will be called from finish();
    }

    //CallbackRoutine Callback after profile is fetched from DB. See SharedData impl of fetchProfile()
    public void profileFetched() {
        mMainHandler.removeCallbacksAndMessages(null);  //delete the toast runnables posted above
        Log.w(TAG, "profileFetched invoked ...." + mCommon.toString());
        mAdminCode = mCommon.mProfile.getAdmincode();
        mMemCode = mCommon.mProfile.getMemcode();
        mRootCode = mCommon.mProfile.getRootcode();
        showProgress(false);
        attemptLogin();
    }

    public void alertResult(final String in, final Boolean ok, final Boolean ko) {
    }

    public void completed(final String in, final Boolean ok) {
    }

    public void callback(final String key, final Object inobj) {}

    private void attemptLogin() {
        if(mVerCode < mCommon.mProfile.getMinver()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle(mCommon.getColorString("Update your app", Color.RED));
            builder.setMessage("There is a newer app version available.\nPlease update to the latest version.")
                    .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mCommon.killApplication(LoginActivity.this);
                        }
                    })
                    .show();
        } else {
            attemptLogin2();
        }
    }
    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin2() {

        // Reset errors.
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String club = mClubView.getText().toString().trim();
        String secpd = mPasswordView.getText().toString();

        if(club.isEmpty() || secpd.isEmpty() || mUser.isEmpty()) {
            Toast.makeText(LoginActivity.this,
                    "All fields are mandatory!", Toast.LENGTH_SHORT).show();

            return;
        }

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(secpd) && !isPasswordValid(secpd)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        //Log.v(TAG, "attemptLogin(" + club + "," + secpd + "):" + mAdminCode + ":" + mMemCode);

        //hide keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if(null!=inputMethodManager)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            if (secpd.equals(mAdminCode)) {
                mRole = Constants.ADMIN;
                successfulLogin(club, secpd);
            } else if (secpd.equals(mMemCode)) {
                mRole = Constants.MEMBER;
                successfulLogin(club, secpd);
            } else if (secpd.equals(mRootCode)) {
                mRole = Constants.ROOT;
                successfulLogin(club, secpd);
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
                mCommon.mCount++;
                if(mCommon.mCount == Constants.DATA_LOCKED_COUNT_MAX-2) {
                    Toast.makeText(LoginActivity.this,
                            "You will be locked out if you get it wrong again!",
                            Toast.LENGTH_LONG).show();
                } else if(mCommon.mCount > Constants.DATA_LOCKED_COUNT_MAX) {
                    Long now = System.currentTimeMillis();
                    SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(Constants.DATA_LOCKED, now.toString());
                    editor.apply();
                    lockedMsg(now.toString());
                }

            }
        }
    }

    private void lockedMsg(final String lockTimeStr) {
        Toast.makeText(LoginActivity.this,
                "User locked out!\nToo many failed attempts to log in!\n",
                Toast.LENGTH_LONG).show();
        Log.e(TAG, "lockedMsg: System is locked from:" + lockTimeStr );
        findViewById(R.id.options_ll).setVisibility(View.GONE);
        findViewById(R.id.new_round_btn).setVisibility(View.GONE);
        findViewById(R.id.current_round).setVisibility(View.GONE);
        findViewById(R.id.time_now).setVisibility(View.GONE);
        findViewById(R.id.email_sign_in_button).setEnabled(false);
    }

    private void successfulLogin(String club, String secpd) {
        setUserInDB();
        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.DATA_CLUB, club);
        editor.putString(Constants.DATA_SEC, secpd);
        editor.putString(Constants.DATA_ROLE, mRole);
        //editor.putString(Constants.DATA_USER, mUser);
        editor.putString(Constants.DATA_LOCKED, "");
        editor.apply();
        mCommon.mClub = club;
        mCommon.mRole = mRole;
        //mCommon.mUser = mUser;
        mCommon.wakeUpDBConnection();  //update DB with the new user login
        if (mInitialAttempt && mActToStart.isEmpty()) {
            killActivity();   //finish was not ending the activity here.
            return;
        }

        if(mActToStart.equals(Constants.INITIAL)) {
            //start from Main again. There is no history for initial activities, so we cant just do a
            //killActivity here.
            mCommon.killActivity(this, RESULT_OK);
            Intent intent = new Intent(LoginActivity.this, MainSigninActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }

        int selectedId = mGameTypeRadioGroup.getCheckedRadioButtonId();
        // find the radiobutton by returned id
        mGameTypeRadioButton = findViewById(selectedId);

        selectedId = mGroupRadioGroup.getCheckedRadioButtonId();
        // find the radiobutton by returned id
        mGroupRadioButton = findViewById(selectedId);
        Log.v(TAG, "successfulLogin:" + mGameTypeRadioButton.getText() + ":" + mGroupRadioButton.getText());

        if (mNewRoundFlag) {
            //Are you sure you want to create a new round of games?
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            if(mCommon.mInningsDBKey == -1) {
                                Toast.makeText(LoginActivity.this,
                                        "Go to Settings and create a new innings first.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            String roundName = mCommon.createNewRoundName(true, LoginActivity.this);
                            FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.INNINGS)
                                    .child(mCommon.mInningsDBKey.toString()).child("round").setValue(roundName);
                            Toast.makeText(LoginActivity.this,
                                    "Round " + roundName + " created!", Toast.LENGTH_SHORT).show();
                            mCommon.mRoundName = roundName;
                            mCommon.mGoldPresentPlayerNames.clear();
                            mCommon.mSilverPresentPlayerNames.clear();
                            Log.d(TAG, "WRITTEN mRoundName: " + roundName + " data=" +
                                    mCommon.toString());
                            killActivity();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle(mCommon.getColorString("Really?", Color.RED));
            builder.setMessage("You are about to create a new round!\nAre you sure?")
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        } else {
            createEnterDataActivity();
        }
    }

    private void createEnterDataActivity()     {

        if(mActToStart.equals(Constants.ACTIVITY_SETTINGS)) {
            mCommon.wakeUpDBConnection();
            Intent settingsIntent = new Intent(LoginActivity.this, ClubLeagueSettings.class);
            LoginActivity.this.startActivityForResult(settingsIntent, Constants.SETTINGS_ACTIVITY);
            return;
        } else if(mActToStart.equals(Constants.ACTIVITY_TOURNA_SETTINGS)) {
            mCommon.wakeUpDBConnection();
            Intent settingsIntent = new Intent(LoginActivity.this, TournaSettings.class);
            LoginActivity.this.startActivityForResult(settingsIntent, Constants.SETTINGS_ACTIVITY);
            return;
        }
        if (mTournaFlag) {
            Intent thisIntent = getIntent(); // gets the previously created intent
            String tType = thisIntent.getStringExtra(Constants.TOURNATYPE);

            Log.i(TAG, "successfulLogin, tournament mode");
            if(tType.equals(Constants.SE) || tType.equals(Constants.DE)){
                Intent myIntent = new Intent(LoginActivity.this, TournaSEDEEnterData.class);
                myIntent.putExtra(Constants.TOURNATYPE, tType);
                myIntent.putExtra(Constants.MATCH, thisIntent.getStringExtra(Constants.MATCH));
                myIntent.putExtra(Constants.FIXTURE, thisIntent.getStringExtra(Constants.FIXTURE));
                myIntent.putStringArrayListExtra(Constants.TEAMS, thisIntent.getStringArrayListExtra(Constants.TEAMS));
                myIntent.putStringArrayListExtra(Constants.TEAM1PLAYERS, thisIntent.getStringArrayListExtra(Constants.TEAM1PLAYERS));
                myIntent.putStringArrayListExtra(Constants.TEAM2PLAYERS, thisIntent.getStringArrayListExtra(Constants.TEAM2PLAYERS));
                LoginActivity.this.startActivityForResult(myIntent, Constants.ENTERDATA_ACTIVITY);
            } else {
                Intent myIntent = new Intent(LoginActivity.this, TournaLeagueEnterData.class);
                myIntent.putExtra("gametype", mGameTypeRadioButton.getText());
                LoginActivity.this.startActivityForResult(myIntent, Constants.ENTERDATA_ACTIVITY);
            }

        } else {
            String newRoundFlag = "False";
            Log.i(TAG, "successfulLogin, new round flag:" + newRoundFlag);
            Intent myIntent = new Intent(LoginActivity.this, ClubLeagueEnterData.class);
            myIntent.putExtra("gametype", mGameTypeRadioButton.getText());
            myIntent.putExtra("group", mGroupRadioButton.getText());
            myIntent.putExtra("new_round", newRoundFlag);
            LoginActivity.this.startActivityForResult(myIntent, Constants.ENTERDATA_ACTIVITY);
        }
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 3;
    }

    /**
     * Shows the progress UI and hides the login form.
     */

    private void showProgress(final boolean show) {

        if(show) {

            //it could happen that the user moves this app to background while the background loop is running.
            //In thats case, dialog will fail: "WindowManager$BadTokenException: Unable to add window"
            //So, check if this activity is in foreground before displaying dialogue.
            if (isFinishing()) return;
            if (!ScoreTally.isActivityVisible()) return;

            if (progressDialog != null) {
                return;
            }
            //Log.d(TAG, "startProgressDialog: ");
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setTitle("Fetching club data"); // Setting Title
            progressDialog.setMessage("Connecting..."); // Setting Message
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
            progressDialog.show(); // Display Progress Dialog
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
        } else {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }

    void setUserInDB() {
        if(mUser.isEmpty()) return;
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(Constants.ACTIVE_USERS).child(mUser);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.w(TAG, "setUserInDB: onDataChange");
                if(dataSnapshot.exists()){
                    //no worry about support for prev releases, as users entry under root
                    //was created for the first time in 4.0.0 with UserDBEntry class.
                    UserDBEntry usr = dataSnapshot.getValue(UserDBEntry.class);
                    if(usr==null || dataSnapshot.getKey()==null) {
                        Log.w(TAG, "setUserInDB: usr is null");
                        return;
                    }
                    //if(dataSnapshot.getKey().contains(Constants.USERID_TMP)) {
                    if(dataSnapshot.getKey() != null) {
                        usr.setTs("now");
                        usr.setPh("");
                        usr.setClub(mClub);   //may be last login was for another club
                        usr.setVer( mVerCode );
                        dbRef.setValue(usr);
                        Log.d(TAG, "setUserInDB: DB updated:" + dataSnapshot.getKey() + " >> "
                                + usr.toString());
                    }
                } else {
                    UserDBEntry usr = new UserDBEntry();
                    usr.setClub(mClub);
                    usr.setVer( mVerCode );
                    dbRef.setValue(usr);
                    Log.d(TAG, "setUserInDB: Usr created in DB:" + dataSnapshot.getKey() + " >> "
                            + usr.toString());
                }

                //user id is set in the Club tree too from wakeUpDBConnection()
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "setUserInDB: onCancelled", databaseError.toException());
            }
        });

    }
}

