package com.sg0.baddytally;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

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
    private CheckBox mCheckNewRound;


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
        mCheckNewRound = findViewById(R.id.check_newround);

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

        final String club = prefs.getString(Constants.DATA_CLUB, "");
        final String user = prefs.getString(Constants.DATA_USER, "");
        if (club.isEmpty()) {
            mInitialAttempt = true;
            mGameTypeRadioGroup.setVisibility(View.GONE);
            mGroupRadioGroup.setVisibility(View.GONE);
            mCheckNewRound.setVisibility(View.GONE);
        }else {
            mClubView.setText(club);
            mUserView.setText(user);
        }

        if(!mInitialAttempt) {
            SharedData data = SharedData.getInstance();
            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
            String todaysDate = df.format(c);
            Log.d(TAG, "onCreate LoginActivity["+ todaysDate + "]: data = " + data.toString());
            //If this a new date, then check new round flag.
            if(data.mRoundName.contains(todaysDate))
                mCheckNewRound.setChecked(false);
            else
                mCheckNewRound.setChecked(true);
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

        FloatingActionButton fab = findViewById(R.id.fab_return);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                killActivity();
            }
        });

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
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.PROFILE);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.w(TAG, "fetchInitialData: onDataChange");
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    if(null==child) continue;
                    switch (child.getKey()) {
                        case "admincode":
                            mAdminCode = child.getValue(String.class);
                            break;
                        case "memcode":
                            mMemCode = child.getValue(String.class);
                            break;
                        case "rootcode":
                            mRootCode = child.getValue(String.class);
                            break;
                    }

                }
                Log.w(TAG, "fetchInitialData: onDataChange:" + mAdminCode + "/" + mRootCode);
                attemptLogin();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "fetchInitialData: onCancelled", databaseError.toException());
                Toast.makeText(LoginActivity.this,
                        "Login Profile DB error:" + databaseError.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }

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

        showProgress(false);
        if (mInitialAttempt) {
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

        if(mCheckNewRound.isChecked()) {
            //Are you sure you want to create a new round of games?
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            createEnterDataActivity();
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
        String newRoundFlag = "False";
        if(mCheckNewRound.isChecked()) newRoundFlag = Constants.NEWROUND;
        Log.i(TAG, "successfulLogin, new round flag:" + newRoundFlag);
        Intent myIntent = new Intent(LoginActivity.this, EnterData.class);
        myIntent.putExtra("gametype", mGameTypeRadioButton.getText());
        myIntent.putExtra("group", mGroupRadioButton.getText());
        myIntent.putExtra("new_round", newRoundFlag);
        LoginActivity.this.startActivityForResult(myIntent,Constants.ENTERDATA_ACTIVITY);
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

