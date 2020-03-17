package com.sg0.baddytally;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
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


public class LoginActivity extends AppCompatActivity implements CallbackRoutine {

    private static final String TAG = "LoginActivity";

    private EditText mClubView;
    private TextView mUserView;
    private EditText mPasswordView;
    private RadioGroup mGameTypeRadioGroup;
    private RadioButton mGameTypeRadioButton;
    private RadioGroup mGroupRadioGroup;
    private RadioButton mGroupRadioButton;
    private String mAdminCode;
    private String mMemCode;
    private String mSuperUserCode;
    private String mLoginClub;
    private String mUser;
    private String mLoginRole;
    private boolean mInitialAttempt;
    private boolean mTournaFlag;
    private String mActToStart;
    private Handler mMainHandler;
    private SharedData mCommon;
    private int mVerCode;

    private void killActivity() {
        finish();
    }


    @Override
    protected void onDestroy() {
        mMainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mInitialAttempt = false;
        mClubView = findViewById(R.id.clubname);
        mUserView = findViewById(R.id.username);
        mPasswordView = findViewById(R.id.password);
        mGameTypeRadioGroup = findViewById(R.id.gametype_radiogroup);
        mGroupRadioGroup = findViewById(R.id.gamegroup_radiogroup);
        mTournaFlag = false;
        mMainHandler = new Handler();
        mCommon = SharedData.getInstance();
        mVerCode = SharedData.getAppVersionCode(LoginActivity.this);

        //checked attribute in XML doesnt seem to work in the new API version.
        //so, setting the defaults in code here.
        mGameTypeRadioGroup.clearCheck();
        mGroupRadioGroup.clearCheck();  //after recreate() sometimes the default is not checked
        ((RadioButton) findViewById(R.id.gametype_default)).setChecked(true);
        ((RadioButton) findViewById(R.id.gamegroup_silver)).setChecked(true);

        Intent thisIntent = getIntent(); // gets the previously created intent
        mActToStart = "";
        String actToStart = thisIntent.getStringExtra(Constants.ACTIVITY);
        if (null != actToStart && !actToStart.isEmpty()) {
            mActToStart = actToStart;
        }
        String club = thisIntent.getStringExtra(Constants.DATA_CLUB);
        if (null != club && !club.isEmpty()) {
            mLoginClub = club;
        }

        //Log.d(TAG, "onCreate mode:" + mActToStart);
        if (mActToStart.equals(Constants.INITIAL)) {
            //Log.d(TAG, "onCreate initial mode");
            findViewById(R.id.options_ll).setVisibility(View.GONE);
            findViewById(R.id.next_btn).setVisibility(View.GONE);
            findViewById(R.id.new_round_btn).setVisibility(View.GONE);
            findViewById(R.id.current_round).setVisibility(View.GONE);
            findViewById(R.id.time_now).setVisibility(View.GONE);
        } else if (mActToStart.equals(Constants.ACTIVITY_CLUB_ENTERDATA)) {
            //Log.d(TAG, "onCreate club-enter mode");
            findViewById(R.id.passwd_view_ll).setVisibility(View.GONE);
            findViewById(R.id.email_sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.role_radiogroup).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.header)).setText("Select the group and match type");
        }

        //hide keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (null != inputMethodManager)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        if (mCommon.mNumOfGroups == 1) {
            //Only one group exists
            findViewById(R.id.gamegroup_silver).setEnabled(false);
            ((RadioButton) findViewById(R.id.gamegroup_gold)).setChecked(true);
        }
        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        final String secpd = prefs.getString(Constants.DATA_SEC, "");
        if (!secpd.isEmpty()) {
            mPasswordView.setText(secpd);
        }

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat(Constants.ROUND_DATEFORMAT_SHORT, Locale.CANADA);
        String todaysDate = df.format(c);
        //Log.d(TAG, "onCreate LoginActivity["+ todaysDate + "]: data = " + mCommon.toString());

        if (null == club || club.isEmpty()) {  //if not club passed in, read from local file
            SharedPreferences clubprefs = getSharedPreferences(Constants.USERDATA_LASTCLUB, MODE_PRIVATE);
            club = clubprefs.getString(Constants.DATA_CLUB, "");
        }

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
        } else {
            if(!club.equals(Constants.DEMO_CLUB) && !club.equals(Constants.STCLUB)) {
                mClubView.setText(club);
                mPasswordView.requestFocus();
            }
            String roundStr = "Active round: ";
            if (mCommon.mRoundName.isEmpty()) roundStr += "None";
            else roundStr += mCommon.getShortRoundName(mCommon.mRoundName);
            ((TextView) findViewById(R.id.current_round)).setText(roundStr);
            ((TextView) findViewById(R.id.time_now)).setText(("Today's date: " + df.format(c)));
            if (!mCommon.mRoundName.contains(todaysDate)) {
                findViewById(R.id.new_round_btn).setBackground(getResources().getDrawable(R.drawable.roundltblue));
                //findViewById(R.id.new_round_btn).setBackgroundColor(R.drawable.roundltblue);
                //getResources().getColor(R.color.colorRed));
            }
        }

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                //Log.d(TAG, "onEditorAction: ");
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    //Gotcha: When the user account is locked (after many bad passwds),
                    //the "sign in" button will be Gone. Even in that case, if you restart the app,
                    //you can enter the correct password using "Done" key of the virtual keyboard!
                    prepareForLogin();
                    return true;
                }
                return false;
            }
        });

        final Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        //mEmailSignInButton.setFocus
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                prepareForLogin();
                //Sequence is:
                //prepareForLogin
                //fetchInitialData
                //attemptLogin -> attemptLogin2
                //profileFetched -> attemptLogin -> attemptLogin2
            }
        });

        findViewById(R.id.new_round_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //Are you sure you want to create a new round of games?
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                if (mCommon.mInningsDBKey == -1) {
                                    Toast.makeText(LoginActivity.this,
                                            "Go to Settings and create a new innings first.", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                final String roundName = mCommon.createNewRoundName(true, LoginActivity.this);
                                FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(dataSnapshot.exists()) {
                                                    //create only if the club exists
                                                    FirebaseDatabase.getInstance().getReference().child(mCommon.mClub)
                                                            .child(Constants.INNINGS)
                                                            .child(mCommon.mInningsDBKey.toString())
                                                            .child(Constants.ROUND).setValue(roundName);
                                                    Toast.makeText(LoginActivity.this,
                                                            "Round " + roundName + " created!", Toast.LENGTH_SHORT).show();
                                                    mCommon.mRoundName = roundName;
                                                    mCommon.mGoldPresentPlayerNames.clear();
                                                    mCommon.mSilverPresentPlayerNames.clear();
                                                    //Log.d(TAG, "WRITTEN mRoundName: " + roundName + " data=" +
                                                    //        mCommon.toString());
                                                    //let the user click NEXT after this, dont kill the activity here.
                                                    findViewById(R.id.new_round_btn).setBackground(
                                                            getResources().getDrawable(R.drawable.roundedrect));
                                                    findViewById(R.id.new_round_btn).setEnabled(false);
                                                } else {
                                                    Log.e(TAG, "onDataChange: Club missing in DB:" + mCommon.mClub);
                                                    mCommon.killActivity(LoginActivity.this, RESULT_OK);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

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
            }
        });

        findViewById(R.id.next_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                String newRoundFlag = "False";
                //Log.i(TAG, "next, new round flag:" + newRoundFlag);

                boolean err = false;
                try {
                    //Returns the identifier of the selected radio button in this group.
                    //Upon empty selection, the returned value is -1.
                    int selectedId1 = mGameTypeRadioGroup.getCheckedRadioButtonId();
                    int selectedId2 = mGroupRadioGroup.getCheckedRadioButtonId();

                    if(selectedId1==-1 || selectedId2==-1) {
                        err = true;
                    } else {
                        mGameTypeRadioButton = findViewById(selectedId1);
                        mGroupRadioButton = findViewById(selectedId2);
                    }
                } catch (Exception e) {
                    err = true;
                }
                if(err) {
                    Toast.makeText(LoginActivity.this,
                            "Select both group and match type.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                //Log.v(TAG, "next:" + mGameTypeRadioButton.getText() + ":" + mGroupRadioButton.getText());

                Intent myIntent = new Intent(LoginActivity.this, ClubLeagueEnterData.class);
                myIntent.putExtra("gametype", mGameTypeRadioButton.getText());
                myIntent.putExtra("group", mGroupRadioButton.getText());
                myIntent.putExtra("new_round", newRoundFlag);
                LoginActivity.this.startActivityForResult(myIntent, Constants.ENTERDATA_ACTIVITY);
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab_return);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        String lockedTimeStr = prefs.getString(Constants.DATA_LOCKED, "");
        if (!lockedTimeStr.isEmpty()) {
            Long lockTime;
            try {
                lockTime = Long.parseLong(lockedTimeStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "NumberFormatException: " + lockedTimeStr);
                lockTime = 0L;
            }
            //Log.i(TAG, "lockTime: " + lockTime);
            Long now = System.currentTimeMillis();
            //if((now - lockTime) < 300) {
            if ((now - lockTime) < 18000000) {   //5 hrs = 5 * 60 * 60000
                lockedMsg(lockedTimeStr);
            } else {
                Log.i(TAG, "onCreate: Unlocking the system now:" + now.toString());
            }
        }

        ((RadioGroup)findViewById(R.id.role_radiogroup)).setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                // Reset errors.
                mPasswordView.setError(null);
            }
        });

        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Log.d(TAG, "onOptionsItemSelected: ");
        switch (item.getItemId()) {
            case android.R.id.home:
                //onBackPressed();
                mCommon.logOut(LoginActivity.this, true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void prepareForLogin() {
        mLoginClub = mClubView.getText().toString().trim();
        String secpd = mPasswordView.getText().toString();

        if (mLoginClub.isEmpty() || secpd.isEmpty()) {
            Toast.makeText(LoginActivity.this,
                    "All fields are mandatory!", Toast.LENGTH_SHORT).show();
            return;
        }

        findViewById(R.id.email_sign_in_button).setEnabled(false);  //disable signin button
        fetchInitialData();
    }




    private void fetchInitialData() {
        //Log.i(TAG, mCommon.mCount + ":fetchInitialData: " + mLoginClub + "/" + mCommon.mClub);

        String secpd = mPasswordView.getText().toString();
        if(mCommon.isRootLogin(mLoginClub, secpd)) {
            //root login, using root profile details.
            //Next step is to login again using the actual club name, but with the same root passwd.
            //stored passwd is audited later in isPermitted(), if the root passwd in DB has been changed.
            Toast.makeText(LoginActivity.this,
                    "....",
                    Toast.LENGTH_LONG).show();

            mCommon.addListenerForNewClub(getApplicationContext());
            //When the app gets moved out by android scheduler, listeners will not work any more.
            //So, start a repeating service, so that listener can be pricked every now and then!
            //This service is later stopped from logOut().
            RootService.startRepeatingIntent(getApplicationContext());

            mLoginRole = Constants.ROOT;
            successfulLogin(Constants.STCLUB, secpd);
            return; 
        }

        if (!mCommon.mProfile.getMc().isEmpty() && !mInitialAttempt) {
            Log.i(TAG, "fetchInitialData: data already populated!");
            mAdminCode = mCommon.mProfile.getAc();
            mMemCode = mCommon.mProfile.getMc();
            mSuperUserCode = mCommon.mProfile.getRc();
            attemptLogin();
            return;
        }

        //hide keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (null != inputMethodManager)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        mCommon.startProgressDialog(LoginActivity.this, "Login", "");

        mCommon.fetchProfile(LoginActivity.this, LoginActivity.this, mLoginClub);
        //see profileFetched() for next step

        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this,
                        "Club not found!\nInvalid Club or bad network connectivity!",
                        Toast.LENGTH_LONG).show();
                mCommon.stopProgressDialog(LoginActivity.this);
                getReadyForNextAttempt();
            }
        }, Constants.DB_READ_TIMEOUT);
    }


    private void attemptLogin() {

        if( (mCommon.mProfile!=null && mCommon.mProfile.getVer()>0 &&
                mVerCode < mCommon.mProfile.getVer()) ||
            (mCommon.mRootProfile!=null && mCommon.mRootProfile.getVer()>0 &&
                mVerCode < mCommon.mRootProfile.getVer()) )
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle(mCommon.getColorString("Update your app", Color.RED));
            builder.setMessage("There is a newer app version available.\nPlease update to the latest version.")
                    .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            onBackPressed();
                            //mCommon.killApplication(LoginActivity.this);
                        }
                    })
                    .show();
        } else {
            if (!attemptLogin2()) {
                //login attempt failed
                getReadyForNextAttempt();
            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private boolean attemptLogin2() {

        // Reset errors.
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String club = mClubView.getText().toString().trim();
        String secpd = mPasswordView.getText().toString();

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(secpd) && !isPasswordValid(secpd)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            mPasswordView.requestFocus();
            return false;
        }

        //Log.v(TAG, "attempt:[" + club + "],[" + secpd + "]");

        //hide keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (null != inputMethodManager)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        RadioButton radioBtn = null;
        boolean err = false;
        try {
            //Returns the identifier of the selected radio button in this group.
            //Upon empty selection, the returned value is -1.
            int selectedId = ((RadioGroup)findViewById(R.id.role_radiogroup)).getCheckedRadioButtonId();

            if(selectedId==-1) {
                err = true;
            } else {
                radioBtn = findViewById(selectedId);
            }
        } catch (Exception e) {
            err = true;
        }
        if(err || null==radioBtn) {
            Toast.makeText(LoginActivity.this,
                    "Select the role.",
                    Toast.LENGTH_SHORT).show();
            findViewById(R.id.role_radiogroup).requestFocus();
            return false;
        }

        //Log.v(TAG, "attempt2:[" + mSuperUserCode + "],[" + mAdminCode + "],[" +
        //        mMemCode + "][" + radioBtn.getText().toString() + "]");

        err = true;
        if(mCommon.isRoot()) {
            mLoginRole = Constants.ROOT;
            successfulLogin(club, secpd);
            err = false;
        } else if(radioBtn.getText().toString().equals(getString(R.string.superuser))) {
            if (secpd.equals(mSuperUserCode)) {
                mLoginRole = Constants.SUPERUSER;
                successfulLogin(club, secpd);
                err = false;
            }
        } else if(radioBtn.getText().toString().equals(getString(R.string.admin))) {
            if (secpd.equals(mAdminCode)) {
                mLoginRole = Constants.ADMIN;
                successfulLogin(club, secpd);
                err = false;
            }
        } else if(radioBtn.getText().toString().equals(getString(R.string.member))) {
            if (secpd.equals(mMemCode)) {
                mLoginRole = Constants.MEMBER;
                successfulLogin(club, secpd);
                err = false;
            }
        }  else {
            mPasswordView.setError(getString(R.string.error_incorrect_password));
            mPasswordView.requestFocus();
            return false;
        }

        if(err) {
            Log.d(TAG, "attemptLogin2: mismatch error");
            mPasswordView.setError(getString(R.string.error_incorrect_password));
            mPasswordView.requestFocus();
            return false;
        }

        return true; //successful login
    }

    private void getReadyForNextAttempt() {
        //Log.w(TAG, "getReadyForNextAttempt: " + mCommon.mCount);
        mCommon.mCount++;
        findViewById(R.id.email_sign_in_button).setEnabled(true);
        mPasswordView.requestFocus();
        if (mCommon.mCount == Constants.DATA_LOCKED_COUNT_MAX - 2) {
            Toast.makeText(LoginActivity.this,
                    "You will be locked out if you get it wrong again!",
                    Toast.LENGTH_LONG).show();
        } else if (mCommon.mCount > Constants.DATA_LOCKED_COUNT_MAX) {
            Long now = System.currentTimeMillis();
            SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.DATA_LOCKED, now.toString());
            editor.apply();
            lockedMsg(now.toString());
        }
    }

    private void lockedMsg(final String lockTimeStr) {
        Toast.makeText(LoginActivity.this,
                "User locked out!\nToo many failed attempts to log in!\n",
                Toast.LENGTH_LONG).show();
        Log.e(TAG, "lockedMsg: System is locked from:" + lockTimeStr);
        findViewById(R.id.options_ll).setVisibility(View.GONE);
        findViewById(R.id.new_round_btn).setVisibility(View.GONE);
        findViewById(R.id.current_round).setVisibility(View.GONE);
        findViewById(R.id.time_now).setVisibility(View.GONE);
        findViewById(R.id.email_sign_in_button).setVisibility(View.INVISIBLE);
    }

    private void successfulLogin(String club, String secpd) {
        setUserInDB();
        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.DATA_CLUB, club);
        editor.putString(Constants.DATA_SEC, secpd);
        editor.putString(Constants.DATA_ROLE, mLoginRole);
        editor.putString(Constants.DATA_LOCKED, "");
        //mUser already written in getUserID
        editor.apply();

        //Save club name in a separate file. Even if the cache is cleared (logout),
        //show the last club name in the login page.
        SharedPreferences clubprefs = getSharedPreferences(Constants.USERDATA_LASTCLUB, MODE_PRIVATE);
        SharedPreferences.Editor clubeditor = clubprefs.edit();
        clubeditor.putString(Constants.DATA_CLUB, club);
        clubeditor.apply();

        mCommon.mClub = club;
        mCommon.mRole = mLoginRole;
        mCommon.mUser = mUser;
        mCommon.wakeUpDBConnection();  //update DB with the new user login
        if (mInitialAttempt && mActToStart.isEmpty()) {
            killActivity();   //finish was not ending the activity here.
            return;
        }

        if (mActToStart.equals(Constants.INITIAL)) {
            //start from Main again. There is no history for initial activities, so we cant just do a
            //killActivity here.
            mCommon.killActivity(this, RESULT_OK);
            Intent intent = new Intent(LoginActivity.this, MainSigninActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

    }

    private void createEnterDataActivity() {

        if (mActToStart.equals(Constants.ACTIVITY_SETTINGS)) {
            mCommon.wakeUpDBConnection();
            Intent settingsIntent = new Intent(LoginActivity.this, ClubLeagueSettings.class);
            LoginActivity.this.startActivityForResult(settingsIntent, Constants.SETTINGS_ACTIVITY);
            return;
        } else if (mActToStart.equals(Constants.ACTIVITY_TOURNA_SETTINGS)) {
            mCommon.wakeUpDBConnection();
            Intent settingsIntent = new Intent(LoginActivity.this, TournaSettings.class);
            LoginActivity.this.startActivityForResult(settingsIntent, Constants.SETTINGS_ACTIVITY);
            return;
        }
        if (mTournaFlag) {
            Intent thisIntent = getIntent(); // gets the previously created intent
            String tType = thisIntent.getStringExtra(Constants.TOURNATYPE);

            //Log.i(TAG, "successfulLogin, tournament mode");
            if (tType.equals(Constants.SE) || tType.equals(Constants.DE)) {
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
            //Log.i(TAG, "successfulLogin, new round flag:" + newRoundFlag);
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

    void setUserInDB() {
        if (mUser.isEmpty()) return;
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(Constants.ACTIVE_USERS).child(mUser);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Log.w(TAG, "setUserInDB: onDataChange");
                if (dataSnapshot.exists()) {
                    //no worry about support for prev releases, as users entry under root
                    //was created for the first time in 4.0.0 with UserDBEntry class.
                    UserDBEntry usr = dataSnapshot.getValue(UserDBEntry.class);
                    if (usr == null || dataSnapshot.getKey() == null) {
                        Log.w(TAG, "setUserInDB: usr is null");
                        return;
                    }
                    //if(dataSnapshot.getKey().contains(Constants.USERID_TMP)) {
                    if (dataSnapshot.getKey() != null) {
                        usr.setTs("now");
                        usr.setPh("");
                        usr.setClub(mLoginClub);   //may be last login was for another club
                        usr.setVer(mVerCode);
                        dbRef.setValue(usr);
                        //Log.d(TAG, "setUserInDB: DB updated:" + dataSnapshot.getKey() + " >> "
                        //        + usr.toString());
                    }
                } else {
                    UserDBEntry usr = new UserDBEntry();
                    usr.setClub(mLoginClub);
                    usr.setVer(mVerCode);
                    dbRef.setValue(usr);
                    //Log.d(TAG, "setUserInDB: Usr created in DB:" + dataSnapshot.getKey() + " >> "
                    //        + usr.toString());
                }

                //user id is set in the Club tree too from wakeUpDBConnection()
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "setUserInDB: onCancelled", databaseError.toException());
            }
        });
    }

    //CallbackRoutine Callback after profile is fetched from DB. See SharedData impl of fetchProfile()
    public void profileFetched() {
        //Log.w(TAG, "profileFetched invoked ...." + mCommon.toString());
        mSuperUserCode = mCommon.mProfile.getRc();
        mAdminCode = mCommon.mProfile.getAc();
        mMemCode = mCommon.mProfile.getMc();

        mMainHandler.removeCallbacksAndMessages(null);  //delete the toast runnables posted above
        mCommon.stopProgressDialog(LoginActivity.this);

        attemptLogin();
    }

    public void alertResult(final String in, final Boolean ok, final Boolean ko) {
    }

    public void completed(final String in, final Boolean ok) {
    }

    public void callback(final String key, final Object inobj) {
    }

}

