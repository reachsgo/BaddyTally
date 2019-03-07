package com.sg0.baddytally;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity implements CallbackRoutine{

    private static final String TAG = "LoginActivity";

    private EditText mClubView;
    private EditText mUserView;
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


    private void killActivity(){
        finish();
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

        if(SharedData.getInstance().mNumOfGroups==1) {
            //Only one group exists
            findViewById(R.id.gamegroup_silver).setEnabled(false);
            ((RadioButton)findViewById(R.id.gamegroup_gold)).setChecked(true);
        }
        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        final String secpd = prefs.getString(Constants.DATA_SEC, "");
        if (!secpd.isEmpty()) {
            mPasswordView.setText(secpd);
        }

        SharedData data = SharedData.getInstance();
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat(Constants.ROUND_DATEFORMAT_SHORT, Locale.CANADA);
        String todaysDate = df.format(c);
        Log.d(TAG, "onCreate LoginActivity["+ todaysDate + "]: data = " + data.toString());

        final String club = prefs.getString(Constants.DATA_CLUB, "");
        final String user = prefs.getString(Constants.DATA_USER, "");
        if (club.isEmpty()) {
            mInitialAttempt = true;
            mGameTypeRadioGroup.setVisibility(View.GONE);
            mGroupRadioGroup.setVisibility(View.GONE);
            findViewById(R.id.new_round_btn).setVisibility(View.GONE);
            findViewById(R.id.current_round).setVisibility(View.GONE);
            findViewById(R.id.time_now).setVisibility(View.GONE);
        }else {
            mClubView.setText(club);
            mUserView.setText(user);
            String roundStr =  "Active round: ";
            if(data.mRoundName.isEmpty()) roundStr += "None";
            else roundStr += SharedData.getInstance().getShortRoundName(data.mRoundName);
            ((TextView)findViewById(R.id.current_round)).setText(roundStr);
            ((TextView)findViewById(R.id.time_now)).setText(("Today's date: " + df.format(c)));
            if (!data.mRoundName.contains(todaysDate)) {
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


    }

    private void prepareForLogin(String club, String secpd) {
        mClub = mClubView.getText().toString();
        fetchInitialData();
    }

    //TODO: This DB fetch is done every time data is entered. This could be optimized to
    //reduce number of DB reads.
    private void fetchInitialData(){
        Log.w(TAG, "fetchInitialData: " + mClub );
        if (!SharedData.getInstance().mMemCode.isEmpty() && !mInitialAttempt) {
            Log.w(TAG, "fetchInitialData: data already populated!");
            SharedData data = SharedData.getInstance();
            mAdminCode = data.mAdminCode;
            mMemCode = data.mMemCode;
            mRootCode = data.mRootCode;
            attemptLogin();
            return;
        }

        SharedData.getInstance().fetchProfile(LoginActivity.this, LoginActivity.this , mClub);
    }

    //CallbackRoutine Callback after profile is fetched from DB. See SharedData impl of fetchProfile()
    public void profileFetched() {
        SharedData data = SharedData.getInstance();
        Log.w(TAG, "profileFetched invoked ...." + data.toString());
        mAdminCode = data.mAdminCode;
        mMemCode = data.mMemCode;
        mRootCode = data.mRootCode;
        attemptLogin();
    }

    public void alertResult(final String in, final Boolean ok, final Boolean ko) {
    }

    public void completed(final String in, final Boolean ok) {
    }

    public void callback(final String key, final Object inobj) {}

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String club = mClubView.getText().toString();
        String secpd = mPasswordView.getText().toString();
        mUser = mUserView.getText().toString();

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

        Log.v(TAG, "attemptLogin(" + club + "," + secpd + "):" + mAdminCode + ":" + mMemCode);

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            showProgress(true);
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
                showProgress(false);
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }
    }

    private void successfulLogin(String club, String secpd) {
        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.DATA_CLUB, club);
        editor.putString(Constants.DATA_SEC, secpd);
        editor.putString(Constants.DATA_ROLE, mRole);
        editor.putString(Constants.DATA_USER, mUser);
        editor.apply();
        SharedData data = SharedData.getInstance();
        data.mClub = club;
        data.mRole = mRole;
        data.mUser = mUser;

        showProgress(false);
        if (mInitialAttempt && mActToStart.isEmpty()) {
            killActivity();   //finish was not ending the activity here.
            return;
        }

        if(mActToStart.equals(Constants.INITIAL)) {
            killActivity();   //finish was not ending the activity here.
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
                            String roundName = SharedData.getInstance().createNewRoundName(true, LoginActivity.this);
                            FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.INNINGS)
                                    .child(SharedData.getInstance().mInningsDBKey.toString()).child("round").setValue(roundName);
                            Toast.makeText(LoginActivity.this,
                                    "Round " + roundName + " created!", Toast.LENGTH_SHORT).show();
                            SharedData.getInstance().mRoundName = roundName;
                            SharedData.getInstance().mGoldPresentPlayerNames.clear();
                            SharedData.getInstance().mSilverPresentPlayerNames.clear();
                            Log.d(TAG, "WRITTEN mRoundName: " + roundName + " data=" + SharedData.getInstance().toString());
                            killActivity();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle(SharedData.getInstance().getColorString("Really?", Color.RED));
            builder.setMessage("You are about to create a new round!\nAre you sure?")
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        } else {
            createEnterDataActivity();
        }
    }

    private void createEnterDataActivity()     {

        if(mActToStart.equals(Constants.ACTIVITY_SETTINGS)) {
            SharedData.getInstance().wakeUpDBConnection();
            Intent settingsIntent = new Intent(LoginActivity.this, Settings.class);
            LoginActivity.this.startActivityForResult(settingsIntent, Constants.SETTINGS_ACTIVITY);
            return;
        } else if(mActToStart.equals(Constants.ACTIVITY_TOURNA_SETTINGS)) {
            SharedData.getInstance().wakeUpDBConnection();
            Intent settingsIntent = new Intent(LoginActivity.this, TournaSettings.class);
            LoginActivity.this.startActivityForResult(settingsIntent, Constants.SETTINGS_ACTIVITY);
            return;
        }
        //if (SharedData.getInstance().mTournaMode) {
        if (mTournaFlag) {
            Intent thisIntent = getIntent(); // gets the previously created intent
            String tType = thisIntent.getStringExtra(Constants.TOURNATYPE);

            Log.i(TAG, "successfulLogin, tournament mode");
            if(tType.equals(Constants.SE) || tType.equals(Constants.DE)){
                Intent myIntent = new Intent(LoginActivity.this, TournaBaseEnterData.class);
                myIntent.putExtra(Constants.TOURNATYPE, tType);
                myIntent.putExtra(Constants.MATCH, thisIntent.getStringExtra(Constants.MATCH));
                myIntent.putExtra(Constants.FIXTURE, thisIntent.getStringExtra(Constants.FIXTURE));
                myIntent.putStringArrayListExtra(Constants.TEAMS, thisIntent.getStringArrayListExtra(Constants.TEAMS));
                myIntent.putStringArrayListExtra(Constants.TEAM1PLAYERS, thisIntent.getStringArrayListExtra(Constants.TEAM1PLAYERS));
                myIntent.putStringArrayListExtra(Constants.TEAM2PLAYERS, thisIntent.getStringArrayListExtra(Constants.TEAM2PLAYERS));
                LoginActivity.this.startActivityForResult(myIntent, Constants.ENTERDATA_ACTIVITY);
            } else {
                Intent myIntent = new Intent(LoginActivity.this, TournaEnterData.class);
                myIntent.putExtra("gametype", mGameTypeRadioButton.getText());
                LoginActivity.this.startActivityForResult(myIntent, Constants.ENTERDATA_ACTIVITY);
            }

        } else {
            String newRoundFlag = "False";
            Log.i(TAG, "successfulLogin, new round flag:" + newRoundFlag);
            Intent myIntent = new Intent(LoginActivity.this, EnterData.class);
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
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}

